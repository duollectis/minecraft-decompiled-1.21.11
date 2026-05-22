package net.minecraft.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Страж — водный моб, атакующий лучом. В закрытом состоянии (шипы выпущены) наносит
 * урон атакующим в ближнем бою. Луч заряжается {@code WARMUP_TIME} тиков перед выстрелом.
 * Передвигается только в воде; на суше прыгает и задыхается.
 */
public class GuardianEntity extends HostileEntity {

	protected static final int WARMUP_TIME = 80;
	private static final TrackedData<Boolean>
			SPIKES_RETRACTED =
			DataTracker.registerData(GuardianEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer>
			BEAM_TARGET_ID =
			DataTracker.registerData(GuardianEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private float tailAngle;
	private float lastTailAngle;
	private float spikesExtensionRate;
	private float spikesExtension;
	private float lastSpikesExtension;
	private @Nullable LivingEntity cachedBeamTarget;
	private int beamTicks;
	private boolean flopping;
	protected @Nullable WanderAroundGoal wanderGoal;

	public GuardianEntity(EntityType<? extends GuardianEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 10;
		setPathfindingPenalty(PathNodeType.WATER, 0.0F);
		moveControl = new GuardianEntity.GuardianMoveControl(this);
		tailAngle = random.nextFloat();
		lastTailAngle = tailAngle;
	}

	@Override
	protected void initGoals() {
		GoToWalkTargetGoal goToWalkTargetGoal = new GoToWalkTargetGoal(this, 1.0);
		wanderGoal = new WanderAroundGoal(this, 1.0, WARMUP_TIME);
		goalSelector.add(4, new GuardianEntity.FireBeamGoal(this));
		goalSelector.add(5, goToWalkTargetGoal);
		goalSelector.add(7, wanderGoal);
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(8, new LookAtEntityGoal(this, GuardianEntity.class, 12.0F, 0.01F));
		goalSelector.add(9, new LookAroundGoal(this));
		wanderGoal.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		goToWalkTargetGoal.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		targetSelector.add(
				1,
				new ActiveTargetGoal<>(
						this,
						LivingEntity.class,
						10,
						true,
						false,
						new GuardianEntity.GuardianTargetPredicate(this)
				)
		);
	}

	public static DefaultAttributeContainer.Builder createGuardianAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.5)
		                    .add(EntityAttributes.MAX_HEALTH, 30.0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new SwimNavigation(this, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SPIKES_RETRACTED, false);
		builder.add(BEAM_TARGET_ID, 0);
	}

	public boolean areSpikesRetracted() {
		return dataTracker.get(SPIKES_RETRACTED);
	}

	void setSpikesRetracted(boolean retracted) {
		dataTracker.set(SPIKES_RETRACTED, retracted);
	}

	public int getWarmupTime() {
		return WARMUP_TIME;
	}

	void setBeamTarget(int entityId) {
		dataTracker.set(BEAM_TARGET_ID, entityId);
	}

	public boolean hasBeamTarget() {
		return dataTracker.get(BEAM_TARGET_ID) != 0;
	}

	/**
	 * Возвращает текущую цель луча. На клиенте использует кэш по ID сущности,
	 * на сервере — напрямую возвращает цель атаки.
	 */
	public @Nullable LivingEntity getBeamTarget() {
		if (!hasBeamTarget()) {
			return null;
		}

		if (getEntityWorld().isClient()) {
			if (cachedBeamTarget != null) {
				return cachedBeamTarget;
			}

			Entity entity = getEntityWorld().getEntityById(dataTracker.get(BEAM_TARGET_ID));
			if (entity instanceof LivingEntity livingEntity) {
				cachedBeamTarget = livingEntity;
				return cachedBeamTarget;
			}

			return null;
		}

		return getTarget();
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (BEAM_TARGET_ID.equals(data)) {
			beamTicks = 0;
			cachedBeamTarget = null;
		}
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 160;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isTouchingWater() ? SoundEvents.ENTITY_GUARDIAN_AMBIENT : SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isTouchingWater() ? SoundEvents.ENTITY_GUARDIAN_HURT : SoundEvents.ENTITY_GUARDIAN_HURT_LAND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isTouchingWater() ? SoundEvents.ENTITY_GUARDIAN_DEATH : SoundEvents.ENTITY_GUARDIAN_DEATH_LAND;
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	public float getPathfindingFavor(BlockPos pos, WorldView world) {
		return world.getFluidState(pos).isIn(FluidTags.WATER)
				? 10.0F + world.getPhototaxisFavor(pos)
				: super.getPathfindingFavor(pos, world);
	}

	@Override
	public void tickMovement() {
		if (isAlive()) {
			if (getEntityWorld().isClient()) {
				lastTailAngle = tailAngle;

				if (!isTouchingWater()) {
						spikesExtensionRate = 2.0F;
					Vec3d velocity = getVelocity();

					if (velocity.y > 0.0 && flopping && !isSilent()) {
						getEntityWorld().playSoundClient(
								getX(), getY(), getZ(),
								getFlopSound(), getSoundCategory(),
								1.0F, 1.0F, false
						);
					}

					flopping = velocity.y < 0.0 && getEntityWorld().isTopSolid(getBlockPos().down(), this);
				} else if (areSpikesRetracted()) {
					spikesExtensionRate = spikesExtensionRate < 0.5F
							? 4.0F
							: spikesExtensionRate + (0.5F - spikesExtensionRate) * 0.1F;
				} else {
					spikesExtensionRate = spikesExtensionRate + (0.125F - spikesExtensionRate) * 0.2F;
				}

				tailAngle += spikesExtensionRate;
				lastSpikesExtension = spikesExtension;

				if (!isTouchingWater()) {
						spikesExtension = random.nextFloat();
				} else if (areSpikesRetracted()) {
					spikesExtension += (0.0F - spikesExtension) * 0.25F;
				} else {
					spikesExtension += (1.0F - spikesExtension) * 0.06F;
				}

				if (areSpikesRetracted() && isTouchingWater()) {
					Vec3d rotVec = getRotationVec(0.0F);
					for (int bubbleIndex = 0; bubbleIndex < 2; bubbleIndex++) {
						getEntityWorld().addParticleClient(
								ParticleTypes.BUBBLE,
								getParticleX(0.5) - rotVec.x * 1.5,
								getRandomBodyY() - rotVec.y * 1.5,
								getParticleZ(0.5) - rotVec.z * 1.5,
								0.0, 0.0, 0.0
						);
					}
				}

				if (hasBeamTarget()) {
					if (beamTicks < getWarmupTime()) {
						beamTicks++;
					}

					LivingEntity beamTarget = getBeamTarget();
					if (beamTarget != null) {
						getLookControl().lookAt(beamTarget, 90.0F, 90.0F);
						getLookControl().tick();
						double beamProgress = getBeamProgress(0.0F);
						double dx = beamTarget.getX() - getX();
						double dy = beamTarget.getBodyY(0.5) - getEyeY();
						double dz = beamTarget.getZ() - getZ();
						double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
						dx /= dist;
						dy /= dist;
						dz /= dist;
						double particlePos = random.nextDouble();

						while (particlePos < dist) {
							particlePos += 1.8 - beamProgress + random.nextDouble() * (1.7 - beamProgress);
							getEntityWorld().addParticleClient(
									ParticleTypes.BUBBLE,
									getX() + dx * particlePos,
									getEyeY() + dy * particlePos,
									getZ() + dz * particlePos,
									0.0, 0.0, 0.0
							);
						}
					}
				}
			}

			if (isTouchingWater()) {
				setAir(300);
			} else if (isOnGround()) {
				setVelocity(getVelocity().add(
						(random.nextFloat() * 2.0F - 1.0F) * 0.4F,
						0.5,
						(random.nextFloat() * 2.0F - 1.0F) * 0.4F
				));
				setYaw(random.nextFloat() * 360.0F);
				setOnGround(false);
				velocityDirty = true;
			}

			if (hasBeamTarget()) {
				setYaw(headYaw);
			}
		}

		super.tickMovement();
	}

	protected SoundEvent getFlopSound() {
		return SoundEvents.ENTITY_GUARDIAN_FLOP;
	}

	public float getTailAngle(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastTailAngle, tailAngle);
	}

	public float getSpikesExtension(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastSpikesExtension, spikesExtension);
	}

	public float getBeamProgress(float tickProgress) {
		return (beamTicks + tickProgress) / getWarmupTime();
	}

	public float getBeamTicks() {
		return beamTicks;
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return world.doesNotIntersectEntities(this);
	}

	public static boolean canSpawn(
			EntityType<? extends GuardianEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return (random.nextInt(20) == 0 || !world.isSkyVisibleAllowingSea(pos))
				&& world.getDifficulty() != Difficulty.PEACEFUL
				&& (SpawnReason.isAnySpawner(spawnReason) || world.getFluidState(pos).isIn(FluidTags.WATER))
				&& world.getFluidState(pos.down()).isIn(FluidTags.WATER);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (!areSpikesRetracted()
				&& !source.isIn(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)
				&& !source.isOf(DamageTypes.THORNS)
				&& source.getSource() instanceof LivingEntity attacker
		) {
			attacker.damage(world, getDamageSources().thorns(this), 2.0F);
		}

		if (wanderGoal != null) {
			wanderGoal.ignoreChanceOnce();
		}

		return super.damage(world, source, amount);
	}

	@Override
	public int getMaxLookPitchChange() {
		return 180;
	}

	@Override
	protected void travelInWater(Vec3d movementInput, double gravity, boolean falling, double y) {
		updateVelocity(0.1F, movementInput);
		move(MovementType.SELF, getVelocity());
		setVelocity(getVelocity().multiply(0.9));

		if (!areSpikesRetracted() && getTarget() == null) {
			setVelocity(getVelocity().add(0.0, -0.005, 0.0));
		}
	}

	static class FireBeamGoal extends Goal {

		private final GuardianEntity guardian;
		private int beamTicks;
		private final boolean elder;

		public FireBeamGoal(GuardianEntity guardian) {
			this.guardian = guardian;
			this.elder = guardian instanceof ElderGuardianEntity;
			this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity livingEntity = this.guardian.getTarget();
			return livingEntity != null && livingEntity.isAlive();
		}

		@Override
		public boolean shouldContinue() {
			return super.shouldContinue() && (this.elder || this.guardian.getTarget() != null
					&& this.guardian.squaredDistanceTo(this.guardian.getTarget()) > 9.0
			);
		}

		@Override
		public void start() {
			this.beamTicks = -10;
			this.guardian.getNavigation().stop();
			LivingEntity livingEntity = this.guardian.getTarget();
			if (livingEntity != null) {
				this.guardian.getLookControl().lookAt(livingEntity, 90.0F, 90.0F);
			}

			this.guardian.velocityDirty = true;
		}

		@Override
		public void stop() {
			this.guardian.setBeamTarget(0);
			this.guardian.setTarget(null);
			this.guardian.wanderGoal.ignoreChanceOnce();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity livingEntity = this.guardian.getTarget();
			if (livingEntity != null) {
				this.guardian.getNavigation().stop();
				this.guardian.getLookControl().lookAt(livingEntity, 90.0F, 90.0F);
				if (!this.guardian.canSee(livingEntity)) {
					this.guardian.setTarget(null);
				}
				else {
					this.beamTicks++;

					if (this.beamTicks == 0) {
						this.guardian.setBeamTarget(livingEntity.getId());

						if (!this.guardian.isSilent()) {
							this.guardian.getEntityWorld().sendEntityStatus(this.guardian, (byte) 21);
						}
					}
					else if (this.beamTicks >= this.guardian.getWarmupTime()) {
						float damage = 1.0F;

						if (this.guardian.getEntityWorld().getDifficulty() == Difficulty.HARD) {
							damage += 2.0F;
						}

						if (this.elder) {
							damage += 2.0F;
						}

						ServerWorld serverWorld = getServerWorld(this.guardian);
						livingEntity.damage(
								serverWorld,
								this.guardian.getDamageSources().indirectMagic(this.guardian, this.guardian),
								damage
						);
						this.guardian.tryAttack(serverWorld, livingEntity);
						this.guardian.setTarget(null);
					}

					super.tick();
				}
			}
		}
	}

	static class GuardianMoveControl extends MoveControl {

		private final GuardianEntity guardian;

		public GuardianMoveControl(GuardianEntity guardian) {
			super(guardian);
			this.guardian = guardian;
		}

		@Override
		public void tick() {
			if (this.state == MoveControl.State.MOVE_TO && !this.guardian.getNavigation().isIdle()) {
				Vec3d delta = new Vec3d(
						this.targetX - this.guardian.getX(),
						this.targetY - this.guardian.getY(),
						this.targetZ - this.guardian.getZ()
				);
				double dist = delta.length();
				double normX = delta.x / dist;
				double normY = delta.y / dist;
				double normZ = delta.z / dist;
				float targetYaw = (float) (MathHelper.atan2(delta.z, delta.x) * 180.0F / (float) Math.PI) - 90.0F;
				this.guardian.setYaw(this.wrapDegrees(this.guardian.getYaw(), targetYaw, 90.0F));
				this.guardian.bodyYaw = this.guardian.getYaw();
				float targetSpeed = (float) (this.speed * this.guardian.getAttributeValue(EntityAttributes.MOVEMENT_SPEED));
				float lerpedSpeed = MathHelper.lerp(0.125F, this.guardian.getMovementSpeed(), targetSpeed);
				this.guardian.setMovementSpeed(lerpedSpeed);
				double sineWobble = Math.sin((this.guardian.age + this.guardian.getId()) * 0.5) * 0.05;
				double cosYaw = Math.cos(this.guardian.getYaw() * (float) (Math.PI / 180.0));
				double sinYaw = Math.sin(this.guardian.getYaw() * (float) (Math.PI / 180.0));
				double cosWobble = Math.sin((this.guardian.age + this.guardian.getId()) * 0.75) * 0.05;
				this.guardian.setVelocity(this.guardian
						.getVelocity()
						.add(sineWobble * cosYaw, cosWobble * (sinYaw + cosYaw) * 0.25 + lerpedSpeed * normY * 0.1, sineWobble * sinYaw));
				LookControl lookControl = this.guardian.getLookControl();
				double lookTargetX = this.guardian.getX() + normX * 2.0;
				double lookTargetY = this.guardian.getEyeY() + normY / dist;
				double lookTargetZ = this.guardian.getZ() + normZ * 2.0;
				double currentLookX = lookControl.getLookX();
				double currentLookY = lookControl.getLookY();
				double currentLookZ = lookControl.getLookZ();

				if (!lookControl.isLookingAtSpecificPosition()) {
					currentLookX = lookTargetX;
					currentLookY = lookTargetY;
					currentLookZ = lookTargetZ;
				}

				this.guardian
						.getLookControl()
						.lookAt(
								MathHelper.lerp(0.125, currentLookX, lookTargetX),
								MathHelper.lerp(0.125, currentLookY, lookTargetY),
								MathHelper.lerp(0.125, currentLookZ, lookTargetZ),
								10.0F,
								40.0F
						);
				this.guardian.setSpikesRetracted(true);
			}
			else {
				this.guardian.setMovementSpeed(0.0F);
				this.guardian.setSpikesRetracted(false);
			}
		}
	}

	static class GuardianTargetPredicate implements TargetPredicate.EntityPredicate {

		private final GuardianEntity owner;

		public GuardianTargetPredicate(GuardianEntity owner) {
			this.owner = owner;
		}

		@Override
		public boolean test(@Nullable LivingEntity livingEntity, ServerWorld serverWorld) {
			return (livingEntity instanceof PlayerEntity || livingEntity instanceof SquidEntity
					|| livingEntity instanceof AxolotlEntity
			)
					&& livingEntity.squaredDistanceTo(this.owner) > 9.0;
		}
	}
}
