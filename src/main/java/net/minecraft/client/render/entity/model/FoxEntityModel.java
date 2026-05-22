package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.FoxEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code FoxEntityModel}.
 */
public class FoxEntityModel extends EntityModel<FoxEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(true, 8.0F, 3.35F, Set.of("head"));
	public final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart tail;
	private static final int SLEEP_TAIL_ANGLE = 6;
	private static final float HEAD_Y_PIVOT = 16.5F;
	private static final float LEG_Y_PIVOT = 17.5F;
	private float legPitchModifier;

	public FoxEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.body = modelPart.getChild("body");
		this.rightHindLeg = modelPart.getChild("right_hind_leg");
		this.leftHindLeg = modelPart.getChild("left_hind_leg");
		this.rightFrontLeg = modelPart.getChild("right_front_leg");
		this.leftFrontLeg = modelPart.getChild("left_front_leg");
		this.tail = this.body.getChild("tail");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(1, 5).cuboid(-3.0F, -2.0F, -5.0F, 8.0F, 6.0F, 6.0F),
				ModelTransform.origin(-1.0F, HEAD_Y_PIVOT, -3.0F)
		);
		modelPartData2.addChild(
				"right_ear",
				ModelPartBuilder.create().uv(8, 1).cuboid(-3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"left_ear",
				ModelPartBuilder.create().uv(15, 1).cuboid(3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F),
				ModelTransform.NONE
		);
		modelPartData2.addChild(
				"nose",
				ModelPartBuilder.create().uv(6, 18).cuboid(-1.0F, 2.01F, -8.0F, 4.0F, 2.0F, 3.0F),
				ModelTransform.NONE
		);
		ModelPartData modelPartData3 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(24, 15).cuboid(-3.0F, 3.999F, -3.5F, 6.0F, 11.0F, 6.0F),
				ModelTransform.of(0.0F, 16.0F, -6.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		Dilation dilation = new Dilation(0.001F);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(4, 24).cuboid(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, dilation);
		ModelPartBuilder
				modelPartBuilder2 =
				ModelPartBuilder.create().uv(13, 24).cuboid(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, dilation);
		modelPartData.addChild("right_hind_leg", modelPartBuilder2, ModelTransform.origin(-5.0F, LEG_Y_PIVOT, 7.0F));
		modelPartData.addChild("left_hind_leg", modelPartBuilder, ModelTransform.origin(-1.0F, LEG_Y_PIVOT, 7.0F));
		modelPartData.addChild("right_front_leg", modelPartBuilder2, ModelTransform.origin(-5.0F, LEG_Y_PIVOT, 0.0F));
		modelPartData.addChild("left_front_leg", modelPartBuilder, ModelTransform.origin(-1.0F, LEG_Y_PIVOT, 0.0F));
		modelPartData3.addChild(
				"tail",
				ModelPartBuilder.create().uv(30, 0).cuboid(2.0F, 0.0F, -1.0F, 4.0F, 9.0F, 5.0F),
				ModelTransform.of(-4.0F, 15.0F, -1.0F, -0.05235988F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 48, 32);
	}

	public void setAngles(FoxEntityRenderState foxEntityRenderState) {
		super.setAngles(foxEntityRenderState);
		float f = foxEntityRenderState.limbSwingAmplitude;
		float g = foxEntityRenderState.limbSwingAnimationProgress;
		this.rightHindLeg.pitch = MathHelper.cos(g * 0.6662F) * 1.4F * f;
		this.leftHindLeg.pitch = MathHelper.cos(g * 0.6662F + (float) Math.PI) * 1.4F * f;
		this.rightFrontLeg.pitch = MathHelper.cos(g * 0.6662F + (float) Math.PI) * 1.4F * f;
		this.leftFrontLeg.pitch = MathHelper.cos(g * 0.6662F) * 1.4F * f;
		this.head.roll = foxEntityRenderState.headRoll;
		this.rightHindLeg.visible = true;
		this.leftHindLeg.visible = true;
		this.rightFrontLeg.visible = true;
		this.leftFrontLeg.visible = true;
		float h = foxEntityRenderState.ageScale;
		if (foxEntityRenderState.inSneakingPose) {
			this.body.pitch += 0.10471976F;
			float i = foxEntityRenderState.bodyRotationHeightOffset;
			this.body.originY += i * h;
			this.head.originY += i * h;
		}
		else if (foxEntityRenderState.sleeping) {
			this.body.roll = (float) (-Math.PI / 2);
			this.body.originY += 5.0F * h;
			this.tail.pitch = (float) (-Math.PI * 5.0 / 6.0);
			if (foxEntityRenderState.baby) {
				this.tail.pitch = -2.1816616F;
				this.body.originZ += 2.0F;
			}

			this.head.originX += 2.0F * h;
			this.head.originY += 2.99F * h;
			this.head.yaw = (float) (-Math.PI * 2.0 / 3.0);
			this.head.roll = 0.0F;
			this.rightHindLeg.visible = false;
			this.leftHindLeg.visible = false;
			this.rightFrontLeg.visible = false;
			this.leftFrontLeg.visible = false;
		}
		else if (foxEntityRenderState.sitting) {
			this.body.pitch = (float) (Math.PI / 6);
			this.body.originY -= 7.0F * h;
			this.body.originZ += 3.0F * h;
			this.tail.pitch = (float) (Math.PI / 4);
			this.tail.originZ -= 1.0F * h;
			this.head.pitch = 0.0F;
			this.head.yaw = 0.0F;
			if (foxEntityRenderState.baby) {
				this.head.originY--;
				this.head.originZ -= 0.375F;
			}
			else {
				this.head.originY -= 6.5F;
				this.head.originZ += 2.75F;
			}

			this.rightHindLeg.pitch = (float) (-Math.PI * 5.0 / 12.0);
			this.rightHindLeg.originY += 4.0F * h;
			this.rightHindLeg.originZ -= 0.25F * h;
			this.leftHindLeg.pitch = (float) (-Math.PI * 5.0 / 12.0);
			this.leftHindLeg.originY += 4.0F * h;
			this.leftHindLeg.originZ -= 0.25F * h;
			this.rightFrontLeg.pitch = (float) (-Math.PI / 12);
			this.leftFrontLeg.pitch = (float) (-Math.PI / 12);
		}

		if (!foxEntityRenderState.sleeping && !foxEntityRenderState.walking && !foxEntityRenderState.inSneakingPose) {
			this.head.pitch = foxEntityRenderState.pitch * (float) (Math.PI / 180.0);
			this.head.yaw = foxEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		}

		if (foxEntityRenderState.sleeping) {
			this.head.pitch = 0.0F;
			this.head.yaw = (float) (-Math.PI * 2.0 / 3.0);
			this.head.roll = MathHelper.cos(foxEntityRenderState.age * 0.027F) / 22.0F;
		}

		if (foxEntityRenderState.inSneakingPose) {
			float i = MathHelper.cos(foxEntityRenderState.age) * 0.01F;
			this.body.yaw = i;
			this.rightHindLeg.roll = i;
			this.leftHindLeg.roll = i;
			this.rightFrontLeg.roll = i / 2.0F;
			this.leftFrontLeg.roll = i / 2.0F;
		}

		if (foxEntityRenderState.walking) {
			float i = 0.1F;
			this.legPitchModifier += 0.67F;
			this.rightHindLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F) * 0.1F;
			this.leftHindLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F + (float) Math.PI) * 0.1F;
			this.rightFrontLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F + (float) Math.PI) * 0.1F;
			this.leftFrontLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F) * 0.1F;
		}
	}
}
