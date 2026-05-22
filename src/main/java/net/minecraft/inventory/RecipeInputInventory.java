package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.input.CraftingRecipeInput;

import java.util.List;

/**
 * Инвентарь, служащий входными данными для рецептов крафта.
 * Предоставляет размеры сетки крафта и список хранимых стаков,
 * а также методы создания {@link CraftingRecipeInput} для проверки рецептов.
 */
public interface RecipeInputInventory extends Inventory, RecipeInputProvider {

	int getWidth();

	int getHeight();

	List<ItemStack> getHeldStacks();

	default CraftingRecipeInput createRecipeInput() {
		return createPositionedRecipeInput().input();
	}

	/**
	 * Создаёт позиционированный ввод рецепта с учётом размеров сетки крафта.
	 * Позиционирование необходимо для корректного определения смещения рецепта
	 * внутри сетки при поиске совпадений.
	 *
	 * @return позиционированный ввод рецепта крафта
	 */
	default CraftingRecipeInput.Positioned createPositionedRecipeInput() {
		return CraftingRecipeInput.createPositioned(getWidth(), getHeight(), getHeldStacks());
	}
}
