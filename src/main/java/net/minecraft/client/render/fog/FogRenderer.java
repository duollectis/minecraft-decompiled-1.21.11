package net.minecraft.client.render.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public class FogRenderer implements AutoCloseable {
   public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
   private static final List<FogModifier> FOG_MODIFIERS = Lists.newArrayList(
      new FogModifier[]{
         new LavaFogModifier(),
         new PowderSnowFogModifier(),
         new BlindnessEffectFogModifier(),
         new DarknessEffectFogModifier(),
         new WaterFogModifier(),
         new AtmosphericFogModifier()
      }
   );
   private static boolean fogEnabled = true;
   private final GpuBuffer emptyBuffer;
   private final MappableRingBuffer fogBuffer;

   public FogRenderer() {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.fogBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = memoryStack.malloc(FOG_UBO_SIZE);
         this.applyFog(byteBuffer, 0, new Vector4f(0.0F), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
         this.emptyBuffer = gpuDevice.createBuffer(() -> "Empty fog", 128, byteBuffer.flip());
      } catch (Throwable var6) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      RenderSystem.setShaderFog(this.getFogBuffer(FogRenderer.FogType.NONE));
   }

   @Override
   public void close() {
      this.emptyBuffer.close();
      this.fogBuffer.close();
   }

   public void rotate() {
      this.fogBuffer.rotate();
   }

   public GpuBufferSlice getFogBuffer(FogRenderer.FogType fogType) {
      if (!fogEnabled) {
         return this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
      } else {
         return switch (fogType) {
            case NONE -> this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
            case WORLD -> this.fogBuffer.getBlocking().slice(0L, FOG_UBO_SIZE);
         };
      }
   }

   private Vector4f getFogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness) {
      CameraSubmersionType cameraSubmersionType = this.getCameraSubmersionType(camera);
      Entity entity = camera.getFocusedEntity();
      FogModifier fogModifier = null;
      FogModifier fogModifier2 = null;

      for (FogModifier fogModifier3 : FOG_MODIFIERS) {
         if (fogModifier3.shouldApply(cameraSubmersionType, entity)) {
            if (fogModifier == null && fogModifier3.isColorSource()) {
               fogModifier = fogModifier3;
            }

            if (fogModifier2 == null && fogModifier3.isDarknessModifier()) {
               fogModifier2 = fogModifier3;
            }
         }
      }

      if (fogModifier == null) {
         throw new IllegalStateException("No color source environment found");
      } else {
         int i = fogModifier.getFogColor(world, camera, viewDistance, tickProgress);
         float f = world.getLevelProperties().getVoidDarknessRange();
         float g = MathHelper.clamp((f + world.getBottomY() - (float)camera.getCameraPos().y) / f, 0.0F, 1.0F);
         if (fogModifier2 != null) {
            LivingEntity livingEntity = (LivingEntity)entity;
            g = fogModifier2.applyDarknessModifier(livingEntity, g, tickProgress);
         }

         float h = ColorHelper.getRedFloat(i);
         float j = ColorHelper.getGreenFloat(i);
         float k = ColorHelper.getBlueFloat(i);
         if (g > 0.0F && cameraSubmersionType != CameraSubmersionType.LAVA && cameraSubmersionType != CameraSubmersionType.POWDER_SNOW) {
            float l = MathHelper.square(1.0F - g);
            h *= l;
            j *= l;
            k *= l;
         }

         if (skyDarkness > 0.0F) {
            h = MathHelper.lerp(skyDarkness, h, h * 0.7F);
            j = MathHelper.lerp(skyDarkness, j, j * 0.6F);
            k = MathHelper.lerp(skyDarkness, k, k * 0.6F);
         }

         float l;
         if (cameraSubmersionType == CameraSubmersionType.WATER) {
            if (entity instanceof ClientPlayerEntity) {
               l = ((ClientPlayerEntity)entity).getUnderwaterVisibility();
            } else {
               l = 1.0F;
            }
         } else if (entity instanceof LivingEntity livingEntity2
            && livingEntity2.hasStatusEffect(StatusEffects.NIGHT_VISION)
            && !livingEntity2.hasStatusEffect(StatusEffects.DARKNESS)) {
            l = GameRenderer.getNightVisionStrength(livingEntity2, tickProgress);
         } else {
            l = 0.0F;
         }

         if (h != 0.0F && j != 0.0F && k != 0.0F) {
            float m = 1.0F / Math.max(h, Math.max(j, k));
            h = MathHelper.lerp(l, h, h * m);
            j = MathHelper.lerp(l, j, j * m);
            k = MathHelper.lerp(l, k, k * m);
         }

         return new Vector4f(h, j, k, 1.0F);
      }
   }

   public static boolean toggleFog() {
      return fogEnabled = !fogEnabled;
   }

   public Vector4f applyFog(Camera camera, int viewDistance, RenderTickCounter renderTickCounter, float f, ClientWorld clientWorld) {
      float g = renderTickCounter.getTickProgress(false);
      Vector4f vector4f = this.getFogColor(camera, g, clientWorld, viewDistance, f);
      float h = viewDistance * 16;
      CameraSubmersionType cameraSubmersionType = this.getCameraSubmersionType(camera);
      Entity entity = camera.getFocusedEntity();
      FogData fogData = new FogData();

      for (FogModifier fogModifier : FOG_MODIFIERS) {
         if (fogModifier.shouldApply(cameraSubmersionType, entity)) {
            fogModifier.applyStartEndModifier(fogData, camera, clientWorld, h, renderTickCounter);
            break;
         }
      }

      float i = MathHelper.clamp(h / 10.0F, 4.0F, 64.0F);
      fogData.renderDistanceStart = h - i;
      fogData.renderDistanceEnd = h;

      try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.fogBuffer.getBlocking(), false, true)) {
         this.applyFog(
            mappedView.data(),
            0,
            vector4f,
            fogData.environmentalStart,
            fogData.environmentalEnd,
            fogData.renderDistanceStart,
            fogData.renderDistanceEnd,
            fogData.skyEnd,
            fogData.cloudEnd
         );
      }

      return vector4f;
   }

   private CameraSubmersionType getCameraSubmersionType(Camera camera) {
      CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
      return cameraSubmersionType == CameraSubmersionType.NONE ? CameraSubmersionType.ATMOSPHERIC : cameraSubmersionType;
   }

   private void applyFog(
      ByteBuffer buffer,
      int bufPos,
      Vector4f fogColor,
      float environmentalStart,
      float environmentalEnd,
      float renderDistanceStart,
      float renderDistanceEnd,
      float skyEnd,
      float cloudEnd
   ) {
      buffer.position(bufPos);
      Std140Builder.intoBuffer(buffer)
         .putVec4(fogColor)
         .putFloat(environmentalStart)
         .putFloat(environmentalEnd)
         .putFloat(renderDistanceStart)
         .putFloat(renderDistanceEnd)
         .putFloat(skyEnd)
         .putFloat(cloudEnd);
   }

   @Environment(EnvType.CLIENT)
   public static enum FogType {
      NONE,
      WORLD;
   }
}
