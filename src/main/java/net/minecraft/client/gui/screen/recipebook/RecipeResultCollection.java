package net.minecraft.client.gui.screen.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.display.RecipeDisplay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Коллекция результатов рецептов для одной группы отображения.
 * Хранит множества крафтабельных и отображаемых рецептов, обновляемые через {@link #populateRecipes}.
 */
@Environment(EnvType.CLIENT)
public class RecipeResultCollection {

	public static final RecipeResultCollection EMPTY = new RecipeResultCollection(List.of());

	private final List<RecipeDisplayEntry> entries;
	private final Set<NetworkRecipeId> craftableRecipes = new HashSet<>();
	private final Set<NetworkRecipeId> displayableRecipes = new HashSet<>();

	public RecipeResultCollection(List<RecipeDisplayEntry> entries) {
		this.entries = entries;
	}

	/**
	 * Обновляет множества крафтабельных и отображаемых рецептов на основе текущего инвентаря.
	 *
	 * @param finder              поисковик рецептов с текущим содержимым инвентаря
	 * @param displayablePredicate предикат, определяющий, должен ли рецепт отображаться
	 */
	public void populateRecipes(RecipeFinder finder, Predicate<RecipeDisplay> displayablePredicate) {
		for (RecipeDisplayEntry entry : entries) {
			boolean displayable = displayablePredicate.test(entry.display());

			if (displayable) {
				displayableRecipes.add(entry.id());
			} else {
				displayableRecipes.remove(entry.id());
			}

			if (displayable && entry.isCraftable(finder)) {
				craftableRecipes.add(entry.id());
			} else {
				craftableRecipes.remove(entry.id());
			}
		}
	}

	public boolean isCraftable(NetworkRecipeId recipeId) {
		return craftableRecipes.contains(recipeId);
	}

	public boolean hasCraftableRecipes() {
		return !craftableRecipes.isEmpty();
	}

	public boolean hasDisplayableRecipes() {
		return !displayableRecipes.isEmpty();
	}

	public List<RecipeDisplayEntry> getAllRecipes() {
		return entries;
	}

	/**
	 * Возвращает отфильтрованный список рецептов согласно режиму фильтрации.
	 *
	 * @param filterMode режим фильтрации: любые, крафтабельные или некрафтабельные
	 * @return список записей рецептов, прошедших фильтр
	 */
	public List<RecipeDisplayEntry> filter(RecipeResultCollection.RecipeFilterMode filterMode) {
		Predicate<NetworkRecipeId> predicate = switch (filterMode) {
			case ANY -> displayableRecipes::contains;
			case CRAFTABLE -> craftableRecipes::contains;
			case NOT_CRAFTABLE ->
					recipeId -> displayableRecipes.contains(recipeId) && !craftableRecipes.contains(recipeId);
		};

		List<RecipeDisplayEntry> result = new ArrayList<>();

		for (RecipeDisplayEntry entry : entries) {
			if (predicate.test(entry.id())) {
				result.add(entry);
			}
		}

		return result;
	}

	@Environment(EnvType.CLIENT)
	public enum RecipeFilterMode {
		ANY,
		CRAFTABLE,
		NOT_CRAFTABLE
	}
}
