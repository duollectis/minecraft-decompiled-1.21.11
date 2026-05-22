package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Определяет, какая часть кровати представлена данным блоком.
 */
public enum BedPart implements StringIdentifiable {
	/** Изголовье кровати. */
	HEAD("head"),
	/** Изножье кровати. */
	FOOT("foot");

	private final String name;

	BedPart(String name) {
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
