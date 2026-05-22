package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

/**
 * Фабричный класс задачи мозга, прекращающей панику существа.
 * Если нет источника урона, враждебных существ и атакующего в радиусе 6 блоков — сбрасывает панику и обновляет расписание.
 */
public class StopPanickingTask {

	private static final double MAX_DISTANCE_SQ = 36.0;

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.HURT_BY),
						                  context.queryMemoryOptional(MemoryModuleType.HURT_BY_ENTITY),
						                  context.queryMemoryOptional(MemoryModuleType.NEAREST_HOSTILE)
				                  )
				                  .apply(
						                  context,
						                  (hurtBy, hurtByEntity, nearestHostile) -> (world, entity, time) -> {
							                  boolean shouldPanic = context.getOptionalValue(hurtBy).isPresent()
									                  || context.getOptionalValue(nearestHostile).isPresent()
									                  || context
									                  .<LivingEntity>getOptionalValue(hurtByEntity)
									                  .filter(attacker -> attacker.squaredDistanceTo(entity) <= MAX_DISTANCE_SQ)
									                  .isPresent();

							                  if (shouldPanic) {
								                  return true;
							                  }

							                  hurtBy.forget();
							                  hurtByEntity.forget();
							                  entity
									                  .getBrain()
									                  .refreshActivities(
											                  world.getEnvironmentAttributes(),
											                  world.getTime(),
											                  entity.getEntityPos()
									                  );

							                  return true;
						                  }
				                  )
		);
	}
}
