package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Блейз — огненный моб крепости Нижнего мира. Летает, стреляет очередями из трёх огненных шаров.
 * Получает урон от воды. Всегда светится (яркость 1.0). Иммунен к огню.
 */
public class BlazeEntity extends HostileEntity {

	private static final TrackedData<Byte>
			BLAZE_FLAGS =
			DataTracker.registerData(BlazeEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final float DEFAULT_EYE_OFFSET = 0.5F;
	private static final int EYE_OFFSET_RESET_INTERVAL = 100;
	private static final double EYE_OFFSET_MEAN = 0.5;
	private static final double EYE_OFFSET_SPREAD = 6.891;
	private float eyeOffset = DEFAULT_EYE_OFFSET;
	private int eyeOffsetCooldown;

	public BlazeEntity(EntityType<? extends BlazeEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.WATER, -1.0F);
		setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
		experiencePoints = 10;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(4, new BlazeEntity.ShootFireballGoal(this));
		goalSelector.add(5, new GoToWalkTargetGoal(this, 1.0));
		goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0F));
		goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	public static DefaultAttributeContainer.Builder createBlazeAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, 0.23F)
		                    .add(EntityAttributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BLAZE_FLAGS, (byte) 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_BLAZE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BLAZE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BLAZE_DEATH;
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0F;
	}

	@Override
	public void tickMovement() {
		if (!isOnGround() && getVelocity().y < 0.0) {
			setVelocity(getVelocity().multiply(1.0, 0.6, 1.0));
		}

		if (getEntityWorld().isClient()) {
			if (random.nextInt(24) == 0 && !isSilent()) {
				getEntityWorld().playSoundClient(
					getX() + 0.5,
					getY() + 0.5,
					getZ() + 0.5,
					SoundEvents.ENTITY_BLAZE_BURN,
					getSoundCategory(),
					1.0F + random.nextFloat(),
					random.nextFloat() * 0.7F + 0.3F,
					false
				);
			}

			for (int i = 0; i < 2; i++) {
				getEntityWorld().addParticleClient(
					ParticleTypes.LARGE_SMOKE,
					getParticleX(0.5),
					getRandomBodyY(),
					getParticleZ(0.5),
					0.0,
					0.0,
					0.0
				);
			}
		}

		super.tickMovement();
	}

	@Override
	public boolean hurtByWater() {
		return true;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		eyeOffsetCooldown--;

		if (eyeOffsetCooldown <= 0) {
			eyeOffsetCooldown = EYE_OFFSET_RESET_INTERVAL;
			eyeOffset = (float) random.nextTriangular(EYE_OFFSET_MEAN, EYE_OFFSET_SPREAD);
		}

		LivingEntity target = getTarget();

		if (target != null && target.getEyeY() > getEyeY() + eyeOffset && canTarget(target)) {
			Vec3d velocity = getVelocity();
			setVelocity(velocity.add(0.0, (0.3F - velocity.y) * 0.3F, 0.0));
			velocityDirty = true;
		}

		super.mobTick(world);
	}

	@Override
	public boolean isOnFire() {
		return isFireActive();
	}

	private boolean isFireActive() {
		return (dataTracker.get(BLAZE_FLAGS) & 1) != 0;
	}

	void setFireActive(boolean fireActive) {
		byte flags = dataTracker.get(BLAZE_FLAGS);
		dataTracker.set(BLAZE_FLAGS, fireActive ? (byte) (flags | 1) : (byte) (flags & -2));
	}

	static class ShootFireballGoal extends Goal {

		private final BlazeEntity blaze;
		private int fireballsFired;
		private int fireballCooldown;
		private int targetNotVisibleTicks;

		public ShootFireballGoal(BlazeEntity blaze) {
			this.blaze = blaze;
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = blaze.getTarget();

			return target != null && target.isAlive() && blaze.canTarget(target);
		}

		@Override
		public void start() {
			fireballsFired = 0;
		}

		@Override
		public void stop() {
			blaze.setFireActive(false);
			targetNotVisibleTicks = 0;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			fireballCooldown--;
			LivingEntity target = blaze.getTarget();

			if (target == null) {
				return;
			}

			boolean canSee = blaze.getVisibilityCache().canSee(target);

			if (canSee) {
				targetNotVisibleTicks = 0;
			} else {
				targetNotVisibleTicks++;
			}

			double distSq = blaze.squaredDistanceTo(target);

			if (distSq < 4.0) {
				if (!canSee) {
					return;
				}

				if (fireballCooldown <= 0) {
					fireballCooldown = 20;
					blaze.tryAttack(getServerWorld(blaze), target);
				}

				blaze.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
			} else if (distSq < getFollowRange() * getFollowRange() && canSee) {
				double dx = target.getX() - blaze.getX();
				double dy = target.getBodyY(0.5) - blaze.getBodyY(0.5);
				double dz = target.getZ() - blaze.getZ();

				if (fireballCooldown <= 0) {
					fireballsFired++;

					if (fireballsFired == 1) {
						fireballCooldown = 60;
						blaze.setFireActive(true);
					} else if (fireballsFired <= 4) {
						fireballCooldown = 6;
					} else {
						fireballCooldown = 100;
						fireballsFired = 0;
						blaze.setFireActive(false);
					}

					if (fireballsFired > 1) {
							double spread = Math.sqrt(Math.sqrt(distSq)) * 0.5;

							if (!blaze.isSilent()) {
								blaze.getEntityWorld().syncWorldEvent(null, 1018, blaze.getBlockPos(), 0);
							}

						Vec3d direction = new Vec3d(
							blaze.getRandom().nextTriangular(dx, 2.297 * spread),
							dy,
							blaze.getRandom().nextTriangular(dz, 2.297 * spread)
						);
						SmallFireballEntity fireball = new SmallFireballEntity(
							blaze.getEntityWorld(),
							blaze,
							direction.normalize()
						);
						fireball.setPosition(
							fireball.getX(),
							blaze.getBodyY(0.5) + 0.5,
							fireball.getZ()
						);
						blaze.getEntityWorld().spawnEntity(fireball);
					}
				}

				blaze.getLookControl().lookAt(target, 10.0F, 10.0F);
			} else if (targetNotVisibleTicks < 5) {
				blaze.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
			}

			super.tick();
		}

		private double getFollowRange() {
			return blaze.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
		}
	}
}
