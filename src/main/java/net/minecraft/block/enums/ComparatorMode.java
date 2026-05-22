package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Режим работы блока компаратора.
 * Определяет, сравнивает ли компаратор сигналы или вычитает один из другого.
 */
public enum ComparatorMode implements StringIdentifiable {
	/** Режим сравнения: выводит сигнал, если задний вход не слабее бокового. */
	COMPARE("compare"),
	/** Режим вычитания: вычитает боковой сигнал из заднего. */
	SUBTRACT("subtract");

	private final String name;

	ComparatorMode(final String name) {
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
