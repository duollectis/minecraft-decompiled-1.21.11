package net.minecraft.util.collection;

import org.jspecify.annotations.Nullable;

/**
 * Итерируемая коллекция, элементы которой имеют целочисленные идентификаторы.
 * Обеспечивает двустороннее отображение: объект ↔ числовой ID.
 *
 * @param <T> тип элементов коллекции
 */
public interface IndexedIterable<T> extends Iterable<T> {

	int ABSENT_RAW_ID = -1;

	int getRawId(T value);

	@Nullable T get(int index);

	/**
	 * Возвращает элемент по ID или бросает исключение, если элемент не найден.
	 *
	 * @param index числовой идентификатор элемента
	 * @return элемент с указанным ID
	 * @throws IllegalArgumentException если элемент с таким ID отсутствует
	 */
	default T getOrThrow(int index) {
		T value = get(index);
		if (value == null) {
			throw new IllegalArgumentException("No value with id " + index);
		}

		return value;
	}

	/**
	 * Возвращает числовой ID элемента или бросает исключение, если элемент не зарегистрирован.
	 *
	 * @param value элемент для поиска
	 * @return числовой ID элемента
	 * @throws IllegalArgumentException если элемент не найден в коллекции
	 */
	default int getRawIdOrThrow(T value) {
		int id = getRawId(value);
		if (id == ABSENT_RAW_ID) {
			throw new IllegalArgumentException("Can't find id for '" + value + "' in map " + this);
		}

		return id;
	}

	int size();
}
