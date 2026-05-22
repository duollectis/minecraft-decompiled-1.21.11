package net.minecraft.util.math;

import java.util.Optional;

/**
 * Вспомогательный класс для работы со свойством поворота блока (16 дискретных значений, 4 бита).
 * Использует {@link RotationCalculator} с точностью 4 бита, что даёт шаг 22.5° на единицу.
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

	public static int fromDirection(Direction direction) {
		return CALCULATOR.toRotation(direction);
	}

	public static int fromYaw(float yaw) {
		return CALCULATOR.toClampedRotation(yaw);
	}

	public static Optional<Direction> toDirection(int rotation) {
		Direction direction = switch (rotation) {
			case NORTH -> Direction.NORTH;
			case EAST -> Direction.EAST;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
			default -> null;
		};

		return Optional.ofNullable(direction);
	}

	public static float toDegrees(int rotation) {
		return CALCULATOR.toWrappedDegrees(rotation);
	}
}
