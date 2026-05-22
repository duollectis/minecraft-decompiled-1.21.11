package net.minecraft.client.gui.hud.bar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

/**
 * Базовый интерфейс для всех полос HUD (опыт, прыжок, локатор).
 * Определяет общие константы размеров и методы позиционирования.
 */
@Environment(EnvType.CLIENT)
public interface Bar {

	int WIDTH = 182;
	int HEIGHT = 5;
	int VERTICAL_OFFSET = 24;

	/** Цвет обводки текста уровня опыта (чёрный). */
	int XP_TEXT_OUTLINE_COLOR = -16777216;

	/** Цвет текста уровня опыта (зелёный). */
	int XP_TEXT_COLOR = -8323296;

	/** Отступ текста уровня опыта от нижнего края экрана. */
	int XP_TEXT_BOTTOM_OFFSET = 24 + 9 + 2;

	Bar EMPTY = new Bar() {
		@Override
		public void renderBar(DrawContext context, RenderTickCounter tickCounter) {
		}

		@Override
		public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
		}
	};

	default int getCenterX(Window window) {
		return (window.getScaledWidth() - WIDTH) / 2;
	}

	default int getCenterY(Window window) {
		return window.getScaledHeight() - VERTICAL_OFFSET - HEIGHT;
	}

	void renderBar(DrawContext context, RenderTickCounter tickCounter);

	void renderAddons(DrawContext context, RenderTickCounter tickCounter);

	/**
	 * Рисует текст уровня опыта по центру экрана с чёрной обводкой.
	 * Обводка достигается четырьмя смещёнными вызовами с чёрным цветом,
	 * поверх которых рисуется основной зелёный текст.
	 */
	static void drawExperienceLevel(DrawContext context, TextRenderer textRenderer, int level) {
		Text text = Text.translatable("gui.experience.level", level);
		int textX = (context.getScaledWindowWidth() - textRenderer.getWidth(text)) / 2;
		int textY = context.getScaledWindowHeight() - XP_TEXT_BOTTOM_OFFSET;

		context.drawText(textRenderer, text, textX + 1, textY, XP_TEXT_OUTLINE_COLOR, false);
		context.drawText(textRenderer, text, textX - 1, textY, XP_TEXT_OUTLINE_COLOR, false);
		context.drawText(textRenderer, text, textX, textY + 1, XP_TEXT_OUTLINE_COLOR, false);
		context.drawText(textRenderer, text, textX, textY - 1, XP_TEXT_OUTLINE_COLOR, false);
		context.drawText(textRenderer, text, textX, textY, XP_TEXT_COLOR, false);
	}
}
