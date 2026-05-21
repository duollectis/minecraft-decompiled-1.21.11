package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.CopperGolemAnimations;
import net.minecraft.client.render.entity.state.CopperGolemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CopperGolemState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

import java.util.Set;
import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
/**
 * {@code CopperGolemEntityModel}.
 */
public class CopperGolemEntityModel extends EntityModel<CopperGolemEntityRenderState> implements ModelWithArms<CopperGolemEntityRenderState>, ModelWithHead {

	private static final float BODY_SCALE_X = 2.0F;
	private static final float BODY_SCALE_Y = 2.5F;
	private static final float ANIMATION_SPEED = 0.015F;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final Animation walkingWithoutItemAnimation;
	private final Animation walkingWithItemAnimation;
	private final Animation spinHeadAnimation;
	private final Animation gettingItemAnimation;
	private final Animation gettingNoItemAnimation;
	private final Animation droppingItemAnimation;
	private final Animation droppingNoItemAnimation;

	public CopperGolemEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.body = modelPart.getChild("body");
		this.head = this.body.getChild("head");
		this.rightArm = this.body.getChild("right_arm");
		this.leftArm = this.body.getChild("left_arm");
		this.walkingWithoutItemAnimation = CopperGolemAnimations.WALKING_WITHOUT_ITEM.createAnimation(modelPart);
		this.walkingWithItemAnimation = CopperGolemAnimations.WALKING_WITH_ITEM.createAnimation(modelPart);
		this.spinHeadAnimation = CopperGolemAnimations.SPIN_HEAD.createAnimation(modelPart);
		this.gettingItemAnimation = CopperGolemAnimations.GETTING_ITEM.createAnimation(modelPart);
		this.gettingNoItemAnimation = CopperGolemAnimations.GETTING_NO_ITEM.createAnimation(modelPart);
		this.droppingItemAnimation = CopperGolemAnimations.DROPPING_ITEM.createAnimation(modelPart);
		this.droppingNoItemAnimation = CopperGolemAnimations.DROPPING_NO_ITEM.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData
				modelData =
				new ModelData().transform((UnaryOperator<ModelTransform>) transform -> transform.moveOrigin(
						0.0F,
						24.0F,
						0.0F
				));
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 15).cuboid(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, Dilation.NONE),
				ModelTransform.origin(0.0F, -5.0F, 0.0F)
		);
		modelPartData2.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F, new Dilation(0.015F))
				                .uv(56, 0)
				                .cuboid(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 2.0F, Dilation.NONE)
				                .uv(37, 8)
				                .cuboid(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F, new Dilation(-0.015F))
				                .uv(37, 0)
				                .cuboid(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(-0.015F)),
				ModelTransform.origin(0.0F, -6.0F, 0.0F)
		);
		modelPartData2.addChild(
				"right_arm",
				ModelPartBuilder.create().uv(36, 16).cuboid(-3.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, Dilation.NONE),
				ModelTransform.origin(-4.0F, -6.0F, 0.0F)
		);
		modelPartData2.addChild(
				"left_arm",
				ModelPartBuilder.create().uv(50, 16).cuboid(0.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, Dilation.NONE),
				ModelTransform.origin(4.0F, -6.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(0, 27).cuboid(-4.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, Dilation.NONE),
				ModelTransform.origin(0.0F, -5.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(16, 27).cuboid(0.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, Dilation.NONE),
				ModelTransform.origin(0.0F, -5.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public static TexturedModelData getRunningTexturedModelData() {
		ModelData
				modelData =
				new ModelData().transform((UnaryOperator<ModelTransform>) transform -> transform.moveOrigin(
						0.0F,
						0.0F,
						0.0F
				));
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("body", ModelPartBuilder.create(), ModelTransform.origin(-1.064F, -5.0F, 0.0F));
		modelPartData2.addChild(
				"body_r1",
				ModelPartBuilder
						.create()
						.uv(0, 15)
						.cuboid(-4.02F, -6.116F, -3.5F, 8.0F, 6.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.of(1.1F, 0.1F, 0.7F, 0.1204F, -0.0064F, -0.0779F)
		);
		modelPartData2.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, -5.1F, -5.0F, 8.0F, 5.0F, 10.0F, new Dilation(0.0F))
				                .uv(56, 0)
				                .cuboid(-1.02F, -2.1F, -6.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F))
				                .uv(37, 8)
				                .cuboid(-1.02F, -9.1F, -1.0F, 2.0F, 4.0F, 2.0F, new Dilation(-0.015F))
				                .uv(37, 0)
				                .cuboid(-2.0F, -13.1F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(-0.015F)),
				ModelTransform.origin(0.7F, -5.6F, -1.8F)
		);
		ModelPartData
				modelPartData3 =
				modelPartData2.addChild(
						"right_arm",
						ModelPartBuilder.create(),
						ModelTransform.origin(-4.0F, -6.0F, 0.0F)
				);
		modelPartData3.addChild(
				"right_arm_r1",
				ModelPartBuilder
						.create()
						.uv(36, 16)
						.cuboid(-3.052F, -1.11F, -2.036F, 3.0F, 10.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.7F, -0.248F, -1.62F, 1.0036F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData4 =
				modelPartData2.addChild(
						"left_arm",
						ModelPartBuilder.create(),
						ModelTransform.origin(4.0F, -6.0F, 0.0F)
				);
		modelPartData4.addChild(
				"left_arm_r1",
				ModelPartBuilder
						.create()
						.uv(50, 16)
						.cuboid(0.032F, -1.1F, -2.0F, 3.0F, 10.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.732F, 0.0F, 0.0F, -0.8715F, -0.0535F, -0.0449F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData.addChild(
						"right_leg",
						ModelPartBuilder.create(),
						ModelTransform.origin(-3.064F, -5.0F, 0.0F)
				);
		modelPartData5.addChild(
				"right_leg_r1",
				ModelPartBuilder
						.create()
						.uv(0, 27)
						.cuboid(-1.856F, -0.1F, -1.09F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(1.048F, 0.0F, -0.9F, -0.8727F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData6 =
				modelPartData.addChild(
						"left_leg",
						ModelPartBuilder.create(),
						ModelTransform.origin(0.936F, -5.0F, 0.0F)
				);
		modelPartData6.addChild(
				"left_leg_r1",
				ModelPartBuilder
						.create()
						.uv(16, 27)
						.cuboid(-2.088F, -0.1F, -2.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(1.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public static TexturedModelData getSittingTexturedModelData() {
		ModelData
				modelData =
				new ModelData().transform((UnaryOperator<ModelTransform>) transform -> transform.moveOrigin(
						0.0F,
						0.0F,
						0.0F
				));
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(3, 19)
				                .cuboid(-3.0F, -4.0F, -4.525F, 6.0F, 1.0F, 6.0F, new Dilation(0.0F))
				                .uv(0, 15)
				                .cuboid(-4.0F, -3.0F, -3.525F, 8.0F, 6.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -3.0F, 2.325F)
		);
		modelPartData2.addChild(
				"body_r1",
				ModelPartBuilder.create().uv(3, 18).cuboid(-4.0F, -3.0F, -2.2F, 8.0F, 6.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, -1.0F, -4.325F, 0.0F, 0.0F, -3.1416F)
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(37, 8)
				                .cuboid(-1.0F, -7.0F, -3.3F, 2.0F, 4.0F, 2.0F, new Dilation(-0.015F))
				                .uv(37, 0)
				                .cuboid(-2.0F, -11.0F, -4.3F, 4.0F, 4.0F, 4.0F, new Dilation(-0.015F))
				                .uv(0, 0)
				                .cuboid(-4.0F, -3.0F, -7.325F, 8.0F, 5.0F, 10.0F, new Dilation(0.0F))
				                .uv(56, 0)
				                .cuboid(-1.0F, 0.0F, -8.325F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -6.0F, -0.2F)
		);
		ModelPartData modelPartData4 = modelPartData2.addChild(
				"right_arm", ModelPartBuilder.create(), ModelTransform.of(-4.0F, -5.6F, -1.8F, 0.4363F, 0.0F, 0.0F)
		);
		modelPartData4.addChild(
				"right_arm_r1",
				ModelPartBuilder
						.create()
						.uv(36, 16)
						.cuboid(-3.075F, -0.9733F, -1.9966F, 3.0F, 10.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0893F, 0.1198F, -1.0472F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData2.addChild(
						"left_arm",
						ModelPartBuilder.create(),
						ModelTransform.of(4.0F, -5.6F, -1.7F, 0.4363F, 0.0F, 0.0F)
				);
		modelPartData5.addChild(
				"left_arm_r1",
				ModelPartBuilder
						.create()
						.uv(50, 16)
						.cuboid(0.075F, -1.0443F, -1.8997F, 3.0F, 10.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, -0.0015F, -0.0808F, -1.0472F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData6 =
				modelPartData.addChild(
						"right_leg",
						ModelPartBuilder.create(),
						ModelTransform.origin(-2.1F, -2.1F, -2.075F)
				);
		modelPartData6.addChild(
				"right_leg_r1",
				ModelPartBuilder.create().uv(0, 27).cuboid(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.05F, -1.9F, 1.075F, -1.5708F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData7 =
				modelPartData.addChild(
						"left_leg",
						ModelPartBuilder.create(),
						ModelTransform.origin(2.0F, -2.0F, -2.075F)
				);
		modelPartData7.addChild(
				"left_leg_r1",
				ModelPartBuilder.create().uv(16, 27).cuboid(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.05F, -2.0F, 1.075F, -1.5708F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public static TexturedModelData getStarTexturedModelData() {
		ModelData
				modelData =
				new ModelData().transform((UnaryOperator<ModelTransform>) transform -> transform.moveOrigin(
						0.0F,
						0.0F,
						0.0F
				));
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 15).cuboid(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, -5.0F, 0.0F)
		);
		modelPartData2.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F, new Dilation(0.0F))
				                .uv(56, 0)
				                .cuboid(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F))
				                .uv(37, 8)
				                .cuboid(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F, new Dilation(-0.015F))
				                .uv(37, 0)
				                .cuboid(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(-0.015F)),
				ModelTransform.origin(0.0F, -6.0F, 0.0F)
		);
		ModelPartData
				modelPartData3 =
				modelPartData2.addChild(
						"right_arm",
						ModelPartBuilder.create(),
						ModelTransform.origin(-4.0F, -6.0F, 0.0F)
				);
		modelPartData3.addChild(
				"right_arm_r1",
				ModelPartBuilder.create().uv(36, 16).cuboid(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.9199F)
		);
		modelPartData3.addChild("rightItem", ModelPartBuilder.create(), ModelTransform.origin(-1.0F, 7.4F, -1.0F));
		ModelPartData
				modelPartData4 =
				modelPartData2.addChild(
						"left_arm",
						ModelPartBuilder.create(),
						ModelTransform.origin(4.0F, -6.0F, 0.0F)
				);
		modelPartData4.addChild(
				"left_arm_r1",
				ModelPartBuilder.create().uv(50, 16).cuboid(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(-1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.9199F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData.addChild(
						"right_leg",
						ModelPartBuilder.create(),
						ModelTransform.origin(-3.0F, -5.0F, 0.0F)
				);
		modelPartData5.addChild(
				"right_leg_r1",
				ModelPartBuilder.create().uv(0, 27).cuboid(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.35F, 2.0F, 0.01F, 0.0F, 0.0F, 0.2618F)
		);
		ModelPartData
				modelPartData6 =
				modelPartData.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.origin(1.0F, -5.0F, 0.0F));
		modelPartData6.addChild(
				"left_leg_r1",
				ModelPartBuilder.create().uv(16, 27).cuboid(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(1.65F, 2.0F, 0.0F, 0.0F, 0.0F, -0.2618F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public static TexturedModelData getEyesTexturedModelData() {
		return getTexturedModelData().transform((ModelTransformer) transform -> {
			transform.getRoot().resetChildrenExcept(Set.of("eyes"));
			return transform;
		});
	}

	public void setAngles(CopperGolemEntityRenderState copperGolemEntityRenderState) {
		super.setAngles(copperGolemEntityRenderState);
		this.head.pitch = copperGolemEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = copperGolemEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		if (copperGolemEntityRenderState.rightHandItemState.isEmpty()
				&& copperGolemEntityRenderState.leftHandItemState.isEmpty()) {
			this.walkingWithoutItemAnimation
					.applyWalking(
							copperGolemEntityRenderState.limbSwingAnimationProgress,
							copperGolemEntityRenderState.limbSwingAmplitude,
							2.0F,
							2.5F
					);
		}
		else {
			this.walkingWithItemAnimation
					.applyWalking(
							copperGolemEntityRenderState.limbSwingAnimationProgress,
							copperGolemEntityRenderState.limbSwingAmplitude,
							2.0F,
							2.5F
					);
			this.clampArmRotations();
		}

		this.spinHeadAnimation.apply(
				copperGolemEntityRenderState.spinHeadAnimationState,
				copperGolemEntityRenderState.age
		);
		this.gettingItemAnimation.apply(
				copperGolemEntityRenderState.gettingItemAnimationState,
				copperGolemEntityRenderState.age
		);
		this.gettingNoItemAnimation.apply(
				copperGolemEntityRenderState.gettingNoItemAnimationState,
				copperGolemEntityRenderState.age
		);
		this.droppingItemAnimation.apply(
				copperGolemEntityRenderState.droppingItemAnimationState,
				copperGolemEntityRenderState.age
		);
		this.droppingNoItemAnimation.apply(
				copperGolemEntityRenderState.droppingNoItemAnimationState,
				copperGolemEntityRenderState.age
		);
	}

	public void setArmAngle(
			CopperGolemEntityRenderState copperGolemEntityRenderState,
			Arm arm,
			MatrixStack matrixStack
	) {
		this.root.applyTransform(matrixStack);
		this.body.applyTransform(matrixStack);
		ModelPart modelPart = arm == Arm.RIGHT ? this.rightArm : this.leftArm;
		modelPart.applyTransform(matrixStack);
		if (copperGolemEntityRenderState.copperGolemState.equals(CopperGolemState.IDLE)) {
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm == Arm.RIGHT ? -90.0F : 90.0F));
			matrixStack.translate(0.0F, 0.0F, 0.125F);
		}
		else {
			matrixStack.scale(0.55F, 0.55F, 0.55F);
			matrixStack.translate(-0.125F, 0.3125F, -0.1875F);
		}
	}

	@Override
	public ModelPart getHead() {
		return this.head;
	}

	@Override
	public void applyTransform(MatrixStack matrices) {
		this.body.applyTransform(matrices);
		this.head.applyTransform(matrices);
		matrices.translate(0.0F, 0.125F, 0.0F);
		matrices.scale(1.0625F, 1.0625F, 1.0625F);
	}

	/**
	 * Трансформирует matrices for block.
	 *
	 * @param matrices matrices
	 */
	public void transformMatricesForBlock(MatrixStack matrices) {
		this.root.applyTransform(matrices);
		this.body.applyTransform(matrices);
		this.head.applyTransform(matrices);
		matrices.translate(0.0, -2.25, 0.0);
	}

	private void clampArmRotations() {
		this.rightArm.pitch = Math.min(this.rightArm.pitch, -0.87266463F);
		this.leftArm.pitch = Math.min(this.leftArm.pitch, -0.87266463F);
		this.rightArm.yaw = Math.min(this.rightArm.yaw, -0.1134464F);
		this.leftArm.yaw = Math.max(this.leftArm.yaw, 0.1134464F);
		this.rightArm.roll = Math.min(this.rightArm.roll, -0.064577185F);
		this.leftArm.roll = Math.max(this.leftArm.roll, 0.064577185F);
	}
}
