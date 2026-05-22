package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.village.raid.Raid;

/**
 * Фабричный класс задачи мозга, переключающей активность существа в режим рейда или пре-рейда.
 * Срабатывает с вероятностью 1/{@code RUN_CHANCE} при наличии активного рейда в текущей позиции.
 */
public class StartRaidTask {

	private static final int RUN_CHANCE = 20;

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(context -> context.point((world, entity, time) -> {
			if (world.random.nextInt(RUN_CHANCE) != 0) {
				return false;
			}

			Brain<?> brain = entity.getBrain();
			Raid raid = world.getRaidAt(entity.getBlockPos());

			if (raid != null) {
				boolean isActiveRaid = raid.hasSpawned() && !raid.isPreRaid();

				if (isActiveRaid) {
					brain.setDefaultActivity(Activity.RAID);
					brain.doExclusively(Activity.RAID);
				} else {
					brain.setDefaultActivity(Activity.PRE_RAID);
					brain.doExclusively(Activity.PRE_RAID);
				}
			}

			return true;
		}));
	}
}
