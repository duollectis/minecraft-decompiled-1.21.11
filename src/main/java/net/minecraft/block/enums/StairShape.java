package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Форма ступени лестницы, определяющая её визуальный вид и коллизию.
 * Зависит от расположения соседних ступеней того же типа.
 */
public enum StairShape implements StringIdentifiable {
	/** Прямая ступень без угловых соединений. */
	STRAIGHT("straight"),
	/** Внутренний угол, открытый влево. */
	INNER_LEFT("inner_left"),
	/** Внутренний угол, открытый вправо. */
	INNER_RIGHT("inner_right"),
	/** Внешний угол, выступающий влево. */
	OUTER_LEFT("outer_left"),
	/** Внешний угол, выступающий вправо. */
	OUTER_RIGHT("outer_right");

	private final String name;

	StairShape(final String name) {
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
