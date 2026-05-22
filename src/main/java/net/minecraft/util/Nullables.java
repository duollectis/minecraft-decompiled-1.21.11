package net.minecraft.util;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Утилитарные методы для безопасной работы с null-значениями.
 * Дополняет {@link Objects} и {@link java.util.Optional} для случаев,
 * когда использование Optional нецелесообразно по соображениям производительности.
 */
public class Nullables {

	/**
	 * Применяет функцию к значению, если оно не null.
	 *
	 * @param value исходное значение (может быть null)
	 * @param mapper функция преобразования
	 * @return результат применения функции или null
	 */
	public static <T, R> @Nullable R map(@Nullable T value, Function<T, R> mapper) {
		return value == null ? null : mapper.apply(value);
	}

	/**
	 * Применяет функцию к значению, если оно не null, иначе возвращает запасное значение.
	 *
	 * @param value исходное значение (может быть null)
	 * @param mapper функция преобразования
	 * @param other запасное значение
	 * @return результат применения функции или {@code other}
	 */
	public static <T, R> R mapOrElse(@Nullable T value, Function<T, R> mapper, R other) {
		return value == null ? other : mapper.apply(value);
	}

	/**
	 * Применяет функцию к значению, если оно не null, иначе вызывает поставщика запасного значения.
	 *
	 * @param value исходное значение (может быть null)
	 * @param mapper функция преобразования
	 * @param getter поставщик запасного значения
	 * @return результат применения функции или результат вызова {@code getter}
	 */
	public static <T, R> R mapOrElseGet(@Nullable T value, Function<T, R> mapper, Supplier<R> getter) {
		return value == null ? getter.get() : mapper.apply(value);
	}

	public static <T> @Nullable T getFirst(Collection<T> collection) {
		Iterator<T> iterator = collection.iterator();
		return iterator.hasNext() ? iterator.next() : null;
	}

	public static <T> T getFirstOrElse(Collection<T> collection, T defaultValue) {
		Iterator<T> iterator = collection.iterator();
		return iterator.hasNext() ? iterator.next() : defaultValue;
	}

	public static <T> T getFirstOrElseGet(Collection<T> collection, Supplier<T> getter) {
		Iterator<T> iterator = collection.iterator();
		return iterator.hasNext() ? iterator.next() : getter.get();
	}

	public static <T> boolean isEmpty(T @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(boolean @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(byte @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(char @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(short @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(int @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(long @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(float @Nullable [] array) {
		return array == null || array.length == 0;
	}

	public static boolean isEmpty(double @Nullable [] array) {
		return array == null || array.length == 0;
	}

	@Deprecated
	public static <T> T requireNonNullElse(@Nullable T first, T second) {
		return Objects.requireNonNullElse(first, second);
	}
}
