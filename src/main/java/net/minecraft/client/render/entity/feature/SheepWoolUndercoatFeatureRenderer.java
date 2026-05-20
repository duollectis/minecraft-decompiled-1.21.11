package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.render.entity.model.SheepWoolEntityModel;
import net.minecraft.client.render.entity.state.SheepEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SheepWoolUndercoatFeatureRenderer extends FeatureRenderer<SheepEntityRenderState, SheepEntityModel> {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/sheep/sheep_wool_undercoat.png");
   private final EntityModel<SheepEntityRenderState> model;
   private final EntityModel<SheepEntityRenderState> babyModel;

   public SheepWoolUndercoatFeatureRenderer(FeatureRendererContext<SheepEntityRenderState, SheepEntityModel> context, LoadedEntityModels loader) {
      super(context);
      this.model = new SheepWoolEntityModel(loader.getModelPart(EntityModelLayers.SHEEP_WOOL_UNDERCOAT));
      this.babyModel = new SheepWoolEntityModel(loader.getModelPart(EntityModelLayers.SHEEP_BABY_WOOL_UNDERCOAT));
   }

   public void render(
      MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, SheepEntityRenderState sheepEntityRenderState, float f, float g
   ) {
      if (!sheepEntityRenderState.invisible && (sheepEntityRenderState.rainbow || sheepEntityRenderState.color != DyeColor.WHITE)) {
         EntityModel<SheepEntityRenderState> entityModel = sheepEntityRenderState.baby ? this.babyModel : this.model;
         render(entityModel, TEXTURE, matrixStack, orderedRenderCommandQueue, i, sheepEntityRenderState, sheepEntityRenderState.getRgbColor(), 1);
      }
   }
}
