package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Определяет размер листьев на стебле бамбука.
 */
public enum BambooLeaves implements StringIdentifiable {
	/** Листья отсутствуют. */
	NONE("none"),
	/** Маленькие листья. */
	SMALL("small"),
	/** Большие листья. */
	LARGE("large");

	private final String name;

	BambooLeaves(String name) {
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
