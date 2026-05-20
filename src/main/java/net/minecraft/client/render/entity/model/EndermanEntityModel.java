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
import net.minecraft.client.render.entity.state.EndermanEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class EndermanEntityModel<T extends EndermanEntityRenderState> extends BipedEntityModel<T> {
   public EndermanEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public static TexturedModelData getTexturedModelData() {
      float f = -14.0F;
      ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, -14.0F);
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.origin(0.0F, -13.0F, 0.0F)
      );
      modelPartData2.addChild(
         "hat", ModelPartBuilder.create().uv(0, 16).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(-0.5F)), ModelTransform.NONE
      );
      modelPartData.addChild(
         "body", ModelPartBuilder.create().uv(32, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), ModelTransform.origin(0.0F, -14.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_arm", ModelPartBuilder.create().uv(56, 0).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F), ModelTransform.origin(-5.0F, -12.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_arm", ModelPartBuilder.create().uv(56, 0).mirrored().cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 30.0F, 2.0F), ModelTransform.origin(5.0F, -12.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_leg", ModelPartBuilder.create().uv(56, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F), ModelTransform.origin(-2.0F, -5.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_leg", ModelPartBuilder.create().uv(56, 0).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 30.0F, 2.0F), ModelTransform.origin(2.0F, -5.0F, 0.0F)
      );
      return TexturedModelData.of(modelData, 64, 32);
   }

   public void setAngles(T endermanEntityRenderState) {
      super.setAngles(endermanEntityRenderState);
      this.head.visible = true;
      this.rightArm.pitch *= 0.5F;
      this.leftArm.pitch *= 0.5F;
      this.rightLeg.pitch *= 0.5F;
      this.leftLeg.pitch *= 0.5F;
      float f = 0.4F;
      this.rightArm.pitch = MathHelper.clamp(this.rightArm.pitch, -0.4F, 0.4F);
      this.leftArm.pitch = MathHelper.clamp(this.leftArm.pitch, -0.4F, 0.4F);
      this.rightLeg.pitch = MathHelper.clamp(this.rightLeg.pitch, -0.4F, 0.4F);
      this.leftLeg.pitch = MathHelper.clamp(this.leftLeg.pitch, -0.4F, 0.4F);
      if (endermanEntityRenderState.carriedBlock != null) {
         this.rightArm.pitch = -0.5F;
         this.leftArm.pitch = -0.5F;
         this.rightArm.roll = 0.05F;
         this.leftArm.roll = -0.05F;
      }

      if (endermanEntityRenderState.angry) {
         float g = 5.0F;
         this.head.originY -= 5.0F;
         this.hat.originY += 5.0F;
      }
   }
}
