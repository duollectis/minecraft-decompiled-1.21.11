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
import net.minecraft.client.render.entity.state.IronGolemEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class IronGolemEntityModel extends EntityModel<IronGolemEntityRenderState> {
   private final ModelPart head;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;

   public IronGolemEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.head = modelPart.getChild("head");
      this.rightArm = modelPart.getChild("right_arm");
      this.leftArm = modelPart.getChild("left_arm");
      this.rightLeg = modelPart.getChild("right_leg");
      this.leftLeg = modelPart.getChild("left_leg");
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "head",
         ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F).uv(24, 0).cuboid(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F),
         ModelTransform.origin(0.0F, -7.0F, -2.0F)
      );
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create()
            .uv(0, 40)
            .cuboid(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F)
            .uv(0, 70)
            .cuboid(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new Dilation(0.5F)),
         ModelTransform.origin(0.0F, -7.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_arm", ModelPartBuilder.create().uv(60, 21).cuboid(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F), ModelTransform.origin(0.0F, -7.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_arm", ModelPartBuilder.create().uv(60, 58).cuboid(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F), ModelTransform.origin(0.0F, -7.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_leg", ModelPartBuilder.create().uv(37, 0).cuboid(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F), ModelTransform.origin(-4.0F, 11.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_leg", ModelPartBuilder.create().uv(60, 0).mirrored().cuboid(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F), ModelTransform.origin(5.0F, 11.0F, 0.0F)
      );
      return TexturedModelData.of(modelData, 128, 128);
   }

   public void setAngles(IronGolemEntityRenderState ironGolemEntityRenderState) {
      super.setAngles(ironGolemEntityRenderState);
      float f = ironGolemEntityRenderState.attackTicksLeft;
      float g = ironGolemEntityRenderState.limbSwingAmplitude;
      float h = ironGolemEntityRenderState.limbSwingAnimationProgress;
      if (f > 0.0F) {
         this.rightArm.pitch = -2.0F + 1.5F * MathHelper.wrap(f, 10.0F);
         this.leftArm.pitch = -2.0F + 1.5F * MathHelper.wrap(f, 10.0F);
      } else {
         int i = ironGolemEntityRenderState.lookingAtVillagerTicks;
         if (i > 0) {
            this.rightArm.pitch = -0.8F + 0.025F * MathHelper.wrap(i, 70.0F);
            this.leftArm.pitch = 0.0F;
         } else {
            this.rightArm.pitch = (-0.2F + 1.5F * MathHelper.wrap(h, 13.0F)) * g;
            this.leftArm.pitch = (-0.2F - 1.5F * MathHelper.wrap(h, 13.0F)) * g;
         }
      }

      this.head.yaw = ironGolemEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
      this.head.pitch = ironGolemEntityRenderState.pitch * (float) (Math.PI / 180.0);
      this.rightLeg.pitch = -1.5F * MathHelper.wrap(h, 13.0F) * g;
      this.leftLeg.pitch = 1.5F * MathHelper.wrap(h, 13.0F) * g;
      this.rightLeg.yaw = 0.0F;
      this.leftLeg.yaw = 0.0F;
   }

   public ModelPart getRightArm() {
      return this.rightArm;
   }
}
