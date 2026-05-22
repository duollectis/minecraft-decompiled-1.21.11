package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Тип плиты (slab): верхняя, нижняя или двойная.
 * Определяет вертикальное положение плиты в блоке и её коллизию.
 */
public enum SlabType implements StringIdentifiable {
	/** Плита занимает верхнюю половину блока. */
	TOP("top"),
	/** Плита занимает нижнюю половину блока. */
	BOTTOM("bottom"),
	/** Две плиты образуют полный блок. */
	DOUBLE("double");

	private final String name;

	SlabType(final String name) {
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
