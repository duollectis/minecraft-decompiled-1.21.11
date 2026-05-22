package net.minecraft.recipe;

import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт покраски брони красителями.
 * <p>
 * Принимает ровно один предмет с тегом {@code dyeable} и один или несколько
 * {@link DyeItem}. Цвета красителей смешиваются через {@link DyedColorComponent#setColor}.
 */
public class ArmorDyeRecipe extends SpecialCraftingRecipe {

	public ArmorDyeRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() < 2) {
			return false;
		}

		boolean hasDyeable = false;
		boolean hasDye = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.isIn(ItemTags.DYEABLE)) {
				if (hasDyeable) {
					return false;
				}

				hasDyeable = true;
			} else {
				if (!(stack.getItem() instanceof DyeItem)) {
					return false;
				}

				hasDye = true;
			}
		}

		return hasDyeable && hasDye;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		List<DyeItem> dyes = new ArrayList<>();
		ItemStack dyeable = ItemStack.EMPTY;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.isIn(ItemTags.DYEABLE)) {
				if (!dyeable.isEmpty()) {
					return ItemStack.EMPTY;
				}

				dyeable = stack.copy();
			} else {
				if (!(stack.getItem() instanceof DyeItem dyeItem)) {
					return ItemStack.EMPTY;
				}

				dyes.add(dyeItem);
			}
		}

		return !dyeable.isEmpty() && !dyes.isEmpty()
			? DyedColorComponent.setColor(dyeable, dyes)
			: ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<ArmorDyeRecipe> getSerializer() {
		return RecipeSerializer.ARMOR_DYE;
	}
}
