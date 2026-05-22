package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.FrogAnimations;
import net.minecraft.client.render.entity.state.FrogEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code FrogEntityModel}.
 */
public class FrogEntityModel extends EntityModel<FrogEntityRenderState> {

	private static final float WALKING_LIMB_ANGLE_SCALE = 1.5F;
	private static final float SWIMMING_LIMB_ANGLE_SCALE = 1.0F;
	private static final float LIMB_DISTANCE_SCALE = 2.5F;
	private final ModelPart body = this.root.getChild("body");
	private final ModelPart head = this.body.getChild("head");
	private final ModelPart eyes = this.head.getChild("eyes");
	private final ModelPart tongue = this.body.getChild("tongue");
	private final ModelPart leftArm = this.body.getChild("left_arm");
	private final ModelPart rightArm = this.body.getChild("right_arm");
	private final ModelPart leftLeg = this.root.getChild("left_leg");
	private final ModelPart rightLeg = this.root.getChild("right_leg");
	private final ModelPart croakingBody = this.body.getChild("croaking_body");
	private final Animation longJumpingAnimation;
	private final Animation croakingAnimation;
	private final Animation usingTongueAnimation;
	private final Animation swimmingAnimation;
	private final Animation walkingAnimation;
	private final Animation idlingInWaterAnimation;

	public FrogEntityModel(ModelPart modelPart) {
		super(modelPart.getChild("root"));
		this.longJumpingAnimation = FrogAnimations.LONG_JUMPING.createAnimation(modelPart);
		this.croakingAnimation = FrogAnimations.CROAKING.createAnimation(modelPart);
		this.usingTongueAnimation = FrogAnimations.USING_TONGUE.createAnimation(modelPart);
		this.swimmingAnimation = FrogAnimations.SWIMMING.createAnimation(modelPart);
		this.walkingAnimation = FrogAnimations.WALKING.createAnimation(modelPart);
		this.idlingInWaterAnimation = FrogAnimations.IDLING_IN_WATER.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("root", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 24.0F, 0.0F));
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder
						.create()
						.uv(3, 1)
						.cuboid(-3.5F, -2.0F, -8.0F, 7.0F, 3.0F, 9.0F)
						.uv(23, 22)
						.cuboid(-3.5F, -1.0F, -8.0F, 7.0F, 0.0F, 9.0F),
				ModelTransform.origin(0.0F, -2.0F, 4.0F)
		);
		ModelPartData modelPartData4 = modelPartData3.addChild(
				"head",
				ModelPartBuilder
						.create()
						.uv(23, 13)
						.cuboid(-3.5F, -1.0F, -7.0F, 7.0F, 0.0F, 9.0F)
						.uv(0, 13)
						.cuboid(-3.5F, -2.0F, -7.0F, 7.0F, 3.0F, 9.0F),
				ModelTransform.origin(0.0F, -2.0F, -1.0F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData4.addChild("eyes", ModelPartBuilder.create(), ModelTransform.origin(-0.5F, 0.0F, 2.0F));
		modelPartData5.addChild(
				"right_eye",
				ModelPartBuilder.create().uv(0, 0).cuboid(-WALKING_LIMB_ANGLE_SCALE, -1.0F, -WALKING_LIMB_ANGLE_SCALE, 3.0F, 2.0F, 3.0F),
				ModelTransform.origin(-WALKING_LIMB_ANGLE_SCALE, -3.0F, -6.5F)
		);
		modelPartData5.addChild(
				"left_eye",
				ModelPartBuilder.create().uv(0, 5).cuboid(-WALKING_LIMB_ANGLE_SCALE, -1.0F, -WALKING_LIMB_ANGLE_SCALE, 3.0F, 2.0F, 3.0F),
				ModelTransform.origin(LIMB_DISTANCE_SCALE, -3.0F, -6.5F)
		);
		modelPartData3.addChild(
				"croaking_body",
				ModelPartBuilder.create().uv(26, 5).cuboid(-3.5F, -0.1F, -2.9F, 7.0F, 2.0F, 3.0F, new Dilation(-0.1F)),
				ModelTransform.origin(0.0F, -1.0F, -5.0F)
		);
		ModelPartData modelPartData6 = modelPartData3.addChild(
				"tongue",
				ModelPartBuilder.create().uv(17, 13).cuboid(-2.0F, 0.0F, -7.1F, 4.0F, 0.0F, 7.0F),
				ModelTransform.origin(0.0F, -1.01F, 1.0F)
		);
		ModelPartData modelPartData7 = modelPartData3.addChild(
				"left_arm",
				ModelPartBuilder.create().uv(0, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F),
				ModelTransform.origin(4.0F, -1.0F, -6.5F)
		);
		modelPartData7.addChild(
				"left_hand",
				ModelPartBuilder.create().uv(18, 40).cuboid(-4.0F, 0.01F, -4.0F, 8.0F, 0.0F, 8.0F),
				ModelTransform.origin(0.0F, 3.0F, -1.0F)
		);
		ModelPartData modelPartData8 = modelPartData3.addChild(
				"right_arm",
				ModelPartBuilder.create().uv(0, 38).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F),
				ModelTransform.origin(-4.0F, -1.0F, -6.5F)
		);
		modelPartData8.addChild(
				"right_hand",
				ModelPartBuilder.create().uv(2, 40).cuboid(-4.0F, 0.01F, -5.0F, 8.0F, 0.0F, 8.0F),
				ModelTransform.origin(0.0F, 3.0F, 0.0F)
		);
		ModelPartData modelPartData9 = modelPartData2.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(14, 25).cuboid(-1.0F, 0.0F, -2.0F, 3.0F, 3.0F, 4.0F),
				ModelTransform.origin(3.5F, -3.0F, 4.0F)
		);
		modelPartData9.addChild(
				"left_foot",
				ModelPartBuilder.create().uv(2, 32).cuboid(-4.0F, 0.01F, -4.0F, 8.0F, 0.0F, 8.0F),
				ModelTransform.origin(2.0F, 3.0F, 0.0F)
		);
		ModelPartData modelPartData10 = modelPartData2.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(0, 25).cuboid(-2.0F, 0.0F, -2.0F, 3.0F, 3.0F, 4.0F),
				ModelTransform.origin(-3.5F, -3.0F, 4.0F)
		);
		modelPartData10.addChild(
				"right_foot",
				ModelPartBuilder.create().uv(18, 32).cuboid(-4.0F, 0.01F, -4.0F, 8.0F, 0.0F, 8.0F),
				ModelTransform.origin(-2.0F, 3.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 48, 48);
	}

	public void setAngles(FrogEntityRenderState frogEntityRenderState) {
		super.setAngles(frogEntityRenderState);
		this.longJumpingAnimation.apply(frogEntityRenderState.longJumpingAnimationState, frogEntityRenderState.age);
		this.croakingAnimation.apply(frogEntityRenderState.croakingAnimationState, frogEntityRenderState.age);
		this.usingTongueAnimation.apply(frogEntityRenderState.usingTongueAnimationState, frogEntityRenderState.age);
		if (frogEntityRenderState.insideWaterOrBubbleColumn) {
			this.swimmingAnimation.applyWalking(
					frogEntityRenderState.limbSwingAnimationProgress,
					frogEntityRenderState.limbSwingAmplitude,
					1.0F,
					LIMB_DISTANCE_SCALE
			);
		}
		else {
			this.walkingAnimation.applyWalking(
					frogEntityRenderState.limbSwingAnimationProgress,
					frogEntityRenderState.limbSwingAmplitude,
					WALKING_LIMB_ANGLE_SCALE,
					LIMB_DISTANCE_SCALE
			);
		}

		this.idlingInWaterAnimation.apply(frogEntityRenderState.idlingInWaterAnimationState, frogEntityRenderState.age);
		this.croakingBody.visible = frogEntityRenderState.croakingAnimationState.isRunning();
	}
}
