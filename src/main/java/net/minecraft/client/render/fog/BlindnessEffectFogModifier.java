package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class BlindnessEffectFogModifier extends StatusEffectFogModifier {
   @Override
   public RegistryEntry<StatusEffect> getStatusEffect() {
      return StatusEffects.BLINDNESS;
   }

   @Override
   public void applyStartEndModifier(FogData data, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter) {
      if (camera.getFocusedEntity() instanceof LivingEntity livingEntity) {
         StatusEffectInstance statusEffectInstance = livingEntity.getStatusEffect(this.getStatusEffect());
         if (statusEffectInstance != null) {
            float g = statusEffectInstance.isInfinite() ? 5.0F : MathHelper.lerp(Math.min(1.0F, statusEffectInstance.getDuration() / 20.0F), f, 5.0F);
            data.environmentalStart = g * 0.25F;
            data.environmentalEnd = g;
            data.skyEnd = g * 0.8F;
            data.cloudEnd = g * 0.8F;
         }
      }
   }

   @Override
   public float applyDarknessModifier(LivingEntity cameraEntity, float darkness, float tickProgress) {
      StatusEffectInstance statusEffectInstance = cameraEntity.getStatusEffect(this.getStatusEffect());
      if (statusEffectInstance != null) {
         if (statusEffectInstance.isDurationBelow(19)) {
            darkness = Math.max(statusEffectInstance.getDuration() / 20.0F, darkness);
         } else {
            darkness = 1.0F;
         }
      }

      return darkness;
   }
}
