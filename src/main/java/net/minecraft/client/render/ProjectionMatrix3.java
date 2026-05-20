package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public class ProjectionMatrix3 implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice slice;
   private final float nearZ;
   private final float farZ;
   private int width;
   private int height;
   private float fov;

   public ProjectionMatrix3(String name, float nearZ, float farZ) {
      this.nearZ = nearZ;
      this.farZ = farZ;
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.buffer = gpuDevice.createBuffer(() -> "Projection matrix UBO " + name, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.slice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice set(int width, int height, float fov) {
      if (this.width != width || this.height != height || this.fov != fov) {
         Matrix4f matrix4f = this.getMatrix(width, height, fov);
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
         } catch (Throwable var9) {
            if (memoryStack != null) {
               try {
                  memoryStack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }

         this.width = width;
         this.height = height;
         this.fov = fov;
      }

      return this.slice;
   }

   private Matrix4f getMatrix(int width, int height, float fov) {
      return new Matrix4f().perspective(fov * (float) (Math.PI / 180.0), (float)width / height, this.nearZ, this.farZ);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
