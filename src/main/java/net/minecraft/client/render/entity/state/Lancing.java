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

/**
 * Утилитарный класс для анимации кинетического оружия (копья, трезубца).
 * <p>
 * Содержит статические методы для позиционирования рук и применения матричных
 * трансформаций при замахе, удержании и броске кинетического оружия.
 * Все анимации управляются через {@link AnimationState}, вычисляемый из
 * {@link KineticWeaponComponent} и текущего времени использования предмета.
 */
@Environment(EnvType.CLIENT)
public class Lancing {

	static float clampedLerpProgress(float value, float start, float end) {
		return MathHelper.clamp(MathHelper.getLerpProgress(value, start, end), 0.0F, 1.0F);
	}

	/**
	 * Позиционирует руку для удержания копья в режиме прицеливания.
	 * Учитывает планирование и наклон тела, а также активную фазу замаха.
	 *
	 * @param arm       рука, держащая копьё
	 * @param head      голова (для синхронизации поворота)
	 * @param right     {@code true}, если это правая рука
	 * @param itemStack стек предмета (для получения {@link KineticWeaponComponent})
	 * @param state     состояние рендера двуногой сущности
	 */
	public static <T extends BipedEntityRenderState> void positionArmForSpear(
			ModelPart arm,
			ModelPart head,
			boolean right,
			ItemStack itemStack,
			T state
	) {
		int side = right ? 1 : -1;
		arm.yaw = -0.1F * side + head.yaw;
		arm.pitch = (float) (-Math.PI / 2) + head.pitch + 0.8F;

		if (state.isGliding || state.leaningPitch > 0.0F) {
			arm.pitch -= 0.9599311F;
		}

		arm.yaw = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float) Math.PI) * arm.yaw, -60.0F, 60.0F);
		arm.pitch = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float) Math.PI) * arm.pitch, -120.0F, 30.0F);

		if (state.itemUseTime <= 0.0F) {
			return;
		}

		if (state.isUsingItem && state.activeHand != (right ? Hand.MAIN_HAND : Hand.OFF_HAND)) {
			return;
		}

		KineticWeaponComponent weapon = itemStack.get(DataComponentTypes.KINETIC_WEAPON);

		if (weapon == null) {
			return;
		}

		AnimationState anim = AnimationState.create(weapon, state.itemUseTime);
		arm.yaw = arm.yaw + -side * anim.swayScaleFast() * (float) (Math.PI / 180.0) * anim.swayIntensity() * 1.0F;
		arm.roll = arm.roll + -side * anim.swayScaleSlow() * (float) (Math.PI / 180.0) * anim.swayIntensity() * 0.5F;
		arm.pitch = arm.pitch
				+ (float) (Math.PI / 180.0)
				* (
				-40.0F * anim.raiseProgressStart()
						+ 30.0F * anim.raiseProgressMiddle()
						+ -20.0F * anim.raiseProgressEnd()
						+ 20.0F * anim.lowerProgress()
						+ 10.0F * anim.raiseBackProgress()
						+ 0.6F * anim.swayScaleSlow() * anim.swayIntensity()
		);
	}

	/**
	 * Применяет трансформацию матрицы для анимации кинетического оружия от первого лица.
	 *
	 * @param armedState состояние вооружённой сущности
	 * @param matrices   стек матриц
	 * @param useTime    время использования предмета
	 * @param arm        рука, держащая оружие
	 * @param itemStack  стек предмета
	 */
	public static <S extends ArmedEntityRenderState> void applyFirstPersonLancingTransform(
			S armedState,
			MatrixStack matrices,
			float useTime,
			Arm arm,
			ItemStack itemStack
	) {
		KineticWeaponComponent weapon = itemStack.get(DataComponentTypes.KINETIC_WEAPON);

		if (weapon == null || useTime == 0.0F) {
			return;
		}

		float swingEaseIn = Easing.inQuad(clampedLerpProgress(armedState.handSwingProgress, 0.05F, 0.2F));
		float swingEaseOut = Easing.inOutExpo(clampedLerpProgress(armedState.handSwingProgress, 0.4F, 1.0F));
		AnimationState anim = AnimationState.create(weapon, useTime);
		int side = arm == Arm.RIGHT ? 1 : -1;
		float raiseEased = 1.0F - Easing.outBack(1.0F - anim.raiseProgress());
		float bobOffset = computeAttackBobOffset(armedState.timeSinceLastKineticAttack);

		matrices.translate(
				0.0,
				-bobOffset * 0.4,
				(double) (-weapon.forwardMovement() * (raiseEased - anim.raiseBackProgress()) + bobOffset)
		);
		matrices.multiply(
				RotationAxis.NEGATIVE_X.rotationDegrees(
						70.0F * (anim.raiseProgress() - anim.raiseBackProgress()) - 40.0F * (swingEaseIn - swingEaseOut)
				),
				0.0F,
				-0.03125F,
				0.125F
		);
		matrices.multiply(
				RotationAxis.POSITIVE_Y.rotationDegrees(
						side * 90 * (anim.raiseProgress() - anim.swayProgress() + 3.0F * swingEaseOut + swingEaseIn)
				),
				0.0F,
				0.0F,
				0.125F
		);
	}

	/**
	 * Применяет трансформацию замаха к матрице для анимации от третьего лица.
	 *
	 * @param armedState состояние вооружённой сущности
	 * @param matrices   стек матриц
	 */
	public static <S extends ArmedEntityRenderState> void applySwingMatrixTransform(
			S armedState,
			MatrixStack matrices
	) {
		if (armedState.handSwingProgress <= 0.0F) {
			return;
		}

		KineticWeaponComponent weapon = armedState.getMainHandItemStack().get(DataComponentTypes.KINETIC_WEAPON);
		float forwardMovement = weapon != null ? weapon.forwardMovement() : 0.0F;
		float swingProgress = armedState.handSwingProgress;
		float swingEaseIn = Easing.inQuad(clampedLerpProgress(swingProgress, 0.05F, 0.2F));
		float swingEaseOut = Easing.inOutExpo(clampedLerpProgress(swingProgress, 0.4F, 1.0F));

		matrices.multiply(
				RotationAxis.NEGATIVE_X.rotationDegrees(70.0F * (swingEaseIn - swingEaseOut)),
				0.0F,
				-0.125F,
				0.125F
		);
		matrices.translate(0.0F, forwardMovement * (swingEaseIn - swingEaseOut), 0.0F);
	}

	/**
	 * Применяет трансформацию удержания кинетического оружия в руке.
	 *
	 * @param timeSinceAttack время с последней атаки (для боббинга)
	 * @param matrices        стек матриц
	 * @param useTime         время использования предмета
	 * @param arm             рука, держащая оружие
	 * @param itemStack       стек предмета
	 */
	public static void applyHeldItemLancingTransform(
			float timeSinceAttack,
			MatrixStack matrices,
			float useTime,
			Arm arm,
			ItemStack itemStack
	) {
		KineticWeaponComponent weapon = itemStack.get(DataComponentTypes.KINETIC_WEAPON);

		if (weapon == null) {
			return;
		}

		AnimationState anim = AnimationState.create(weapon, useTime);
		int side = arm == Arm.RIGHT ? 1 : -1;

		matrices.translate(
				(double) (side * (anim.raiseProgress() * 0.15F + anim.raiseProgressEnd() * -0.05F
						+ anim.swayProgress() * -0.1F + anim.swayScaleSlow() * 0.005F
				)
				),
				(double) (anim.raiseProgress() * -0.075F + anim.raiseProgressMiddle() * 0.075F
						+ anim.swayScaleFast() * 0.01F
				),
				anim.raiseProgressStart() * 0.05 + anim.raiseProgressEnd() * -0.05 + anim.swayScaleSlow() * 0.005F
		);
		matrices.multiply(
				RotationAxis.POSITIVE_X
						.rotationDegrees(
								-65.0F * Easing.inOutBack(anim.raiseProgress()) - 35.0F * anim.lowerProgress()
										+ 100.0F * anim.raiseBackProgress() + -0.5F * anim.swayScaleFast()
						),
				0.0F,
				0.1F,
				0.0F
		);
		matrices.multiply(
				RotationAxis.NEGATIVE_Y
						.rotationDegrees(side * (-90.0F * clampedLerpProgress(anim.raiseProgress(), 0.5F, 0.55F)
								+ 90.0F * anim.swayProgress() + 2.0F * anim.swayScaleSlow()
						)),
				side * 0.15F,
				0.0F,
				0.0F
		);
		matrices.translate(0.0F, -computeAttackBobOffset(timeSinceAttack), 0.0F);
	}

	/**
	 * Применяет трансформацию для отображения снаряда (трезубца) в полёте.
	 *
	 * @param useTime  время использования предмета
	 * @param matrices стек матриц
	 * @param side     сторона (1 = правая, -1 = левая)
	 * @param arm      рука броска
	 */
	public static void applyProjectileTransform(float useTime, MatrixStack matrices, int side, Arm arm) {
		float easeIn = Easing.inOutSine(clampedLerpProgress(useTime, 0.0F, 0.05F));
		float easeOut = Easing.outBack(clampedLerpProgress(useTime, 0.05F, 0.2F));
		float easeExpo = Easing.inOutExpo(clampedLerpProgress(useTime, 0.4F, 1.0F));
		matrices.translate(side * 0.1F * (easeIn - easeOut), -0.075F * (easeIn - easeExpo), 0.65F * (easeIn - easeOut));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F * (easeIn - easeExpo)));
		matrices.translate(0.0, 0.0, -0.25 * (easeExpo - easeOut));
	}

	private static float computeAttackBobOffset(float timeSinceAttack) {
		return 0.4F * (
				Easing.outQuart(clampedLerpProgress(timeSinceAttack, 1.0F, 3.0F))
						- Easing.inOutSine(clampedLerpProgress(timeSinceAttack, 3.0F, 10.0F))
		);
	}

	/**
	 * Вычисленное состояние анимации кинетического оружия для одного кадра.
	 * <p>
	 * Все поля — нормализованные прогрессы [0..1] различных фаз анимации:
	 * подъём, опускание, возврат, покачивание. Создаётся через {@link #create}.
	 */
	@Environment(EnvType.CLIENT)
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

		/**
		 * Вычисляет состояние анимации из параметров кинетического оружия и текущего времени использования.
		 *
		 * @param weapon  компонент кинетического оружия
		 * @param useTime текущее время использования предмета в тиках
		 * @return вычисленное состояние анимации
		 */
		public static Lancing.AnimationState create(KineticWeaponComponent weapon, float useTime) {
			int delayTicks = weapon.delayTicks();
			int dismountEnd = weapon.dismountConditions()
			                       .map(KineticWeaponComponent.Condition::maxDurationTicks)
			                       .orElse(0) + delayTicks;
			int dismountStart = dismountEnd - 20;
			int knockbackEnd = weapon.knockbackConditions()
			                        .map(KineticWeaponComponent.Condition::maxDurationTicks)
			                        .orElse(0) + delayTicks;
			int knockbackStart = knockbackEnd - 40;
			int damageEnd = weapon.damageConditions()
			                     .map(KineticWeaponComponent.Condition::maxDurationTicks)
			                     .orElse(0) + delayTicks;

			float raiseProgress = clampedLerpProgress(useTime, 0.0F, delayTicks);
			float raiseStart = clampedLerpProgress(raiseProgress, 0.0F, 0.5F);
			float raiseMiddle = clampedLerpProgress(raiseProgress, 0.5F, 0.8F);
			float raiseEnd = clampedLerpProgress(raiseProgress, 0.8F, 1.0F);
			float swayProgress = clampedLerpProgress(useTime, dismountStart, knockbackStart);
			float lowerProgress = Easing.outCubic(
					Easing.inOutElastic(clampedLerpProgress(useTime - 20.0F, knockbackStart, knockbackEnd))
			);
			float raiseBackProgress = clampedLerpProgress(useTime, damageEnd - 5, damageEnd);
			float swayIntensity = 2.0F * Easing.outCirc(swayProgress) - 2.0F * Easing.inCirc(raiseBackProgress);
			float swayScaleSlow = MathHelper.sin(useTime * 19.0F * (float) (Math.PI / 180.0)) * swayIntensity;
			float swayScaleFast = MathHelper.sin(useTime * 30.0F * (float) (Math.PI / 180.0)) * swayIntensity;

			return new Lancing.AnimationState(
					raiseProgress,
					raiseStart,
					raiseMiddle,
					raiseEnd,
					swayProgress,
					lowerProgress,
					raiseBackProgress,
					swayIntensity,
					swayScaleSlow,
					swayScaleFast
			);
		}
	}
}
