package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

/**
 * Половина двухблочной структуры (двери, большого папоротника и т.п.).
 * Каждая половина хранит направление к своей паре для быстрого поиска соседнего блока.
 */
public enum DoubleBlockHalf implements StringIdentifiable {
	/** Верхняя половина; направление к паре — вниз. */
	UPPER(Direction.DOWN),
	/** Нижняя половина; направление к паре — вверх. */
	LOWER(Direction.UP);

	private final Direction oppositeDirection;

	DoubleBlockHalf(final Direction oppositeDirection) {
		this.oppositeDirection = oppositeDirection;
	}

	public Direction getOppositeDirection() {
		return oppositeDirection;
	}

	@Override
	public String toString() {
		return asString();
	}

	@Override
	public String asString() {
		return this == UPPER ? "upper" : "lower";
	}

	public DoubleBlockHalf getOtherHalf() {
		return this == UPPER ? LOWER : UPPER;
	}
}
