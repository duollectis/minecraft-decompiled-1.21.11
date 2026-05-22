package net.minecraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;

/**
 * Рецепт плавки в доменной печи. Работает вдвое быстрее обычной печи,
 * но принимает только руды, металлы и схожие материалы (не еду).
 */
public class BlastingRecipe extends AbstractCookingRecipe {

	public BlastingRecipe(
		String group,
		CookingRecipeCategory category,
		Ingredient ingredient,
		ItemStack result,
		float experience,
		int cookingTime
	) {
		super(group, category, ingredient, result, experience, cookingTime);
	}

	@Override
	protected Item getCookerItem() {
		return Items.BLAST_FURNACE;
	}

	@Override
	public RecipeSerializer<BlastingRecipe> getSerializer() {
		return RecipeSerializer.BLASTING;
	}

	@Override
	public RecipeType<BlastingRecipe> getType() {
		return RecipeType.BLASTING;
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return switch (getCategory()) {
			case BLOCKS -> RecipeBookCategories.BLAST_FURNACE_BLOCKS;
			case FOOD, MISC -> RecipeBookCategories.BLAST_FURNACE_MISC;
		};
	}
}
