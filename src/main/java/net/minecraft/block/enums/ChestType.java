package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Тип сундука, определяющий его конфигурацию при объединении двух сундуков в двойной.
 * Используется свойством блока для отображения правильной половины текстуры.
 */
public enum ChestType implements StringIdentifiable {
	/** Одиночный сундук, не объединённый с соседним. */
	SINGLE("single"),
	/** Левая половина двойного сундука. */
	LEFT("left"),
	/** Правая половина двойного сундука. */
	RIGHT("right");

	private final String name;

	ChestType(final String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}

	public ChestType getOpposite() {
		return switch (this) {
			case SINGLE -> SINGLE;
			case LEFT -> RIGHT;
			case RIGHT -> LEFT;
		};
	}
}
