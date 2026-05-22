package net.minecraft.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

/**
 * Инвентарь слота результата крафта. Содержит ровно один слот (индекс 0),
 * в котором хранится предмет-результат текущего рецепта.
 * Также реализует {@link RecipeUnlocker} для отслеживания последнего использованного рецепта.
 */
public class CraftingResultInventory implements Inventory, RecipeUnlocker {

	private static final int RESULT_SLOT = 0;
	private static final int SIZE = 1;

	private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
	private @Nullable RecipeEntry<?> lastRecipe;

	@Override
	public int size() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return stacks.get(RESULT_SLOT);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return Inventories.removeStack(stacks, RESULT_SLOT);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(stacks, RESULT_SLOT);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		stacks.set(RESULT_SLOT, stack);
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		stacks.clear();
	}

	@Override
	public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
		lastRecipe = recipe;
	}

	@Override
	public @Nullable RecipeEntry<?> getLastRecipe() {
		return lastRecipe;
	}
}
