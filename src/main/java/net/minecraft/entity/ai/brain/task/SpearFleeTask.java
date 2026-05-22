package net.minecraft.entity.ai.brain.task;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Задача мозга, реализующая фазу отступления после броска копья.
 * Ищет позицию для отхода от цели и движется к ней; по завершении сбрасывает состояние копья.
 */
public class SpearFleeTask extends MultiTickTask<PathAwareEntity> {

	public static final int MIN_FLEE_DISTANCE = 9;
	public static final int MAX_FLEE_DISTANCE = 11;
	public static final int RUN_TIME = 100;

	private final double speed;

	public SpearFleeTask(double speedFactor) {
		super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryModuleState.VALUE_PRESENT), RUN_TIME);
		speed = speedFactor;
	}

	private @Nullable LivingEntity getAttackTarget(PathAwareEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
	}

	private boolean shouldAttack(PathAwareEntity entity) {
		return getAttackTarget(entity) != null
				&& entity.getMainHandStack().contains(DataComponentTypes.KINETIC_WEAPON);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, PathAwareEntity entity) {
		if (!shouldAttack(entity) || entity.isUsingItem()) {
			return false;
		}

		boolean isRetreating = entity.getBrain()
		                             .getOptionalRegisteredMemory(MemoryModuleType.SPEAR_STATUS)
		                             .orElse(SpearChargeTask.AdvanceState.APPROACH) == SpearChargeTask.AdvanceState.RETREAT;

		if (!isRetreating) {
			return false;
		}

		LivingEntity target = getAttackTarget(entity);
		double distSq = entity.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		int vehicleBonus = entity.hasVehicle() ? 2 : 0;
		double dist = Math.sqrt(distSq);

		Vec3d fleePos = FuzzyTargeting.findFrom(
				entity,
				Math.max(0.0, MIN_FLEE_DISTANCE + vehicleBonus - dist),
				Math.max(1.0, MAX_FLEE_DISTANCE + vehicleBonus - dist),
				MAX_FLEE_DISTANCE,
				target.getEntityPos()
		);

		if (fleePos == null) {
			return false;
		}

		entity.getBrain().remember(MemoryModuleType.SPEAR_FLEEING_POSITION, fleePos);
		return true;
	}

	@Override
	protected void run(ServerWorld world, PathAwareEntity entity, long time) {
		entity.setAttacking(true);
		entity.getBrain().remember(MemoryModuleType.SPEAR_FLEEING_TIME, 0);
		super.run(world, entity, time);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(RUN_TIME) < RUN_TIME
				&& entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_POSITION).isPresent()
				&& !entity.getNavigation().isIdle()
				&& shouldAttack(entity);
	}

	@Override
	protected void keepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		LivingEntity target = getAttackTarget(entity);
		float speedMultiplier = entity.getRootVehicle() instanceof MobEntity mobEntity
				? mobEntity.getRiderChargingSpeedMultiplier()
				: 1.0F;

		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
		entity.getBrain().remember(
				MemoryModuleType.SPEAR_FLEEING_TIME,
				entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_TIME).orElse(0) + 1
		);
		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.SPEAR_FLEEING_POSITION)
		      .ifPresent(pos -> entity.getNavigation().startMovingTo(pos.x, pos.y, pos.z, speedMultiplier * speed));
	}

	@Override
	protected void finishRunning(ServerWorld world, PathAwareEntity entity, long time) {
		entity.getNavigation().stop();
		entity.setAttacking(false);
		entity.clearActiveItem();
		entity.getBrain().forget(MemoryModuleType.SPEAR_FLEEING_TIME);
		entity.getBrain().forget(MemoryModuleType.SPEAR_FLEEING_POSITION);
		entity.getBrain().forget(MemoryModuleType.SPEAR_STATUS);
	}
}
