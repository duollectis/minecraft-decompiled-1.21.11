package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.HoglinEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.Set;

@Environment(EnvType.CLIENT)
/**
 * {@code HoglinEntityModel}.
 */
public class HoglinEntityModel extends EntityModel<HoglinEntityRenderState> {

	public static final ModelTransformer
			BABY_TRANSFORMER =
			new BabyModelTransformer(true, 8.0F, 6.0F, 1.9F, 2.0F, 24.0F, Set.of("head"));
	private static final float HEAD_PITCH_START = 0.87266463F;
	private static final float HEAD_PITCH_END = (float) (-Math.PI / 9);
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart body;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart mane;

	public HoglinEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.body = modelPart.getChild("body");
		this.mane = this.body.getChild("mane");
		this.head = modelPart.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.rightFrontLeg = modelPart.getChild("right_front_leg");
		this.leftFrontLeg = modelPart.getChild("left_front_leg");
		this.rightHindLeg = modelPart.getChild("right_hind_leg");
		this.leftHindLeg = modelPart.getChild("left_hind_leg");
	}

	private static ModelData getModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(1, 1).cuboid(-8.0F, -7.0F, -13.0F, 16.0F, 14.0F, 26.0F),
				ModelTransform.origin(0.0F, 7.0F, 0.0F)
		);
		modelPartData2.addChild(
				"mane",
				ModelPartBuilder
						.create()
						.uv(90, 33)
						.cuboid(0.0F, 0.0F, -9.0F, 0.0F, 10.0F, 19.0F, new Dilation(0.001F)),
				ModelTransform.origin(0.0F, -14.0F, -7.0F)
		);
		ModelPartData modelPartData3 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(61, 1).cuboid(-7.0F, -3.0F, -19.0F, 14.0F, 6.0F, 19.0F),
				ModelTransform.of(0.0F, 2.0F, -12.0F, 0.87266463F, 0.0F, 0.0F)
		);
		modelPartData3.addChild(
				"right_ear",
				ModelPartBuilder.create().uv(1, 1).cuboid(-6.0F, -1.0F, -2.0F, 6.0F, 1.0F, 4.0F),
				ModelTransform.of(-6.0F, -2.0F, -3.0F, 0.0F, 0.0F, (float) (-Math.PI * 2.0 / 9.0))
		);
		modelPartData3.addChild(
				"left_ear",
				ModelPartBuilder.create().uv(1, 6).cuboid(0.0F, -1.0F, -2.0F, 6.0F, 1.0F, 4.0F),
				ModelTransform.of(6.0F, -2.0F, -3.0F, 0.0F, 0.0F, (float) (Math.PI * 2.0 / 9.0))
		);
		modelPartData3.addChild(
				"right_horn",
				ModelPartBuilder.create().uv(10, 13).cuboid(-1.0F, -11.0F, -1.0F, 2.0F, 11.0F, 2.0F),
				ModelTransform.origin(-7.0F, 2.0F, -12.0F)
		);
		modelPartData3.addChild(
				"left_horn",
				ModelPartBuilder.create().uv(1, 13).cuboid(-1.0F, -11.0F, -1.0F, 2.0F, 11.0F, 2.0F),
				ModelTransform.origin(7.0F, 2.0F, -12.0F)
		);
		int i = 14;
		int j = 11;
		modelPartData.addChild(
				"right_front_leg",
				ModelPartBuilder.create().uv(66, 42).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F),
				ModelTransform.origin(-4.0F, 10.0F, -8.5F)
		);
		modelPartData.addChild(
				"left_front_leg",
				ModelPartBuilder.create().uv(41, 42).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F),
				ModelTransform.origin(4.0F, 10.0F, -8.5F)
		);
		modelPartData.addChild(
				"right_hind_leg",
				ModelPartBuilder.create().uv(21, 45).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F),
				ModelTransform.origin(-5.0F, 13.0F, 10.0F)
		);
		modelPartData.addChild(
				"left_hind_leg",
				ModelPartBuilder.create().uv(0, 45).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 11.0F, 5.0F),
				ModelTransform.origin(5.0F, 13.0F, 10.0F)
		);
		return modelData;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = getModelData();
		return TexturedModelData.of(modelData, 128, 64);
	}

	public static TexturedModelData getBabyTexturedModelData() {
		ModelData modelData = getModelData();
		ModelPartData modelPartData = modelData.getRoot().getChild("body");
		modelPartData.addChild(
				"mane",
				ModelPartBuilder
						.create()
						.uv(90, 33)
						.cuboid(0.0F, 0.0F, -9.0F, 0.0F, 10.0F, 19.0F, new Dilation(0.001F)),
				ModelTransform.origin(0.0F, -14.0F, -3.0F)
		);
		return TexturedModelData.of(modelData, 128, 64).transform(BABY_TRANSFORMER);
	}

	public void setAngles(HoglinEntityRenderState hoglinEntityRenderState) {
		super.setAngles(hoglinEntityRenderState);
		float f = hoglinEntityRenderState.limbSwingAmplitude;
		float g = hoglinEntityRenderState.limbSwingAnimationProgress;
		this.rightEar.roll = (float) (-Math.PI * 2.0 / 9.0) - f * MathHelper.sin(g);
		this.leftEar.roll = (float) (Math.PI * 2.0 / 9.0) + f * MathHelper.sin(g);
		this.head.yaw = hoglinEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		float h = 1.0F - MathHelper.abs(10 - 2 * hoglinEntityRenderState.movementCooldownTicks) / 10.0F;
		this.head.pitch = MathHelper.lerp(h, 0.87266463F, (float) (-Math.PI / 9));
		if (hoglinEntityRenderState.baby) {
			this.head.originY += h * 2.5F;
		}

		float i = 1.2F;
		this.rightFrontLeg.pitch = MathHelper.cos(g) * 1.2F * f;
		this.leftFrontLeg.pitch = MathHelper.cos(g + (float) Math.PI) * 1.2F * f;
		this.rightHindLeg.pitch = this.leftFrontLeg.pitch;
		this.leftHindLeg.pitch = this.rightFrontLeg.pitch;
	}
}
