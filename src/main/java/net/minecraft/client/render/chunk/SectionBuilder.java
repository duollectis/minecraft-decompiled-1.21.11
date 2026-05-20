package net.minecraft.client.render.chunk;

import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SectionBuilder {
   private final BlockRenderManager blockRenderManager;
   private final BlockEntityRenderManager blockEntityRenderDispatcher;

   public SectionBuilder(BlockRenderManager blockRenderManager, BlockEntityRenderManager blockEntityRenderDispatcher) {
      this.blockRenderManager = blockRenderManager;
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
   }

   public SectionBuilder.RenderData build(
      ChunkSectionPos sectionPos, ChunkRendererRegion renderRegion, VertexSorter vertexSorter, BlockBufferAllocatorStorage allocatorStorage
   ) {
      SectionBuilder.RenderData renderData = new SectionBuilder.RenderData();
      BlockPos blockPos = sectionPos.getMinPos();
      BlockPos blockPos2 = blockPos.add(15, 15, 15);
      ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
      MatrixStack matrixStack = new MatrixStack();
      BlockModelRenderer.enableBrightnessCache();
      Map<BlockRenderLayer, BufferBuilder> map = new EnumMap<>(BlockRenderLayer.class);
      Random random = Random.create();
      List<BlockModelPart> list = new ObjectArrayList();

      for (BlockPos blockPos3 : BlockPos.iterate(blockPos, blockPos2)) {
         BlockState blockState = renderRegion.getBlockState(blockPos3);
         if (blockState.isOpaqueFullCube()) {
            chunkOcclusionDataBuilder.markClosed(blockPos3);
         }

         if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = renderRegion.getBlockEntity(blockPos3);
            if (blockEntity != null) {
               this.addBlockEntity(renderData, blockEntity);
            }
         }

         FluidState fluidState = blockState.getFluidState();
         if (!fluidState.isEmpty()) {
            BlockRenderLayer blockRenderLayer = BlockRenderLayers.getFluidLayer(fluidState);
            BufferBuilder bufferBuilder = this.beginBufferBuilding(map, allocatorStorage, blockRenderLayer);
            this.blockRenderManager.renderFluid(blockPos3, renderRegion, bufferBuilder, blockState, fluidState);
         }

         if (blockState.getRenderType() == BlockRenderType.MODEL) {
            BlockRenderLayer blockRenderLayer = BlockRenderLayers.getBlockLayer(blockState);
            BufferBuilder bufferBuilder = this.beginBufferBuilding(map, allocatorStorage, blockRenderLayer);
            random.setSeed(blockState.getRenderingSeed(blockPos3));
            this.blockRenderManager.getModel(blockState).addParts(random, list);
            matrixStack.push();
            matrixStack.translate(
               (float)ChunkSectionPos.getLocalCoord(blockPos3.getX()),
               (float)ChunkSectionPos.getLocalCoord(blockPos3.getY()),
               (float)ChunkSectionPos.getLocalCoord(blockPos3.getZ())
            );
            this.blockRenderManager.renderBlock(blockState, blockPos3, renderRegion, matrixStack, bufferBuilder, true, list);
            matrixStack.pop();
            list.clear();
         }
      }

      for (Entry<BlockRenderLayer, BufferBuilder> entry : map.entrySet()) {
         BlockRenderLayer blockRenderLayer2 = entry.getKey();
         BuiltBuffer builtBuffer = entry.getValue().endNullable();
         if (builtBuffer != null) {
            if (blockRenderLayer2 == BlockRenderLayer.TRANSLUCENT) {
               renderData.translucencySortingData = builtBuffer.sortQuads(allocatorStorage.get(blockRenderLayer2), vertexSorter);
            }

            renderData.buffers.put(blockRenderLayer2, builtBuffer);
         }
      }

      BlockModelRenderer.disableBrightnessCache();
      renderData.chunkOcclusionData = chunkOcclusionDataBuilder.build();
      return renderData;
   }

   private BufferBuilder beginBufferBuilding(
      Map<BlockRenderLayer, BufferBuilder> builders, BlockBufferAllocatorStorage allocatorStorage, BlockRenderLayer layer
   ) {
      BufferBuilder bufferBuilder = builders.get(layer);
      if (bufferBuilder == null) {
         BufferAllocator bufferAllocator = allocatorStorage.get(layer);
         bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
         builders.put(layer, bufferBuilder);
      }

      return bufferBuilder;
   }

   private <E extends BlockEntity> void addBlockEntity(SectionBuilder.RenderData data, E blockEntity) {
      BlockEntityRenderer<E, ?> blockEntityRenderer = this.blockEntityRenderDispatcher.get(blockEntity);
      if (blockEntityRenderer != null && !blockEntityRenderer.rendersOutsideBoundingBox()) {
         data.blockEntities.add(blockEntity);
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class RenderData {
      public final List<BlockEntity> blockEntities = new ArrayList<>();
      public final Map<BlockRenderLayer, BuiltBuffer> buffers = new EnumMap<>(BlockRenderLayer.class);
      public ChunkOcclusionData chunkOcclusionData = new ChunkOcclusionData();
      public BuiltBuffer.@Nullable SortState translucencySortingData;

      public void close() {
         this.buffers.values().forEach(BuiltBuffer::close);
      }
   }
}
