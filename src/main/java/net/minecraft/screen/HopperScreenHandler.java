package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Обработчик экрана воронки.
 * <p>
 * Содержит 5 слотов воронки в ряд и стандартный инвентарь игрока.
 */
public class HopperScreenHandler extends ScreenHandler {

	public static final int SLOT_COUNT = 5;
	private static final int SLOT_START_X = 44;
	private static final int SLOT_Y = 20;
	private static final int SLOT_STEP = 18;

	private final Inventory inventory;

	public HopperScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(SLOT_COUNT));
	}

	public HopperScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(ScreenHandlerType.HOPPER, syncId);
		checkSize(inventory, SLOT_COUNT);
		this.inventory = inventory;
		inventory.onOpen(playerInventory.player);

		for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
			addSlot(new Slot(inventory, slotIndex, SLOT_START_X + slotIndex * SLOT_STEP, SLOT_Y));
		}

		addPlayerSlots(playerInventory, 8, 51);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();
		int hopperSize = inventory.size();

		if (slot < hopperSize) {
			if (!insertItem(slotStack, hopperSize, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		}
		else if (!insertItem(slotStack, 0, hopperSize, false)) {
			return ItemStack.EMPTY;
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		}
		else {
			sourceSlot.markDirty();
		}

		return original;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		inventory.onClose(player);
	}
}
