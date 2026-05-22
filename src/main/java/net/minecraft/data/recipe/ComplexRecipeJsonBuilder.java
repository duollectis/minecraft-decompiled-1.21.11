package net.minecraft.data.recipe;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * {@code ComplexRecipeJsonBuilder}.
 */
public class ComplexRecipeJsonBuilder {

	private final Function<CraftingRecipeCategory, Recipe<?>> recipeFactory;

	public ComplexRecipeJsonBuilder(Function<CraftingRecipeCategory, Recipe<?>> recipeFactory) {
		this.recipeFactory = recipeFactory;
	}

	public static ComplexRecipeJsonBuilder create(Function<CraftingRecipeCategory, Recipe<?>> recipeFactory) {
		return new ComplexRecipeJsonBuilder(recipeFactory);
	}

	public void offerTo(RecipeExporter exporter, String id) {
		offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(id)));
	}

	public void offerTo(RecipeExporter exporter, RegistryKey<Recipe<?>> recipeKey) {
		exporter.accept(recipeKey, this.recipeFactory.apply(CraftingRecipeCategory.MISC), null);
	}
}
