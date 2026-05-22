package net.minecraft.screen.slot;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AbstractFurnaceScreenHandler;

/**
 * Слот топлива печи.
 * <p>
 * Принимает любое топливо, зарегистрированное в реестре топлива мира,
 * а также пустые вёдра (которые остаются после сжигания лавы).
 * Для вёдер ограничивает стак до 1 предмета.
 */
public class FurnaceFuelSlot extends Slot {

	private final AbstractFurnaceScreenHandler handler;

	public FurnaceFuelSlot(AbstractFurnaceScreenHandler handler, Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
		this.handler = handler;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return handler.isFuel(stack) || isBucket(stack);
	}

	@Override
	public int getMaxItemCount(ItemStack stack) {
		return isBucket(stack) ? 1 : super.getMaxItemCount(stack);
	}

	public static boolean isBucket(ItemStack stack) {
		return stack.isOf(Items.BUCKET);
	}
}
