package net.minecraft.screen;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.sync.TrackedSlot;

public interface ScreenHandlerSyncHandler {
   void updateState(ScreenHandler handler, List<ItemStack> stacks, ItemStack cursorStack, int[] properties);

   void updateSlot(ScreenHandler handler, int slot, ItemStack stack);

   void updateCursorStack(ScreenHandler handler, ItemStack stack);

   void updateProperty(ScreenHandler handler, int property, int value);

   TrackedSlot createTrackedSlot();
}
