package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Рецепт клонирования заполненной карты на пустые карты.
 * <p>
 * Принимает ровно одну заполненную карту (с компонентом {@code MAP_ID})
 * и одну или несколько пустых карт ({@link Items#MAP}).
 * Результат — стак копий заполненной карты в количестве (пустые карты + 1).
 */
public class MapCloningRecipe extends SpecialCraftingRecipe {

	public MapCloningRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() < 2) {
			return false;
		}

		boolean hasFilledMap = false;
		boolean hasEmptyMap = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.contains(DataComponentTypes.MAP_ID)) {
				if (hasFilledMap) {
					return false;
				}

				hasFilledMap = true;
			} else {
				if (!stack.isOf(Items.MAP)) {
					return false;
				}

				hasEmptyMap = true;
			}
		}

		return hasFilledMap && hasEmptyMap;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		int emptyMapCount = 0;
		ItemStack filledMap = ItemStack.EMPTY;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.contains(DataComponentTypes.MAP_ID)) {
				if (!filledMap.isEmpty()) {
					return ItemStack.EMPTY;
				}

				filledMap = stack;
			} else {
				if (!stack.isOf(Items.MAP)) {
					return ItemStack.EMPTY;
				}

				emptyMapCount++;
			}
		}

		return !filledMap.isEmpty() && emptyMapCount >= 1
			? filledMap.copyWithCount(emptyMapCount + 1)
			: ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<MapCloningRecipe> getSerializer() {
		return RecipeSerializer.MAP_CLONING;
	}
}
