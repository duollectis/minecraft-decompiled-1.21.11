package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.AxolotlEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code AxolotlEntityModel}.
 */
public class AxolotlEntityModel extends EntityModel<AxolotlEntityRenderState> {

	public static final float MOVING_IN_WATER_LEG_PITCH = 1.8849558F;
	public static final ModelTransformer BABY_TRANSFORMER = ModelTransformer.scaling(0.5F);
	private final ModelPart tail;
	private final ModelPart leftHindLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart topGills;
	private final ModelPart leftGills;
	private final ModelPart rightGills;

	public AxolotlEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.body = modelPart.getChild("body");
		this.head = this.body.getChild("head");
		this.rightHindLeg = this.body.getChild("right_hind_leg");
		this.leftHindLeg = this.body.getChild("left_hind_leg");
		this.rightFrontLeg = this.body.getChild("right_front_leg");
		this.leftFrontLeg = this.body.getChild("left_front_leg");
		this.tail = this.body.getChild("tail");
		this.topGills = this.head.getChild("top_gills");
		this.leftGills = this.head.getChild("left_gills");
		this.rightGills = this.head.getChild("right_gills");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder
						.create()
						.uv(0, 11)
						.cuboid(-4.0F, -2.0F, -9.0F, 8.0F, 4.0F, 10.0F)
						.uv(2, 17)
						.cuboid(0.0F, -3.0F, -8.0F, 0.0F, 5.0F, 9.0F),
				ModelTransform.origin(0.0F, 20.0F, 5.0F)
		);
		Dilation dilation = new Dilation(0.001F);
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 1).cuboid(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 5.0F, dilation),
				ModelTransform.origin(0.0F, 0.0F, -9.0F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(3, 37).cuboid(-4.0F, -3.0F, 0.0F, 8.0F, 3.0F, 0.0F, dilation);
		ModelPartBuilder
				modelPartBuilder2 =
				ModelPartBuilder.create().uv(0, 40).cuboid(-3.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, dilation);
		ModelPartBuilder
				modelPartBuilder3 =
				ModelPartBuilder.create().uv(11, 40).cuboid(0.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, dilation);
		modelPartData3.addChild("top_gills", modelPartBuilder, ModelTransform.origin(0.0F, -3.0F, -1.0F));
		modelPartData3.addChild("left_gills", modelPartBuilder2, ModelTransform.origin(-4.0F, 0.0F, -1.0F));
		modelPartData3.addChild("right_gills", modelPartBuilder3, ModelTransform.origin(4.0F, 0.0F, -1.0F));
		ModelPartBuilder
				modelPartBuilder4 =
				ModelPartBuilder.create().uv(2, 13).cuboid(-1.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, dilation);
		ModelPartBuilder
				modelPartBuilder5 =
				ModelPartBuilder.create().uv(2, 13).cuboid(-2.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, dilation);
		modelPartData2.addChild("right_hind_leg", modelPartBuilder5, ModelTransform.origin(-3.5F, 1.0F, -1.0F));
		modelPartData2.addChild("left_hind_leg", modelPartBuilder4, ModelTransform.origin(3.5F, 1.0F, -1.0F));
		modelPartData2.addChild("right_front_leg", modelPartBuilder5, ModelTransform.origin(-3.5F, 1.0F, -8.0F));
		modelPartData2.addChild("left_front_leg", modelPartBuilder4, ModelTransform.origin(3.5F, 1.0F, -8.0F));
		modelPartData2.addChild(
				"tail",
				ModelPartBuilder.create().uv(2, 19).cuboid(0.0F, -3.0F, 0.0F, 0.0F, 5.0F, 12.0F),
				ModelTransform.origin(0.0F, 0.0F, 1.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(AxolotlEntityRenderState axolotlEntityRenderState) {
		super.setAngles(axolotlEntityRenderState);
		float f = axolotlEntityRenderState.playingDeadValue;
		float g = axolotlEntityRenderState.inWaterValue;
		float h = axolotlEntityRenderState.onGroundValue;
		float i = axolotlEntityRenderState.isMovingValue;
		float j = 1.0F - i;
		float k = 1.0F - Math.min(h, i);
		this.body.yaw = this.body.yaw + axolotlEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		this.setMovingInWaterAngles(axolotlEntityRenderState.age, axolotlEntityRenderState.pitch, Math.min(i, g));
		this.setStandingInWaterAngles(axolotlEntityRenderState.age, Math.min(j, g));
		this.setMovingOnGroundAngles(axolotlEntityRenderState.age, Math.min(i, h));
		this.setStandingOnGroundAngles(axolotlEntityRenderState.age, Math.min(j, h));
		this.setPlayingDeadAngles(f);
		this.copyLegAngles(k);
	}

	private void setStandingOnGroundAngles(float animationProgress, float headYaw) {
		if (!(headYaw <= 1.0E-5F)) {
			float f = animationProgress * 0.09F;
			float g = MathHelper.sin(f);
			float h = MathHelper.cos(f);
			float i = g * g - 2.0F * g;
			float j = h * h - 3.0F * g;
			this.head.pitch += -0.09F * i * headYaw;
			this.head.roll += -0.2F * headYaw;
			this.tail.yaw += (-0.1F + 0.1F * i) * headYaw;
			float k = (0.6F + 0.05F * j) * headYaw;
			this.topGills.pitch += k;
			this.leftGills.yaw -= k;
			this.rightGills.yaw += k;
			this.leftHindLeg.pitch += 1.1F * headYaw;
			this.leftHindLeg.yaw += 1.0F * headYaw;
			this.leftFrontLeg.pitch += 0.8F * headYaw;
			this.leftFrontLeg.yaw += 2.3F * headYaw;
			this.leftFrontLeg.roll -= 0.5F * headYaw;
		}
	}

	private void setMovingOnGroundAngles(float animationProgress, float headYaw) {
		if (!(headYaw <= 1.0E-5F)) {
			float f = animationProgress * 0.11F;
			float g = MathHelper.cos(f);
			float h = (g * g - 2.0F * g) / 5.0F;
			float i = 0.7F * g;
			float j = 0.09F * g * headYaw;
			this.head.yaw += j;
			this.tail.yaw += j;
			float k = (0.6F - 0.08F * (g * g + 2.0F * MathHelper.sin(f))) * headYaw;
			this.topGills.pitch += k;
			this.leftGills.yaw -= k;
			this.rightGills.yaw += k;
			float l = 0.9424779F * headYaw;
			float m = 1.0995574F * headYaw;
			this.leftHindLeg.pitch += l;
			this.leftHindLeg.yaw += (1.5F - h) * headYaw;
			this.leftHindLeg.roll += -0.1F * headYaw;
			this.leftFrontLeg.pitch += m;
			this.leftFrontLeg.yaw += ((float) (Math.PI / 2) - i) * headYaw;
			this.rightHindLeg.pitch += l;
			this.rightHindLeg.yaw += (-1.0F - h) * headYaw;
			this.rightFrontLeg.pitch += m;
			this.rightFrontLeg.yaw += ((float) (-Math.PI / 2) - i) * headYaw;
		}
	}

	private void setStandingInWaterAngles(float f, float g) {
		if (!(g <= 1.0E-5F)) {
			float h = f * 0.075F;
			float i = MathHelper.cos(h);
			float j = MathHelper.sin(h) * 0.15F;
			float k = (-0.15F + 0.075F * i) * g;
			this.body.pitch += k;
			this.body.originY -= j * g;
			this.head.pitch -= k;
			this.topGills.pitch += 0.2F * i * g;
			float l = (-0.3F * i - 0.19F) * g;
			this.leftGills.yaw += l;
			this.rightGills.yaw -= l;
			this.leftHindLeg.pitch += ((float) (Math.PI * 3.0 / 4.0) - i * 0.11F) * g;
			this.leftHindLeg.yaw += 0.47123894F * g;
			this.leftHindLeg.roll += 1.7278761F * g;
			this.leftFrontLeg.pitch += ((float) (Math.PI / 4) - i * 0.2F) * g;
			this.leftFrontLeg.yaw += 2.042035F * g;
			this.tail.yaw += 0.5F * i * g;
		}
	}

	private void setMovingInWaterAngles(float f, float headPitch, float g) {
		if (!(g <= 1.0E-5F)) {
			float h = f * 0.33F;
			float i = MathHelper.sin(h);
			float j = MathHelper.cos(h);
			float k = 0.13F * i;
			this.body.pitch += (headPitch * (float) (Math.PI / 180.0) + k) * g;
			this.head.pitch -= k * 1.8F * g;
			this.body.originY -= 0.45F * j * g;
			this.topGills.pitch += (-0.5F * i - 0.8F) * g;
			float l = (0.3F * i + 0.9F) * g;
			this.leftGills.yaw += l;
			this.rightGills.yaw -= l;
			this.tail.yaw = this.tail.yaw + 0.3F * MathHelper.cos(h * 0.9F) * g;
			this.leftHindLeg.pitch += MOVING_IN_WATER_LEG_PITCH * g;
			this.leftHindLeg.yaw += -0.4F * i * g;
			this.leftHindLeg.roll += (float) (Math.PI / 2) * g;
			this.leftFrontLeg.pitch += MOVING_IN_WATER_LEG_PITCH * g;
			this.leftFrontLeg.yaw += (-0.2F * j - 0.1F) * g;
			this.leftFrontLeg.roll += (float) (Math.PI / 2) * g;
		}
	}

	private void setPlayingDeadAngles(float headYaw) {
		if (!(headYaw <= 1.0E-5F)) {
			this.leftHindLeg.pitch += 1.4137167F * headYaw;
			this.leftHindLeg.yaw += 1.0995574F * headYaw;
			this.leftHindLeg.roll += (float) (Math.PI / 4) * headYaw;
			this.leftFrontLeg.pitch += (float) (Math.PI / 4) * headYaw;
			this.leftFrontLeg.yaw += 2.042035F * headYaw;
			this.body.pitch += -0.15F * headYaw;
			this.body.roll += 0.35F * headYaw;
		}
	}

	private void copyLegAngles(float f) {
		if (!(f <= 1.0E-5F)) {
			this.rightHindLeg.pitch = this.rightHindLeg.pitch + this.leftHindLeg.pitch * f;
			ModelPart var2 = this.rightHindLeg;
			var2.yaw = var2.yaw + -this.leftHindLeg.yaw * f;
			var2 = this.rightHindLeg;
			var2.roll = var2.roll + -this.leftHindLeg.roll * f;
			this.rightFrontLeg.pitch = this.rightFrontLeg.pitch + this.leftFrontLeg.pitch * f;
			var2 = this.rightFrontLeg;
			var2.yaw = var2.yaw + -this.leftFrontLeg.yaw * f;
			var2 = this.rightFrontLeg;
			var2.roll = var2.roll + -this.leftFrontLeg.roll * f;
		}
	}
}
