package net.minecraft.entity.conversion;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.scoreboard.Team;
import org.jspecify.annotations.Nullable;

public record EntityConversionContext(EntityConversionType type, boolean keepEquipment, boolean preserveCanPickUpLoot, @Nullable Team team) {
   public static EntityConversionContext create(MobEntity entity, boolean keepEquipment, boolean preserveCanPickUpLoot) {
      return new EntityConversionContext(EntityConversionType.SINGLE, keepEquipment, preserveCanPickUpLoot, entity.getScoreboardTeam());
   }

   @FunctionalInterface
   public interface Finalizer<T extends MobEntity> {
      void finalizeConversion(T convertedEntity);
   }
}
