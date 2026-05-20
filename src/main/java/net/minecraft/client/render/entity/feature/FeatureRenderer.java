package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public abstract class FeatureRenderer<S extends EntityRenderState, M extends EntityModel<? super S>> {
   private final FeatureRendererContext<S, M> context;

   public FeatureRenderer(FeatureRendererContext<S, M> context) {
      this.context = context;
   }

   protected static <S extends LivingEntityRenderState> void render(
      Model<? super S> model,
      Identifier texture,
      MatrixStack matrices,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int light,
      S state,
      int color,
      int i
   ) {
      if (!state.invisible) {
         renderModel(model, texture, matrices, orderedRenderCommandQueue, light, state, color, i);
      }
   }

   protected static <S extends LivingEntityRenderState> void renderModel(
      Model<? super S> model,
      Identifier identifier,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int light,
      S state,
      int color,
      int i
   ) {
      orderedRenderCommandQueue.getBatchingQueue(i)
         .submitModel(
            model,
            state,
            matrixStack,
            RenderLayers.entityCutoutNoCull(identifier),
            light,
            LivingEntityRenderer.getOverlay(state, 0.0F),
            color,
            null,
            state.outlineColor,
            null
         );
   }

   public M getContextModel() {
      return this.context.getModel();
   }

   public abstract void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, S state, float limbAngle, float limbDistance);
}
