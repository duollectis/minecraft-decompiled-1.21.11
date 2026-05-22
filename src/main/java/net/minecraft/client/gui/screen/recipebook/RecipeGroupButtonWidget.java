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

/**
 * Кнопка вкладки книги рецептов с анимацией подпрыгивания при появлении новых рецептов.
 */
@Environment(EnvType.CLIENT)
public class RecipeGroupButtonWidget extends TexturedButtonWidget {

	private static final ButtonTextures TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("recipe_book/tab"),
			Identifier.ofVanilla("recipe_book/tab_selected")
	);
	public static final int BUTTON_WIDTH = 35;
	public static final int BUTTON_HEIGHT = 27;
	private static final float ANIMATION_DURATION_TICKS = 15.0F;
	private static final int ICON_OFFSET_X_SINGLE = 9;
	private static final int ICON_OFFSET_X_PRIMARY = 3;
	private static final int ICON_OFFSET_X_SECONDARY = 14;
	private static final int ICON_OFFSET_Y = 5;
	private static final int FOCUSED_SHIFT = -2;

	private final RecipeBookWidget.Tab tab;
	private float bounce;
	private boolean groupFocused = false;

	public RecipeGroupButtonWidget(int x, int y, RecipeBookWidget.Tab tab, ButtonWidget.PressAction onPress) {
		super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, TEXTURES, onPress);
		this.tab = tab;
	}

	/**
	 * Проверяет наличие новых (подсвеченных) рецептов в категории вкладки
	 * и запускает анимацию подпрыгивания при их обнаружении.
	 */
	public void checkForNewRecipes(ClientRecipeBook recipeBook, boolean filteringCraftable) {
		RecipeResultCollection.RecipeFilterMode filterMode = filteringCraftable
				? RecipeResultCollection.RecipeFilterMode.CRAFTABLE
				: RecipeResultCollection.RecipeFilterMode.ANY;

		for (RecipeResultCollection collection : recipeBook.getResultsForCategory(tab.category())) {
			for (RecipeDisplayEntry entry : collection.filter(filterMode)) {
				if (recipeBook.isHighlighted(entry.id())) {
					bounce = ANIMATION_DURATION_TICKS;
					return;
				}
			}
		}
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (bounce > 0.0F) {
			float scale = 1.0F + 0.1F * (float) Math.sin(bounce / ANIMATION_DURATION_TICKS * (float) Math.PI);
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(getX() + 8, getY() + 12);
			context.getMatrices().scale(1.0F, scale);
			context.getMatrices().translate(-(getX() + 8), -(getY() + 12));
		}

		Identifier tabTexture = textures.get(true, groupFocused);
		int drawX = groupFocused ? getX() + FOCUSED_SHIFT : getX();

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, tabTexture, drawX, getY(), width, height);
		renderIcons(context);

		if (bounce > 0.0F) {
			context.getMatrices().popMatrix();
			bounce -= deltaTicks;
		}
	}

	@Override
	protected void setCursor(DrawContext context) {
		if (!groupFocused) {
			super.setCursor(context);
		}
	}

	private void renderIcons(DrawContext context) {
		int iconShift = groupFocused ? FOCUSED_SHIFT : 0;

		if (tab.secondaryIcon().isPresent()) {
			context.drawItemWithoutEntity(tab.primaryIcon(), getX() + ICON_OFFSET_X_PRIMARY + iconShift, getY() + ICON_OFFSET_Y);
			context.drawItemWithoutEntity(tab.secondaryIcon().get(), getX() + ICON_OFFSET_X_SECONDARY + iconShift, getY() + ICON_OFFSET_Y);
		} else {
			context.drawItemWithoutEntity(tab.primaryIcon(), getX() + ICON_OFFSET_X_SINGLE + iconShift, getY() + ICON_OFFSET_Y);
		}
	}

	public RecipeBookGroup getCategory() {
		return tab.category();
	}

	public boolean hasKnownRecipes(ClientRecipeBook recipeBook) {
		List<RecipeResultCollection> collections = recipeBook.getResultsForCategory(tab.category());
		visible = false;

		for (RecipeResultCollection collection : collections) {
			if (collection.hasDisplayableRecipes()) {
				visible = true;
				break;
			}
		}

		return visible;
	}

	public void focus() {
		groupFocused = true;
	}

	public void unfocus() {
		groupFocused = false;
	}
}
