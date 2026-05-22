package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Экран стойки для зелий. Отображает прогресс варки, уровень топлива и анимацию пузырьков.
 */
@Environment(EnvType.CLIENT)
public class BrewingStandScreen extends HandledScreen<BrewingStandScreenHandler> {

	private static final Identifier FUEL_LENGTH_TEXTURE = Identifier.ofVanilla("container/brewing_stand/fuel_length");
	private static final Identifier BREW_PROGRESS_TEXTURE = Identifier.ofVanilla("container/brewing_stand/brew_progress");
	private static final Identifier BUBBLES_TEXTURE = Identifier.ofVanilla("container/brewing_stand/bubbles");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/brewing_stand.png");
	private static final int[] BUBBLE_PROGRESS = new int[]{29, 24, 20, 16, 11, 6, 0};
	private static final int FUEL_MAX_WIDTH = 18;
	private static final int FUEL_MAX_VALUE = 20;
	private static final int BREW_TOTAL_TICKS = 400;
	private static final int BREW_PROGRESS_HEIGHT = 28;
	private static final int BUBBLE_CYCLE = 7;

	public BrewingStandScreen(BrewingStandScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);

		int fuelWidth = MathHelper.clamp((FUEL_MAX_WIDTH * handler.getFuel() + FUEL_MAX_VALUE - 1) / FUEL_MAX_VALUE, 0, FUEL_MAX_WIDTH);
		if (fuelWidth > 0) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				FUEL_LENGTH_TEXTURE,
				FUEL_MAX_WIDTH,
				4,
				0,
				0,
				bgX + 60,
				bgY + 44,
				fuelWidth,
				4
			);
		}

		int brewTime = handler.getBrewTime();
		if (brewTime > 0) {
			int brewProgress = (int) (BREW_PROGRESS_HEIGHT * (1.0F - brewTime / (float) BREW_TOTAL_TICKS));
			if (brewProgress > 0) {
				context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					BREW_PROGRESS_TEXTURE,
					9,
					BREW_PROGRESS_HEIGHT,
					0,
					0,
					bgX + 97,
					bgY + 16,
					9,
					brewProgress
				);
			}

			int bubbleHeight = BUBBLE_PROGRESS[brewTime / 2 % BUBBLE_CYCLE];
			if (bubbleHeight > 0) {
				context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					BUBBLES_TEXTURE,
					12,
					29,
					0,
					29 - bubbleHeight,
					bgX + 63,
					bgY + 14 + 29 - bubbleHeight,
					12,
					bubbleHeight
				);
			}
		}
	}
}
