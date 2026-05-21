package net.minecraft.entity.ai.goal;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * {@code ChargeKineticWeaponGoal}.
 */
public class ChargeKineticWeaponGoal<T extends HostileEntity> extends Goal {

	static final int MIN_CHARGE_START_DISTANCE = 6;
	static final int MAX_CHARGE_START_DISTANCE = 7;
	static final int MIN_ATTACK_DISTANCE = 9;
	static final int MAX_ATTACK_DISTANCE = 11;
	static final double CHARGING_TIME_TICKS = toGoalTicks(100);
	private final T entity;
	private ChargeKineticWeaponGoal.@Nullable Data data;
	double speed;
	double targetFollowingSpeed;
	float maxSquaredDistanceToTarget;
	float minSquaredDistanceToTarget;

	public ChargeKineticWeaponGoal(
			T entity,
			double speed,
			double targetFollowingSpeed,
			float maxDistanceToTarget,
			float minDistanceToTarget
	) {
		this.entity = entity;
		this.speed = speed;
		this.targetFollowingSpeed = targetFollowingSpeed;
		this.maxSquaredDistanceToTarget = maxDistanceToTarget * maxDistanceToTarget;
		this.minSquaredDistanceToTarget = minDistanceToTarget * minDistanceToTarget;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return this.canAttack() && !this.entity.isUsingItem();
	}

	private boolean canAttack() {
		return this.entity.getTarget() != null && this.entity
				.getMainHandStack()
				.contains(DataComponentTypes.KINETIC_WEAPON);
	}

	private int getUseGoalTicks() {
		int
				i =
				Optional
						.ofNullable(this.entity.getMainHandStack().get(DataComponentTypes.KINETIC_WEAPON))
						.map(KineticWeaponComponent::getUseTicks)
						.orElse(0);
		return toGoalTicks(i);
	}

	@Override
	public boolean shouldContinue() {
		return this.data != null && !this.data.charged && this.canAttack();
	}

	@Override
	public void start() {
		super.start();
		this.entity.setAttacking(true);
		this.data = new ChargeKineticWeaponGoal.Data();
	}

	@Override
	public void stop() {
		super.stop();
		this.entity.getNavigation().stop();
		this.entity.setAttacking(false);
		this.data = null;
		this.entity.clearActiveItem();
	}

	@Override
	public void tick() {
		if (this.data != null) {
			LivingEntity livingEntity = this.entity.getTarget();
			double d = this.entity.squaredDistanceTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
			Entity entity = this.entity.getRootVehicle();
			float f = 1.0F;
			if (entity instanceof MobEntity mobEntity) {
				f = mobEntity.getRiderChargingSpeedMultiplier();
			}

			int i = this.entity.hasVehicle() ? 2 : 0;
			this.entity.lookAtEntity(livingEntity, 30.0F, 30.0F);
			this.entity.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
			if (this.data.isIdle()) {
				if (d > this.maxSquaredDistanceToTarget) {
					this.entity.getNavigation().startMovingTo(livingEntity, f * this.targetFollowingSpeed);
					return;
				}

				this.data.setRemainingUseTicks(this.getUseGoalTicks());
				this.entity.setCurrentHand(Hand.MAIN_HAND);
			}

			if (this.data.canStartCharging()) {
				this.entity.clearActiveItem();
				double e = Math.sqrt(d);
				this.data.startPos =
						FuzzyTargeting.findFrom(
								this.entity,
								Math.max(0.0, 9 + i - e),
								Math.max(1.0, 11 + i - e),
								7,
								livingEntity.getEntityPos()
						);
				this.data.chargeTicks = 1;
			}

			if (!this.data.finishedCharging()) {
				if (this.data.startPos != null) {
					this.entity
							.getNavigation()
							.startMovingTo(
									this.data.startPos.x,
									this.data.startPos.y,
									this.data.startPos.z,
									f * this.targetFollowingSpeed
							);
					if (this.entity.getNavigation().isIdle()) {
						if (this.data.chargeTicks > 0) {
							this.data.charged = true;
							return;
						}

						this.data.startPos = null;
					}
				}
				else {
					this.entity.getNavigation().startMovingTo(livingEntity, f * this.speed);
					if (d < this.minSquaredDistanceToTarget || this.entity.getNavigation().isIdle()) {
						double e = Math.sqrt(d);
						this.data.startPos =
								FuzzyTargeting.findFrom(
										this.entity,
										6 + i - e,
										7 + i - e,
										7,
										livingEntity.getEntityPos()
								);
					}
				}
			}
		}
	}

	/**
	 * {@code Data}.
	 */
	public static class Data {

		private int remainingUseTicks = -1;
		int chargeTicks = -1;
		@Nullable Vec3d startPos;
		boolean charged = false;

		public boolean isIdle() {
			return this.remainingUseTicks < 0;
		}

		public void setRemainingUseTicks(int remainingUseTicks) {
			this.remainingUseTicks = remainingUseTicks;
		}

		public boolean canStartCharging() {
			if (this.remainingUseTicks > 0) {
				this.remainingUseTicks--;
				if (this.remainingUseTicks == 0) {
					return true;
				}
			}

			return false;
		}

		public boolean finishedCharging() {
			if (this.chargeTicks > 0) {
				this.chargeTicks++;
				if (this.chargeTicks > ChargeKineticWeaponGoal.CHARGING_TIME_TICKS) {
					this.charged = true;
					return true;
				}
			}

			return false;
		}
	}
}
