package net.minecraft.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

/**
 * Инвентарь сетки крафта, привязанный к {@link ScreenHandler}.
 * Любое изменение содержимого немедленно уведомляет обработчик экрана
 * через {@link ScreenHandler#onContentChanged(Inventory)}, что инициирует
 * пересчёт доступных рецептов.
 */
public class CraftingInventory implements RecipeInputInventory {

	private final DefaultedList<ItemStack> stacks;
	private final int width;
	private final int height;
	private final ScreenHandler handler;

	public CraftingInventory(ScreenHandler handler, int width, int height) {
		this(handler, width, height, DefaultedList.ofSize(width * height, ItemStack.EMPTY));
	}

	private CraftingInventory(ScreenHandler handler, int width, int height, DefaultedList<ItemStack> stacks) {
		this.stacks = stacks;
		this.handler = handler;
		this.width = width;
		this.height = height;
	}

	@Override
	public int size() {
		return stacks.size();
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
		return slot >= size() ? ItemStack.EMPTY : stacks.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(stacks, slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack removed = Inventories.splitStack(stacks, slot, amount);

		if (!removed.isEmpty()) {
			handler.onContentChanged(this);
		}

		return removed;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		stacks.set(slot, stack);
		handler.onContentChanged(this);
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
	public int getHeight() {
		return height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public List<ItemStack> getHeldStacks() {
		return List.copyOf(stacks);
	}

	@Override
	public void provideRecipeInputs(RecipeFinder finder) {
		for (ItemStack stack : stacks) {
			finder.addInputIfUsable(stack);
		}
	}
}
