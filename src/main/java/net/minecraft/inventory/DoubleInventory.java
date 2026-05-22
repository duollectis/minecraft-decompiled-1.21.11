package net.minecraft.inventory;

import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Составной инвентарь, объединяющий два отдельных инвентаря в один логический.
 * Слоты первого инвентаря занимают диапазон {@code [0, first.size())},
 * слоты второго — {@code [first.size(), first.size() + second.size())}.
 * Используется, например, для двойных сундуков.
 */
public class DoubleInventory implements Inventory {

	private final Inventory first;
	private final Inventory second;

	public DoubleInventory(Inventory first, Inventory second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public int size() {
		return first.size() + second.size();
	}

	@Override
	public boolean isEmpty() {
		return first.isEmpty() && second.isEmpty();
	}

	public boolean isPart(Inventory inventory) {
		return first == inventory || second == inventory;
	}

	@Override
	public ItemStack getStack(int slot) {
		return slot >= first.size()
			? second.getStack(slot - first.size())
			: first.getStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return slot >= first.size()
			? second.removeStack(slot - first.size(), amount)
			: first.removeStack(slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return slot >= first.size()
			? second.removeStack(slot - first.size())
			: first.removeStack(slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (slot >= first.size()) {
			second.setStack(slot - first.size(), stack);
		} else {
			first.setStack(slot, stack);
		}
	}

	@Override
	public int getMaxCountPerStack() {
		return first.getMaxCountPerStack();
	}

	@Override
	public void markDirty() {
		first.markDirty();
		second.markDirty();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return first.canPlayerUse(player) && second.canPlayerUse(player);
	}

	@Override
	public void onOpen(ContainerUser user) {
		first.onOpen(user);
		second.onOpen(user);
	}

	@Override
	public void onClose(ContainerUser user) {
		first.onClose(user);
		second.onClose(user);
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return slot >= first.size()
			? second.isValid(slot - first.size(), stack)
			: first.isValid(slot, stack);
	}

	@Override
	public void clear() {
		first.clear();
		second.clear();
	}
}
