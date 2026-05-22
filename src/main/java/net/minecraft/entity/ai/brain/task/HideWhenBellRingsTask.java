package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.village.raid.Raid;

/**
 * Фабричный класс задачи мозга, переключающей сущность в активность укрытия при звоне колокола.
 * Не переключает активность, если в деревне идёт рейд.
 */
public class HideWhenBellRingsTask {

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryValue(MemoryModuleType.HEARD_BELL_TIME)).apply(
						context, heardBellTime -> (world, entity, time) -> {
							Raid raid = world.getRaidAt(entity.getBlockPos());
							if (raid == null) {
								entity.getBrain().doExclusively(Activity.HIDE);
							}

							return true;
						}
				)
		);
	}
}
