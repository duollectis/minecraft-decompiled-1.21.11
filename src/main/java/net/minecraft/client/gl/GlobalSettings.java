package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public class GlobalSettings implements AutoCloseable {
   public static final int SIZE = new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();
   private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, SIZE);

   public void set(
      int width, int height, double glintStrength, long time, RenderTickCounter tickCounter, int menuBackgroundBlurriness, Camera camera, boolean bl
   ) {
      Vec3d vec3d = camera.getCameraPos();
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         int i = MathHelper.floor(vec3d.x);
         int j = MathHelper.floor(vec3d.y);
         int k = MathHelper.floor(vec3d.z);
         ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, SIZE)
            .putIVec3(i, j, k)
            .putVec3((float)(i - vec3d.x), (float)(j - vec3d.y), (float)(k - vec3d.z))
            .putVec2(width, height)
            .putFloat((float)glintStrength)
            .putFloat(((float)(time % 24000L) + tickCounter.getTickProgress(false)) / 24000.0F)
            .putInt(menuBackgroundBlurriness)
            .putInt(bl ? 1 : 0)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
      } catch (Throwable var18) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var17) {
               var18.addSuppressed(var17);
            }
         }

         throw var18;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      RenderSystem.setGlobalSettingsUniform(this.buffer);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
