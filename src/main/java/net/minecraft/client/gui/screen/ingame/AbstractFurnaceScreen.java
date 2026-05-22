package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.recipebook.FurnaceRecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Базовый экран для всех печей (обычная, доменная, коптильня).
 * Отрисовывает прогресс горения топлива и прогресс плавки/готовки.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractFurnaceScreen<T extends AbstractFurnaceScreenHandler> extends RecipeBookScreen<T> {

	private static final int LIT_PROGRESS_FULL = 14;
	private static final int COOK_PROGRESS_FULL = 24;
	private static final int COOK_PROGRESS_HEIGHT = 16;

	private final Identifier background;
	private final Identifier litProgressTexture;
	private final Identifier burnProgressTexture;

	public AbstractFurnaceScreen(
		T handler,
		PlayerInventory playerInventory,
		Text title,
		Text toggleCraftableButtonText,
		Identifier background,
		Identifier litProgressTexture,
		Identifier burnProgressTexture,
		List<RecipeBookWidget.Tab> recipeBookTabs
	) {
		super(
			handler,
			new FurnaceRecipeBookWidget(handler, toggleCraftableButtonText, recipeBookTabs),
			playerInventory,
			title
		);
		this.background = background;
		this.litProgressTexture = litProgressTexture;
		this.burnProgressTexture = burnProgressTexture;
	}

	@Override
	public void init() {
		super.init();
		titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
	}

	@Override
	protected ScreenPos getRecipeBookButtonPos() {
		return new ScreenPos(x + 20, height / 2 - 49);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			background,
			x,
			y,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);

		if (handler.isBurning()) {
			int litProgress = MathHelper.ceil(handler.getFuelProgress() * 13.0F) + 1;
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				litProgressTexture,
				LIT_PROGRESS_FULL,
				LIT_PROGRESS_FULL,
				0,
				LIT_PROGRESS_FULL - litProgress,
				x + 56,
				y + 36 + LIT_PROGRESS_FULL - litProgress,
				LIT_PROGRESS_FULL,
				litProgress
			);
		}

		int cookProgress = MathHelper.ceil(handler.getCookProgress() * COOK_PROGRESS_FULL);
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			burnProgressTexture,
			COOK_PROGRESS_FULL,
			COOK_PROGRESS_HEIGHT,
			0,
			0,
			x + 79,
			y + 34,
			cookProgress,
			COOK_PROGRESS_HEIGHT
		);
	}
}
