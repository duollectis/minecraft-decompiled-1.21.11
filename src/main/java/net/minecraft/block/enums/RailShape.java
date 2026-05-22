package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Форма (конфигурация) рельсового блока.
 * Определяет направление движения вагонетки и визуальный вид рельса.
 * Прямые формы ({@link #NORTH_SOUTH}, {@link #EAST_WEST}) и подъёмы
 * доступны всем типам рельсов; угловые формы — только обычным рельсам.
 */
public enum RailShape implements StringIdentifiable {
	/** Прямой рельс по оси север–юг. */
	NORTH_SOUTH("north_south"),
	/** Прямой рельс по оси восток–запад. */
	EAST_WEST("east_west"),
	/** Подъём на восток. */
	ASCENDING_EAST("ascending_east"),
	/** Подъём на запад. */
	ASCENDING_WEST("ascending_west"),
	/** Подъём на север. */
	ASCENDING_NORTH("ascending_north"),
	/** Подъём на юг. */
	ASCENDING_SOUTH("ascending_south"),
	/** Поворот юго-восток (только обычный рельс). */
	SOUTH_EAST("south_east"),
	/** Поворот юго-запад (только обычный рельс). */
	SOUTH_WEST("south_west"),
	/** Поворот северо-запад (только обычный рельс). */
	NORTH_WEST("north_west"),
	/** Поворот северо-восток (только обычный рельс). */
	NORTH_EAST("north_east");

	private final String name;

	RailShape(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isAscending() {
		return this == ASCENDING_NORTH
				|| this == ASCENDING_EAST
				|| this == ASCENDING_SOUTH
				|| this == ASCENDING_WEST;
	}

	@Override
	public String asString() {
		return name;
	}
}
