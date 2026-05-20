package net.minecraft.client.render.entity.model;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class PlayerEntityModel extends BipedEntityModel<PlayerEntityRenderState> {
   protected static final String LEFT_SLEEVE = "left_sleeve";
   protected static final String RIGHT_SLEEVE = "right_sleeve";
   protected static final String LEFT_PANTS = "left_pants";
   protected static final String RIGHT_PANTS = "right_pants";
   private final List<ModelPart> parts;
   public final ModelPart leftSleeve;
   public final ModelPart rightSleeve;
   public final ModelPart leftPants;
   public final ModelPart rightPants;
   public final ModelPart jacket;
   private final boolean thinArms;

   public PlayerEntityModel(ModelPart root, boolean thinArms) {
      super(root, RenderLayers::entityTranslucent);
      this.thinArms = thinArms;
      this.leftSleeve = this.leftArm.getChild("left_sleeve");
      this.rightSleeve = this.rightArm.getChild("right_sleeve");
      this.leftPants = this.leftLeg.getChild("left_pants");
      this.rightPants = this.rightLeg.getChild("right_pants");
      this.jacket = this.body.getChild("jacket");
      this.parts = List.of(this.head, this.body, this.leftArm, this.rightArm, this.leftLeg, this.rightLeg);
   }

   public static ModelData getTexturedModelData(Dilation dilation, boolean slim) {
      ModelData modelData = BipedEntityModel.getModelData(dilation, 0.0F);
      ModelPartData modelPartData = modelData.getRoot();
      float f = 0.25F;
      if (slim) {
         ModelPartData modelPartData2 = modelPartData.addChild(
            "left_arm", ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation), ModelTransform.origin(5.0F, 2.0F, 0.0F)
         );
         ModelPartData modelPartData3 = modelPartData.addChild(
            "right_arm",
            ModelPartBuilder.create().uv(40, 16).cuboid(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation),
            ModelTransform.origin(-5.0F, 2.0F, 0.0F)
         );
         modelPartData2.addChild(
            "left_sleeve", ModelPartBuilder.create().uv(48, 48).cuboid(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
         );
         modelPartData3.addChild(
            "right_sleeve", ModelPartBuilder.create().uv(40, 32).cuboid(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
         );
      } else {
         ModelPartData modelPartData2 = modelPartData.addChild(
            "left_arm", ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation), ModelTransform.origin(5.0F, 2.0F, 0.0F)
         );
         ModelPartData modelPartData3 = modelPartData.getChild("right_arm");
         modelPartData2.addChild(
            "left_sleeve", ModelPartBuilder.create().uv(48, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
         );
         modelPartData3.addChild(
            "right_sleeve", ModelPartBuilder.create().uv(40, 32).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
         );
      }

      ModelPartData modelPartData2 = modelPartData.addChild(
         "left_leg", ModelPartBuilder.create().uv(16, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation), ModelTransform.origin(1.9F, 12.0F, 0.0F)
      );
      ModelPartData modelPartData3 = modelPartData.getChild("right_leg");
      modelPartData2.addChild(
         "left_pants", ModelPartBuilder.create().uv(0, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
      );
      modelPartData3.addChild(
         "right_pants", ModelPartBuilder.create().uv(0, 32).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
      );
      ModelPartData modelPartData4 = modelPartData.getChild("body");
      modelPartData4.addChild(
         "jacket", ModelPartBuilder.create().uv(16, 32).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.NONE
      );
      return modelData;
   }

   public static EquipmentModelData<ModelData> createEquipmentModelData(Dilation hatDilation, Dilation armorDilation) {
      return BipedEntityModel.createEquipmentModelData(hatDilation, armorDilation).map(modelData -> {
         ModelPartData modelPartData = modelData.getRoot();
         ModelPartData modelPartData2 = modelPartData.getChild("left_arm");
         ModelPartData modelPartData3 = modelPartData.getChild("right_arm");
         modelPartData2.addChild("left_sleeve", ModelPartBuilder.create(), ModelTransform.NONE);
         modelPartData3.addChild("right_sleeve", ModelPartBuilder.create(), ModelTransform.NONE);
         ModelPartData modelPartData4 = modelPartData.getChild("left_leg");
         ModelPartData modelPartData5 = modelPartData.getChild("right_leg");
         modelPartData4.addChild("left_pants", ModelPartBuilder.create(), ModelTransform.NONE);
         modelPartData5.addChild("right_pants", ModelPartBuilder.create(), ModelTransform.NONE);
         ModelPartData modelPartData6 = modelPartData.getChild("body");
         modelPartData6.addChild("jacket", ModelPartBuilder.create(), ModelTransform.NONE);
         return (ModelData)modelData;
      });
   }

   public void setAngles(PlayerEntityRenderState playerEntityRenderState) {
      boolean bl = !playerEntityRenderState.spectator;
      this.body.visible = bl;
      this.rightArm.visible = bl;
      this.leftArm.visible = bl;
      this.rightLeg.visible = bl;
      this.leftLeg.visible = bl;
      this.hat.visible = playerEntityRenderState.hatVisible;
      this.jacket.visible = playerEntityRenderState.jacketVisible;
      this.leftPants.visible = playerEntityRenderState.leftPantsLegVisible;
      this.rightPants.visible = playerEntityRenderState.rightPantsLegVisible;
      this.leftSleeve.visible = playerEntityRenderState.leftSleeveVisible;
      this.rightSleeve.visible = playerEntityRenderState.rightSleeveVisible;
      super.setAngles(playerEntityRenderState);
   }

   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      this.leftSleeve.visible = visible;
      this.rightSleeve.visible = visible;
      this.leftPants.visible = visible;
      this.rightPants.visible = visible;
      this.jacket.visible = visible;
   }

   public void setArmAngle(PlayerEntityRenderState playerEntityRenderState, Arm arm, MatrixStack matrixStack) {
      this.getRootPart().applyTransform(matrixStack);
      ModelPart modelPart = this.getArm(arm);
      if (this.thinArms) {
         float f = 0.5F * (arm == Arm.RIGHT ? 1 : -1);
         modelPart.originX += f;
         modelPart.applyTransform(matrixStack);
         modelPart.originX -= f;
      } else {
         modelPart.applyTransform(matrixStack);
      }
   }

   public ModelPart getRandomPart(Random random) {
      return Util.getRandom(this.parts, random);
   }
}
