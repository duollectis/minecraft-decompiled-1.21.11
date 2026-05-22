package net.minecraft.util.function;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Утилитарный класс для создания функций отображения целочисленного индекса на значение перечисления.
 * Поддерживает разреженные (через хэш-карту) и плотные (через массив) индексы,
 * а также настраиваемое поведение при выходе за границы.
 */
public class ValueLists {

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T> IntFunction<T> createIndexToValueFunction(ToIntFunction<T> valueToIndexFunction, T[] values) {
		if (values.length == 0) {
			throw new IllegalArgumentException("Empty value list");
		}

		Int2ObjectMap<T> indexMap = new Int2ObjectOpenHashMap();

		for (T value : values) {
			int index = valueToIndexFunction.applyAsInt(value);
			T previous = (T) indexMap.put(index, value);

			if (previous != null) {
				throw new IllegalArgumentException(
					"Duplicate entry on id " + index + ": current=" + value + ", previous=" + previous
				);
			}
		}

		return indexMap;
	}

	public static <T> IntFunction<T> createIndexToValueFunction(
		ToIntFunction<T> valueToIndexFunction,
		T[] values,
		T fallback
	) {
		IntFunction<T> indexToValue = createIndexToValueFunction(valueToIndexFunction, values);
		return index -> Objects.requireNonNullElse(indexToValue.apply(index), fallback);
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] validate(ToIntFunction<T> valueToIndexFunction, T[] values) {
		int size = values.length;

		if (size == 0) {
			throw new IllegalArgumentException("Empty value list");
		}

		T[] indexed = (T[]) values.clone();
		Arrays.fill(indexed, null);

		for (T value : values) {
			int index = valueToIndexFunction.applyAsInt(value);

			if (index < 0 || index >= size) {
				throw new IllegalArgumentException(
					"Values are not continous, found index " + index + " for value " + value
				);
			}

			T existing = indexed[index];

			if (existing != null) {
				throw new IllegalArgumentException(
					"Duplicate entry on id " + index + ": current=" + value + ", previous=" + existing
				);
			}

			indexed[index] = value;
		}

		for (int i = 0; i < size; i++) {
			if (indexed[i] == null) {
				throw new IllegalArgumentException("Missing value at index: " + i);
			}
		}

		return indexed;
	}

	public static <T> IntFunction<T> createIndexToValueFunction(
		ToIntFunction<T> valueToIndexFunction,
		T[] values,
		OutOfBoundsHandling outOfBoundsHandling
	) {
		T[] indexed = validate(valueToIndexFunction, values);
		int size = indexed.length;

		return switch (outOfBoundsHandling) {
			case ZERO -> {
				T zeroValue = indexed[0];
				yield index -> index >= 0 && index < size ? indexed[index] : zeroValue;
			}
			case WRAP -> index -> indexed[MathHelper.floorMod(index, size)];
			case CLAMP -> index -> indexed[MathHelper.clamp(index, 0, size - 1)];
		};
	}

	/** Стратегия обработки индекса, выходящего за пределы допустимого диапазона. */
	public enum OutOfBoundsHandling {
		/** Возвращает элемент с индексом 0. */
		ZERO,
		/** Оборачивает индекс по модулю размера массива. */
		WRAP,
		/** Зажимает индекс в диапазон {@code [0, size-1]}. */
		CLAMP
	}
}
