package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Простой экран с одним текстовым сообщением по центру.
 * Используется для отображения статусных сообщений без кнопок.
 */
@Environment(EnvType.CLIENT)
public class MessageScreen extends Screen {

	private @Nullable NarratedMultilineTextWidget textWidget;

	public MessageScreen(Text text) {
		super(text);
	}

	@Override
	protected void init() {
		textWidget = addDrawableChild(
				NarratedMultilineTextWidget
						.builder(title, textRenderer, 12)
						.innerWidth(textRenderer.getWidth(title))
						.build()
		);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (textWidget == null) {
			return;
		}

		textWidget.setPosition(width / 2 - textWidget.getWidth() / 2, height / 2 - 9 / 2);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	protected boolean hasUsageText() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		renderPanoramaBackground(context, deltaTicks);
		applyBlur(context);
		renderDarkening(context);
	}
}
