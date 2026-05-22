package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Обработчик экрана контейнера 3×3 (например, воронка-дроппер).
 * <p>
 * Содержит 9 слотов контейнера и стандартный инвентарь игрока.
 * Расширяется {@link net.minecraft.screen.NautilusScreenHandler} и другими
 * специализированными обработчиками через метод {@link #add3x3Slots}.
 */
public class Generic3x3ContainerScreenHandler extends ScreenHandler {

	private static final int CONTAINER_SIZE = 9;
	private static final int CONTAINER_ROWS = 3;
	private static final int CONTAINER_COLS = 3;
	private static final int SLOT_STEP = 18;
	private static final int INVENTORY_START = 9;
	private static final int INVENTORY_END = 36;
	private static final int HOTBAR_START = 36;
	private static final int HOTBAR_END = 45;

	private final Inventory inventory;

	public Generic3x3ContainerScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(CONTAINER_SIZE));
	}

	public Generic3x3ContainerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(ScreenHandlerType.GENERIC_3X3, syncId);
		checkSize(inventory, CONTAINER_SIZE);
		this.inventory = inventory;
		inventory.onOpen(playerInventory.player);
		add3x3Slots(inventory, 62, 17);
		addPlayerSlots(playerInventory, 8, 84);
	}

	/**
	 * Добавляет сетку 3×3 слотов в указанной позиции.
	 * Метод защищён для переопределения в подклассах.
	 *
	 * @param inventory инвентарь, которому принадлежат слоты
	 * @param x         левая координата первого слота
	 * @param y         верхняя координата первого слота
	 */
	protected void add3x3Slots(Inventory inventory, int x, int y) {
		for (int row = 0; row < CONTAINER_ROWS; row++) {
			for (int col = 0; col < CONTAINER_COLS; col++) {
				int slotIndex = col + row * CONTAINER_COLS;
				addSlot(new Slot(inventory, slotIndex, x + col * SLOT_STEP, y + row * SLOT_STEP));
			}
		}
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

		if (slot < CONTAINER_SIZE) {
			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}
		}
		else if (!insertItem(slotStack, 0, CONTAINER_SIZE, false)) {
			return ItemStack.EMPTY;
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		}
		else {
			sourceSlot.markDirty();
		}

		if (slotStack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, slotStack);

		return original;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		inventory.onClose(player);
	}
}
