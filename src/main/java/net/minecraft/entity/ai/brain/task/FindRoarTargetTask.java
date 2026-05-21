package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;

import java.util.Optional;
import java.util.function.Function;

/**
 * {@code FindRoarTargetTask}.
 */
public class FindRoarTargetTask {

	/**
	 * Create.
	 *
	 * @param targetFinder target finder
	 *
	 * @return Task — результат операции
	 */
	public static <E extends WardenEntity> Task<E> create(Function<E, Optional<? extends LivingEntity>> targetFinder) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryAbsent(MemoryModuleType.ROAR_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
				                  )
				                  .apply(
						                  context,
						                  (roarTarget, attackTarget, cantReachWalkTargetSince) -> (world, entity, time) -> {
							                  Optional<? extends LivingEntity>
									                  optional =
									                  targetFinder.apply((E) entity);
							                  if (optional.filter(entity::isValidTarget).isEmpty()) {
								                  return false;
							                  }
							                  else {
								                  roarTarget.remember(optional.get());
								                  cantReachWalkTargetSince.forget();
								                  return true;
							                  }
						                  }
				                  )
		);
	}
}
