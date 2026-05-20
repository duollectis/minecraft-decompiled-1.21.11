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

@Environment(EnvType.CLIENT)
public class Deadmau5EarsEntityModel extends PlayerEntityModel {
   public Deadmau5EarsEntityModel(ModelPart modelPart) {
      super(modelPart, false);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
      ModelPartData modelPartData = modelData.getRoot().resetChildrenParts();
      ModelPartData modelPartData2 = modelPartData.getChild("head");
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(24, 0).cuboid(-3.0F, -6.0F, -1.0F, 6.0F, 6.0F, 1.0F, new Dilation(1.0F));
      modelPartData2.addChild("left_ear", modelPartBuilder, ModelTransform.origin(-6.0F, -6.0F, 0.0F));
      modelPartData2.addChild("right_ear", modelPartBuilder, ModelTransform.origin(6.0F, -6.0F, 0.0F));
      return TexturedModelData.of(modelData, 64, 64);
   }
}
