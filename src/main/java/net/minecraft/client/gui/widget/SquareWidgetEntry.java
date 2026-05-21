package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code SquareWidgetEntry}.
 */
public interface SquareWidgetEntry {

	default boolean isInside(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength && y >= 0 && y < sideLength;
	}

	default boolean isLeft(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength / 2 && y >= 0 && y < sideLength;
	}

	default boolean isRight(int x, int y, int sideLength) {
		return x >= sideLength / 2 && x < sideLength && y >= 0 && y < sideLength;
	}

	default boolean isBottomRight(int x, int y, int sideLength) {
		return x >= sideLength / 2 && x < sideLength && y >= 0 && y < sideLength / 2;
	}

	default boolean isTopRight(int x, int y, int sideLength) {
		return x >= sideLength / 2 && x < sideLength && y >= sideLength / 2 && y < sideLength;
	}

	default boolean isBottomLeft(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength / 2 && y >= 0 && y < sideLength / 2;
	}

	default boolean isTopLeft(int x, int y, int sideLength) {
		return x >= 0 && x < sideLength / 2 && y >= sideLength / 2 && y < sideLength;
	}
}
