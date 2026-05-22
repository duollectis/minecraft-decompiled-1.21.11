package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import org.jspecify.annotations.Nullable;

/**
 * Запечённый (готовый к рендерингу) глиф, размещённый в атласе текстур.
 * Хранит метрики и умеет создавать {@link TextDrawable.DrawnGlyphRect}
 * для последующей отрисовки в буфере вершин.
 */
@Environment(EnvType.CLIENT)
public interface BakedGlyph {

	GlyphMetrics getMetrics();

	/**
	 * Создаёт объект отрисовки глифа с учётом позиции, цвета и стиля.
	 *
	 * @param x            X-координата начала глифа
	 * @param y            Y-координата начала глифа
	 * @param color        основной цвет (ARGB)
	 * @param shadowColor  цвет тени (ARGB), 0 — без тени
	 * @param style        стиль текста (жирный, курсив и т.д.)
	 * @param boldOffset   горизонтальное смещение для имитации жирности
	 * @param shadowOffset смещение тени в пикселях
	 * @return объект отрисовки или {@code null}, если глиф невидим
	 */
	TextDrawable.@Nullable DrawnGlyphRect create(
			float x,
			float y,
			int color,
			int shadowColor,
			Style style,
			float boldOffset,
			float shadowOffset
	);
}
