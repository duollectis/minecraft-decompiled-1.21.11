package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.SkeletonEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code SkeletonEntityModel}.
 */
public class SkeletonEntityModel<S extends SkeletonEntityRenderState> extends BipedEntityModel<S> {

	public SkeletonEntityModel(ModelPart modelPart) {
		super(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
		ModelPartData modelPartData = modelData.getRoot();
		addLimbs(modelPartData);
		return TexturedModelData.of(modelData, 64, 32);
	}

	/**
	 * Добавляет limbs.
	 *
	 * @param data data
	 */
	protected static void addLimbs(ModelPartData data) {
		data.addChild(
				"right_arm",
				ModelPartBuilder.create().uv(40, 16).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				ModelTransform.origin(-5.0F, 2.0F, 0.0F)
		);
		data.addChild(
				"left_arm",
				ModelPartBuilder.create().uv(40, 16).mirrored().cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				ModelTransform.origin(5.0F, 2.0F, 0.0F)
		);
		data.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(0, 16).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				ModelTransform.origin(-2.0F, 12.0F, 0.0F)
		);
		data.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				ModelTransform.origin(2.0F, 12.0F, 0.0F)
		);
	}

	public static TexturedModelData getParchedTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(16, 16)
				                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				                .uv(28, 0)
				                .cuboid(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F)
				                .uv(16, 48)
				                .cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.025F)),
				ModelTransform.origin(0.0F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				             "head",
				             ModelPartBuilder.create()
				                             .uv(0, 0)
				                             .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				                             .uv(0, 32)
				                             .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.2F)),
				             ModelTransform.origin(0.0F, 0.0F, 0.0F)
		             )
		             .addChild("hat", ModelPartBuilder.create(), ModelTransform.NONE);
		modelPartData.addChild(
				"right_arm",
				ModelPartBuilder
						.create()
						.uv(40, 16)
						.cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
						.uv(42, 33)
						.cuboid(-1.55F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
				ModelTransform.origin(-5.5F, 2.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_arm",
				ModelPartBuilder
						.create()
						.uv(56, 16)
						.cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
						.uv(40, 48)
						.cuboid(-1.45F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F),
				ModelTransform.origin(5.5F, 2.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_leg",
				ModelPartBuilder
						.create()
						.uv(0, 16)
						.cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
						.uv(0, 49)
						.cuboid(-1.5F, -0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
				ModelTransform.origin(-2.0F, 12.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_leg",
				ModelPartBuilder
						.create()
						.uv(0, 16)
						.cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
						.uv(4, 49)
						.cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
				ModelTransform.origin(2.0F, 12.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(S skeletonEntityRenderState) {
		super.setAngles(skeletonEntityRenderState);
		if (skeletonEntityRenderState.attacking && !skeletonEntityRenderState.holdingBow) {
			float f = skeletonEntityRenderState.handSwingProgress;
			float g = MathHelper.sin(f * (float) Math.PI);
			float h = MathHelper.sin((1.0F - (1.0F - f) * (1.0F - f)) * (float) Math.PI);
			this.rightArm.roll = 0.0F;
			this.leftArm.roll = 0.0F;
			this.rightArm.yaw = -(0.1F - g * 0.6F);
			this.leftArm.yaw = 0.1F - g * 0.6F;
			this.rightArm.pitch = (float) (-Math.PI / 2);
			this.leftArm.pitch = (float) (-Math.PI / 2);
			this.rightArm.pitch -= g * 1.2F - h * 0.4F;
			this.leftArm.pitch -= g * 1.2F - h * 0.4F;
			ArmPosing.swingArms(this.rightArm, this.leftArm, skeletonEntityRenderState.age);
		}
	}

	public void setArmAngle(SkeletonEntityRenderState skeletonEntityRenderState, Arm arm, MatrixStack matrixStack) {
		this.getRootPart().applyTransform(matrixStack);
		float f = arm == Arm.RIGHT ? 1.0F : -1.0F;
		ModelPart modelPart = this.getArm(arm);
		modelPart.originX += f;
		modelPart.applyTransform(matrixStack);
		modelPart.originX -= f;
	}
}
