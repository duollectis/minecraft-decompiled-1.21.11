package net.minecraft.util.math;

import java.util.Optional;

/**
 * {@code RotationPropertyHelper}.
 */
public class RotationPropertyHelper {

	private static final RotationCalculator CALCULATOR = new RotationCalculator(4);
	private static final int MAX = CALCULATOR.getMax();
	private static final int NORTH = 0;
	private static final int EAST = 4;
	private static final int SOUTH = 8;
	private static final int WEST = 12;

	public static int getMax() {
		return MAX;
	}

	/**
	 * From direction.
	 *
	 * @param direction direction
	 *
	 * @return int — результат операции
	 */
	public static int fromDirection(Direction direction) {
		return CALCULATOR.toRotation(direction);
	}

	/**
	 * From yaw.
	 *
	 * @param yaw yaw
	 *
	 * @return int — результат операции
	 */
	public static int fromYaw(float yaw) {
		return CALCULATOR.toClampedRotation(yaw);
	}

	/**
	 * To direction.
	 *
	 * @param rotation rotation
	 *
	 * @return Optional — результат операции
	 */
	public static Optional<Direction> toDirection(int rotation) {
		Direction direction = switch (rotation) {
			case 0 -> Direction.NORTH;
			case 4 -> Direction.EAST;
			case 8 -> Direction.SOUTH;
			case 12 -> Direction.WEST;
			default -> null;
		};
		return Optional.ofNullable(direction);
	}

	/**
	 * To degrees.
	 *
	 * @param rotation rotation
	 *
	 * @return float — результат операции
	 */
	public static float toDegrees(int rotation) {
		return CALCULATOR.toWrappedDegrees(rotation);
	}
}
