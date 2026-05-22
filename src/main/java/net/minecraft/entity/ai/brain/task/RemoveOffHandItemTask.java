package net.minecraft.entity.ai.brain.task;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;

/**
 * Фабричный класс задачи мозга пиглина, потребляющей предмет из левой руки.
 * Срабатывает только если пиглин не восхищается предметом и в левой руке есть потребляемый предмет.
 */
public class RemoveOffHandItemTask {

	public static Task<PiglinEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.ADMIRING_ITEM)).apply(
						context, admiringItem -> (world, entity, time) -> {
							boolean hasConsumableOffhand = !entity.getOffHandStack().isEmpty()
									&& !entity.getOffHandStack().contains(DataComponentTypes.BLOCKS_ATTACKS);

							if (!hasConsumableOffhand) {
								return false;
							}

							PiglinBrain.consumeOffHandItem(world, entity, true);
							return true;
						}
				)
		);
	}
}
