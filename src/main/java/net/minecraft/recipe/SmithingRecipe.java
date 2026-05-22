package net.minecraft.recipe;

import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Интерфейс для всех рецептов кузнечного стола.
 * <p>
 * Рецепт кузнеца принимает три слота: шаблон, основу и добавку.
 * Каждый из них может быть опциональным ({@link Optional#empty()} означает,
 * что слот принимает любой предмет, включая пустой).
 */
public interface SmithingRecipe extends Recipe<SmithingRecipeInput> {

	@Override
	default RecipeType<SmithingRecipe> getType() {
		return RecipeType.SMITHING;
	}

	@Override
	RecipeSerializer<? extends SmithingRecipe> getSerializer();

	/**
	 * Проверяет соответствие входных данных рецепту.
	 * Шаблон и добавка проверяются через {@link Ingredient#matches(Optional, ItemStack)},
	 * основа — через {@link Ingredient#test(ItemStack)}.
	 */
	@Override
	default boolean matches(SmithingRecipeInput input, World world) {
		return Ingredient.matches(template(), input.template())
			&& base().test(input.base())
			&& Ingredient.matches(addition(), input.addition());
	}

	Optional<Ingredient> template();

	Ingredient base();

	Optional<Ingredient> addition();

	@Override
	default RecipeBookCategory getRecipeBookCategory() {
		return RecipeBookCategories.SMITHING;
	}
}
