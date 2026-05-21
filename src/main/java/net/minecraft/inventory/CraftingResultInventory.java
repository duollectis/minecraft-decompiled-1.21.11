package net.minecraft.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

/**
 * {@code CraftingResultInventory}.
 */
public class CraftingResultInventory implements Inventory, RecipeUnlocker {

	private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(1, ItemStack.EMPTY);
	private @Nullable RecipeEntry<?> lastRecipe;

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemStack : this.stacks) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return this.stacks.get(0);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return Inventories.removeStack(this.stacks, 0);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(this.stacks, 0);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		this.stacks.set(0, stack);
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
		this.stacks.clear();
	}

	@Override
	public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
		this.lastRecipe = recipe;
	}

	@Override
	public @Nullable RecipeEntry<?> getLastRecipe() {
		return this.lastRecipe;
	}
}
