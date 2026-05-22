package net.minecraft.world;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.NetherFortressStructure;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Утилитарный класс, реализующий логику естественного спавна мобов в мире.
 * <p>
 * Отвечает за сбор статистики существующих мобов, проверку лимитов групп спавна,
 * выбор случайных позиций и типов мобов, а также за первоначальное заселение чанков
 * при генерации мира.
 */
public final class SpawnHelper {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Минимальное расстояние от игрока (в блоках), ближе которого спавн запрещён. */
	private static final int MIN_SPAWN_DISTANCE = 24;

	/** Радиус спавна вокруг игрока в чанках. */
	public static final int SPAWN_RANGE = 8;

	/** Максимальное расстояние спавна от игрока в блоках. */
	public static final int MAX_SPAWN_DISTANCE = 128;

	/** Диагональный радиус спавна (SPAWN_RANGE / sqrt(2)). */
	public static final int SPAWN_RANGE_DIAGONAL = MathHelper.floor(8.0F / MathHelper.SQUARE_ROOT_OF_TWO);

	/** Площадь зоны спавна в чанках (17×17). */
	static final int CHUNK_AREA = (int) Math.pow(17.0, 2.0);

	/**
	 * Вероятность снижения спавна водных существ в биомах с тегом
	 * {@link BiomeTags#REDUCE_WATER_AMBIENT_SPAWNS}.
	 */
	private static final float WATER_AMBIENT_SPAWN_REDUCTION_CHANCE = 0.98F;

	/** Все группы спавна, кроме MISC (не участвуют в естественном спавне). */
	private static final SpawnGroup[] SPAWNABLE_GROUPS = Stream.of(SpawnGroup.values())
		.filter(spawnGroup -> spawnGroup != SpawnGroup.MISC)
		.toArray(SpawnGroup[]::new);

	private SpawnHelper() {
	}

	/**
	 * Собирает статистику существующих мобов в мире и формирует объект {@link Info}
	 * для последующего принятия решений о спавне.
	 * <p>
	 * Для каждого моба, не являющегося постоянным, обновляется гравитационное поле
	 * плотности и счётчик группы спавна.
	 *
	 * @param spawningChunkCount количество чанков, участвующих в спавне
	 * @param entities           все сущности в мире
	 * @param chunkSource        источник для получения чанков по позиции
	 * @param densityCapper      ограничитель плотности спавна на игрока
	 * @return объект {@link Info} с актуальной статистикой
	 */
	public static SpawnHelper.Info setupSpawn(
		int spawningChunkCount,
		Iterable<Entity> entities,
		SpawnHelper.ChunkSource chunkSource,
		SpawnDensityCapper densityCapper
	) {
		GravityField gravityField = new GravityField();
		Object2IntOpenHashMap<SpawnGroup> groupToCount = new Object2IntOpenHashMap<>();

		for (Entity entity : entities) {
			if (entity instanceof MobEntity mobEntity && (mobEntity.isPersistent() || mobEntity.cannotDespawn())) {
				continue;
			}

			SpawnGroup spawnGroup = entity.getType().getSpawnGroup();
			if (spawnGroup == SpawnGroup.MISC) {
				continue;
			}

			BlockPos blockPos = entity.getBlockPos();
			chunkSource.query(ChunkPos.toLong(blockPos), chunk -> {
				SpawnSettings.SpawnDensity spawnDensity = getBiomeDirectly(blockPos, chunk)
					.getSpawnSettings()
					.getSpawnDensity(entity.getType());

				if (spawnDensity != null) {
					gravityField.addPoint(entity.getBlockPos(), spawnDensity.mass());
				}

				if (entity instanceof MobEntity) {
					densityCapper.increaseDensity(chunk.getPos(), spawnGroup);
				}

				groupToCount.addTo(spawnGroup, 1);
			});
		}

		return new SpawnHelper.Info(spawningChunkCount, groupToCount, gravityField, densityCapper);
	}

	static Biome getBiomeDirectly(BlockPos pos, Chunk chunk) {
		return chunk.getBiomeForNoiseGen(
			BiomeCoords.fromBlock(pos.getX()),
			BiomeCoords.fromBlock(pos.getY()),
			BiomeCoords.fromBlock(pos.getZ())
		).value();
	}

	/**
	 * Возвращает список групп спавна, для которых разрешён спавн в текущем тике.
	 *
	 * @param info          статистика текущего спавна
	 * @param spawnAnimals  разрешён ли спавн мирных мобов
	 * @param spawnMonsters разрешён ли спавн враждебных мобов
	 * @param rare          разрешён ли спавн редких мобов
	 * @return список допустимых групп спавна
	 */
	public static List<SpawnGroup> collectSpawnableGroups(
		SpawnHelper.Info info,
		boolean spawnAnimals,
		boolean spawnMonsters,
		boolean rare
	) {
		List<SpawnGroup> result = new ArrayList<>(SPAWNABLE_GROUPS.length);

		for (SpawnGroup spawnGroup : SPAWNABLE_GROUPS) {
			if ((spawnAnimals || !spawnGroup.isPeaceful())
				&& (spawnMonsters || spawnGroup.isPeaceful())
				&& (rare || !spawnGroup.isRare())
				&& info.isBelowCap(spawnGroup)
			) {
				result.add(spawnGroup);
			}
		}

		return result;
	}

	public static void spawn(
		ServerWorld world,
		WorldChunk chunk,
		SpawnHelper.Info info,
		List<SpawnGroup> spawnableGroups
	) {
		Profiler profiler = Profilers.get();
		profiler.push("spawner");

		for (SpawnGroup spawnGroup : spawnableGroups) {
			if (info.canSpawn(spawnGroup, chunk.getPos())) {
				spawnEntitiesInChunk(spawnGroup, world, chunk, info::test, info::run);
			}
		}

		profiler.pop();
	}

	public static void spawnEntitiesInChunk(
		SpawnGroup group,
		ServerWorld world,
		WorldChunk chunk,
		SpawnHelper.Checker checker,
		SpawnHelper.Runner runner
	) {
		BlockPos spawnPos = getRandomPosInChunkSection(world, chunk);
		if (spawnPos.getY() >= world.getBottomY() + 1) {
			spawnEntitiesInChunk(group, world, chunk, spawnPos, checker, runner);
		}
	}

	/**
	 * Спавнит мобов в чанке в указанной позиции (отладочный метод).
	 *
	 * @param group группа спавна
	 * @param world серверный мир
	 * @param pos   позиция спавна
	 */
	@Debug
	public static void spawnEntitiesInChunk(SpawnGroup group, ServerWorld world, BlockPos pos) {
		spawnEntitiesInChunk(
			group,
			world,
			world.getChunk(pos),
			pos,
			(type, posx, chunk) -> true,
			(entity, chunk) -> {}
		);
	}

	/**
	 * Основной алгоритм спавна мобов в чанке.
	 * <p>
	 * Выполняет 3 попытки выбора кластера, в каждой из которых случайно смещает
	 * позицию и пытается заспавнить группу мобов одного типа. Количество мобов
	 * в группе определяется из {@link SpawnSettings.SpawnEntry#minGroupSize()} /
	 * {@link SpawnSettings.SpawnEntry#maxGroupSize()}.
	 *
	 * @param group   группа спавна
	 * @param world   серверный мир
	 * @param chunk   целевой чанк
	 * @param pos     начальная позиция спавна
	 * @param checker дополнительная проверка перед спавном
	 * @param runner  колбэк после успешного спавна
	 */
	public static void spawnEntitiesInChunk(
		SpawnGroup group,
		ServerWorld world,
		Chunk chunk,
		BlockPos pos,
		SpawnHelper.Checker checker,
		SpawnHelper.Runner runner
	) {
		StructureAccessor structureAccessor = world.getStructureAccessor();
		ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
		int spawnY = pos.getY();
		BlockState blockState = chunk.getBlockState(pos);

		if (blockState.isSolidBlock(chunk, pos)) {
			return;
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int totalSpawned = 0;

		for (int attempt = 0; attempt < 3; attempt++) {
			int spawnX = pos.getX();
			int spawnZ = pos.getZ();
			SpawnSettings.SpawnEntry spawnEntry = null;
			EntityData entityData = null;
			int groupSize = MathHelper.ceil(world.random.nextFloat() * 4.0F);
			int groupSpawned = 0;

			for (int groupAttempt = 0; groupAttempt < groupSize; groupAttempt++) {
				spawnX += world.random.nextInt(6) - world.random.nextInt(6);
				spawnZ += world.random.nextInt(6) - world.random.nextInt(6);
				mutable.set(spawnX, spawnY, spawnZ);
				double centerX = spawnX + 0.5;
				double centerZ = spawnZ + 0.5;
				PlayerEntity nearestPlayer = world.getClosestPlayer(centerX, spawnY, centerZ, -1.0, false);

				if (nearestPlayer == null) {
					continue;
				}

				double squaredDist = nearestPlayer.squaredDistanceTo(centerX, spawnY, centerZ);
				if (!isAcceptableSpawnPosition(world, chunk, mutable, squaredDist)) {
					continue;
				}

				if (spawnEntry == null) {
					Optional<SpawnSettings.SpawnEntry> optional = pickRandomSpawnEntry(
						world, structureAccessor, chunkGenerator, group, world.random, mutable
					);
					if (optional.isEmpty()) {
						break;
					}

					spawnEntry = optional.get();
					groupSize = spawnEntry.minGroupSize() + world.random.nextInt(
						1 + spawnEntry.maxGroupSize() - spawnEntry.minGroupSize()
					);
				}

				if (!canSpawn(world, group, structureAccessor, chunkGenerator, spawnEntry, mutable, squaredDist)
					|| !checker.test(spawnEntry.type(), mutable, chunk)
				) {
					continue;
				}

				MobEntity mobEntity = createMob(world, spawnEntry.type());
				if (mobEntity == null) {
					return;
				}

				mobEntity.refreshPositionAndAngles(centerX, spawnY, centerZ, world.random.nextFloat() * 360.0F, 0.0F);
				if (!isValidSpawn(world, mobEntity, squaredDist)) {
					continue;
				}

				entityData = mobEntity.initialize(
					world,
					world.getLocalDifficulty(mobEntity.getBlockPos()),
					SpawnReason.NATURAL,
					entityData
				);
				totalSpawned++;
				groupSpawned++;
				world.spawnEntityAndPassengers(mobEntity);
				runner.run(mobEntity, chunk);

				if (totalSpawned >= mobEntity.getLimitPerChunk()) {
					return;
				}

				if (mobEntity.spawnsTooManyForEachTry(groupSpawned)) {
					break;
				}
			}
		}
	}

	private static boolean isAcceptableSpawnPosition(
		ServerWorld world,
		Chunk chunk,
		BlockPos.Mutable pos,
		double squaredDistance
	) {
		// Слишком близко к игроку — спавн запрещён
		if (squaredDistance <= 576.0) {
			return false;
		}

		WorldProperties.SpawnPoint spawnPoint = world.getSpawnPoint();
		if (spawnPoint.getDimension() == world.getRegistryKey()
			&& spawnPoint.getPos().isWithinDistance(
				new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
				MIN_SPAWN_DISTANCE
			)
		) {
			return false;
		}

		ChunkPos chunkPos = new ChunkPos(pos);
		return Objects.equals(chunkPos, chunk.getPos()) || world.canSpawnEntitiesAt(chunkPos);
	}

	private static boolean canSpawn(
		ServerWorld world,
		SpawnGroup group,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		SpawnSettings.SpawnEntry spawnEntry,
		BlockPos.Mutable pos,
		double squaredDistance
	) {
		EntityType<?> entityType = spawnEntry.type();

		if (entityType.getSpawnGroup() == SpawnGroup.MISC) {
			return false;
		}

		double despawnRangeSq = (double) entityType.getSpawnGroup().getImmediateDespawnRange()
			* entityType.getSpawnGroup().getImmediateDespawnRange();
		if (!entityType.isSpawnableFarFromPlayer() && squaredDistance > despawnRangeSq) {
			return false;
		}

		if (!entityType.isSummonable()
			|| !containsSpawnEntry(world, structureAccessor, chunkGenerator, group, spawnEntry, pos)
		) {
			return false;
		}

		if (!SpawnRestriction.isSpawnPosAllowed(entityType, world, pos)) {
			return false;
		}

		if (!SpawnRestriction.canSpawn(entityType, world, SpawnReason.NATURAL, pos, world.random)) {
			return false;
		}

		return world.isSpaceEmpty(entityType.getSpawnBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}

	private static @Nullable MobEntity createMob(ServerWorld world, EntityType<?> type) {
		try {
			if (type.create(world, SpawnReason.NATURAL) instanceof MobEntity mobEntity) {
				return mobEntity;
			}

			LOGGER.warn("Can't spawn entity of type: {}", Registries.ENTITY_TYPE.getId(type));
		} catch (Exception exception) {
			LOGGER.warn("Failed to create mob", exception);
		}

		return null;
	}

	private static boolean isValidSpawn(ServerWorld world, MobEntity entity, double squaredDistance) {
		double despawnRangeSq = (double) entity.getType().getSpawnGroup().getImmediateDespawnRange()
			* entity.getType().getSpawnGroup().getImmediateDespawnRange();

		if (squaredDistance > despawnRangeSq && entity.canImmediatelyDespawn(squaredDistance)) {
			return false;
		}

		return entity.canSpawn(world, SpawnReason.NATURAL) && entity.canSpawn(world);
	}

	private static Optional<SpawnSettings.SpawnEntry> pickRandomSpawnEntry(
		ServerWorld world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		SpawnGroup spawnGroup,
		Random random,
		BlockPos pos
	) {
		RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
		if (spawnGroup == SpawnGroup.WATER_AMBIENT
			&& biomeEntry.isIn(BiomeTags.REDUCE_WATER_AMBIENT_SPAWNS)
			&& random.nextFloat() < WATER_AMBIENT_SPAWN_REDUCTION_CHANCE
		) {
			return Optional.empty();
		}

		return getSpawnEntries(world, structureAccessor, chunkGenerator, spawnGroup, pos, biomeEntry)
			.getOrEmpty(random);
	}

	private static boolean containsSpawnEntry(
		ServerWorld world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		SpawnGroup spawnGroup,
		SpawnSettings.SpawnEntry spawnEntry,
		BlockPos pos
	) {
		return getSpawnEntries(world, structureAccessor, chunkGenerator, spawnGroup, pos, null)
			.contains(spawnEntry);
	}

	private static Pool<SpawnSettings.SpawnEntry> getSpawnEntries(
		ServerWorld world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		SpawnGroup spawnGroup,
		BlockPos pos,
		@Nullable RegistryEntry<Biome> biomeEntry
	) {
		return shouldUseNetherFortressSpawns(pos, world, spawnGroup, structureAccessor)
			? NetherFortressStructure.MONSTER_SPAWNS
			: chunkGenerator.getEntitySpawnList(
				biomeEntry != null ? biomeEntry : world.getBiome(pos),
				structureAccessor,
				spawnGroup,
				pos
			);
	}

	/**
	 * Проверяет, должны ли в данной позиции использоваться таблицы спавна
	 * крепости Нижнего мира вместо биомных.
	 * <p>
	 * Условие: группа MONSTER, под позицией — блок {@code NETHER_BRICKS},
	 * и позиция находится внутри структуры крепости.
	 *
	 * @param pos               проверяемая позиция
	 * @param world             серверный мир
	 * @param spawnGroup        группа спавна
	 * @param structureAccessor доступ к структурам мира
	 * @return {@code true} если нужно использовать спавн крепости
	 */
	public static boolean shouldUseNetherFortressSpawns(
		BlockPos pos,
		ServerWorld world,
		SpawnGroup spawnGroup,
		StructureAccessor structureAccessor
	) {
		if (spawnGroup != SpawnGroup.MONSTER || !world.getBlockState(pos.down()).isOf(Blocks.NETHER_BRICKS)) {
			return false;
		}

		Structure structure = structureAccessor
			.getRegistryManager()
			.getOrThrow(RegistryKeys.STRUCTURE)
			.get(StructureKeys.FORTRESS);

		return structure != null && structureAccessor.getStructureAt(pos, structure).hasChildren();
	}

	private static BlockPos getRandomPosInChunkSection(World world, WorldChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		int x = chunkPos.getStartX() + world.random.nextInt(16);
		int z = chunkPos.getStartZ() + world.random.nextInt(16);
		int surfaceY = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) + 1;
		int y = MathHelper.nextBetween(world.random, world.getBottomY(), surfaceY);
		return new BlockPos(x, y, z);
	}

	/**
	 * Проверяет, является ли блок допустимым для спавна моба внутри него.
	 * <p>
	 * Блок не должен быть полным кубом, не должен излучать редстоун-сигнал,
	 * не должен содержать жидкость и не должен быть в теге {@link BlockTags#PREVENT_MOB_SPAWNING_INSIDE}.
	 *
	 * @param blockView  доступ к блокам
	 * @param pos        позиция блока
	 * @param state      состояние блока
	 * @param fluidState состояние жидкости
	 * @param entityType тип сущности
	 * @return {@code true} если блок допустим для спавна
	 */
	public static boolean isClearForSpawn(
		BlockView blockView,
		BlockPos pos,
		BlockState state,
		FluidState fluidState,
		EntityType<?> entityType
	) {
		if (state.isFullCube(blockView, pos)) {
			return false;
		}

		if (state.emitsRedstonePower()) {
			return false;
		}

		if (!fluidState.isEmpty()) {
			return false;
		}

		if (state.isIn(BlockTags.PREVENT_MOB_SPAWNING_INSIDE)) {
			return false;
		}

		return !entityType.isInvalidSpawn(state);
	}

	/**
	 * Заселяет чанк мирными мобами при генерации мира.
	 * <p>
	 * Вызывается один раз при создании чанка. Использует вероятность спавна
	 * из {@link SpawnSettings#getCreatureSpawnProbability()} и случайно размещает
	 * группы мобов типа {@link SpawnGroup#CREATURE}.
	 *
	 * @param world      доступ к серверному миру
	 * @param biomeEntry биом чанка
	 * @param chunkPos   позиция чанка
	 * @param random     генератор случайных чисел
	 */
	public static void populateEntities(
		ServerWorldAccess world,
		RegistryEntry<Biome> biomeEntry,
		ChunkPos chunkPos,
		Random random
	) {
		SpawnSettings spawnSettings = biomeEntry.value().getSpawnSettings();
		Pool<SpawnSettings.SpawnEntry> pool = spawnSettings.getSpawnEntries(SpawnGroup.CREATURE);

		if (pool.isEmpty() || !world.toServerWorld().getGameRules().getValue(GameRules.DO_MOB_SPAWNING)) {
			return;
		}

		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();

		while (random.nextFloat() < spawnSettings.getCreatureSpawnProbability()) {
			Optional<SpawnSettings.SpawnEntry> optional = pool.getOrEmpty(random);
			if (optional.isEmpty()) {
				continue;
			}

			SpawnSettings.SpawnEntry spawnEntry = optional.get();
			int groupSize = spawnEntry.minGroupSize() + random.nextInt(
				1 + spawnEntry.maxGroupSize() - spawnEntry.minGroupSize()
			);
			EntityData entityData = null;
			int spawnX = startX + random.nextInt(16);
			int spawnZ = startZ + random.nextInt(16);
			int originX = spawnX;
			int originZ = spawnZ;

			for (int groupIndex = 0; groupIndex < groupSize; groupIndex++) {
				boolean spawned = false;

				for (int retryCount = 0; !spawned && retryCount < 4; retryCount++) {
					BlockPos blockPos = getEntitySpawnPos(world, spawnEntry.type(), spawnX, spawnZ);

					if (!spawnEntry.type().isSummonable()
						|| !SpawnRestriction.isSpawnPosAllowed(spawnEntry.type(), world, blockPos)
					) {
						break;
					}

					float entityWidth = spawnEntry.type().getWidth();
					double clampedX = MathHelper.clamp((double) spawnX, (double) startX + entityWidth, startX + 16.0 - entityWidth);
					double clampedZ = MathHelper.clamp((double) spawnZ, (double) startZ + entityWidth, startZ + 16.0 - entityWidth);

					if (world.isSpaceEmpty(spawnEntry.type().getSpawnBox(clampedX, blockPos.getY(), clampedZ))
						|| !SpawnRestriction.canSpawn(
							spawnEntry.type(),
							world,
							SpawnReason.CHUNK_GENERATION,
							BlockPos.ofFloored(clampedX, blockPos.getY(), clampedZ),
							world.getRandom()
						)
					) {
						spawnX += random.nextInt(5) - random.nextInt(5);
						spawnZ += random.nextInt(5) - random.nextInt(5);
						while (spawnX < startX || spawnX >= startX + 16 || spawnZ < startZ || spawnZ >= startZ + 16) {
							spawnX = originX + random.nextInt(5) - random.nextInt(5);
							spawnZ = originZ + random.nextInt(5) - random.nextInt(5);
						}
						continue;
					}

					Entity entity;
					try {
						entity = spawnEntry.type().create(world.toServerWorld(), SpawnReason.NATURAL);
					} catch (Exception exception) {
						LOGGER.warn("Failed to create mob", exception);
						continue;
					}

					if (entity == null) {
						continue;
					}

					entity.refreshPositionAndAngles(clampedX, blockPos.getY(), clampedZ, random.nextFloat() * 360.0F, 0.0F);

					if (entity instanceof MobEntity mobEntity
						&& mobEntity.canSpawn(world, SpawnReason.CHUNK_GENERATION)
						&& mobEntity.canSpawn(world)
					) {
						entityData = mobEntity.initialize(
							world,
							world.getLocalDifficulty(mobEntity.getBlockPos()),
							SpawnReason.CHUNK_GENERATION,
							entityData
						);
						world.spawnEntityAndPassengers(mobEntity);
						spawned = true;
					}
				}
			}
		}
	}

	private static BlockPos getEntitySpawnPos(WorldView world, EntityType<?> entityType, int x, int z) {
		int topY = world.getTopY(SpawnRestriction.getHeightmapType(entityType), x, z);
		BlockPos.Mutable mutable = new BlockPos.Mutable(x, topY, z);

		if (world.getDimension().hasCeiling()) {
			do {
				mutable.move(Direction.DOWN);
			} while (!world.getBlockState(mutable).isAir());

			do {
				mutable.move(Direction.DOWN);
			} while (world.getBlockState(mutable).isAir() && mutable.getY() > world.getBottomY());
		}

		return SpawnRestriction.getLocation(entityType).adjustPosition(world, mutable.toImmutable());
	}

	/**
	 * Функциональный интерфейс для дополнительной проверки перед спавном моба.
	 */
	@FunctionalInterface
	public interface Checker {

		boolean test(EntityType<?> type, BlockPos pos, Chunk chunk);
	}

	/**
	 * Функциональный интерфейс для получения чанка по позиции.
	 */
	@FunctionalInterface
	public interface ChunkSource {

		void query(long pos, Consumer<WorldChunk> chunkConsumer);
	}

	/**
	 * Хранит статистику текущего цикла спавна: количество мобов по группам,
	 * гравитационное поле плотности и ограничитель плотности на игрока.
	 */
	public static class Info {

		private final int spawningChunkCount;
		private final Object2IntOpenHashMap<SpawnGroup> groupToCount;
		private final GravityField densityField;
		private final Object2IntMap<SpawnGroup> groupToCountView;
		private final SpawnDensityCapper densityCapper;
		private @Nullable BlockPos cachedPos;
		private @Nullable EntityType<?> cachedEntityType;
		private double cachedDensityMass;

		Info(
			int spawningChunkCount,
			Object2IntOpenHashMap<SpawnGroup> groupToCount,
			GravityField densityField,
			SpawnDensityCapper densityCapper
		) {
			this.spawningChunkCount = spawningChunkCount;
			this.groupToCount = groupToCount;
			this.densityField = densityField;
			this.densityCapper = densityCapper;
			this.groupToCountView = Object2IntMaps.unmodifiable(groupToCount);
		}

		private boolean test(EntityType<?> type, BlockPos pos, Chunk chunk) {
			cachedPos = pos;
			cachedEntityType = type;
			SpawnSettings.SpawnDensity spawnDensity = SpawnHelper.getBiomeDirectly(pos, chunk)
				.getSpawnSettings()
				.getSpawnDensity(type);

			if (spawnDensity == null) {
				cachedDensityMass = 0.0;
				return true;
			}

			double mass = spawnDensity.mass();
			cachedDensityMass = mass;
			double density = densityField.calculate(pos, mass);
			return density <= spawnDensity.gravityLimit();
		}

		private void run(MobEntity entity, Chunk chunk) {
			EntityType<?> entityType = entity.getType();
			BlockPos blockPos = entity.getBlockPos();
			double mass;

			if (blockPos.equals(cachedPos) && entityType == cachedEntityType) {
				mass = cachedDensityMass;
			} else {
				SpawnSettings.SpawnDensity spawnDensity = SpawnHelper.getBiomeDirectly(blockPos, chunk)
					.getSpawnSettings()
					.getSpawnDensity(entityType);
				mass = spawnDensity != null ? spawnDensity.mass() : 0.0;
			}

			densityField.addPoint(blockPos, mass);
			SpawnGroup spawnGroup = entityType.getSpawnGroup();
			groupToCount.addTo(spawnGroup, 1);
			densityCapper.increaseDensity(new ChunkPos(blockPos), spawnGroup);
		}

		public int getSpawningChunkCount() {
			return spawningChunkCount;
		}

		public Object2IntMap<SpawnGroup> getGroupToCount() {
			return groupToCountView;
		}

		boolean isBelowCap(SpawnGroup group) {
			int cap = group.getCapacity() * spawningChunkCount / SpawnHelper.CHUNK_AREA;
			return groupToCount.getInt(group) < cap;
		}

		boolean canSpawn(SpawnGroup group, ChunkPos chunkPos) {
			return densityCapper.canSpawn(group, chunkPos) || SharedConstants.IGNORE_LOCAL_MOB_CAP;
		}
	}

	/**
		* Функциональный интерфейс для выполнения действий после успешного спавна моба.
		*/
	@FunctionalInterface
	public interface Runner {

		void run(MobEntity entity, Chunk chunk);
	}
}
