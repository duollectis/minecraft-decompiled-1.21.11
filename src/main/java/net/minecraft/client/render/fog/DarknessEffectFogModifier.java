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
public class DarknessEffectFogModifier extends StatusEffectFogModifier {
   @Override
   public RegistryEntry<StatusEffect> getStatusEffect() {
      return StatusEffects.DARKNESS;
   }

   @Override
   public void applyStartEndModifier(FogData data, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter) {
      if (camera.getFocusedEntity() instanceof LivingEntity livingEntity) {
         StatusEffectInstance statusEffectInstance = livingEntity.getStatusEffect(this.getStatusEffect());
         if (statusEffectInstance != null) {
            float g = MathHelper.lerp(statusEffectInstance.getFadeFactor(livingEntity, renderTickCounter.getTickProgress(false)), f, 15.0F);
            data.environmentalStart = g * 0.75F;
            data.environmentalEnd = g;
            data.skyEnd = g;
            data.cloudEnd = g;
         }
      }
   }

   @Override
   public float applyDarknessModifier(LivingEntity cameraEntity, float darkness, float tickProgress) {
      StatusEffectInstance statusEffectInstance = cameraEntity.getStatusEffect(this.getStatusEffect());
      return statusEffectInstance != null ? Math.max(statusEffectInstance.getFadeFactor(cameraEntity, tickProgress), darkness) : darkness;
   }
}
