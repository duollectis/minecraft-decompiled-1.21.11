package net.minecraft.recipe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Рецепт добавления эффекта угасания к звезде фейерверка.
 * <p>
 * Принимает ровно одну звезду фейерверка и один или несколько красителей.
 * Цвета угасания добавляются к существующему компоненту взрыва через
 * {@link FireworkExplosionComponent#withFadeColors}.
 */
public class FireworkStarFadeRecipe extends SpecialCraftingRecipe {

	private static final Ingredient INPUT_STAR = Ingredient.ofItem(Items.FIREWORK_STAR);

	public FireworkStarFadeRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() < 2) {
			return false;
		}

		boolean hasStar = false;
		boolean hasDye = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.getItem() instanceof DyeItem) {
				hasDye = true;
			} else {
				if (!INPUT_STAR.test(stack)) {
					return false;
				}

				if (hasStar) {
					return false;
				}

				hasStar = true;
			}
		}

		return hasStar && hasDye;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		IntList fadeColors = new IntArrayList();
		ItemStack starCopy = null;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.getItem() instanceof DyeItem dyeItem) {
				fadeColors.add(dyeItem.getColor().getFireworkColor());
			} else if (INPUT_STAR.test(stack)) {
				starCopy = stack.copyWithCount(1);
			}
		}

		if (starCopy == null || fadeColors.isEmpty()) {
			return ItemStack.EMPTY;
		}

		starCopy.apply(
			DataComponentTypes.FIREWORK_EXPLOSION,
			FireworkExplosionComponent.DEFAULT,
			fadeColors,
			FireworkExplosionComponent::withFadeColors
		);
		return starCopy;
	}

	@Override
	public RecipeSerializer<FireworkStarFadeRecipe> getSerializer() {
		return RecipeSerializer.FIREWORK_STAR_FADE;
	}
}
