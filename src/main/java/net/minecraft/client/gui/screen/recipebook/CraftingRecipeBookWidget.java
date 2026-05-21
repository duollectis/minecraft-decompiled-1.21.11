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

@Environment(EnvType.CLIENT)
/**
 * {@code CraftingRecipeBookWidget}.
 */
public class CraftingRecipeBookWidget extends RecipeBookWidget<AbstractCraftingScreenHandler> {

	private static final ButtonTextures FILTER_BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/filter_enabled"),
			Identifier.ofVanilla("recipe_book/filter_disabled"),
			Identifier.ofVanilla("recipe_book/filter_enabled_highlighted"),
			Identifier.ofVanilla("recipe_book/filter_disabled_highlighted")
	);
	private static final Text TOGGLE_CRAFTABLE_TEXT = Text.translatable("gui.recipebook.toggleRecipes.craftable");
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
		return this.craftingScreenHandler.getOutputSlot() == slot || this.craftingScreenHandler
				.getInputSlots()
				.contains(slot);
	}

	private boolean canDisplay(RecipeDisplay display) {
		int i = this.craftingScreenHandler.getWidth();
		int j = this.craftingScreenHandler.getHeight();

		return switch (display) {
			case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay ->
					i >= shapedCraftingRecipeDisplay.width() && j >= shapedCraftingRecipeDisplay.height();
			case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay ->
					i * j >= shapelessCraftingRecipeDisplay.ingredients().size();
			default -> false;
		};
	}

	@Override
	protected void showGhostRecipe(GhostRecipe ghostRecipe, RecipeDisplay display, ContextParameterMap context) {
		ghostRecipe.addResults(this.craftingScreenHandler.getOutputSlot(), context, display.result());
		switch (display) {
			case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay:
				List<Slot> list = this.craftingScreenHandler.getInputSlots();
				RecipeGridAligner.alignRecipeToGrid(
						this.craftingScreenHandler.getWidth(),
						this.craftingScreenHandler.getHeight(),
						shapedCraftingRecipeDisplay.width(),
						shapedCraftingRecipeDisplay.height(),
						shapedCraftingRecipeDisplay.ingredients(),
						(slot, index, x, y) -> {
							Slot slot2 = list.get(index);
							ghostRecipe.addInputs(slot2, context, slot);
						}
				);
				break;
			case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay:
				label15:
				{
					List<Slot> list2 = this.craftingScreenHandler.getInputSlots();
					int i = Math.min(shapelessCraftingRecipeDisplay.ingredients().size(), list2.size());

					for (int j = 0; j < i; j++) {
						ghostRecipe.addInputs(
								list2.get(j),
								context,
								shapelessCraftingRecipeDisplay.ingredients().get(j)
						);
					}
					break label15;
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
