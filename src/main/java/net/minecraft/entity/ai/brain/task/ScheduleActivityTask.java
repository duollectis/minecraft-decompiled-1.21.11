package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;

/**
 * {@code ScheduleActivityTask}.
 */
public class ScheduleActivityTask {

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(context -> context.point((world, entity, time) -> {
			entity
					.getBrain()
					.refreshActivities(world.getEnvironmentAttributes(), world.getTime(), entity.getEntityPos());
			return true;
		}));
	}
}
