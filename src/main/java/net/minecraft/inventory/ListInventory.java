package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Predicate;

/**
 * Инвентарь, хранящий стаки в {@link DefaultedList}.
 * Предоставляет стандартные реализации всех методов {@link Inventory}
 * на основе списка, что избавляет конкретные классы от дублирования кода.
 */
public interface ListInventory extends Inventory {

	DefaultedList<ItemStack> getHeldStacks();

	default int getFilledSlotCount() {
		return (int) getHeldStacks().stream().filter(Predicate.not(ItemStack::isEmpty)).count();
	}

	@Override
	default int size() {
		return getHeldStacks().size();
	}

	@Override
	default void clear() {
		getHeldStacks().clear();
	}

	@Override
	default boolean isEmpty() {
		return getHeldStacks().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	default ItemStack getStack(int slot) {
		return getHeldStacks().get(slot);
	}

	@Override
	default ItemStack removeStack(int slot, int amount) {
		ItemStack removed = Inventories.splitStack(getHeldStacks(), slot, amount);

		if (!removed.isEmpty()) {
			markDirty();
		}

		return removed;
	}

	@Override
	default ItemStack removeStack(int slot) {
		return Inventories.splitStack(getHeldStacks(), slot, getMaxCountPerStack());
	}

	@Override
	default boolean isValid(int slot, ItemStack stack) {
		return canAccept(stack)
			&& (getStack(slot).isEmpty() || getStack(slot).getCount() < getMaxCount(stack));
	}

	default boolean canAccept(ItemStack stack) {
		return true;
	}

	@Override
	default void setStack(int slot, ItemStack stack) {
		setStackNoMarkDirty(slot, stack);
		markDirty();
	}

	/**
	 * Устанавливает стак в слот без вызова {@link #markDirty()}.
	 * Используется в случаях, когда уведомление об изменении будет отправлено
	 * вручную после серии операций.
	 *
	 * @param slot  индекс слота
	 * @param stack стак для установки
	 */
	default void setStackNoMarkDirty(int slot, ItemStack stack) {
		getHeldStacks().set(slot, stack);
		stack.capCount(getMaxCount(stack));
	}
}
