package net.minecraft.world.tick;

import com.mojang.serialization.Codec;

/**
 * Приоритет выполнения тика. Чем меньше числовой индекс, тем выше приоритет.
 * При одинаковом времени срабатывания тики с более высоким приоритетом выполняются первыми.
 */
public enum TickPriority {
	EXTREMELY_HIGH(-3),
	VERY_HIGH(-2),
	HIGH(-1),
	NORMAL(0),
	LOW(1),
	VERY_LOW(2),
	EXTREMELY_LOW(3);

	public static final Codec<TickPriority> CODEC = Codec.INT.xmap(TickPriority::byIndex, TickPriority::getIndex);

	private final int index;

	TickPriority(int index) {
		this.index = index;
	}

	/**
	 * Возвращает приоритет по числовому индексу.
	 * Если индекс выходит за допустимые границы, возвращается ближайший крайний приоритет.
	 *
	 * @param index числовой индекс приоритета
	 */
	public static TickPriority byIndex(int index) {
		for (TickPriority priority : values()) {
			if (priority.index == index) {
				return priority;
			}
		}

		return index < EXTREMELY_HIGH.index ? EXTREMELY_HIGH : EXTREMELY_LOW;
	}

	public int getIndex() {
		return index;
	}
}
