package net.minecraft.screen;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.screen.slot.Slot;
import org.jspecify.annotations.Nullable;

/**
 * Базовый обработчик экрана ковки (наковальня, кузнечный стол).
 * <p>
 * Управляет входными слотами и слотом результата, делегируя конкретную логику
 * подклассам через {@link #updateResult()}, {@link #canTakeOutput} и {@link #onTakeOutput}.
 */
public abstract class ForgingScreenHandler extends ScreenHandler {

	private static final int PLAYER_INVENTORY_SIZE = 27;
	private static final int HOTBAR_SIZE = 9;

	protected final ScreenHandlerContext context;
	protected final PlayerEntity player;
	protected final Inventory input;
	protected final CraftingResultInventory output = new CraftingResultInventory() {
		@Override
		public void markDirty() {
			ForgingScreenHandler.this.onContentChanged(this);
		}
	};

	private final int resultSlotIndex;

	protected boolean canTakeOutput(PlayerEntity player, boolean present) {
		return true;
	}

	protected abstract void onTakeOutput(PlayerEntity player, ItemStack stack);

	protected abstract boolean canUse(BlockState state);

	public ForgingScreenHandler(
			@Nullable ScreenHandlerType<?> type,
			int syncId,
			PlayerInventory playerInventory,
			ScreenHandlerContext context,
			ForgingSlotsManager forgingSlotsManager
	) {
		super(type, syncId);
		this.context = context;
		this.player = playerInventory.player;
		this.input = createInputInventory(forgingSlotsManager.getInputSlotCount());
		this.resultSlotIndex = forgingSlotsManager.getResultSlotIndex();
		addInputSlots(forgingSlotsManager);
		addResultSlot(forgingSlotsManager);
		addPlayerSlots(playerInventory, 8, 84);
	}

	private void addInputSlots(ForgingSlotsManager forgingSlotsManager) {
		for (final ForgingSlotsManager.ForgingSlot forgingSlot : forgingSlotsManager.getInputSlots()) {
			addSlot(new Slot(input, forgingSlot.slotId(), forgingSlot.x(), forgingSlot.y()) {
				@Override
				public boolean canInsert(ItemStack stack) {
					return forgingSlot.mayPlace().test(stack);
				}
			});
		}
	}

	private void addResultSlot(ForgingSlotsManager forgingSlotsManager) {
		ForgingSlotsManager.ForgingSlot resultSlot = forgingSlotsManager.getResultSlot();
		addSlot(new Slot(output, resultSlot.slotId(), resultSlot.x(), resultSlot.y()) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public boolean canTakeItems(PlayerEntity playerEntity) {
				return ForgingScreenHandler.this.canTakeOutput(playerEntity, hasStack());
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				ForgingScreenHandler.this.onTakeOutput(player, stack);
			}
		});
	}

	public abstract void updateResult();

	private SimpleInventory createInputInventory(int size) {
		return new SimpleInventory(size) {
			@Override
			public void markDirty() {
				super.markDirty();
				ForgingScreenHandler.this.onContentChanged(this);
			}
		};
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		if (inventory == input) {
			updateResult();
		}
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		context.run((world, pos) -> dropInventory(player, input));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return context.get(
				(world, pos) -> canUse(world.getBlockState(pos))
						&& player.canInteractWithBlockAt(pos, 4.0),
				true
		);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		ItemStack original = ItemStack.EMPTY;
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = sourceSlot.getStack();
		original = stack.copy();

		int inventoryStart = getPlayerInventoryStartIndex();
		int hotbarEnd = getPlayerHotbarEndIndex();

		if (slot == getResultSlotIndex()) {
			if (!insertItem(stack, inventoryStart, hotbarEnd, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(stack, original);
		} else if (slot >= 0 && slot < getResultSlotIndex()) {
			if (!insertItem(stack, inventoryStart, hotbarEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else if (isValidIngredient(stack)
				&& slot >= inventoryStart
				&& slot < hotbarEnd) {
			if (!insertItem(stack, 0, getResultSlotIndex(), false)) {
				return ItemStack.EMPTY;
			}
		} else if (slot >= inventoryStart && slot < getPlayerInventoryEndIndex()) {
			if (!insertItem(stack, getPlayerHotbarStartIndex(), hotbarEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slot >= getPlayerHotbarStartIndex()
				&& slot < hotbarEnd
				&& !insertItem(stack, inventoryStart, getPlayerInventoryEndIndex(), false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		} else {
			sourceSlot.markDirty();
		}

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, stack);
		return original;
	}

	protected boolean isValidIngredient(ItemStack stack) {
		return true;
	}

	public int getResultSlotIndex() {
		return resultSlotIndex;
	}

	private int getPlayerInventoryStartIndex() {
		return getResultSlotIndex() + 1;
	}

	private int getPlayerInventoryEndIndex() {
		return getPlayerInventoryStartIndex() + PLAYER_INVENTORY_SIZE;
	}

	private int getPlayerHotbarStartIndex() {
		return getPlayerInventoryEndIndex();
	}

	private int getPlayerHotbarEndIndex() {
		return getPlayerHotbarStartIndex() + HOTBAR_SIZE;
	}
}
