package net.minecraft.recipe;

import net.minecraft.util.math.MathHelper;

import java.util.Iterator;

/**
 * Утилита для центрирования рецепта в сетке крафта.
 * Если рецепт меньше сетки — он выравнивается по центру по горизонтали и вертикали.
 */
public interface RecipeGridAligner {

	static <T> void alignRecipeToGrid(
		int width,
		int height,
		Recipe<?> recipe,
		Iterable<T> slots,
		RecipeGridAligner.Filler<T> filler
	) {
		if (recipe instanceof ShapedRecipe shapedRecipe) {
			alignRecipeToGrid(width, height, shapedRecipe.getWidth(), shapedRecipe.getHeight(), slots, filler);
		} else {
			alignRecipeToGrid(width, height, width, height, slots, filler);
		}
	}

	static <T> void alignRecipeToGrid(
		int gridWidth,
		int gridHeight,
		int recipeWidth,
		int recipeHeight,
		Iterable<T> slots,
		RecipeGridAligner.Filler<T> filler
	) {
		Iterator<T> iterator = slots.iterator();
		int slotIndex = 0;

		for (int row = 0; row < gridHeight; row++) {
			boolean recipeSmallerThanGrid = recipeHeight < gridHeight / 2.0F;
			int verticalOffset = MathHelper.floor(gridHeight / 2.0F - recipeHeight / 2.0F);

			if (recipeSmallerThanGrid && verticalOffset > row) {
				slotIndex += gridWidth;
				row++;
			}

			for (int col = 0; col < gridWidth; col++) {
				if (!iterator.hasNext()) {
					return;
				}

				boolean recipeSmallerHorizontally = recipeWidth < gridWidth / 2.0F;
				int horizontalOffset = MathHelper.floor(gridWidth / 2.0F - recipeWidth / 2.0F);
				int recipeEnd = recipeWidth;
				boolean inRecipeColumn = col < recipeWidth;

				if (recipeSmallerHorizontally) {
					recipeEnd = horizontalOffset + recipeWidth;
					inRecipeColumn = horizontalOffset <= col && col < horizontalOffset + recipeWidth;
				}

				if (inRecipeColumn) {
					filler.addItemToSlot(iterator.next(), slotIndex, col, row);
				} else if (recipeEnd == col) {
					slotIndex += gridWidth - col;
					break;
				}

				slotIndex++;
			}
		}
	}

	@FunctionalInterface
	interface Filler<T> {

		void addItemToSlot(T slot, int index, int x, int y);
	}
}
