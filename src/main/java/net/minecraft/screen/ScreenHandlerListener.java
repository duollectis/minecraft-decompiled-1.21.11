package net.minecraft.screen;

import net.minecraft.item.ItemStack;

/**
 * {@code ScreenHandlerListener}.
 */
public interface ScreenHandlerListener {

	void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack);

	void onPropertyUpdate(ScreenHandler handler, int property, int value);
}
