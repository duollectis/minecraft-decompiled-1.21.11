package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Блок-сущность диспенсера. Хранит 9 предметов в сетке 3×3 и предоставляет
 * метод случайного выбора непустого слота для выстрела.
 */
public class DispenserBlockEntity extends LootableContainerBlockEntity {

	public static final int INVENTORY_SIZE = 9;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.dispenser");
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

	protected DispenserBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
		super(blockEntityType, blockPos, blockState);
	}

	public DispenserBlockEntity(BlockPos pos, BlockState state) {
		this(BlockEntityType.DISPENSER, pos, state);
	}

	@Override
	public int size() {
		return INVENTORY_SIZE;
	}

	/**
	 * Выбирает случайный непустой слот с равномерным распределением вероятности
	 * (алгоритм резервуарной выборки). Возвращает -1, если все слоты пусты.
	 */
	public int chooseNonEmptySlot(Random random) {
		generateLoot(null);
		int selectedSlot = -1;
		int candidateCount = 1;

		for (int slot = 0; slot < inventory.size(); slot++) {
			if (!inventory.get(slot).isEmpty() && random.nextInt(candidateCount++) == 0) {
				selectedSlot = slot;
			}
		}

		return selectedSlot;
	}

	/**
	 * Добавляет предмет в первый подходящий слот (пустой или с таким же предметом).
	 * Возвращает остаток стака, который не удалось разместить.
	 */
	public ItemStack addToFirstFreeSlot(ItemStack stack) {
		int maxCount = getMaxCount(stack);

		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack slotStack = inventory.get(slot);
			if (slotStack.isEmpty() || ItemStack.areItemsAndComponentsEqual(stack, slotStack)) {
				int transferCount = Math.min(stack.getCount(), maxCount - slotStack.getCount());
				if (transferCount > 0) {
					if (slotStack.isEmpty()) {
						setStack(slot, stack.split(transferCount));
					} else {
						stack.decrement(transferCount);
						slotStack.increment(transferCount);
					}
				}

				if (stack.isEmpty()) {
					break;
				}
			}
		}

		return stack;
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
		if (!readLootTable(view)) {
			Inventories.readData(view, inventory);
		}
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		if (!writeLootTable(view)) {
			Inventories.writeData(view, inventory);
		}
	}

	@Override
	protected DefaultedList<ItemStack> getHeldStacks() {
		return inventory;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> newInventory) {
		inventory = newInventory;
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new Generic3x3ContainerScreenHandler(syncId, playerInventory, this);
	}
}
