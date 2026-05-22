package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Форма соединения стены (Wall) с соседними блоками по горизонтали.
 * Определяет высоту и наличие столбика стены с каждой из четырёх сторон.
 */
public enum WallShape implements StringIdentifiable {

	/** Стена не соединяется с этой стороны — нет столбика. */
	NONE("none"),
	/** Низкое соединение — столбик не достигает верхней грани блока. */
	LOW("low"),
	/** Высокое соединение — столбик поднимается до верхней грани блока. */
	TALL("tall");

	private final String name;

	WallShape(final String name) {
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
}
