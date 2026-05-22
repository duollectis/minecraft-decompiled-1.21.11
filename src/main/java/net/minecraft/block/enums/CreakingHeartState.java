package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Состояние блока сердца скрипуна (Creaking Heart).
 * Определяет активность связанного моба-скрипуна и анимацию блока.
 */
public enum CreakingHeartState implements StringIdentifiable {
	/** Блок вырван из дерева и не функционирует. */
	UPROOTED("uprooted"),
	/** Блок находится в дереве, но скрипун спит (дневное время). */
	DORMANT("dormant"),
	/** Блок активен, скрипун бодрствует и патрулирует (ночное время). */
	AWAKE("awake");

	private final String id;

	CreakingHeartState(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public String asString() {
		return id;
	}
}
