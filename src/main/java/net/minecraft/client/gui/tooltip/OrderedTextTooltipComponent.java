package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;

@Environment(EnvType.CLIENT)
/**
 * {@code OrderedTextTooltipComponent}.
 */
public class OrderedTextTooltipComponent implements TooltipComponent {

	private final OrderedText text;

	public OrderedTextTooltipComponent(OrderedText text) {
		this.text = text;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return textRenderer.getWidth(this.text);
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return 10;
	}

	@Override
	public void drawText(DrawContext context, TextRenderer textRenderer, int x, int y) {
		context.drawText(textRenderer, this.text, x, y, -1, true);
	}
}
