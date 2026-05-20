package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class PlayerCapeModel extends PlayerEntityModel {
   private static final String CAPE = "cape";
   private final ModelPart cape = this.body.getChild("cape");

   public PlayerCapeModel(ModelPart modelPart) {
      super(modelPart, false);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
      ModelPartData modelPartData = modelData.getRoot().resetChildrenParts();
      ModelPartData modelPartData2 = modelPartData.getChild("body");
      modelPartData2.addChild(
         "cape",
         ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, Dilation.NONE, 1.0F, 0.5F),
         ModelTransform.of(0.0F, 0.0F, 2.0F, 0.0F, (float) Math.PI, 0.0F)
      );
      return TexturedModelData.of(modelData, 64, 64);
   }

   @Override
   public void setAngles(PlayerEntityRenderState playerEntityRenderState) {
      super.setAngles(playerEntityRenderState);
      this.cape
         .rotate(
            new Quaternionf()
               .rotateY((float) -Math.PI)
               .rotateX((6.0F + playerEntityRenderState.field_53537 / 2.0F + playerEntityRenderState.field_53536) * (float) (Math.PI / 180.0))
               .rotateZ(playerEntityRenderState.field_53538 / 2.0F * (float) (Math.PI / 180.0))
               .rotateY((180.0F - playerEntityRenderState.field_53538 / 2.0F) * (float) (Math.PI / 180.0))
         );
   }
}
