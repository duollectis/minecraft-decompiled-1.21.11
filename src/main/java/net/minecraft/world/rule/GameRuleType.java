package net.minecraft.world.rule;

import net.minecraft.util.StringIdentifiable;

/**
 * Тип данных правила игры — определяет, является ли значение целым числом или булевым.
 */
public enum GameRuleType implements StringIdentifiable {
	INT("integer"),
	BOOL("boolean");

	private final String name;

	GameRuleType(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}
}
