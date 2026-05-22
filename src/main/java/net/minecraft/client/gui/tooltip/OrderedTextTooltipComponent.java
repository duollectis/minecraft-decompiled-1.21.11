package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

/**
 * Компонент тултипа для одной строки текста в виде {@link OrderedText}.
 * Используется для отображения стандартных текстовых строк в тултипах предметов.
 */
@Environment(EnvType.CLIENT)
public class OrderedTextTooltipComponent implements TooltipComponent {

	private static final int LINE_HEIGHT = 10;
	/** Цвет текста тултипа (белый с тенью). */
	private static final int TEXT_COLOR = -1;

	private final OrderedText text;

	public OrderedTextTooltipComponent(OrderedText text) {
		this.text = text;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return textRenderer.getWidth(text);
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return LINE_HEIGHT;
	}

	@Override
	public void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
		context.drawText(textRenderer, text, x, y, TEXT_COLOR, true);
	}
}
