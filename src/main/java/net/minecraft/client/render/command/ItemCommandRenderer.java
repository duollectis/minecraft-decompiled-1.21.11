package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public class ItemCommandRenderer {
   private final MatrixStack matrices = new MatrixStack();

   public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers, OutlineVertexConsumerProvider outlineVertexConsumers) {
      for (OrderedRenderCommandQueueImpl.ItemCommand itemCommand : queue.getItemCommands()) {
         this.matrices.push();
         this.matrices.peek().copy(itemCommand.positionMatrix());
         ItemRenderer.renderItem(
            itemCommand.displayContext(),
            this.matrices,
            vertexConsumers,
            itemCommand.lightCoords(),
            itemCommand.overlayCoords(),
            itemCommand.tintLayers(),
            itemCommand.quads(),
            itemCommand.renderLayer(),
            itemCommand.glintType()
         );
         if (itemCommand.outlineColor() != 0) {
            outlineVertexConsumers.setColor(itemCommand.outlineColor());
            ItemRenderer.renderItem(
               itemCommand.displayContext(),
               this.matrices,
               outlineVertexConsumers,
               itemCommand.lightCoords(),
               itemCommand.overlayCoords(),
               itemCommand.tintLayers(),
               itemCommand.quads(),
               itemCommand.renderLayer(),
               ItemRenderState.Glint.NONE
            );
         }

         this.matrices.pop();
      }
   }
}
