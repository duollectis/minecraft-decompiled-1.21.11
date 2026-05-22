package net.minecraft.inventory;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Базовая реализация {@link Inventory} на основе {@link DefaultedList}.
 * Поддерживает подписку слушателей на изменения через {@link InventoryChangedListener},
 * а также операции добавления предметов с автоматическим объединением стаков.
 */
public class SimpleInventory implements Inventory, RecipeInputProvider {

	private final int size;
	public final DefaultedList<ItemStack> heldStacks;
	private @Nullable List<InventoryChangedListener> listeners;

	public SimpleInventory(int size) {
		this.size = size;
		heldStacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
	}

	public SimpleInventory(ItemStack... items) {
		size = items.length;
		heldStacks = DefaultedList.copyOf(ItemStack.EMPTY, items);
	}

	public void addListener(InventoryChangedListener listener) {
		if (listeners == null) {
			listeners = Lists.newArrayList();
		}

		listeners.add(listener);
	}

	public void removeListener(InventoryChangedListener listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	@Override
	public ItemStack getStack(int slot) {
		return slot >= 0 && slot < heldStacks.size() ? heldStacks.get(slot) : ItemStack.EMPTY;
	}

	/**
	 * Извлекает все непустые стаки из инвентаря в список и очищает инвентарь.
	 *
	 * @return список непустых стаков, которые были в инвентаре до очистки
	 */
	public List<ItemStack> clearToList() {
		List<ItemStack> nonEmpty = heldStacks.stream()
			.filter(stack -> !stack.isEmpty())
			.collect(Collectors.toList());

		clear();

		return nonEmpty;
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack removed = Inventories.splitStack(heldStacks, slot, amount);

		if (!removed.isEmpty()) {
			markDirty();
		}

		return removed;
	}

	/**
	 * Извлекает из инвентаря указанное количество предметов заданного типа,
	 * обходя слоты в обратном порядке (с конца к началу).
	 *
	 * @param item  тип предмета для извлечения
	 * @param count желаемое количество предметов
	 * @return стак с извлечёнными предметами; может содержать меньше {@code count}, если предметов не хватило
	 */
	public ItemStack removeItem(Item item, int count) {
		ItemStack collected = new ItemStack(item, 0);

		for (int slot = size - 1; slot >= 0; slot--) {
			ItemStack slotStack = getStack(slot);

			if (!slotStack.getItem().equals(item)) {
				continue;
			}

			int needed = count - collected.getCount();
			ItemStack taken = slotStack.split(needed);
			collected.increment(taken.getCount());

			if (collected.getCount() == count) {
				break;
			}
		}

		if (!collected.isEmpty()) {
			markDirty();
		}

		return collected;
	}

	/**
	 * Добавляет стак в инвентарь: сначала пытается объединить с существующими стаками,
	 * затем помещает остаток в первый свободный слот.
	 *
	 * @param stack стак для добавления
	 * @return остаток стака, который не удалось разместить; {@link ItemStack#EMPTY} если всё вошло
	 */
	public ItemStack addStack(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack copy = stack.copy();
		addToExistingSlot(copy);

		if (copy.isEmpty()) {
			return ItemStack.EMPTY;
		}

		addToNewSlot(copy);

		return copy.isEmpty() ? ItemStack.EMPTY : copy;
	}

	/**
	 * Проверяет, можно ли добавить хотя бы один предмет из стака в инвентарь.
	 *
	 * @param stack стак для проверки
	 * @return {@code true}, если есть свободный слот или слот с совместимым стаком, не заполненным до максимума
	 */
	public boolean canInsert(ItemStack stack) {
		for (ItemStack slotStack : heldStacks) {
			boolean hasSpace = slotStack.isEmpty()
				|| (ItemStack.areItemsAndComponentsEqual(slotStack, stack)
				&& slotStack.getCount() < slotStack.getMaxCount());

			if (hasSpace) {
				return true;
			}
		}

		return false;
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack stack = heldStacks.get(slot);

		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		heldStacks.set(slot, ItemStack.EMPTY);

		return stack;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		heldStacks.set(slot, stack);
		stack.capCount(getMaxCount(stack));
		markDirty();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : heldStacks) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void markDirty() {
		if (listeners == null) {
			return;
		}

		for (InventoryChangedListener listener : listeners) {
			listener.onInventoryChanged(this);
		}
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		heldStacks.clear();
		markDirty();
	}

	@Override
	public void provideRecipeInputs(RecipeFinder finder) {
		for (ItemStack stack : heldStacks) {
			finder.addInput(stack);
		}
	}

	@Override
	public String toString() {
		return heldStacks.stream()
			.filter(stack -> !stack.isEmpty())
			.collect(Collectors.toList())
			.toString();
	}

	public void readDataList(ReadView.TypedListReadView<ItemStack> list) {
		clear();

		for (ItemStack stack : list) {
			addStack(stack);
		}
	}

	public void toDataList(WriteView.ListAppender<ItemStack> list) {
		for (int slot = 0; slot < size(); slot++) {
			ItemStack stack = getStack(slot);

			if (!stack.isEmpty()) {
				list.add(stack);
			}
		}
	}

	public DefaultedList<ItemStack> getHeldStacks() {
		return heldStacks;
	}

	private void addToNewSlot(ItemStack stack) {
		for (int slot = 0; slot < size; slot++) {
			if (getStack(slot).isEmpty()) {
				setStack(slot, stack.copyAndEmpty());
				return;
			}
		}
	}

	private void addToExistingSlot(ItemStack stack) {
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotStack = getStack(slot);

			if (ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
				transfer(stack, slotStack);

				if (stack.isEmpty()) {
					return;
				}
			}
		}
	}

	private void transfer(ItemStack source, ItemStack target) {
		int maxCount = getMaxCount(target);
		int transferable = Math.min(source.getCount(), maxCount - target.getCount());

		if (transferable > 0) {
			target.increment(transferable);
			source.decrement(transferable);
			markDirty();
		}
	}
}
