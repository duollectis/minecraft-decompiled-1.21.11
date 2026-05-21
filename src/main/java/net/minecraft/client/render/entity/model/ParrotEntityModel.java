package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.ParrotEntityRenderState;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code ParrotEntityModel}.
 */
public class ParrotEntityModel extends EntityModel<ParrotEntityRenderState> {

	private static final String FEATHER = "feather";
	private final ModelPart body;
	private final ModelPart tail;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart head;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public ParrotEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.body = modelPart.getChild("body");
		this.tail = modelPart.getChild("tail");
		this.leftWing = modelPart.getChild("left_wing");
		this.rightWing = modelPart.getChild("right_wing");
		this.head = modelPart.getChild("head");
		this.leftLeg = modelPart.getChild("left_leg");
		this.rightLeg = modelPart.getChild("right_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(2, 8).cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				ModelTransform.of(0.0F, 16.5F, -3.0F, 0.4937F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"tail",
				ModelPartBuilder.create().uv(22, 1).cuboid(-1.5F, -1.0F, -1.0F, 3.0F, 4.0F, 1.0F),
				ModelTransform.of(0.0F, 21.07F, 1.16F, 1.015F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_wing",
				ModelPartBuilder.create().uv(19, 8).cuboid(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
				ModelTransform.of(1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
		);
		modelPartData.addChild(
				"right_wing",
				ModelPartBuilder.create().uv(19, 8).cuboid(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
				ModelTransform.of(-1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
		);
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(2, 2).cuboid(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F),
				ModelTransform.origin(0.0F, 15.69F, -2.76F)
		);
		modelPartData2.addChild(
				"head2",
				ModelPartBuilder.create().uv(10, 0).cuboid(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F),
				ModelTransform.origin(0.0F, -2.0F, -1.0F)
		);
		modelPartData2.addChild(
				"beak1",
				ModelPartBuilder.create().uv(11, 7).cuboid(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				ModelTransform.origin(0.0F, -0.5F, -1.5F)
		);
		modelPartData2.addChild(
				"beak2",
				ModelPartBuilder.create().uv(16, 7).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				ModelTransform.origin(0.0F, -1.75F, -2.45F)
		);
		modelPartData2.addChild(
				"feather",
				ModelPartBuilder.create().uv(2, 18).cuboid(0.0F, -4.0F, -2.0F, 0.0F, 5.0F, 4.0F),
				ModelTransform.of(0.0F, -2.15F, 0.15F, -0.2214F, 0.0F, 0.0F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(14, 18).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		modelPartData.addChild(
				"left_leg",
				modelPartBuilder,
				ModelTransform.of(1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F)
		);
		modelPartData.addChild(
				"right_leg",
				modelPartBuilder,
				ModelTransform.of(-1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F)
		);
		return TexturedModelData.of(modelData, 32, 32);
	}

	public void setAngles(ParrotEntityRenderState parrotEntityRenderState) {
		super.setAngles(parrotEntityRenderState);
		this.animateModel(parrotEntityRenderState.parrotPose);
		this.head.pitch = parrotEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = parrotEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);
		switch (parrotEntityRenderState.parrotPose) {
			case STANDING:
				this.leftLeg.pitch = this.leftLeg.pitch
						+ MathHelper.cos(parrotEntityRenderState.limbSwingAnimationProgress * 0.6662F) * 1.4F
						* parrotEntityRenderState.limbSwingAmplitude;
				this.rightLeg.pitch = this.rightLeg.pitch
						+ MathHelper.cos(parrotEntityRenderState.limbSwingAnimationProgress * 0.6662F + (float) Math.PI)
						* 1.4F
						* parrotEntityRenderState.limbSwingAmplitude;
			case FLYING:
			case ON_SHOULDER:
			default:
				float h = parrotEntityRenderState.flapAngle * 0.3F;
				this.head.originY += h;
				this.tail.pitch = this.tail.pitch
						+ MathHelper.cos(parrotEntityRenderState.limbSwingAnimationProgress * 0.6662F) * 0.3F
						* parrotEntityRenderState.limbSwingAmplitude;
				this.tail.originY += h;
				this.body.originY += h;
				this.leftWing.roll = -0.0873F - parrotEntityRenderState.flapAngle;
				this.leftWing.originY += h;
				this.rightWing.roll = 0.0873F + parrotEntityRenderState.flapAngle;
				this.rightWing.originY += h;
				this.leftLeg.originY += h;
				this.rightLeg.originY += h;
			case SITTING:
				break;
			case PARTY:
				float f = MathHelper.cos(parrotEntityRenderState.age);
				float g = MathHelper.sin(parrotEntityRenderState.age);
				this.head.originX += f;
				this.head.originY += g;
				this.head.pitch = 0.0F;
				this.head.yaw = 0.0F;
				this.head.roll = MathHelper.sin(parrotEntityRenderState.age) * 0.4F;
				this.body.originX += f;
				this.body.originY += g;
				this.leftWing.roll = -0.0873F - parrotEntityRenderState.flapAngle;
				this.leftWing.originX += f;
				this.leftWing.originY += g;
				this.rightWing.roll = 0.0873F + parrotEntityRenderState.flapAngle;
				this.rightWing.originX += f;
				this.rightWing.originY += g;
				this.tail.originX += f;
				this.tail.originY += g;
		}
	}

	private void animateModel(ParrotEntityModel.Pose pose) {
		switch (pose) {
			case FLYING:
				this.leftLeg.pitch += (float) (Math.PI * 2.0 / 9.0);
				this.rightLeg.pitch += (float) (Math.PI * 2.0 / 9.0);
			case STANDING:
			case ON_SHOULDER:
			default:
				break;
			case SITTING:
				float f = 1.9F;
				this.head.originY++;
				this.tail.pitch += (float) (Math.PI / 6);
				this.tail.originY++;
				this.body.originY++;
				this.leftWing.roll = -0.0873F;
				this.leftWing.originY++;
				this.rightWing.roll = 0.0873F;
				this.rightWing.originY++;
				this.leftLeg.originY++;
				this.rightLeg.originY++;
				this.leftLeg.pitch++;
				this.rightLeg.pitch++;
				break;
			case PARTY:
				this.leftLeg.roll = (float) (-Math.PI / 9);
				this.rightLeg.roll = (float) (Math.PI / 9);
		}
	}

	public static ParrotEntityModel.Pose getPose(ParrotEntity parrot) {
		if (parrot.isSongPlaying()) {
			return ParrotEntityModel.Pose.PARTY;
		}
		else if (parrot.isInSittingPose()) {
			return ParrotEntityModel.Pose.SITTING;
		}
		else {
			return parrot.isInAir() ? ParrotEntityModel.Pose.FLYING : ParrotEntityModel.Pose.STANDING;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Pose}.
	 */
	public static enum Pose {
		FLYING,
		STANDING,
		SITTING,
		PARTY,
		ON_SHOULDER;
	}
}
