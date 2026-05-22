package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Вспомогательный интерфейс для виджетов с квадратной областью взаимодействия.
 * Предоставляет методы проверки попадания точки ({@code x}, {@code y}) в различные
 * зоны квадрата со стороной {@code sideLength}.
 *
 * <p>Координаты ({@code x}, {@code y}) — относительные (от 0 до sideLength).
 * Ось Y направлена вниз: y=0 — верх, y=sideLength — низ.</p>
 */
@Environment(EnvType.CLIENT)
public interface SquareWidgetEntry {

	/** Проверяет, находится ли точка внутри квадрата. */
	default boolean isInside(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength && y >= 0 && y < sideLength;
	}

	/** Проверяет, находится ли точка в левой половине квадрата. */
	default boolean isLeft(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength / 2 && y >= 0 && y < sideLength;
	}

	/** Проверяет, находится ли точка в правой половине квадрата. */
	default boolean isRight(int x, int y, int sideLength) {
		return x >= sideLength / 2 && x < sideLength && y >= 0 && y < sideLength;
	}

	/** Проверяет, находится ли точка в правом нижнем квадранте (y < sideLength/2). */
	default boolean isBottomRight(int x, int y, int sideLength) {
		return x >= sideLength / 2 && x < sideLength && y >= 0 && y < sideLength / 2;
	}

	/** Проверяет, находится ли точка в правом верхнем квадранте (y >= sideLength/2). */
	default boolean isTopRight(int x, int y, int sideLength) {
		return x >= sideLength / 2 && x < sideLength && y >= sideLength / 2 && y < sideLength;
	}

	/** Проверяет, находится ли точка в левом нижнем квадранте (y < sideLength/2). */
	default boolean isBottomLeft(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength / 2 && y >= 0 && y < sideLength / 2;
	}

	/** Проверяет, находится ли точка в левом верхнем квадранте (y >= sideLength/2). */
	default boolean isTopLeft(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength / 2 && y >= sideLength / 2 && y < sideLength;
	}
}
