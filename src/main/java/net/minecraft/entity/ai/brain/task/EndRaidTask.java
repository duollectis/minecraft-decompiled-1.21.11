package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.village.raid.Raid;

/**
 * Фабричный класс задачи мозга, завершающей активность рейда у сущности.
 * Проверяет состояние рейда раз в {@code CHECK_INTERVAL} тиков и сбрасывает активность на IDLE.
 */
public class EndRaidTask {

	private static final int CHECK_INTERVAL = 20;

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(context -> context.point((world, entity, time) -> {
			if (world.random.nextInt(CHECK_INTERVAL) != 0) {
				return false;
			}

			Brain<?> brain = entity.getBrain();
			Raid raid = world.getRaidAt(entity.getBlockPos());

			if (raid == null || raid.hasStopped() || raid.hasLost()) {
				brain.setDefaultActivity(Activity.IDLE);
				brain.refreshActivities(world.getEnvironmentAttributes(), world.getTime(), entity.getEntityPos());
			}

			return true;
		}));
	}
}
