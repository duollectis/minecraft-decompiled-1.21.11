package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PiglinBrain;

/**
 * Фабричный класс задачи мозга пиглина, фиксирующей успешную охоту на хоглина.
 * Устанавливает память {@code HUNTED_RECENTLY} на случайное время из диапазона {@code HUNT_MEMORY_DURATION}.
 */
public class HuntFinishTask {

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
								context.queryMemoryOptional(MemoryModuleType.HUNTED_RECENTLY)
						)
						.apply(
								context, (attackTarget, huntedRecently) -> (world, entity, time) -> {
									LivingEntity target = context.getValue(attackTarget);

									if (target.getType() == EntityType.HOGLIN && target.isDead()) {
										huntedRecently.remember(
												true,
												PiglinBrain.HUNT_MEMORY_DURATION.get(entity.getEntityWorld().random)
										);
									}

									return true;
								}
						)
		);
	}
}
