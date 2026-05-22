package net.minecraft.block.entity;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.BlockState;
import net.minecraft.block.CrafterBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Блок-сущность крафтера. Хранит 9 слотов крафтинговой сетки, состояние
 * отключённых слотов и флаг триггера. Поддерживает автоматический крафт
 * при получении редстоун-сигнала.
 */
public class CrafterBlockEntity extends LootableContainerBlockEntity implements RecipeInputInventory {

	public static final int GRID_WIDTH = 3;
	public static final int GRID_HEIGHT = 3;
	public static final int GRID_SIZE = 9;
	public static final int SLOT_DISABLED = 1;
	public static final int SLOT_ENABLED = 0;
	public static final int TRIGGERED_PROPERTY = 9;
	public static final int PROPERTIES_COUNT = 10;
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("container.crafter");

	private DefaultedList<ItemStack> inputStacks = DefaultedList.ofSize(GRID_SIZE, ItemStack.EMPTY);
	private int craftingTicksRemaining;
	protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
		private final int[] disabledSlots = new int[GRID_SIZE];
		private int triggered;

		@Override
		public int get(int index) {
			return index == TRIGGERED_PROPERTY ? triggered : disabledSlots[index];
		}

		@Override
		public void set(int index, int value) {
			if (index == TRIGGERED_PROPERTY) {
				triggered = value;
			} else {
				disabledSlots[index] = value;
			}
		}

		@Override
		public int size() {
			return PROPERTIES_COUNT;
		}
	};

	public CrafterBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.CRAFTER, pos, state);
	}

	@Override
	protected Text getContainerName() {
		return CONTAINER_NAME_TEXT;
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new CrafterScreenHandler(syncId, playerInventory, this, propertyDelegate);
	}

	public void setSlotEnabled(int slot, boolean enabled) {
		if (canToggleSlot(slot)) {
			propertyDelegate.set(slot, enabled ? SLOT_ENABLED : SLOT_DISABLED);
			markDirty();
		}
	}

	public boolean isSlotDisabled(int slot) {
		return slot >= 0 && slot < GRID_SIZE
				? propertyDelegate.get(slot) == SLOT_DISABLED
				: false;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		if (propertyDelegate.get(slot) == SLOT_DISABLED) {
			return false;
		}

		ItemStack existing = inputStacks.get(slot);
		int existingCount = existing.getCount();

		if (existingCount >= existing.getMaxCount()) {
			return false;
		}

		return existing.isEmpty() ? true : !betterSlotExists(existingCount, existing, slot);
	}

	private boolean betterSlotExists(int count, ItemStack stack, int slot) {
		for (int nextSlot = slot + 1; nextSlot < GRID_SIZE; nextSlot++) {
			if (isSlotDisabled(nextSlot)) {
				continue;
			}

			ItemStack candidate = getStack(nextSlot);
			if (candidate.isEmpty()
					|| candidate.getCount() < count && ItemStack.areItemsAndComponentsEqual(candidate, stack)
			) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		craftingTicksRemaining = view.getInt("crafting_ticks_remaining", 0);
		inputStacks = DefaultedList.ofSize(size(), ItemStack.EMPTY);
		if (!readLootTable(view)) {
			Inventories.readData(view, inputStacks);
		}

		for (int slot = 0; slot < GRID_SIZE; slot++) {
			propertyDelegate.set(slot, 0);
		}

		view.getOptionalIntArray("disabled_slots").ifPresent(slots -> {
			for (int slot : slots) {
				if (canToggleSlot(slot)) {
					propertyDelegate.set(slot, SLOT_DISABLED);
				}
			}
		});
		propertyDelegate.set(TRIGGERED_PROPERTY, view.getInt("triggered", 0));
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putInt("crafting_ticks_remaining", craftingTicksRemaining);
		if (!writeLootTable(view)) {
			Inventories.writeData(view, inputStacks);
		}

		putDisabledSlots(view);
		putTriggered(view);
	}

	@Override
	public int size() {
		return GRID_SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : inputStacks) {
			if (!stack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return inputStacks.get(slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (isSlotDisabled(slot)) {
			setSlotEnabled(slot, true);
		}

		super.setStack(slot, stack);
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return Inventory.canPlayerUse(this, player);
	}

	@Override
	public DefaultedList<ItemStack> getHeldStacks() {
		return inputStacks;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
		inputStacks = inventory;
	}

	@Override
	public int getWidth() {
		return GRID_WIDTH;
	}

	@Override
	public int getHeight() {
		return GRID_HEIGHT;
	}

	@Override
	public void provideRecipeInputs(RecipeFinder finder) {
		for (ItemStack stack : inputStacks) {
			finder.addInputIfUsable(stack);
		}
	}

	private void putDisabledSlots(WriteView view) {
		IntList disabledList = new IntArrayList();

		for (int slot = 0; slot < GRID_SIZE; slot++) {
			if (isSlotDisabled(slot)) {
				disabledList.add(slot);
			}
		}

		view.putIntArray("disabled_slots", disabledList.toIntArray());
	}

	private void putTriggered(WriteView view) {
		view.putInt("triggered", propertyDelegate.get(TRIGGERED_PROPERTY));
	}

	public void setTriggered(boolean triggered) {
		propertyDelegate.set(TRIGGERED_PROPERTY, triggered ? 1 : 0);
	}

	@VisibleForTesting
	public boolean isTriggered() {
		return propertyDelegate.get(TRIGGERED_PROPERTY) == 1;
	}

	public static void tickCrafting(World world, BlockPos pos, BlockState state, CrafterBlockEntity blockEntity) {
		int remaining = blockEntity.craftingTicksRemaining - 1;
		if (remaining >= 0) {
			blockEntity.craftingTicksRemaining = remaining;
			if (remaining == 0) {
				world.setBlockState(pos, state.with(CrafterBlock.CRAFTING, false), 3);
			}
		}
	}

	public void setCraftingTicksRemaining(int craftingTicksRemaining) {
		this.craftingTicksRemaining = craftingTicksRemaining;
	}

	public int getComparatorOutput() {
		int filledSlots = 0;

		for (int slot = 0; slot < size(); slot++) {
			if (!getStack(slot).isEmpty() || isSlotDisabled(slot)) {
				filledSlots++;
			}
		}

		return filledSlots;
	}

	private boolean canToggleSlot(int slot) {
		return slot > -1 && slot < GRID_SIZE && inputStacks.get(slot).isEmpty();
	}
}
