package net.minecraft.client.render.block.entity;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
public class LoadedBlockEntityModels {
   public static final LoadedBlockEntityModels EMPTY = new LoadedBlockEntityModels(Map.of());
   private final Map<Block, SpecialModelRenderer<?>> renderers;

   public LoadedBlockEntityModels(Map<Block, SpecialModelRenderer<?>> renderers) {
      this.renderers = renderers;
   }

   public static LoadedBlockEntityModels fromModels(SpecialModelRenderer.BakeContext context) {
      return new LoadedBlockEntityModels(SpecialModelTypes.buildBlockToModelTypeMap(context));
   }

   public void render(
      Block block, ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, int outlineColor
   ) {
      SpecialModelRenderer<?> specialModelRenderer = this.renderers.get(block);
      if (specialModelRenderer != null) {
         specialModelRenderer.render(null, displayContext, matrices, queue, light, overlay, false, outlineColor);
      }
   }
}
