package net.minecraft.screen.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Выходной слот стола крафта.
 * <p>
 * При взятии результата: уменьшает ингредиенты в сетке крафта, возвращает
 * остатки рецепта (например, вёдра), начисляет статистику крафта и разблокирует
 * рецепт в книге рецептов игрока.
 */
public class CraftingResultSlot extends Slot {

	private final RecipeInputInventory input;
	private final PlayerEntity player;
	private int amount;

	public CraftingResultSlot(
			PlayerEntity player,
			RecipeInputInventory input,
			Inventory inventory,
			int index,
			int x,
			int y
	) {
		super(inventory, index, x, y);
		this.player = player;
		this.input = input;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack takeStack(int amount) {
		if (hasStack()) {
			this.amount += Math.min(amount, getStack().getCount());
		}

		return super.takeStack(amount);
	}

	@Override
	protected void onCrafted(ItemStack stack, int amount) {
		this.amount += amount;
		onCrafted(stack);
	}

	@Override
	public void onTake(int amount) {
		this.amount += amount;
	}

	@Override
	protected void onCrafted(ItemStack stack) {
		if (amount > 0) {
			stack.onCraftByPlayer(player, amount);
		}

		if (inventory instanceof RecipeUnlocker recipeUnlocker) {
			recipeUnlocker.unlockLastRecipe(player, input.getHeldStacks());
		}

		amount = 0;
	}

	/**
	 * При взятии результата уменьшает ингредиенты в сетке и раскладывает
	 * остатки рецепта обратно в слоты или в инвентарь игрока.
	 */
	@Override
	public void onTakeItem(PlayerEntity player, ItemStack stack) {
		onCrafted(stack);

		CraftingRecipeInput.Positioned positioned = input.createPositionedRecipeInput();
		CraftingRecipeInput recipeInput = positioned.input();
		int offsetLeft = positioned.left();
		int offsetTop = positioned.top();
		DefaultedList<ItemStack> remainders = getRecipeRemainders(recipeInput, player.getEntityWorld());

		for (int row = 0; row < recipeInput.getHeight(); row++) {
			for (int col = 0; col < recipeInput.getWidth(); col++) {
				int slotIndex = col + offsetLeft + (row + offsetTop) * input.getWidth();
				ItemStack slotStack = input.getStack(slotIndex);
				ItemStack remainder = remainders.get(col + row * recipeInput.getWidth());

				if (!slotStack.isEmpty()) {
					input.removeStack(slotIndex, 1);
					slotStack = input.getStack(slotIndex);
				}

				if (remainder.isEmpty()) {
					continue;
				}

				if (slotStack.isEmpty()) {
					input.setStack(slotIndex, remainder);
				} else if (ItemStack.areItemsAndComponentsEqual(slotStack, remainder)) {
					remainder.increment(slotStack.getCount());
					input.setStack(slotIndex, remainder);
				} else if (!player.getInventory().insertStack(remainder)) {
					player.dropItem(remainder, false);
				}
			}
		}
	}

	@Override
	public boolean disablesDynamicDisplay() {
		return true;
	}

	private static DefaultedList<ItemStack> copyInput(CraftingRecipeInput recipeInput) {
		DefaultedList<ItemStack> copy = DefaultedList.ofSize(recipeInput.size(), ItemStack.EMPTY);

		for (int index = 0; index < copy.size(); index++) {
			copy.set(index, recipeInput.getStackInSlot(index));
		}

		return copy;
	}

	private DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput recipeInput, World world) {
		return world instanceof ServerWorld serverWorld
				? serverWorld.getRecipeManager()
						.getFirstMatch(RecipeType.CRAFTING, recipeInput, serverWorld)
						.map(recipe -> recipe.value().getRecipeRemainders(recipeInput))
						.orElseGet(() -> copyInput(recipeInput))
				: CraftingRecipe.collectRecipeRemainders(recipeInput);
	}
}
