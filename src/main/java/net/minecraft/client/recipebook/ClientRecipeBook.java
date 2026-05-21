package net.minecraft.client.recipebook;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Table;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.book.RecipeBookGroup;

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code ClientRecipeBook}.
 */
public class ClientRecipeBook extends RecipeBook {

	private final Map<NetworkRecipeId, RecipeDisplayEntry> recipes = new HashMap<>();
	private final Set<NetworkRecipeId> highlightedRecipes = new HashSet<>();
	private Map<RecipeBookGroup, List<RecipeResultCollection>> resultsByCategory = Map.of();
	private List<RecipeResultCollection> orderedResults = List.of();

	/**
	 * Add.
	 *
	 * @param entry entry
	 */
	public void add(RecipeDisplayEntry entry) {
		this.recipes.put(entry.id(), entry);
	}

	/**
	 * Remove.
	 *
	 * @param recipeId recipe id
	 */
	public void remove(NetworkRecipeId recipeId) {
		this.recipes.remove(recipeId);
		this.highlightedRecipes.remove(recipeId);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.recipes.clear();
		this.highlightedRecipes.clear();
	}

	public boolean isHighlighted(NetworkRecipeId recipeId) {
		return this.highlightedRecipes.contains(recipeId);
	}

	/**
	 * Unmark highlighted.
	 *
	 * @param recipeId recipe id
	 */
	public void unmarkHighlighted(NetworkRecipeId recipeId) {
		this.highlightedRecipes.remove(recipeId);
	}

	/**
	 * Mark highlighted.
	 *
	 * @param recipeId recipe id
	 */
	public void markHighlighted(NetworkRecipeId recipeId) {
		this.highlightedRecipes.add(recipeId);
	}

	/**
	 * Refresh.
	 */
	public void refresh() {
		Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = toGroupedMap(this.recipes.values());
		Map<RecipeBookGroup, List<RecipeResultCollection>> map2 = new HashMap<>();
		Builder<RecipeResultCollection> builder = ImmutableList.builder();
		map.forEach(
				(group, resultCollections) -> map2.put(
						group,
						(List) resultCollections
								.stream()
								.map(RecipeResultCollection::new)
								.peek(builder::add)
								.collect(ImmutableList.toImmutableList())
				)
		);

		for (RecipeBookType recipeBookType : RecipeBookType.values()) {
			map2.put(
					recipeBookType,
					recipeBookType
							.getCategories()
							.stream()
							.flatMap(group -> map2.getOrDefault(group, List.of()).stream())
							.collect(ImmutableList.toImmutableList())
			);
		}

		this.resultsByCategory = Map.copyOf(map2);
		this.orderedResults = builder.build();
	}

	private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> toGroupedMap(Iterable<RecipeDisplayEntry> recipes) {
		Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = new HashMap<>();
		Table<RecipeBookCategory, Integer, List<RecipeDisplayEntry>> table = HashBasedTable.create();

		for (RecipeDisplayEntry recipeDisplayEntry : recipes) {
			RecipeBookCategory recipeBookCategory = recipeDisplayEntry.category();
			OptionalInt optionalInt = recipeDisplayEntry.group();
			if (optionalInt.isEmpty()) {
				map.computeIfAbsent(recipeBookCategory, group -> new ArrayList<>()).add(List.of(recipeDisplayEntry));
			}
			else {
				List<RecipeDisplayEntry>
						list =
						(List<RecipeDisplayEntry>) table.get(recipeBookCategory, optionalInt.getAsInt());
				if (list == null) {
					list = new ArrayList<>();
					table.put(recipeBookCategory, optionalInt.getAsInt(), list);
					map.computeIfAbsent(recipeBookCategory, group -> new ArrayList<>()).add(list);
				}

				list.add(recipeDisplayEntry);
			}
		}

		return map;
	}

	public List<RecipeResultCollection> getOrderedResults() {
		return this.orderedResults;
	}

	public List<RecipeResultCollection> getResultsForCategory(RecipeBookGroup category) {
		return this.resultsByCategory.getOrDefault(category, Collections.emptyList());
	}
}
