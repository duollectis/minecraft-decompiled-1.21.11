package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Рецепт нанесения узора баннера на щит.
 * <p>
 * Принимает ровно два предмета: один баннер и один чистый щит
 * (без компонента {@code BANNER_PATTERNS}). Узор и цвет баннера
 * переносятся на щит через компоненты {@code BANNER_PATTERNS} и {@code BASE_COLOR}.
 */
public class ShieldDecorationRecipe extends SpecialCraftingRecipe {

	public ShieldDecorationRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() != 2) {
			return false;
		}

		boolean hasBanner = false;
		boolean hasCleanShield = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.getItem() instanceof BannerItem) {
				if (hasBanner) {
					return false;
				}

				hasBanner = true;
			} else {
				if (!stack.isOf(Items.SHIELD)) {
					return false;
				}

				if (hasCleanShield) {
					return false;
				}

				BannerPatternsComponent existingPatterns = stack.getOrDefault(
					DataComponentTypes.BANNER_PATTERNS,
					BannerPatternsComponent.DEFAULT
				);

				if (!existingPatterns.layers().isEmpty()) {
					return false;
				}

				hasCleanShield = true;
			}
		}

		return hasCleanShield && hasBanner;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		ItemStack banner = ItemStack.EMPTY;
		ItemStack shield = ItemStack.EMPTY;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.getItem() instanceof BannerItem) {
				banner = stack;
			} else if (stack.isOf(Items.SHIELD)) {
				shield = stack.copy();
			}
		}

		if (shield.isEmpty()) {
			return ItemStack.EMPTY;
		}

		shield.set(DataComponentTypes.BANNER_PATTERNS, banner.get(DataComponentTypes.BANNER_PATTERNS));
		shield.set(DataComponentTypes.BASE_COLOR, ((BannerItem) banner.getItem()).getColor());
		return shield;
	}

	@Override
	public RecipeSerializer<ShieldDecorationRecipe> getSerializer() {
		return RecipeSerializer.SHIELD_DECORATION;
	}
}
