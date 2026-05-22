package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга жителя, подтверждающей захват рабочего места.
 * При достижении потенциального места работы присваивает жителю профессию по типу POI.
 */
public class UpdateJobSiteTask {

	private static final byte STATUS_JOB_SITE_ACQUIRED = 14;

	public static Task<VillagerEntity> create() {
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE),
								context.queryMemoryOptional(MemoryModuleType.JOB_SITE)
						)
						.apply(
								context,
								(potentialJobSite, jobSite) -> (world, entity, time) -> {
									GlobalPos newJobSite = context.getValue(potentialJobSite);

									if (!newJobSite.pos().isWithinDistance(entity.getEntityPos(), 2.0)
											&& !entity.isNatural()) {
										return false;
									}

									potentialJobSite.forget();
									jobSite.remember(newJobSite);
									world.sendEntityStatus(entity, STATUS_JOB_SITE_ACQUIRED);

									if (!entity.getVillagerData().profession().matchesKey(VillagerProfession.NONE)) {
										return true;
									}

									MinecraftServer server = world.getServer();
									Optional.ofNullable(server.getWorld(newJobSite.dimension()))
									        .flatMap(jobSiteWorld -> jobSiteWorld
											        .getPointOfInterestStorage()
											        .getType(newJobSite.pos()))
									        .flatMap(
											        poiType -> Registries.VILLAGER_PROFESSION
													        .streamEntries()
													        .filter(profession -> profession
															        .value()
															        .heldWorkstation()
															        .test((RegistryEntry<PointOfInterestType>) poiType))
													        .findFirst()
									        )
									        .ifPresent(profession -> {
										        entity.setVillagerData(entity.getVillagerData().withProfession(profession));
										        entity.reinitializeBrain(world);
									        });

									return true;
								}
						)
		);
	}
}
