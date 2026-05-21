package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.BeeEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code BeeEntityModel}.
 */
public class BeeEntityModel extends EntityModel<BeeEntityRenderState> {

	public static final ModelTransformer BABY_TRANSFORMER = ModelTransformer.scaling(0.5F);
	private static final String BONE = "bone";
	private static final String STINGER = "stinger";
	private static final String LEFT_ANTENNA = "left_antenna";
	private static final String RIGHT_ANTENNA = "right_antenna";
	private static final String FRONT_LEGS = "front_legs";
	private static final String MIDDLE_LEGS = "middle_legs";
	private static final String BACK_LEGS = "back_legs";
	private final ModelPart bone;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart frontLegs;
	private final ModelPart middleLegs;
	private final ModelPart backLegs;
	private final ModelPart stinger;
	private final ModelPart leftAntenna;
	private final ModelPart rightAntenna;
	private float bodyPitch;

	public BeeEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.bone = modelPart.getChild("bone");
		ModelPart modelPart2 = this.bone.getChild("body");
		this.stinger = modelPart2.getChild("stinger");
		this.leftAntenna = modelPart2.getChild("left_antenna");
		this.rightAntenna = modelPart2.getChild("right_antenna");
		this.rightWing = this.bone.getChild("right_wing");
		this.leftWing = this.bone.getChild("left_wing");
		this.frontLegs = this.bone.getChild("front_legs");
		this.middleLegs = this.bone.getChild("middle_legs");
		this.backLegs = this.bone.getChild("back_legs");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("bone", ModelPartBuilder.create(), ModelTransform.origin(0.0F, 19.0F, 0.0F));
		ModelPartData modelPartData3 = modelPartData2.addChild(
				"body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"stinger",
				ModelPartBuilder.create().uv(26, 7).cuboid(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F),
				ModelTransform.NONE
		);
		modelPartData3.addChild(
				"left_antenna",
				ModelPartBuilder.create().uv(2, 0).cuboid(1.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F),
				ModelTransform.origin(0.0F, -2.0F, -5.0F)
		);
		modelPartData3.addChild(
				"right_antenna",
				ModelPartBuilder.create().uv(2, 3).cuboid(-2.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F),
				ModelTransform.origin(0.0F, -2.0F, -5.0F)
		);
		Dilation dilation = new Dilation(0.001F);
		modelPartData2.addChild(
				"right_wing",
				ModelPartBuilder.create().uv(0, 18).cuboid(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, dilation),
				ModelTransform.of(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F)
		);
		modelPartData2.addChild(
				"left_wing",
				ModelPartBuilder.create().uv(0, 18).mirrored().cuboid(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, dilation),
				ModelTransform.of(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F)
		);
		modelPartData2.addChild(
				"front_legs",
				ModelPartBuilder.create().cuboid("front_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 1),
				ModelTransform.origin(1.5F, 3.0F, -2.0F)
		);
		modelPartData2.addChild(
				"middle_legs",
				ModelPartBuilder.create().cuboid("middle_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 3),
				ModelTransform.origin(1.5F, 3.0F, 0.0F)
		);
		modelPartData2.addChild(
				"back_legs",
				ModelPartBuilder.create().cuboid("back_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 5),
				ModelTransform.origin(1.5F, 3.0F, 2.0F)
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	public void setAngles(BeeEntityRenderState beeEntityRenderState) {
		super.setAngles(beeEntityRenderState);
		this.bodyPitch = beeEntityRenderState.bodyPitch;
		this.stinger.visible = beeEntityRenderState.hasStinger;
		if (!beeEntityRenderState.stoppedOnGround) {
			float f = beeEntityRenderState.age * 120.32113F * (float) (Math.PI / 180.0);
			this.rightWing.yaw = 0.0F;
			this.rightWing.roll = MathHelper.cos(f) * (float) Math.PI * 0.15F;
			this.leftWing.pitch = this.rightWing.pitch;
			this.leftWing.yaw = this.rightWing.yaw;
			this.leftWing.roll = -this.rightWing.roll;
			this.frontLegs.pitch = (float) (Math.PI / 4);
			this.middleLegs.pitch = (float) (Math.PI / 4);
			this.backLegs.pitch = (float) (Math.PI / 4);
		}

		if (!beeEntityRenderState.angry && !beeEntityRenderState.stoppedOnGround) {
			float f = MathHelper.cos(beeEntityRenderState.age * 0.18F);
			this.bone.pitch = 0.1F + f * (float) Math.PI * 0.025F;
			this.leftAntenna.pitch = f * (float) Math.PI * 0.03F;
			this.rightAntenna.pitch = f * (float) Math.PI * 0.03F;
			this.frontLegs.pitch = -f * (float) Math.PI * 0.1F + (float) (Math.PI / 8);
			this.backLegs.pitch = -f * (float) Math.PI * 0.05F + (float) (Math.PI / 4);
			this.bone.originY = this.bone.originY - MathHelper.cos(beeEntityRenderState.age * 0.18F) * 0.9F;
		}

		if (this.bodyPitch > 0.0F) {
			this.bone.pitch = MathHelper.lerpAngleRadians(this.bodyPitch, this.bone.pitch, 3.0915928F);
		}
	}
}
