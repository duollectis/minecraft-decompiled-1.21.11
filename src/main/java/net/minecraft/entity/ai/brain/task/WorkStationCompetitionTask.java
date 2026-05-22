package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.List;
import java.util.Optional;

/**
 * Фабричный класс задачи мозга жителя, разрешающей конкуренцию за рабочее место.
 * Среди жителей, претендующих на одну и ту же POI, сохраняет место за более опытным.
 */
public class WorkStationCompetitionTask {

	public static Task<VillagerEntity> create() {
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryValue(MemoryModuleType.JOB_SITE),
								context.queryMemoryValue(MemoryModuleType.MOBS)
						)
						.apply(
								context,
								(jobSite, mobs) -> (world, entity, time) -> {
									GlobalPos jobSitePos = context.getValue(jobSite);
									world.getPointOfInterestStorage()
									     .getType(jobSitePos.pos())
									     .ifPresent(
											     poiType -> context.<List<LivingEntity>>getValue(mobs)
											                       .stream()
											                       .filter(mob -> mob instanceof VillagerEntity && mob != entity)
											                       .map(villager -> (VillagerEntity) villager)
											                       .filter(LivingEntity::isAlive)
											                       .filter(villager -> isUsingWorkStationAt(jobSitePos, poiType, villager))
											                       .reduce(entity, WorkStationCompetitionTask::keepJobSiteForMoreExperiencedVillager)
									     );

									return true;
								}
						)
		);
	}

	private static VillagerEntity keepJobSiteForMoreExperiencedVillager(VillagerEntity first, VillagerEntity second) {
		VillagerEntity experienced = first.getExperience() > second.getExperience() ? first : second;
		VillagerEntity lessExperienced = experienced == first ? second : first;
		lessExperienced.getBrain().forget(MemoryModuleType.JOB_SITE);
		return experienced;
	}

	private static boolean isUsingWorkStationAt(
			GlobalPos pos,
			RegistryEntry<PointOfInterestType> poiType,
			VillagerEntity villager
	) {
		Optional<GlobalPos> jobSiteOpt = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
		return jobSiteOpt.isPresent()
				&& pos.equals(jobSiteOpt.get())
				&& isCompletedWorkStation(poiType, villager.getVillagerData().profession());
	}

	private static boolean isCompletedWorkStation(
			RegistryEntry<PointOfInterestType> poiType,
			RegistryEntry<VillagerProfession> profession
	) {
		return profession.value().heldWorkstation().test(poiType);
	}
}
