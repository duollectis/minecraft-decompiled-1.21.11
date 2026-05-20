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
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;

@Environment(EnvType.CLIENT)
public class SkullEntityModel extends SkullBlockEntityModel {
   protected final ModelPart head;

   public SkullEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.head = modelPart.getChild("head");
   }

   public static ModelData getModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
      return modelData;
   }

   public static TexturedModelData getHeadTexturedModelData() {
      ModelData modelData = getModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.getChild("head")
         .addChild("hat", ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.25F)), ModelTransform.NONE);
      return TexturedModelData.of(modelData, 64, 64);
   }

   public static TexturedModelData getSkullTexturedModelData() {
      ModelData modelData = getModelData();
      return TexturedModelData.of(modelData, 64, 32);
   }

   public void setAngles(SkullBlockEntityModel.SkullModelState skullModelState) {
      super.setAngles(skullModelState);
      this.head.yaw = skullModelState.yaw * (float) (Math.PI / 180.0);
      this.head.pitch = skullModelState.pitch * (float) (Math.PI / 180.0);
   }
}
