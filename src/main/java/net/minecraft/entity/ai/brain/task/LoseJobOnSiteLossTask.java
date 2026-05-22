package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

/**
 * Фабричный класс задачи мозга, сбрасывающей профессию жителя при потере рабочего места.
 * Сбрасывает только если у жителя нет опыта и уровень профессии равен 1.
 */
public class LoseJobOnSiteLossTask {

	public static Task<VillagerEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.JOB_SITE)).apply(
						context, jobSite -> (world, entity, time) -> {
							VillagerData data = entity.getVillagerData();
							boolean hasRealJob = !data.profession().matchesKey(VillagerProfession.NONE)
									&& !data.profession().matchesKey(VillagerProfession.NITWIT);

							if (!hasRealJob || entity.getExperience() != 0 || data.level() > 1) {
								return false;
							}

							entity.setVillagerData(entity.getVillagerData().withProfession(world.getRegistryManager(), VillagerProfession.NONE));
							entity.reinitializeBrain(world);
							return true;
						}
				)
		);
	}
}
