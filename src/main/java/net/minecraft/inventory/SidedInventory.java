package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

public interface SidedInventory extends Inventory {
   int[] getAvailableSlots(Direction side);

   boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir);

   boolean canExtract(int slot, ItemStack stack, Direction dir);
}
