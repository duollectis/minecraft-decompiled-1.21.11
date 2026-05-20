package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface RenderPass extends AutoCloseable {
   void pushDebugGroup(Supplier<String> labelGetter);

   void popDebugGroup();

   void setPipeline(RenderPipeline pipeline);

   void bindTexture(String name, @Nullable GpuTextureView gpuTextureView, @Nullable GpuSampler sampler);

   void setUniform(String name, GpuBuffer buffer);

   void setUniform(String name, GpuBufferSlice slice);

   void enableScissor(int x, int y, int width, int height);

   void disableScissor();

   void setVertexBuffer(int index, GpuBuffer buffer);

   void setIndexBuffer(GpuBuffer indexBuffer, VertexFormat.IndexType indexType);

   void drawIndexed(int baseVertex, int firstIndex, int count, int instanceCount);

   <T> void drawMultipleIndexed(
      Collection<RenderPass.RenderObject<T>> objects,
      @Nullable GpuBuffer buffer,
      VertexFormat.@Nullable IndexType indexType,
      Collection<String> validationSkippedUniforms,
      T object
   );

   void draw(int offset, int count);

   @Override
   void close();

   @Environment(EnvType.CLIENT)
   public record RenderObject<T>(
      int slot,
      GpuBuffer vertexBuffer,
      @Nullable GpuBuffer indexBuffer,
      VertexFormat.@Nullable IndexType indexType,
      int firstIndex,
      int indexCount,
      @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
   ) {
      public RenderObject(int slot, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, int firstIndex, int indexCount) {
         this(slot, vertexBuffer, indexBuffer, indexType, firstIndex, indexCount, null);
      }
   }

   @Environment(EnvType.CLIENT)
   public interface UniformUploader {
      void upload(String name, GpuBufferSlice slice);
   }
}
