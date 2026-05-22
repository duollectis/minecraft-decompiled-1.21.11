package net.minecraft.recipe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Описывает расположение ингредиентов рецепта по слотам для автозаполнения сетки крафта.
 * Хранит список ингредиентов и соответствующие им индексы слотов.
 * Слот со значением {@link #EMPTY_SLOT} означает отсутствие ингредиента в данной позиции.
 */
public class IngredientPlacement {

	public static final int EMPTY_SLOT = -1;
	public static final IngredientPlacement NONE = new IngredientPlacement(List.of(), IntList.of());

	private final List<Ingredient> ingredients;
	private final IntList placementSlots;

	private IngredientPlacement(List<Ingredient> ingredients, IntList placementSlots) {
		this.ingredients = ingredients;
		this.placementSlots = placementSlots;
	}

	public static IngredientPlacement forSingleSlot(Ingredient ingredient) {
		return ingredient.isEmpty() ? NONE : new IngredientPlacement(List.of(ingredient), IntList.of(0));
	}

	/**
	 * Создаёт размещение для рецептов с фиксированными слотами (например, кузнечный стол).
	 * Опциональные ингредиенты, отсутствующие в списке, получают индекс {@link #EMPTY_SLOT}.
	 * Если хотя бы один присутствующий ингредиент пуст — возвращает {@link #NONE}.
	 */
	public static IngredientPlacement forMultipleSlots(List<Optional<Ingredient>> ingredients) {
		int slotCount = ingredients.size();
		List<Ingredient> presentIngredients = new ArrayList<>(slotCount);
		IntList slotIndices = new IntArrayList(slotCount);
		int ingredientIndex = 0;

		for (Optional<Ingredient> optional : ingredients) {
			if (optional.isPresent()) {
				Ingredient ingredient = optional.get();

				if (ingredient.isEmpty()) {
					return NONE;
				}

				presentIngredients.add(ingredient);
				slotIndices.add(ingredientIndex++);
			} else {
				slotIndices.add(EMPTY_SLOT);
			}
		}

		return new IngredientPlacement(presentIngredients, slotIndices);
	}

	/**
	 * Создаёт размещение для бесформенных рецептов: каждый ингредиент
	 * получает индекс, равный его позиции в списке.
	 * Если хотя бы один ингредиент пуст — возвращает {@link #NONE}.
	 */
	public static IngredientPlacement forShapeless(List<Ingredient> ingredients) {
		int count = ingredients.size();
		IntList slotIndices = new IntArrayList(count);

		for (int index = 0; index < count; index++) {
			if (ingredients.get(index).isEmpty()) {
				return NONE;
			}

			slotIndices.add(index);
		}

		return new IngredientPlacement(ingredients, slotIndices);
	}

	public IntList getPlacementSlots() {
		return placementSlots;
	}

	public List<Ingredient> getIngredients() {
		return ingredients;
	}

	public boolean hasNoPlacement() {
		return placementSlots.isEmpty();
	}
}
