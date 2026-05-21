package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

@Environment(EnvType.CLIENT)
/**
 * {@code DebugChart}.
 */
public abstract class DebugChart {

	protected static final int CHART_HEIGHT = 60;
	protected static final int CHART_SCALE = 1;
	protected final TextRenderer textRenderer;
	protected final MultiValueDebugSampleLog log;

	protected DebugChart(TextRenderer textRenderer, MultiValueDebugSampleLog log) {
		this.textRenderer = textRenderer;
		this.log = log;
	}

	public int getWidth(int centerX) {
		return Math.min(this.log.getDimension() + 2, centerX);
	}

	public int getHeight() {
		return 60 + 9;
	}

	/**
	 * Render.
	 *
	 * @param context context
	 * @param x x
	 * @param width width
	 */
	public void render(DrawContext context, int x, int width) {
		int i = context.getScaledWindowHeight();
		context.fill(x, i - 60, x + width, i, -1873784752);
		long l = 0L;
		long m = 2147483647L;
		long n = -2147483648L;
		int j = Math.max(0, this.log.getDimension() - (width - 2));
		int k = this.log.getLength() - j;

		for (int o = 0; o < k; o++) {
			int p = x + o + 1;
			int q = j + o;
			long r = this.get(q);
			m = Math.min(m, r);
			n = Math.max(n, r);
			l += r;
			this.drawBar(context, i, p, q);
		}

		context.drawHorizontalLine(x, x + width - 1, i - 60, -1);
		context.drawHorizontalLine(x, x + width - 1, i - 1, -1);
		context.drawVerticalLine(x, i - 60, i, -1);
		context.drawVerticalLine(x + width - 1, i - 60, i, -1);
		if (k > 0) {
			String string = this.format(m) + " min";
			String string2 = this.format((double) l / k) + " avg";
			String string3 = this.format(n) + " max";
			context.drawTextWithShadow(this.textRenderer, string, x + 2, i - 60 - 9, -2039584);
			context.drawCenteredTextWithShadow(this.textRenderer, string2, x + width / 2, i - 60 - 9, -2039584);
			context.drawTextWithShadow(
					this.textRenderer,
					string3,
					x + width - this.textRenderer.getWidth(string3) - 2,
					i - 60 - 9,
					-2039584
			);
		}

		this.renderThresholds(context, x, width, i);
	}

	/**
	 * Draw bar.
	 *
	 * @param context context
	 * @param y y
	 * @param x x
	 * @param index index
	 */
	protected void drawBar(DrawContext context, int y, int x, int index) {
		this.drawTotalBar(context, y, x, index);
		this.drawOverlayBar(context, y, x, index);
	}

	/**
	 * Draw total bar.
	 *
	 * @param context context
	 * @param y y
	 * @param x x
	 * @param index index
	 */
	protected void drawTotalBar(DrawContext context, int y, int x, int index) {
		long l = this.log.get(index);
		int i = this.getHeight(l);
		int j = this.getColor(l);
		context.fill(x, y - i, x + 1, y, j);
	}

	/**
	 * Draw overlay bar.
	 *
	 * @param context context
	 * @param y y
	 * @param x x
	 * @param index index
	 */
	protected void drawOverlayBar(DrawContext context, int y, int x, int index) {
	}

	/**
	 * Get.
	 *
	 * @param index index
	 *
	 * @return long — 
	 */
	protected long get(int index) {
		return this.log.get(index);
	}

	/**
	 * Отрисовывает thresholds.
	 *
	 * @param context context
	 * @param x x
	 * @param width width
	 * @param height height
	 */
	protected void renderThresholds(DrawContext context, int x, int width, int height) {
	}

	/**
	 * Draw bordered text.
	 *
	 * @param context context
	 * @param string string
	 * @param x x
	 * @param y y
	 */
	protected void drawBorderedText(DrawContext context, String string, int x, int y) {
		context.fill(x, y, x + this.textRenderer.getWidth(string) + 1, y + 9, -1873784752);
		context.drawText(this.textRenderer, string, x + 1, y + 1, -2039584, false);
	}

	/**
	 * Format.
	 *
	 * @param value value
	 *
	 * @return String — результат операции
	 */
	protected abstract String format(double value);

	protected abstract int getHeight(double value);

	protected abstract int getColor(long value);

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
