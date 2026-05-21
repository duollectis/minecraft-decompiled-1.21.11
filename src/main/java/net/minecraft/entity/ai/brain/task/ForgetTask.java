package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.function.Predicate;

/**
 * {@code ForgetTask}.
 */
public class ForgetTask {

	public static <E extends LivingEntity> Task<E> create(Predicate<E> condition, MemoryModuleType<?> memory) {
		return TaskTriggerer.task(context -> context.group(context.queryMemoryValue(memory)).apply(
				context, queryResult -> (world, entity, time) -> {
					if (condition.test((E) entity)) {
						queryResult.forget();
						return true;
					}
					else {
						return false;
					}
				}
		));
	}
}
