package net.minecraft.screen;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Обработчик экрана верстака (3×3 сетка крафта).
 * <p>
 * Управляет слотами крафта, синхронизирует результат с клиентом через
 * {@link ScreenHandlerSlotUpdateS2CPacket} и поддерживает флаг {@code filling}
 * для подавления пересчёта рецепта во время автозаполнения сетки.
 */
public class CraftingScreenHandler extends AbstractCraftingScreenHandler {

	private static final int CRAFTING_GRID_WIDTH = 3;
	private static final int CRAFTING_GRID_HEIGHT = 3;
	public static final int RESULT_ID = 0;
	private static final int INPUT_START = 1;
	private static final int CRAFTING_GRID_SIZE = 9;
	private static final int INPUT_END = 10;
	private static final int INVENTORY_START = 10;
	private static final int INVENTORY_END = 37;
	private static final int HOTBAR_START = 37;
	private static final int HOTBAR_END = 46;

	private final ScreenHandlerContext context;
	private final PlayerEntity player;
	private boolean filling;

	public CraftingScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public CraftingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ScreenHandlerType.CRAFTING, syncId, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT);
		this.context = context;
		this.player = playerInventory.player;
		addResultSlot(this.player, 124, 35);
		addInputSlots(30, 17);
		addPlayerSlots(playerInventory, 8, 84);
	}

	/**
	 * Пересчитывает результат крафта на сервере и отправляет обновление клиенту.
	 * <p>
	 * Метод намеренно {@code protected static}, чтобы его мог вызывать
	 * {@link PlayerScreenHandler} для синхронизации инвентарного крафта 2×2.
	 *
	 * @param handler           обработчик экрана, которому принадлежит слот результата
	 * @param world             серверный мир для поиска рецепта
	 * @param player            игрок, выполняющий крафт
	 * @param craftingInventory инвентарь с ингредиентами
	 * @param resultInventory   инвентарь результата
	 * @param recipe            подсказка рецепта (может быть {@code null})
	 */
	protected static void updateResult(
			ScreenHandler handler,
			ServerWorld world,
			PlayerEntity player,
			RecipeInputInventory craftingInventory,
			CraftingResultInventory resultInventory,
			@Nullable RecipeEntry<CraftingRecipe> recipe
	) {
		CraftingRecipeInput recipeInput = craftingInventory.createRecipeInput();
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

		Optional<RecipeEntry<CraftingRecipe>> match = world.getServer()
				.getRecipeManager()
				.getFirstMatch(RecipeType.CRAFTING, recipeInput, world, recipe);

		ItemStack result = ItemStack.EMPTY;

		if (match.isPresent()) {
			RecipeEntry<CraftingRecipe> recipeEntry = match.get();
			CraftingRecipe craftingRecipe = recipeEntry.value();

			if (resultInventory.shouldCraftRecipe(serverPlayer, recipeEntry)) {
				ItemStack crafted = craftingRecipe.craft(recipeInput, world.getRegistryManager());

				if (crafted.isItemEnabled(world.getEnabledFeatures())) {
					result = crafted;
				}
			}
		}

		resultInventory.setStack(0, result);
		handler.setReceivedStack(0, result);
		serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
				handler.syncId,
				handler.nextRevision(),
				0,
				result
		));
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		if (filling) {
			return;
		}

		context.run((world, pos) -> {
			if (world instanceof ServerWorld serverWorld) {
				updateResult(this, serverWorld, player, craftingInventory, craftingResultInventory, null);
			}
		});
	}

	@Override
	public void onInputSlotFillStart() {
		filling = true;
	}

	@Override
	public void onInputSlotFillFinish(ServerWorld world, RecipeEntry<CraftingRecipe> recipe) {
		filling = false;
		updateResult(this, world, player, craftingInventory, craftingResultInventory, recipe);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		context.run((world, pos) -> dropInventory(player, craftingInventory));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, Blocks.CRAFTING_TABLE);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		Slot sourceSlot = slots.get(slot);

		if (sourceSlot == null || !sourceSlot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStack = sourceSlot.getStack();
		ItemStack original = slotStack.copy();

		if (slot == RESULT_ID) {
			slotStack.getItem().onCraftByPlayer(slotStack, player);

			if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, true)) {
				return ItemStack.EMPTY;
			}

			sourceSlot.onQuickTransfer(slotStack, original);
		}
		else if (slot >= INVENTORY_START && slot < HOTBAR_END) {
			if (!insertItem(slotStack, INPUT_START, INPUT_END, false)) {
				if (slot < INVENTORY_END) {
					if (!insertItem(slotStack, HOTBAR_START, HOTBAR_END, false)) {
						return ItemStack.EMPTY;
					}
				}
				else if (!insertItem(slotStack, INVENTORY_START, INVENTORY_END, false)) {
					return ItemStack.EMPTY;
				}
			}
		}
		else if (!insertItem(slotStack, INVENTORY_START, HOTBAR_END, false)) {
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

		if (slot == RESULT_ID) {
			player.dropItem(slotStack, false);
		}

		return original;
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return slot.inventory != craftingResultInventory && super.canInsertIntoSlot(stack, slot);
	}

	@Override
	public Slot getOutputSlot() {
		return slots.get(RESULT_ID);
	}

	@Override
	public List<Slot> getInputSlots() {
		return slots.subList(INPUT_START, INPUT_END);
	}

	@Override
	public RecipeBookType getCategory() {
		return RecipeBookType.CRAFTING;
	}

	@Override
	protected PlayerEntity getPlayer() {
		return player;
	}
}
