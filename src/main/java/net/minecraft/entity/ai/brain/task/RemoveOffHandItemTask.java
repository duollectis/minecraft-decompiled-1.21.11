package net.minecraft.entity.ai.brain.task;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinEntity;

/**
 * {@code RemoveOffHandItemTask}.
 */
public class RemoveOffHandItemTask {

	public static Task<PiglinEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.ADMIRING_ITEM)).apply(
						context, admiringItem -> (world, entity, time) -> {
							if (!entity.getOffHandStack().isEmpty() && !entity
									.getOffHandStack()
									.contains(DataComponentTypes.BLOCKS_ATTACKS)) {
								PiglinBrain.consumeOffHandItem(world, entity, true);
								return true;
							}
							else {
								return false;
							}
						}
				)
		);
	}
}
