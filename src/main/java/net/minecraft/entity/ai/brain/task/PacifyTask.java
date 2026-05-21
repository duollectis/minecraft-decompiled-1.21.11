package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

/**
 * {@code PacifyTask}.
 */
public class PacifyTask {

	/**
	 * Create.
	 *
	 * @param requiredMemory required memory
	 * @param duration duration
	 *
	 * @return Task — результат операции
	 */
	public static Task<LivingEntity> create(MemoryModuleType<?> requiredMemory, int duration) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.PACIFIED),
						                  context.queryMemoryValue(requiredMemory)
				                  )
				                  .apply(
						                  context,
						                  context.supply(
								                  () -> "[BecomePassive if " + requiredMemory + " present]",
								                  (attackTarget, pacified, requiredMemoryResult) -> (world, entity, time) -> {
									                  pacified.remember(true, duration);
									                  attackTarget.forget();
									                  return true;
								                  }
						                  )
				                  )
		);
	}
}
