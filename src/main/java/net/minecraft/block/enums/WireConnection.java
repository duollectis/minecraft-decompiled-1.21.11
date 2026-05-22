package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Тип соединения провода (Redstone Wire) с соседним блоком по горизонтали.
 * Определяет, как красный камень визуально и функционально соединяется с каждой из четырёх сторон.
 */
public enum WireConnection implements StringIdentifiable {

	/** Провод поднимается вверх по соседнему блоку (вертикальное соединение). */
	UP("up"),
	/** Провод соединяется горизонтально с соседним блоком. */
	SIDE("side"),
	/** Провод не соединяется с этой стороны. */
	NONE("none");

	private final String name;

	WireConnection(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return asString();
	}

	@Override
	public String asString() {
		return name;
	}

	public boolean isConnected() {
		return this != NONE;
	}
}
