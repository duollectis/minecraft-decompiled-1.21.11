package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.List;
import java.util.Optional;

/**
 * Фабричный класс задачи мозга жителя, захватывающей потенциальное рабочее место.
 * Если другой житель уже претендует на это место — уступает ему и направляет его туда.
 */
public class TakeJobSiteTask {

	public static Task<VillagerEntity> create(float speed) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE),
						                  context.queryMemoryAbsent(MemoryModuleType.JOB_SITE),
						                  context.queryMemoryValue(MemoryModuleType.MOBS),
						                  context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (potentialJobSite, jobSite, mobs, walkTarget, lookTarget) -> (world, entity, time) -> {
							                  if (entity.isBaby()) {
								                  return false;
							                  }

							                  if (!entity.getVillagerData().profession().matchesKey(VillagerProfession.NONE)) {
								                  return false;
							                  }

							                  BlockPos jobSitePos = context.<GlobalPos>getValue(potentialJobSite).pos();
							                  Optional<RegistryEntry<PointOfInterestType>> poiTypeOpt =
									                  world.getPointOfInterestStorage().getType(jobSitePos);

							                  if (poiTypeOpt.isEmpty()) {
								                  return true;
							                  }

							                  context.<List<LivingEntity>>getValue(mobs)
							                         .stream()
							                         .filter(mob -> mob instanceof VillagerEntity && mob != entity)
							                         .map(villager -> (VillagerEntity) villager)
							                         .filter(LivingEntity::isAlive)
							                         .filter(villager -> canUseJobSite(poiTypeOpt.get(), villager, jobSitePos))
							                         .findFirst()
							                         .ifPresent(villager -> {
								                         walkTarget.forget();
								                         lookTarget.forget();
								                         potentialJobSite.forget();

								                         if (villager.getBrain()
								                                     .getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE)
								                                     .isEmpty()) {
									                         TargetUtil.walkTowards(villager, jobSitePos, speed, 1);
									                         villager.getBrain().remember(
											                         MemoryModuleType.POTENTIAL_JOB_SITE,
											                         GlobalPos.create(world.getRegistryKey(), jobSitePos)
									                         );
									                         world.getSubscriptionTracker().onPoiUpdated(jobSitePos);
								                         }
							                         });

							                  return true;
						                  }
				                  )
		);
	}

	private static boolean canUseJobSite(
			RegistryEntry<PointOfInterestType> poiType,
			VillagerEntity villager,
			BlockPos pos
	) {
		boolean hasPotentialSite = villager.getBrain()
		                                   .getOptionalRegisteredMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
		                                   .isPresent();

		if (hasPotentialSite) {
			return false;
		}

		Optional<GlobalPos> currentJobSite = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
		RegistryEntry<VillagerProfession> profession = villager.getVillagerData().profession();

		if (!profession.value().heldWorkstation().test(poiType)) {
			return false;
		}

		return currentJobSite.isEmpty()
				? canReachJobSite(villager, pos, poiType.value())
				: currentJobSite.get().pos().equals(pos);
	}

	private static boolean canReachJobSite(PathAwareEntity entity, BlockPos pos, PointOfInterestType poiType) {
		Path path = entity.getNavigation().findPathTo(pos, poiType.searchDistance());
		return path != null && path.reachesTarget();
	}
}
