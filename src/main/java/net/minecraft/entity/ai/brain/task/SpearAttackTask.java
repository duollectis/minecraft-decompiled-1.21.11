package net.minecraft.entity.ai.brain.task;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Задача мозга, управляющей фазой сближения с целью при атаке копьём.
 * Переводит существо в состояние {@code CHARGING} как только цель оказывается в радиусе атаки.
 */
public class SpearAttackTask extends MultiTickTask<PathAwareEntity> {

	private final double speed;
	private final float squaredAttackRange;

	public SpearAttackTask(double speed, float attackRange) {
		super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryModuleState.VALUE_ABSENT));
		this.speed = speed;
		squaredAttackRange = attackRange * attackRange;
	}

	private boolean canRun(PathAwareEntity entity) {
		return getAttackTarget(entity) != null
				&& entity.getMainHandStack().contains(DataComponentTypes.KINETIC_WEAPON);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, PathAwareEntity entity) {
		return canRun(entity) && !entity.isUsingItem();
	}

	@Override
	protected void run(ServerWorld world, PathAwareEntity entity, long time) {
		entity.setAttacking(true);
		entity.getBrain().remember(MemoryModuleType.SPEAR_STATUS, SpearChargeTask.AdvanceState.APPROACH);
		super.run(world, entity, time);
	}

	private @Nullable LivingEntity getAttackTarget(PathAwareEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		return canRun(entity) && isTargetOutOfRange(entity);
	}

	private boolean isTargetOutOfRange(PathAwareEntity entity) {
		LivingEntity target = getAttackTarget(entity);
		double distSq = entity.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		return distSq > squaredAttackRange;
	}

	@Override
	protected void keepRunning(ServerWorld world, PathAwareEntity entity, long time) {
		LivingEntity target = getAttackTarget(entity);
		Entity rootVehicle = entity.getRootVehicle();
		float speedMultiplier = rootVehicle instanceof MobEntity mobEntity
				? mobEntity.getRiderChargingSpeedMultiplier()
				: 1.0F;

		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
		entity.getNavigation().startMovingTo(target, speedMultiplier * speed);
	}

	@Override
	protected void finishRunning(ServerWorld world, PathAwareEntity entity, long time) {
		entity.getNavigation().stop();
		entity.getBrain().remember(MemoryModuleType.SPEAR_STATUS, SpearChargeTask.AdvanceState.CHARGING);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}
}
