package net.minecraft.screen;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
 * Обработчик экрана камнерезного станка.
 *
 * <p>Управляет одним входным слотом и одним выходным. Список доступных
 * рецептов фильтруется по предмету во входном слоте через
 * {@link CuttingRecipeDisplay.Grouping}. Выбранный рецепт синхронизируется
 * через {@link Property} {@code selectedRecipe}.</p>
 */
public class StonecutterScreenHandler extends ScreenHandler {

	public static final int INPUT_ID = 0;
	public static final int OUTPUT_ID = 1;
	private static final int INVENTORY_START = 2;
	private static final int INVENTORY_END = 29;
	private static final int OUTPUT_START = 29;
	private static final int OUTPUT_END = 38;
	private static final int INPUT_SLOT_X = 20;
	private static final int INPUT_SLOT_Y = 33;
	private static final int OUTPUT_SLOT_X = 143;
	private static final int OUTPUT_SLOT_Y = 33;
	private static final int PLAYER_SLOTS_X = 8;
	private static final int PLAYER_SLOTS_Y = 84;
	private static final int NO_RECIPE_SELECTED = -1;

	private final ScreenHandlerContext context;
	final Property selectedRecipe = Property.create();
	private final World world;
	private CuttingRecipeDisplay.Grouping<StonecuttingRecipe> availableRecipes = CuttingRecipeDisplay.Grouping.empty();
	private ItemStack inputStack = ItemStack.EMPTY;
	long lastTakeTime;
	final Slot inputSlot;
	final Slot outputSlot;
	Runnable contentsChangedListener = () -> {};

	public final Inventory input = new SimpleInventory(1) {
		@Override
		public void markDirty() {
			super.markDirty();
			StonecutterScreenHandler.this.onContentChanged(this);
			StonecutterScreenHandler.this.contentsChangedListener.run();
		}
	};

	final CraftingResultInventory output = new CraftingResultInventory();

	public StonecutterScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public StonecutterScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ScreenHandlerType.STONECUTTER, syncId);
		this.context = context;
		world = playerInventory.player.getEntityWorld();
		inputSlot = addSlot(new Slot(input, INPUT_ID, INPUT_SLOT_X, INPUT_SLOT_Y));
		outputSlot = addSlot(new Slot(output, OUTPUT_ID, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				stack.onCraftByPlayer(player, stack.getCount());
				StonecutterScreenHandler.this.output.unlockLastRecipe(player, getInputStacks());

				ItemStack consumed = StonecutterScreenHandler.this.inputSlot.takeStack(1);

				if (consumed.isEmpty() == false) {
					StonecutterScreenHandler.this.populateResult(StonecutterScreenHandler.this.selectedRecipe.get());
				}

				context.run((world, pos) -> {
					long currentTime = world.getTime();

					if (StonecutterScreenHandler.this.lastTakeTime != currentTime) {
						world.playSound(
							null,
							pos,
							SoundEvents.UI_STONECUTTER_TAKE_RESULT,
							SoundCategory.BLOCKS,
							1.0F,
							1.0F
						);
						StonecutterScreenHandler.this.lastTakeTime = currentTime;
					}
				});

				super.onTakeItem(player, stack);
			}

			private List<ItemStack> getInputStacks() {
				return List.of(StonecutterScreenHandler.this.inputSlot.getStack());
			}
		});

		addPlayerSlots(playerInventory, PLAYER_SLOTS_X, PLAYER_SLOTS_Y);
		addProperty(selectedRecipe);
	}

	public int getSelectedRecipe() {
		return selectedRecipe.get();
	}

	public CuttingRecipeDisplay.Grouping<StonecuttingRecipe> getAvailableRecipes() {
		return availableRecipes;
	}

	public int getAvailableRecipeCount() {
		return availableRecipes.size();
	}

	public boolean canCraft() {
		return inputSlot.hasStack() && availableRecipes.isEmpty() == false;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.STONECUTTER);
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (selectedRecipe.get() == id) {
			return false;
		}

		if (isInBounds(id)) {
			selectedRecipe.set(id);
			populateResult(id);
		}

		return true;
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		ItemStack currentInput = inputSlot.getStack();

		if (currentInput.isOf(inputStack.getItem())) {
			return;
		}

		inputStack = currentInput.copy();
		updateInput(currentInput);
	}

	/**
	 * Обновляет список доступных рецептов при смене входного предмета.
	 *
	 * <p>Сбрасывает выбранный рецепт и очищает выходной слот без
	 * уведомления слушателей (через {@code setStackNoCallbacks}).</p>
	 */
	void populateResult(int selectedId) {
		Optional<RecipeEntry<StonecuttingRecipe>> optional;

		if (availableRecipes.isEmpty() == false && isInBounds(selectedId)) {
			CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> groupEntry =
				availableRecipes.entries().get(selectedId);
			optional = groupEntry.recipe().recipe();
		} else {
			optional = Optional.empty();
		}

		optional.ifPresentOrElse(
			recipe -> {
				output.setLastRecipe((RecipeEntry<?>) recipe);
				outputSlot.setStackNoCallbacks(recipe.value().craft(
					new SingleStackRecipeInput(input.getStack(INPUT_ID)),
					world.getRegistryManager()
				));
			},
			() -> {
				outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
				output.setLastRecipe(null);
			}
		);

		sendContentUpdates();
	}

	@Override
	public ScreenHandlerType<?> getType() {
		return ScreenHandlerType.STONECUTTER;
	}

	public void setContentsChangedListener(Runnable contentsChangedListener) {
		this.contentsChangedListener = contentsChangedListener;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != output && super.canInsertIntoSlot(stack, slot);
	}

	/**
	 * Быстрое перемещение предмета (Shift+Click) с учётом зон инвентаря.
	 *
	 * <p>Результат (слот 1) перемещается в инвентарь игрока с вызовом
	 * {@code onCraftByPlayer}. Предметы, подходящие для рецептов, уходят
	 * во входной слот (0). Остальные предметы перемещаются между
	 * инвентарём и хотбаром.</p>
	 */
	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		Item item = slotStack.getItem();
		ItemStack original = slotStack.copy();

		if (slot == OUTPUT_ID) {
			item.onCraftByPlayer(slotStack, player);

			if (!insertItem(slotStack, INVENTORY_START, OUTPUT_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(slotStack, original);
		} else if (slot == INPUT_ID) {
			if (!insertItem(slotStack, INVENTORY_START, OUTPUT_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (world.getRecipeManager().getStonecutterRecipes().contains(slotStack)) {
			if (!insertItem(slotStack, INPUT_ID, INPUT_ID + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
			if (!insertItem(slotStack, OUTPUT_START, OUTPUT_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (slot >= OUTPUT_START && slot < OUTPUT_END) {
			if (!insertItem(slotStack, INVENTORY_START, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (slotStack.isEmpty()) {
			sourceSlot.setStack(ItemStack.EMPTY);
		}

		sourceSlot.markDirty();

		if (slotStack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTakeItem(player, slotStack);

		if (slot == OUTPUT_ID) {
			player.dropItem(slotStack, false);
		}

		sendContentUpdates();

		return original;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		output.removeStack(OUTPUT_ID);
		context.run((world, pos) -> dropInventory(player, input));
	}

	private void updateInput(ItemStack stack) {
		selectedRecipe.set(NO_RECIPE_SELECTED);
		outputSlot.setStackNoCallbacks(ItemStack.EMPTY);

		availableRecipes = stack.isEmpty()
			? CuttingRecipeDisplay.Grouping.empty()
			: world.getRecipeManager().getStonecutterRecipes().filter(stack);
	}

	private boolean isInBounds(int id) {
		return id >= 0 && id < availableRecipes.size();
	}
}
