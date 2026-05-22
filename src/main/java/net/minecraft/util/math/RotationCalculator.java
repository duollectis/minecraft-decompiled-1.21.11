package net.minecraft.util.math;

/**
 * Конвертирует углы поворота между градусами, целочисленными битовыми значениями и направлениями.
 * Точность задаётся количеством бит (от 2 до 30), что определяет разрешение шкалы вращения.
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

		if (precision > 30) {
			throw new IllegalArgumentException("Precision cannot be greater than 30 bits");
		}

		int steps = 1 << precision;
		this.max = steps - 1;
		this.precision = precision;
		this.rotationPerDegrees = steps / 360.0F;
		this.degreesPerRotation = 360.0F / steps;
	}

	public boolean areRotationsParallel(int alpha, int beta) {
		int halfMax = getMax() >> 1;
		return (alpha & halfMax) == (beta & halfMax);
	}

	public int toRotation(Direction direction) {
		if (direction.getAxis().isVertical()) {
			return 0;
		}

		int quarterTurns = direction.getHorizontalQuarterTurns();
		return quarterTurns << precision - 2;
	}

	public int toRotation(float degrees) {
		return Math.round(degrees * rotationPerDegrees);
	}

	public int toClampedRotation(float degrees) {
		return clamp(toRotation(degrees));
	}

	public float toDegrees(int rotation) {
		return rotation * degreesPerRotation;
	}

	public float toWrappedDegrees(int rotation) {
		float degrees = toDegrees(clamp(rotation));
		return degrees >= 180.0F ? degrees - 360.0F : degrees;
	}

	public int clamp(int rotationBits) {
		return rotationBits & max;
	}

	public int getMax() {
		return max;
	}
}
