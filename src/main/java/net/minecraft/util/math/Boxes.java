package net.minecraft.util.math;

/**
 * Утилитарный класс для операций над {@link Box} (AABB в вещественных координатах).
 */
public class Boxes {

	/**
	 * Создаёт тонкий AABB, вытянутый от грани {@code box} в направлении {@code direction}
	 * на длину {@code length}. Используется для проверки столкновений при движении сущностей.
	 */
	public static Box stretch(Box box, Direction direction, double length) {
		double offset = length * direction.getDirection().offset();
		double negativeOffset = Math.min(offset, 0.0);
		double positiveOffset = Math.max(offset, 0.0);

		return switch (direction) {
			case WEST -> new Box(box.minX + negativeOffset, box.minY, box.minZ, box.minX + positiveOffset, box.maxY, box.maxZ);
			case EAST -> new Box(box.maxX + negativeOffset, box.minY, box.minZ, box.maxX + positiveOffset, box.maxY, box.maxZ);
			case DOWN -> new Box(box.minX, box.minY + negativeOffset, box.minZ, box.maxX, box.minY + positiveOffset, box.maxZ);
			case NORTH -> new Box(box.minX, box.minY, box.minZ + negativeOffset, box.maxX, box.maxY, box.minZ + positiveOffset);
			case SOUTH -> new Box(box.minX, box.minY, box.maxZ + negativeOffset, box.maxX, box.maxY, box.maxZ + positiveOffset);
			default -> new Box(box.minX, box.maxY + negativeOffset, box.minZ, box.maxX, box.maxY + positiveOffset, box.maxZ);
		};
	}
}
