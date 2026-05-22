package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Направление навигации по GUI-элементам с помощью клавиатуры.
 */
@Environment(EnvType.CLIENT)
public enum NavigationDirection {
	UP,
	DOWN,
	LEFT,
	RIGHT;

	private final IntComparator comparator = (a, b) -> a == b ? 0 : (isBefore(a, b) ? -1 : 1);

	public NavigationAxis getAxis() {
		return switch (this) {
			case UP, DOWN -> NavigationAxis.VERTICAL;
			case LEFT, RIGHT -> NavigationAxis.HORIZONTAL;
		};
	}

	public NavigationDirection getOpposite() {
		return switch (this) {
			case UP -> DOWN;
			case DOWN -> UP;
			case LEFT -> RIGHT;
			case RIGHT -> LEFT;
		};
	}

	public boolean isPositive() {
		return switch (this) {
			case UP, LEFT -> false;
			case DOWN, RIGHT -> true;
		};
	}

	public boolean isAfter(int a, int b) {
		return isPositive() ? a > b : b > a;
	}

	public boolean isBefore(int a, int b) {
		return isPositive() ? a < b : b < a;
	}

	public IntComparator getComparator() {
		return comparator;
	}
}
