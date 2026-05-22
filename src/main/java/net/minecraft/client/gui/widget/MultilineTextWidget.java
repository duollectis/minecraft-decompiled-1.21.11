package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.CachedMapper;
import net.minecraft.util.Util;

import java.util.OptionalInt;

/**
 * Виджет многострочного текста с кешированием разбивки строк и поддержкой центрирования.
 */
@Environment(EnvType.CLIENT)
public class MultilineTextWidget extends AbstractTextWidget {

	private static final int LINE_HEIGHT = 9;

	private OptionalInt maxWidth = OptionalInt.empty();
	private OptionalInt maxRows = OptionalInt.empty();
	private final CachedMapper<MultilineTextWidget.CacheKey, MultilineText> cacheKeyToText;
	private boolean centered = false;

	public MultilineTextWidget(Text message, TextRenderer textRenderer) {
		this(0, 0, message, textRenderer);
	}

	public MultilineTextWidget(int x, int y, Text message, TextRenderer textRenderer) {
		super(x, y, 0, 0, message, textRenderer);
		cacheKeyToText = Util.cachedMapper(
				cacheKey -> cacheKey.maxRows.isPresent()
				            ? MultilineText.create(
						textRenderer,
						cacheKey.maxWidth,
						cacheKey.maxRows.getAsInt(),
						cacheKey.message
				)
				            : MultilineText.create(textRenderer, cacheKey.message, cacheKey.maxWidth)
		);
		active = false;
	}

	public MultilineTextWidget setMaxWidth(int maxWidth) {
		this.maxWidth = OptionalInt.of(maxWidth);
		return this;
	}

	public MultilineTextWidget setMaxRows(int maxRows) {
		this.maxRows = OptionalInt.of(maxRows);
		return this;
	}

	public MultilineTextWidget setCentered(boolean centered) {
		this.centered = centered;
		return this;
	}

	@Override
	public int getWidth() {
		return cacheKeyToText.map(getCacheKey()).getMaxWidth();
	}

	@Override
	public int getHeight() {
		return cacheKeyToText.map(getCacheKey()).getLineCount() * LINE_HEIGHT;
	}

	@Override
	public void draw(DrawnTextConsumer textConsumer) {
		MultilineText multilineText = cacheKeyToText.map(getCacheKey());
		int textX = getTextX();
		int textY = getTextY();

		if (centered) {
			int centerX = getX() + getWidth() / 2;
			multilineText.draw(Alignment.CENTER, centerX, textY, LINE_HEIGHT, textConsumer);
		}
		else {
			multilineText.draw(Alignment.LEFT, textX, textY, LINE_HEIGHT, textConsumer);
		}
	}

	protected int getTextX() {
		return getX();
	}

	protected int getTextY() {
		return getY();
	}

	private MultilineTextWidget.CacheKey getCacheKey() {
		return new MultilineTextWidget.CacheKey(
				getMessage(),
				maxWidth.orElse(Integer.MAX_VALUE),
				maxRows
		);
	}

	@Environment(EnvType.CLIENT)
	record CacheKey(Text message, int maxWidth, OptionalInt maxRows) {
	}
}
