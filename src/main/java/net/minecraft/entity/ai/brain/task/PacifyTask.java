package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

/**
 * Фабричный класс задачи мозга, переводящей сущность в пассивное состояние при наличии указанной памяти.
 * Устанавливает память {@code PACIFIED} и сбрасывает цель атаки.
 */
public class PacifyTask {

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
