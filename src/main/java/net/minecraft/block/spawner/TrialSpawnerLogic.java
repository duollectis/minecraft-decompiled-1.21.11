package net.minecraft.block.spawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TrialSpawnerBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.enums.TrialSpawnerState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Основная логика спаунера испытаний: управляет циклом жизни спаунера
 * (активация, спаун мобов, перезарядка), обнаружением игроков,
 * зловещим режимом и выбросом лута по завершении волны.
 */
public final class TrialSpawnerLogic {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int OMINOUS_ACTIVATION_DELAY_TICKS = 40;
	private static final int DEFAULT_COOLDOWN_LENGTH = 36000;
	private static final int DEFAULT_ENTITY_DETECTION_RANGE = 14;
	private static final int MAX_ENTITY_DISTANCE = 47;
	private static final int MAX_ENTITY_DISTANCE_SQUARED = MathHelper.square(47);
	private static final float SOUND_RATE_PER_TICK = 0.02F;
	private final TrialSpawnerData data = new TrialSpawnerData();
	private TrialSpawnerLogic.FullConfig fullConfig;
	private final TrialSpawnerLogic.TrialSpawner trialSpawner;
	private EntityDetector entityDetector;
	private final EntityDetector.Selector entitySelector;
	private boolean forceActivate;
	private boolean ominous;

	public TrialSpawnerLogic(
			TrialSpawnerLogic.FullConfig fullConfig,
			TrialSpawnerLogic.TrialSpawner trialSpawner,
			EntityDetector entityDetector,
			EntityDetector.Selector entitySelector
	) {
		this.fullConfig = fullConfig;
		this.trialSpawner = trialSpawner;
		this.entityDetector = entityDetector;
		this.entitySelector = entitySelector;
	}

	public TrialSpawnerConfig getConfig() {
		return this.ominous ? this.fullConfig.ominous().value() : this.fullConfig.normal.value();
	}

	public TrialSpawnerConfig getNormalConfig() {
		return this.fullConfig.normal.value();
	}

	public TrialSpawnerConfig getOminousConfig() {
		return this.fullConfig.ominous.value();
	}

	public void readData(ReadView view) {
		view.<TrialSpawnerData.Packed>read(TrialSpawnerData.Packed.CODEC).ifPresent(this.data::unpack);
		this.fullConfig =
				view
						.<TrialSpawnerLogic.FullConfig>read(TrialSpawnerLogic.FullConfig.CODEC)
						.orElse(TrialSpawnerLogic.FullConfig.DEFAULT);
	}

	public void writeData(WriteView view) {
		view.put(TrialSpawnerData.Packed.CODEC, this.data.pack());
		view.put(TrialSpawnerLogic.FullConfig.CODEC, this.fullConfig);
	}

	public void setOminous(ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, world.getBlockState(pos).with(TrialSpawnerBlock.OMINOUS, true), 3);
		world.syncWorldEvent(3020, pos, 1);
		this.ominous = true;
		this.data.resetAndClearMobs(this, world);
	}

	public void setNotOminous(ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, world.getBlockState(pos).with(TrialSpawnerBlock.OMINOUS, false), 3);
		this.ominous = false;
	}

	public boolean isOminous() {
		return this.ominous;
	}

	public int getCooldownLength() {
		return this.fullConfig.targetCooldownLength;
	}

	public int getDetectionRadius() {
		return this.fullConfig.requiredPlayerRange;
	}

	public TrialSpawnerState getSpawnerState() {
		return this.trialSpawner.getSpawnerState();
	}

	public TrialSpawnerData getData() {
		return this.data;
	}

	public void setSpawnerState(World world, TrialSpawnerState spawnerState) {
		this.trialSpawner.setSpawnerState(world, spawnerState);
	}

	public void updateListeners() {
		this.trialSpawner.updateListeners();
	}

	public EntityDetector getEntityDetector() {
		return this.entityDetector;
	}

	public EntityDetector.Selector getEntitySelector() {
		return this.entitySelector;
	}

	public boolean canActivate(ServerWorld world) {
		if (!world.getGameRules().getValue(GameRules.SPAWNER_BLOCKS_WORK)) {
			return false;
		}
		else if (this.forceActivate) {
			return true;
		}
		else {
			return world.getDifficulty() == Difficulty.PEACEFUL ? false : world
			                                                              .getGameRules()
			                                                              .getValue(GameRules.DO_MOB_SPAWNING);
		}
	}

	public Optional<UUID> trySpawnMob(ServerWorld world, BlockPos pos) {
		Random random = world.getRandom();
		MobSpawnerEntry mobSpawnerEntry = this.data.getSpawnData(this, world.getRandom());

		Optional<UUID> spawnedEntityUuid;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(() -> "spawner@" + pos, LOGGER)) {
			ReadView readView = NbtReadView.create(logging, world.getRegistryManager(), mobSpawnerEntry.entity());
			Optional<EntityType<?>> optional = EntityType.fromData(readView);
			if (optional.isEmpty()) {
				return Optional.empty();
			}

			Vec3d vec3d = readView.<Vec3d>read("Pos", Vec3d.CODEC)
			                      .orElseGet(
					                      () -> {
						                      TrialSpawnerConfig trialSpawnerConfig = this.getConfig();
						                      return new Vec3d(
								                      pos.getX() + (random.nextDouble() - random.nextDouble())
										                      * trialSpawnerConfig.spawnRange() + 0.5,
								                      pos.getY() + random.nextInt(3) - 1,
								                      pos.getZ() + (random.nextDouble() - random.nextDouble())
										                      * trialSpawnerConfig.spawnRange() + 0.5
						                      );
					                      }
			                      );
			if (!world.isSpaceEmpty(optional.get().getSpawnBox(vec3d.x, vec3d.y, vec3d.z))) {
				return Optional.empty();
			}

			if (!hasLineOfSight(world, pos.toCenterPos(), vec3d)) {
				return Optional.empty();
			}

			BlockPos blockPos = BlockPos.ofFloored(vec3d);
			if (!SpawnRestriction.canSpawn(
					optional.get(),
					world,
					SpawnReason.TRIAL_SPAWNER,
					blockPos,
					world.getRandom()
			)) {
				return Optional.empty();
			}

			if (mobSpawnerEntry.getCustomSpawnRules().isPresent()) {
				MobSpawnerEntry.CustomSpawnRules customSpawnRules = mobSpawnerEntry.getCustomSpawnRules().get();
				if (!customSpawnRules.canSpawn(blockPos, world)) {
					return Optional.empty();
				}
			}

			Entity entity = EntityType.loadEntityWithPassengers(
					readView, world, SpawnReason.TRIAL_SPAWNER, entityx -> {
						entityx.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, random.nextFloat() * 360.0F, 0.0F);
						return entityx;
					}
			);
			if (entity == null) {
				return Optional.empty();
			}

			if (entity instanceof MobEntity mobEntity) {
				if (!mobEntity.canSpawn(world)) {
					return Optional.empty();
				}

				boolean
						bl =
						mobSpawnerEntry.getNbt().getSize() == 1 && mobSpawnerEntry.getNbt().getString("id").isPresent();
				if (bl) {
					mobEntity.initialize(
							world,
							world.getLocalDifficulty(mobEntity.getBlockPos()),
							SpawnReason.TRIAL_SPAWNER,
							null
					);
				}

				mobEntity.setPersistent();
				mobSpawnerEntry.getEquipment().ifPresent(mobEntity::setEquipmentFromTable);
			}

			if (!world.spawnNewEntityAndPassengers(entity)) {
				return Optional.empty();
			}

			TrialSpawnerLogic.Type type = this.ominous ? TrialSpawnerLogic.Type.OMINOUS : TrialSpawnerLogic.Type.NORMAL;
			world.syncWorldEvent(3011, pos, type.getIndex());
			world.syncWorldEvent(3012, blockPos, type.getIndex());
			world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
			spawnedEntityUuid = Optional.of(entity.getUuid());
		}

		return spawnedEntityUuid;
	}

	public void ejectLootTable(ServerWorld world, BlockPos pos, RegistryKey<LootTable> lootTable) {
		LootTable lootTable2 = world.getServer().getReloadableRegistries().getLootTable(lootTable);
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world).build(LootContextTypes.EMPTY);
		ObjectArrayList<ItemStack> objectArrayList = lootTable2.generateLoot(lootWorldContext);
		if (!objectArrayList.isEmpty()) {
			for (ItemStack itemStack : objectArrayList) {
				ItemDispenserBehavior.spawnItem(
						world,
						itemStack,
						2,
						Direction.UP,
						Vec3d.ofBottomCenter(pos).offset(Direction.UP, 1.2)
				);
			}

			world.syncWorldEvent(3014, pos, 0);
		}
	}

	public void tickClient(World world, BlockPos pos, boolean ominous) {
		TrialSpawnerState trialSpawnerState = this.getSpawnerState();
		trialSpawnerState.emitParticles(world, pos, ominous);
		if (trialSpawnerState.doesDisplayRotate()) {
			double d = Math.max(0L, this.data.nextMobSpawnsAt - world.getTime());
			this.data.lastDisplayEntityRotation = this.data.displayEntityRotation;
			this.data.displayEntityRotation =
					(this.data.displayEntityRotation + trialSpawnerState.getDisplayRotationSpeed() / (d + 200.0))
							% 360.0;
		}

		if (trialSpawnerState.playsSound()) {
			Random random = world.getRandom();
			if (random.nextFloat() <= SOUND_RATE_PER_TICK) {
				SoundEvent
						soundEvent =
						ominous ? SoundEvents.BLOCK_TRIAL_SPAWNER_AMBIENT_OMINOUS
						        : SoundEvents.BLOCK_TRIAL_SPAWNER_AMBIENT;
				world.playSoundAtBlockCenterClient(
						pos,
						soundEvent,
						SoundCategory.BLOCKS,
						random.nextFloat() * 0.25F + 0.75F,
						random.nextFloat() + 0.5F,
						false
				);
			}
		}
	}

	public void tickServer(ServerWorld world, BlockPos pos, boolean ominous) {
		this.ominous = ominous;
		TrialSpawnerState trialSpawnerState = this.getSpawnerState();
		if (this.data.spawnedMobsAlive.removeIf(uuid -> shouldRemoveMobFromData(world, pos, uuid))) {
			this.data.nextMobSpawnsAt = world.getTime() + this.getConfig().ticksBetweenSpawn();
		}

		TrialSpawnerState trialSpawnerState2 = trialSpawnerState.tick(pos, this, world);
		if (trialSpawnerState2 != trialSpawnerState) {
			this.setSpawnerState(world, trialSpawnerState2);
		}
	}

	private static boolean shouldRemoveMobFromData(ServerWorld world, BlockPos pos, UUID uuid) {
		Entity entity = world.getEntity(uuid);
		return entity == null
				|| !entity.isAlive()
				|| !entity.getEntityWorld().getRegistryKey().equals(world.getRegistryKey())
				|| entity.getBlockPos().getSquaredDistance(pos) > MAX_ENTITY_DISTANCE_SQUARED;
	}

	private static boolean hasLineOfSight(World world, Vec3d spawnerPos, Vec3d spawnPos) {
		BlockHitResult blockHitResult = world.raycast(
				new RaycastContext(
						spawnPos,
						spawnerPos,
						RaycastContext.ShapeType.VISUAL,
						RaycastContext.FluidHandling.NONE,
						ShapeContext.absent()
				)
		);
		return blockHitResult.getBlockPos().equals(BlockPos.ofFloored(spawnerPos))
				|| blockHitResult.getType() == HitResult.Type.MISS;
	}

	public static void addMobSpawnParticles(World world, BlockPos pos, Random random, SimpleParticleType particle) {
		for (int i = 0; i < 20; i++) {
			double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
			double e = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
			double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
			world.addParticleClient(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0);
			world.addParticleClient(particle, d, e, f, 0.0, 0.0, 0.0);
		}
	}

	public static void addTrialOmenParticles(World world, BlockPos pos, Random random) {
		for (int i = 0; i < 20; i++) {
			double d = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
			double e = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
			double f = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
			double g = random.nextGaussian() * 0.02;
			double h = random.nextGaussian() * 0.02;
			double j = random.nextGaussian() * 0.02;
			world.addParticleClient(ParticleTypes.TRIAL_OMEN, d, e, f, g, h, j);
			world.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, d, e, f, g, h, j);
		}
	}

	public static void addDetectionParticles(
			World world,
			BlockPos pos,
			Random random,
			int playerCount,
			ParticleEffect particle
	) {
		for (int i = 0; i < 30 + Math.min(playerCount, 10) * 5; i++) {
			double d = (2.0F * random.nextFloat() - 1.0F) * 0.65;
			double e = (2.0F * random.nextFloat() - 1.0F) * 0.65;
			double f = pos.getX() + 0.5 + d;
			double g = pos.getY() + 0.1 + random.nextFloat() * 0.8;
			double h = pos.getZ() + 0.5 + e;
			world.addParticleClient(particle, f, g, h, 0.0, 0.0, 0.0);
		}
	}

	public static void addEjectItemParticles(World world, BlockPos pos, Random random) {
		for (int i = 0; i < 20; i++) {
			double d = pos.getX() + 0.4 + random.nextDouble() * 0.2;
			double e = pos.getY() + 0.4 + random.nextDouble() * 0.2;
			double f = pos.getZ() + 0.4 + random.nextDouble() * 0.2;
			double g = random.nextGaussian() * 0.02;
			double h = random.nextGaussian() * 0.02;
			double j = random.nextGaussian() * 0.02;
			world.addParticleClient(ParticleTypes.SMALL_FLAME, d, e, f, g, h, j * 0.25);
			world.addParticleClient(ParticleTypes.SMOKE, d, e, f, g, h, j);
		}
	}

	public void setEntityType(EntityType<?> entityType, World world) {
		this.data.reset();
		this.fullConfig = this.fullConfig.withEntityType(entityType);
		this.setSpawnerState(world, TrialSpawnerState.INACTIVE);
	}

	@Deprecated(forRemoval = true)
	@VisibleForTesting
	public void setEntityDetector(EntityDetector detector) {
		this.entityDetector = detector;
	}

	/**
	 * Принудительно активирует спаунер вне зависимости от игровых правил.
	 * Используется исключительно в тестах — не вызывать в продакшн-коде.
	 */
	@Deprecated(forRemoval = true)
	@VisibleForTesting
	public void forceActivate() {
		this.forceActivate = true;
	}

	/**
	 * Полная конфигурация спаунера испытаний, объединяющая обычный и зловещий
	 * варианты {@link TrialSpawnerConfig}, а также параметры обнаружения игроков.
	 */
	public record FullConfig(
			RegistryEntry<TrialSpawnerConfig> normal,
			RegistryEntry<TrialSpawnerConfig> ominous,
			int targetCooldownLength,
			int requiredPlayerRange
	) {

		public static final MapCodec<TrialSpawnerLogic.FullConfig> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    TrialSpawnerConfig.ENTRY_CODEC
								                    .optionalFieldOf("normal_config", RegistryEntry.of(TrialSpawnerConfig.DEFAULT))
								                    .forGetter(TrialSpawnerLogic.FullConfig::normal),
						                    TrialSpawnerConfig.ENTRY_CODEC
								                    .optionalFieldOf("ominous_config", RegistryEntry.of(TrialSpawnerConfig.DEFAULT))
								                    .forGetter(TrialSpawnerLogic.FullConfig::ominous),
						                    Codecs.NON_NEGATIVE_INT
								                    .optionalFieldOf("target_cooldown_length", DEFAULT_COOLDOWN_LENGTH)
								                    .forGetter(TrialSpawnerLogic.FullConfig::targetCooldownLength),
						                    Codec
								                    .intRange(1, 128)
								                    .optionalFieldOf("required_player_range", DEFAULT_ENTITY_DETECTION_RANGE)
								                    .forGetter(TrialSpawnerLogic.FullConfig::requiredPlayerRange)
				                    )
				                    .apply(instance, TrialSpawnerLogic.FullConfig::new)
		);
		public static final TrialSpawnerLogic.FullConfig DEFAULT = new TrialSpawnerLogic.FullConfig(
				RegistryEntry.of(TrialSpawnerConfig.DEFAULT), RegistryEntry.of(TrialSpawnerConfig.DEFAULT), DEFAULT_COOLDOWN_LENGTH, DEFAULT_ENTITY_DETECTION_RANGE
		);

		public TrialSpawnerLogic.FullConfig withEntityType(EntityType<?> entityType) {
			return new TrialSpawnerLogic.FullConfig(
					RegistryEntry.of(this.normal.value().withSpawnPotential(entityType)),
					RegistryEntry.of(this.ominous.value().withSpawnPotential(entityType)),
					this.targetCooldownLength,
					this.requiredPlayerRange
			);
		}
	}

	/**
	 * Контракт блока, реализующего логику спаунера испытаний.
	 * Позволяет {@link TrialSpawnerLogic} управлять состоянием блока
	 * без прямой зависимости от конкретной реализации блок-сущности.
	 */
	public interface TrialSpawner {

		void setSpawnerState(World world, TrialSpawnerState spawnerState);

		TrialSpawnerState getSpawnerState();

		void updateListeners();
	}

	/**
	 * Тип спаунера испытаний, определяющий визуальные эффекты частиц
	 * для обычного и зловещего режимов.
	 */
	public enum Type {
		NORMAL(ParticleTypes.FLAME),
		OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

		public final SimpleParticleType particle;

		private Type(final SimpleParticleType particle) {
			this.particle = particle;
		}

		public static TrialSpawnerLogic.Type fromIndex(int index) {
			TrialSpawnerLogic.Type[] types = values();
			return index <= types.length && index >= 0 ? types[index] : NORMAL;
		}

		public int getIndex() {
			return this.ordinal();
		}
	}
}
