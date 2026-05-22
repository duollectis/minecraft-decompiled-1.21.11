package net.minecraft.screen;

import net.minecraft.block.CrafterBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.slot.CrafterInputSlot;
import net.minecraft.screen.slot.CrafterOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Обработчик экрана блока-крафтера (автоматический крафтер).
 * <p>
 * Управляет сеткой 3×3 входных слотов с возможностью отключения отдельных ячеек,
 * слотом результата и синхронизацией состояния через {@link PropertyDelegate}.
 * Слоты 0–8 — входные, слот 9 — результат (добавляется после слотов игрока).
 */
public class CrafterScreenHandler extends ScreenHandler implements ScreenHandlerListener {

	protected static final int CRAFTER_GRID_SIZE = 9;

	private static final int GRID_ROWS = 3;
	private static final int GRID_COLS = 3;
	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final int TOTAL_SLOT_COUNT = 45;
	private static final int PROP_TRIGGERED = 9;
	private static final int PROP_COUNT = 10;
	private static final int GRID_START_X = 26;
	private static final int GRID_START_Y = 17;
	private static final int GRID_STEP = 18;
	private static final int OUTPUT_X = 134;
	private static final int OUTPUT_Y = 35;

	private final CraftingResultInventory resultInventory = new CraftingResultInventory();
	private final PropertyDelegate propertyDelegate;
	private final PlayerEntity player;
	private final RecipeInputInventory inputInventory;

	public CrafterScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(ScreenHandlerType.CRAFTER_3X3, syncId);
		player = playerInventory.player;
		propertyDelegate = new ArrayPropertyDelegate(PROP_COUNT);
		inputInventory = new CraftingInventory(this, GRID_COLS, GRID_ROWS);
		addSlots(playerInventory);
	}

	public CrafterScreenHandler(
			int syncId,
			PlayerInventory playerInventory,
			RecipeInputInventory inputInventory,
			PropertyDelegate propertyDelegate
	) {
		super(ScreenHandlerType.CRAFTER_3X3, syncId);
		player = playerInventory.player;
		this.propertyDelegate = propertyDelegate;
		this.inputInventory = inputInventory;
		checkSize(inputInventory, CRAFTER_GRID_SIZE);
		inputInventory.onOpen(playerInventory.player);
		addSlots(playerInventory);
		addListener(this);
	}

	private void addSlots(PlayerInventory playerInventory) {
		for (int row = 0; row < GRID_ROWS; row++) {
			for (int col = 0; col < GRID_COLS; col++) {
				int slotIndex = col + row * GRID_COLS;
				addSlot(new CrafterInputSlot(
						inputInventory,
						slotIndex,
						GRID_START_X + col * GRID_STEP,
						GRID_START_Y + row * GRID_STEP,
						this
				));
			}
		}

		addPlayerSlots(playerInventory, 8, 84);
		addSlot(new CrafterOutputSlot(resultInventory, 0, OUTPUT_X, OUTPUT_Y));
		addProperties(propertyDelegate);
		updateResult();
	}

	public void setSlotEnabled(int slot, boolean enabled) {
		CrafterInputSlot crafterSlot = (CrafterInputSlot) getSlot(slot);
		propertyDelegate.set(crafterSlot.id, enabled ? 0 : 1);
		sendContentUpdates();
	}

	public boolean isSlotDisabled(int slot) {
		return slot >= 0 && slot < CRAFTER_GRID_SIZE && propertyDelegate.get(slot) == 1;
	}

	public boolean isTriggered() {
		return propertyDelegate.get(PROP_TRIGGERED) == 1;
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

		if (slot < CRAFTER_GRID_SIZE) {
			if (!insertItem(stack, CRAFTER_GRID_SIZE, TOTAL_SLOT_COUNT, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!insertItem(stack, 0, CRAFTER_GRID_SIZE, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			sourceSlot.setStackNoCallbacks(ItemStack.EMPTY);
		} else {
			sourceSlot.markDirty();
		}

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, stack);
		return original;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inputInventory.canPlayerUse(player);
	}

	/**
	 * Пересчитывает результат крафта на основе текущего содержимого сетки.
	 * Выполняется только на сервере — клиент получает результат через синхронизацию.
	 */
	private void updateResult() {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}

		ServerWorld serverWorld = serverPlayer.getEntityWorld();
		CraftingRecipeInput recipeInput = inputInventory.createRecipeInput();
		ItemStack result = CrafterBlock.getCraftingRecipe(serverWorld, recipeInput)
				.map(entry -> entry.value().craft(recipeInput, serverWorld.getRegistryManager()))
				.orElse(ItemStack.EMPTY);

		resultInventory.setStack(0, result);
	}

	public Inventory getInputInventory() {
		return inputInventory;
	}

	@Override
	public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
		updateResult();
	}

	@Override
	public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
	}
}
