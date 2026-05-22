package net.minecraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.collection.DefaultedList;

/**
 * Интерфейс для всех рецептов верстака (крафтинга).
 * <p>
 * Определяет тип рецепта как {@link RecipeType#CRAFTING} и маппинг категории
 * рецепта на соответствующую вкладку книги рецептов.
 */
public interface CraftingRecipe extends Recipe<CraftingRecipeInput> {

	@Override
	default RecipeType<CraftingRecipe> getType() {
		return RecipeType.CRAFTING;
	}

	@Override
	RecipeSerializer<? extends CraftingRecipe> getSerializer();

	CraftingRecipeCategory getCategory();

	default DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
		return collectRecipeRemainders(input);
	}

	/**
	 * Собирает остатки от всех ингредиентов в сетке крафта.
	 * Например, вёдра возвращают пустое ведро, бутылки — пустую бутылку.
	 *
	 * @param input сетка крафта с текущими ингредиентами
	 * @return список остатков, по одному на каждый слот сетки
	 */
	static DefaultedList<ItemStack> collectRecipeRemainders(CraftingRecipeInput input) {
		DefaultedList<ItemStack> remainders = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);

		for (int slotIndex = 0; slotIndex < remainders.size(); slotIndex++) {
			Item item = input.getStackInSlot(slotIndex).getItem();
			remainders.set(slotIndex, item.getRecipeRemainder());
		}

		return remainders;
	}

	@Override
	default RecipeBookCategory getRecipeBookCategory() {
		return switch (getCategory()) {
			case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
			case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
			case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
			case MISC -> RecipeBookCategories.CRAFTING_MISC;
		};
	}
}
