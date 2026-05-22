package net.minecraft.inventory;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Инвентарь, содержащий ровно один слот. Делегирует все операции
 * с конкретным слотом к методам {@link #getStack()} и {@link #setStack(ItemStack)},
 * которые реализующий класс обязан предоставить.
 */
public interface SingleStackInventory extends Inventory {

	ItemStack getStack();

	void setStack(ItemStack stack);

	default ItemStack decreaseStack(int count) {
		return getStack().split(count);
	}

	default ItemStack emptyStack() {
		return decreaseStack(getMaxCountPerStack());
	}

	@Override
	default int size() {
		return 1;
	}

	@Override
	default boolean isEmpty() {
		return getStack().isEmpty();
	}

	@Override
	default void clear() {
		emptyStack();
	}

	@Override
	default ItemStack getStack(int slot) {
		return slot == 0 ? getStack() : ItemStack.EMPTY;
	}

	@Override
	default ItemStack removeStack(int slot) {
		return removeStack(slot, getMaxCountPerStack());
	}

	@Override
	default ItemStack removeStack(int slot, int amount) {
		return slot != 0 ? ItemStack.EMPTY : decreaseStack(amount);
	}

	@Override
	default void setStack(int slot, ItemStack stack) {
		if (slot == 0) {
			setStack(stack);
		}
	}

	/**
	 * Специализация {@link SingleStackInventory} для блок-сущностей.
	 * Делегирует проверку прав доступа игрока к стандартной логике
	 * {@link Inventory#canPlayerUse(BlockEntity, PlayerEntity)}.
	 */
	interface SingleStackBlockEntityInventory extends SingleStackInventory {

		BlockEntity asBlockEntity();

		@Override
		default boolean canPlayerUse(PlayerEntity player) {
			return Inventory.canPlayerUse(asBlockEntity(), player);
		}
	}
}
