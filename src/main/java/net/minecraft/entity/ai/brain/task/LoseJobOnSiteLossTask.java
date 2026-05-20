package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

public class LoseJobOnSiteLossTask {
   public static Task<VillagerEntity> create() {
      return TaskTriggerer.task(
         context -> context.group(context.queryMemoryAbsent(MemoryModuleType.JOB_SITE)).apply(context, jobSite -> (world, entity, time) -> {
            VillagerData villagerData = entity.getVillagerData();
            boolean bl = !villagerData.profession().matchesKey(VillagerProfession.NONE) && !villagerData.profession().matchesKey(VillagerProfession.NITWIT);
            if (bl && entity.getExperience() == 0 && villagerData.level() <= 1) {
               entity.setVillagerData(entity.getVillagerData().withProfession(world.getRegistryManager(), VillagerProfession.NONE));
               entity.reinitializeBrain(world);
               return true;
            } else {
               return false;
            }
         })
      );
   }
}
