package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.Deadmau5EarsEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public class Deadmau5FeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private final BipedEntityModel<PlayerEntityRenderState> model;

   public Deadmau5FeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, LoadedEntityModels entityModels) {
      super(context);
      this.model = new Deadmau5EarsEntityModel(entityModels.getModelPart(EntityModelLayers.PLAYER_EARS));
   }

   public void render(
      MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float g
   ) {
      if (playerEntityRenderState.extraEars && !playerEntityRenderState.invisible) {
         int j = LivingEntityRenderer.getOverlay(playerEntityRenderState, 0.0F);
         orderedRenderCommandQueue.submitModel(
            this.model,
            playerEntityRenderState,
            matrixStack,
            RenderLayers.entitySolid(playerEntityRenderState.skinTextures.body().texturePath()),
            i,
            j,
            playerEntityRenderState.outlineColor,
            null
         );
      }
   }
}
