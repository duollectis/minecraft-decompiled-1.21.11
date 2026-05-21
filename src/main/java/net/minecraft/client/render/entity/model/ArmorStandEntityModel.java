package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
/**
 * {@code ArmorStandEntityModel}.
 */
public class ArmorStandEntityModel extends ArmorStandArmorEntityModel {

	private static final String RIGHT_BODY_STICK = "right_body_stick";
	private static final String LEFT_BODY_STICK = "left_body_stick";
	private static final String SHOULDER_STICK = "shoulder_stick";
	private static final String BASE_PLATE = "base_plate";
	private final ModelPart rightBodyStick;
	private final ModelPart leftBodyStick;
	private final ModelPart shoulderStick;
	private final ModelPart basePlate;

	public ArmorStandEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.rightBodyStick = modelPart.getChild("right_body_stick");
		this.leftBodyStick = modelPart.getChild("left_body_stick");
		this.shoulderStick = modelPart.getChild("shoulder_stick");
		this.basePlate = modelPart.getChild("base_plate");
		this.hat.visible = false;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				ModelTransform.origin(0.0F, 1.0F, 0.0F)
		);
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 26).cuboid(-6.0F, 0.0F, -1.5F, 12.0F, 3.0F, 3.0F),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"right_arm",
				ModelPartBuilder.create().uv(24, 0).cuboid(-2.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				ModelTransform.origin(-5.0F, 2.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_arm",
				ModelPartBuilder.create().uv(32, 16).mirrored().cuboid(0.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				ModelTransform.origin(5.0F, 2.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
				ModelTransform.origin(-1.9F, 12.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(40, 16).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
				ModelTransform.origin(1.9F, 12.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_body_stick",
				ModelPartBuilder.create().uv(16, 0).cuboid(-3.0F, 3.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"left_body_stick",
				ModelPartBuilder.create().uv(48, 16).cuboid(1.0F, 3.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"shoulder_stick",
				ModelPartBuilder.create().uv(0, 48).cuboid(-4.0F, 10.0F, -1.0F, 8.0F, 2.0F, 2.0F),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"base_plate",
				ModelPartBuilder.create().uv(0, 32).cuboid(-6.0F, 11.0F, -6.0F, 12.0F, 1.0F, 12.0F),
				ModelTransform.origin(0.0F, 12.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(ArmorStandEntityRenderState armorStandEntityRenderState) {
		super.setAngles(armorStandEntityRenderState);
		this.basePlate.yaw = (float) (Math.PI / 180.0) * -armorStandEntityRenderState.yaw;
		this.leftArm.visible = armorStandEntityRenderState.showArms;
		this.rightArm.visible = armorStandEntityRenderState.showArms;
		this.basePlate.visible = armorStandEntityRenderState.showBasePlate;
		this.rightBodyStick.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.pitch();
		this.rightBodyStick.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.yaw();
		this.rightBodyStick.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.roll();
		this.leftBodyStick.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.pitch();
		this.leftBodyStick.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.yaw();
		this.leftBodyStick.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.roll();
		this.shoulderStick.pitch = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.pitch();
		this.shoulderStick.yaw = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.yaw();
		this.shoulderStick.roll = (float) (Math.PI / 180.0) * armorStandEntityRenderState.bodyRotation.roll();
	}

	public void setArmAngle(ArmorStandEntityRenderState armorStandEntityRenderState, Arm arm, MatrixStack matrixStack) {
		ModelPart modelPart = this.getArm(arm);
		boolean bl = modelPart.visible;
		modelPart.visible = true;
		super.setArmAngle(armorStandEntityRenderState, arm, matrixStack);
		modelPart.visible = bl;
	}
}
