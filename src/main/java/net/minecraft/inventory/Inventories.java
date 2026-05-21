package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;
import java.util.function.Predicate;

/**
 * {@code Inventories}.
 */
public class Inventories {

	public static final String ITEMS_NBT_KEY = "Items";

	public static ItemStack splitStack(List<ItemStack> stacks, int slot, int amount) {
		return slot >= 0 && slot < stacks.size() && !stacks.get(slot).isEmpty() && amount > 0 ? stacks
		                                                                                        .get(slot)
		                                                                                        .split(amount)
		                                                                                      : ItemStack.EMPTY;
	}

	public static ItemStack removeStack(List<ItemStack> stacks, int slot) {
		return slot >= 0 && slot < stacks.size() ? stacks.set(slot, ItemStack.EMPTY) : ItemStack.EMPTY;
	}

	public static void writeData(WriteView view, DefaultedList<ItemStack> stacks) {
		writeData(view, stacks, true);
	}

	public static void writeData(WriteView view, DefaultedList<ItemStack> stacks, boolean setIfEmpty) {
		WriteView.ListAppender<StackWithSlot> listAppender = view.getListAppender("Items", StackWithSlot.CODEC);

		for (int i = 0; i < stacks.size(); i++) {
			ItemStack itemStack = stacks.get(i);
			if (!itemStack.isEmpty()) {
				listAppender.add(new StackWithSlot(i, itemStack));
			}
		}

		if (listAppender.isEmpty() && !setIfEmpty) {
			view.remove("Items");
		}
	}

	public static void readData(ReadView view, DefaultedList<ItemStack> stacks) {
		for (StackWithSlot stackWithSlot : view.getTypedListView("Items", StackWithSlot.CODEC)) {
			if (stackWithSlot.isValidSlot(stacks.size())) {
				stacks.set(stackWithSlot.slot(), stackWithSlot.stack());
			}
		}
	}

	public static int remove(Inventory inventory, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
		int i = 0;

		for (int j = 0; j < inventory.size(); j++) {
			ItemStack itemStack = inventory.getStack(j);
			int k = remove(itemStack, shouldRemove, maxCount - i, dryRun);
			if (k > 0 && !dryRun && itemStack.isEmpty()) {
				inventory.setStack(j, ItemStack.EMPTY);
			}

			i += k;
		}

		return i;
	}

	public static int remove(ItemStack stack, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
		if (stack.isEmpty() || !shouldRemove.test(stack)) {
			return 0;
		}
		else if (dryRun) {
			return stack.getCount();
		}
		else {
			int i = maxCount < 0 ? stack.getCount() : Math.min(maxCount, stack.getCount());
			stack.decrement(i);
			return i;
		}
	}
}
