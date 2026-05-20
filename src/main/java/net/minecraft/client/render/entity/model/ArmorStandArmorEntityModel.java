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
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;

@Environment(EnvType.CLIENT)
public class ArmorStandArmorEntityModel extends BipedEntityModel<ArmorStandEntityRenderState> {
   public ArmorStandArmorEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public static EquipmentModelData<TexturedModelData> getEquipmentModelData(Dilation hatDilation, Dilation armorDilation) {
      return createEquipmentModelData(ArmorStandArmorEntityModel::getTexturedModelData, hatDilation, armorDilation)
         .map(modelData -> TexturedModelData.of(modelData, 64, 32));
   }

   private static ModelData getTexturedModelData(Dilation dilation) {
      ModelData modelData = BipedEntityModel.getModelData(dilation, 0.0F);
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation), ModelTransform.origin(0.0F, 1.0F, 0.0F)
      );
      modelPartData2.addChild("hat", ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation.add(0.5F)), ModelTransform.NONE);
      modelPartData.addChild(
         "right_leg",
         ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(-0.1F)),
         ModelTransform.origin(-1.9F, 11.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_leg",
         ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(-0.1F)),
         ModelTransform.origin(1.9F, 11.0F, 0.0F)
      );
      return modelData;
   }

   public void setAngles(ArmorStandEntityRenderState armorStandEntityRenderState) {
      super.setAngles(armorStandEntityRenderState);
      this.head.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.headRotation.pitch();
      this.head.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.headRotation.yaw();
      this.head.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.headRotation.roll();
      this.body.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.pitch();
      this.body.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.yaw();
      this.body.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.roll();
      this.leftArm.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.leftArmRotation.pitch();
      this.leftArm.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.leftArmRotation.yaw();
      this.leftArm.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.leftArmRotation.roll();
      this.rightArm.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.rightArmRotation.pitch();
      this.rightArm.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.rightArmRotation.yaw();
      this.rightArm.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.rightArmRotation.roll();
      this.leftLeg.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.leftLegRotation.pitch();
      this.leftLeg.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.leftLegRotation.yaw();
      this.leftLeg.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.leftLegRotation.roll();
      this.rightLeg.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.rightLegRotation.pitch();
      this.rightLeg.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.rightLegRotation.yaw();
      this.rightLeg.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.rightLegRotation.roll();
   }
}
