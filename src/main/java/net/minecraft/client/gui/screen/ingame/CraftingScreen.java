package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.recipebook.CraftingRecipeBookWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран верстака. Отображает сетку крафта 3×3 с книгой рецептов.
 */
@Environment(EnvType.CLIENT)
public class CraftingScreen extends RecipeBookScreen<CraftingScreenHandler> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/crafting_table.png");
	private static final int TITLE_X = 29;
	private static final int RECIPE_BOOK_BUTTON_X_OFFSET = 5;
	private static final int RECIPE_BOOK_BUTTON_Y_OFFSET = 49;

	public CraftingScreen(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, new CraftingRecipeBookWidget(handler), inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleX = TITLE_X;
	}

	@Override
	protected ScreenPos getRecipeBookButtonPos() {
		return new ScreenPos(x + RECIPE_BOOK_BUTTON_X_OFFSET, height / 2 - RECIPE_BOOK_BUTTON_Y_OFFSET);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgY = (height - backgroundHeight) / 2;
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			x,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
	}
}
