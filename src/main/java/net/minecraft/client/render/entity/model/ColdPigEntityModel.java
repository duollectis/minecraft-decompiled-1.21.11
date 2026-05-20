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
public class ColdPigEntityModel extends PigEntityModel {
   public ColdPigEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public static TexturedModelData getTexturedModelData(Dilation dilation) {
      ModelData modelData = getModelData(dilation);
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create()
            .uv(28, 8)
            .cuboid(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F)
            .uv(28, 32)
            .cuboid(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F, new Dilation(0.5F)),
         ModelTransform.of(0.0F, 11.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      return TexturedModelData.of(modelData, 64, 64);
   }
}
