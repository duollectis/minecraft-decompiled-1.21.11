package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Сторона навески петли двери.
 * Определяет, с какой стороны блока двери расположены петли,
 * что влияет на направление открытия.
 */
public enum DoorHinge implements StringIdentifiable {
	/** Петли расположены слева. */
	LEFT,
	/** Петли расположены справа. */
	RIGHT;

	@Override
	public String toString() {
		return asString();
	}

	@Override
	public String asString() {
		return this == LEFT ? "left" : "right";
	}
}
