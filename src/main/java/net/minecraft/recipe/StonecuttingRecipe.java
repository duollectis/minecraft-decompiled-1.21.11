package net.minecraft.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.StonecutterRecipeDisplay;

import java.util.List;

/**
 * Рецепт камнерезного станка: преобразует один каменный материал в другой.
 * В отличие от крафтового стола, позволяет получать больше вариантов из одного блока.
 */
public class StonecuttingRecipe extends SingleStackRecipe {

	public StonecuttingRecipe(String group, Ingredient ingredient, ItemStack result) {
		super(group, ingredient, result);
	}

	@Override
	public RecipeType<StonecuttingRecipe> getType() {
		return RecipeType.STONECUTTING;
	}

	@Override
	public RecipeSerializer<StonecuttingRecipe> getSerializer() {
		return RecipeSerializer.STONECUTTING;
	}

	@Override
	public List<RecipeDisplay> getDisplays() {
		return List.of(
			new StonecutterRecipeDisplay(
				ingredient().toDisplay(),
				createResultDisplay(),
				new SlotDisplay.ItemSlotDisplay(Items.STONECUTTER)
			)
		);
	}

	public SlotDisplay createResultDisplay() {
		return new SlotDisplay.StackSlotDisplay(result());
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return RecipeBookCategories.STONECUTTER;
	}
}
