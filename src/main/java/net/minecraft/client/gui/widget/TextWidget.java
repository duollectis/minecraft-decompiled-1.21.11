package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

/**
 * Виджет однострочного текста с поддержкой ограничения ширины и двух режимов переполнения:
 * обрезки с многоточием ({@link TextOverflow#CLAMPED}) и прокрутки ({@link TextOverflow#SCROLLING}).
 */
@Environment(EnvType.CLIENT)
public class TextWidget extends AbstractTextWidget {

	private static final int LINE_HEIGHT = 9;
	private static final int SCROLL_MARGIN = 2;

	private int maxWidth = 0;
	private int cachedWidth = 0;
	private boolean cachedWidthDirty = true;
	private TextWidget.TextOverflow textOverflow = TextWidget.TextOverflow.CLAMPED;

	public TextWidget(Text message, TextRenderer textRenderer) {
		this(0, 0, textRenderer.getWidth(message.asOrderedText()), LINE_HEIGHT, message, textRenderer);
	}

	public TextWidget(int width, int height, Text message, TextRenderer textRenderer) {
		this(0, 0, width, height, message, textRenderer);
	}

	public TextWidget(int x, int y, int width, int height, Text message, TextRenderer textRenderer) {
		super(x, y, width, height, message, textRenderer);
		active = false;
	}

	@Override
	public void setMessage(Text message) {
		super.setMessage(message);
		cachedWidthDirty = true;
	}

	public TextWidget setMaxWidth(int width) {
		return setMaxWidth(width, TextWidget.TextOverflow.CLAMPED);
	}

	public TextWidget setMaxWidth(int width, TextWidget.TextOverflow textOverflow) {
		this.maxWidth = width;
		this.textOverflow = textOverflow;
		return this;
	}

	@Override
	public int getWidth() {
		if (maxWidth <= 0) {
			return super.getWidth();
		}

		if (cachedWidthDirty) {
			cachedWidth = Math.min(maxWidth, getTextRenderer().getWidth(getMessage().asOrderedText()));
			cachedWidthDirty = false;
		}

		return cachedWidth;
	}

	@Override
	public void draw(DrawnTextConsumer textConsumer) {
		Text text = getMessage();
		TextRenderer textRenderer = getTextRenderer();
		int availableWidth = maxWidth > 0 ? maxWidth : getWidth();
		int textWidth = textRenderer.getWidth(text);
		int drawX = getX();
		int drawY = getY() + (getHeight() - LINE_HEIGHT) / 2;

		if (textWidth <= availableWidth) {
			textConsumer.text(drawX, drawY, text.asOrderedText());
			return;
		}

		switch (textOverflow) {
			case CLAMPED -> textConsumer.text(drawX, drawY, trim(text, textRenderer, availableWidth));
			case SCROLLING -> drawTextWithMargin(textConsumer, text, SCROLL_MARGIN);
		}
	}

	public static OrderedText trim(Text text, TextRenderer textRenderer, int width) {
		StringVisitable trimmed = textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS));
		return Language.getInstance().reorder(StringVisitable.concat(trimmed, ScreenTexts.ELLIPSIS));
	}

	@Environment(EnvType.CLIENT)
	public enum TextOverflow {
		CLAMPED,
		SCROLLING
	}
}
