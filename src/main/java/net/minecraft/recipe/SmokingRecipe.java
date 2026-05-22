package net.minecraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;

/**
 * Рецепт готовки в коптильне. Работает вдвое быстрее обычной печи,
 * но принимает только еду.
 */
public class SmokingRecipe extends AbstractCookingRecipe {

	public SmokingRecipe(
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
		return Items.SMOKER;
	}

	@Override
	public RecipeType<SmokingRecipe> getType() {
		return RecipeType.SMOKING;
	}

	@Override
	public RecipeSerializer<SmokingRecipe> getSerializer() {
		return RecipeSerializer.SMOKING;
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return RecipeBookCategories.SMOKER_FOOD;
	}
}
