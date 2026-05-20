package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuDeviceInfo;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class VertexFormat {
   public static final int UNKNOWN_ELEMENT = -1;
   private final List<VertexFormatElement> elements;
   private final List<String> names;
   private final int vertexSize;
   private final int elementsMask;
   private final int[] offsetsByElement = new int[32];
   private @Nullable GpuBuffer immediateDrawVertexBuffer;
   private @Nullable GpuBuffer immediateDrawIndexBuffer;

   VertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
      this.elements = elements;
      this.names = names;
      this.vertexSize = vertexSize;
      this.elementsMask = elements.stream().mapToInt(VertexFormatElement::mask).reduce(0, (a, b) -> a | b);

      for (int i = 0; i < this.offsetsByElement.length; i++) {
         VertexFormatElement vertexFormatElement = VertexFormatElement.byId(i);
         int j = vertexFormatElement != null ? elements.indexOf(vertexFormatElement) : -1;
         this.offsetsByElement[i] = j != -1 ? offsets.getInt(j) : -1;
      }
   }

   public static VertexFormat.Builder builder() {
      return new VertexFormat.Builder();
   }

   @Override
   public String toString() {
      return "VertexFormat" + this.names;
   }

   public int getVertexSize() {
      return this.vertexSize;
   }

   public List<VertexFormatElement> getElements() {
      return this.elements;
   }

   public List<String> getElementAttributeNames() {
      return this.names;
   }

   public int[] getOffsetsByElement() {
      return this.offsetsByElement;
   }

   public int getOffset(VertexFormatElement element) {
      return this.offsetsByElement[element.id()];
   }

   public boolean contains(VertexFormatElement element) {
      return (this.elementsMask & element.mask()) != 0;
   }

   public int getElementsMask() {
      return this.elementsMask;
   }

   public String getElementName(VertexFormatElement element) {
      int i = this.elements.indexOf(element);
      if (i == -1) {
         throw new IllegalArgumentException(element + " is not contained in format");
      } else {
         return this.names.get(i);
      }
   }

   @Override
   public boolean equals(Object o) {
      return this == o
         ? true
         : o instanceof VertexFormat vertexFormat
            && this.elementsMask == vertexFormat.elementsMask
            && this.vertexSize == vertexFormat.vertexSize
            && this.names.equals(vertexFormat.names)
            && Arrays.equals(this.offsetsByElement, vertexFormat.offsetsByElement);
   }

   @Override
   public int hashCode() {
      return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
   }

   private static GpuBuffer uploadToBuffer(@Nullable GpuBuffer gpuBuffer, ByteBuffer data, @GpuBuffer.Usage int usage, Supplier<String> labelGetter) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      if (GpuDeviceInfo.get(gpuDevice).requiresRecreateOnUploadToBuffer()) {
         if (gpuBuffer != null) {
            gpuBuffer.close();
         }

         return gpuDevice.createBuffer(labelGetter, usage, data);
      } else {
         if (gpuBuffer == null) {
            gpuBuffer = gpuDevice.createBuffer(labelGetter, usage, data);
         } else {
            CommandEncoder commandEncoder = gpuDevice.createCommandEncoder();
            if (gpuBuffer.size() < data.remaining()) {
               gpuBuffer.close();
               gpuBuffer = gpuDevice.createBuffer(labelGetter, usage, data);
            } else {
               commandEncoder.writeToBuffer(gpuBuffer.slice(), data);
            }
         }

         return gpuBuffer;
      }
   }

   public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer data) {
      this.immediateDrawVertexBuffer = uploadToBuffer(this.immediateDrawVertexBuffer, data, 40, () -> "Immediate vertex buffer for " + this);
      return this.immediateDrawVertexBuffer;
   }

   public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer data) {
      this.immediateDrawIndexBuffer = uploadToBuffer(this.immediateDrawIndexBuffer, data, 72, () -> "Immediate index buffer for " + this);
      return this.immediateDrawIndexBuffer;
   }

   @Environment(EnvType.CLIENT)
   @DeobfuscateClass
   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
      private final IntList offsets = new IntArrayList();
      private int offset;

      Builder() {
      }

      public VertexFormat.Builder add(String name, VertexFormatElement element) {
         this.elements.put(name, element);
         this.offsets.add(this.offset);
         this.offset = this.offset + element.byteSize();
         return this;
      }

      public VertexFormat.Builder padding(int padding) {
         this.offset += padding;
         return this;
      }

      public VertexFormat build() {
         ImmutableMap<String, VertexFormatElement> immutableMap = this.elements.buildOrThrow();
         ImmutableList<VertexFormatElement> immutableList = immutableMap.values().asList();
         ImmutableList<String> immutableList2 = immutableMap.keySet().asList();
         return new VertexFormat(immutableList, immutableList2, this.offsets, this.offset);
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum DrawMode {
      LINES(2, 2, false),
      DEBUG_LINES(2, 2, false),
      DEBUG_LINE_STRIP(2, 1, true),
      POINTS(1, 1, false),
      TRIANGLES(3, 3, false),
      TRIANGLE_STRIP(3, 1, true),
      TRIANGLE_FAN(3, 1, true),
      QUADS(4, 4, false);

      public final int firstVertexCount;
      public final int additionalVertexCount;
      public final boolean shareVertices;

      private DrawMode(final int firstVertexCount, final int additionalVertexCount, final boolean shareVertices) {
         this.firstVertexCount = firstVertexCount;
         this.additionalVertexCount = additionalVertexCount;
         this.shareVertices = shareVertices;
      }

      public int getIndexCount(int vertexCount) {
         return switch (this) {
            case LINES, QUADS -> vertexCount / 4 * 6;
            case DEBUG_LINES, DEBUG_LINE_STRIP, POINTS, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> vertexCount;
            default -> 0;
         };
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum IndexType {
      SHORT(2),
      INT(4);

      public final int size;

      private IndexType(final int size) {
         this.size = size;
      }

      public static VertexFormat.IndexType smallestFor(int i) {
         return (i & -65536) != 0 ? INT : SHORT;
      }
   }
}
