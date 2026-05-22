package net.minecraft.recipe;

/**
 * Реализуется контейнерами, которые могут предоставлять свои предметы
 * для проверки возможности крафта через {@link RecipeFinder}.
 */
@FunctionalInterface
public interface RecipeInputProvider {

	void provideRecipeInputs(RecipeFinder finder);
}
