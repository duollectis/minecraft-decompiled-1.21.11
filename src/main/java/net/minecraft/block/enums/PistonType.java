package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Тип поршня: обычный или липкий.
 * Определяет, будет ли поршень тянуть блоки обратно при втягивании.
 */
public enum PistonType implements StringIdentifiable {
	/** Обычный поршень — только толкает блоки, не тянет. */
	DEFAULT("normal"),
	/** Липкий поршень — толкает и тянет блоки обратно при втягивании. */
	STICKY("sticky");

	private final String name;

	PistonType(final String name) {
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
