package net.minecraft.client.render;

import net.minecraft.util.math.ColorHelper;

/**
 * Описывает стиль отрисовки примитивов: цвет и толщину обводки, цвет заливки.
 * Используется в системе отладочного и гизмо-рендеринга для единообразного задания
 * визуальных параметров линий, полигонов и точек.
 */
public record DrawStyle(int stroke, float strokeWidth, int fill) {

	private static final float DEFAULT_STROKE_WIDTH = 2.5F;

	/** Создаёт стиль только с обводкой стандартной толщины {@value #DEFAULT_STROKE_WIDTH}. */
	public static DrawStyle stroked(int stroke) {
		return new DrawStyle(stroke, DEFAULT_STROKE_WIDTH, 0);
	}

	/** Создаёт стиль только с обводкой заданной толщины. */
	public static DrawStyle stroked(int stroke, float strokeWidth) {
		return new DrawStyle(stroke, strokeWidth, 0);
	}

	/** Создаёт стиль только с заливкой без обводки. */
	public static DrawStyle filled(int fill) {
		return new DrawStyle(0, 0.0F, fill);
	}

	/** Создаёт стиль с заливкой и обводкой одновременно. */
	public static DrawStyle filledAndStroked(int stroke, float strokeWidth, int fill) {
		return new DrawStyle(stroke, strokeWidth, fill);
	}

	public boolean hasFill() {
		return fill != 0;
	}

	public boolean hasStroke() {
		return stroke != 0 && strokeWidth > 0.0F;
	}

	/** Возвращает цвет обводки с применённым коэффициентом прозрачности. */
	public int stroke(float opacity) {
		return ColorHelper.scaleAlpha(stroke, opacity);
	}

	/** Возвращает цвет заливки с применённым коэффициентом прозрачности. */
	public int fill(float opacity) {
		return ColorHelper.scaleAlpha(fill, opacity);
	}
}
