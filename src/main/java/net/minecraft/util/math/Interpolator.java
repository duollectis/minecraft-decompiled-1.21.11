package net.minecraft.util.math;

/**
 * Функциональный интерфейс для интерполяции между двумя значениями типа {@code T}
 * по параметру {@code t} в диапазоне [0, 1].
 */
public interface Interpolator<T> {

	static Interpolator<Float> ofFloat() {
		return MathHelper::lerp;
	}

	static Interpolator<Float> angle(float maxDeviation) {
		return (t, a, b) -> {
			float delta = MathHelper.wrapDegrees(b - a);
			return Math.abs(delta) >= maxDeviation ? b : a + t * delta;
		};
	}

	static <T> Interpolator<T> first() {
		return (t, a, b) -> a;
	}

	static <T> Interpolator<T> threshold(float threshold) {
		return (t, a, b) -> t >= threshold ? b : a;
	}

	static Interpolator<Integer> ofColor() {
		return ColorHelper::lerp;
	}

	T apply(float t, T a, T b);
}
