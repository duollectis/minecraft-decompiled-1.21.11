package net.minecraft.util.collection;

import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Утилитарный класс для работы со взвешенными коллекциями.
 * Предоставляет методы для подсчёта суммы весов и случайного выбора элемента
 * с учётом весов.
 */
public class Weighting {

	private Weighting() {
	}

	/**
	 * Вычисляет сумму весов всех элементов пула.
	 *
	 * @param pool         список элементов
	 * @param weightGetter функция извлечения веса из элемента
	 * @return сумма весов как {@code int}
	 * @throws IllegalArgumentException если сумма превышает {@link Integer#MAX_VALUE}
	 */
	public static <T> int getWeightSum(List<T> pool, ToIntFunction<T> weightGetter) {
		long sum = 0L;

		for (T element : pool) {
			sum += weightGetter.applyAsInt(element);
		}

		if (sum > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
		}

		return (int) sum;
	}

	/**
	 * Выбирает случайный элемент из пула с учётом весов.
	 *
	 * @param random      источник случайности
	 * @param pool        список элементов
	 * @param totalWeight предварительно вычисленная сумма весов
	 * @param weightGetter функция извлечения веса
	 * @return случайный элемент или {@link Optional#empty()} если пул пуст
	 * @throws IllegalArgumentException если {@code totalWeight} отрицателен
	 */
	public static <T> Optional<T> getRandom(
		Random random,
		List<T> pool,
		int totalWeight,
		ToIntFunction<T> weightGetter
	) {
		if (totalWeight < 0) {
			throw (IllegalArgumentException) Util.getFatalOrPause(
				new IllegalArgumentException("Negative total weight in getRandomItem")
			);
		}

		if (totalWeight == 0) {
			return Optional.empty();
		}

		return getAt(pool, random.nextInt(totalWeight), weightGetter);
	}

	public static <T> Optional<T> getAt(List<T> pool, int totalWeight, ToIntFunction<T> weightGetter) {
		for (T element : pool) {
			totalWeight -= weightGetter.applyAsInt(element);

			if (totalWeight < 0) {
				return Optional.of(element);
			}
		}

		return Optional.empty();
	}

	public static <T> Optional<T> getRandom(Random random, List<T> pool, ToIntFunction<T> weightGetter) {
		return getRandom(random, pool, getWeightSum(pool, weightGetter), weightGetter);
	}
}
