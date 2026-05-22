package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

/**
 * Многострочный текстовый виджет с нарративным описанием, опциональным фоном и рамкой.
 * Поддерживает три режима отрисовки фона через {@link BackgroundRendering}.
 */
@Environment(EnvType.CLIENT)
public class NarratedMultilineTextWidget extends MultilineTextWidget {

	private static final int LINE_HEIGHT = 9;
	private static final int NO_CUSTOM_WIDTH = -1;

	public static final int DEFAULT_MARGIN = 4;

	private final int margin;
	private final int customWidth;
	private final boolean alwaysShowBorders;
	private final NarratedMultilineTextWidget.BackgroundRendering backgroundRendering;

	NarratedMultilineTextWidget(
			Text text,
			TextRenderer textRenderer,
			int margin,
			int customWidth,
			NarratedMultilineTextWidget.BackgroundRendering backgroundRendering,
			boolean alwaysShowBorders
	) {
		super(text, textRenderer);
		active = true;
		this.margin = margin;
		this.customWidth = customWidth;
		this.alwaysShowBorders = alwaysShowBorders;
		this.backgroundRendering = backgroundRendering;
		updateWidth();
		updateHeight();
		setCentered(true);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, getMessage());
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int borderColor = alwaysShowBorders && !isFocused()
				? ColorHelper.withAlpha(alpha, -6250336)
				: ColorHelper.getWhite(alpha);

		switch (backgroundRendering) {
			case ALWAYS -> context.fill(getX() + 1, getY(), getRight(), getBottom(), ColorHelper.toAlpha(alpha));
			case ON_FOCUS -> {
				if (isFocused()) {
					context.fill(getX() + 1, getY(), getRight(), getBottom(), ColorHelper.toAlpha(alpha));
				}
			}
			case NEVER -> {}
		}

		if (isFocused() || alwaysShowBorders) {
			context.drawStrokedRectangle(getX(), getY(), getWidth(), getHeight(), borderColor);
		}

		super.renderWidget(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	protected int getTextX() {
		return getX() + margin;
	}

	@Override
	protected int getTextY() {
		return super.getTextY() + margin;
	}

	@Override
	public MultilineTextWidget setMaxWidth(int maxWidth) {
		return super.setMaxWidth(maxWidth - margin * 2);
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	public int getMargin() {
		return margin;
	}

	public void updateWidth() {
		if (customWidth != NO_CUSTOM_WIDTH) {
			setWidth(customWidth);
			setMaxWidth(customWidth);
		}
		else {
			setWidth(getTextRenderer().getWidth(getMessage()) + margin * 2);
		}
	}

	public void updateHeight() {
		int textHeight = LINE_HEIGHT * getTextRenderer().wrapLines(getMessage(), super.getWidth()).size();
		setHeight(textHeight + margin * 2);
	}

	@Override
	public void setMessage(Text message) {
		this.message = message;
		int newWidth = customWidth != NO_CUSTOM_WIDTH
				? customWidth
				: getTextRenderer().getWidth(message) + margin * 2;
		setWidth(newWidth);
		updateHeight();
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	public static NarratedMultilineTextWidget.Builder builder(Text text, TextRenderer textRenderer) {
		return new NarratedMultilineTextWidget.Builder(text, textRenderer);
	}

	public static NarratedMultilineTextWidget.Builder builder(Text text, TextRenderer textRenderer, int margin) {
		return new NarratedMultilineTextWidget.Builder(text, textRenderer, margin);
	}

	@Environment(EnvType.CLIENT)
	public enum BackgroundRendering {
		ALWAYS,
		ON_FOCUS,
		NEVER
	}

	/**
	 * Строитель для создания экземпляров {@link NarratedMultilineTextWidget}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final Text text;
		private final TextRenderer textRenderer;
		private final int margin;
		private int customWidth = NO_CUSTOM_WIDTH;
		private boolean alwaysShowBorders = true;
		private NarratedMultilineTextWidget.BackgroundRendering backgroundRendering = BackgroundRendering.ALWAYS;

		Builder(Text text, TextRenderer textRenderer) {
			this(text, textRenderer, DEFAULT_MARGIN);
		}

		Builder(Text text, TextRenderer textRenderer, int margin) {
			this.text = text;
			this.textRenderer = textRenderer;
			this.margin = margin;
		}

		public NarratedMultilineTextWidget.Builder width(int width) {
			customWidth = width;
			return this;
		}

		public NarratedMultilineTextWidget.Builder innerWidth(int width) {
			customWidth = width + margin * 2;
			return this;
		}

		public NarratedMultilineTextWidget.Builder alwaysShowBorders(boolean alwaysShowBorders) {
			this.alwaysShowBorders = alwaysShowBorders;
			return this;
		}

		public NarratedMultilineTextWidget.Builder backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering backgroundRendering) {
			this.backgroundRendering = backgroundRendering;
			return this;
		}

		public NarratedMultilineTextWidget build() {
			return new NarratedMultilineTextWidget(
					text,
					textRenderer,
					margin,
					customWidth,
					backgroundRendering,
					alwaysShowBorders
			);
		}
	}
}
