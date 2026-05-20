package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer;
import net.minecraft.client.render.entity.model.CamelEntityModel;
import net.minecraft.client.render.entity.model.CamelSaddleEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.CamelEntityRenderState;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CamelHuskEntityRenderer extends CamelEntityRenderer {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/camel/camel_husk.png");

   public CamelHuskEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
   }

   @Override
   protected SaddleFeatureRenderer<CamelEntityRenderState, CamelEntityModel, CamelSaddleEntityModel> createSaddleFeatureRenderer(
      EntityRendererFactory.Context context
   ) {
      return new SaddleFeatureRenderer<>(
         this,
         context.getEquipmentRenderer(),
         EquipmentModel.LayerType.CAMEL_HUSK_SADDLE,
         state -> state.saddleStack,
         new CamelSaddleEntityModel(context.getPart(EntityModelLayers.CAMEL_HUSK)),
         new CamelSaddleEntityModel(context.getPart(EntityModelLayers.CAMEL_HUSK_BABY))
      );
   }

   @Override
   public Identifier getTexture(CamelEntityRenderState camelEntityRenderState) {
      return TEXTURE;
   }
}
