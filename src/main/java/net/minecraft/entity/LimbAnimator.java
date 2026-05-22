package net.minecraft.entity;

import net.minecraft.util.math.MathHelper;

/**
 * Управляет анимацией конечностей сущности (ходьба, бег).
 * Хранит текущую скорость, предыдущую скорость и прогресс анимации.
 */
public class LimbAnimator {

	private static final float LIMB_MOVING_THRESHOLD = 1.0E-5F;

	private float lastSpeed;
	private float speed;
	private float animationProgress;
	private float timeScale = 1.0F;

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	/**
	 * Обновляет скорость конечностей с плавным переходом и накапливает прогресс анимации.
	 *
	 * @param targetSpeed      целевая скорость движения
	 * @param speedChangeRate  скорость интерполяции к целевому значению (0–1)
	 * @param timeScale        масштаб времени для воспроизведения анимации
	 */
	public void updateLimbs(float targetSpeed, float speedChangeRate, float timeScale) {
		lastSpeed = speed;
		speed = speed + (targetSpeed - speed) * speedChangeRate;
		animationProgress = animationProgress + speed;
		this.timeScale = timeScale;
	}

	public void reset() {
		lastSpeed = 0.0F;
		speed = 0.0F;
		animationProgress = 0.0F;
	}

	public float getSpeed() {
		return speed;
	}

	public float getAmplitude(float tickProgress) {
		return Math.min(MathHelper.lerp(tickProgress, lastSpeed, speed), 1.0F);
	}

	public float getAnimationProgress() {
		return animationProgress * timeScale;
	}

	public float getAnimationProgress(float tickProgress) {
		return (animationProgress - speed * (1.0F - tickProgress)) * timeScale;
	}

	public boolean isLimbMoving() {
		return speed > LIMB_MOVING_THRESHOLD;
	}
}
