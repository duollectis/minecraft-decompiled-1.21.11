package net.minecraft.client.render.entity.model;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.state.PolarBearEntityRenderState;

@Environment(EnvType.CLIENT)
public class PolarBearEntityModel extends QuadrupedEntityModel<PolarBearEntityRenderState> {
   private static final float field_53834 = 2.25F;
   private static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(true, 16.0F, 4.0F, 2.25F, 2.0F, 24.0F, Set.of("head"));

   public PolarBearEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public static TexturedModelData getTexturedModelData(boolean bl) {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "head",
         ModelPartBuilder.create()
            .uv(0, 0)
            .cuboid(-3.5F, -3.0F, -3.0F, 7.0F, 7.0F, 7.0F)
            .uv(0, 44)
            .cuboid("mouth", -2.5F, 1.0F, -6.0F, 5.0F, 3.0F, 3.0F)
            .uv(26, 0)
            .cuboid("right_ear", -4.5F, -4.0F, -1.0F, 2.0F, 2.0F, 1.0F)
            .uv(26, 0)
            .mirrored()
            .cuboid("left_ear", 2.5F, -4.0F, -1.0F, 2.0F, 2.0F, 1.0F),
         ModelTransform.origin(0.0F, 10.0F, -16.0F)
      );
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create().uv(0, 19).cuboid(-5.0F, -13.0F, -7.0F, 14.0F, 14.0F, 11.0F).uv(39, 0).cuboid(-4.0F, -25.0F, -7.0F, 12.0F, 12.0F, 10.0F),
         ModelTransform.of(-2.0F, 9.0F, 12.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      int i = 10;
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(50, 22).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 8.0F);
      modelPartData.addChild("right_hind_leg", modelPartBuilder, ModelTransform.origin(-4.5F, 14.0F, 6.0F));
      modelPartData.addChild("left_hind_leg", modelPartBuilder, ModelTransform.origin(4.5F, 14.0F, 6.0F));
      ModelPartBuilder modelPartBuilder2 = ModelPartBuilder.create().uv(50, 40).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 6.0F);
      modelPartData.addChild("right_front_leg", modelPartBuilder2, ModelTransform.origin(-3.5F, 14.0F, -8.0F));
      modelPartData.addChild("left_front_leg", modelPartBuilder2, ModelTransform.origin(3.5F, 14.0F, -8.0F));
      return TexturedModelData.of(modelData, 128, 64).transform(bl ? BABY_TRANSFORMER : ModelTransformer.NO_OP).transform(ModelTransformer.scaling(1.2F));
   }

   public void setAngles(PolarBearEntityRenderState polarBearEntityRenderState) {
      super.setAngles(polarBearEntityRenderState);
      float f = polarBearEntityRenderState.warningAnimationProgress * polarBearEntityRenderState.warningAnimationProgress;
      float g = polarBearEntityRenderState.ageScale;
      float h = polarBearEntityRenderState.baby ? 0.44444445F : 1.0F;
      this.body.pitch -= f * (float) Math.PI * 0.35F;
      this.body.originY += f * g * 2.0F;
      this.rightFrontLeg.originY -= f * g * 20.0F;
      this.rightFrontLeg.originZ += f * g * 4.0F;
      this.rightFrontLeg.pitch -= f * (float) Math.PI * 0.45F;
      this.leftFrontLeg.originY = this.rightFrontLeg.originY;
      this.leftFrontLeg.originZ = this.rightFrontLeg.originZ;
      this.leftFrontLeg.pitch -= f * (float) Math.PI * 0.45F;
      this.head.originY -= f * h * 24.0F;
      this.head.originZ += f * h * 13.0F;
      this.head.pitch += f * (float) Math.PI * 0.15F;
   }
}
