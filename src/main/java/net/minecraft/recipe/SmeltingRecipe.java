package net.minecraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;

/**
 * Рецепт плавки в обычной печи. Универсальный тип готовки:
 * принимает блоки, еду и прочие материалы.
 */
public class SmeltingRecipe extends AbstractCookingRecipe {

	public SmeltingRecipe(
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
		return Items.FURNACE;
	}

	@Override
	public RecipeSerializer<SmeltingRecipe> getSerializer() {
		return RecipeSerializer.SMELTING;
	}

	@Override
	public RecipeType<SmeltingRecipe> getType() {
		return RecipeType.SMELTING;
	}

	@Override
	public RecipeBookCategory getRecipeBookCategory() {
		return switch (getCategory()) {
			case BLOCKS -> RecipeBookCategories.FURNACE_BLOCKS;
			case FOOD -> RecipeBookCategories.FURNACE_FOOD;
			case MISC -> RecipeBookCategories.FURNACE_MISC;
		};
	}
}
