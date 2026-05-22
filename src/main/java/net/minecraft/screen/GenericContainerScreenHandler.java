package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Универсальный обработчик экрана контейнера с сеткой 9×N (от 1 до 6 рядов).
 * <p>
 * Используется для сундуков, бочек и других блоков-хранилищ.
 * Количество рядов определяется при создании и влияет на расположение
 * слотов инвентаря игрока.
 */
public class GenericContainerScreenHandler extends ScreenHandler {

	private static final int COLUMNS = 9;
	private static final int SLOT_STEP = 18;
	private static final int PLAYER_INVENTORY_TOP_OFFSET = 13;

	private final Inventory inventory;
	private final int rows;

	private GenericContainerScreenHandler(
			ScreenHandlerType<?> type,
			int syncId,
			PlayerInventory playerInventory,
			int rows
	) {
		this(type, syncId, playerInventory, new SimpleInventory(COLUMNS * rows), rows);
	}

	public static GenericContainerScreenHandler createGeneric9x1(int syncId, PlayerInventory playerInventory) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X1, syncId, playerInventory, 1);
	}

	public static GenericContainerScreenHandler createGeneric9x2(int syncId, PlayerInventory playerInventory) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X2, syncId, playerInventory, 2);
	}

	/**
	 * Создаёт обработчик для контейнера 9×3 с пустым инвентарём (клиентская сторона).
	 */
	public static GenericContainerScreenHandler createGeneric9x3(int syncId, PlayerInventory playerInventory) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, 3);
	}

	public static GenericContainerScreenHandler createGeneric9x4(int syncId, PlayerInventory playerInventory) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, 4);
	}

	public static GenericContainerScreenHandler createGeneric9x5(int syncId, PlayerInventory playerInventory) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X5, syncId, playerInventory, 5);
	}

	/**
	 * Создаёт обработчик для контейнера 9×6 с пустым инвентарём (клиентская сторона).
	 */
	public static GenericContainerScreenHandler createGeneric9x6(int syncId, PlayerInventory playerInventory) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, 6);
	}

	/**
	 * Создаёт обработчик для контейнера 9×3 с реальным инвентарём блока (серверная сторона).
	 */
	public static GenericContainerScreenHandler createGeneric9x3(
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory
	) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
	}

	/**
	 * Создаёт обработчик для контейнера 9×6 с реальным инвентарём блока (серверная сторона).
	 */
	public static GenericContainerScreenHandler createGeneric9x6(
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory
	) {
		return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, 6);
	}

	public GenericContainerScreenHandler(
			ScreenHandlerType<?> type,
			int syncId,
			PlayerInventory playerInventory,
			Inventory inventory,
			int rows
	) {
		super(type, syncId);
		checkSize(inventory, rows * COLUMNS);
		this.inventory = inventory;
		this.rows = rows;
		inventory.onOpen(playerInventory.player);
		addInventorySlots(inventory, 8, SLOT_STEP);
		int playerSlotsTop = SLOT_STEP + rows * SLOT_STEP + PLAYER_INVENTORY_TOP_OFFSET;
		addPlayerSlots(playerInventory, 8, playerSlotsTop);
	}

	private void addInventorySlots(Inventory inventory, int left, int top) {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < COLUMNS; col++) {
				addSlot(new Slot(inventory, col + row * COLUMNS, left + col * SLOT_STEP, top + row * SLOT_STEP));
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
		int containerSize = rows * COLUMNS;

		if (slot < containerSize) {
			if (!insertItem(slotStack, containerSize, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		}
		else if (!insertItem(slotStack, 0, containerSize, false)) {
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

	public Inventory getInventory() {
		return inventory;
	}

	public int getRows() {
		return rows;
	}
}
