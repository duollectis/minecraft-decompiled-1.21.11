package net.minecraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;

/**
 * Рецепт готовки на костре. Готовит медленнее печи, но не требует топлива.
 * Принимает только еду.
 */
public class CampfireCookingRecipe extends AbstractCookingRecipe {

	public CampfireCookingRecipe(
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
		return Items.CAMPFIRE;
	}

	@Override
	public RecipeSerializer<CampfireCookingRecipe> getSerializer() {
		return RecipeSerializer.CAMPFIRE_COOKING;
	}

	@Override
	public RecipeType<CampfireCookingRecipe> getType() {
		return RecipeType.CAMPFIRE_COOKING;
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return RecipeBookCategories.CAMPFIRE;
	}
}
