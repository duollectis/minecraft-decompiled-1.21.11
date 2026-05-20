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
import net.minecraft.client.render.entity.state.ChickenEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class ChickenEntityModel extends EntityModel<ChickenEntityRenderState> {
   public static final String RED_THING = "red_thing";
   public static final float field_56579 = 16.0F;
   public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(false, 5.0F, 2.0F, 2.0F, 1.99F, 24.0F, Set.of("head", "beak", "red_thing"));
   private final ModelPart head;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;
   private final ModelPart rightWing;
   private final ModelPart leftWing;

   public ChickenEntityModel(ModelPart modelPart) {
      super(modelPart);
      this.head = modelPart.getChild("head");
      this.rightLeg = modelPart.getChild("right_leg");
      this.leftLeg = modelPart.getChild("left_leg");
      this.rightWing = modelPart.getChild("right_wing");
      this.leftWing = modelPart.getChild("left_wing");
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = getModelData();
      return TexturedModelData.of(modelData, 64, 32);
   }

   protected static ModelData getModelData() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "head", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 3.0F), ModelTransform.origin(0.0F, 15.0F, -4.0F)
      );
      modelPartData2.addChild("beak", ModelPartBuilder.create().uv(14, 0).cuboid(-2.0F, -4.0F, -4.0F, 4.0F, 2.0F, 2.0F), ModelTransform.NONE);
      modelPartData2.addChild("red_thing", ModelPartBuilder.create().uv(14, 4).cuboid(-1.0F, -2.0F, -3.0F, 2.0F, 2.0F, 2.0F), ModelTransform.NONE);
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create().uv(0, 9).cuboid(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
         ModelTransform.of(0.0F, 16.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(26, 0).cuboid(-1.0F, 0.0F, -3.0F, 3.0F, 5.0F, 3.0F);
      modelPartData.addChild("right_leg", modelPartBuilder, ModelTransform.origin(-2.0F, 19.0F, 1.0F));
      modelPartData.addChild("left_leg", modelPartBuilder, ModelTransform.origin(1.0F, 19.0F, 1.0F));
      modelPartData.addChild(
         "right_wing", ModelPartBuilder.create().uv(24, 13).cuboid(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F), ModelTransform.origin(-4.0F, 13.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_wing", ModelPartBuilder.create().uv(24, 13).cuboid(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F), ModelTransform.origin(4.0F, 13.0F, 0.0F)
      );
      return modelData;
   }

   public void setAngles(ChickenEntityRenderState chickenEntityRenderState) {
      super.setAngles(chickenEntityRenderState);
      float f = (MathHelper.sin(chickenEntityRenderState.flapProgress) + 1.0F) * chickenEntityRenderState.maxWingDeviation;
      this.head.pitch = chickenEntityRenderState.pitch * (float) (Math.PI / 180.0);
      this.head.yaw = chickenEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
      float g = chickenEntityRenderState.limbSwingAmplitude;
      float h = chickenEntityRenderState.limbSwingAnimationProgress;
      this.rightLeg.pitch = MathHelper.cos(h * 0.6662F) * 1.4F * g;
      this.leftLeg.pitch = MathHelper.cos(h * 0.6662F + (float) Math.PI) * 1.4F * g;
      this.rightWing.roll = f;
      this.leftWing.roll = -f;
   }
}
