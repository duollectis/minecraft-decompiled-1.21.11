package net.minecraft.nbt;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Запечатанный интерфейс для всех NBT-элементов с индексированным доступом.
 *
 * <p>Объединяет {@link NbtList}, {@link NbtByteArray}, {@link NbtIntArray} и {@link NbtLongArray}
 * под единым контрактом списка. Предоставляет стандартный {@link Iterator} и {@link Stream}
 * поверх индексированного доступа.</p>
 */
public sealed interface AbstractNbtList extends Iterable<NbtElement>, NbtElement
		permits NbtList, NbtByteArray, NbtIntArray, NbtLongArray {

	void clear();

	/**
	 * Заменяет элемент по индексу.
	 *
	 * @param index индекс заменяемого элемента
	 * @param element новый элемент
	 * @return {@code true} если замена прошла успешно (тип совместим)
	 */
	boolean setElement(int index, NbtElement element);

	/**
	 * Вставляет элемент по индексу.
	 *
	 * @param index позиция вставки
	 * @param element вставляемый элемент
	 * @return {@code true} если вставка прошла успешно (тип совместим)
	 */
	boolean addElement(int index, NbtElement element);

	/**
	 * Удаляет и возвращает элемент по индексу.
	 *
	 * @param index индекс удаляемого элемента
	 * @return удалённый элемент
	 */
	NbtElement remove(int index);

	/**
	 * Возвращает элемент по индексу.
	 *
	 * @param index индекс элемента
	 * @return элемент
	 */
	NbtElement get(int index);

	/** @return количество элементов в списке */
	int size();

	default boolean isEmpty() {
		return size() == 0;
	}

	@Override
	default Iterator<NbtElement> iterator() {
		return new Iterator<>() {
			private int cursor;

			@Override
			public boolean hasNext() {
				return cursor < AbstractNbtList.this.size();
			}

			@Override
			public NbtElement next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				return AbstractNbtList.this.get(cursor++);
			}
		};
	}

	default Stream<NbtElement> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
}
