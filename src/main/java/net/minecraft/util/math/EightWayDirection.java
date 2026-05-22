package net.minecraft.util.math;

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Set;

/**
 * Восемь горизонтальных направлений (стороны света + диагонали).
 * Каждое направление хранит набор составляющих {@link Direction} и суммарный смещающий вектор.
 */
public enum EightWayDirection {
	NORTH(Direction.NORTH),
	NORTH_EAST(Direction.NORTH, Direction.EAST),
	EAST(Direction.EAST),
	SOUTH_EAST(Direction.SOUTH, Direction.EAST),
	SOUTH(Direction.SOUTH),
	SOUTH_WEST(Direction.SOUTH, Direction.WEST),
	WEST(Direction.WEST),
	NORTH_WEST(Direction.NORTH, Direction.WEST);

	private final Set<Direction> directions;
	private final Vec3i offset;

	EightWayDirection(final Direction... directions) {
		this.directions = Sets.immutableEnumSet(Arrays.asList(directions));
		offset = new Vec3i(0, 0, 0);

		for (Direction direction : directions) {
			offset
					.setX(offset.getX() + direction.getOffsetX())
					.setY(offset.getY() + direction.getOffsetY())
					.setZ(offset.getZ() + direction.getOffsetZ());
		}
	}

	public Set<Direction> getDirections() {
		return directions;
	}

	public int getOffsetX() {
		return offset.getX();
	}

	public int getOffsetZ() {
		return offset.getZ();
	}
}
