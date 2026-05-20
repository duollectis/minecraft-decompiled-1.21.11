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
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class CreeperEntityModel extends EntityModel<CreeperEntityRenderState> {
   private final ModelPart head;
   private final ModelPart leftHindLeg;
   private final ModelPart rightHindLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightFrontLeg;
   private static final int HEAD_AND_BODY_Y_PIVOT = 6;

   public CreeperEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.head = modelPart.getChild("head");
      this.rightHindLeg = modelPart.getChild("right_hind_leg");
      this.leftHindLeg = modelPart.getChild("left_hind_leg");
      this.rightFrontLeg = modelPart.getChild("right_front_leg");
      this.leftFrontLeg = modelPart.getChild("left_front_leg");
   }

   public static TexturedModelData getTexturedModelData(Dilation dilation) {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation), ModelTransform.origin(0.0F, 6.0F, 0.0F)
      );
      modelPartData.addChild(
         "body", ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation), ModelTransform.origin(0.0F, 6.0F, 0.0F)
      );
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, dilation);
      modelPartData.addChild("right_hind_leg", modelPartBuilder, ModelTransform.origin(-2.0F, 18.0F, 4.0F));
      modelPartData.addChild("left_hind_leg", modelPartBuilder, ModelTransform.origin(2.0F, 18.0F, 4.0F));
      modelPartData.addChild("right_front_leg", modelPartBuilder, ModelTransform.origin(-2.0F, 18.0F, -4.0F));
      modelPartData.addChild("left_front_leg", modelPartBuilder, ModelTransform.origin(2.0F, 18.0F, -4.0F));
      return TexturedModelData.of(modelData, 64, 32);
   }

   public void setAngles(CreeperEntityRenderState creeperEntityRenderState) {
      super.setAngles(creeperEntityRenderState);
      this.head.yaw = creeperEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
      this.head.pitch = creeperEntityRenderState.pitch * (float) (Math.PI / 180.0);
      float f = creeperEntityRenderState.limbSwingAmplitude;
      float g = creeperEntityRenderState.limbSwingAnimationProgress;
      this.leftHindLeg.pitch = MathHelper.cos(g * 0.6662F) * 1.4F * f;
      this.rightHindLeg.pitch = MathHelper.cos(g * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.leftFrontLeg.pitch = MathHelper.cos(g * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.rightFrontLeg.pitch = MathHelper.cos(g * 0.6662F) * 1.4F * f;
   }
}
