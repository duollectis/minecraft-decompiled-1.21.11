package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Определяет способ крепления блока (колокол, кнопка и т.п.) к поверхности.
 */
public enum Attachment implements StringIdentifiable {
	/** Блок стоит на полу. */
	FLOOR("floor"),
	/** Блок висит на потолке. */
	CEILING("ceiling"),
	/** Блок прикреплён к одной стене. */
	SINGLE_WALL("single_wall"),
	/** Блок прикреплён к двум противоположным стенам. */
	DOUBLE_WALL("double_wall");

	private final String name;

	Attachment(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}
}
