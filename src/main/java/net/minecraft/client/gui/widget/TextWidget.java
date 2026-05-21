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

@Environment(EnvType.CLIENT)
/**
 * {@code TextWidget}.
 */
public class TextWidget extends AbstractTextWidget {

	private static final int PADDING = 2;
	private int maxWidth = 0;
	private int cachedWidth = 0;
	private boolean cachedWidthDirty = true;
	private TextWidget.TextOverflow textOverflow = TextWidget.TextOverflow.CLAMPED;

	public TextWidget(Text message, TextRenderer textRenderer) {
		this(0, 0, textRenderer.getWidth(message.asOrderedText()), 9, message, textRenderer);
	}

	public TextWidget(int width, int height, Text message, TextRenderer textRenderer) {
		this(0, 0, width, height, message, textRenderer);
	}

	public TextWidget(int x, int y, int width, int height, Text message, TextRenderer textRenderer) {
		super(x, y, width, height, message, textRenderer);
		this.active = false;
	}

	@Override
	public void setMessage(Text message) {
		super.setMessage(message);
		this.cachedWidthDirty = true;
	}

	public TextWidget setMaxWidth(int width) {
		return this.setMaxWidth(width, TextWidget.TextOverflow.CLAMPED);
	}

	public TextWidget setMaxWidth(int width, TextWidget.TextOverflow textOverflow) {
		this.maxWidth = width;
		this.textOverflow = textOverflow;
		return this;
	}

	@Override
	public int getWidth() {
		if (this.maxWidth > 0) {
			if (this.cachedWidthDirty) {
				this.cachedWidth =
						Math.min(this.maxWidth, this.getTextRenderer().getWidth(this.getMessage().asOrderedText()));
				this.cachedWidthDirty = false;
			}

			return this.cachedWidth;
		}
		else {
			return super.getWidth();
		}
	}

	@Override
	public void draw(DrawnTextConsumer textConsumer) {
		Text text = this.getMessage();
		TextRenderer textRenderer = this.getTextRenderer();
		int i = this.maxWidth > 0 ? this.maxWidth : this.getWidth();
		int j = textRenderer.getWidth(text);
		int k = this.getX();
		int l = this.getY() + (this.getHeight() - 9) / 2;
		boolean bl = j > i;
		if (bl) {
			switch (this.textOverflow) {
				case CLAMPED:
					textConsumer.text(k, l, trim(text, textRenderer, i));
					break;
				case SCROLLING:
					this.drawTextWithMargin(textConsumer, text, 2);
			}
		}
		else {
			textConsumer.text(k, l, text.asOrderedText());
		}
	}

	public static OrderedText trim(Text text, TextRenderer textRenderer, int width) {
		StringVisitable
				stringVisitable =
				textRenderer.trimToWidth(text, width - textRenderer.getWidth(ScreenTexts.ELLIPSIS));
		return Language.getInstance().reorder(StringVisitable.concat(stringVisitable, ScreenTexts.ELLIPSIS));
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TextOverflow}.
	 */
	public static enum TextOverflow {
		CLAMPED,
		SCROLLING;
	}
}
