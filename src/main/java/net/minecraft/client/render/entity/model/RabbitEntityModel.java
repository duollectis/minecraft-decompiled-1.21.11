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
import net.minecraft.client.render.entity.state.RabbitEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class RabbitEntityModel extends EntityModel<RabbitEntityRenderState> {
   private static final float HAUNCH_JUMP_PITCH_MULTIPLIER = 50.0F;
   private static final float FRONT_LEGS_JUMP_PITCH_MULTIPLIER = -40.0F;
   private static final float SCALE = 0.6F;
   private static final ModelTransformer ADULT_TRANSFORMER = ModelTransformer.scaling(0.6F);
   private static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(
      true, 22.0F, 2.0F, 2.65F, 2.5F, 36.0F, Set.of("head", "left_ear", "right_ear", "nose")
   );
   private static final String LEFT_HAUNCH = "left_haunch";
   private static final String RIGHT_HAUNCH = "right_haunch";
   private final ModelPart leftHaunch;
   private final ModelPart rightHaunch;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart head;

   public RabbitEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.leftHaunch = modelPart.getChild("left_haunch");
      this.rightHaunch = modelPart.getChild("right_haunch");
      this.leftFrontLeg = modelPart.getChild("left_front_leg");
      this.rightFrontLeg = modelPart.getChild("right_front_leg");
      this.head = modelPart.getChild("head");
   }

   public static TexturedModelData getTexturedModelData(boolean baby) {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "left_haunch",
         ModelPartBuilder.create().uv(30, 15).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 5.0F),
         ModelTransform.of(3.0F, 17.5F, 3.7F, -0.36651915F, 0.0F, 0.0F)
      );
      ModelPartData modelPartData3 = modelPartData.addChild(
         "right_haunch",
         ModelPartBuilder.create().uv(16, 15).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 5.0F),
         ModelTransform.of(-3.0F, 17.5F, 3.7F, -0.36651915F, 0.0F, 0.0F)
      );
      modelPartData2.addChild(
         "left_hind_foot", ModelPartBuilder.create().uv(26, 24).cuboid(-1.0F, 5.5F, -3.7F, 2.0F, 1.0F, 7.0F), ModelTransform.rotation(0.36651915F, 0.0F, 0.0F)
      );
      modelPartData3.addChild(
         "right_hind_foot", ModelPartBuilder.create().uv(8, 24).cuboid(-1.0F, 5.5F, -3.7F, 2.0F, 1.0F, 7.0F), ModelTransform.rotation(0.36651915F, 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -2.0F, -10.0F, 6.0F, 5.0F, 10.0F),
         ModelTransform.of(0.0F, 19.0F, 8.0F, (float) (-Math.PI / 9), 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_front_leg",
         ModelPartBuilder.create().uv(8, 15).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
         ModelTransform.of(3.0F, 17.0F, -1.0F, -0.19198622F, 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_front_leg",
         ModelPartBuilder.create().uv(0, 15).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
         ModelTransform.of(-3.0F, 17.0F, -1.0F, -0.19198622F, 0.0F, 0.0F)
      );
      ModelPartData modelPartData4 = modelPartData.addChild(
         "head", ModelPartBuilder.create().uv(32, 0).cuboid(-2.5F, -4.0F, -5.0F, 5.0F, 4.0F, 5.0F), ModelTransform.origin(0.0F, 16.0F, -1.0F)
      );
      modelPartData4.addChild(
         "right_ear",
         ModelPartBuilder.create().uv(52, 0).cuboid(-2.5F, -9.0F, -1.0F, 2.0F, 5.0F, 1.0F),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 12), 0.0F)
      );
      modelPartData4.addChild(
         "left_ear",
         ModelPartBuilder.create().uv(58, 0).cuboid(0.5F, -9.0F, -1.0F, 2.0F, 5.0F, 1.0F),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 12), 0.0F)
      );
      modelPartData.addChild(
         "tail",
         ModelPartBuilder.create().uv(52, 6).cuboid(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F),
         ModelTransform.of(0.0F, 20.0F, 7.0F, -0.3490659F, 0.0F, 0.0F)
      );
      modelPartData4.addChild("nose", ModelPartBuilder.create().uv(32, 9).cuboid(-0.5F, -2.5F, -5.5F, 1.0F, 1.0F, 1.0F), ModelTransform.NONE);
      return TexturedModelData.of(modelData, 64, 32).transform(baby ? BABY_TRANSFORMER : ADULT_TRANSFORMER);
   }

   public void setAngles(RabbitEntityRenderState rabbitEntityRenderState) {
      super.setAngles(rabbitEntityRenderState);
      this.head.pitch = rabbitEntityRenderState.pitch * (float) (Math.PI / 180.0);
      this.head.yaw = rabbitEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
      float f = MathHelper.sin(rabbitEntityRenderState.jumpProgress * (float) Math.PI);
      this.leftHaunch.pitch += f * 50.0F * (float) (Math.PI / 180.0);
      this.rightHaunch.pitch += f * 50.0F * (float) (Math.PI / 180.0);
      this.leftFrontLeg.pitch += f * -40.0F * (float) (Math.PI / 180.0);
      this.rightFrontLeg.pitch += f * -40.0F * (float) (Math.PI / 180.0);
   }
}
