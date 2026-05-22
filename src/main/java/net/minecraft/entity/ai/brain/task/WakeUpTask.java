package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;

/**
 * Задача мозга, пробуждающая существо если оно спит вне активности REST.
 * Используется для принудительного выхода из сна при смене расписания.
 */
public class WakeUpTask {

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(context -> context.point((world, entity, time) -> {
			if (!entity.getBrain().hasActivity(Activity.REST) && entity.isSleeping()) {
				entity.wakeUp();
				return true;
			}

			return false;
		}));
	}
}
