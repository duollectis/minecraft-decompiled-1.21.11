package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;

/**
 * Фабричный класс задачи мозга, начинающей верховую езду на целевом транспорте.
 * Если транспорт в радиусе 1 блока — садится на него; иначе — идёт к нему.
 */
public class StartRidingTask {

	private static final int COMPLETION_RANGE = 1;

	public static Task<LivingEntity> create(float speed) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.RIDE_TARGET)
				                  )
				                  .apply(
						                  context, (lookTarget, walkTarget, rideTarget) -> (world, entity, time) -> {
							                  if (entity.hasVehicle()) {
								                  return false;
							                  }

							                  Entity rideVehicle = context.getValue(rideTarget);

							                  if (rideVehicle.isInRange(entity, 1.0)) {
								                  entity.startRiding(rideVehicle);
							                  } else {
								                  lookTarget.remember(new EntityLookTarget(rideVehicle, true));
								                  walkTarget.remember(new WalkTarget(
										                  new EntityLookTarget(rideVehicle, false),
										                  speed,
										                  COMPLETION_RANGE
								                  ));
							                  }

							                  return true;
						                  }
				                  )
		);
	}
}
