package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.DyeColor;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Рецепт копирования узора баннера на чистый баннер того же цвета.
 * <p>
 * Принимает ровно два баннера одного цвета: один с узором (источник)
 * и один без узора (цель). Количество слоёв узора не должно превышать
 * {@link #MAX_BANNER_PATTERN_LAYERS}.
 */
public class BannerDuplicateRecipe extends SpecialCraftingRecipe {

	private static final int MAX_BANNER_PATTERN_LAYERS = 6;

	public BannerDuplicateRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() != 2) {
			return false;
		}

		DyeColor bannerColor = null;
		boolean hasPatternedBanner = false;
		boolean hasBlankBanner = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (!(stack.getItem() instanceof BannerItem bannerItem)) {
				return false;
			}

			if (bannerColor == null) {
				bannerColor = bannerItem.getColor();
			} else if (bannerColor != bannerItem.getColor()) {
				return false;
			}

			int patternCount = stack
				.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT)
				.layers()
				.size();

			if (patternCount > 6) {
				return false;
			}

			if (patternCount > 0) {
				if (hasPatternedBanner) {
					return false;
				}

				hasPatternedBanner = true;
			} else {
				if (hasBlankBanner) {
					return false;
				}

				hasBlankBanner = true;
			}
		}

		return hasPatternedBanner && hasBlankBanner;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			int patternCount = stack
				.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT)
				.layers()
				.size();

			if (patternCount > 0 && patternCount <= 6) {
				return stack.copyWithCount(1);
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
		DefaultedList<ItemStack> remainders = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);

		for (int slotIndex = 0; slotIndex < remainders.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			ItemStack remainder = stack.getItem().getRecipeRemainder();

			if (!remainder.isEmpty()) {
				remainders.set(slotIndex, remainder);
			} else if (!stack
				.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT)
				.layers()
				.isEmpty()
			) {
				remainders.set(slotIndex, stack.copyWithCount(1));
			}
		}

		return remainders;
	}

	@Override
	public RecipeSerializer<BannerDuplicateRecipe> getSerializer() {
		return RecipeSerializer.BANNER_DUPLICATE;
	}
}
