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
public class NautilusSaddleEntityModel extends NautilusEntityModel {
   private final ModelPart saddleRoot;
   private final ModelPart shell;

   public NautilusSaddleEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.saddleRoot = modelPart.getChild("root");
      this.shell = this.saddleRoot.getChild("shell");
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = getModelData();
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 29.0F, -6.0F));
      modelPartData2.addChild(
         "shell",
         ModelPartBuilder.create().uv(0, 0).cuboid(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F, new Dilation(0.2F)),
         ModelTransform.origin(0.0F, -13.0F, 5.0F)
      );
      return TexturedModelData.of(modelData, 128, 128);
   }
}
