package net.minecraft.client.render;

import net.minecraft.util.math.ColorHelper;

/**
 * {@code DrawStyle}.
 */
public record DrawStyle(int stroke, float strokeWidth, int fill) {

	private static final float DEFAULT_STROKE_WIDTH = 2.5F;

	/**
	 * Stroked.
	 *
	 * @param stroke stroke
	 *
	 * @return DrawStyle — результат операции
	 */
	public static DrawStyle stroked(int stroke) {
		return new DrawStyle(stroke, 2.5F, 0);
	}

	/**
	 * Stroked.
	 *
	 * @param stroke stroke
	 * @param strokeWidth stroke width
	 *
	 * @return DrawStyle — результат операции
	 */
	public static DrawStyle stroked(int stroke, float strokeWidth) {
		return new DrawStyle(stroke, strokeWidth, 0);
	}

	/**
	 * Filled.
	 *
	 * @param fill fill
	 *
	 * @return DrawStyle — результат операции
	 */
	public static DrawStyle filled(int fill) {
		return new DrawStyle(0, 0.0F, fill);
	}

	/**
	 * Filled and stroked.
	 *
	 * @param stroke stroke
	 * @param strokeWidth stroke width
	 * @param fill fill
	 *
	 * @return DrawStyle — результат операции
	 */
	public static DrawStyle filledAndStroked(int stroke, float strokeWidth, int fill) {
		return new DrawStyle(stroke, strokeWidth, fill);
	}

	public boolean hasFill() {
		return this.fill != 0;
	}

	public boolean hasStroke() {
		return this.stroke != 0 && this.strokeWidth > 0.0F;
	}

	/**
	 * Stroke.
	 *
	 * @param opacity opacity
	 *
	 * @return int — результат операции
	 */
	public int stroke(float opacity) {
		return ColorHelper.scaleAlpha(this.stroke, opacity);
	}

	/**
	 * Fill.
	 *
	 * @param opacity opacity
	 *
	 * @return int — результат операции
	 */
	public int fill(float opacity) {
		return ColorHelper.scaleAlpha(this.fill, opacity);
	}
}
