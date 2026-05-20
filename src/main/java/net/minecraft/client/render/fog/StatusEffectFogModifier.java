package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class StatusEffectFogModifier extends FogModifier {
   public abstract RegistryEntry<StatusEffect> getStatusEffect();

   @Override
   public boolean isColorSource() {
      return false;
   }

   @Override
   public boolean isDarknessModifier() {
      return true;
   }

   @Override
   public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
      return cameraEntity instanceof LivingEntity livingEntity && livingEntity.hasStatusEffect(this.getStatusEffect());
   }
}
