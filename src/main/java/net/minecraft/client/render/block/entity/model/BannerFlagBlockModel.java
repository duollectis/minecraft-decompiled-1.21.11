package net.minecraft.client.render.block.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class BannerFlagBlockModel extends Model<Float> {
   private final ModelPart flag;

   public BannerFlagBlockModel(ModelPart root) {
      super(root, RenderLayers::entitySolid);
      this.flag = root.getChild("flag");
   }

   public static TexturedModelData getTexturedModelData(boolean standing) {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "flag",
         ModelPartBuilder.create().uv(0, 0).cuboid(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F),
         ModelTransform.origin(0.0F, standing ? -44.0F : -20.5F, standing ? 0.0F : 10.5F)
      );
      return TexturedModelData.of(modelData, 64, 64);
   }

   public void setAngles(Float float_) {
      super.setAngles(float_);
      this.flag.pitch = (-0.0125F + 0.01F * MathHelper.cos((float) (Math.PI * 2) * float_)) * (float) Math.PI;
   }
}
