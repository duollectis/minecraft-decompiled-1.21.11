package net.minecraft.recipe;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Фасад над {@link RecipeMatcher} для работы с предметами инвентаря.
 * Накапливает доступные предметы и проверяет возможность крафта рецептов.
 */
public class RecipeFinder {

	private final RecipeMatcher<RegistryEntry<Item>> recipeMatcher = new RecipeMatcher<>();

	public void addInputIfUsable(ItemStack item) {
		if (PlayerInventory.usableWhenFillingSlot(item)) {
			addInput(item);
		}
	}

	public void addInput(ItemStack item) {
		addInput(item, item.getMaxCount());
	}

	public void addInput(ItemStack item, int maxCount) {
		if (item.isEmpty()) {
			return;
		}

		int count = Math.min(maxCount, item.getCount());
		recipeMatcher.add(item.getRegistryEntry(), count);
	}

	public boolean isCraftable(
		Recipe<?> recipe,
		RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return isCraftable(recipe, 1, itemCallback);
	}

	public boolean isCraftable(
		Recipe<?> recipe,
		int quantity,
		RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		IngredientPlacement placement = recipe.getIngredientPlacement();

		return placement.hasNoPlacement()
			? false
			: isCraftable(placement.getIngredients(), quantity, itemCallback);
	}

	public boolean isCraftable(
		List<? extends RecipeMatcher.RawIngredient<RegistryEntry<Item>>> rawIngredients,
		RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return isCraftable(rawIngredients, 1, itemCallback);
	}

	private boolean isCraftable(
		List<? extends RecipeMatcher.RawIngredient<RegistryEntry<Item>>> rawIngredients,
		int quantity,
		RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return recipeMatcher.match(rawIngredients, quantity, itemCallback);
	}

	public int countCrafts(Recipe<?> recipe, RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback) {
		return countCrafts(recipe, Integer.MAX_VALUE, itemCallback);
	}

	public int countCrafts(
		Recipe<?> recipe,
		int max,
		RecipeMatcher.@Nullable ItemCallback<RegistryEntry<Item>> itemCallback
	) {
		return recipeMatcher.countCrafts(recipe.getIngredientPlacement().getIngredients(), max, itemCallback);
	}

	public void clear() {
		recipeMatcher.clear();
	}
}
