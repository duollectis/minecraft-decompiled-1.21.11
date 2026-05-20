package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraOverride;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.biome.Biome;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class AtmosphericFogModifier extends FogModifier {
   private static final int field_60795 = 8;
   private static final float field_60587 = -160.0F;
   private static final float field_60588 = -256.0F;
   private float fogMultiplier;

   @Override
   public int getFogColor(ClientWorld world, Camera camera, int viewDistance, float skyDarkness) {
      int i = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.FOG_COLOR_VISUAL, skyDarkness);
      if (viewDistance >= 4) {
         float f = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SUN_ANGLE_VISUAL, skyDarkness) * (float) (Math.PI / 180.0);
         float g = MathHelper.sin(f) > 0.0F ? -1.0F : 1.0F;
         CameraOverride cameraOverride = MinecraftClient.getInstance().gameRenderer.getCameraOverride();
         Vector3fc vector3fc = cameraOverride != null ? cameraOverride.forwardVector() : camera.getHorizontalPlane();
         float h = vector3fc.dot(g, 0.0F, 0.0F);
         if (h > 0.0F) {
            int j = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL, skyDarkness);
            float k = ColorHelper.getAlphaFloat(j);
            if (k > 0.0F) {
               i = ColorHelper.lerp(h * k, i, ColorHelper.fullAlpha(j));
            }
         }
      }

      int l = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SKY_COLOR_VISUAL, skyDarkness);
      l = method_76556(l, world.getRainGradient(skyDarkness), world.getThunderGradient(skyDarkness));
      float g = Math.min(
         camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SKY_FOG_END_DISTANCE_VISUAL, skyDarkness) / 16.0F, (float)viewDistance
      );
      float m = MathHelper.clampedLerp(g / 32.0F, 0.25F, 1.0F);
      m = 1.0F - (float)Math.pow(m, 0.25);
      return ColorHelper.lerp(m, i, l);
   }

   private static int method_76556(int i, float f, float g) {
      if (f > 0.0F) {
         float h = 1.0F - f * 0.5F;
         float j = 1.0F - f * 0.4F;
         i = ColorHelper.scaleRgb(i, h, h, j);
      }

      if (g > 0.0F) {
         i = ColorHelper.scaleRgb(i, 1.0F - g * 0.5F);
      }

      return i;
   }

   @Override
   public void applyStartEndModifier(FogData data, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter) {
      this.method_76304(camera, clientWorld, renderTickCounter);
      float g = renderTickCounter.getTickProgress(false);
      data.environmentalStart = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.FOG_START_DISTANCE_VISUAL, g);
      data.environmentalEnd = camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.FOG_END_DISTANCE_VISUAL, g);
      data.environmentalStart = data.environmentalStart + -160.0F * this.fogMultiplier;
      float h = Math.min(96.0F, data.environmentalEnd);
      data.environmentalEnd = Math.max(h, data.environmentalEnd + -256.0F * this.fogMultiplier);
      data.skyEnd = Math.min(f, camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.SKY_FOG_END_DISTANCE_VISUAL, g));
      data.cloudEnd = Math.min(
         (float)(MinecraftClient.getInstance().options.getCloudRenderDistance().getValue() * 16),
         camera.getEnvironmentAttributeInterpolator().get(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE_VISUAL, g)
      );
      if (MinecraftClient.getInstance().inGameHud.getBossBarHud().shouldThickenFog()) {
         data.environmentalStart = Math.min(data.environmentalStart, 10.0F);
         data.environmentalEnd = Math.min(data.environmentalEnd, 96.0F);
         data.skyEnd = data.environmentalEnd;
         data.cloudEnd = data.environmentalEnd;
      }
   }

   private void method_76304(Camera camera, ClientWorld clientWorld, RenderTickCounter renderTickCounter) {
      BlockPos blockPos = camera.getBlockPos();
      Biome biome = clientWorld.getBiome(blockPos).value();
      float f = renderTickCounter.getDynamicDeltaTicks();
      float g = renderTickCounter.getTickProgress(false);
      boolean bl = biome.hasPrecipitation();
      float h = MathHelper.clamp((clientWorld.getLightingProvider().get(LightType.SKY).getLightLevel(blockPos) - 8.0F) / 7.0F, 0.0F, 1.0F);
      float i = clientWorld.getRainGradient(g) * h * (bl ? 1.0F : 0.5F);
      this.fogMultiplier = this.fogMultiplier + (i - this.fogMultiplier) * f * 0.2F;
   }

   @Override
   public boolean shouldApply(@Nullable CameraSubmersionType submersionType, Entity cameraEntity) {
      return submersionType == CameraSubmersionType.ATMOSPHERIC;
   }
}
