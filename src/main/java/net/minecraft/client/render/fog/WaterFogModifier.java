package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WaterFogModifier extends FogModifier {
   @Override
   public void applyStartEndModifier(FogData data, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter) {
      float g = renderTickCounter.getTickProgress(false);
      data.environmentalStart = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.WATER_FOG_START_DISTANCE_VISUAL, g);
      data.environmentalEnd = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.WATER_FOG_END_DISTANCE_VISUAL, g);
      if (camera.getFocusedEntity() instanceof ClientPlayerEntity clientPlayerEntity) {
         data.environmentalEnd = data.environmentalEnd * Math.max(0.25F, clientPlayerEntity.getUnderwaterVisibility());
      }

      data.skyEnd = data.environmentalEnd;
      data.cloudEnd = data.environmentalEnd;
   }

   @Override
   public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
      return submersionType == CameraSubmersionType.WATER;
   }

   @Override
   public int getFogColor(ClientWorld world, Camera camera, int viewDistance, float skyDarkness) {
      return camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.WATER_FOG_COLOR_VISUAL, skyDarkness);
   }
}
