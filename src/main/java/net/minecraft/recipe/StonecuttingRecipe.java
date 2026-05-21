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
 * {@code StonecuttingRecipe}.
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
						this.ingredient().toDisplay(),
						this.createResultDisplay(),
						new SlotDisplay.ItemSlotDisplay(Items.STONECUTTER)
				)
		);
	}

	/**
	 * Создаёт result display.
	 *
	 * @return SlotDisplay — результат операции
	 */
	public SlotDisplay createResultDisplay() {
		return new SlotDisplay.StackSlotDisplay(this.result());
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return RecipeBookCategories.STONECUTTER;
	}
}
