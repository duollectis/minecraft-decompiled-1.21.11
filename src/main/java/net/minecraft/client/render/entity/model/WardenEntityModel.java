package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.WardenAnimations;
import net.minecraft.client.render.entity.state.WardenEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code WardenEntityModel}.
 */
public class WardenEntityModel extends EntityModel<WardenEntityRenderState> {

	private static final float TENDRIL_ROTATION_SCALE = 13.0F;
	private static final float TENDRIL_IDLE_ANGLE = 1.0F;
	protected final ModelPart bone;
	protected final ModelPart body;
	protected final ModelPart head;
	protected final ModelPart rightTendril;
	protected final ModelPart leftTendril;
	protected final ModelPart leftLeg;
	protected final ModelPart leftArm;
	protected final ModelPart leftRibcage;
	protected final ModelPart rightArm;
	protected final ModelPart rightLeg;
	protected final ModelPart rightRibcage;
	private final Animation attackingAnimation;
	private final Animation chargingSonicBoomAnimation;
	private final Animation diggingAnimation;
	private final Animation emergingAnimation;
	private final Animation roaringAnimation;
	private final Animation sniffingAnimation;

	public WardenEntityModel(ModelPart modelPart) {
		super(modelPart, RenderLayers::entityCutoutNoCull);
		this.bone = modelPart.getChild("bone");
		this.body = this.bone.getChild("body");
		this.head = this.body.getChild("head");
		this.rightLeg = this.bone.getChild("right_leg");
		this.leftLeg = this.bone.getChild("left_leg");
		this.rightArm = this.body.getChild("right_arm");
		this.leftArm = this.body.getChild("left_arm");
		this.rightTendril = this.head.getChild("right_tendril");
		this.leftTendril = this.head.getChild("left_tendril");
		this.rightRibcage = this.body.getChild("right_ribcage");
		this.leftRibcage = this.body.getChild("left_ribcage");
		this.attackingAnimation = WardenAnimations.ATTACKING.createAnimation(modelPart);
		this.chargingSonicBoomAnimation = WardenAnimations.CHARGING_SONIC_BOOM.createAnimation(modelPart);
		this.diggingAnimation = WardenAnimations.DIGGING.createAnimation(modelPart);
		this.emergingAnimation = WardenAnimations.EMERGING.createAnimation(modelPart);
		this.roaringAnimation = WardenAnimations.ROARING.createAnimation(modelPart);
		this.sniffingAnimation = WardenAnimations.SNIFFING.createAnimation(modelPart);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("bone", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 24.0F, 0.0F));
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-9.0F, -13.0F, -4.0F, 18.0F, 21.0F, 11.0F),
				ModelTransform.origin(0.0F, -21.0F, 0.0F)
		);
		modelPartData3.addChild(
				"right_ribcage",
				ModelPartBuilder.create().uv(90, 11).cuboid(-2.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F),
				ModelTransform.origin(-7.0F, -2.0F, -4.0F)
		);
		modelPartData3.addChild(
				"left_ribcage",
				ModelPartBuilder
						.create()
						.uv(90, 11)
						.mirrored()
						.cuboid(-7.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F)
						.mirrored(false),
				ModelTransform.origin(7.0F, -2.0F, -4.0F)
		);
		ModelPartData modelPartData4 = modelPartData3.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 32).cuboid(-8.0F, -16.0F, -5.0F, 16.0F, 16.0F, 10.0F),
				ModelTransform.origin(0.0F, -13.0F, 0.0F)
		);
		modelPartData4.addChild(
				"right_tendril",
				ModelPartBuilder.create().uv(52, 32).cuboid(-16.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F),
				ModelTransform.origin(-8.0F, -12.0F, 0.0F)
		);
		modelPartData4.addChild(
				"left_tendril",
				ModelPartBuilder.create().uv(58, 0).cuboid(0.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F),
				ModelTransform.origin(8.0F, -12.0F, 0.0F)
		);
		modelPartData3.addChild(
				"right_arm",
				ModelPartBuilder.create().uv(44, 50).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F),
				ModelTransform.origin(-13.0F, -13.0F, 1.0F)
		);
		modelPartData3.addChild(
				"left_arm",
				ModelPartBuilder.create().uv(0, 58).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F),
				ModelTransform.origin(13.0F, -13.0F, 1.0F)
		);
		modelPartData2.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(76, 48).cuboid(-3.1F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F),
				ModelTransform.origin(-5.9F, -13.0F, 0.0F)
		);
		modelPartData2.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(76, 76).cuboid(-2.9F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F),
				ModelTransform.origin(5.9F, -13.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 128, 128);
	}

	public static TexturedModelData getTendrilsTexturedModelData() {
		return getTexturedModelData().transform(modelData -> {
			modelData.getRoot().resetChildrenExceptExact(Set.of("left_tendril", "right_tendril"));
			return modelData;
		});
	}

	public static TexturedModelData getHeartTexturedModelData() {
		return getTexturedModelData().transform(modelData -> {
			modelData.getRoot().resetChildrenExceptExact(Set.of("body"));
			return modelData;
		});
	}

	public static TexturedModelData getBioluminescentTexturedModelData() {
		return getTexturedModelData().transform(modelData -> {
			modelData
					.getRoot()
					.resetChildrenExceptExact(Set.of("head", "left_arm", "right_arm", "left_leg", "right_leg"));
			return modelData;
		});
	}

	public static TexturedModelData getPulsatingSpotsTexturedModelData() {
		return getTexturedModelData().transform(modelData -> {
			modelData
					.getRoot()
					.resetChildrenExceptExact(Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"));
			return modelData;
		});
	}

	public void setAngles(WardenEntityRenderState wardenEntityRenderState) {
		super.setAngles(wardenEntityRenderState);
		this.setHeadAngle(wardenEntityRenderState.relativeHeadYaw, wardenEntityRenderState.pitch);
		this.setLimbAngles(
				wardenEntityRenderState.limbSwingAnimationProgress,
				wardenEntityRenderState.limbSwingAmplitude
		);
		this.setHeadAndBodyAngles(wardenEntityRenderState.age);
		this.setTendrilPitches(wardenEntityRenderState, wardenEntityRenderState.age);
		this.attackingAnimation.apply(wardenEntityRenderState.attackingAnimationState, wardenEntityRenderState.age);
		this.chargingSonicBoomAnimation.apply(
				wardenEntityRenderState.chargingSonicBoomAnimationState,
				wardenEntityRenderState.age
		);
		this.diggingAnimation.apply(wardenEntityRenderState.diggingAnimationState, wardenEntityRenderState.age);
		this.emergingAnimation.apply(wardenEntityRenderState.emergingAnimationState, wardenEntityRenderState.age);
		this.roaringAnimation.apply(wardenEntityRenderState.roaringAnimationState, wardenEntityRenderState.age);
		this.sniffingAnimation.apply(wardenEntityRenderState.sniffingAnimationState, wardenEntityRenderState.age);
	}

	private void setHeadAngle(float yaw, float pitch) {
		this.head.pitch = pitch * (float) (Math.PI / 180.0);
		this.head.yaw = yaw * (float) (Math.PI / 180.0);
	}

	private void setHeadAndBodyAngles(float animationProgress) {
		float f = animationProgress * 0.1F;
		float g = MathHelper.cos(f);
		float h = MathHelper.sin(f);
		this.head.roll += 0.06F * g;
		this.head.pitch += 0.06F * h;
		this.body.roll += 0.025F * h;
		this.body.pitch += 0.025F * g;
	}

	private void setLimbAngles(float angle, float distance) {
		float f = Math.min(0.5F, 3.0F * distance);
		float g = angle * 0.8662F;
		float h = MathHelper.cos(g);
		float i = MathHelper.sin(g);
		float j = Math.min(0.35F, f);
		this.head.roll += 0.3F * i * f;
		this.head.pitch = this.head.pitch + 1.2F * MathHelper.cos(g + (float) (Math.PI / 2)) * j;
		this.body.roll = 0.1F * i * f;
		this.body.pitch = 1.0F * h * j;
		this.leftLeg.pitch = 1.0F * h * f;
		this.rightLeg.pitch = 1.0F * MathHelper.cos(g + (float) Math.PI) * f;
		this.leftArm.pitch = -(0.8F * h * f);
		this.leftArm.roll = 0.0F;
		this.rightArm.pitch = -(0.8F * i * f);
		this.rightArm.roll = 0.0F;
		this.setArmPivots();
	}

	private void setArmPivots() {
		this.leftArm.yaw = 0.0F;
		this.leftArm.originZ = 1.0F;
		this.leftArm.originX = 13.0F;
		this.leftArm.originY = -13.0F;
		this.rightArm.yaw = 0.0F;
		this.rightArm.originZ = 1.0F;
		this.rightArm.originX = -13.0F;
		this.rightArm.originY = -13.0F;
	}

	private void setTendrilPitches(WardenEntityRenderState state, float animationProgress) {
		float f = state.tendrilAlpha * (float) (Math.cos(animationProgress * 2.25) * Math.PI * 0.1F);
		this.leftTendril.pitch = f;
		this.rightTendril.pitch = -f;
	}
}
