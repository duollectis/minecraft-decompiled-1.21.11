package net.minecraft.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.InputSlotFiller;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Базовый обработчик экрана крафтинга.
 * <p>
 * Управляет сеткой входных слотов и слотом результата, а также реализует логику
 * автозаполнения рецептов через {@link InputSlotFiller}.
 */
public abstract class AbstractCraftingScreenHandler extends AbstractRecipeScreenHandler {

	private final int width;
	private final int height;
	protected final RecipeInputInventory craftingInventory;
	protected final CraftingResultInventory craftingResultInventory = new CraftingResultInventory();

	public AbstractCraftingScreenHandler(ScreenHandlerType<?> type, int syncId, int width, int height) {
		super(type, syncId);
		this.width = width;
		this.height = height;
		craftingInventory = new CraftingInventory(this, width, height);
	}

	protected Slot addResultSlot(PlayerEntity player, int x, int y) {
		return addSlot(new CraftingResultSlot(
				player,
				craftingInventory,
				craftingResultInventory,
				0,
				x,
				y
		));
	}

	protected void addInputSlots(int x, int y) {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				addSlot(new Slot(craftingInventory, col + row * width, x + col * 18, y + row * 18));
			}
		}
	}

	/**
	 * Заполняет входные слоты предметами для выбранного рецепта крафтинга.
	 * Использует {@link InputSlotFiller} для поиска предметов в инвентаре игрока.
	 */
	@Override
	public PostFillAction fillInputSlots(
			boolean craftAll, boolean creative, RecipeEntry<?> recipe, ServerWorld world, PlayerInventory inventory
	) {
		RecipeEntry<CraftingRecipe> craftingRecipe = (RecipeEntry<CraftingRecipe>) recipe;
		onInputSlotFillStart();

		PostFillAction result;
		try {
			List<Slot> inputSlots = getInputSlots();
			result = InputSlotFiller.fill(
					new InputSlotFiller.Handler<CraftingRecipe>() {
						@Override
						public void populateRecipeFinder(RecipeFinder finder) {
							AbstractCraftingScreenHandler.this.populateRecipeFinder(finder);
						}

						@Override
						public void clear() {
							AbstractCraftingScreenHandler.this.craftingResultInventory.clear();
							AbstractCraftingScreenHandler.this.craftingInventory.clear();
						}

						@Override
						public boolean matches(RecipeEntry<CraftingRecipe> entry) {
							return entry.value()
									.matches(
											AbstractCraftingScreenHandler.this.craftingInventory.createRecipeInput(),
											AbstractCraftingScreenHandler.this.getPlayer().getEntityWorld()
									);
						}
					},
					width,
					height,
					inputSlots,
					inputSlots,
					inventory,
					craftingRecipe,
					craftAll,
					creative
			);
		} finally {
			onInputSlotFillFinish(world, craftingRecipe);
		}

		return result;
	}

	protected void onInputSlotFillStart() {
	}

	protected void onInputSlotFillFinish(ServerWorld world, RecipeEntry<CraftingRecipe> recipe) {
	}

	public abstract Slot getOutputSlot();

	public abstract List<Slot> getInputSlots();

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	protected abstract PlayerEntity getPlayer();

	@Override
	public void populateRecipeFinder(RecipeFinder finder) {
		craftingInventory.provideRecipeInputs(finder);
	}
}
