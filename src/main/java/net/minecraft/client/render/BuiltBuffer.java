package net.minecraft.client.render;

import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.Vec3fArray;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public class BuiltBuffer implements AutoCloseable {
   private final BufferAllocator.CloseableBuffer buffer;
   private BufferAllocator.@Nullable CloseableBuffer sortedBuffer;
   private final BuiltBuffer.DrawParameters drawParameters;

   public BuiltBuffer(BufferAllocator.CloseableBuffer buffer, BuiltBuffer.DrawParameters drawParameters) {
      this.buffer = buffer;
      this.drawParameters = drawParameters;
   }

   private static Vec3fArray collectCentroids(ByteBuffer buffer, int vertexCount, VertexFormat format) {
      int i = format.getOffset(VertexFormatElement.POSITION);
      if (i == -1) {
         throw new IllegalArgumentException("Cannot identify quad centers with no position element");
      } else {
         FloatBuffer floatBuffer = buffer.asFloatBuffer();
         int j = format.getVertexSize() / 4;
         int k = j * 4;
         int l = vertexCount / 4;
         Vec3fArray vec3fArray = new Vec3fArray(l);

         for (int m = 0; m < l; m++) {
            int n = m * k + i;
            int o = n + j * 2;
            float f = floatBuffer.get(n + 0);
            float g = floatBuffer.get(n + 1);
            float h = floatBuffer.get(n + 2);
            float p = floatBuffer.get(o + 0);
            float q = floatBuffer.get(o + 1);
            float r = floatBuffer.get(o + 2);
            float s = (f + p) / 2.0F;
            float t = (g + q) / 2.0F;
            float u = (h + r) / 2.0F;
            vec3fArray.set(m, s, t, u);
         }

         return vec3fArray;
      }
   }

   public ByteBuffer getBuffer() {
      return this.buffer.getBuffer();
   }

   public @Nullable ByteBuffer getSortedBuffer() {
      return this.sortedBuffer != null ? this.sortedBuffer.getBuffer() : null;
   }

   public BuiltBuffer.DrawParameters getDrawParameters() {
      return this.drawParameters;
   }

   public BuiltBuffer.@Nullable SortState sortQuads(BufferAllocator allocator, VertexSorter sorter) {
      if (this.drawParameters.mode() != VertexFormat.DrawMode.QUADS) {
         return null;
      } else {
         Vec3fArray vec3fArray = collectCentroids(this.buffer.getBuffer(), this.drawParameters.vertexCount(), this.drawParameters.format());
         BuiltBuffer.SortState sortState = new BuiltBuffer.SortState(vec3fArray, this.drawParameters.indexType());
         this.sortedBuffer = sortState.sortAndStore(allocator, sorter);
         return sortState;
      }
   }

   @Override
   public void close() {
      this.buffer.close();
      if (this.sortedBuffer != null) {
         this.sortedBuffer.close();
      }
   }

   @Environment(EnvType.CLIENT)
   public record DrawParameters(VertexFormat format, int vertexCount, int indexCount, VertexFormat.DrawMode mode, VertexFormat.IndexType indexType) {
   }

   @Environment(EnvType.CLIENT)
   public record SortState(Vec3fArray centroids, VertexFormat.IndexType indexType) {
      public BufferAllocator.@Nullable CloseableBuffer sortAndStore(BufferAllocator allocator, VertexSorter sorter) {
         int[] is = sorter.sort(this.centroids);
         long l = allocator.allocate(is.length * 6 * this.indexType.size);
         IntConsumer intConsumer = this.getStorer(l, this.indexType);

         for (int i : is) {
            intConsumer.accept(i * 4 + 0);
            intConsumer.accept(i * 4 + 1);
            intConsumer.accept(i * 4 + 2);
            intConsumer.accept(i * 4 + 2);
            intConsumer.accept(i * 4 + 3);
            intConsumer.accept(i * 4 + 0);
         }

         return allocator.getAllocated();
      }

      private IntConsumer getStorer(long pointer, VertexFormat.IndexType indexType) {
         MutableLong mutableLong = new MutableLong(pointer);

         return switch (indexType) {
            case SHORT -> i -> MemoryUtil.memPutShort(mutableLong.getAndAdd(2L), (short)i);
            case INT -> i -> MemoryUtil.memPutInt(mutableLong.getAndAdd(4L), i);
         };
      }
   }
}
