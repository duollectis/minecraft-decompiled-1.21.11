package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Определяет, какая половина блока (верхняя или нижняя) представлена данным состоянием.
 * Используется для ступеней, плит и аналогичных блоков.
 */
public enum BlockHalf implements StringIdentifiable {
	/** Верхняя половина блока. */
	TOP("top"),
	/** Нижняя половина блока. */
	BOTTOM("bottom");

	private final String name;

	BlockHalf(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String asString() {
		return name;
	}
}
