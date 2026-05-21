package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * {@code WallShape}.
 */
public enum WallShape implements StringIdentifiable {
	NONE("none"),
	LOW("low"),
	TALL("tall");

	private final String name;

	private WallShape(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.asString();
	}

	@Override
	public String asString() {
		return this.name;
	}
}
