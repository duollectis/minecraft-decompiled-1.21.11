package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HappyGhastEntityModel;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.HappyGhastEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class HappyGhastRopesFeatureRenderer<M extends HappyGhastEntityModel> extends FeatureRenderer<HappyGhastEntityRenderState, M> {
   private final RenderLayer renderLayer;
   private final HappyGhastEntityModel model;
   private final HappyGhastEntityModel babyModel;

   public HappyGhastRopesFeatureRenderer(FeatureRendererContext<HappyGhastEntityRenderState, M> context, LoadedEntityModels loader, Identifier texture) {
      super(context);
      this.renderLayer = RenderLayers.entityCutoutNoCull(texture);
      this.model = new HappyGhastEntityModel(loader.getModelPart(EntityModelLayers.HAPPY_GHAST_ROPES));
      this.babyModel = new HappyGhastEntityModel(loader.getModelPart(EntityModelLayers.HAPPY_GHAST_BABY_ROPES));
   }

   public void render(
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      HappyGhastEntityRenderState happyGhastEntityRenderState,
      float f,
      float g
   ) {
      if (happyGhastEntityRenderState.hasRopes && happyGhastEntityRenderState.harnessStack.isIn(ItemTags.HARNESSES)) {
         HappyGhastEntityModel happyGhastEntityModel = happyGhastEntityRenderState.baby ? this.babyModel : this.model;
         orderedRenderCommandQueue.submitModel(
            happyGhastEntityModel,
            happyGhastEntityRenderState,
            matrixStack,
            this.renderLayer,
            i,
            OverlayTexture.DEFAULT_UV,
            happyGhastEntityRenderState.outlineColor,
            null
         );
      }
   }
}
