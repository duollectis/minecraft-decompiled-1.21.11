package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.ShulkerBoxSlot;
import net.minecraft.screen.slot.Slot;

/**
 * Обработчик экрана сундука-шалкера.
 *
 * <p>Управляет сеткой 9×3 (27 слотов) внутри шалкера и стандартным
 * инвентарём игрока. Слоты шалкера используют {@link ShulkerBoxSlot},
 * который запрещает вкладывать другие шалкеры во избежание рекурсии.</p>
 */
public class ShulkerBoxScreenHandler extends ScreenHandler {

	private static final int SHULKER_ROWS = 3;
	private static final int SHULKER_COLS = 9;
	private static final int INVENTORY_SIZE = SHULKER_ROWS * SHULKER_COLS;
	private static final int SLOT_STEP = 18;
	private static final int SHULKER_START_X = 8;
	private static final int SHULKER_START_Y = 18;
	private static final int PLAYER_SLOTS_X = 8;
	private static final int PLAYER_SLOTS_Y = 84;

	private final Inventory inventory;

	public ShulkerBoxScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE));
	}

	public ShulkerBoxScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(ScreenHandlerType.SHULKER_BOX, syncId);
		checkSize(inventory, INVENTORY_SIZE);
		this.inventory = inventory;
		inventory.onOpen(playerInventory.player);

		for (int row = 0; row < SHULKER_ROWS; row++) {
			for (int col = 0; col < SHULKER_COLS; col++) {
				addSlot(new ShulkerBoxSlot(
					inventory,
					col + row * SHULKER_COLS,
					SHULKER_START_X + col * SLOT_STEP,
					SHULKER_START_Y + row * SLOT_STEP
				));
			}
		}

		addPlayerSlots(playerInventory, PLAYER_SLOTS_X, PLAYER_SLOTS_Y);
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

		if (slot < inventory.size()) {
			if (!insertItem(slotStack, inventory.size(), slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!insertItem(slotStack, 0, inventory.size(), false)) {
			return ItemStack.EMPTY;
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		} else {
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
