package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.LancerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.SwingAnimationType;
import net.minecraft.util.math.MathHelper;

/**
 * Утилитарный класс для позиционирования рук модели двуногой сущности.
 * <p>
 * Содержит статические методы для стандартных поз: удержание предмета,
 * натяжение арбалета, ближний бой, качание рук при ходьбе и атака зомби.
 */
@Environment(EnvType.CLIENT)
public class ArmPosing {

	/**
	 * Позиционирует руки для удержания предмета двумя руками (лук, арбалет в режиме прицеливания).
	 *
	 * @param holdingArm рука, держащая предмет
	 * @param otherArm   вторая рука
	 * @param head       голова (для синхронизации поворота)
	 * @param rightArm   {@code true}, если держащая рука — правая
	 */
	public static void hold(ModelPart holdingArm, ModelPart otherArm, ModelPart head, boolean rightArm) {
		ModelPart primary = rightArm ? holdingArm : otherArm;
		ModelPart secondary = rightArm ? otherArm : holdingArm;
		primary.yaw = (rightArm ? -0.3F : 0.3F) + head.yaw;
		secondary.yaw = (rightArm ? 0.6F : -0.6F) + head.yaw;
		primary.pitch = (float) (-Math.PI / 2) + head.pitch + 0.1F;
		secondary.pitch = -1.5F + head.pitch;
	}

	/**
	 * Позиционирует руки в процессе натяжения арбалета.
	 *
	 * @param holdingArm      рука, держащая арбалет
	 * @param pullingArm      рука, натягивающая тетиву
	 * @param crossbowPullTime полное время натяжения
	 * @param pullProgress    текущий прогресс натяжения
	 * @param rightArm        {@code true}, если держащая рука — правая
	 */
	public static void charge(
			ModelPart holdingArm,
			ModelPart pullingArm,
			float crossbowPullTime,
			float pullProgress,
			boolean rightArm
	) {
		ModelPart primary = rightArm ? holdingArm : pullingArm;
		ModelPart secondary = rightArm ? pullingArm : holdingArm;
		primary.yaw = rightArm ? -0.8F : 0.8F;
		primary.pitch = -0.97079635F;
		secondary.pitch = primary.pitch;
		float clampedProgress = MathHelper.clamp(pullProgress, 0.0F, crossbowPullTime);
		float normalizedProgress = clampedProgress / crossbowPullTime;
		secondary.yaw = MathHelper.lerp(normalizedProgress, 0.4F, 0.85F) * (rightArm ? 1 : -1);
		secondary.pitch = MathHelper.lerp(normalizedProgress, secondary.pitch, (float) (-Math.PI / 2));
	}

	/**
	 * Позиционирует руки для анимации удара в ближнем бою.
	 *
	 * @param rightArm          правая рука
	 * @param leftArm           левая рука
	 * @param mainArm           основная рука атакующего
	 * @param swingProgress     прогресс замаха [0..1]
	 * @param animationProgress общий прогресс анимации ходьбы
	 */
	public static void meleeAttack(
			ModelPart rightArm,
			ModelPart leftArm,
			Arm mainArm,
			float swingProgress,
			float animationProgress
	) {
		float swingSin = MathHelper.sin(swingProgress * (float) Math.PI);
		float swingEase = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
		rightArm.roll = 0.0F;
		leftArm.roll = 0.0F;
		rightArm.yaw = (float) (Math.PI / 20);
		leftArm.yaw = (float) (-Math.PI / 20);

		if (mainArm == Arm.RIGHT) {
			rightArm.pitch = -1.8849558F + MathHelper.cos(animationProgress * 0.09F) * 0.15F;
			leftArm.pitch = -0.0F + MathHelper.cos(animationProgress * 0.19F) * 0.5F;
			rightArm.pitch += swingSin * 2.2F - swingEase * 0.4F;
			leftArm.pitch += swingSin * 1.2F - swingEase * 0.4F;
		}
		else {
			rightArm.pitch = -0.0F + MathHelper.cos(animationProgress * 0.19F) * 0.5F;
			leftArm.pitch = -1.8849558F + MathHelper.cos(animationProgress * 0.09F) * 0.15F;
			rightArm.pitch += swingSin * 1.2F - swingEase * 0.4F;
			leftArm.pitch += swingSin * 2.2F - swingEase * 0.4F;
		}

		swingArms(rightArm, leftArm, animationProgress);
	}

	/** Добавляет покачивание одной руки в такт ходьбе. */
	public static void swingArm(ModelPart arm, float animationProgress, float sigma) {
		arm.roll = arm.roll + sigma * (MathHelper.cos(animationProgress * 0.09F) * 0.05F + 0.05F);
		arm.pitch = arm.pitch + sigma * (MathHelper.sin(animationProgress * 0.067F) * 0.05F);
	}

	/** Добавляет покачивание обеих рук в такт ходьбе (правая и левая в противофазе). */
	public static void swingArms(ModelPart rightArm, ModelPart leftArm, float animationProgress) {
		swingArm(rightArm, animationProgress, 1.0F);
		swingArm(leftArm, animationProgress, -1.0F);
	}

	/**
	 * Позиционирует руки в стиле зомби: вытянуты вперёд при атаке.
	 * Если тип замаха — {@link SwingAnimationType#STAB}, анимация пропускается.
	 *
	 * @param leftArm                  левая рука
	 * @param rightArm                 правая рука
	 * @param attacking                {@code true} при активной атаке (более агрессивный угол)
	 * @param lancerEntityRenderState  состояние рендера сущности-копейщика
	 */
	public static <T extends LancerEntityRenderState> void zombieArms(
			ModelPart leftArm,
			ModelPart rightArm,
			boolean attacking,
			T lancerEntityRenderState
	) {
		if (lancerEntityRenderState.swingAnimationType == SwingAnimationType.STAB) {
			swingArms(rightArm, leftArm, lancerEntityRenderState.age);
			return;
		}

		float swingProgress = lancerEntityRenderState.handSwingProgress;
		float basePitch = (float) -Math.PI / (attacking ? 1.5F : 2.25F);
		float swingSin = MathHelper.sin(swingProgress * (float) Math.PI);
		float swingEase = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
		rightArm.roll = 0.0F;
		rightArm.yaw = -(0.1F - swingSin * 0.6F);
		rightArm.pitch = basePitch;
		rightArm.pitch += swingSin * 1.2F - swingEase * 0.4F;
		leftArm.roll = 0.0F;
		leftArm.yaw = 0.1F - swingSin * 0.6F;
		leftArm.pitch = basePitch;
		leftArm.pitch += swingSin * 1.2F - swingEase * 0.4F;

		swingArms(rightArm, leftArm, lancerEntityRenderState.age);
	}
}
