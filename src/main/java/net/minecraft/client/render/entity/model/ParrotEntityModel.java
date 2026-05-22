package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.state.ParrotEntityRenderState;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Модель попугая с поддержкой поз: стоя, летя, сидя, на плече и в режиме вечеринки.
 * <p>
 * Анимация крыльев, хвоста и ног управляется через {@link Pose} и поля
 * {@link ParrotEntityRenderState}: прогресс качания конечностей, угол взмаха крыльев,
 * угол поворота головы и возраст сущности.
 */
@Environment(EnvType.CLIENT)
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
		body = modelPart.getChild("body");
		tail = modelPart.getChild("tail");
		leftWing = modelPart.getChild("left_wing");
		rightWing = modelPart.getChild("right_wing");
		head = modelPart.getChild("head");
		leftLeg = modelPart.getChild("left_leg");
		rightLeg = modelPart.getChild("right_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();
		root.addChild(
				"body",
				ModelPartBuilder.create().uv(2, 8).cuboid(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				ModelTransform.of(0.0F, 16.5F, -3.0F, 0.4937F, 0.0F, 0.0F)
		);
		root.addChild(
				"tail",
				ModelPartBuilder.create().uv(22, 1).cuboid(-1.5F, -1.0F, -1.0F, 3.0F, 4.0F, 1.0F),
				ModelTransform.of(0.0F, 21.07F, 1.16F, 1.015F, 0.0F, 0.0F)
		);
		root.addChild(
				"left_wing",
				ModelPartBuilder.create().uv(19, 8).cuboid(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
				ModelTransform.of(1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
		);
		root.addChild(
				"right_wing",
				ModelPartBuilder.create().uv(19, 8).cuboid(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F),
				ModelTransform.of(-1.5F, 16.94F, -2.76F, -0.6981F, (float) -Math.PI, 0.0F)
		);
		ModelPartData headPart = root.addChild(
				"head",
				ModelPartBuilder.create().uv(2, 2).cuboid(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F),
				ModelTransform.origin(0.0F, 15.69F, -2.76F)
		);
		headPart.addChild(
				"head2",
				ModelPartBuilder.create().uv(10, 0).cuboid(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F),
				ModelTransform.origin(0.0F, -2.0F, -1.0F)
		);
		headPart.addChild(
				"beak1",
				ModelPartBuilder.create().uv(11, 7).cuboid(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				ModelTransform.origin(0.0F, -0.5F, -1.5F)
		);
		headPart.addChild(
				"beak2",
				ModelPartBuilder.create().uv(16, 7).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				ModelTransform.origin(0.0F, -1.75F, -2.45F)
		);
		headPart.addChild(
				FEATHER,
				ModelPartBuilder.create().uv(2, 18).cuboid(0.0F, -4.0F, -2.0F, 0.0F, 5.0F, 4.0F),
				ModelTransform.of(0.0F, -2.15F, 0.15F, -0.2214F, 0.0F, 0.0F)
		);
		ModelPartBuilder legBuilder =
				ModelPartBuilder.create().uv(14, 18).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		root.addChild("left_leg", legBuilder, ModelTransform.of(1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F));
		root.addChild("right_leg", legBuilder, ModelTransform.of(-1.0F, 22.0F, -1.05F, -0.0299F, 0.0F, 0.0F));
		return TexturedModelData.of(modelData, 32, 32);
	}

	@Override
	public void setAngles(ParrotEntityRenderState state) {
		super.setAngles(state);
		animateModel(state.parrotPose);
		head.pitch = state.pitch * (float) (Math.PI / 180.0);
		head.yaw = state.relativeHeadYaw * (float) (Math.PI / 180.0);

		switch (state.parrotPose) {
			case STANDING:
				leftLeg.pitch = leftLeg.pitch
						+ MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F) * 1.4F
						* state.limbSwingAmplitude;
				rightLeg.pitch = rightLeg.pitch
						+ MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F + (float) Math.PI)
						* 1.4F
						* state.limbSwingAmplitude;
			case FLYING:
			case ON_SHOULDER:
			default:
				float flapOffset = state.flapAngle * 0.3F;
				head.originY += flapOffset;
				tail.pitch = tail.pitch
						+ MathHelper.cos(state.limbSwingAnimationProgress * 0.6662F) * 0.3F
						* state.limbSwingAmplitude;
				tail.originY += flapOffset;
				body.originY += flapOffset;
				leftWing.roll = -0.0873F - state.flapAngle;
				leftWing.originY += flapOffset;
				rightWing.roll = 0.0873F + state.flapAngle;
				rightWing.originY += flapOffset;
				leftLeg.originY += flapOffset;
				rightLeg.originY += flapOffset;
				break;
			case SITTING:
				break;
			case PARTY:
				float cosAge = MathHelper.cos(state.age);
				float sinAge = MathHelper.sin(state.age);
				head.originX += cosAge;
				head.originY += sinAge;
				head.pitch = 0.0F;
				head.yaw = 0.0F;
				head.roll = MathHelper.sin(state.age) * 0.4F;
				body.originX += cosAge;
				body.originY += sinAge;
				leftWing.roll = -0.0873F - state.flapAngle;
				leftWing.originX += cosAge;
				leftWing.originY += sinAge;
				rightWing.roll = 0.0873F + state.flapAngle;
				rightWing.originX += cosAge;
				rightWing.originY += sinAge;
				tail.originX += cosAge;
				tail.originY += sinAge;
				break;
		}
	}

	private void animateModel(ParrotEntityModel.Pose pose) {
		switch (pose) {
			case FLYING:
				leftLeg.pitch += (float) (Math.PI * 2.0 / 9.0);
				rightLeg.pitch += (float) (Math.PI * 2.0 / 9.0);
				break;
			case SITTING:
				head.originY++;
				tail.pitch += (float) (Math.PI / 6);
				tail.originY++;
				body.originY++;
				leftWing.roll = -0.0873F;
				leftWing.originY++;
				rightWing.roll = 0.0873F;
				rightWing.originY++;
				leftLeg.originY++;
				rightLeg.originY++;
				leftLeg.pitch++;
				rightLeg.pitch++;
				break;
			case PARTY:
				leftLeg.roll = (float) (-Math.PI / 9);
				rightLeg.roll = (float) (Math.PI / 9);
				break;
			default:
				break;
		}
	}

	public static ParrotEntityModel.Pose getPose(ParrotEntity parrot) {
		if (parrot.isSongPlaying()) {
			return ParrotEntityModel.Pose.PARTY;
		}

		if (parrot.isInSittingPose()) {
			return ParrotEntityModel.Pose.SITTING;
		}

		return parrot.isInAir() ? ParrotEntityModel.Pose.FLYING : ParrotEntityModel.Pose.STANDING;
	}

	/** Поза попугая, определяющая набор применяемых анимаций. */
	@Environment(EnvType.CLIENT)
	public enum Pose {
		FLYING,
		STANDING,
		SITTING,
		PARTY,
		ON_SHOULDER;
	}
}
