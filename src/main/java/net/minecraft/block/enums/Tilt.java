package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Угол наклона большого листа дриплифа (Big Dripleaf).
 * Определяет, насколько лист отклонился под весом существа, стоящего на нём.
 */
public enum Tilt implements StringIdentifiable {

	/** Лист горизонтален, существо может стоять на нём без проблем. */
	NONE("none", true),
	/** Лист начинает наклоняться — переходное состояние, ещё можно стоять. */
	UNSTABLE("unstable", false),
	/** Лист наклонён частично — существо начинает соскальзывать. */
	PARTIAL("partial", true),
	/** Лист полностью опущен вниз — существо падает. */
	FULL("full", true);

	private final String name;
	private final boolean stable;

	Tilt(final String name, final boolean stable) {
		this.name = name;
		this.stable = stable;
	}

	@Override
	public String asString() {
		return name;
	}

	public boolean isStable() {
		return stable;
	}
}
