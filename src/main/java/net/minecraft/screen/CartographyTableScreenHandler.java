package net.minecraft.screen;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Обработчик экрана стола картографа.
 * <p>
 * Управляет двумя входными слотами (карта и материал) и одним слотом результата.
 * Поддерживает операции: масштабирование карты (бумага), блокировка (стекло), копирование (пустая карта).
 */
public class CartographyTableScreenHandler extends ScreenHandler {

	public static final int MAP_SLOT_INDEX = 0;
	public static final int MATERIAL_SLOT_INDEX = 1;
	public static final int RESULT_SLOT_INDEX = 2;

	private static final int INVENTORY_START = 3;
	private static final int INVENTORY_END = 30;
	private static final int HOTBAR_START = 30;
	private static final int HOTBAR_END = 39;

	private final ScreenHandlerContext context;
	long lastTakeResultTime;

	public final Inventory inventory = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			CartographyTableScreenHandler.this.onContentChanged(this);
			super.markDirty();
		}
	};

	private final CraftingResultInventory resultInventory = new CraftingResultInventory() {
		@Override
		public void markDirty() {
			CartographyTableScreenHandler.this.onContentChanged(this);
			super.markDirty();
		}
	};

	public CartographyTableScreenHandler(int syncId, PlayerInventory inventory) {
		this(syncId, inventory, ScreenHandlerContext.EMPTY);
	}

	public CartographyTableScreenHandler(int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
		super(ScreenHandlerType.CARTOGRAPHY_TABLE, syncId);
		this.context = context;
		addSlot(new Slot(this.inventory, MAP_SLOT_INDEX, 15, 15) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.contains(DataComponentTypes.MAP_ID);
			}
		});
		addSlot(new Slot(this.inventory, MATERIAL_SLOT_INDEX, 15, 52) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(Items.PAPER) || stack.isOf(Items.MAP) || stack.isOf(Items.GLASS_PANE);
			}
		});
		addSlot(new Slot(resultInventory, RESULT_SLOT_INDEX, 145, HOTBAR_END) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				CartographyTableScreenHandler.this.slots.get(MAP_SLOT_INDEX).takeStack(1);
				CartographyTableScreenHandler.this.slots.get(MATERIAL_SLOT_INDEX).takeStack(1);
				stack.getItem().onCraftByPlayer(stack, player);
				context.run((world, pos) -> {
					long currentTime = world.getTime();
					if (CartographyTableScreenHandler.this.lastTakeResultTime != currentTime) {
						world.playSound(
								null,
								pos,
								SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
								SoundCategory.BLOCKS,
								1.0F,
								1.0F
						);
						CartographyTableScreenHandler.this.lastTakeResultTime = currentTime;
					}
				});
				super.onTakeItem(player, stack);
			}
		});
		addPlayerSlots(inventory, 8, 84);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.CARTOGRAPHY_TABLE);
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		ItemStack map = this.inventory.getStack(MAP_SLOT_INDEX);
		ItemStack material = this.inventory.getStack(MATERIAL_SLOT_INDEX);
		ItemStack currentResult = resultInventory.getStack(RESULT_SLOT_INDEX);

		if (currentResult.isEmpty() || map.isEmpty() || material.isEmpty()) {
			resultInventory.removeStack(RESULT_SLOT_INDEX);
			return;
		}

		updateResult(map, material, currentResult);
	}

	/**
	 * Вычисляет результат операции картографа в зависимости от типа материала:
	 * бумага — масштабирование, стекло — блокировка, пустая карта — копирование.
	 */
	private void updateResult(ItemStack map, ItemStack material, ItemStack oldResult) {
		context.run((world, pos) -> {
			MapState mapState = FilledMapItem.getMapState(map, world);
			if (mapState == null) {
				return;
			}

			ItemStack result;
			if (material.isOf(Items.PAPER) && !mapState.locked && mapState.scale < 4) {
				result = map.copyWithCount(1);
				result.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.SCALE);
				sendContentUpdates();
			} else if (material.isOf(Items.GLASS_PANE) && !mapState.locked) {
				result = map.copyWithCount(1);
				result.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.LOCK);
				sendContentUpdates();
			} else if (material.isOf(Items.MAP)) {
				result = map.copyWithCount(2);
				sendContentUpdates();
			} else {
				resultInventory.removeStack(RESULT_SLOT_INDEX);
				sendContentUpdates();
				return;
			}

			if (!ItemStack.areEqual(result, oldResult)) {
				resultInventory.setStack(RESULT_SLOT_INDEX, result);
				sendContentUpdates();
			}
		});
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != resultInventory && super.canInsertIntoSlot(stack, slot);
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

		if (slot == RESULT_SLOT_INDEX) {
			stack.getItem().onCraftByPlayer(stack, player);
			if (!insertItem(stack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(stack, original);
		} else if (slot == MAP_SLOT_INDEX || slot == MATERIAL_SLOT_INDEX) {
			if (!insertItem(stack, INVENTORY_START, HOTBAR_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (stack.contains(DataComponentTypes.MAP_ID)) {
				if (!insertItem(stack, MAP_SLOT_INDEX, MAP_SLOT_INDEX + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (stack.isOf(Items.PAPER) || stack.isOf(Items.MAP) || stack.isOf(Items.GLASS_PANE)) {
				if (!insertItem(stack, MATERIAL_SLOT_INDEX, MATERIAL_SLOT_INDEX + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
				if (!insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slot >= HOTBAR_START && slot < HOTBAR_END) {
				if (!insertItem(stack, INVENTORY_START, INVENTORY_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		}

		if (stack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		}

		sourceSlot.markDirty();

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, stack);
		sendContentUpdates();
		return original;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		resultInventory.removeStack(RESULT_SLOT_INDEX);
		context.run((world, pos) -> dropInventory(player, inventory));
	}
}
