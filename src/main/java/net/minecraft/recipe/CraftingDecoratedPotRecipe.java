package net.minecraft.recipe;

import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.Sherds;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.world.World;

/**
 * Рецепт крафта декорированного горшка из четырёх черепков (или кирпичей).
 * <p>
 * Требует сетку 3×3 с ровно 4 предметами в позициях «ромба»:
 * верх (1,0), лево (0,1), право (2,1), низ (1,2).
 * Все четыре предмета должны иметь тег {@code decorated_pot_ingredients}.
 */
public class CraftingDecoratedPotRecipe extends SpecialCraftingRecipe {

	public CraftingDecoratedPotRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	private static ItemStack getBack(CraftingRecipeInput input) {
		return input.getStackInSlot(1, 0);
	}

	private static ItemStack getLeft(CraftingRecipeInput input) {
		return input.getStackInSlot(0, 1);
	}

	private static ItemStack getRight(CraftingRecipeInput input) {
		return input.getStackInSlot(2, 1);
	}

	private static ItemStack getFront(CraftingRecipeInput input) {
		return input.getStackInSlot(1, 2);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getWidth() != 3 || input.getHeight() != 3 || input.getStackCount() != 4) {
			return false;
		}

		return getBack(input).isIn(ItemTags.DECORATED_POT_INGREDIENTS)
			&& getLeft(input).isIn(ItemTags.DECORATED_POT_INGREDIENTS)
			&& getRight(input).isIn(ItemTags.DECORATED_POT_INGREDIENTS)
			&& getFront(input).isIn(ItemTags.DECORATED_POT_INGREDIENTS);
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		Sherds sherds = new Sherds(
			getBack(input).getItem(),
			getLeft(input).getItem(),
			getRight(input).getItem(),
			getFront(input).getItem()
		);
		return DecoratedPotBlockEntity.getStackWith(sherds);
	}

	@Override
	public RecipeSerializer<CraftingDecoratedPotRecipe> getSerializer() {
		return RecipeSerializer.CRAFTING_DECORATED_POT;
	}
}
