package net.minecraft.util.function;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;

import java.util.function.Function;

/**
 * Функция, отображающая значение типа {@code C} в примитивный {@code float}.
 * Дополнительно предоставляет контракт диапазона через {@link #min()} и {@link #max()}.
 */
public interface ToFloatFunction<C> {

	ToFloatFunction<Float> IDENTITY = fromFloat(value -> value);

	float apply(C x);

	float min();

	float max();

	/**
	 * Создаёт {@code ToFloatFunction<Float>} на основе примитивной функции {@code float → float}.
	 * Диапазон результата не ограничен: {@code [-∞, +∞]}.
	 *
	 * @param delegate примитивная функция преобразования
	 * @return обёртка над {@code delegate}
	 */
	static ToFloatFunction<Float> fromFloat(Float2FloatFunction delegate) {
		return new ToFloatFunction<>() {
			@Override
			public float apply(Float value) {
				return delegate.apply(value);
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
		ToFloatFunction<C> outer = this;
		return new ToFloatFunction<>() {
			@Override
			public float apply(C2 x) {
				return outer.apply(before.apply(x));
			}

			@Override
			public float min() {
				return outer.min();
			}

			@Override
			public float max() {
				return outer.max();
			}
		};
	}
}
