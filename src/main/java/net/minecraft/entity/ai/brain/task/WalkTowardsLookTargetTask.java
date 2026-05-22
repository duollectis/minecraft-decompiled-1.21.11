package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Фабричный класс задачи мозга, направляющей существо к цели взгляда, вычисленной функцией.
 * Задача активируется только если предикат выполнен и цель находится дальше {@code searchRange} блоков.
 */
public class WalkTowardsLookTargetTask {

	public static Task<LivingEntity> create(
			Function<LivingEntity, Optional<LookTarget>> lookTargetFunction,
			Predicate<LivingEntity> predicate,
			int completionRange,
			int searchRange,
			float speed
	) {
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
								context.queryMemoryOptional(MemoryModuleType.WALK_TARGET)
						)
						.apply(
								context, (lookTarget, walkTarget) -> (world, entity, time) -> {
									Optional<LookTarget> lookTargetOpt = lookTargetFunction.apply(entity);

									if (lookTargetOpt.isEmpty() || !predicate.test(entity)) {
										return false;
									}

									LookTarget resolvedTarget = lookTargetOpt.get();

									if (entity.getEntityPos().isInRange(resolvedTarget.getPos(), searchRange)) {
										return false;
									}

									lookTarget.remember(resolvedTarget);
									walkTarget.remember(new WalkTarget(resolvedTarget, speed, completionRange));
									return true;
								}
						)
		);
	}
}
