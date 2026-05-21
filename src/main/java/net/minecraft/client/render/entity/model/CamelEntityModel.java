package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.CamelAnimations;
import net.minecraft.client.render.entity.state.CamelEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code CamelEntityModel}.
 */
public class CamelEntityModel extends EntityModel<CamelEntityRenderState> {

	private static final float LIMB_ANGLE_SCALE = 2.0F;
	private static final float LIMB_DISTANCE_SCALE = 2.5F;
	public static final ModelTransformer BABY_TRANSFORMER = ModelTransformer.scaling(0.45F);
	protected final ModelPart head;
	private final Animation walkingAnimation;
	private final Animation sittingTransitionAnimation;
	private final Animation sittingAnimation;
	private final Animation standingTransitionAnimation;
	private final Animation idlingAnimation;
	private final Animation dashingAnimation;

	public CamelEntityModel(ModelPart modelPart) {
		super(modelPart);
		ModelPart modelPart2 = modelPart.getChild("body");
		this.head = modelPart2.getChild("head");
		this.walkingAnimation = CamelAnimations.WALKING.createAnimation(modelPart);
		this.sittingTransitionAnimation = CamelAnimations.SITTING_TRANSITION.createAnimation(modelPart);
		this.sittingAnimation = CamelAnimations.SITTING.createAnimation(modelPart);
		this.standingTransitionAnimation = CamelAnimations.STANDING_TRANSITION.createAnimation(modelPart);
		this.idlingAnimation = CamelAnimations.IDLING.createAnimation(modelPart);
		this.dashingAnimation = CamelAnimations.DASHING.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		return TexturedModelData.of(getModelData(), 128, 128);
	}

	protected static ModelData getModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 25).cuboid(-7.5F, -12.0F, -23.5F, 15.0F, 12.0F, 27.0F),
				ModelTransform.origin(0.0F, 4.0F, 9.5F)
		);
		modelPartData2.addChild(
				"hump",
				ModelPartBuilder.create().uv(74, 0).cuboid(-4.5F, -5.0F, -5.5F, 9.0F, 5.0F, 11.0F),
				ModelTransform.origin(0.0F, -12.0F, -10.0F)
		);
		modelPartData2.addChild(
				"tail",
				ModelPartBuilder.create().uv(122, 0).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 0.0F),
				ModelTransform.origin(0.0F, -9.0F, 3.5F)
		);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"head",
				ModelPartBuilder.create()
				                .uv(60, 24)
				                .cuboid(-3.5F, -7.0F, -15.0F, 7.0F, 8.0F, 19.0F)
				                .uv(21, 0)
				                .cuboid(-3.5F, -21.0F, -15.0F, 7.0F, 14.0F, 7.0F)
				                .uv(50, 0)
				                .cuboid(-2.5F, -21.0F, -21.0F, 5.0F, 5.0F, 6.0F),
				ModelTransform.origin(0.0F, -3.0F, -19.5F)
		);
		modelPartData3.addChild(
				"left_ear",
				ModelPartBuilder.create().uv(45, 0).cuboid(-0.5F, 0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
				ModelTransform.origin(2.5F, -21.0F, -9.5F)
		);
		modelPartData3.addChild(
				"right_ear",
				ModelPartBuilder.create().uv(67, 0).cuboid(-2.5F, 0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
				ModelTransform.origin(-2.5F, -21.0F, -9.5F)
		);
		modelPartData.addChild(
				"left_hind_leg",
				ModelPartBuilder.create().uv(58, 16).cuboid(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F),
				ModelTransform.origin(4.9F, 1.0F, 9.5F)
		);
		modelPartData.addChild(
				"right_hind_leg",
				ModelPartBuilder.create().uv(94, 16).cuboid(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F),
				ModelTransform.origin(-4.9F, 1.0F, 9.5F)
		);
		modelPartData.addChild(
				"left_front_leg",
				ModelPartBuilder.create().uv(0, 0).cuboid(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F),
				ModelTransform.origin(4.9F, 1.0F, -10.5F)
		);
		modelPartData.addChild(
				"right_front_leg",
				ModelPartBuilder.create().uv(0, 26).cuboid(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F),
				ModelTransform.origin(-4.9F, 1.0F, -10.5F)
		);
		return modelData;
	}

	public void setAngles(CamelEntityRenderState camelEntityRenderState) {
		super.setAngles(camelEntityRenderState);
		this.setHeadAngles(
				camelEntityRenderState,
				camelEntityRenderState.relativeHeadYaw,
				camelEntityRenderState.pitch
		);
		this.walkingAnimation.applyWalking(
				camelEntityRenderState.limbSwingAnimationProgress,
				camelEntityRenderState.limbSwingAmplitude,
				2.0F,
				2.5F
		);
		this.sittingTransitionAnimation.apply(
				camelEntityRenderState.sittingTransitionAnimationState,
				camelEntityRenderState.age
		);
		this.sittingAnimation.apply(camelEntityRenderState.sittingAnimationState, camelEntityRenderState.age);
		this.standingTransitionAnimation.apply(
				camelEntityRenderState.standingTransitionAnimationState,
				camelEntityRenderState.age
		);
		this.idlingAnimation.apply(camelEntityRenderState.idlingAnimationState, camelEntityRenderState.age);
		this.dashingAnimation.apply(camelEntityRenderState.dashingAnimationState, camelEntityRenderState.age);
	}

	private void setHeadAngles(CamelEntityRenderState state, float headYaw, float headPitch) {
		headYaw = MathHelper.clamp(headYaw, -30.0F, 30.0F);
		headPitch = MathHelper.clamp(headPitch, -25.0F, 45.0F);
		if (state.jumpCooldown > 0.0F) {
			float f = 45.0F * state.jumpCooldown / 55.0F;
			headPitch = MathHelper.clamp(headPitch + f, -25.0F, 70.0F);
		}

		this.head.yaw = headYaw * (float) (Math.PI / 180.0);
		this.head.pitch = headPitch * (float) (Math.PI / 180.0);
	}
}
