package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

/**
 * Базовый класс для отладочных графиков. Отрисовывает столбчатую диаграмму
 * с рамкой, подписями min/avg/max и опциональными пороговыми линиями.
 */
@Environment(EnvType.CLIENT)
public abstract class DebugChart {

	protected static final int CHART_HEIGHT = 60;
	protected static final int CHART_SCALE = 1;

	/** Цвет фона графика (полупрозрачный тёмный). */
	private static final int BACKGROUND_COLOR = -1873784752;

	/** Цвет текста подписей (светло-жёлтый). */
	private static final int LABEL_COLOR = -2039584;

	protected final TextRenderer textRenderer;
	protected final MultiValueDebugSampleLog log;

	protected DebugChart(TextRenderer textRenderer, MultiValueDebugSampleLog log) {
		this.textRenderer = textRenderer;
		this.log = log;
	}

	public int getWidth(int centerX) {
		return Math.min(log.getDimension() + 2, centerX);
	}

	public int getHeight() {
		return CHART_HEIGHT + 9;
	}

	public void render(DrawContext context, int x, int width) {
		int screenHeight = context.getScaledWindowHeight();

		context.fill(x, screenHeight - CHART_HEIGHT, x + width, screenHeight, BACKGROUND_COLOR);

		long sum = 0L;
		long minValue = Long.MAX_VALUE;
		long maxValue = Long.MIN_VALUE;
		int startOffset = Math.max(0, log.getDimension() - (width - 2));
		int sampleCount = log.getLength() - startOffset;

		for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
			int barX = x + sampleIndex + 1;
			int logIndex = startOffset + sampleIndex;
			long value = get(logIndex);

			minValue = Math.min(minValue, value);
			maxValue = Math.max(maxValue, value);
			sum += value;
			drawBar(context, screenHeight, barX, logIndex);
		}

		context.drawHorizontalLine(x, x + width - 1, screenHeight - CHART_HEIGHT, -1);
		context.drawHorizontalLine(x, x + width - 1, screenHeight - 1, -1);
		context.drawVerticalLine(x, screenHeight - CHART_HEIGHT, screenHeight, -1);
		context.drawVerticalLine(x + width - 1, screenHeight - CHART_HEIGHT, screenHeight, -1);

		if (sampleCount > 0) {
			String minLabel = format(minValue) + " min";
			String avgLabel = format((double) sum / sampleCount) + " avg";
			String maxLabel = format(maxValue) + " max";
			int labelY = screenHeight - CHART_HEIGHT - 9;

			context.drawTextWithShadow(textRenderer, minLabel, x + 2, labelY, LABEL_COLOR);
			context.drawCenteredTextWithShadow(textRenderer, avgLabel, x + width / 2, labelY, LABEL_COLOR);
			context.drawTextWithShadow(textRenderer, maxLabel, x + width - textRenderer.getWidth(maxLabel) - 2, labelY, LABEL_COLOR);
		}

		renderThresholds(context, x, width, screenHeight);
	}

	protected void drawBar(DrawContext context, int y, int x, int index) {
		drawTotalBar(context, y, x, index);
		drawOverlayBar(context, y, x, index);
	}

	protected void drawTotalBar(DrawContext context, int y, int x, int index) {
		long value = log.get(index);
		int barHeight = getHeight(value);
		int color = getColor(value);
		context.fill(x, y - barHeight, x + 1, y, color);
	}

	protected void drawOverlayBar(DrawContext context, int y, int x, int index) {
	}

	protected long get(int index) {
		return log.get(index);
	}

	protected void renderThresholds(DrawContext context, int x, int width, int height) {
	}

	protected void drawBorderedText(DrawContext context, String text, int x, int y) {
		context.fill(x, y, x + textRenderer.getWidth(text) + 1, y + 9, BACKGROUND_COLOR);
		context.drawText(textRenderer, text, x + 1, y + 1, LABEL_COLOR, false);
	}

	protected abstract String format(double value);

	protected abstract int getHeight(double value);

	protected abstract int getColor(long value);

	/**
	 * Интерполирует цвет между тремя опорными точками (min → median → max).
	 * Используется подклассами для цветовой индикации нагрузки.
	 */
	protected int getColor(
		double value,
		double min,
		int minColor,
		double median,
		int medianColor,
		double max,
		int maxColor
	) {
		value = MathHelper.clamp(value, min, max);
		return value < median
			? ColorHelper.lerp((float) ((value - min) / (median - min)), minColor, medianColor)
			: ColorHelper.lerp((float) ((value - median) / (max - median)), medianColor, maxColor);
	}
}
