package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Глиф-эффект, создающий прямоугольные декоративные элементы текста:
 * подчёркивание, зачёркивание, фоновый прямоугольник.
 * Использует белый пиксель из атласа, окрашивая его в нужный цвет при рендеринге.
 */
@Environment(EnvType.CLIENT)
public interface EffectGlyph {

	/**
	 * Создаёт {@link TextDrawable} для прямоугольного эффекта с заданными границами.
	 *
	 * @param minX         левая граница прямоугольника
	 * @param minY         верхняя граница прямоугольника
	 * @param maxX         правая граница прямоугольника
	 * @param maxY         нижняя граница прямоугольника
	 * @param depth        глубина (Z-координата) прямоугольника
	 * @param color        основной цвет (ARGB)
	 * @param shadowColor  цвет тени (ARGB), 0 — без тени
	 * @param shadowOffset смещение тени в пикселях
	 * @return объект отрисовки прямоугольного эффекта
	 */
	TextDrawable create(
			float minX,
			float minY,
			float maxX,
			float maxY,
			float depth,
			int color,
			int shadowColor,
			float shadowOffset
	);
}
