package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Определяет поверхность, к которой прикреплён блок (кнопка, рычаг и т.п.).
 */
public enum BlockFace implements StringIdentifiable {
	/** Блок стоит на полу. */
	FLOOR("floor"),
	/** Блок прикреплён к стене. */
	WALL("wall"),
	/** Блок висит на потолке. */
	CEILING("ceiling");

	private final String name;

	BlockFace(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}
}
