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
 * Цель атаки кинетическим оружием: моб сначала занимает стартовую позицию
 * на дистанции от цели, затем выполняет заряженный бросок/удар.
 * Логика состояния хранится в {@link Data}.
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
		return canAttack() && !entity.isUsingItem();
	}

	private boolean canAttack() {
		return entity.getTarget() != null
			&& entity.getMainHandStack().contains(DataComponentTypes.KINETIC_WEAPON);
	}

	private int getUseGoalTicks() {
		int useTicks = Optional
			.ofNullable(entity.getMainHandStack().get(DataComponentTypes.KINETIC_WEAPON))
			.map(KineticWeaponComponent::getUseTicks)
			.orElse(0);

		return toGoalTicks(useTicks);
	}

	@Override
	public boolean shouldContinue() {
		return data != null && !data.charged && canAttack();
	}

	@Override
	public void start() {
		super.start();
		entity.setAttacking(true);
		data = new ChargeKineticWeaponGoal.Data();
	}

	@Override
	public void stop() {
		super.stop();
		entity.getNavigation().stop();
		entity.setAttacking(false);
		data = null;
		entity.clearActiveItem();
	}

	@Override
	public void tick() {
		if (data == null) {
			return;
		}

		LivingEntity target = entity.getTarget();
		double distSq = entity.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		Entity rootVehicle = entity.getRootVehicle();
		float speedMultiplier = 1.0F;

		if (rootVehicle instanceof MobEntity mobEntity) {
			speedMultiplier = mobEntity.getRiderChargingSpeedMultiplier();
		}

		int vehicleOffset = entity.hasVehicle() ? 2 : 0;
		entity.lookAtEntity(target, 30.0F, 30.0F);
		entity.getLookControl().lookAt(target, 30.0F, 30.0F);

		if (data.isIdle()) {
			if (distSq > maxSquaredDistanceToTarget) {
				entity.getNavigation().startMovingTo(target, speedMultiplier * targetFollowingSpeed);
				return;
			}

			data.setRemainingUseTicks(getUseGoalTicks());
			entity.setCurrentHand(Hand.MAIN_HAND);
		}

		if (data.canStartCharging()) {
			entity.clearActiveItem();
			double dist = Math.sqrt(distSq);
			data.startPos = FuzzyTargeting.findFrom(
				entity,
				Math.max(0.0, MIN_ATTACK_DISTANCE + vehicleOffset - dist),
				Math.max(1.0, MAX_ATTACK_DISTANCE + vehicleOffset - dist),
				7,
				target.getEntityPos()
			);
			data.chargeTicks = 1;
		}

		if (data.finishedCharging()) {
			return;
		}

		if (data.startPos != null) {
			entity.getNavigation().startMovingTo(
				data.startPos.x,
				data.startPos.y,
				data.startPos.z,
				speedMultiplier * targetFollowingSpeed
			);

			if (entity.getNavigation().isIdle()) {
				if (data.chargeTicks > 0) {
					data.charged = true;
					return;
				}

				data.startPos = null;
			}
		} else {
			entity.getNavigation().startMovingTo(target, speedMultiplier * speed);

			if (distSq < minSquaredDistanceToTarget || entity.getNavigation().isIdle()) {
				double dist = Math.sqrt(distSq);
				data.startPos = FuzzyTargeting.findFrom(
					entity,
					MIN_CHARGE_START_DISTANCE + vehicleOffset - dist,
					MAX_CHARGE_START_DISTANCE + vehicleOffset - dist,
					7,
					target.getEntityPos()
				);
			}
		}
	}

	/**
	 * Хранит изменяемое состояние одного цикла атаки кинетическим оружием:
	 * оставшееся время зарядки, позицию старта броска и флаг завершения.
	 */
	public static class Data {

		private int remainingUseTicks = -1;
		int chargeTicks = -1;
		@Nullable Vec3d startPos;
		boolean charged = false;

		public boolean isIdle() {
			return remainingUseTicks < 0;
		}

		public void setRemainingUseTicks(int remainingUseTicks) {
			this.remainingUseTicks = remainingUseTicks;
		}

		/**
		 * Уменьшает счётчик оставшихся тиков зарядки и возвращает {@code true}
		 * в момент, когда счётчик достигает нуля — сигнал к началу броска.
		 */
		public boolean canStartCharging() {
			if (remainingUseTicks > 0) {
				remainingUseTicks--;

				if (remainingUseTicks == 0) {
					return true;
				}
			}

			return false;
		}

		/**
		 * Увеличивает счётчик тиков броска и возвращает {@code true},
		 * когда бросок завершён (превышен порог {@link ChargeKineticWeaponGoal#CHARGING_TIME_TICKS}).
		 */
		public boolean finishedCharging() {
			if (chargeTicks > 0) {
				chargeTicks++;

				if (chargeTicks > ChargeKineticWeaponGoal.CHARGING_TIME_TICKS) {
					charged = true;
					return true;
				}
			}

			return false;
		}
	}
}
