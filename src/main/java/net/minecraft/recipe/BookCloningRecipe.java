package net.minecraft.recipe;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Рецепт клонирования написанной книги.
 * <p>
 * Принимает ровно одну написанную книгу (источник) и одну или несколько
 * книг-целей с тегом {@code book_cloning_target}. Результат — стак копий
 * книги-источника в количестве, равном числу книг-целей.
 * Оригинал возвращается как остаток крафта.
 */
public class BookCloningRecipe extends SpecialCraftingRecipe {

	public BookCloningRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getStackCount() < 2) {
			return false;
		}

		boolean hasWrittenBook = false;
		boolean hasTarget = false;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
				if (hasWrittenBook) {
					return false;
				}

				hasWrittenBook = true;
			} else {
				if (!stack.isIn(ItemTags.BOOK_CLONING_TARGET)) {
					return false;
				}

				hasTarget = true;
			}
		}

		return hasWrittenBook && hasTarget;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		int targetCount = 0;
		ItemStack sourceBook = ItemStack.EMPTY;

		for (int slotIndex = 0; slotIndex < input.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);

			if (stack.isEmpty()) {
				continue;
			}

			if (stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
				if (!sourceBook.isEmpty()) {
					return ItemStack.EMPTY;
				}

				sourceBook = stack;
			} else {
				if (!stack.isIn(ItemTags.BOOK_CLONING_TARGET)) {
					return ItemStack.EMPTY;
				}

				targetCount++;
			}
		}

		WrittenBookContentComponent bookContent = sourceBook.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

		if (sourceBook.isEmpty() || targetCount < 1 || bookContent == null) {
			return ItemStack.EMPTY;
		}

		WrittenBookContentComponent copiedContent = bookContent.copy();

		if (copiedContent == null) {
			return ItemStack.EMPTY;
		}

		ItemStack result = sourceBook.copyWithCount(targetCount);
		result.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, copiedContent);
		return result;
	}

	@Override
	public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
		DefaultedList<ItemStack> remainders = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);

		for (int slotIndex = 0; slotIndex < remainders.size(); slotIndex++) {
			ItemStack stack = input.getStackInSlot(slotIndex);
			ItemStack remainder = stack.getItem().getRecipeRemainder();

			if (!remainder.isEmpty()) {
				remainders.set(slotIndex, remainder);
			} else if (stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
				remainders.set(slotIndex, stack.copyWithCount(1));
				break;
			}
		}

		return remainders;
	}

	@Override
	public RecipeSerializer<BookCloningRecipe> getSerializer() {
		return RecipeSerializer.BOOK_CLONING;
	}
}
