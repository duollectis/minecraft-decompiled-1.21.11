package net.minecraft.world;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.BlockParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.block.ChainRestrictedNeighborUpdater;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.tick.TickManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Базовый абстрактный класс игрового мира Minecraft.
 * Реализует общую логику для клиентского и серверного миров:
 * управление блоками, сущностями, звуком, частицами, взрывами и тиками.
 */
public abstract class World implements WorldAccess, AutoCloseable, AttachmentTarget {

	public static final Codec<RegistryKey<World>> CODEC = RegistryKey.createCodec(RegistryKeys.WORLD);

	public static final RegistryKey<World> OVERWORLD =
		RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla("overworld"));
	public static final RegistryKey<World> NETHER =
		RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla("the_nether"));
	public static final RegistryKey<World> END =
		RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla("the_end"));

	public static final int HORIZONTAL_LIMIT = 30000000;
	public static final int MAX_UPDATE_DEPTH = 512;
	public static final int GENERATION_AREA_CHUNK_RADIUS = 32;
	public static final int MAX_LIGHT_LEVEL = 15;
	public static final int MAX_Y = 20000000;
	public static final int MIN_Y = -20000000;

	/** Маска для извлечения флагов обновления соседей (убирает биты NOTIFY и NO_RERENDER). */
	private static final int NEIGHBOR_UPDATE_FLAGS_MASK = -34;

	/** Порог яркости неба, ниже которого считается ночь. */
	private static final int NIGHT_DARKNESS_THRESHOLD = 4;

	/** Порог градиента грозы для определения активной грозы. */
	private static final double THUNDER_ACTIVE_THRESHOLD = 0.9;

	/** Порог градиента дождя для определения активного дождя. */
	private static final double RAIN_ACTIVE_THRESHOLD = 0.2;

	private static final Pool<BlockParticleEffect> EXPLOSION_BLOCK_PARTICLES = Pool.<BlockParticleEffect>builder()
		.add(new BlockParticleEffect(ParticleTypes.POOF, 0.5F, 1.0F))
		.add(new BlockParticleEffect(ParticleTypes.SMOKE, 1.0F, 1.0F))
		.build();

	protected final List<BlockEntityTickInvoker> blockEntityTickers = Lists.newArrayList();
	protected final ChainRestrictedNeighborUpdater neighborUpdater;
	private final List<BlockEntityTickInvoker> pendingBlockEntityTickers = Lists.newArrayList();
	private boolean iteratingTickingBlockEntities;
	private final Thread thread;
	private final boolean debugWorld;
	private int ambientDarkness;
	protected int lcgBlockSeed = Random.create().nextInt();
	protected final int lcgBlockSeedIncrement = 1013904223;
	protected float lastRainGradient;
	protected float rainGradient;
	protected float lastThunderGradient;
	protected float thunderGradient;
	public final Random random = Random.create();
	@Deprecated
	private final Random threadSafeRandom = Random.createThreadSafe();
	private final RegistryEntry<DimensionType> dimensionEntry;
	protected final MutableWorldProperties properties;
	private final boolean isClient;
	private final BiomeAccess biomeAccess;
	private final RegistryKey<World> registryKey;
	private final DynamicRegistryManager registryManager;
	private final DamageSources damageSources;
	private final PalettesFactory palettesFactory;
	private long tickOrder;

	protected World(
		MutableWorldProperties properties,
		RegistryKey<World> registryRef,
		DynamicRegistryManager registryManager,
		RegistryEntry<DimensionType> dimensionEntry,
		boolean isClient,
		boolean debugWorld,
		long seed,
		int maxChainedNeighborUpdates
	) {
		this.properties = properties;
		this.dimensionEntry = dimensionEntry;
		this.registryKey = registryRef;
		this.isClient = isClient;
		this.thread = Thread.currentThread();
		this.biomeAccess = new BiomeAccess(this, seed);
		this.debugWorld = debugWorld;
		this.neighborUpdater = new ChainRestrictedNeighborUpdater(this, maxChainedNeighborUpdates);
		this.registryManager = registryManager;
		this.palettesFactory = PalettesFactory.fromRegistryManager(registryManager);
		this.damageSources = new DamageSources(registryManager);
	}

	@Override
	public boolean isClient() {
		return isClient;
	}

	@Override
	public @Nullable MinecraftServer getServer() {
		return null;
	}

	public boolean isInBuildLimit(BlockPos pos) {
		return !isOutOfHeightLimit(pos) && isValidHorizontally(pos);
	}

	public boolean isInGenerationArea(BlockPos pos) {
		return !isOutOfHeightLimit(pos) && isChunkPosInGenerationArea(pos);
	}

	public static boolean isValid(BlockPos pos) {
		return !isInvalidVertically(pos.getY()) && isValidHorizontally(pos);
	}

	private static boolean isValidHorizontally(BlockPos pos) {
		return pos.getX() >= -HORIZONTAL_LIMIT
			&& pos.getZ() >= -HORIZONTAL_LIMIT
			&& pos.getX() < HORIZONTAL_LIMIT
			&& pos.getZ() < HORIZONTAL_LIMIT;
	}

	private static boolean isChunkPosInGenerationArea(BlockPos pos) {
		int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
		int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
		return ChunkPos.isWithinGenerationArea(chunkX, chunkZ);
	}

	private static boolean isInvalidVertically(int y) {
		return y < -MAX_Y || y >= MAX_Y;
	}

	public WorldChunk getWorldChunk(BlockPos pos) {
		return getChunk(
			ChunkSectionPos.getSectionCoord(pos.getX()),
			ChunkSectionPos.getSectionCoord(pos.getZ())
		);
	}

	public WorldChunk getChunk(int chunkX, int chunkZ) {
		return (WorldChunk) getChunk(chunkX, chunkZ, ChunkStatus.FULL);
	}

	@Override
	public @Nullable Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
		Chunk chunk = getChunkManager().getChunk(chunkX, chunkZ, leastStatus, create);
		if (chunk == null && create) {
			throw new IllegalStateException("Should always be able to create a chunk!");
		}

		return chunk;
	}

	@Override
	public boolean setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		return setBlockState(pos, state, flags, MAX_UPDATE_DEPTH);
	}

	/**
	 * Устанавливает состояние блока в мире с полным контролем над глубиной обновлений.
	 * Обрабатывает уведомление слушателей, обновление соседей и подготовку состояния.
	 * Флаги управляют поведением: бит 1 — обновить соседей, бит 2 — уведомить клиент,
	 * бит 4 — не отправлять клиенту, бит 16 — не обновлять соседей рекурсивно.
	 */
	@Override
	public boolean setBlockState(
		BlockPos pos,
		BlockState state,
		@Block.SetBlockStateFlag int flags,
		int maxUpdateDepth
	) {
		if (!isInGenerationArea(pos)) {
			return false;
		}

		if (!isClient() && isDebugWorld()) {
			return false;
		}

		WorldChunk worldChunk = getWorldChunk(pos);
		Block block = state.getBlock();
		BlockState previousState = worldChunk.setBlockState(pos, state, flags);
		if (previousState == null) {
			return false;
		}

		BlockState currentState = getBlockState(pos);
		if (currentState == state) {
			if (previousState != currentState) {
				scheduleBlockRerenderIfNeeded(pos, previousState, currentState);
			}

			boolean shouldNotifyClient = (flags & 2) != 0
				&& (!isClient() || (flags & 4) == 0)
				&& (isClient() || worldChunk.getLevelType() != null
					&& worldChunk.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING));

			if (shouldNotifyClient) {
				updateListeners(pos, previousState, state, flags);
			}

			if ((flags & 1) != 0) {
				updateNeighbors(pos, previousState.getBlock());
				if (!isClient() && state.hasComparatorOutput()) {
					updateComparators(pos, block);
				}
			}

			if ((flags & 16) == 0 && maxUpdateDepth > 0) {
				int neighborFlags = flags & NEIGHBOR_UPDATE_FLAGS_MASK;
				previousState.prepare(this, pos, neighborFlags, maxUpdateDepth - 1);
				state.updateNeighbors(this, pos, neighborFlags, maxUpdateDepth - 1);
				state.prepare(this, pos, neighborFlags, maxUpdateDepth - 1);
			}

			onBlockStateChanged(pos, previousState, currentState);
		}

		return true;
	}

	/** Вызывается после изменения состояния блока. Переопределяется в подклассах для реакции на изменения. */
	public void onBlockStateChanged(BlockPos pos, BlockState oldState, BlockState newState) {
	}

	@Override
	public boolean removeBlock(BlockPos pos, boolean move) {
		FluidState fluidState = getFluidState(pos);
		return setBlockState(pos, fluidState.getBlockState(), 3 | (move ? Block.MOVED : 0));
	}

	@Override
	public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
		BlockState blockState = getBlockState(pos);
		if (blockState.isAir()) {
			return false;
		}

		FluidState fluidState = getFluidState(pos);
		if (!(blockState.getBlock() instanceof AbstractFireBlock)) {
			syncWorldEvent(2001, pos, Block.getRawIdFromState(blockState));
		}

		if (drop) {
			BlockEntity blockEntity = blockState.hasBlockEntity() ? getBlockEntity(pos) : null;
			Block.dropStacks(blockState, this, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
		}

		boolean broken = setBlockState(pos, fluidState.getBlockState(), 3, maxUpdateDepth);
		if (broken) {
			emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(breakingEntity, blockState));
		}

		return broken;
	}

	/** Воспроизводит визуальные частицы разрушения блока. Реализуется на клиенте. */
	public void addBlockBreakParticles(BlockPos pos, BlockState state) {
	}

	public boolean setBlockState(BlockPos pos, BlockState state) {
		return setBlockState(pos, state, 3);
	}

	public abstract void updateListeners(
		BlockPos pos,
		BlockState oldState,
		BlockState newState,
		@Block.SetBlockStateFlag int flags
	);

	/** Планирует перерендер блока, если визуальное состояние изменилось. Реализуется на клиенте. */
	public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
	}

	/** Обновляет всех соседей блока без исключений. */
	public void updateNeighborsAlways(BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation) {
	}

	public void updateNeighborsExcept(
		BlockPos pos,
		Block sourceBlock,
		Direction direction,
		@Nullable WireOrientation orientation
	) {
	}

	/** Уведомляет конкретного соседа об изменении блока. */
	public void updateNeighbor(BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation) {
	}

	public void updateNeighbor(
		BlockState state,
		BlockPos pos,
		Block sourceBlock,
		@Nullable WireOrientation orientation,
		boolean notify
	) {
	}

	@Override
	public void replaceWithStateForNeighborUpdate(
		Direction direction,
		BlockPos pos,
		BlockPos neighborPos,
		BlockState neighborState,
		@Block.SetBlockStateFlag int flags,
		int maxUpdateDepth
	) {
		neighborUpdater.replaceWithStateForNeighborUpdate(
			direction,
			neighborState,
			pos,
			neighborPos,
			flags,
			maxUpdateDepth
		);
	}

	/**
	 * Возвращает Y-координату верхнего блока по заданной карте высот.
	 * Для координат вне горизонтального лимита возвращает уровень моря + 1.
	 * Для незагруженных чанков возвращает нижнюю границу мира.
	 */
	@Override
	public int getTopY(Heightmap.Type heightmap, int x, int z) {
		if (x < -HORIZONTAL_LIMIT || z < -HORIZONTAL_LIMIT || x >= HORIZONTAL_LIMIT || z >= HORIZONTAL_LIMIT) {
			return getSeaLevel() + 1;
		}

		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);
		if (!isChunkLoaded(chunkX, chunkZ)) {
			return getBottomY();
		}

		return getChunk(chunkX, chunkZ)
			.sampleHeightmap(heightmap, x & MAX_LIGHT_LEVEL, z & MAX_LIGHT_LEVEL) + 1;
	}

	@Override
	public LightingProvider getLightingProvider() {
		return getChunkManager().getLightingProvider();
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if (!isInGenerationArea(pos)) {
			return Blocks.VOID_AIR.getDefaultState();
		}

		WorldChunk worldChunk = getChunk(
			ChunkSectionPos.getSectionCoord(pos.getX()),
			ChunkSectionPos.getSectionCoord(pos.getZ())
		);
		return worldChunk.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		if (!isInGenerationArea(pos)) {
			return Fluids.EMPTY.getDefaultState();
		}

		return getWorldChunk(pos).getFluidState(pos);
	}

	public boolean isDay() {
		return !getDimension().hasFixedTime() && ambientDarkness < NIGHT_DARKNESS_THRESHOLD;
	}

	public boolean isNight() {
		return !getDimension().hasFixedTime() && !isDay();
	}

	@Override
	public void playSound(
		@Nullable Entity source,
		BlockPos pos,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch
	) {
		playSound(source, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, category, volume, pitch);
	}

	public abstract void playSound(
		@Nullable Entity source,
		double x,
		double y,
		double z,
		RegistryEntry<SoundEvent> sound,
		SoundCategory category,
		float volume,
		float pitch,
		long seed
	);

	public void playSound(
		@Nullable Entity source,
		double x,
		double y,
		double z,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch,
		long seed
	) {
		playSound(source, x, y, z, Registries.SOUND_EVENT.getEntry(sound), category, volume, pitch, seed);
	}

	public abstract void playSoundFromEntity(
		@Nullable Entity source,
		Entity entity,
		RegistryEntry<SoundEvent> sound,
		SoundCategory category,
		float volume,
		float pitch,
		long seed
	);

	public void playSound(
		@Nullable Entity source,
		double x,
		double y,
		double z,
		SoundEvent sound,
		SoundCategory category
	) {
		playSound(source, x, y, z, sound, category, 1.0F, 1.0F);
	}

	public void playSound(
		@Nullable Entity source,
		double x,
		double y,
		double z,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch
	) {
		playSound(source, x, y, z, sound, category, volume, pitch, threadSafeRandom.nextLong());
	}

	public void playSound(
		@Nullable Entity source,
		double x,
		double y,
		double z,
		RegistryEntry<SoundEvent> sound,
		SoundCategory category,
		float volume,
		float pitch
	) {
		playSound(source, x, y, z, sound, category, volume, pitch, threadSafeRandom.nextLong());
	}

	public void playSoundFromEntity(
		@Nullable Entity source,
		Entity entity,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch
	) {
		playSoundFromEntity(
			source,
			entity,
			Registries.SOUND_EVENT.getEntry(sound),
			category,
			volume,
			pitch,
			threadSafeRandom.nextLong()
		);
	}

	public void playSoundAtBlockCenterClient(
		BlockPos pos,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch,
		boolean useDistance
	) {
		playSoundClient(
			pos.getX() + 0.5,
			pos.getY() + 0.5,
			pos.getZ() + 0.5,
			sound,
			category,
			volume,
			pitch,
			useDistance
		);
	}

	public void playSoundFromEntityClient(
		Entity entity,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch
	) {
	}

	public void playSoundClient(
		double x,
		double y,
		double z,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch,
		boolean useDistance
	) {
	}

	public void playSoundClient(SoundEvent sound, SoundCategory category, float volume, float pitch) {
	}

	@Override
	public void addParticleClient(
		ParticleEffect parameters,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ
	) {
	}

	public void addParticleClient(
		ParticleEffect parameters,
		boolean force,
		boolean canSpawnOnMinimal,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ
	) {
	}

	public void addImportantParticleClient(
		ParticleEffect parameters,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ
	) {
	}

	public void addImportantParticleClient(
		ParticleEffect parameters,
		boolean force,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ
	) {
	}

	/**
	 * Добавляет тикер блок-сущности в очередь обработки.
	 * Если в данный момент идёт итерация по тикерам — помещает в отложенную очередь.
	 */
	public void addBlockEntityTicker(BlockEntityTickInvoker ticker) {
		(iteratingTickingBlockEntities ? pendingBlockEntityTickers : blockEntityTickers).add(ticker);
	}

	/**
	 * Выполняет тик всех зарегистрированных блок-сущностей.
	 * Удаляет помеченные на удаление тикеры и переносит отложенные в основной список.
	 */
	public void tickBlockEntities() {
		iteratingTickingBlockEntities = true;
		if (!pendingBlockEntityTickers.isEmpty()) {
			blockEntityTickers.addAll(pendingBlockEntityTickers);
			pendingBlockEntityTickers.clear();
		}

		boolean shouldTick = getTickManager().shouldTick();
		Iterator<BlockEntityTickInvoker> iterator = blockEntityTickers.iterator();

		while (iterator.hasNext()) {
			BlockEntityTickInvoker ticker = iterator.next();
			if (ticker.isRemoved()) {
				iterator.remove();
			} else if (shouldTick && shouldTickBlockPos(ticker.getPos())) {
				ticker.tick();
			}
		}

		iteratingTickingBlockEntities = false;
	}

	/**
	 * Выполняет тик сущности через переданный consumer с перехватом ошибок.
	 * При исключении формирует подробный CrashReport с данными сущности.
	 */
	public <T extends Entity> void tickEntity(Consumer<T> tickConsumer, T entity) {
		try {
			tickConsumer.accept(entity);
		} catch (Throwable throwable) {
			CrashReport crashReport = CrashReport.create(throwable, "Ticking entity");
			CrashReportSection section = crashReport.addElement("Entity being ticked");
			entity.populateCrashReport(section);
			throw new CrashException(crashReport);
		}
	}

	/** Определяет, нужно ли обновлять сущность после её смерти. По умолчанию — да. */
	public boolean shouldUpdatePostDeath(Entity entity) {
		return true;
	}

	/** Определяет, нужно ли тикать блоки в чанке по его long-позиции. По умолчанию — да. */
	public boolean shouldTickBlocksInChunk(long chunkPos) {
		return true;
	}

	public boolean shouldTickBlockPos(BlockPos pos) {
		return shouldTickBlocksInChunk(ChunkPos.toLong(pos));
	}

	public void createExplosion(
		@Nullable Entity entity,
		double x,
		double y,
		double z,
		float power,
		World.ExplosionSourceType explosionSourceType
	) {
		createExplosion(
			entity,
			Explosion.createDamageSource(this, entity),
			null,
			x, y, z,
			power,
			false,
			explosionSourceType,
			ParticleTypes.EXPLOSION,
			ParticleTypes.EXPLOSION_EMITTER,
			EXPLOSION_BLOCK_PARTICLES,
			SoundEvents.ENTITY_GENERIC_EXPLODE
		);
	}

	public void createExplosion(
		@Nullable Entity entity,
		double x,
		double y,
		double z,
		float power,
		boolean createFire,
		World.ExplosionSourceType explosionSourceType
	) {
		createExplosion(
			entity,
			Explosion.createDamageSource(this, entity),
			null,
			x, y, z,
			power,
			createFire,
			explosionSourceType,
			ParticleTypes.EXPLOSION,
			ParticleTypes.EXPLOSION_EMITTER,
			EXPLOSION_BLOCK_PARTICLES,
			SoundEvents.ENTITY_GENERIC_EXPLODE
		);
	}

	public void createExplosion(
		@Nullable Entity entity,
		@Nullable DamageSource damageSource,
		@Nullable ExplosionBehavior behavior,
		Vec3d pos,
		float power,
		boolean createFire,
		World.ExplosionSourceType explosionSourceType
	) {
		createExplosion(
			entity,
			damageSource,
			behavior,
			pos.getX(), pos.getY(), pos.getZ(),
			power,
			createFire,
			explosionSourceType,
			ParticleTypes.EXPLOSION,
			ParticleTypes.EXPLOSION_EMITTER,
			EXPLOSION_BLOCK_PARTICLES,
			SoundEvents.ENTITY_GENERIC_EXPLODE
		);
	}

	public void createExplosion(
		@Nullable Entity entity,
		@Nullable DamageSource damageSource,
		@Nullable ExplosionBehavior behavior,
		double x,
		double y,
		double z,
		float power,
		boolean createFire,
		World.ExplosionSourceType explosionSourceType
	) {
		createExplosion(
			entity,
			damageSource,
			behavior,
			x, y, z,
			power,
			createFire,
			explosionSourceType,
			ParticleTypes.EXPLOSION,
			ParticleTypes.EXPLOSION_EMITTER,
			EXPLOSION_BLOCK_PARTICLES,
			SoundEvents.ENTITY_GENERIC_EXPLODE
		);
	}

	public abstract void createExplosion(
		@Nullable Entity entity,
		@Nullable DamageSource damageSource,
		@Nullable ExplosionBehavior behavior,
		double x,
		double y,
		double z,
		float power,
		boolean createFire,
		World.ExplosionSourceType explosionSourceType,
		ParticleEffect smallParticle,
		ParticleEffect largeParticle,
		Pool<BlockParticleEffect> blockParticles,
		RegistryEntry<SoundEvent> soundEvent
	);

	public abstract String asString();

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		if (!isInGenerationArea(pos)) {
			return null;
		}

		if (!isClient() && Thread.currentThread() != thread) {
			return null;
		}

		return getWorldChunk(pos).getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
	}

	public void addBlockEntity(BlockEntity blockEntity) {
		BlockPos blockPos = blockEntity.getPos();
		if (isInGenerationArea(blockPos)) {
			getWorldChunk(blockPos).addBlockEntity(blockEntity);
		}
	}

	public void removeBlockEntity(BlockPos pos) {
		if (isInGenerationArea(pos)) {
			getWorldChunk(pos).removeBlockEntity(pos);
		}
	}

	public boolean isPosLoaded(BlockPos pos) {
		return isInGenerationArea(pos)
			&& getChunkManager().isChunkLoaded(
				ChunkSectionPos.getSectionCoord(pos.getX()),
				ChunkSectionPos.getSectionCoord(pos.getZ())
			);
	}

	public boolean isDirectionSolid(BlockPos pos, Entity entity, Direction direction) {
		if (!isInGenerationArea(pos)) {
			return false;
		}

		Chunk chunk = getChunk(
			ChunkSectionPos.getSectionCoord(pos.getX()),
			ChunkSectionPos.getSectionCoord(pos.getZ()),
			ChunkStatus.FULL,
			false
		);
		return chunk != null && chunk.getBlockState(pos).isSolidSurface(this, pos, entity, direction);
	}

	public boolean isTopSolid(BlockPos pos, Entity entity) {
		return isDirectionSolid(pos, entity, Direction.UP);
	}

	/**
	 * Пересчитывает уровень окружающей темноты на основе атрибута освещённости неба.
	 * Используется для определения дня/ночи и поведения мобов.
	 */
	public void calculateAmbientDarkness() {
		ambientDarkness = (int) (15.0F - getEnvironmentAttributes()
			.getAttributeValue(EnvironmentAttributes.SKY_LIGHT_LEVEL_GAMEPLAY));
	}

	public void setMobSpawnOptions(boolean spawnMonsters) {
		getChunkManager().setMobSpawnOptions(spawnMonsters);
	}

	public abstract void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint);

	public abstract WorldProperties.SpawnPoint getSpawnPoint();

	/**
	 * Гарантирует, что точка спавна находится внутри границы мира.
	 * Если точка за границей — перемещает её к центру границы на поверхность.
	 */
	public WorldProperties.SpawnPoint ensureWithinBorder(WorldProperties.SpawnPoint spawnPoint) {
		WorldBorder worldBorder = getWorldBorder();
		if (worldBorder.contains(spawnPoint.getPos())) {
			return spawnPoint;
		}

		BlockPos centerPos = getTopPosition(
			Heightmap.Type.MOTION_BLOCKING,
			BlockPos.ofFloored(worldBorder.getCenterX(), 0.0, worldBorder.getCenterZ())
		);
		return WorldProperties.SpawnPoint.create(
			spawnPoint.getDimension(),
			centerPos,
			spawnPoint.yaw(),
			spawnPoint.pitch()
		);
	}

	/** Инициализирует градиенты погоды при загрузке мира, если дождь/гроза уже активны. */
	protected void initWeatherGradients() {
		if (properties.isRaining()) {
			rainGradient = 1.0F;
			if (properties.isThundering()) {
				thunderGradient = 1.0F;
			}
		}
	}

	@Override
	public void close() throws IOException {
		getChunkManager().close();
	}

	@Override
	public @Nullable BlockView getChunkAsView(int chunkX, int chunkZ) {
		return getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
	}

	@Override
	public List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
		Profilers.get().visit("getEntities");
		List<Entity> result = Lists.newArrayList();
		getEntityLookup().forEachIntersects(
			box, entity -> {
				if (entity != except && predicate.test(entity)) {
					result.add(entity);
				}
			}
		);

		for (EnderDragonPart part : getEnderDragonParts()) {
			if (part != except
				&& part.owner != except
				&& predicate.test(part)
				&& box.intersects(part.getBoundingBox())
			) {
				result.add(part);
			}
		}

		return result;
	}

	@Override
	public <T extends Entity> List<T> getEntitiesByType(
		TypeFilter<Entity, T> filter,
		Box box,
		Predicate<? super T> predicate
	) {
		List<T> result = Lists.newArrayList();
		collectEntitiesByType(filter, box, predicate, result);
		return result;
	}

	public <T extends Entity> void collectEntitiesByType(
		TypeFilter<Entity, T> filter,
		Box box,
		Predicate<? super T> predicate,
		List<? super T> result
	) {
		collectEntitiesByType(filter, box, predicate, result, Integer.MAX_VALUE);
	}

	public <T extends Entity> void collectEntitiesByType(
		TypeFilter<Entity, T> filter,
		Box box,
		Predicate<? super T> predicate,
		List<? super T> result,
		int limit
	) {
		Profilers.get().visit("getEntities");
		getEntityLookup().forEachIntersects(
			filter, box, entity -> {
				if (predicate.test(entity)) {
					result.add(entity);
					if (result.size() >= limit) {
						return LazyIterationConsumer.NextIteration.ABORT;
					}
				}

				if (entity instanceof EnderDragonEntity dragon) {
					for (EnderDragonPart part : dragon.getBodyParts()) {
						T castPart = filter.downcast(part);
						if (castPart != null && predicate.test(castPart)) {
							result.add(castPart);
							if (result.size() >= limit) {
								return LazyIterationConsumer.NextIteration.ABORT;
							}
						}
					}
				}

				return LazyIterationConsumer.NextIteration.CONTINUE;
			}
		);
	}

	public <T extends Entity> boolean hasEntities(
		TypeFilter<Entity, T> filter,
		Box box,
		Predicate<? super T> predicate
	) {
		Profilers.get().visit("hasEntities");
		MutableBoolean found = new MutableBoolean();
		getEntityLookup().forEachIntersects(
			filter, box, entity -> {
				if (predicate.test(entity)) {
					found.setTrue();
					return LazyIterationConsumer.NextIteration.ABORT;
				}

				if (entity instanceof EnderDragonEntity dragon) {
					for (EnderDragonPart part : dragon.getBodyParts()) {
						T castPart = filter.downcast(part);
						if (castPart != null && predicate.test(castPart)) {
							found.setTrue();
							return LazyIterationConsumer.NextIteration.ABORT;
						}
					}
				}

				return LazyIterationConsumer.NextIteration.CONTINUE;
			}
		);
		return found.isTrue();
	}

	public List<Entity> getCrammedEntities(Entity entity, Box box) {
		return getOtherEntities(entity, box, EntityPredicates.canBePushedBy(entity));
	}

	public abstract @Nullable Entity getEntityById(int id);

	public @Nullable Entity getEntity(UUID uuid) {
		return getEntityLookup().get(uuid);
	}

	public @Nullable Entity getEntityAnyDimension(UUID uuid) {
		return getEntity(uuid);
	}

	public @Nullable PlayerEntity getPlayerAnyDimension(UUID uuid) {
		return getPlayerByUuid(uuid);
	}

	public abstract Collection<EnderDragonPart> getEnderDragonParts();

	public void markDirty(BlockPos pos) {
		if (isChunkLoaded(pos)) {
			getWorldChunk(pos).markNeedsSaving();
		}
	}

	/** Вызывается при загрузке блок-сущности из NBT. Переопределяется в подклассах при необходимости. */
	public void loadBlockEntity(BlockEntity blockEntity) {
	}

	public long getTimeOfDay() {
		return properties.getTimeOfDay();
	}

	/** Проверяет, может ли сущность изменять блоки в данной позиции. По умолчанию — да. */
	public boolean canEntityModifyAt(Entity entity, BlockPos pos) {
		return true;
	}

	/** Отправляет статусный байт сущности всем наблюдателям. Реализуется на сервере. */
	public void sendEntityStatus(Entity entity, byte status) {
	}

	/** Отправляет информацию об уроне сущности. Реализуется на сервере. */
	public void sendEntityDamage(Entity entity, DamageSource damageSource) {
	}

	public void addSyncedBlockEvent(BlockPos pos, Block block, int type, int data) {
		getBlockState(pos).onSyncedBlockEvent(this, pos, type, data);
	}

	@Override
	public WorldProperties getLevelProperties() {
		return properties;
	}

	public abstract TickManager getTickManager();

	public float getThunderGradient(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastThunderGradient, thunderGradient) * getRainGradient(tickProgress);
	}

	public void setThunderGradient(float thunderGradient) {
		float clamped = MathHelper.clamp(thunderGradient, 0.0F, 1.0F);
		lastThunderGradient = clamped;
		this.thunderGradient = clamped;
	}

	public float getRainGradient(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastRainGradient, rainGradient);
	}

	public void setRainGradient(float rainGradient) {
		float clamped = MathHelper.clamp(rainGradient, 0.0F, 1.0F);
		lastRainGradient = clamped;
		this.rainGradient = clamped;
	}

	/** Проверяет, может ли в данном измерении быть погода (дождь/гроза). */
	public boolean canHaveWeather() {
		return getDimension().hasSkyLight() && !getDimension().hasCeiling() && getRegistryKey() != END;
	}

	public boolean isThundering() {
		return canHaveWeather() && getThunderGradient(1.0F) > THUNDER_ACTIVE_THRESHOLD;
	}

	public boolean isRaining() {
		return canHaveWeather() && getRainGradient(1.0F) > RAIN_ACTIVE_THRESHOLD;
	}

	public boolean hasRain(BlockPos pos) {
		return getPrecipitation(pos) == Biome.Precipitation.RAIN;
	}

	/**
	 * Определяет тип осадков в данной позиции с учётом погоды, видимости неба и биома.
	 * Возвращает NONE если нет дождя, небо закрыто или блок выше перекрывает позицию.
	 */
	public Biome.Precipitation getPrecipitation(BlockPos pos) {
		if (!isRaining()) {
			return Biome.Precipitation.NONE;
		}

		if (!isSkyVisible(pos)) {
			return Biome.Precipitation.NONE;
		}

		if (getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY() > pos.getY()) {
			return Biome.Precipitation.NONE;
		}

		Biome biome = getBiome(pos).value();
		return biome.getPrecipitation(pos, getSeaLevel());
	}

	public abstract @Nullable MapState getMapState(MapIdComponent id);

	/** Синхронизирует глобальное игровое событие всем игрокам. Реализуется на сервере. */
	public void syncGlobalEvent(int eventId, BlockPos pos, int data) {
	}

	/**
	 * Добавляет подробную информацию о мире в отчёт об ошибке.
	 * Включает список игроков, статистику чанков и данные измерения.
	 */
	public CrashReportSection addDetailsToCrashReport(CrashReport report) {
		CrashReportSection section = report.addElement("Affected level", 1);
		section.add(
			"All players", () -> {
				List<? extends PlayerEntity> players = getPlayers();
				return players.size() + " total; " + players
					.stream()
					.map(PlayerEntity::asString)
					.collect(Collectors.joining(", "));
			}
		);
		section.add("Chunk stats", getChunkManager()::getDebugString);
		section.add("Level dimension", () -> getRegistryKey().getValue().toString());

		try {
			properties.populateCrashReport(section, this);
		} catch (Throwable throwable) {
			section.add("Level Data Unobtainable", throwable);
		}

		return section;
	}

	public abstract void setBlockBreakingInfo(int entityId, BlockPos pos, int progress);

	public void addFireworkParticle(
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ,
		List<FireworkExplosionComponent> explosions
	) {
	}

	public abstract Scoreboard getScoreboard();

	/**
	 * Обновляет компараторы, соседствующие с данной позицией.
	 * Проверяет как прямых соседей, так и соседей через сплошной блок.
	 */
	public void updateComparators(BlockPos pos, Block block) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighborPos = pos.offset(direction);
			if (!isChunkLoaded(neighborPos)) {
				continue;
			}

			BlockState neighborState = getBlockState(neighborPos);
			if (neighborState.isOf(Blocks.COMPARATOR)) {
				updateNeighbor(neighborState, neighborPos, block, null, false);
			} else if (neighborState.isSolidBlock(this, neighborPos)) {
				BlockPos behindPos = neighborPos.offset(direction);
				BlockState behindState = getBlockState(behindPos);
				if (behindState.isOf(Blocks.COMPARATOR)) {
					updateNeighbor(behindState, behindPos, block, null, false);
				}
			}
		}
	}

	@Override
	public int getAmbientDarkness() {
		return ambientDarkness;
	}

	public void setLightningTicksLeft(int lightningTicksLeft) {
	}

	public void sendPacket(Packet<?> packet) {
		throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
	}

	@Override
	public DimensionType getDimension() {
		return dimensionEntry.value();
	}

	public RegistryEntry<DimensionType> getDimensionEntry() {
		return dimensionEntry;
	}

	public RegistryKey<World> getRegistryKey() {
		return registryKey;
	}

	@Override
	public Random getRandom() {
		return random;
	}

	@Override
	public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
		return state.test(getBlockState(pos));
	}

	@Override
	public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
		return state.test(getFluidState(pos));
	}

	public abstract RecipeManager getRecipeManager();

	/**
	 * Возвращает случайную позицию внутри чанка с использованием LCG-генератора.
	 * Используется для случайных тиков блоков.
	 */
	public BlockPos getRandomPosInChunk(int x, int y, int z, int yMask) {
		lcgBlockSeed = lcgBlockSeed * 3 + lcgBlockSeedIncrement;
		int seed = lcgBlockSeed >> 2;
		return new BlockPos(
			x + (seed & MAX_LIGHT_LEVEL),
			y + (seed >> 16 & yMask),
			z + (seed >> 8 & MAX_LIGHT_LEVEL)
		);
	}

	public boolean isSavingDisabled() {
		return false;
	}

	@Override
	public BiomeAccess getBiomeAccess() {
		return biomeAccess;
	}

	public final boolean isDebugWorld() {
		return debugWorld;
	}

	protected abstract EntityLookup<Entity> getEntityLookup();

	@Override
	public long getTickOrder() {
		return tickOrder++;
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return registryManager;
	}

	public DamageSources getDamageSources() {
		return damageSources;
	}

	public abstract WorldEnvironmentAttributeAccess getEnvironmentAttributes();

	public abstract BrewingRecipeRegistry getBrewingRecipeRegistry();

	public abstract FuelRegistry getFuelRegistry();

	public int getBlockColor(BlockPos pos) {
		return 0;
	}

	public PalettesFactory getPalettesFactory() {
		return palettesFactory;
	}

	/** Тип источника взрыва, определяющий поведение разрушения блоков и урона. */
	public enum ExplosionSourceType implements StringIdentifiable {
		NONE("none"),
		BLOCK("block"),
		MOB("mob"),
		TNT("tnt"),
		TRIGGER("trigger");

		public static final Codec<World.ExplosionSourceType> CODEC =
			StringIdentifiable.createCodec(World.ExplosionSourceType::values);

		private final String id;

		ExplosionSourceType(final String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return id;
		}
	}
}
