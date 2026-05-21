package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Easing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
/**
 * {@code Lancing}.
 */
public class Lancing {

	static float clampedLerpProgress(float f, float g, float h) {
		return MathHelper.clamp(MathHelper.getLerpProgress(f, g, h), 0.0F, 1.0F);
	}

	public static <T extends BipedEntityRenderState> void positionArmForSpear(
			ModelPart arm,
			ModelPart head,
			boolean right,
			ItemStack itemStack,
			T state
	) {
		int i = right ? 1 : -1;
		arm.yaw = -0.1F * i + head.yaw;
		arm.pitch = (float) (-Math.PI / 2) + head.pitch + 0.8F;
		if (state.isGliding || state.leaningPitch > 0.0F) {
			arm.pitch -= 0.9599311F;
		}

		arm.yaw = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float) Math.PI) * arm.yaw, -60.0F, 60.0F);
		arm.pitch = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float) Math.PI) * arm.pitch, -120.0F, 30.0F);
		if (!(state.itemUseTime <= 0.0F) && (!state.isUsingItem || state.activeHand == (right ? Hand.MAIN_HAND
		                                                                                      : Hand.OFF_HAND
		)
		)) {
			KineticWeaponComponent kineticWeaponComponent = itemStack.get(DataComponentTypes.KINETIC_WEAPON);
			if (kineticWeaponComponent != null) {
				Lancing.AnimationState lv = Lancing.AnimationState.create(kineticWeaponComponent, state.itemUseTime);
				arm.yaw = arm.yaw + -i * lv.swayScaleFast() * (float) (Math.PI / 180.0) * lv.swayIntensity() * 1.0F;
				arm.roll = arm.roll + -i * lv.swayScaleSlow() * (float) (Math.PI / 180.0) * lv.swayIntensity() * 0.5F;
				arm.pitch = arm.pitch
						+ (float) (Math.PI / 180.0)
						* (
						-40.0F * lv.raiseProgressStart()
								+ 30.0F * lv.raiseProgressMiddle()
								+ -20.0F * lv.raiseProgressEnd()
								+ 20.0F * lv.lowerProgress()
								+ 10.0F * lv.raiseBackProgress()
								+ 0.6F * lv.swayScaleSlow() * lv.swayIntensity()
				);
			}
		}
	}

	public static <S extends ArmedEntityRenderState> void applyFirstPersonLancingTransform(
			S armedEntityRenderState,
			MatrixStack matrixStack,
			float f,
			Arm arm,
			ItemStack itemStack
	) {
		KineticWeaponComponent kineticWeaponComponent = itemStack.get(DataComponentTypes.KINETIC_WEAPON);
		if (kineticWeaponComponent != null && f != 0.0F) {
			float g = Easing.inQuad(clampedLerpProgress(armedEntityRenderState.handSwingProgress, 0.05F, 0.2F));
			float h = Easing.inOutExpo(clampedLerpProgress(armedEntityRenderState.handSwingProgress, 0.4F, 1.0F));
			Lancing.AnimationState lv = Lancing.AnimationState.create(kineticWeaponComponent, f);
			int i = arm == Arm.RIGHT ? 1 : -1;
			float j = 1.0F - Easing.outBack(1.0F - lv.raiseProgress());
			float k = 0.125F;
			float l = computeAttackBobOffset(armedEntityRenderState.timeSinceLastKineticAttack);
			matrixStack.translate(
					0.0,
					-l * 0.4,
					(double) (-kineticWeaponComponent.forwardMovement() * (j - lv.raiseBackProgress()) + l)
			);
			matrixStack.multiply(
					RotationAxis.NEGATIVE_X.rotationDegrees(
							70.0F * (lv.raiseProgress() - lv.raiseBackProgress()) - 40.0F * (g - h)),
					0.0F,
					-0.03125F,
					0.125F
			);
			matrixStack.multiply(
					RotationAxis.POSITIVE_Y.rotationDegrees(
							i * 90 * (lv.raiseProgress() - lv.swayProgress() + 3.0F * h + g)), 0.0F, 0.0F, 0.125F
			);
		}
	}

	public static <T extends BipedEntityRenderState> void applyArmSwingTransform(
			BipedEntityModel<T> bipedEntityModel,
			T bipedEntityRenderState
	) {
		float f = bipedEntityRenderState.handSwingProgress;
		Arm arm = bipedEntityRenderState.preferredArm;
		bipedEntityModel.rightArm.yaw = bipedEntityModel.rightArm.yaw - bipedEntityModel.body.yaw;
		bipedEntityModel.leftArm.yaw = bipedEntityModel.leftArm.yaw - bipedEntityModel.body.yaw;
		bipedEntityModel.leftArm.pitch = bipedEntityModel.leftArm.pitch - bipedEntityModel.body.yaw;
		float g = Easing.inOutSine(clampedLerpProgress(f, 0.0F, 0.05F));
		float h = Easing.inQuad(clampedLerpProgress(f, 0.05F, 0.2F));
		float i = Easing.inOutExpo(clampedLerpProgress(f, 0.4F, 1.0F));
		bipedEntityModel.getArm(arm).pitch += (90.0F * g - 120.0F * h + 30.0F * i) * (float) (Math.PI / 180.0);
	}

	public static <S extends ArmedEntityRenderState> void applySwingMatrixTransform(
			S armedEntityRenderState,
			MatrixStack matrixStack
	) {
		if (!(armedEntityRenderState.handSwingProgress <= 0.0F)) {
			KineticWeaponComponent
					kineticWeaponComponent =
					armedEntityRenderState.getMainHandItemStack().get(DataComponentTypes.KINETIC_WEAPON);
			float f = kineticWeaponComponent != null ? kineticWeaponComponent.forwardMovement() : 0.0F;
			float g = 0.125F;
			float h = armedEntityRenderState.handSwingProgress;
			float i = Easing.inQuad(clampedLerpProgress(h, 0.05F, 0.2F));
			float j = Easing.inOutExpo(clampedLerpProgress(h, 0.4F, 1.0F));
			matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(70.0F * (i - j)), 0.0F, -0.125F, 0.125F);
			matrixStack.translate(0.0F, f * (i - j), 0.0F);
		}
	}

	private static float computeAttackBobOffset(float f) {
		return 0.4F * (Easing.outQuart(clampedLerpProgress(f, 1.0F, 3.0F)) - Easing.inOutSine(clampedLerpProgress(f, 3.0F, 10.0F)));
	}

	public static void applyHeldItemLancingTransform(float f, MatrixStack matrixStack, float g, Arm arm, ItemStack itemStack) {
		KineticWeaponComponent kineticWeaponComponent = itemStack.get(DataComponentTypes.KINETIC_WEAPON);
		if (kineticWeaponComponent != null) {
			Lancing.AnimationState lv = Lancing.AnimationState.create(kineticWeaponComponent, g);
			int i = arm == Arm.RIGHT ? 1 : -1;
			matrixStack.translate(
					(double) (i * (lv.raiseProgress() * 0.15F + lv.raiseProgressEnd() * -0.05F
							+ lv.swayProgress() * -0.1F + lv.swayScaleSlow() * 0.005F
					)
					),
					(double) (lv.raiseProgress() * -0.075F + lv.raiseProgressMiddle() * 0.075F
							+ lv.swayScaleFast() * 0.01F
					),
					lv.raiseProgressStart() * 0.05 + lv.raiseProgressEnd() * -0.05 + lv.swayScaleSlow() * 0.005F
			);
			matrixStack.multiply(
					RotationAxis.POSITIVE_X
							.rotationDegrees(
									-65.0F * Easing.inOutBack(lv.raiseProgress()) - 35.0F * lv.lowerProgress()
											+ 100.0F * lv.raiseBackProgress() + -0.5F * lv.swayScaleFast()
							),
					0.0F,
					0.1F,
					0.0F
			);
			matrixStack.multiply(
					RotationAxis.NEGATIVE_Y
							.rotationDegrees(i * (-90.0F * clampedLerpProgress(lv.raiseProgress(), 0.5F, 0.55F)
									+ 90.0F * lv.swayProgress() + 2.0F * lv.swayScaleSlow()
							)),
					i * 0.15F,
					0.0F,
					0.0F
			);
			matrixStack.translate(0.0F, -computeAttackBobOffset(f), 0.0F);
		}
	}

	public static void applyProjectileTransform(float f, MatrixStack matrixStack, int i, Arm arm) {
		float g = Easing.inOutSine(clampedLerpProgress(f, 0.0F, 0.05F));
		float h = Easing.outBack(clampedLerpProgress(f, 0.05F, 0.2F));
		float j = Easing.inOutExpo(clampedLerpProgress(f, 0.4F, 1.0F));
		matrixStack.translate(i * 0.1F * (g - h), -0.075F * (g - j), 0.65F * (g - h));
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F * (g - j)));
		matrixStack.translate(0.0, 0.0, -0.25 * (j - h));
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AnimationState}.
	 */
	record AnimationState(
			float raiseProgress,
			float raiseProgressStart,
			float raiseProgressMiddle,
			float raiseProgressEnd,
			float swayProgress,
			float lowerProgress,
			float raiseBackProgress,
			float swayIntensity,
			float swayScaleSlow,
			float swayScaleFast
	) {

		public static Lancing.AnimationState create(KineticWeaponComponent kineticWeaponComponent, float f) {
			int i = kineticWeaponComponent.delayTicks();
			int
					j =
					kineticWeaponComponent
							.dismountConditions()
							.map(KineticWeaponComponent.Condition::maxDurationTicks)
							.orElse(0) + i;
			int k = j - 20;
			int
					l =
					kineticWeaponComponent
							.knockbackConditions()
							.map(KineticWeaponComponent.Condition::maxDurationTicks)
							.orElse(0) + i;
			int m = l - 40;
			int
					n =
					kineticWeaponComponent
							.damageConditions()
							.map(KineticWeaponComponent.Condition::maxDurationTicks)
							.orElse(0) + i;
			float g = Lancing.clampedLerpProgress(f, 0.0F, i);
			float h = Lancing.clampedLerpProgress(g, 0.0F, 0.5F);
			float o = Lancing.clampedLerpProgress(g, 0.5F, 0.8F);
			float p = Lancing.clampedLerpProgress(g, 0.8F, 1.0F);
			float q = Lancing.clampedLerpProgress(f, k, m);
			float r = Easing.outCubic(Easing.inOutElastic(Lancing.clampedLerpProgress(f - 20.0F, m, l)));
			float s = Lancing.clampedLerpProgress(f, n - 5, n);
			float t = 2.0F * Easing.outCirc(q) - 2.0F * Easing.inCirc(s);
			float u = MathHelper.sin(f * 19.0F * (float) (Math.PI / 180.0)) * t;
			float v = MathHelper.sin(f * 30.0F * (float) (Math.PI / 180.0)) * t;
			return new Lancing.AnimationState(g, h, o, p, q, r, s, t, u, v);
		}
	}
}
