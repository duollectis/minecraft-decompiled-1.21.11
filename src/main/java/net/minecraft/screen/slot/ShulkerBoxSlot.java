package net.minecraft.screen.slot;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Слот инвентаря ящика шалкера.
 * <p>
 * Запрещает вставку предметов, которые не могут быть вложены в контейнер
 * (например, других ящиков шалкера), предотвращая рекурсивное вложение.
 */
public class ShulkerBoxSlot extends Slot {

	public ShulkerBoxSlot(Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return stack.getItem().canBeNested();
	}
}
