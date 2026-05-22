package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Толщина сталактита или сталагмита из заострённого камня (Pointed Dripstone).
 * Определяет визуальную форму и физические свойства секции.
 */
public enum Thickness implements StringIdentifiable {

	/** Кончик, слитый с соседним кончиком напротив (образует полный столб). */
	TIP_MERGE("tip_merge"),
	/** Острый кончик — самая тонкая часть. */
	TIP("tip"),
	/** Усечённая (конусообразная) секция под кончиком. */
	FRUSTUM("frustum"),
	/** Средняя секция столба. */
	MIDDLE("middle"),
	/** Основание — самая широкая часть, крепящаяся к блоку. */
	BASE("base");

	private final String name;

	Thickness(final String name) {
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
