package net.minecraft.recipe;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Заполняет слоты сетки крафта предметами из инвентаря игрока для выбранного рецепта.
 * Поддерживает режим «скрафтить всё» (craftAll) и обычный одиночный крафт.
 * Перед заполнением проверяет, можно ли вернуть текущие предметы из сетки в инвентарь.
 */
public class InputSlotFiller<R extends Recipe<?>> {

	private static final int NO_SLOT = -1;

	private final PlayerInventory inventory;
	private final InputSlotFiller.Handler<R> handler;
	private final boolean craftAll;
	private final int width;
	private final int height;
	private final List<Slot> inputSlots;
	private final List<Slot> slotsToReturn;

	/**
	 * Создаёт заполнитель и немедленно выполняет заполнение сетки.
	 * Если инвентарь не в режиме creative и предметы нельзя вернуть — возвращает {@code PLACE_GHOST_RECIPE}.
	 */
	public static <I extends RecipeInput, R extends Recipe<I>> AbstractRecipeScreenHandler.PostFillAction fill(
		InputSlotFiller.Handler<R> handler,
		int width,
		int height,
		List<Slot> inputSlots,
		List<Slot> slotsToReturn,
		PlayerInventory inventory,
		RecipeEntry<R> recipe,
		boolean craftAll,
		boolean creative
	) {
		InputSlotFiller<R> filler = new InputSlotFiller<>(handler, inventory, craftAll, width, height, inputSlots, slotsToReturn);

		if (!creative && !filler.canReturnInputs()) {
			return AbstractRecipeScreenHandler.PostFillAction.NOTHING;
		}

		RecipeFinder recipeFinder = new RecipeFinder();
		inventory.populateRecipeFinder(recipeFinder);
		handler.populateRecipeFinder(recipeFinder);

		return filler.tryFill(recipe, recipeFinder);
	}

	private InputSlotFiller(
		InputSlotFiller.Handler<R> handler,
		PlayerInventory inventory,
		boolean craftAll,
		int width,
		int height,
		List<Slot> inputSlots,
		List<Slot> slotsToReturn
	) {
		this.handler = handler;
		this.inventory = inventory;
		this.craftAll = craftAll;
		this.width = width;
		this.height = height;
		this.inputSlots = inputSlots;
		this.slotsToReturn = slotsToReturn;
	}

	private AbstractRecipeScreenHandler.PostFillAction tryFill(RecipeEntry<R> recipe, RecipeFinder finder) {
		if (finder.isCraftable(recipe.value(), null)) {
			fill(recipe, finder);
			inventory.markDirty();
			return AbstractRecipeScreenHandler.PostFillAction.NOTHING;
		}

		returnInputs();
		inventory.markDirty();

		return AbstractRecipeScreenHandler.PostFillAction.PLACE_GHOST_RECIPE;
	}

	private void returnInputs() {
		for (Slot slot : slotsToReturn) {
			ItemStack stack = slot.getStack().copy();
			inventory.offer(stack, false);
			slot.setStackNoCallbacks(stack);
		}

		handler.clear();
	}

	private void fill(RecipeEntry<R> recipe, RecipeFinder finder) {
		boolean alreadyMatches = handler.matches(recipe);
		int maxCrafts = finder.countCrafts(recipe.value(), null);

		if (alreadyMatches) {
			for (Slot slot : inputSlots) {
				ItemStack stack = slot.getStack();

				if (!stack.isEmpty() && Math.min(maxCrafts, stack.getMaxCount()) < stack.getCount() + 1) {
					return;
				}
			}
		}

		int craftAmount = calculateCraftAmount(maxCrafts, alreadyMatches);
		List<RegistryEntry<Item>> consumedItems = new ArrayList<>();

		if (finder.isCraftable(recipe.value(), craftAmount, consumedItems::add)) {
			int clampedAmount = clampToMaxCount(craftAmount, consumedItems);

			if (clampedAmount != craftAmount) {
				consumedItems.clear();

				if (!finder.isCraftable(recipe.value(), clampedAmount, consumedItems::add)) {
					return;
				}
			}

			returnInputs();

			RecipeGridAligner.alignRecipeToGrid(
				width,
				height,
				recipe.value(),
				recipe.value().getIngredientPlacement().getPlacementSlots(),
				(slotIngredientIndex, index, x, y) -> {
					if (slotIngredientIndex == NO_SLOT) {
						return;
					}

					Slot targetSlot = inputSlots.get(index);
					RegistryEntry<Item> item = consumedItems.get(slotIngredientIndex);
					int remaining = clampedAmount;

					while (remaining > 0) {
						remaining = fillInputSlot(targetSlot, item, remaining);

						if (remaining == NO_SLOT) {
							return;
						}
					}
				}
			);
		}
	}

	private static int clampToMaxCount(int count, List<RegistryEntry<Item>> entries) {
		for (RegistryEntry<Item> entry : entries) {
			count = Math.min(count, entry.value().getMaxCount());
		}

		return count;
	}

	private int calculateCraftAmount(int maxCrafts, boolean alreadyMatches) {
		if (craftAll) {
			return maxCrafts;
		}

		if (alreadyMatches) {
			int minCount = Integer.MAX_VALUE;

			for (Slot slot : inputSlots) {
				ItemStack stack = slot.getStack();

				if (!stack.isEmpty() && minCount > stack.getCount()) {
					minCount = stack.getCount();
				}
			}

			return minCount == Integer.MAX_VALUE ? 1 : minCount + 1;
		}

		return 1;
	}

	private int fillInputSlot(Slot slot, RegistryEntry<Item> item, int count) {
		ItemStack existing = slot.getStack();
		int inventorySlot = inventory.getMatchingSlot(item, existing);

		if (inventorySlot == NO_SLOT) {
			return NO_SLOT;
		}

		ItemStack inventoryStack = inventory.getStack(inventorySlot);
		ItemStack taken = count < inventoryStack.getCount()
			? inventory.removeStack(inventorySlot, count)
			: inventory.removeStack(inventorySlot);

		int takenCount = taken.getCount();

		if (existing.isEmpty()) {
			slot.setStackNoCallbacks(taken);
		} else {
			existing.increment(takenCount);
		}

		return count - takenCount;
	}

	/**
	 * Проверяет, можно ли вернуть все предметы из сетки крафта в инвентарь.
	 * Симулирует возврат без реального перемещения предметов.
	 */
	private boolean canReturnInputs() {
		List<ItemStack> pendingReturn = Lists.newArrayList();
		int freeSlots = getFreeInventorySlots();

		for (Slot slot : inputSlots) {
			ItemStack stack = slot.getStack().copy();

			if (stack.isEmpty()) {
				continue;
			}

			int existingSlot = inventory.getOccupiedSlotWithRoomForStack(stack);

			if (existingSlot == NO_SLOT && pendingReturn.size() <= freeSlots) {
				for (ItemStack pending : pendingReturn) {
					if (ItemStack.areItemsEqual(pending, stack)
						&& pending.getCount() != pending.getMaxCount()
						&& pending.getCount() + stack.getCount() <= pending.getMaxCount()
					) {
						pending.increment(stack.getCount());
						stack.setCount(0);
						break;
					}
				}

				if (!stack.isEmpty()) {
					if (pendingReturn.size() >= freeSlots) {
						return false;
					}

					pendingReturn.add(stack);
				}
			} else if (existingSlot == NO_SLOT) {
				return false;
			}
		}

		return true;
	}

	private int getFreeInventorySlots() {
		int count = 0;

		for (ItemStack stack : inventory.getMainStacks()) {
			if (stack.isEmpty()) {
				count++;
			}
		}

		return count;
	}

	public interface Handler<T extends Recipe<?>> {

		void populateRecipeFinder(RecipeFinder finder);

		void clear();

		boolean matches(RecipeEntry<T> entry);
	}
}
