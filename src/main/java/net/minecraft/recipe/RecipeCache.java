package net.minecraft.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;

/**
 * LRU-кэш результатов крафта для ускорения повторных запросов к менеджеру рецептов.
 * При смене менеджера рецептов (перезагрузка ресурсов) кэш автоматически инвалидируется.
 * Ссылка на менеджер слабая — не препятствует его сборке мусором.
 */
public class RecipeCache {

	private final RecipeCache.@Nullable CachedRecipe[] cache;
	private WeakReference<@Nullable ServerRecipeManager> recipeManagerRef = new WeakReference<>(null);

	public RecipeCache(int size) {
		cache = new RecipeCache.CachedRecipe[size];
	}

	public Optional<RecipeEntry<CraftingRecipe>> getRecipe(ServerWorld world, CraftingRecipeInput input) {
		if (input.isEmpty()) {
			return Optional.empty();
		}

		validateRecipeManager(world);

		for (int index = 0; index < cache.length; index++) {
			RecipeCache.CachedRecipe cached = cache[index];

			if (cached != null && cached.matches(input)) {
				sendToFront(index);
				return Optional.ofNullable(cached.value());
			}
		}

		return getAndCacheRecipe(input, world);
	}

	private void validateRecipeManager(ServerWorld world) {
		ServerRecipeManager manager = world.getRecipeManager();

		if (manager != recipeManagerRef.get()) {
			recipeManagerRef = new WeakReference<>(manager);
			Arrays.fill(cache, null);
		}
	}

	private Optional<RecipeEntry<CraftingRecipe>> getAndCacheRecipe(CraftingRecipeInput input, ServerWorld world) {
		Optional<RecipeEntry<CraftingRecipe>> result = world.getRecipeManager()
			.getFirstMatch(RecipeType.CRAFTING, input, world);

		cache(input, result.orElse(null));

		return result;
	}

	private void sendToFront(int index) {
		if (index <= 0) {
			return;
		}

		RecipeCache.CachedRecipe cached = cache[index];
		System.arraycopy(cache, 0, cache, 1, index);
		cache[0] = cached;
	}

	private void cache(CraftingRecipeInput input, @Nullable RecipeEntry<CraftingRecipe> recipe) {
		DefaultedList<ItemStack> snapshot = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			snapshot.set(slotIndex, input.getStackInSlot(slotIndex).copyWithCount(1));
		}

		System.arraycopy(cache, 0, cache, 1, cache.length - 1);
		cache[0] = new RecipeCache.CachedRecipe(snapshot, input.getWidth(), input.getHeight(), recipe);
	}

	record CachedRecipe(
		DefaultedList<ItemStack> key,
		int width,
		int height,
		@Nullable RecipeEntry<CraftingRecipe> value
	) {

		public boolean matches(CraftingRecipeInput input) {
			if (width != input.getWidth() || height != input.getHeight()) {
				return false;
			}

			for (int slotIndex = 0; slotIndex < key.size(); slotIndex++) {
				if (!ItemStack.areItemsAndComponentsEqual(key.get(slotIndex), input.getStackInSlot(slotIndex))) {
					return false;
				}
			}

			return true;
		}
	}
}
