package net.minecraft.entity.ai.brain.task;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Задача мозга, реализующая фазу зарядки и броска копья.
 * Управляет позицией отскока для создания дистанции перед броском; переходит в состояние {@code RETREAT} по завершении.
 */
public class SpearChargeTask extends MultiTickTask<PathAwareEntity> {

	public static final int MIN_CHARGE_DISTANCE = 6;
	public static final int MAX_CHARGE_DISTANCE = 7;

	private final double chargeStartSpeed;
	private final double chargeSpeed;
	private final float squaredMaxDistance;
	private final float squaredChargeRange;

	public SpearChargeTask(double chargeStartSpeed, double chargeSpeed, float maxDistance, float chargeRange) {
		super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryModuleState.VALUE_PRESENT));
		this.chargeStartSpeed = chargeStartSpeed;
		this.chargeSpeed = chargeSpeed;
		squaredMaxDistance = maxDistance * maxDistance;
		squaredChargeRange = chargeRange * chargeRange;
	}

	private @Nullable LivingEntity getTarget(PathAwareEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
	}

	private boolean shouldAttack(PathAwareEntity entity) {
		return getTarget(entity) != null
				&& entity.getMainHandStack().contains(DataComponentTypes.KINETIC_WEAPON);
	}

	private int getSpearUseTicks(PathAwareEntity entity) {
		return Optional.ofNullable(entity.getMainHandStack().get(DataComponentTypes.KINETIC_WEAPON))
		               .map(KineticWeaponComponent::getUseTicks)
		               .orElse(0);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, PathAwareEntity entity) {
		return entity.getBrain()
		             .getOptionalRegisteredMemory(MemoryModuleType.SPEAR_STATUS)
		             .orElse(AdvanceState.APPROACH) == AdvanceState.CHARGING
				&& shouldAttack(entity)
				&& !entity.isUsingItem();
	}

	@Override
	protected void run(ServerWorld world, PathAwareEntity entity, long time) {
		entity.setAttacking(true);
		entity.getBrain().remember(MemoryModuleType.SPEAR_ENGAGE_TIME, getSpearUseTicks(entity));
		entity.getBrain().forget(MemoryModuleType.SPEAR_CHARGE_POSITION);
		entity.setCurrentHand(Hand.MAIN_HAND);
		super.run(world, entity, time);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) > 0
				&& shouldAttack(entity);
	}

	@Override
	protected void keepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		LivingEntity target = getTarget(entity);
		double distSq = entity.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		Entity rootVehicle = entity.getRootVehicle();
		float speedMultiplier = rootVehicle instanceof MobEntity mobEntity
				? mobEntity.getRiderChargingSpeedMultiplier()
				: 1.0F;
		int vehicleBonus = entity.hasVehicle() ? 2 : 0;

		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
		entity.getBrain().remember(
				MemoryModuleType.SPEAR_ENGAGE_TIME,
				entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_ENGAGE_TIME).orElse(0) - 1
		);

		Vec3d chargePos = entity.getBrain()
		                        .getOptionalRegisteredMemory(MemoryModuleType.SPEAR_CHARGE_POSITION)
		                        .orElse(null);

		if (chargePos != null) {
			entity.getNavigation().startMovingTo(chargePos.x, chargePos.y, chargePos.z, speedMultiplier * chargeSpeed);

			if (entity.getNavigation().isIdle()) {
				entity.getBrain().forget(MemoryModuleType.SPEAR_CHARGE_POSITION);
			}

			return;
		}

		entity.getNavigation().startMovingTo(target, speedMultiplier * chargeStartSpeed);

		if (distSq < squaredChargeRange || entity.getNavigation().isIdle()) {
			double dist = Math.sqrt(distSq);
			Vec3d newChargePos = FuzzyTargeting.findFrom(
					entity,
					MIN_CHARGE_DISTANCE + vehicleBonus - dist,
					MAX_CHARGE_DISTANCE + vehicleBonus - dist,
					MAX_CHARGE_DISTANCE,
					target.getEntityPos()
			);
			entity.getBrain().remember(MemoryModuleType.SPEAR_CHARGE_POSITION, newChargePos);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, PathAwareEntity entity, long time) {
		entity.getNavigation().stop();
		entity.clearActiveItem();
		entity.getBrain().forget(MemoryModuleType.SPEAR_CHARGE_POSITION);
		entity.getBrain().forget(MemoryModuleType.SPEAR_ENGAGE_TIME);
		entity.getBrain().remember(MemoryModuleType.SPEAR_STATUS, AdvanceState.RETREAT);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	public enum AdvanceState {
		APPROACH,
		CHARGING,
		RETREAT
	}
}
