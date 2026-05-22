package net.minecraft.recipe.input;

import net.minecraft.item.ItemStack;

/**
 * Входные данные рецепта с единственным слотом (печь, коптильня, костёр, камнерез).
 */
public record SingleStackRecipeInput(ItemStack item) implements RecipeInput {

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot != 0) {
			throw new IllegalArgumentException("No item for index " + slot);
		}

		return item;
	}

	@Override
	public int size() {
		return 1;
	}
}
