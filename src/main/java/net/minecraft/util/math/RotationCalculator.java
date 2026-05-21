package net.minecraft.util.math;

/**
 * {@code RotationCalculator}.
 */
public class RotationCalculator {

	private final int max;
	private final int precision;
	private final float rotationPerDegrees;
	private final float degreesPerRotation;

	public RotationCalculator(int precision) {
		if (precision < 2) {
			throw new IllegalArgumentException("Precision cannot be less than 2 bits");
		}
		else if (precision > 30) {
			throw new IllegalArgumentException("Precision cannot be greater than 30 bits");
		}
		else {
			int i = 1 << precision;
			this.max = i - 1;
			this.precision = precision;
			this.rotationPerDegrees = i / 360.0F;
			this.degreesPerRotation = 360.0F / i;
		}
	}

	/**
	 * Are rotations parallel.
	 *
	 * @param alpha alpha
	 * @param beta beta
	 *
	 * @return boolean — результат операции
	 */
	public boolean areRotationsParallel(int alpha, int beta) {
		int i = this.getMax() >> 1;
		return (alpha & i) == (beta & i);
	}

	/**
	 * To rotation.
	 *
	 * @param direction direction
	 *
	 * @return int — результат операции
	 */
	public int toRotation(Direction direction) {
		if (direction.getAxis().isVertical()) {
			return 0;
		}
		else {
			int i = direction.getHorizontalQuarterTurns();
			return i << this.precision - 2;
		}
	}

	/**
	 * To rotation.
	 *
	 * @param degrees degrees
	 *
	 * @return int — результат операции
	 */
	public int toRotation(float degrees) {
		return Math.round(degrees * this.rotationPerDegrees);
	}

	/**
	 * To clamped rotation.
	 *
	 * @param degrees degrees
	 *
	 * @return int — результат операции
	 */
	public int toClampedRotation(float degrees) {
		return this.clamp(this.toRotation(degrees));
	}

	/**
	 * To degrees.
	 *
	 * @param rotation rotation
	 *
	 * @return float — результат операции
	 */
	public float toDegrees(int rotation) {
		return rotation * this.degreesPerRotation;
	}

	/**
	 * To wrapped degrees.
	 *
	 * @param rotation rotation
	 *
	 * @return float — результат операции
	 */
	public float toWrappedDegrees(int rotation) {
		float f = this.toDegrees(this.clamp(rotation));
		return f >= 180.0F ? f - 360.0F : f;
	}

	/**
	 * Clamp.
	 *
	 * @param rotationBits rotation bits
	 *
	 * @return int — результат операции
	 */
	public int clamp(int rotationBits) {
		return rotationBits & this.max;
	}

	public int getMax() {
		return this.max;
	}
}
