package net.minecraft.world.tick;

import com.mojang.serialization.Codec;

/**
 * {@code TickPriority}.
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

	private TickPriority(final int index) {
		this.index = index;
	}

	public static TickPriority byIndex(int index) {
		for (TickPriority tickPriority : values()) {
			if (tickPriority.index == index) {
				return tickPriority;
			}
		}

		return index < EXTREMELY_HIGH.index ? EXTREMELY_HIGH : EXTREMELY_LOW;
	}

	public int getIndex() {
		return this.index;
	}
}
