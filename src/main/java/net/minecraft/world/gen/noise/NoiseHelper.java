package net.minecraft.world.gen.noise;

import java.util.Locale;

/**
 * Утилитарные методы для работы с шумом: применение slide-функции
 * и форматирование отладочной информации о сэмплерах.
 */
public class NoiseHelper {

	/**
	 * Применяет синусоидальное смягчение к значению шума.
	 * Используется для плавного перехода между значениями у границ диапазона.
	 *
	 * @param value исходное значение шума
	 * @param factor коэффициент силы смягчения
	 * @return сглаженное значение
	 */
	public static double applySlide(double value, double factor) {
		return value + Math.sin(Math.PI * value) * factor / Math.PI;
	}

	public static void appendDebugInfo(
			StringBuilder builder,
			double originX,
			double originY,
			double originZ,
			byte[] permutation
	) {
		builder.append(
				String.format(
						Locale.ROOT,
						"xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d",
						(float) originX,
						(float) originY,
						(float) originZ,
						permutation[0],
						permutation[255]
				)
		);
	}

	public static void appendDebugInfo(
			StringBuilder builder,
			double originX,
			double originY,
			double originZ,
			int[] permutation
	) {
		builder.append(
				String.format(
						Locale.ROOT,
						"xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d",
						(float) originX,
						(float) originY,
						(float) originZ,
						permutation[0],
						permutation[255]
				)
		);
	}
}
