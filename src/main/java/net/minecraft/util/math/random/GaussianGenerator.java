package net.minecraft.util.math.random;

import net.minecraft.util.math.MathHelper;

/**
 * Генератор нормально распределённых (гауссовых) случайных чисел
 * методом Бокса-Мюллера. Каждый вызов {@link #next()} производит два значения,
 * второе кешируется для следующего вызова, что вдвое сокращает число обращений к RNG.
 */
public class GaussianGenerator {

	public final Random baseRandom;
	private double cachedGaussian;
	private boolean hasCachedGaussian;

	public GaussianGenerator(Random baseRandom) {
		this.baseRandom = baseRandom;
	}

	public void reset() {
		hasCachedGaussian = false;
	}

	public double next() {
		if (hasCachedGaussian) {
			hasCachedGaussian = false;

			return cachedGaussian;
		}

		double u;
		double v;
		double s;

		do {
			u = 2.0 * baseRandom.nextDouble() - 1.0;
			v = 2.0 * baseRandom.nextDouble() - 1.0;
			s = MathHelper.square(u) + MathHelper.square(v);
		} while (s >= 1.0 || s == 0.0);

		double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
		cachedGaussian = v * multiplier;
		hasCachedGaussian = true;

		return u * multiplier;
	}
}
