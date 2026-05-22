package net.minecraft.recipe.input;

import net.minecraft.item.ItemStack;

/**
 * Базовый интерфейс входных данных рецепта. Предоставляет доступ к стекам предметов
 * по индексу слота и позволяет проверить, пуст ли весь ввод.
 */
public interface RecipeInput {

	ItemStack getStackInSlot(int slot);

	int size();

	default boolean isEmpty() {
		for (int slot = 0; slot < size(); slot++) {
			if (!getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}
