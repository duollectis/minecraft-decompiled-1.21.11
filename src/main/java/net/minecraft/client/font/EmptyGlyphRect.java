package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;

/**
 * Прямоугольная область пустого (невидимого) глифа.
 * Используется для hit-testing при обработке кликов и наведения мыши
 * на символы, у которых нет визуального представления (пробелы и т.п.).
 */
@Environment(EnvType.CLIENT)
public record EmptyGlyphRect(
		float x,
		float y,
		float advance,
		float ascent,
		float height,
		Style style
) implements GlyphRect {

	/** Стандартная высота строки в пикселях. */
	public static final float DEFAULT_HEIGHT = 9.0F;

	/** Стандартный подъём (ascent) в пикселях. */
	public static final float DEFAULT_ASCENT = 7.0F;

	@Override
	public float getLeft() {
		return x;
	}

	@Override
	public float getTop() {
		return y + DEFAULT_ASCENT - ascent;
	}

	@Override
	public float getRight() {
		return x + advance;
	}

	@Override
	public float getBottom() {
		return getTop() + height;
	}
}
