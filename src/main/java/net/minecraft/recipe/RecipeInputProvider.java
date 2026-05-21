package net.minecraft.recipe;

@FunctionalInterface
/**
 * {@code RecipeInputProvider}.
 */
public interface RecipeInputProvider {

	void provideRecipeInputs(RecipeFinder finder);
}
