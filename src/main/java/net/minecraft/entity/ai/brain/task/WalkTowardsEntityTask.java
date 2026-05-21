package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.function.Function;

/**
 * {@code WalkTowardsEntityTask}.
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
							                  else {
								                  LivingEntity livingEntity = context.getValue(target);
								                  if (entity.isInRange(livingEntity, executionRange.getMax() + 1)
										                  && !entity.isInRange(livingEntity, executionRange.getMin())) {
									                  WalkTarget walkTargetx = new WalkTarget(
											                  new EntityLookTarget(livingEntity, eyeHeight, eyeHeight),
											                  speed.apply(entity),
											                  executionRange.getMin() - 1
									                  );
									                  lookTarget.remember(new EntityLookTarget(
											                  livingEntity,
											                  true,
											                  eyeHeight
									                  ));
									                  walkTarget.remember(walkTargetx);
									                  return true;
								                  }
								                  else {
									                  return false;
								                  }
							                  }
						                  }
				                  )
		);
	}
}
