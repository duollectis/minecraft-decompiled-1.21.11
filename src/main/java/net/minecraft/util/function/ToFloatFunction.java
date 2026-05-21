package net.minecraft.util.function;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;

import java.util.function.Function;

/**
 * {@code ToFloatFunction}.
 */
public interface ToFloatFunction<C> {

	ToFloatFunction<Float> IDENTITY = fromFloat(value -> value);

	float apply(C x);

	float min();

	float max();

	static ToFloatFunction<Float> fromFloat(Float2FloatFunction delegate) {
		return new ToFloatFunction<Float>() {
			public float apply(Float float_) {
				return (Float) delegate.apply(float_);
			}

			@Override
			public float min() {
				return Float.NEGATIVE_INFINITY;
			}

			@Override
			public float max() {
				return Float.POSITIVE_INFINITY;
			}
		};
	}

	default <C2> ToFloatFunction<C2> compose(Function<C2, C> before) {
		final ToFloatFunction<C> toFloatFunction = this;
		return new ToFloatFunction<C2>() {
			@Override
			public float apply(C2 x) {
				return toFloatFunction.apply(before.apply(x));
			}

			@Override
			public float min() {
				return toFloatFunction.min();
			}

			@Override
			public float max() {
				return toFloatFunction.max();
			}
		};
	}
}
