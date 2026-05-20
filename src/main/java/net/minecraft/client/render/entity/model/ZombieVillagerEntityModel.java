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
import net.minecraft.client.render.entity.state.ZombieVillagerRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public class ZombieVillagerEntityModel<S extends ZombieVillagerRenderState> extends BipedEntityModel<S> implements ModelWithHat<S> {
   public ZombieVillagerEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "head",
         new ModelPartBuilder().uv(0, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F).uv(24, 0).cuboid(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F),
         ModelTransform.NONE
      );
      ModelPartData modelPartData3 = modelPartData2.addChild(
         "hat", ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new Dilation(0.5F)), ModelTransform.NONE
      );
      modelPartData3.addChild(
         "hat_rim",
         ModelPartBuilder.create().uv(30, 47).cuboid(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
         ModelTransform.rotation((float) (-Math.PI / 2), 0.0F, 0.0F)
      );
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create()
            .uv(16, 20)
            .cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
            .uv(0, 38)
            .cuboid(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new Dilation(0.05F)),
         ModelTransform.NONE
      );
      modelPartData.addChild(
         "right_arm", ModelPartBuilder.create().uv(44, 22).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(-5.0F, 2.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_arm", ModelPartBuilder.create().uv(44, 22).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(5.0F, 2.0F, 0.0F)
      );
      modelPartData.addChild(
         "right_leg", ModelPartBuilder.create().uv(0, 22).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(-2.0F, 12.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_leg", ModelPartBuilder.create().uv(0, 22).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.origin(2.0F, 12.0F, 0.0F)
      );
      return TexturedModelData.of(modelData, 64, 64);
   }

   public static TexturedModelData getTexturedModelDataWithoutHead() {
      return getTexturedModelData().transform(data -> {
         data.getRoot().resetChildrenParts("head").resetChildrenParts();
         return data;
      });
   }

   public static EquipmentModelData<TexturedModelData> getEquipmentModelData(Dilation hatDilation, Dilation armorDilation) {
      return createEquipmentModelData(ZombieVillagerEntityModel::getArmorTexturedModelData, hatDilation, armorDilation)
         .map(modelData -> TexturedModelData.of(modelData, 64, 32));
   }

   private static ModelData getArmorTexturedModelData(Dilation dilation) {
      ModelData modelData = BipedEntityModel.getModelData(dilation, 0.0F);
      ModelPartData modelPartData = modelData.getRoot();
      ModelPartData modelPartData2 = modelPartData.addChild(
         "head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -10.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation), ModelTransform.NONE
      );
      modelPartData.addChild(
         "body", ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation.add(0.1F)), ModelTransform.NONE
      );
      modelPartData.addChild(
         "right_leg",
         ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.1F)),
         ModelTransform.origin(-2.0F, 12.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_leg",
         ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.1F)),
         ModelTransform.origin(2.0F, 12.0F, 0.0F)
      );
      modelPartData2.getChild("hat").addChild("hat_rim", ModelPartBuilder.create(), ModelTransform.NONE);
      return modelData;
   }

   public void setAngles(S zombieVillagerRenderState) {
      super.setAngles(zombieVillagerRenderState);
      ArmPosing.zombieArms(this.leftArm, this.rightArm, zombieVillagerRenderState.attacking, zombieVillagerRenderState);
   }

   public void rotateArms(ZombieVillagerRenderState zombieVillagerRenderState, MatrixStack matrixStack) {
      this.setArmAngle(zombieVillagerRenderState, Arm.RIGHT, matrixStack);
   }
}
