package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.SlimeEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.SlimeEntityModel;
import net.minecraft.client.render.entity.state.SlimeEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public class SlimeOverlayFeatureRenderer extends FeatureRenderer<SlimeEntityRenderState, SlimeEntityModel> {
   private final SlimeEntityModel model;

   public SlimeOverlayFeatureRenderer(FeatureRendererContext<SlimeEntityRenderState, SlimeEntityModel> context, LoadedEntityModels loader) {
      super(context);
      this.model = new SlimeEntityModel(loader.getModelPart(EntityModelLayers.SLIME_OUTER));
   }

   public void render(
      MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, SlimeEntityRenderState slimeEntityRenderState, float f, float g
   ) {
      boolean bl = slimeEntityRenderState.hasOutline() && slimeEntityRenderState.invisible;
      if (!slimeEntityRenderState.invisible || bl) {
         int j = LivingEntityRenderer.getOverlay(slimeEntityRenderState, 0.0F);
         if (bl) {
            orderedRenderCommandQueue.getBatchingQueue(1)
               .submitModel(
                  this.model,
                  slimeEntityRenderState,
                  matrixStack,
                  RenderLayers.outlineNoCull(SlimeEntityRenderer.TEXTURE),
                  i,
                  j,
                  -1,
                  null,
                  slimeEntityRenderState.outlineColor,
                  null
               );
         } else {
            orderedRenderCommandQueue.getBatchingQueue(1)
               .submitModel(
                  this.model,
                  slimeEntityRenderState,
                  matrixStack,
                  RenderLayers.entityTranslucent(SlimeEntityRenderer.TEXTURE),
                  i,
                  j,
                  -1,
                  null,
                  slimeEntityRenderState.outlineColor,
                  null
               );
         }
      }
   }
}
