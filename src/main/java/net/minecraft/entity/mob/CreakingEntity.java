package net.minecraft.entity.mob;

import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CreakingHeartBlock;
import net.minecraft.block.entity.CreakingHeartBlockEntity;
import net.minecraft.block.enums.CreakingHeartState;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Крикун — моб из бледного сада, неуязвимый без наблюдателя.
 */
public class CreakingEntity extends HostileEntity {

	private static final int ATTACK_ANIMATION_DURATION = 15;
	private static final int INVULNERABLE_TIMER_RESET = 8;
	private static final int PLAYER_INTERSECTION_THRESHOLD = 4;
	private static final float ATTACK_DAMAGE = 3.0F;
	private static final float FOLLOW_RANGE = 32.0F;
	private static final float ACTIVATION_RANGE_SQUARED = 144.0F;
	private static final float MOVEMENT_SPEED = 0.4F;
	private static final int STATUS_INVULNERABLE = 66;
	private static final int STATUS_ATTACK = 4;
	public static final int CRUMBLING_DEATH_TICKS = 40;
	public static final int CRUMBLING_FINISH_TICKS = 45;
	public static final float LIMB_ANIMATION_SPEED = 0.3F;
	public static final int EYE_GLOW_COLOR = 16545810;
	public static final int EYE_INACTIVE_COLOR = 6250335;
	private static final TrackedData<Boolean>
			UNROOTED =
			DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			ACTIVE =
			DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean>
			CRUMBLING =
			DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Optional<BlockPos>>
			HOME_POS =
			DataTracker.registerData(CreakingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
	private int attackAnimationTimer;
	public final AnimationState attackAnimationState = new AnimationState();
	public final AnimationState invulnerableAnimationState = new AnimationState();
	public final AnimationState crumblingAnimationState = new AnimationState();
	private int invulnerableAnimationTimer;
	private boolean glowingEyesWhileCrumbling;
	private int nextEyeFlickerTime;
	private int playerIntersectionTimer;

	public CreakingEntity(EntityType<? extends CreakingEntity> entityType, World world) {
		super(entityType, world);
		lookControl = new CreakingEntity.CreakingLookControl(this);
		moveControl = new CreakingEntity.CreakingMoveControl(this);
		jumpControl = new CreakingEntity.CreakingJumpControl(this);
		MobNavigation mobNavigation = (MobNavigation) getNavigation();
		mobNavigation.setCanSwim(true);
		experiencePoints = 0;
	}

	/**
	 * Привязывает Creaking к Creaking Heart по позиции блока,
	 * настраивая штрафы пути для опасных блоков.
	 */
	public void initHomePos(BlockPos homePos) {
		setHomePos(homePos);
		setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 8.0F);
		setPathfindingPenalty(PathNodeType.POWDER_SNOW, 8.0F);
		setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
	}

	public boolean isTransient() {
		return getHomePos() != null;
	}

	@Override
	protected BodyControl createBodyControl() {
		return new CreakingEntity.CreakingBodyControl(this);
	}

	@Override
	protected Brain.Profile<CreakingEntity> createBrainProfile() {
		return CreakingBrain.createBrainProfile();
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return CreakingBrain.create(this, createBrainProfile().deserialize(dynamic));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(UNROOTED, true);
		builder.add(ACTIVE, false);
		builder.add(CRUMBLING, false);
		builder.add(HOME_POS, Optional.empty());
	}

	public static DefaultAttributeContainer.Builder createCreakingAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 1.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
		                    .add(EntityAttributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
		                    .add(EntityAttributes.FOLLOW_RANGE, FOLLOW_RANGE)
		                    .add(EntityAttributes.STEP_HEIGHT, 1.0625);
	}

	public boolean isUnrooted() {
		return dataTracker.get(UNROOTED);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (!(target instanceof LivingEntity)) {
			return false;
		}

		attackAnimationTimer = ATTACK_ANIMATION_DURATION;
		getEntityWorld().sendEntityStatus(this, (byte) STATUS_ATTACK);
		return super.tryAttack(world, target);
	}

	/**
	 * Обрабатывает урон с учётом привязки к Creaking Heart:
	 * если сущность привязана к сердцу, урон не наносится напрямую —
	 * вместо этого сердце получает уведомление и воспроизводится анимация неуязвимости.
	 */
	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		BlockPos homePos = getHomePos();
		if (homePos == null || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.damage(world, source, amount);
		}

		if (isInvulnerableTo(world, source) || invulnerableAnimationTimer > 0 || isDead()) {
			return false;
		}

		PlayerEntity attacker = becomeAngryAndGetPlayer(source);
		Entity sourceEntity = source.getSource();
		if (!(sourceEntity instanceof LivingEntity)
				&& !(sourceEntity instanceof ProjectileEntity)
				&& attacker == null) {
			return false;
		}

		invulnerableAnimationTimer = INVULNERABLE_TIMER_RESET;
		getEntityWorld().sendEntityStatus(this, (byte) STATUS_INVULNERABLE);
		emitGameEvent(GameEvent.ENTITY_ACTION);
		if (getEntityWorld().getBlockEntity(homePos) instanceof CreakingHeartBlockEntity heart
				&& heart.isPuppet(this)) {
			if (attacker != null) {
				heart.onPuppetDamage();
			}

			playHurtSound(source);
		}

		return true;
	}

	public PlayerEntity becomeAngryAndGetPlayer(DamageSource damageSource) {
		becomeAngry(damageSource);
		return setAttackingPlayer(damageSource);
	}

	@Override
	public boolean isPushable() {
		return super.isPushable() && isUnrooted();
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (isUnrooted()) {
			super.addVelocity(deltaX, deltaY, deltaZ);
		}
	}

	@Override
	public Brain<CreakingEntity> getBrain() {
		return (Brain<CreakingEntity>) super.getBrain();
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("creakingBrain");
		getBrain().tick((ServerWorld) getEntityWorld(), this);
		profiler.pop();
		CreakingBrain.updateActivities(this);
	}

	@Override
	public void tickMovement() {
		if (invulnerableAnimationTimer > 0) {
			invulnerableAnimationTimer--;
		}

		if (attackAnimationTimer > 0) {
			attackAnimationTimer--;
		}

		if (!getEntityWorld().isClient()) {
			boolean wasUnrooted = dataTracker.get(UNROOTED);
			boolean shouldUnroot = shouldBeUnrooted();
			if (shouldUnroot != wasUnrooted) {
				emitGameEvent(GameEvent.ENTITY_ACTION);
				if (shouldUnroot) {
					playSound(SoundEvents.ENTITY_CREAKING_UNFREEZE);
				}
				else {
					stopMovement();
					playSound(SoundEvents.ENTITY_CREAKING_FREEZE);
				}
			}

			dataTracker.set(UNROOTED, shouldUnroot);
		}

		super.tickMovement();
	}

	@Override
	public void tick() {
		if (!getEntityWorld().isClient()) {
			BlockPos homePos = getHomePos();
			if (homePos != null) {
				boolean isPuppet = getEntityWorld()
						.getBlockEntity(homePos) instanceof CreakingHeartBlockEntity heart
						&& heart.isPuppet(this);
				if (!isPuppet) {
					setHealth(0.0F);
				}
			}
		}

		super.tick();
		if (getEntityWorld().isClient()) {
			tickAttackAnimation();
			updateCrumblingEyeFlicker();
		}
	}

	@Override
	protected void updatePostDeath() {
		if (isTransient() && isCrumbling()) {
			deathTime++;
			if (!getEntityWorld().isClient() && deathTime > CRUMBLING_FINISH_TICKS && !isRemoved()) {
				finishCrumbling();
			}
		}
		else {
			super.updatePostDeath();
		}
	}

	@Override
	protected void updateLimbs(float posDelta) {
		float clamped = Math.min(posDelta * 25.0F, 3.0F);
		limbAnimator.updateLimbs(clamped, MOVEMENT_SPEED, 1.0F);
	}

	private void tickAttackAnimation() {
		attackAnimationState.setRunning(attackAnimationTimer > 0, age);
		invulnerableAnimationState.setRunning(invulnerableAnimationTimer > 0, age);
		crumblingAnimationState.setRunning(isCrumbling(), age);
	}

	/**
	 * Завершает анимацию рассыпания: спавнит частицы блоков и удаляет сущность.
	 * Вызывается только на сервере после истечения {@link #CRUMBLING_FINISH_TICKS}.
	 */
	public void finishCrumbling() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			Box box = getBoundingBox();
			Vec3d center = box.getCenter();
			double spreadX = box.getLengthX() * 0.3;
			double spreadY = box.getLengthY() * 0.3;
			double spreadZ = box.getLengthZ() * 0.3;
			serverWorld.spawnParticles(
					new BlockStateParticleEffect(ParticleTypes.BLOCK_CRUMBLE, Blocks.PALE_OAK_WOOD.getDefaultState()),
					center.x,
					center.y,
					center.z,
					100,
					spreadX,
					spreadY,
					spreadZ,
					0.0
			);
			serverWorld.spawnParticles(
					new BlockStateParticleEffect(
							ParticleTypes.BLOCK_CRUMBLE,
							Blocks.CREAKING_HEART
									.getDefaultState()
									.with(CreakingHeartBlock.ACTIVE, CreakingHeartState.AWAKE)
					),
					center.x,
					center.y,
					center.z,
					10,
					spreadX,
					spreadY,
					spreadZ,
					0.0
			);
		}

		playSound(getDeathSound());
		remove(Entity.RemovalReason.DISCARDED);
	}

	public void killFromHeart(DamageSource damageSource) {
		becomeAngryAndGetPlayer(damageSource);
		onDeath(damageSource);
		playSound(SoundEvents.ENTITY_CREAKING_TWITCH);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == STATUS_INVULNERABLE) {
			invulnerableAnimationTimer = INVULNERABLE_TIMER_RESET;
			playHurtSound(getDamageSources().generic());
		}
		else if (status == STATUS_ATTACK) {
			attackAnimationTimer = ATTACK_ANIMATION_DURATION;
			playAttackSound();
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	public boolean isFireImmune() {
		return isTransient() || super.isFireImmune();
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return !isTransient() && super.canUsePortals(allowVehicles);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new CreakingEntity.CreakingNavigation(this, world);
	}

	public boolean isStuckWithPlayer() {
		List<PlayerEntity> nearestPlayers = brain
				.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS)
				.orElse(List.of());
		if (nearestPlayers.isEmpty()) {
			playerIntersectionTimer = 0;
			return false;
		}

		Box box = getBoundingBox();
		for (PlayerEntity player : nearestPlayers) {
			if (box.contains(player.getEyePos())) {
				playerIntersectionTimer++;
				return playerIntersectionTimer > PLAYER_INTERSECTION_THRESHOLD;
			}
		}

		playerIntersectionTimer = 0;
		return false;
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		view.<BlockPos>read("home_pos", BlockPos.CODEC).ifPresent(this::initHomePos);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("home_pos", BlockPos.CODEC, getHomePos());
	}

	public void setHomePos(BlockPos pos) {
		dataTracker.set(HOME_POS, Optional.of(pos));
	}

	public @Nullable BlockPos getHomePos() {
		return dataTracker.get(HOME_POS).orElse(null);
	}

	public void setCrumbling() {
		dataTracker.set(CRUMBLING, true);
	}

	public boolean isCrumbling() {
		return dataTracker.get(CRUMBLING);
	}

	public boolean hasGlowingEyesWhileCrumbling() {
		return glowingEyesWhileCrumbling;
	}

	/**
	 * Мерцание глаз во время анимации рассыпания: переключает состояние свечения
	 * с нарастающей частотой по мере увеличения {@code deathTime}.
	 */
	public void updateCrumblingEyeFlicker() {
		if (deathTime <= nextEyeFlickerTime) {
			return;
		}

		int minInterval = glowingEyesWhileCrumbling ? 2 : deathTime / 4;
		int maxInterval = glowingEyesWhileCrumbling ? 8 : deathTime / 2;
		nextEyeFlickerTime = deathTime + getRandom().nextBetween(minInterval, maxInterval);
		glowingEyesWhileCrumbling = !glowingEyesWhileCrumbling;
	}

	@Override
	public void playAttackSound() {
		playSound(SoundEvents.ENTITY_CREAKING_ATTACK);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isActive() ? null : SoundEvents.ENTITY_CREAKING_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isTransient() ? SoundEvents.ENTITY_CREAKING_SWAY : super.getHurtSound(source);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CREAKING_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_CREAKING_STEP, 0.15F, 1.0F);
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return getTargetInBrain();
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
		if (isUnrooted()) {
			super.takeKnockback(strength, x, z);
		}
	}

	/**
	 * Определяет, должна ли сущность быть разморожена (unrooted).
	 * Если игрок смотрит на Creaking — он замораживается или активируется.
	 * Если ни один подходящий игрок не найден — деактивируется.
	 */
	public boolean shouldBeUnrooted() {
		List<PlayerEntity> nearestPlayers = brain
				.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS)
				.orElse(List.of());
		boolean active = isActive();
		if (nearestPlayers.isEmpty()) {
			if (active) {
				deactivate();
			}

			return true;
		}

		boolean hasValidTarget = false;
		for (PlayerEntity player : nearestPlayers) {
			if (!canTarget(player) || isTeammate(player)) {
				continue;
			}

			hasValidTarget = true;
			boolean canSeeWithoutDisguise = !active
					|| LivingEntity.NOT_WEARING_GAZE_DISGUISE_PREDICATE.test(player);
			if (canSeeWithoutDisguise
					&& isEntityLookingAtMe(
							player,
							0.5,
							false,
							true,
							getEyeY(),
							getY() + 0.5 * getScale(),
							(getEyeY() + getY()) / 2.0
					)) {
				if (active) {
					return false;
				}

				if (player.squaredDistanceTo(this) < ACTIVATION_RANGE_SQUARED) {
					activate(player);
					return false;
				}
			}
		}

		if (!hasValidTarget && active) {
			deactivate();
		}

		return true;
	}

	public void activate(PlayerEntity player) {
		getBrain().remember(MemoryModuleType.ATTACK_TARGET, player);
		emitGameEvent(GameEvent.ENTITY_ACTION);
		playSound(SoundEvents.ENTITY_CREAKING_ACTIVATE);
		setActive(true);
	}

	public void deactivate() {
		getBrain().forget(MemoryModuleType.ATTACK_TARGET);
		emitGameEvent(GameEvent.ENTITY_ACTION);
		playSound(SoundEvents.ENTITY_CREAKING_DEACTIVATE);
		setActive(false);
	}

	public void setActive(boolean active) {
		dataTracker.set(ACTIVE, active);
	}

	public boolean isActive() {
		return dataTracker.get(ACTIVE);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.0F;
	}

	/** Контроллер тела скрипуна — блокирует вращение корпуса, пока моб укоренён. */
	class CreakingBodyControl extends BodyControl {

		public CreakingBodyControl(final CreakingEntity creaking) {
			super(creaking);
		}

		@Override
		public void tick() {
			if (CreakingEntity.this.isUnrooted()) {
				super.tick();
			}
		}
	}

	/** Контроллер прыжка скрипуна — запрещает прыжки в укоренённом состоянии. */
	class CreakingJumpControl extends JumpControl {

		public CreakingJumpControl(final CreakingEntity creaking) {
			super(creaking);
		}

		@Override
		public void tick() {
			if (CreakingEntity.this.isUnrooted()) {
				super.tick();
			}
			else {
				CreakingEntity.this.setJumping(false);
			}
		}
	}

	class CreakingLandPathNodeMaker extends LandPathNodeMaker {

		private static final int MAX_PATHFINDING_DISTANCE_SQUARED = 1024;

		@Override
		public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
			BlockPos homePos = CreakingEntity.this.getHomePos();
			if (homePos == null) {
				return super.getDefaultNodeType(context, x, y, z);
			}

			double distToNode = homePos.getSquaredDistance(new Vec3i(x, y, z));
			boolean tooFarAndFarther = distToNode > MAX_PATHFINDING_DISTANCE_SQUARED
					&& distToNode >= homePos.getSquaredDistance(context.getEntityPos());
			return tooFarAndFarther
					? PathNodeType.BLOCKED
					: super.getDefaultNodeType(context, x, y, z);
		}
	}

	/** Контроллер взгляда скрипуна — блокирует поворот головы, пока моб укоренён. */
	class CreakingLookControl extends LookControl {

		public CreakingLookControl(final CreakingEntity creaking) {
			super(creaking);
		}

		@Override
		public void tick() {
			if (CreakingEntity.this.isUnrooted()) {
				super.tick();
			}
		}
	}

	/** Контроллер движения скрипуна — блокирует перемещение в укоренённом состоянии. */
	class CreakingMoveControl extends MoveControl {

		public CreakingMoveControl(final CreakingEntity creaking) {
			super(creaking);
		}

		@Override
		public void tick() {
			if (CreakingEntity.this.isUnrooted()) {
				super.tick();
			}
		}
	}

	/** Навигация скрипуна — ограничивает поиск пути радиусом от домашней позиции. */
	class CreakingNavigation extends MobNavigation {

		CreakingNavigation(final CreakingEntity creaking, final World world) {
			super(creaking, world);
		}

		@Override
		public void tick() {
			if (CreakingEntity.this.isUnrooted()) {
				super.tick();
			}
		}

		@Override
		protected PathNodeNavigator createPathNodeNavigator(int range) {
			this.nodeMaker = CreakingEntity.this.new CreakingLandPathNodeMaker();
			this.nodeMaker.setCanEnterOpenDoors(true);
			return new PathNodeNavigator(this.nodeMaker, range);
		}
	}
}
