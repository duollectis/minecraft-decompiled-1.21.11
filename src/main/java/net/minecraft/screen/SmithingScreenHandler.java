package net.minecraft.screen;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
 * Обработчик экрана кузнечного стола.
 *
 * <p>Управляет тремя входными слотами (шаблон, снаряжение, материал)
 * и одним выходным. Проверяет допустимость ингредиентов через
 * {@link RecipePropertySet} и отображает индикатор «неверный рецепт»
 * через синхронизируемое свойство {@code invalidRecipe}.</p>
 */
public class SmithingScreenHandler extends ForgingScreenHandler {

	public static final int TEMPLATE_ID = 0;
	public static final int EQUIPMENT_ID = 1;
	public static final int MATERIAL_ID = 2;
	public static final int OUTPUT_ID = 3;
	public static final int TEMPLATE_X = 8;
	public static final int EQUIPMENT_X = 26;
	public static final int MATERIAL_X = 44;
	private static final int OUTPUT_X = 98;
	public static final int SLOT_Y = 48;
	private static final int INVALID_RECIPE_FALSE = 0;
	private static final int INVALID_RECIPE_TRUE = 1;

	private final World world;
	private final RecipePropertySet basePropertySet;
	private final RecipePropertySet templatePropertySet;
	private final RecipePropertySet additionPropertySet;
	private final Property invalidRecipe = Property.create();

	public SmithingScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public SmithingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		this(syncId, playerInventory, context, playerInventory.player.getEntityWorld());
	}

	private SmithingScreenHandler(
		int syncId,
		PlayerInventory playerInventory,
		ScreenHandlerContext context,
		World world
	) {
		super(
			ScreenHandlerType.SMITHING,
			syncId,
			playerInventory,
			context,
			createForgingSlotsManager(world.getRecipeManager())
		);
		this.world = world;
		basePropertySet = world.getRecipeManager().getPropertySet(RecipePropertySet.SMITHING_BASE);
		templatePropertySet = world.getRecipeManager().getPropertySet(RecipePropertySet.SMITHING_TEMPLATE);
		additionPropertySet = world.getRecipeManager().getPropertySet(RecipePropertySet.SMITHING_ADDITION);
		addProperty(invalidRecipe).set(INVALID_RECIPE_FALSE);
	}

	private static ForgingSlotsManager createForgingSlotsManager(RecipeManager recipeManager) {
		RecipePropertySet baseSet = recipeManager.getPropertySet(RecipePropertySet.SMITHING_BASE);
		RecipePropertySet templateSet = recipeManager.getPropertySet(RecipePropertySet.SMITHING_TEMPLATE);
		RecipePropertySet additionSet = recipeManager.getPropertySet(RecipePropertySet.SMITHING_ADDITION);

		return ForgingSlotsManager.builder()
			.input(TEMPLATE_ID, TEMPLATE_X, SLOT_Y, templateSet::canUse)
			.input(EQUIPMENT_ID, EQUIPMENT_X, SLOT_Y, baseSet::canUse)
			.input(MATERIAL_ID, MATERIAL_X, SLOT_Y, additionSet::canUse)
			.output(OUTPUT_ID, OUTPUT_X, SLOT_Y)
			.build();
	}

	@Override
	protected boolean canUse(BlockState state) {
		return state.isOf(Blocks.SMITHING_TABLE);
	}

	@Override
	protected void onTakeOutput(PlayerEntity player, ItemStack stack) {
		stack.onCraftByPlayer(player, stack.getCount());
		output.unlockLastRecipe(player, getInputStacks());
		decrementStack(TEMPLATE_ID);
		decrementStack(EQUIPMENT_ID);
		decrementStack(MATERIAL_ID);
		context.run((world, pos) -> world.syncWorldEvent(1044, pos, 0));
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);

		if (world instanceof ServerWorld == false) {
			return;
		}

		boolean allInputsPresent = getSlot(TEMPLATE_ID).hasStack()
			&& getSlot(EQUIPMENT_ID).hasStack()
			&& getSlot(MATERIAL_ID).hasStack()
			&& !getSlot(getResultSlotIndex()).hasStack();

		invalidRecipe.set(allInputsPresent ? INVALID_RECIPE_TRUE : INVALID_RECIPE_FALSE);
	}

	/**
	 * Ищет подходящий рецепт кузнечного стола и обновляет выходной слот.
	 *
	 * <p>На клиентской стороне всегда очищает выход, так как рецепты
	 * вычисляются только на сервере.</p>
	 */
	@Override
	public void updateResult() {
		SmithingRecipeInput recipeInput = createRecipeInput();

		Optional<RecipeEntry<SmithingRecipe>> optional = world instanceof ServerWorld serverWorld
			? serverWorld.getRecipeManager().getFirstMatch(RecipeType.SMITHING, recipeInput, serverWorld)
			: Optional.empty();

		optional.ifPresentOrElse(
			recipe -> {
				ItemStack result = recipe.value().craft(recipeInput, world.getRegistryManager());
				output.setLastRecipe((RecipeEntry<?>) recipe);
				output.setStack(0, result);
			},
			() -> {
				output.setLastRecipe(null);
				output.setStack(0, ItemStack.EMPTY);
			}
		);
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != output && super.canInsertIntoSlot(stack, slot);
	}

	@Override
	public boolean isValidIngredient(ItemStack stack) {
		if (templatePropertySet.canUse(stack) && !getSlot(TEMPLATE_ID).hasStack()) {
			return true;
		}

		if (basePropertySet.canUse(stack) && !getSlot(EQUIPMENT_ID).hasStack()) {
			return true;
		}

		return additionPropertySet.canUse(stack) && !getSlot(MATERIAL_ID).hasStack();
	}

	public boolean hasInvalidRecipe() {
		return invalidRecipe.get() > INVALID_RECIPE_FALSE;
	}

	private List<ItemStack> getInputStacks() {
		return List.of(input.getStack(TEMPLATE_ID), input.getStack(EQUIPMENT_ID), input.getStack(MATERIAL_ID));
	}

	private SmithingRecipeInput createRecipeInput() {
		return new SmithingRecipeInput(
			input.getStack(TEMPLATE_ID),
			input.getStack(EQUIPMENT_ID),
			input.getStack(MATERIAL_ID)
		);
	}

	private void decrementStack(int slot) {
		ItemStack stack = input.getStack(slot);

		if (stack.isEmpty()) {
			return;
		}

		stack.decrement(1);
		input.setStack(slot, stack);
	}
}
