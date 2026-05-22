package net.minecraft.util;

import org.jspecify.annotations.Nullable;

/**
 * Накопитель исключений с отложенной доставкой.
 * Первое добавленное исключение становится основным; все последующие добавляются
 * как подавленные ({@link Throwable#addSuppressed}) к первому.
 * Позволяет собрать все ошибки из цикла и выбросить их одним вызовом {@link #deliver()}.
 *
 * @param <T> тип накапливаемого исключения
 */
public class ThrowableDeliverer<T extends Throwable> {

	private @Nullable T throwable;

	/**
	 * Добавляет исключение. Первое становится основным, остальные — подавленными.
	 *
	 * @param throwable исключение для добавления
	 */
	public void add(T throwable) {
		if (this.throwable == null) {
			this.throwable = throwable;
		} else {
			this.throwable.addSuppressed(throwable);
		}
	}

	/**
	 * Выбрасывает накопленное исключение, если оно есть.
	 *
	 * @throws T если было добавлено хотя бы одно исключение
	 */
	public void deliver() throws T {
		if (throwable != null) {
			throw throwable;
		}
	}
}
