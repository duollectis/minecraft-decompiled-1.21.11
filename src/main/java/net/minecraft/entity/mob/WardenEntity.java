package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.WardenAngerManager;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.SonicBoomTask;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Страж (Warden) — слепой подземный босс, реагирующий на вибрации и звуки.
 * Управляет системой гнева через {@link WardenAngerManager}, слушает игровые события
 * через {@link Vibrations.VibrationListener} и применяет эффект темноты к ближайшим игрокам.
 */
public class WardenEntity extends HostileEntity implements Vibrations {

	private static final int VIBRATION_COOLDOWN_TICKS = 40;
	private static final int SONIC_BOOM_COOLDOWN_TICKS = 200;
	private static final int MAX_HEALTH = 500;
	private static final float MOVEMENT_SPEED = 0.3F;
	private static final float KNOCKBACK_RESISTANCE = 1.0F;
	private static final float ATTACK_KNOCKBACK = 1.5F;
	private static final int ATTACK_DAMAGE = 30;
	private static final int FOLLOW_RANGE = 24;
	private static final TrackedData<Integer>
			ANGER =
			DataTracker.registerData(WardenEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int DARKNESS_EFFECT_MIN_MULTIPLIER = 200;
	private static final int DARKNESS_EFFECT_DURATION = 260;
	private static final int ANGER_UPDATE_INTERVAL_TICKS = 20;
	private static final int DARKNESS_APPLY_INTERVAL_TICKS = 120;
	private static final int TOUCH_COOLDOWN_TICKS = 20;
	private static final int ANGRINESS_AMOUNT = 35;
	private static final int PROJECTILE_ANGER_AMOUNT = 10;
	private static final int DARKNESS_EFFECT_RANGE = 20;
	private static final int RECENT_PROJECTILE_MEMORY_TICKS = 100;
	private static final int HEARTBEAT_RESET_TICKS = 20;
	private static final int VIBRATION_ENTITY_DETECT_RANGE = 30;
	private static final float DIG_PARTICLE_DURATION_SECONDS = 4.5F;
	private static final float DIG_PARTICLE_SPREAD = 0.7F;
	private static final int SONIC_BOOM_ATTACK_COOLDOWN_TICKS = 30;
	private int tendrilAlpha;
	private int lastTendrilAlpha;
	private int heartbeatCooldown;
	private int lastHeartbeatCooldown;
	public AnimationState roaringAnimationState = new AnimationState();
	public AnimationState sniffingAnimationState = new AnimationState();
	public AnimationState emergingAnimationState = new AnimationState();
	public AnimationState diggingAnimationState = new AnimationState();
	public AnimationState attackingAnimationState = new AnimationState();
	public AnimationState chargingSonicBoomAnimationState = new AnimationState();
	private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler;
	private final Vibrations.Callback vibrationCallback;
	private Vibrations.ListenerData vibrationListenerData;
	WardenAngerManager angerManager = new WardenAngerManager(this::isValidTarget, Collections.emptyList());

	public WardenEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		vibrationCallback = new WardenEntity.VibrationCallback();
		vibrationListenerData = new Vibrations.ListenerData();
		gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));
		experiencePoints = 5;
		getNavigation().setCanSwim(true);
		setPathfindingPenalty(PathNodeType.UNPASSABLE_RAIL, 0.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 8.0F);
		setPathfindingPenalty(PathNodeType.POWDER_SNOW, 8.0F);
		setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		return new EntitySpawnS2CPacket(this, entityTrackerEntry, isInPose(EntityPose.EMERGING) ? 1 : 0);
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		if (packet.getEntityData() == 1) {
			setPose(EntityPose.EMERGING);
		}
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return super.canSpawn(world) && world.isSpaceEmpty(
				this,
				getType().getDimensions().getBoxAt(getEntityPos())
		);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.0F;
	}

	@Override
	public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
		if (isDiggingOrEmerging() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}

		return super.isInvulnerableTo(world, source);
	}

	/**
	 * Проверяет, находится ли страж в процессе закапывания или появления из земли.
	 * В этих позах страж неуязвим к большинству источников урона.
	 */
	boolean isDiggingOrEmerging() {
		return isInPose(EntityPose.DIGGING) || isInPose(EntityPose.EMERGING);
	}

	@Override
	protected boolean canStartRiding(Entity entity) {
		return false;
	}

	@Override
	public float getWeaponDisableBlockingForSeconds() {
		return 5.0F;
	}

	@Override
	protected float calculateNextStepSoundDistance() {
		return distanceTraveled + 0.55F;
	}

	public static DefaultAttributeContainer.Builder addAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, MAX_HEALTH)
		                    .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
		                    .add(EntityAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
		                    .add(EntityAttributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK)
		                    .add(EntityAttributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
		                    .add(EntityAttributes.FOLLOW_RANGE, FOLLOW_RANGE);
	}

	@Override
	public boolean occludeVibrationSignals() {
		return true;
	}

	@Override
	protected float getSoundVolume() {
		return 4.0F;
	}

	private static final int ATTACK_STATUS = 4;
	private static final int TENDRIL_VIBRATION_STATUS = 61;
	private static final int SONIC_BOOM_CHARGE_STATUS = 62;

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (isInPose(EntityPose.ROARING) || isDiggingOrEmerging()) {
			return null;
		}

		return getAngriness().getSound();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WARDEN_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_WARDEN_STEP, 10.0F, 1.0F);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		world.sendEntityStatus(this, (byte) ATTACK_STATUS);
		playSound(SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, 10.0F, getSoundPitch());
		SonicBoomTask.cooldown(this, VIBRATION_COOLDOWN_TICKS);
		return super.tryAttack(world, target);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ANGER, 0);
	}

	public int getAnger() {
		return dataTracker.get(ANGER);
	}

	private void updateAnger() {
		dataTracker.set(ANGER, getAngerAtTarget());
	}

	@Override
	public void tick() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			Vibrations.Ticker.tick(serverWorld, vibrationListenerData, vibrationCallback);
			if (isPersistent() || cannotDespawn()) {
				WardenBrain.resetDigCooldown(this);
			}
		}

		super.tick();
		if (!getEntityWorld().isClient()) {
			return;
		}

		if (age % getHeartRate() == 0) {
			heartbeatCooldown = PROJECTILE_ANGER_AMOUNT;
			if (!isSilent()) {
				getEntityWorld().playSoundClient(
						getX(),
						getY(),
						getZ(),
						SoundEvents.ENTITY_WARDEN_HEARTBEAT,
						getSoundCategory(),
						5.0F,
						getSoundPitch(),
						false
				);
			}
		}

		lastTendrilAlpha = tendrilAlpha;
		if (tendrilAlpha > 0) {
			tendrilAlpha--;
		}

		lastHeartbeatCooldown = heartbeatCooldown;
		if (heartbeatCooldown > 0) {
			heartbeatCooldown--;
		}

		switch (getPose()) {
			case EMERGING -> addDigParticles(emergingAnimationState);
			case DIGGING -> addDigParticles(diggingAnimationState);
			default -> {}
		}
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("wardenBrain");
		getBrain().tick(world, this);
		profiler.pop();
		super.mobTick(world);
		if ((age + getId()) % DARKNESS_APPLY_INTERVAL_TICKS == 0) {
			addDarknessToClosePlayers(world, getEntityPos(), this, DARKNESS_EFFECT_RANGE);
		}

		if (age % ANGER_UPDATE_INTERVAL_TICKS == 0) {
			angerManager.tick(world, this::isValidTarget);
			updateAnger();
		}

		WardenBrain.updateActivities(this);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == ATTACK_STATUS) {
			roaringAnimationState.stop();
			attackingAnimationState.start(age);
		} else if (status == TENDRIL_VIBRATION_STATUS) {
			tendrilAlpha = PROJECTILE_ANGER_AMOUNT;
		} else if (status == SONIC_BOOM_CHARGE_STATUS) {
			chargingSonicBoomAnimationState.start(age);
		} else {
			super.handleStatus(status);
		}
	}

	/**
	 * Вычисляет интервал сердцебиения в тиках: чем выше гнев, тем быстрее бьётся сердце.
	 * Диапазон: от {@code VIBRATION_COOLDOWN_TICKS} (спокойный) до {@code VIBRATION_COOLDOWN_TICKS - 30} (злой).
	 */
	private int getHeartRate() {
		float angerRatio = (float) getAnger() / Angriness.ANGRY.getThreshold();
		return VIBRATION_COOLDOWN_TICKS - MathHelper.floor(MathHelper.clamp(angerRatio, 0.0F, 1.0F) * 30.0F);
	}

	/**
	 * Возвращает интерполированную прозрачность щупалец для текущего кадра рендера.
	 * Значение нормализовано в диапазон [0.0, 1.0] делением на 10.
	 */
	public float getTendrilAlpha(float tickProgress) {
		return MathHelper.lerp(tickProgress, (float) lastTendrilAlpha, (float) tendrilAlpha) / 10.0F;
	}

	/**
	 * Возвращает интерполированную яркость свечения сердца для текущего кадра рендера.
	 * Значение нормализовано в диапазон [0.0, 1.0] делением на 10.
	 */
	public float getHeartAlpha(float tickProgress) {
		return MathHelper.lerp(tickProgress, (float) lastHeartbeatCooldown, (float) heartbeatCooldown) / 10.0F;
	}

	/**
	 * Спавнит частицы блока под стражем во время анимации закапывания/появления.
	 * Частицы генерируются только в первые {@code DIG_PARTICLE_DURATION_SECONDS} секунды анимации.
	 */
	private void addDigParticles(AnimationState animationState) {
		float animationMillis = (float) animationState.getTimeInMilliseconds(age);
		if (animationMillis >= DIG_PARTICLE_DURATION_SECONDS * 1000.0F) {
			return;
		}

		Random random = getRandom();
		BlockState blockState = getSteppingBlockState();
		if (blockState.getRenderType() == BlockRenderType.INVISIBLE) {
			return;
		}

		for (int particleIndex = 0; particleIndex < 30; particleIndex++) {
			double particleX = getX() + MathHelper.nextBetween(random, -DIG_PARTICLE_SPREAD, DIG_PARTICLE_SPREAD);
			double particleY = getY();
			double particleZ = getZ() + MathHelper.nextBetween(random, -DIG_PARTICLE_SPREAD, DIG_PARTICLE_SPREAD);
			getEntityWorld().addParticleClient(
					new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
					particleX,
					particleY,
					particleZ,
					0.0,
					0.0,
					0.0
			);
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (POSE.equals(data)) {
			switch (getPose()) {
				case EMERGING -> emergingAnimationState.start(age);
				case DIGGING -> diggingAnimationState.start(age);
				case ROARING -> roaringAnimationState.start(age);
				case SNIFFING -> sniffingAnimationState.start(age);
				default -> {}
			}
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public boolean isImmuneToExplosion(Explosion explosion) {
		return isDiggingOrEmerging();
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return WardenBrain.create(this, dynamic);
	}

	@Override
	public Brain<WardenEntity> getBrain() {
		return (Brain<WardenEntity>) super.getBrain();
	}

	@Override
	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			callback.accept(gameEventHandler, serverWorld);
		}
	}

	/**
	 * Проверяет, является ли сущность допустимой целью для гнева стража.
	 * Исключает творческих/наблюдателей, союзников, стойки для брони, других стражей и неуязвимых.
	 */
	@Contract("null->false")
	public boolean isValidTarget(@Nullable Entity entity) {
		return entity instanceof LivingEntity livingEntity
				&& getEntityWorld() == entity.getEntityWorld()
				&& EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity)
				&& !isTeammate(entity)
				&& livingEntity.getType() != EntityType.ARMOR_STAND
				&& livingEntity.getType() != EntityType.WARDEN
				&& !livingEntity.isInvulnerable()
				&& !livingEntity.isDead()
				&& getEntityWorld().getWorldBorder().contains(livingEntity.getBoundingBox());
	}

	/**
	 * Применяет эффект темноты ко всем игрокам в радиусе {@code range} блоков от позиции.
	 * Используется как периодическая аура стража и при появлении из земли.
	 */
	public static void addDarknessToClosePlayers(ServerWorld world, Vec3d pos, @Nullable Entity entity, int range) {
		StatusEffectInstance darknessEffect = new StatusEffectInstance(
				StatusEffects.DARKNESS,
				DARKNESS_EFFECT_DURATION,
				0,
				false,
				false
		);
		StatusEffectUtil.addEffectToPlayersWithinDistance(world, entity, pos, range, darknessEffect, DARKNESS_EFFECT_MIN_MULTIPLIER);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("anger", WardenAngerManager.createCodec(this::isValidTarget), angerManager);
		view.put("listener", Vibrations.ListenerData.CODEC, vibrationListenerData);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		angerManager = view.<WardenAngerManager>read("anger", WardenAngerManager.createCodec(this::isValidTarget))
		                   .orElseGet(() -> new WardenAngerManager(this::isValidTarget, Collections.emptyList()));
		updateAnger();
		vibrationListenerData = view
				.<Vibrations.ListenerData>read("listener", Vibrations.ListenerData.CODEC)
				.orElseGet(Vibrations.ListenerData::new);
	}

	/**
	 * Воспроизводит звук реакции на вибрацию в зависимости от текущего уровня гнева.
	 * Не воспроизводится, если страж уже рычит.
	 */
	private void playListeningSound() {
		if (isInPose(EntityPose.ROARING)) {
			return;
		}

		playSound(getAngriness().getListeningSound(), 10.0F, getSoundPitch());
	}

	/**
	 * Возвращает текущий уровень ярости стража относительно его основной цели.
	 */
	public Angriness getAngriness() {
		return Angriness.getForAnger(getAngerAtTarget());
	}

	private int getAngerAtTarget() {
		return angerManager.getAngerFor(getTarget());
	}

	/**
	 * Удаляет сущность из списка подозреваемых менеджера гнева.
	 */
	public void removeSuspect(Entity entity) {
		angerManager.removeSuspect(entity);
	}

	/**
	 * Увеличивает гнев стража к сущности на стандартное значение {@code ANGRINESS_AMOUNT} с воспроизведением звука.
	 */
	public void increaseAngerAt(@Nullable Entity entity) {
		increaseAngerAt(entity, ANGRINESS_AMOUNT, true);
	}

	/**
	 * Увеличивает гнев стража к сущности на заданное значение.
	 * Если цель сменилась с не-игрока на игрока и достигнут порог ярости — сбрасывает цель атаки из памяти мозга.
	 *
	 * @param listening воспроизводить ли звук реакции на вибрацию
	 */
	@VisibleForTesting
	public void increaseAngerAt(@Nullable Entity entity, int amount, boolean listening) {
		if (isAiDisabled() || !isValidTarget(entity)) {
			return;
		}

		WardenBrain.resetDigCooldown(this);
		boolean hadNonPlayerTarget = !(getTarget() instanceof PlayerEntity);
		int newAnger = angerManager.increaseAngerAt(entity, amount);

		if (entity instanceof PlayerEntity && hadNonPlayerTarget && Angriness.getForAnger(newAnger).isAngry()) {
			getBrain().forget(MemoryModuleType.ATTACK_TARGET);
		}

		if (listening) {
			playListeningSound();
		}
	}

	/**
	 * Возвращает главного подозреваемого, если страж достиг уровня ярости {@link Angriness#isAngry()}.
	 */
	public Optional<LivingEntity> getPrimeSuspect() {
		return getAngriness().isAngry() ? angerManager.getPrimeSuspect() : Optional.empty();
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return getTargetInBrain();
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		getBrain().remember(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
		if (spawnReason == SpawnReason.TRIGGERED) {
			setPose(EntityPose.EMERGING);
			getBrain().remember(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, WardenBrain.EMERGE_DURATION);
			playSound(SoundEvents.ENTITY_WARDEN_AGITATED, 5.0F, 1.0F);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean damaged = super.damage(world, source, amount);
		if (!isAiDisabled() && !isDiggingOrEmerging()) {
			Entity attacker = source.getAttacker();
			increaseAngerAt(attacker, Angriness.ANGRY.getThreshold() + ANGER_UPDATE_INTERVAL_TICKS, false);
			if (brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).isEmpty()
					&& attacker instanceof LivingEntity livingAttacker
					&& (source.isDirect() || isInRange(livingAttacker, 5.0))) {
				updateAttackTarget(livingAttacker);
			}
		}

		return damaged;
	}

	/**
	 * Устанавливает новую цель атаки в памяти мозга, сбрасывая цель рёва и кулдаун Sonic Boom.
	 */
	public void updateAttackTarget(LivingEntity target) {
		getBrain().forget(MemoryModuleType.ROAR_TARGET);
		getBrain().remember(MemoryModuleType.ATTACK_TARGET, target);
		getBrain().forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		SonicBoomTask.cooldown(this, SONIC_BOOM_COOLDOWN_TICKS);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		EntityDimensions entityDimensions = super.getBaseDimensions(pose);
		return isDiggingOrEmerging()
				? EntityDimensions.fixed(entityDimensions.width(), 1.0F)
				: entityDimensions;
	}

	@Override
	public boolean isPushable() {
		return !isDiggingOrEmerging() && super.isPushable();
	}

	@Override
	protected void pushAway(Entity entity) {
		if (isAiDisabled() || getBrain().hasMemoryModule(MemoryModuleType.TOUCH_COOLDOWN)) {
			super.pushAway(entity);
			return;
		}

		getBrain().remember(MemoryModuleType.TOUCH_COOLDOWN, Unit.INSTANCE, TOUCH_COOLDOWN_TICKS);
		increaseAngerAt(entity);
		WardenBrain.lookAtDisturbance(this, entity.getBlockPos());
		super.pushAway(entity);
	}

	@VisibleForTesting
	public WardenAngerManager getAngerManager() {
		return angerManager;
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new MobNavigation(this, world) {
			@Override
			protected PathNodeNavigator createPathNodeNavigator(int range) {
				this.nodeMaker = new LandPathNodeMaker();
				return new PathNodeNavigator(this.nodeMaker, range) {
					@Override
					protected float getDistance(PathNode a, PathNode b) {
						return a.getHorizontalDistance(b);
					}
				};
			}
		};
	}

	@Override
	public Vibrations.ListenerData getVibrationListenerData() {
		return vibrationListenerData;
	}

	@Override
	public Vibrations.Callback getVibrationCallback() {
		return vibrationCallback;
	}

	class VibrationCallback implements Vibrations.Callback {

		private static final int RANGE = 16;
		private final PositionSource
				positionSource =
				new EntityPositionSource(WardenEntity.this, WardenEntity.this.getStandingEyeHeight());

		@Override
		public int getRange() {
			return RANGE;
		}

		@Override
		public PositionSource getPositionSource() {
			return this.positionSource;
		}

		@Override
		public TagKey<GameEvent> getTag() {
			return GameEventTags.WARDEN_CAN_LISTEN;
		}

		@Override
		public boolean triggersAvoidCriterion() {
			return true;
		}

		/**
		 * Проверяет, должен ли страж реагировать на вибрацию: игнорирует события во время закапывания,
		 * при активном кулдауне вибрации, а также вибрации от невалидных целей.
		 */
		@Override
		public boolean accepts(
				ServerWorld world,
				BlockPos pos,
				RegistryEntry<GameEvent> event,
				GameEvent.Emitter emitter
		) {
			if (WardenEntity.this.isAiDisabled()
					|| WardenEntity.this.isDead()
					|| WardenEntity.this.getBrain().hasMemoryModule(MemoryModuleType.VIBRATION_COOLDOWN)
					|| WardenEntity.this.isDiggingOrEmerging()
					|| !world.getWorldBorder().contains(pos)
			) {
				return false;
			}

			if (emitter.sourceEntity() instanceof LivingEntity livingEntity
					&& !WardenEntity.this.isValidTarget(livingEntity)) {
				return false;
			}

			return true;
		}

		/**
		 * Обрабатывает принятую вибрацию: увеличивает гнев к источнику, воспроизводит звук щупалец
		 * и направляет взгляд стража к точке возмущения. Снаряды обрабатываются с пониженным гневом,
		 * если нет памяти о недавнем снаряде.
		 */
		@Override
		public void accept(
				ServerWorld world,
				BlockPos pos,
				RegistryEntry<GameEvent> event,
				@Nullable Entity sourceEntity,
				@Nullable Entity entity,
				float distance
		) {
			if (WardenEntity.this.isDead()) {
				return;
			}

			WardenEntity.this.brain.remember(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, VIBRATION_COOLDOWN_TICKS);
			world.sendEntityStatus(WardenEntity.this, (byte) TENDRIL_VIBRATION_STATUS);
			WardenEntity.this.playSound(
					SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS,
					5.0F,
					WardenEntity.this.getSoundPitch()
			);
			BlockPos disturbancePos = pos;

			if (entity != null) {
				if (WardenEntity.this.isInRange(entity, VIBRATION_ENTITY_DETECT_RANGE)) {
					if (WardenEntity.this.getBrain().hasMemoryModule(MemoryModuleType.RECENT_PROJECTILE)) {
						if (WardenEntity.this.isValidTarget(entity)) {
							disturbancePos = entity.getBlockPos();
						}

						WardenEntity.this.increaseAngerAt(entity);
					} else {
						WardenEntity.this.increaseAngerAt(entity, PROJECTILE_ANGER_AMOUNT, true);
					}
				}

				WardenEntity.this.getBrain().remember(MemoryModuleType.RECENT_PROJECTILE, Unit.INSTANCE, RECENT_PROJECTILE_MEMORY_TICKS);
			} else {
				WardenEntity.this.increaseAngerAt(sourceEntity);
			}

			if (WardenEntity.this.getAngriness().isAngry()) {
				return;
			}

			Optional<LivingEntity> primeSuspect = WardenEntity.this.angerManager.getPrimeSuspect();
			if (entity != null || primeSuspect.isEmpty() || primeSuspect.get() == sourceEntity) {
				WardenBrain.lookAtDisturbance(WardenEntity.this, disturbancePos);
			}
		}
	}
}
