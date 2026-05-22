package net.minecraft.util.math.random;

/**
 * Базовая реализация {@link Random} на основе линейного конгруэнтного генератора.
 * Предоставляет реализации по умолчанию для всех методов через единственный
 * абстрактный метод {@link #next(int)}, возвращающий заданное количество случайных бит.
 */
public interface BaseRandom extends Random {

	float FLOAT_MULTIPLIER = 5.9604645E-8F;
	double DOUBLE_MULTIPLIER = 1.110223E-16F;

	int next(int bits);

	@Override
	default int nextInt() {
		return next(32);
	}

	@Override
	default int nextInt(int bound) {
		if (bound <= 0) {
			throw new IllegalArgumentException("Bound must be positive");
		}

		if ((bound & bound - 1) == 0) {
			return (int) ((long) bound * next(31) >> 31);
		}

		int raw;
		int result;

		do {
			raw = next(31);
			result = raw % bound;
		} while (raw - result + (bound - 1) < 0);

		return result;
	}

	@Override
	default long nextLong() {
		int hi = next(32);
		int lo = next(32);

		return ((long) hi << 32) + lo;
	}

	@Override
	default boolean nextBoolean() {
		return next(1) != 0;
	}

	@Override
	default float nextFloat() {
		return next(24) * FLOAT_MULTIPLIER;
	}

	@Override
	default double nextDouble() {
		int hi = next(26);
		int lo = next(27);

		return (((long) hi << 27) + lo) * DOUBLE_MULTIPLIER;
	}
}
