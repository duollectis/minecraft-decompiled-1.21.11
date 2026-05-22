package net.minecraft.recipe.input;

import net.minecraft.item.ItemStack;

/**
 * Входные данные рецепта кузнечного стола: шаблон, основа и добавка.
 */
public record SmithingRecipeInput(ItemStack template, ItemStack base, ItemStack addition) implements RecipeInput {

	@Override
	public ItemStack getStackInSlot(int slot) {
		return switch (slot) {
			case 0 -> template;
			case 1 -> base;
			case 2 -> addition;
			default -> throw new IllegalArgumentException("Recipe does not contain slot " + slot);
		};
	}

	@Override
	public int size() {
		return 3;
	}

	@Override
	public boolean isEmpty() {
		return template.isEmpty() && base.isEmpty() && addition.isEmpty();
	}
}
