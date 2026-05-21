package net.minecraft.util.math;

/**
 * {@code Interpolator}.
 */
public interface Interpolator<T> {

	static Interpolator<Float> ofFloat() {
		return MathHelper::lerp;
	}

	static Interpolator<Float> angle(float maxDeviation) {
		return (t, a, b) -> {
			float g = MathHelper.wrapDegrees(b - a);
			return Math.abs(g) >= maxDeviation ? b : a + t * g;
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
