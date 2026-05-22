package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.ArmadilloAnimations;
import net.minecraft.client.render.entity.state.ArmadilloEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code ArmadilloEntityModel}.
 */
public class ArmadilloEntityModel extends EntityModel<ArmadilloEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = ModelTransformer.scaling(0.6F);
	private static final float MAX_HEAD_PITCH = 25.0F;
	private static final float MIN_HEAD_PITCH = 22.5F;
	private static final float WALK_ANIMATION_SPEED = 16.5F;
	private static final float WALK_ANIMATION_AMPLITUDE = 2.5F;
	private static final String HEAD_CUBE = "head_cube";
	private static final String RIGHT_EAR_CUBE = "right_ear_cube";
	private static final String LEFT_EAR_CUBE = "left_ear_cube";
	private final ModelPart body;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart cube;
	private final ModelPart head;
	private final ModelPart tail;
	private final Animation walkingAnimation;
	private final Animation unrollingAnimation;
	private final Animation rollingAnimation;
	private final Animation scaredAnimation;

	public ArmadilloEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.body = modelPart.getChild("body");
		this.rightHindLeg = modelPart.getChild("right_hind_leg");
		this.leftHindLeg = modelPart.getChild("left_hind_leg");
		this.head = this.body.getChild("head");
		this.tail = this.body.getChild("tail");
		this.cube = modelPart.getChild("cube");
		this.walkingAnimation = ArmadilloAnimations.WALKING.createAnimation(modelPart);
		this.unrollingAnimation = ArmadilloAnimations.UNROLLING.createAnimation(modelPart);
		this.rollingAnimation = ArmadilloAnimations.ROLLING.createAnimation(modelPart);
		this.scaredAnimation = ArmadilloAnimations.SCARED.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create()
				                .uv(0, 20)
				                .cuboid(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new Dilation(0.3F))
				                .uv(0, 40)
				                .cuboid(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 21.0F, 4.0F)
		);
		modelPartData2.addChild(
				"tail",
				ModelPartBuilder
						.create()
						.uv(44, 53)
						.cuboid(-0.5F, -0.0865F, 0.0933F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData3 =
				modelPartData2.addChild("head", ModelPartBuilder.create(), ModelTransform.origin(0.0F, -2.0F, -11.0F));
		modelPartData3.addChild(
				"head_cube",
				ModelPartBuilder.create().uv(43, 15).cuboid(-1.5F, -1.0F, -1.0F, 3.0F, 5.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F)
		);
		ModelPartData
				modelPartData4 =
				modelPartData3.addChild(
						"right_ear",
						ModelPartBuilder.create(),
						ModelTransform.origin(-1.0F, -1.0F, 0.0F)
				);
		modelPartData4.addChild(
				"right_ear_cube",
				ModelPartBuilder.create().uv(43, 10).cuboid(-2.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.of(-0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F)
		);
		ModelPartData
				modelPartData5 =
				modelPartData3.addChild(
						"left_ear",
						ModelPartBuilder.create(),
						ModelTransform.origin(1.0F, -2.0F, 0.0F)
				);
		modelPartData5.addChild(
				"left_ear_cube",
				ModelPartBuilder.create().uv(47, 10).cuboid(0.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.of(0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F)
		);
		modelPartData.addChild(
				"right_hind_leg",
				ModelPartBuilder.create().uv(51, 31).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.origin(-2.0F, 21.0F, 4.0F)
		);
		modelPartData.addChild(
				"left_hind_leg",
				ModelPartBuilder.create().uv(42, 31).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.origin(2.0F, 21.0F, 4.0F)
		);
		modelPartData.addChild(
				"right_front_leg",
				ModelPartBuilder.create().uv(51, 43).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.origin(-2.0F, 21.0F, -4.0F)
		);
		modelPartData.addChild(
				"left_front_leg",
				ModelPartBuilder.create().uv(42, 43).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.origin(2.0F, 21.0F, -4.0F)
		);
		modelPartData.addChild(
				"cube",
				ModelPartBuilder
						.create()
						.uv(0, 0)
						.cuboid(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 10.0F, new Dilation(0.0F)),
				ModelTransform.origin(0.0F, 24.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(ArmadilloEntityRenderState armadilloEntityRenderState) {
		super.setAngles(armadilloEntityRenderState);
		if (armadilloEntityRenderState.rolledUp) {
			this.body.hidden = true;
			this.leftHindLeg.visible = false;
			this.rightHindLeg.visible = false;
			this.tail.visible = false;
			this.cube.visible = true;
		}
		else {
			this.body.hidden = false;
			this.leftHindLeg.visible = true;
			this.rightHindLeg.visible = true;
			this.tail.visible = true;
			this.cube.visible = false;
			this.head.pitch =
					MathHelper.clamp(armadilloEntityRenderState.pitch, -MIN_HEAD_PITCH, MAX_HEAD_PITCH) * (float) (Math.PI / 180.0);
			this.head.yaw =
					MathHelper.clamp(armadilloEntityRenderState.relativeHeadYaw, -32.5F, 32.5F) * (float) (Math.PI
							/ 180.0
					);
		}

		this.walkingAnimation.applyWalking(
				armadilloEntityRenderState.limbSwingAnimationProgress,
				armadilloEntityRenderState.limbSwingAmplitude,
				WALK_ANIMATION_SPEED,
				WALK_ANIMATION_AMPLITUDE
		);
		this.unrollingAnimation.apply(
				armadilloEntityRenderState.unrollingAnimationState,
				armadilloEntityRenderState.age
		);
		this.rollingAnimation.apply(armadilloEntityRenderState.rollingAnimationState, armadilloEntityRenderState.age);
		this.scaredAnimation.apply(armadilloEntityRenderState.scaredAnimationState, armadilloEntityRenderState.age);
	}
}
