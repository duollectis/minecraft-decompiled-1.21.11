package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.Lancing;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Easing;
import net.minecraft.util.math.MathHelper;

import java.util.Set;
import java.util.function.Function;

/**
 * Модель двуногой сущности (игрок, зомби, скелет и т.д.).
 * Управляет позиционированием рук, ног, головы и тела в зависимости от состояния рендера.
 */
@Environment(EnvType.CLIENT)
public class BipedEntityModel<T extends BipedEntityRenderState> extends EntityModel<T> implements ModelWithArms<T>, ModelWithHead {

	public static final ModelTransformer
			BABY_TRANSFORMER =
			new BabyModelTransformer(true, 16.0F, 0.0F, 2.0F, 2.0F, 24.0F, Set.of("head"));
	public static final float SNEAKING_LEG_PITCH_OFFSET = 0.25F;
	public static final float SNEAKING_ARM_PITCH_OFFSET = 0.5F;
	public static final float SWIM_LEAN_PITCH_OFFSET = -0.1F;
	private static final float LEG_IDLE_ROLL = 0.005F;
	private static final float SPYGLASS_ARM_YAW_OFFSET = (float) (Math.PI / 12);
	private static final float SPYGLASS_ARM_PITCH_OFFSET = 1.9198622F;
	private static final float SPYGLASS_SNEAKING_ARM_PITCH_OFFSET = (float) (Math.PI / 12);
	private static final float BLOCKING_ARM_MIN_PITCH = (float) (-Math.PI * 4.0 / 9.0);
	private static final float BLOCKING_ARM_MAX_PITCH = 0.43633232F;
	private static final float BLOCKING_ARM_YAW_LIMIT = (float) (Math.PI / 6);
	public static final float TOOT_HORN_ARM_PITCH_OFFSET = 1.4835298F;
	public static final float TOOT_HORN_ARM_YAW_OFFSET = (float) (Math.PI / 6);
	public final ModelPart head;
	public final ModelPart hat;
	public final ModelPart body;
	public final ModelPart rightArm;
	public final ModelPart leftArm;
	public final ModelPart rightLeg;
	public final ModelPart leftLeg;

	public BipedEntityModel(ModelPart modelPart) {
		this(modelPart, RenderLayers::entityCutoutNoCull);
	}

	public BipedEntityModel(ModelPart modelPart, Function<Identifier, RenderLayer> function) {
		super(modelPart, function);
		this.head = modelPart.getChild("head");
		this.hat = this.head.getChild("hat");
		this.body = modelPart.getChild("body");
		this.rightArm = modelPart.getChild("right_arm");
		this.leftArm = modelPart.getChild("left_arm");
		this.rightLeg = modelPart.getChild("right_leg");
		this.leftLeg = modelPart.getChild("left_leg");
	}

	public static ModelData getModelData(Dilation dilation, float pivotOffsetY) {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData modelPartData2 = modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation),
				ModelTransform.origin(0.0F, 0.0F + pivotOffsetY, 0.0F)
		);
		modelPartData2.addChild(
				"hat",
				ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation.add(0.5F)),
				ModelTransform.NONE
		);
		modelPartData.addChild(
				"body",
				ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation),
				ModelTransform.origin(0.0F, 0.0F + pivotOffsetY, 0.0F)
		);
		modelPartData.addChild(
				"right_arm",
				ModelPartBuilder.create().uv(40, 16).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation),
				ModelTransform.origin(-5.0F, 2.0F + pivotOffsetY, 0.0F)
		);
		modelPartData.addChild(
				"left_arm",
				ModelPartBuilder
						.create()
						.uv(40, 16)
						.mirrored()
						.cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation),
				ModelTransform.origin(5.0F, 2.0F + pivotOffsetY, 0.0F)
		);
		modelPartData.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation),
				ModelTransform.origin(-1.9F, 12.0F + pivotOffsetY, 0.0F)
		);
		modelPartData.addChild(
				"left_leg",
				ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation),
				ModelTransform.origin(1.9F, 12.0F + pivotOffsetY, 0.0F)
		);
		return modelData;
	}

	public static EquipmentModelData<ModelData> createEquipmentModelData(Dilation hatDilation, Dilation armorDilation) {
		return createEquipmentModelData(BipedEntityModel::createEquipmentModelData, hatDilation, armorDilation);
	}

	protected static EquipmentModelData<ModelData> createEquipmentModelData(
			Function<Dilation, ModelData> toModelData, Dilation hatDilation, Dilation armorDilation
	) {
		ModelData modelData = toModelData.apply(armorDilation);
		modelData.getRoot().resetChildrenExcept(Set.of("head"));
		ModelData modelData2 = toModelData.apply(armorDilation);
		modelData2.getRoot().resetChildrenExceptExact(Set.of("body", "left_arm", "right_arm"));
		ModelData modelData3 = toModelData.apply(hatDilation);
		modelData3.getRoot().resetChildrenExceptExact(Set.of("left_leg", "right_leg", "body"));
		ModelData modelData4 = toModelData.apply(armorDilation);
		modelData4.getRoot().resetChildrenExceptExact(Set.of("left_leg", "right_leg"));
		return new EquipmentModelData<>(modelData, modelData2, modelData3, modelData4);
	}

	private static ModelData createEquipmentModelData(Dilation dilation) {
		ModelData modelData = getModelData(dilation, 0.0F);
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"right_leg",
				ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(-0.1F)),
				ModelTransform.origin(-1.9F, 12.0F, 0.0F)
		);
		modelPartData.addChild(
				"left_leg",
				ModelPartBuilder
						.create()
						.uv(0, 16)
						.mirrored()
						.cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(-0.1F)),
				ModelTransform.origin(1.9F, 12.0F, 0.0F)
		);
		return modelData;
	}

	public void setAngles(T bipedEntityRenderState) {
		super.setAngles(bipedEntityRenderState);
		BipedEntityModel.ArmPose leftArmPose = bipedEntityRenderState.leftArmPose;
		BipedEntityModel.ArmPose rightArmPose = bipedEntityRenderState.rightArmPose;
		float leaningPitch = bipedEntityRenderState.leaningPitch;
		boolean isGliding = bipedEntityRenderState.isGliding;
		this.head.pitch = bipedEntityRenderState.pitch * (float) (Math.PI / 180.0);
		this.head.yaw = bipedEntityRenderState.relativeHeadYaw * (float) (Math.PI / 180.0);

		if (isGliding) {
			this.head.pitch = (float) (-Math.PI / 4);
		} else if (leaningPitch > 0.0F) {
			this.head.pitch = MathHelper.lerpAngleRadians(leaningPitch, this.head.pitch, (float) (-Math.PI / 4));
		}

		float limbSwing = bipedEntityRenderState.limbSwingAnimationProgress;
		float limbAmplitude = bipedEntityRenderState.limbSwingAmplitude;
		this.rightArm.pitch =
				MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbAmplitude * 0.5F
						/ bipedEntityRenderState.limbAmplitudeInverse;
		this.leftArm.pitch =
				MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbAmplitude * 0.5F / bipedEntityRenderState.limbAmplitudeInverse;
		this.rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbAmplitude / bipedEntityRenderState.limbAmplitudeInverse;
		this.leftLeg.pitch =
				MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbAmplitude / bipedEntityRenderState.limbAmplitudeInverse;
		this.rightLeg.yaw = LEG_IDLE_ROLL;
		this.leftLeg.yaw = -LEG_IDLE_ROLL;
		this.rightLeg.roll = LEG_IDLE_ROLL;
		this.leftLeg.roll = -LEG_IDLE_ROLL;
		if (bipedEntityRenderState.hasVehicle) {
			this.rightArm.pitch += (float) (-Math.PI / 5);
			this.leftArm.pitch += (float) (-Math.PI / 5);
			this.rightLeg.pitch = -1.4137167F;
			this.rightLeg.yaw = (float) (Math.PI / 10);
			this.rightLeg.roll = 0.07853982F;
			this.leftLeg.pitch = -1.4137167F;
			this.leftLeg.yaw = (float) (-Math.PI / 10);
			this.leftLeg.roll = -0.07853982F;
		}

		boolean isRightMainArm = bipedEntityRenderState.mainArm == Arm.RIGHT;

		if (bipedEntityRenderState.isUsingItem) {
			boolean isMainHandActive = bipedEntityRenderState.activeHand == Hand.MAIN_HAND;

			if (isMainHandActive == isRightMainArm) {
				this.positionRightArm(bipedEntityRenderState);

				if (!bipedEntityRenderState.rightArmPose.affectsOppositeArm()) {
					this.positionLeftArm(bipedEntityRenderState);
				}
			} else {
				this.positionLeftArm(bipedEntityRenderState);

				if (!bipedEntityRenderState.leftArmPose.affectsOppositeArm()) {
					this.positionRightArm(bipedEntityRenderState);
				}
			}
		} else {
			boolean isTwoHandedDominant = isRightMainArm ? leftArmPose.isTwoHanded() : rightArmPose.isTwoHanded();

			if (isRightMainArm != isTwoHandedDominant) {
				this.positionLeftArm(bipedEntityRenderState);

				if (!bipedEntityRenderState.leftArmPose.affectsOppositeArm()) {
					this.positionRightArm(bipedEntityRenderState);
				}
			} else {
				this.positionRightArm(bipedEntityRenderState);

				if (!bipedEntityRenderState.rightArmPose.affectsOppositeArm()) {
					this.positionLeftArm(bipedEntityRenderState);
				}
			}
		}

		this.animateArms(bipedEntityRenderState);
		if (bipedEntityRenderState.isInSneakingPose) {
			this.body.pitch = 0.5F;
			this.rightArm.pitch += 0.4F;
			this.leftArm.pitch += 0.4F;
			this.rightLeg.originZ += 4.0F;
			this.leftLeg.originZ += 4.0F;
			this.head.originY += 4.2F;
			this.body.originY += 3.2F;
			this.leftArm.originY += 3.2F;
			this.rightArm.originY += 3.2F;
		}

		if (rightArmPose != BipedEntityModel.ArmPose.SPYGLASS) {
			ArmPosing.swingArm(this.rightArm, bipedEntityRenderState.age, 1.0F);
		}

		if (leftArmPose != BipedEntityModel.ArmPose.SPYGLASS) {
			ArmPosing.swingArm(this.leftArm, bipedEntityRenderState.age, -1.0F);
		}

		if (leaningPitch > 0.0F) {
			float swimCycle = limbSwing % 26.0F;
			Arm preferredArm = bipedEntityRenderState.preferredArm;
			float rightArmLean = bipedEntityRenderState.rightArmPose != BipedEntityModel.ArmPose.SPEAR
					&& (preferredArm != Arm.RIGHT || !(bipedEntityRenderState.handSwingProgress > 0.0F))
					? leaningPitch
					: 0.0F;
			float leftArmLean = bipedEntityRenderState.leftArmPose != BipedEntityModel.ArmPose.SPEAR
					&& (preferredArm != Arm.LEFT || !(bipedEntityRenderState.handSwingProgress > 0.0F))
					? leaningPitch
					: 0.0F;

			if (!bipedEntityRenderState.isUsingItem) {
				if (swimCycle < 14.0F) {
					this.leftArm.pitch = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.pitch, 0.0F);
					this.rightArm.pitch = MathHelper.lerp(rightArmLean, this.rightArm.pitch, 0.0F);
					this.leftArm.yaw = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.yaw, (float) Math.PI);
					this.rightArm.yaw = MathHelper.lerp(rightArmLean, this.rightArm.yaw, (float) Math.PI);
					this.leftArm.roll = MathHelper.lerpAngleRadians(
							leftArmLean,
							this.leftArm.roll,
							(float) Math.PI + 1.8707964F * this.computeSwimArmRollCurve(swimCycle) / this.computeSwimArmRollCurve(14.0F)
					);
					this.rightArm.roll = MathHelper.lerp(
							rightArmLean,
							this.rightArm.roll,
							(float) Math.PI - 1.8707964F * this.computeSwimArmRollCurve(swimCycle) / this.computeSwimArmRollCurve(14.0F)
					);
				} else if (swimCycle >= 14.0F && swimCycle < 22.0F) {
					float swimPhase = (swimCycle - 14.0F) / 8.0F;
					this.leftArm.pitch = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.pitch, (float) (Math.PI / 2) * swimPhase);
					this.rightArm.pitch = MathHelper.lerp(rightArmLean, this.rightArm.pitch, (float) (Math.PI / 2) * swimPhase);
					this.leftArm.yaw = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.yaw, (float) Math.PI);
					this.rightArm.yaw = MathHelper.lerp(rightArmLean, this.rightArm.yaw, (float) Math.PI);
					this.leftArm.roll = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.roll, 5.012389F - 1.8707964F * swimPhase);
					this.rightArm.roll = MathHelper.lerp(rightArmLean, this.rightArm.roll, 1.2707963F + 1.8707964F * swimPhase);
				} else if (swimCycle >= 22.0F && swimCycle < 26.0F) {
					float swimPhase = (swimCycle - 22.0F) / 4.0F;
					this.leftArm.pitch = MathHelper.lerpAngleRadians(
							leftArmLean,
							this.leftArm.pitch,
							(float) (Math.PI / 2) - (float) (Math.PI / 2) * swimPhase
					);
					this.rightArm.pitch = MathHelper.lerp(
							rightArmLean,
							this.rightArm.pitch,
							(float) (Math.PI / 2) - (float) (Math.PI / 2) * swimPhase
					);
					this.leftArm.yaw = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.yaw, (float) Math.PI);
					this.rightArm.yaw = MathHelper.lerp(rightArmLean, this.rightArm.yaw, (float) Math.PI);
					this.leftArm.roll = MathHelper.lerpAngleRadians(leftArmLean, this.leftArm.roll, (float) Math.PI);
					this.rightArm.roll = MathHelper.lerp(rightArmLean, this.rightArm.roll, (float) Math.PI);
				}
			}

			this.leftLeg.pitch = MathHelper.lerp(leaningPitch, this.leftLeg.pitch, 0.3F * MathHelper.cos(limbSwing * 0.33333334F + (float) Math.PI));
			this.rightLeg.pitch = MathHelper.lerp(leaningPitch, this.rightLeg.pitch, 0.3F * MathHelper.cos(limbSwing * 0.33333334F));
		}
	}

	private void positionRightArm(T state) {
		switch (state.rightArmPose) {
			case EMPTY:
				this.rightArm.yaw = 0.0F;
				break;
			case ITEM:
				this.rightArm.pitch = this.rightArm.pitch * 0.5F - (float) (Math.PI / 10);
				this.rightArm.yaw = 0.0F;
				break;
			case BLOCK:
				this.positionBlockingArm(this.rightArm, true);
				break;
			case BOW_AND_ARROW:
				this.rightArm.yaw = SWIM_LEAN_PITCH_OFFSET + this.head.yaw;
				this.leftArm.yaw = 0.1F + this.head.yaw + 0.4F;
				this.rightArm.pitch = (float) (-Math.PI / 2) + this.head.pitch;
				this.leftArm.pitch = (float) (-Math.PI / 2) + this.head.pitch;
				break;
			case THROW_TRIDENT:
				this.rightArm.pitch = this.rightArm.pitch * 0.5F - (float) Math.PI;
				this.rightArm.yaw = 0.0F;
				break;
			case CROSSBOW_CHARGE:
				ArmPosing.charge(this.rightArm, this.leftArm, state.crossbowPullTime, state.itemUseTime, true);
				break;
			case CROSSBOW_HOLD:
				ArmPosing.hold(this.rightArm, this.leftArm, this.head, true);
				break;
			case SPYGLASS:
				this.rightArm.pitch =
						MathHelper.clamp(
								this.head.pitch - SPYGLASS_ARM_PITCH_OFFSET - (state.isInSneakingPose ? (float) (Math.PI / 12) : 0.0F),
								-2.4F,
								3.3F
						);
				this.rightArm.yaw = this.head.yaw - (float) (Math.PI / 12);
				break;
			case TOOT_HORN:
				this.rightArm.pitch = MathHelper.clamp(this.head.pitch, -1.2F, 1.2F) - TOOT_HORN_ARM_PITCH_OFFSET;
				this.rightArm.yaw = this.head.yaw - (float) (Math.PI / 6);
				break;
			case BRUSH:
				this.rightArm.pitch = this.rightArm.pitch * 0.5F - (float) (Math.PI / 5);
				this.rightArm.yaw = 0.0F;
				break;
			case SPEAR:
				Lancing.positionArmForSpear(this.rightArm, this.head, true, state.getItemStackForArm(Arm.RIGHT), state);
		}
	}

	private void positionLeftArm(T state) {
		switch (state.leftArmPose) {
			case EMPTY:
				this.leftArm.yaw = 0.0F;
				break;
			case ITEM:
				this.leftArm.pitch = this.leftArm.pitch * 0.5F - (float) (Math.PI / 10);
				this.leftArm.yaw = 0.0F;
				break;
			case BLOCK:
				this.positionBlockingArm(this.leftArm, false);
				break;
			case BOW_AND_ARROW:
				this.rightArm.yaw = SWIM_LEAN_PITCH_OFFSET + this.head.yaw - 0.4F;
				this.leftArm.yaw = 0.1F + this.head.yaw;
				this.rightArm.pitch = (float) (-Math.PI / 2) + this.head.pitch;
				this.leftArm.pitch = (float) (-Math.PI / 2) + this.head.pitch;
				break;
			case THROW_TRIDENT:
				this.leftArm.pitch = this.leftArm.pitch * 0.5F - (float) Math.PI;
				this.leftArm.yaw = 0.0F;
				break;
			case CROSSBOW_CHARGE:
				ArmPosing.charge(this.rightArm, this.leftArm, state.crossbowPullTime, state.itemUseTime, false);
				break;
			case CROSSBOW_HOLD:
				ArmPosing.hold(this.rightArm, this.leftArm, this.head, false);
				break;
			case SPYGLASS:
				this.leftArm.pitch =
						MathHelper.clamp(
								this.head.pitch - SPYGLASS_ARM_PITCH_OFFSET - (state.isInSneakingPose ? (float) (Math.PI / 12) : 0.0F),
								-2.4F,
								3.3F
						);
				this.leftArm.yaw = this.head.yaw + (float) (Math.PI / 12);
				break;
			case TOOT_HORN:
				this.leftArm.pitch = MathHelper.clamp(this.head.pitch, -1.2F, 1.2F) - TOOT_HORN_ARM_PITCH_OFFSET;
				this.leftArm.yaw = this.head.yaw + (float) (Math.PI / 6);
				break;
			case BRUSH:
				this.leftArm.pitch = this.leftArm.pitch * 0.5F - (float) (Math.PI / 5);
				this.leftArm.yaw = 0.0F;
				break;
			case SPEAR:
				Lancing.positionArmForSpear(this.leftArm, this.head, false, state.getItemStackForArm(Arm.LEFT), state);
		}
	}

	private void positionBlockingArm(ModelPart arm, boolean rightArm) {
		arm.pitch =
				arm.pitch * 0.5F - 0.9424779F + MathHelper.clamp(
						this.head.pitch,
						(float) (-Math.PI * 4.0 / 9.0),
						BLOCKING_ARM_MAX_PITCH
				);
		arm.yaw =
				(rightArm ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + MathHelper.clamp(
						this.head.yaw,
						(float) (-Math.PI / 6),
						(float) (Math.PI / 6)
				);
	}

	protected void animateArms(T bipedEntityRenderState) {
		float swingProgress = bipedEntityRenderState.handSwingProgress;

		if (swingProgress <= 0.0F) {
			return;
		}

		this.body.yaw = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2)) * 0.2F;
		if (bipedEntityRenderState.preferredArm == Arm.LEFT) {
			this.body.yaw *= -1.0F;
		}

		float ageScale = bipedEntityRenderState.ageScale;
		this.rightArm.originZ = MathHelper.sin(this.body.yaw) * 5.0F * ageScale;
		this.rightArm.originX = -MathHelper.cos(this.body.yaw) * 5.0F * ageScale;
		this.leftArm.originZ = -MathHelper.sin(this.body.yaw) * 5.0F * ageScale;
		this.leftArm.originX = MathHelper.cos(this.body.yaw) * 5.0F * ageScale;
		this.rightArm.yaw = this.rightArm.yaw + this.body.yaw;
		this.leftArm.yaw = this.leftArm.yaw + this.body.yaw;
		this.leftArm.pitch = this.leftArm.pitch + this.body.yaw;

		switch (bipedEntityRenderState.swingAnimationType) {
			case WHACK:
				float eased = Easing.outQuart(swingProgress);
				float swingSin = MathHelper.sin(eased * (float) Math.PI);
				float headPitchOffset = MathHelper.sin(swingProgress * (float) Math.PI) * -(this.head.pitch - 0.7F) * 0.75F;
				ModelPart swingArm = this.getArm(bipedEntityRenderState.preferredArm);
				swingArm.pitch -= swingSin * 1.2F + headPitchOffset;
				swingArm.yaw = swingArm.yaw + this.body.yaw * 2.0F;
				swingArm.roll = swingArm.roll + MathHelper.sin(swingProgress * (float) Math.PI) * -0.4F;
			case NONE:
			default:
				break;
			case STAB:
				// applyArmSwingTransform отсутствует в Lancing — анимация STAB пропускается
				break;
		}
	}

	private float computeSwimArmRollCurve(float f) {
		return -65.0F * f + f * f;
	}

	public void setVisible(boolean visible) {
		this.head.visible = visible;
		this.hat.visible = visible;
		this.body.visible = visible;
		this.rightArm.visible = visible;
		this.leftArm.visible = visible;
		this.rightLeg.visible = visible;
		this.leftLeg.visible = visible;
	}

	public void setArmAngle(BipedEntityRenderState bipedEntityRenderState, Arm arm, MatrixStack matrixStack) {
		this.root.applyTransform(matrixStack);
		this.getArm(arm).applyTransform(matrixStack);
	}

	public ModelPart getArm(Arm arm) {
		return arm == Arm.LEFT ? this.leftArm : this.rightArm;
	}

	@Override
	public ModelPart getHead() {
		return this.head;
	}

	/**
	 * Поза руки сущности, определяющая анимацию и трансформацию при рендере от первого лица.
	 */
	@Environment(EnvType.CLIENT)
	public static enum ArmPose {
		EMPTY(false, false),
		ITEM(false, false),
		BLOCK(false, false),
		BOW_AND_ARROW(true, true),
		THROW_TRIDENT(false, true),
		CROSSBOW_CHARGE(true, true),
		CROSSBOW_HOLD(true, true),
		SPYGLASS(false, false),
		TOOT_HORN(false, false),
		BRUSH(false, false),
		SPEAR(false, true) {
			@Override
			public <S extends ArmedEntityRenderState> void applyFirstPersonTransform(
					S armedEntityRenderState,
					MatrixStack matrixStack,
					float tickProgress,
					Arm arm,
					ItemStack itemStack
			) {
				Lancing.applyFirstPersonLancingTransform(armedEntityRenderState, matrixStack, tickProgress, arm, itemStack);
			}
		};

		private final boolean twoHanded;
		private final boolean affectsOppositeArm;

		ArmPose(final boolean twoHanded, final boolean affectsOppositeArm) {
			this.twoHanded = twoHanded;
			this.affectsOppositeArm = affectsOppositeArm;
		}

		public boolean isTwoHanded() {
			return this.twoHanded;
		}

		public boolean affectsOppositeArm() {
			return this.affectsOppositeArm;
		}

		public <S extends ArmedEntityRenderState> void applyFirstPersonTransform(
				S armedEntityRenderState,
				MatrixStack matrixStack,
				float tickProgress,
				Arm arm,
				ItemStack itemStack
		) {
		}
	}
}
