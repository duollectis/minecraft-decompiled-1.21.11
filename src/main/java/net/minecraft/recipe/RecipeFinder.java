package net.minecraft.recipe;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@code RecipeFinder}.
 */
public class RecipeFinder {

	private final RecipeMatcher<RegistryEntry<Item>> recipeMatcher = new RecipeMatcher<>();

	public void addInputIfUsable(ItemStack item) {
		if (PlayerInventory.usableWhenFillingSlot(item)) {
			this.addInput(item);
		}
	}

	public void addInput(ItemStack item) {
		this.addInput(item, item.getMaxCount());
	}

	public void addInput(ItemStack item, int maxCount) {
		if (!item.isEmpty()) {
			int i = Math.min(maxCount, item.getCount());
			this.recipeMatcher.add(item.getRegistryEntry(), i);
		}
	}

	public boolean isCraftable(
			Recipe<?> recipe,
			RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return this.isCraftable(recipe, 1, itemCallback);
	}

	public boolean isCraftable(
			Recipe<?> recipe,
			int quantity,
			RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		IngredientPlacement ingredientPlacement = recipe.getIngredientPlacement();
		return ingredientPlacement.hasNoPlacement() ? false : this.isCraftable(
				ingredientPlacement.getIngredients(),
				quantity,
				itemCallback
		);
	}

	public boolean isCraftable(
			List<? extends RecipeMatcher.RawIngredient<RegistryEntry<Item>>> rawIngredients,
			RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return this.isCraftable(rawIngredients, 1, itemCallback);
	}

	private boolean isCraftable(
			List<? extends RecipeMatcher.RawIngredient<RegistryEntry<Item>>> rawIngredients,
			int quantity,
			RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return this.recipeMatcher.match(rawIngredients, quantity, itemCallback);
	}

	public int countCrafts(Recipe<?> recipe, RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback) {
		return this.countCrafts(recipe, Integer.MAX_VALUE, itemCallback);
	}

	public int countCrafts(
			Recipe<?> recipe,
			int max,
			RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return this.recipeMatcher.countCrafts(recipe.getIngredientPlacement().getIngredients(), max, itemCallback);
	}

	public void clear() {
		this.recipeMatcher.clear();
	}
}
