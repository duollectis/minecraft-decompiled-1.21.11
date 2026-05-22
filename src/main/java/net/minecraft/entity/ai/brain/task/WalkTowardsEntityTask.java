package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.function.Function;

/**
 * Фабричный класс задачи мозга детёныша, следующего за ближайшим видимым взрослым.
 * Задаёт цель ходьбы только если детёныш находится в допустимом диапазоне расстояний.
 */
public class WalkTowardsEntityTask {

	public static SingleTickTask<LivingEntity> createNearestVisibleAdult(
			UniformIntProvider executionRange,
			float speed
	) {
		return create(executionRange, entity -> speed, MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
	}

	public static SingleTickTask<LivingEntity> create(
			UniformIntProvider executionRange,
			Function<LivingEntity, Float> speed,
			MemoryModuleType<? extends LivingEntity> targetType,
			boolean eyeHeight
	) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(targetType),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (target, lookTarget, walkTarget) -> (world, entity, time) -> {
							                  if (!entity.isBaby()) {
								                  return false;
							                  }

							                  LivingEntity followTarget = context.getValue(target);
							                  boolean inOuterRange = entity.isInRange(followTarget, executionRange.getMax() + 1);
							                  boolean notInInnerRange = !entity.isInRange(followTarget, executionRange.getMin());

							                  if (inOuterRange && notInInnerRange) {
								                  WalkTarget newWalkTarget = new WalkTarget(
										                  new EntityLookTarget(followTarget, eyeHeight, eyeHeight),
										                  speed.apply(entity),
										                  executionRange.getMin() - 1
								                  );
								                  lookTarget.remember(new EntityLookTarget(followTarget, true, eyeHeight));
								                  walkTarget.remember(newWalkTarget);
								                  return true;
							                  }

							                  return false;
						                  }
				                  )
		);
	}
}
