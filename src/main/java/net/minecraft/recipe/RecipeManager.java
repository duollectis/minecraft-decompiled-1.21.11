package net.minecraft.recipe;

import net.fabricmc.fabric.api.recipe.v1.FabricRecipeManager;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.registry.RegistryKey;

/**
 * Интерфейс менеджера рецептов, предоставляющий доступ к наборам свойств ингредиентов
 * и рецептам камнерезного станка для клиентской и серверной сторон.
 */
public interface RecipeManager extends FabricRecipeManager {

	RecipePropertySet getPropertySet(RegistryKey<RecipePropertySet> key);

	CuttingRecipeDisplay.Grouping<StonecuttingRecipe> getStonecutterRecipes();
}
