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
 * {@code CreakingEntity}.
 */
public class CreakingEntity extends HostileEntity {

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
	private static final int ATTACK_ANIMATION_DURATION = 15;
	private static final int INVULNERABLE_ANIMATION_DURATION = 1;
	private static final float ATTACK_DAMAGE = 3.0F;
	private static final float FOLLOW_RANGE = 32.0F;
	private static final float ACTIVATION_RANGE_SQUARED = 144.0F;
	public static final int CRUMBLING_DEATH_TICKS = 40;
	private static final float MOVEMENT_SPEED = 0.4F;
	public static final float LIMB_ANIMATION_SPEED = 0.3F;
	public static final int EYE_GLOW_COLOR = 16545810;
	public static final int EYE_INACTIVE_COLOR = 6250335;
	public static final int INVULNERABLE_TIMER_RESET = 8;
	public static final int CRUMBLING_FINISH_TICKS = 45;
	private static final int PLAYER_INTERSECTION_THRESHOLD = 4;
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
		this.lookControl = new CreakingEntity.CreakingLookControl(this);
		this.moveControl = new CreakingEntity.CreakingMoveControl(this);
		this.jumpControl = new CreakingEntity.CreakingJumpControl(this);
		MobNavigation mobNavigation = (MobNavigation) this.getNavigation();
		mobNavigation.setCanSwim(true);
		this.experiencePoints = 0;
	}

	public void initHomePos(BlockPos homePos) {
		this.setHomePos(homePos);
		this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 8.0F);
		this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, 8.0F);
		this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
		this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
		this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
	}

	public boolean isTransient() {
		return this.getHomePos() != null;
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
		return CreakingBrain.create(this, this.createBrainProfile().deserialize(dynamic));
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
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.4F)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
		                    .add(EntityAttributes.FOLLOW_RANGE, 32.0)
		                    .add(EntityAttributes.STEP_HEIGHT, 1.0625);
	}

	public boolean isUnrooted() {
		return this.dataTracker.get(UNROOTED);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (!(target instanceof LivingEntity)) {
			return false;
		}
		else {
			this.attackAnimationTimer = 15;
			this.getEntityWorld().sendEntityStatus(this, (byte) 4);
			return super.tryAttack(world, target);
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		BlockPos blockPos = this.getHomePos();
		if (blockPos == null || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.damage(world, source, amount);
		}
		else if (!this.isInvulnerableTo(world, source) && this.invulnerableAnimationTimer <= 0 && !this.isDead()) {
			PlayerEntity playerEntity = this.becomeAngryAndGetPlayer(source);
			Entity entity = source.getSource();
			if (!(entity instanceof LivingEntity) && !(entity instanceof ProjectileEntity) && playerEntity == null) {
				return false;
			}
			else {
				this.invulnerableAnimationTimer = 8;
				this.getEntityWorld().sendEntityStatus(this, (byte) 66);
				this.emitGameEvent(GameEvent.ENTITY_ACTION);
				if (this
						.getEntityWorld()
						.getBlockEntity(blockPos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity
						&& creakingHeartBlockEntity.isPuppet(this)) {
					if (playerEntity != null) {
						creakingHeartBlockEntity.onPuppetDamage();
					}

					this.playHurtSound(source);
				}

				return true;
			}
		}
		else {
			return false;
		}
	}

	public PlayerEntity becomeAngryAndGetPlayer(DamageSource damageSource) {
		this.becomeAngry(damageSource);
		return this.setAttackingPlayer(damageSource);
	}

	@Override
	public boolean isPushable() {
		return super.isPushable() && this.isUnrooted();
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (this.isUnrooted()) {
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
		this.getBrain().tick((ServerWorld) this.getEntityWorld(), this);
		profiler.pop();
		CreakingBrain.updateActivities(this);
	}

	@Override
	public void tickMovement() {
		if (this.invulnerableAnimationTimer > 0) {
			this.invulnerableAnimationTimer--;
		}

		if (this.attackAnimationTimer > 0) {
			this.attackAnimationTimer--;
		}

		if (!this.getEntityWorld().isClient()) {
			boolean bl = this.dataTracker.get(UNROOTED);
			boolean bl2 = this.shouldBeUnrooted();
			if (bl2 != bl) {
				this.emitGameEvent(GameEvent.ENTITY_ACTION);
				if (bl2) {
					this.playSound(SoundEvents.ENTITY_CREAKING_UNFREEZE);
				}
				else {
					this.stopMovement();
					this.playSound(SoundEvents.ENTITY_CREAKING_FREEZE);
				}
			}

			this.dataTracker.set(UNROOTED, bl2);
		}

		super.tickMovement();
	}

	@Override
	public void tick() {
		if (!this.getEntityWorld().isClient()) {
			BlockPos blockPos = this.getHomePos();
			if (blockPos != null) {
				boolean
						bl =
						this
								.getEntityWorld()
								.getBlockEntity(blockPos) instanceof CreakingHeartBlockEntity creakingHeartBlockEntity
								&& creakingHeartBlockEntity.isPuppet(this);
				if (!bl) {
					this.setHealth(0.0F);
				}
			}
		}

		super.tick();
		if (this.getEntityWorld().isClient()) {
			this.tickAttackAnimation();
			this.updateCrumblingEyeFlicker();
		}
	}

	@Override
	protected void updatePostDeath() {
		if (this.isTransient() && this.isCrumbling()) {
			this.deathTime++;
			if (!this.getEntityWorld().isClient() && this.deathTime > 45 && !this.isRemoved()) {
				this.finishCrumbling();
			}
		}
		else {
			super.updatePostDeath();
		}
	}

	@Override
	protected void updateLimbs(float posDelta) {
		float f = Math.min(posDelta * 25.0F, 3.0F);
		this.limbAnimator.updateLimbs(f, 0.4F, 1.0F);
	}

	private void tickAttackAnimation() {
		this.attackAnimationState.setRunning(this.attackAnimationTimer > 0, this.age);
		this.invulnerableAnimationState.setRunning(this.invulnerableAnimationTimer > 0, this.age);
		this.crumblingAnimationState.setRunning(this.isCrumbling(), this.age);
	}

	public void finishCrumbling() {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			Box box = this.getBoundingBox();
			Vec3d vec3d = box.getCenter();
			double d = box.getLengthX() * 0.3;
			double e = box.getLengthY() * 0.3;
			double f = box.getLengthZ() * 0.3;
			serverWorld.spawnParticles(
					new BlockStateParticleEffect(ParticleTypes.BLOCK_CRUMBLE, Blocks.PALE_OAK_WOOD.getDefaultState()),
					vec3d.x,
					vec3d.y,
					vec3d.z,
					100,
					d,
					e,
					f,
					0.0
			);
			serverWorld.spawnParticles(
					new BlockStateParticleEffect(
							ParticleTypes.BLOCK_CRUMBLE,
							Blocks.CREAKING_HEART
									.getDefaultState()
									.with(CreakingHeartBlock.ACTIVE, CreakingHeartState.AWAKE)
					),
					vec3d.x,
					vec3d.y,
					vec3d.z,
					10,
					d,
					e,
					f,
					0.0
			);
		}

		this.playSound(this.getDeathSound());
		this.remove(Entity.RemovalReason.DISCARDED);
	}

	public void killFromHeart(DamageSource damageSource) {
		this.becomeAngryAndGetPlayer(damageSource);
		this.onDeath(damageSource);
		this.playSound(SoundEvents.ENTITY_CREAKING_TWITCH);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 66) {
			this.invulnerableAnimationTimer = 8;
			this.playHurtSound(this.getDamageSources().generic());
		}
		else if (status == 4) {
			this.attackAnimationTimer = 15;
			this.playAttackSound();
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	public boolean isFireImmune() {
		return this.isTransient() || super.isFireImmune();
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return !this.isTransient() && super.canUsePortals(allowVehicles);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new CreakingEntity.CreakingNavigation(this, world);
	}

	public boolean isStuckWithPlayer() {
		List<PlayerEntity>
				list =
				this.brain.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
		if (list.isEmpty()) {
			this.playerIntersectionTimer = 0;
			return false;
		}
		else {
			Box box = this.getBoundingBox();

			for (PlayerEntity playerEntity : list) {
				if (box.contains(playerEntity.getEyePos())) {
					this.playerIntersectionTimer++;
					return this.playerIntersectionTimer > 4;
				}
			}

			this.playerIntersectionTimer = 0;
			return false;
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		view.<BlockPos>read("home_pos", BlockPos.CODEC).ifPresent(this::initHomePos);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("home_pos", BlockPos.CODEC, this.getHomePos());
	}

	public void setHomePos(BlockPos pos) {
		this.dataTracker.set(HOME_POS, Optional.of(pos));
	}

	public @Nullable BlockPos getHomePos() {
		return this.dataTracker.get(HOME_POS).orElse(null);
	}

	public void setCrumbling() {
		this.dataTracker.set(CRUMBLING, true);
	}

	public boolean isCrumbling() {
		return this.dataTracker.get(CRUMBLING);
	}

	public boolean hasGlowingEyesWhileCrumbling() {
		return this.glowingEyesWhileCrumbling;
	}

	public void updateCrumblingEyeFlicker() {
		if (this.deathTime > this.nextEyeFlickerTime) {
			this.nextEyeFlickerTime = this.deathTime
					+ this
					.getRandom()
					.nextBetween(
							this.glowingEyesWhileCrumbling ? 2 : this.deathTime / 4,
							this.glowingEyesWhileCrumbling ? 8 : this.deathTime / 2
					);
			this.glowingEyesWhileCrumbling = !this.glowingEyesWhileCrumbling;
		}
	}

	@Override
	public void playAttackSound() {
		this.playSound(SoundEvents.ENTITY_CREAKING_ATTACK);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isActive() ? null : SoundEvents.ENTITY_CREAKING_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.isTransient() ? SoundEvents.ENTITY_CREAKING_SWAY : super.getHurtSound(source);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CREAKING_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.ENTITY_CREAKING_STEP, 0.15F, 1.0F);
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return this.getTargetInBrain();
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
		if (this.isUnrooted()) {
			super.takeKnockback(strength, x, z);
		}
	}

	public boolean shouldBeUnrooted() {
		List<PlayerEntity>
				list =
				this.brain.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
		boolean bl = this.isActive();
		if (list.isEmpty()) {
			if (bl) {
				this.deactivate();
			}

			return true;
		}
		else {
			boolean bl2 = false;

			for (PlayerEntity playerEntity : list) {
				if (this.canTarget(playerEntity) && !this.isTeammate(playerEntity)) {
					bl2 = true;
					if ((!bl || LivingEntity.NOT_WEARING_GAZE_DISGUISE_PREDICATE.test(playerEntity))
							&& this.isEntityLookingAtMe(
							playerEntity,
							0.5,
							false,
							true,
							this.getEyeY(),
							this.getY() + 0.5 * this.getScale(),
							(this.getEyeY() + this.getY()) / 2.0
					)) {
						if (bl) {
							return false;
						}

						if (playerEntity.squaredDistanceTo(this) < 144.0) {
							this.activate(playerEntity);
							return false;
						}
					}
				}
			}

			if (!bl2 && bl) {
				this.deactivate();
			}

			return true;
		}
	}

	public void activate(PlayerEntity player) {
		this.getBrain().remember(MemoryModuleType.ATTACK_TARGET, player);
		this.emitGameEvent(GameEvent.ENTITY_ACTION);
		this.playSound(SoundEvents.ENTITY_CREAKING_ACTIVATE);
		this.setActive(true);
	}

	public void deactivate() {
		this.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
		this.emitGameEvent(GameEvent.ENTITY_ACTION);
		this.playSound(SoundEvents.ENTITY_CREAKING_DEACTIVATE);
		this.setActive(false);
	}

	public void setActive(boolean active) {
		this.dataTracker.set(ACTIVE, active);
	}

	public boolean isActive() {
		return this.dataTracker.get(ACTIVE);
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return 0.0F;
	}

	/**
	 * {@code CreakingBodyControl}.
	 */
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

	/**
	 * {@code CreakingJumpControl}.
	 */
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

	/**
	 * {@code CreakingLandPathNodeMaker}.
	 */
	class CreakingLandPathNodeMaker extends LandPathNodeMaker {

		private static final int MAX_PATHFINDING_DISTANCE_SQUARED = 1024;

		@Override
		public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
			BlockPos blockPos = CreakingEntity.this.getHomePos();
			if (blockPos == null) {
				return super.getDefaultNodeType(context, x, y, z);
			}
			else {
				double d = blockPos.getSquaredDistance(new Vec3i(x, y, z));
				return d > 1024.0 && d >= blockPos.getSquaredDistance(context.getEntityPos()) ? PathNodeType.BLOCKED
				                                                                              : super.getDefaultNodeType(
						                                                                              context,
						                                                                              x,
						                                                                              y,
						                                                                              z
				                                                                              );
			}
		}
	}

	/**
	 * {@code CreakingLookControl}.
	 */
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

	/**
	 * {@code CreakingMoveControl}.
	 */
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

	/**
	 * {@code CreakingNavigation}.
	 */
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
