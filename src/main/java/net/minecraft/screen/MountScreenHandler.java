package net.minecraft.screen;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Базовый обработчик экрана верхового существа (лошадь, лама, наутилус и т.д.).
 * <p>
 * Управляет инвентарём существа и инвентарём игрока.
 * Слоты 0 и 1 зарезервированы для седла и брони соответственно.
 * Начиная со слота 2 располагается инвентарь существа.
 */
public abstract class MountScreenHandler extends ScreenHandler {

	protected static final int SADDLE_SLOT_INDEX = 0;
	protected static final int ARMOR_SLOT_INDEX = 1;
	protected static final int INVENTORY_START_INDEX = 2;
	protected static final int MIN_INVENTORY_COLUMNS = 3;

	protected final Inventory inventory;
	public final LivingEntity mount;

	protected MountScreenHandler(
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory,
			LivingEntity mount
	) {
		super(null, syncId);
		this.inventory = inventory;
		this.mount = mount;
		inventory.onOpen(playerInventory.player);
	}

	/**
	 * Проверяет, отличается ли переданный инвентарь от текущего инвентаря существа.
	 * Используется в {@link #canUse} для обнаружения смены инвентаря (например, при смерти).
	 */
	protected abstract boolean areInventoriesDifferent(Inventory inventory);

	@Override
	public boolean canUse(PlayerEntity player) {
		return !areInventoriesDifferent(inventory)
				&& inventory.canPlayerUse(player)
				&& mount.isAlive()
				&& player.canInteractWithEntity(mount, 4.0);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		inventory.onClose(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();
		int mountInventoryEnd = INVENTORY_START_INDEX + inventory.size();

		if (slot < mountInventoryEnd) {
			if (!insertItem(slotStack, mountInventoryEnd, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		}
		else if (getSlot(ARMOR_SLOT_INDEX).canInsert(slotStack) && !getSlot(ARMOR_SLOT_INDEX).hasStack()) {
			if (!insertItem(slotStack, ARMOR_SLOT_INDEX, ARMOR_SLOT_INDEX + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (getSlot(SADDLE_SLOT_INDEX).canInsert(slotStack) && !getSlot(SADDLE_SLOT_INDEX).hasStack()) {
			if (!insertItem(slotStack, SADDLE_SLOT_INDEX, SADDLE_SLOT_INDEX + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		else if (inventory.size() == 0 || !insertItem(slotStack, INVENTORY_START_INDEX, mountInventoryEnd, false)) {
			int playerInventoryEnd = mountInventoryEnd + 27;
			int hotbarEnd = playerInventoryEnd + 9;

			if (slot >= playerInventoryEnd && slot < hotbarEnd) {
				if (!insertItem(slotStack, mountInventoryEnd, playerInventoryEnd, false)) {
					return ItemStack.EMPTY;
				}
			}
			else if (slot >= mountInventoryEnd && slot < playerInventoryEnd) {
				if (!insertItem(slotStack, playerInventoryEnd, hotbarEnd, false)) {
					return ItemStack.EMPTY;
				}
			}
			else if (!insertItem(slotStack, playerInventoryEnd, playerInventoryEnd, false)) {
				return ItemStack.EMPTY;
			}

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

	public static int getSlotCount(int columns) {
		return columns * MIN_INVENTORY_COLUMNS;
	}
}
