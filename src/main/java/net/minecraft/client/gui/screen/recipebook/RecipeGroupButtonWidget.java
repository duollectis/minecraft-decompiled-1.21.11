package net.minecraft.client.gui.screen.recipebook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.book.RecipeBookGroup;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code RecipeGroupButtonWidget}.
 */
public class RecipeGroupButtonWidget extends TexturedButtonWidget {

	private static final ButtonTextures
			TEXTURES =
			new ButtonTextures(
					Identifier.ofVanilla("recipe_book/tab"),
					Identifier.ofVanilla("recipe_book/tab_selected")
			);
	public static final int BUTTON_WIDTH = 35;
	public static final int BUTTON_HEIGHT = 27;
	private final RecipeBookWidget.Tab tab;
	private static final float ANIMATION_DURATION_TICKS = 15.0F;
	private float bounce;
	private boolean groupFocused = false;

	public RecipeGroupButtonWidget(int x, int y, RecipeBookWidget.Tab tab, ButtonWidget.PressAction onPress) {
		super(x, y, 35, 27, TEXTURES, onPress);
		this.tab = tab;
	}

	/**
	 * Проверяет for new recipes.
	 *
	 * @param recipeBook recipe book
	 * @param filteringCraftable filtering craftable
	 */
	public void checkForNewRecipes(ClientRecipeBook recipeBook, boolean filteringCraftable) {
		RecipeResultCollection.RecipeFilterMode recipeFilterMode = filteringCraftable
		                                                           ? RecipeResultCollection.RecipeFilterMode.CRAFTABLE
		                                                           : RecipeResultCollection.RecipeFilterMode.ANY;

		for (RecipeResultCollection recipeResultCollection : recipeBook.getResultsForCategory(this.tab.category())) {
			for (RecipeDisplayEntry recipeDisplayEntry : recipeResultCollection.filter(recipeFilterMode)) {
				if (recipeBook.isHighlighted(recipeDisplayEntry.id())) {
					this.bounce = 15.0F;
					return;
				}
			}
		}
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.bounce > 0.0F) {
			float f = 1.0F + 0.1F * (float) Math.sin(this.bounce / 15.0F * (float) Math.PI);
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(this.getX() + 8, this.getY() + 12);
			context.getMatrices().scale(1.0F, f);
			context.getMatrices().translate(-(this.getX() + 8), -(this.getY() + 12));
		}

		Identifier identifier = this.textures.get(true, this.groupFocused);
		int i = this.getX();
		if (this.groupFocused) {
			i -= 2;
		}

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, i, this.getY(), this.width, this.height);
		this.renderIcons(context);
		if (this.bounce > 0.0F) {
			context.getMatrices().popMatrix();
			this.bounce -= deltaTicks;
		}
	}

	@Override
	protected void setCursor(DrawContext context) {
		if (!this.groupFocused) {
			super.setCursor(context);
		}
	}

	private void renderIcons(DrawContext context) {
		int i = this.groupFocused ? -2 : 0;
		if (this.tab.secondaryIcon().isPresent()) {
			context.drawItemWithoutEntity(this.tab.primaryIcon(), this.getX() + 3 + i, this.getY() + 5);
			context.drawItemWithoutEntity(this.tab.secondaryIcon().get(), this.getX() + 14 + i, this.getY() + 5);
		}
		else {
			context.drawItemWithoutEntity(this.tab.primaryIcon(), this.getX() + 9 + i, this.getY() + 5);
		}
	}

	public RecipeBookGroup getCategory() {
		return this.tab.category();
	}

	public boolean hasKnownRecipes(ClientRecipeBook recipeBook) {
		List<RecipeResultCollection> list = recipeBook.getResultsForCategory(this.tab.category());
		this.visible = false;

		for (RecipeResultCollection recipeResultCollection : list) {
			if (recipeResultCollection.hasDisplayableRecipes()) {
				this.visible = true;
				break;
			}
		}

		return this.visible;
	}

	/**
	 * Focus.
	 */
	public void focus() {
		this.groupFocused = true;
	}

	/**
	 * Unfocus.
	 */
	public void unfocus() {
		this.groupFocused = false;
	}
}
