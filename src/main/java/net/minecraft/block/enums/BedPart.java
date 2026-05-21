package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * {@code BedPart}.
 */
public enum BedPart implements StringIdentifiable {
	HEAD("head"),
	FOOT("foot");

	private final String name;

	private BedPart(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public String asString() {
		return this.name;
	}
}
