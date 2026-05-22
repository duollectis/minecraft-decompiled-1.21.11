package net.minecraft.client.gui.screen.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.recipebook.RecipeBookType;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay;
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;

import java.util.List;

/**
 * Виджет книги рецептов для верстаков и инвентарного крафта.
 * Поддерживает как фигурные, так и бесформенные рецепты крафта.
 */
@Environment(EnvType.CLIENT)
public class CraftingRecipeBookWidget extends RecipeBookWidget<AbstractCraftingScreenHandler> {

	private static final ButtonTextures FILTER_BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/filter_enabled"),
			Identifier.ofVanilla("recipe_book/filter_disabled"),
			Identifier.ofVanilla("recipe_book/filter_enabled_highlighted"),
			Identifier.ofVanilla("recipe_book/filter_disabled_highlighted")
	);
	private static final Text TOGGLE_CRAFTABLE_TEXT =
			Text.translatable("gui.recipebook.toggleRecipes.craftable");
	private static final List<RecipeBookWidget.Tab> TABS = List.of(
			new RecipeBookWidget.Tab(RecipeBookType.CRAFTING),
			new RecipeBookWidget.Tab(Items.IRON_AXE, Items.GOLDEN_SWORD, RecipeBookCategories.CRAFTING_EQUIPMENT),
			new RecipeBookWidget.Tab(Items.BRICKS, RecipeBookCategories.CRAFTING_BUILDING_BLOCKS),
			new RecipeBookWidget.Tab(Items.LAVA_BUCKET, Items.APPLE, RecipeBookCategories.CRAFTING_MISC),
			new RecipeBookWidget.Tab(Items.REDSTONE, RecipeBookCategories.CRAFTING_REDSTONE)
	);

	public CraftingRecipeBookWidget(AbstractCraftingScreenHandler screenHandler) {
		super(screenHandler, TABS);
	}

	@Override
	protected boolean isCraftingSlot(Slot slot) {
		return craftingScreenHandler.getOutputSlot() == slot
				|| craftingScreenHandler.getInputSlots().contains(slot);
	}

	private boolean canDisplay(RecipeDisplay display) {
		int gridWidth = craftingScreenHandler.getWidth();
		int gridHeight = craftingScreenHandler.getHeight();

		return switch (display) {
			case ShapedCraftingRecipeDisplay shaped ->
					gridWidth >= shaped.width() && gridHeight >= shaped.height();
			case ShapelessCraftingRecipeDisplay shapeless ->
					gridWidth * gridHeight >= shapeless.ingredients().size();
			default -> false;
		};
	}

	@Override
	protected void showGhostRecipe(GhostRecipe ghostRecipe, RecipeDisplay display, ContextParameterMap context) {
		ghostRecipe.addResults(craftingScreenHandler.getOutputSlot(), context, display.result());

		switch (display) {
			case ShapedCraftingRecipeDisplay shaped: {
				List<Slot> inputSlots = craftingScreenHandler.getInputSlots();
				RecipeGridAligner.alignRecipeToGrid(
						craftingScreenHandler.getWidth(),
						craftingScreenHandler.getHeight(),
						shaped.width(),
						shaped.height(),
						shaped.ingredients(),
						(slotDisplay, index, x, y) -> ghostRecipe.addInputs(inputSlots.get(index), context, slotDisplay)
				);
				break;
			}
			case ShapelessCraftingRecipeDisplay shapeless: {
				List<Slot> inputSlots = craftingScreenHandler.getInputSlots();
				int count = Math.min(shapeless.ingredients().size(), inputSlots.size());

				for (int index = 0; index < count; index++) {
					ghostRecipe.addInputs(
							inputSlots.get(index),
							context,
							shapeless.ingredients().get(index)
					);
				}

				break;
			}
			default:
		}
	}

	@Override
	protected ButtonTextures getBookButtonTextures() {
		return FILTER_BUTTON_TEXTURES;
	}

	@Override
	protected Text getToggleCraftableButtonText() {
		return TOGGLE_CRAFTABLE_TEXT;
	}

	@Override
	protected void populateRecipes(RecipeResultCollection recipeResultCollection, RecipeFinder recipeFinder) {
		recipeResultCollection.populateRecipes(recipeFinder, this::canDisplay);
	}
}
