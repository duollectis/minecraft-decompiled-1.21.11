package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.WolfEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code WolfEntityModel}.
 */
public class WolfEntityModel extends EntityModel<WolfEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(Set.of("head"));
	private static final String REAL_HEAD = "real_head";
	private static final String UPPER_BODY = "upper_body";
	private static final String REAL_TAIL = "real_tail";
	private final ModelPart head;
	private final ModelPart realHead;
	private final ModelPart torso;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart tail;
	private final ModelPart realTail;
	private final ModelPart neck;
	private static final int TAIL_ANGLE_SCALE = 8;

	public WolfEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.head = modelPart.getChild("head");
		this.realHead = this.head.getChild("real_head");
		this.torso = modelPart.getChild("body");
		this.neck = modelPart.getChild("upper_body");
		this.rightHindLeg = modelPart.getChild("right_hind_leg");
		this.leftHindLeg = modelPart.getChild("left_hind_leg");
		this.rightFrontLeg = modelPart.getChild("right_front_leg");
		this.leftFrontLeg = modelPart.getChild("left_front_leg");
		this.tail = modelPart.getChild("tail");
		this.realTail = this.tail.getChild("real_tail");
	}

	public static ModelData getTexturedModelData(Dilation dilation) {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		float f = 13.5F;
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("head", ModelPartBuilder.create(), ModelTransform.origin(-1.0F, 13.5F, -7.0F));
		modelPartData2.addChild(
				"real_head",
				ModelPartBuilder.create()
				                .uv(0, 0)
				                .cuboid(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, dilation)
				                .uv(16, 14)
				                .cuboid(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, dilation)
				                .uv(16, 14)
				                .cuboid(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, dilation)
				                .uv(0, 10)
				                .cuboid(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, dilation),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(18, 14).cuboid(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, dilation),
				ModelTransform.of(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"upper_body",
				ModelPartBuilder.create().uv(21, 0).cuboid(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, dilation),
				ModelTransform.of(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(0, 18).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, dilation);
		ModelPartBuilder
				modelPartBuilder2 =
				ModelPartBuilder.create().mirrored().uv(0, 18).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, dilation);
		modelPartData.addChild("right_hind_leg", modelPartBuilder2, ModelTransform.origin(-2.5F, 16.0F, 7.0F));
		modelPartData.addChild("left_hind_leg", modelPartBuilder, ModelTransform.origin(0.5F, 16.0F, 7.0F));
		modelPartData.addChild("right_front_leg", modelPartBuilder2, ModelTransform.origin(-2.5F, 16.0F, -4.0F));
		modelPartData.addChild("left_front_leg", modelPartBuilder, ModelTransform.origin(0.5F, 16.0F, -4.0F));
		ModelPartData modelPartData3 = modelPartData.addChild(
				"tail",
				ModelPartBuilder.create(),
				ModelTransform.of(-1.0F, 12.0F, 8.0F, (float) (Math.PI / 5), 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"real_tail",
				ModelPartBuilder.create().uv(9, 18).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, dilation),
				ModelTransform.NONE
		);
		return modelData;
	}

	public void setAngles(WolfEntityRenderState wolfEntityRenderState) {
		super.setAngles(wolfEntityRenderState);
		float f = wolfEntityRenderState.limbSwingAnimationProgress;
		float g = wolfEntityRenderState.limbSwingAmplitude;
		if (wolfEntityRenderState.angerTime) {
			this.tail.yaw = 0.0F;
		}
		else {
			this.tail.yaw = MathHelper.cos(f * 0.6662F) * 1.4F * g;
		}

		if (wolfEntityRenderState.inSittingPose) {
			float h = wolfEntityRenderState.ageScale;
			this.neck.originY += 2.0F * h;
			this.neck.pitch = (float) (Math.PI * 2.0 / 5.0);
			this.neck.yaw = 0.0F;
			this.torso.originY += 4.0F * h;
			this.torso.originZ -= 2.0F * h;
			this.torso.pitch = (float) (Math.PI / 4);
			this.tail.originY += 9.0F * h;
			this.tail.originZ -= 2.0F * h;
			this.rightHindLeg.originY += 6.7F * h;
			this.rightHindLeg.originZ -= 5.0F * h;
			this.rightHindLeg.pitch = (float) (Math.PI * 3.0 / 2.0);
			this.leftHindLeg.originY += 6.7F * h;
			this.leftHindLeg.originZ -= 5.0F * h;
			this.leftHindLeg.pitch = (float) (Math.PI * 3.0 / 2.0);
			this.rightFrontLeg.pitch = 5.811947F;
			this.rightFrontLeg.originX += 0.01F * h;
			this.rightFrontLeg.originY += 1.0F * h;
			this.leftFrontLeg.pitch = 5.811947F;
			this.leftFrontLeg.originX -= 0.01F * h;
			this.leftFrontLeg.originY += 1.0F * h;
		}
		else {
			this.rightHindLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
			this.leftHindLeg.pitch = MathHelper.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
			this.rightFrontLeg.pitch = MathHelper.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
			this.leftFrontLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
		}

		this.realHead.roll = wolfEntityRenderState.begAnimationProgress + wolfEntityRenderState.getRoll(0.0F);
		this.neck.roll = wolfEntityRenderState.getRoll(-0.08F);
		this.torso.roll = wolfEntityRenderState.getRoll(-0.16F);
		this.realTail.roll = wolfEntityRenderState.getRoll(-0.2F);
		this.head.pitch = wolfEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = wolfEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.tail.pitch = wolfEntityRenderState.tailAngle;
	}
}
