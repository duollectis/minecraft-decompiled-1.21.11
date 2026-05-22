package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Рецепт создания стрел с зельем: заполненная сетка 3×3, где центр — задерживающееся зелье,
 * а все 8 окружающих слотов — обычные стрелы. Результат — 8 стрел с эффектом зелья.
 */
public class TippedArrowRecipe extends SpecialCraftingRecipe {

	private static final int FULL_GRID_SIZE = 9;
	private static final int TIPPED_ARROW_OUTPUT_COUNT = 8;
	private static final int CENTER_COL = 1;
	private static final int CENTER_ROW = 1;

	public TippedArrowRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getWidth() != 3 || input.getHeight() != 3 || input.getStackCount() != FULL_GRID_SIZE) {
			return false;
		}

		for (int row = 0; row < input.getHeight(); row++) {
			for (int col = 0; col < input.getWidth(); col++) {
				ItemStack stack = input.getStackInSlot(col, row);

				if (stack.isEmpty()) {
					return false;
				}

				if (col == CENTER_COL && row == CENTER_ROW) {
					if (!stack.isOf(Items.LINGERING_POTION)) {
						return false;
					}
				} else if (!stack.isOf(Items.ARROW)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup wrapperLookup) {
		ItemStack potion = input.getStackInSlot(CENTER_COL, CENTER_ROW);

		if (!potion.isOf(Items.LINGERING_POTION)) {
			return ItemStack.EMPTY;
		}

		ItemStack result = new ItemStack(Items.TIPPED_ARROW, TIPPED_ARROW_OUTPUT_COUNT);
		result.set(DataComponentTypes.POTION_CONTENTS, potion.get(DataComponentTypes.POTION_CONTENTS));

		return result;
	}

	@Override
	public RecipeSerializer<TippedArrowRecipe> getSerializer() {
		return RecipeSerializer.TIPPED_ARROW;
	}
}
