package net.minecraft.recipe;

import net.minecraft.util.math.MathHelper;

import java.util.Iterator;

/**
 * {@code RecipeGridAligner}.
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
		}
		else {
			alignRecipeToGrid(width, height, width, height, slots, filler);
		}
	}

	static <T> void alignRecipeToGrid(
			int width,
			int height,
			int recipeWidth,
			int recipeHeight,
			Iterable<T> slots,
			RecipeGridAligner.Filler<T> filler
	) {
		Iterator<T> iterator = slots.iterator();
		int i = 0;

		for (int j = 0; j < height; j++) {
			boolean bl = recipeHeight < height / 2.0F;
			int k = MathHelper.floor(height / 2.0F - recipeHeight / 2.0F);
			if (bl && k > j) {
				i += width;
				j++;
			}

			for (int l = 0; l < width; l++) {
				if (!iterator.hasNext()) {
					return;
				}

				bl = recipeWidth < width / 2.0F;
				k = MathHelper.floor(width / 2.0F - recipeWidth / 2.0F);
				int m = recipeWidth;
				boolean bl2 = l < recipeWidth;
				if (bl) {
					m = k + recipeWidth;
					bl2 = k <= l && l < k + recipeWidth;
				}

				if (bl2) {
					filler.addItemToSlot(iterator.next(), i, l, j);
				}
				else if (m == l) {
					i += width - l;
					break;
				}

				i++;
			}
		}
	}

	@FunctionalInterface
	/**
	 * {@code Filler}.
	 */
	public interface Filler<T> {

		void addItemToSlot(T slot, int index, int x, int y);
	}
}
