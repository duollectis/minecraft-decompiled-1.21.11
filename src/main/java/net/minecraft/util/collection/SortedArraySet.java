package net.minecraft.util.collection;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Отсортированное множество на основе массива с бинарным поиском.
 * Обеспечивает O(log n) поиск и O(n) вставку/удаление.
 * Подходит для небольших множеств, где важна компактность памяти.
 *
 * @param <T> тип элементов
 */
public class SortedArraySet<T> extends AbstractSet<T> {

	private static final int DEFAULT_CAPACITY = 10;

	private final Comparator<T> comparator;
	T[] elements;
	int size;

	private SortedArraySet(int initialCapacity, Comparator<T> comparator) {
		this.comparator = comparator;
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Initial capacity (" + initialCapacity + ") is negative");
		}

		elements = cast(new Object[initialCapacity]);
	}

	public static <T extends Comparable<T>> SortedArraySet<T> create() {
		return create(DEFAULT_CAPACITY);
	}

	public static <T extends Comparable<T>> SortedArraySet<T> create(int initialCapacity) {
		return new SortedArraySet<T>(initialCapacity, Comparator.<T>naturalOrder());
	}

	public static <T> SortedArraySet<T> create(Comparator<T> comparator) {
		return create(comparator, DEFAULT_CAPACITY);
	}

	public static <T> SortedArraySet<T> create(Comparator<T> comparator, int initialCapacity) {
		return new SortedArraySet<>(initialCapacity, comparator);
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] cast(Object[] array) {
		return (T[]) array;
	}

	private int binarySearch(T object) {
		return Arrays.binarySearch(elements, 0, size, object, comparator);
	}

	private static int insertionPoint(int binarySearchResult) {
		return -binarySearchResult - 1;
	}

	@Override
	public boolean add(T object) {
		int searchResult = binarySearch(object);
		if (searchResult >= 0) {
			return false;
		}

		add(object, insertionPoint(searchResult));
		return true;
	}

	private void ensureCapacity(int minCapacity) {
		if (minCapacity <= elements.length) {
			return;
		}

		if (elements != ObjectArrays.DEFAULT_EMPTY_ARRAY) {
			minCapacity = Util.nextCapacity(elements.length, minCapacity);
		} else if (minCapacity < DEFAULT_CAPACITY) {
			minCapacity = DEFAULT_CAPACITY;
		}

		Object[] grown = new Object[minCapacity];
		System.arraycopy(elements, 0, grown, 0, size);
		elements = cast(grown);
	}

	private void add(T object, int index) {
		ensureCapacity(size + 1);

		if (index != size) {
			System.arraycopy(elements, index, elements, index + 1, size - index);
		}

		elements[index] = object;
		size++;
	}

	void remove(int index) {
		size--;

		if (index != size) {
			System.arraycopy(elements, index + 1, elements, index, size - index);
		}

		elements[size] = null;
	}

	private T get(int index) {
		return elements[index];
	}

	/**
	 * Добавляет элемент и возвращает существующий эквивалент или сам добавленный элемент.
	 * Полезно для интернирования объектов по значению.
	 *
	 * @param object элемент для добавления
	 * @return существующий элемент если уже присутствует, иначе {@code object}
	 */
	public T addAndGet(T object) {
		int searchResult = binarySearch(object);
		if (searchResult >= 0) {
			return get(searchResult);
		}

		add(object, insertionPoint(searchResult));
		return object;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object object) {
		int searchResult = binarySearch((T) object);
		if (searchResult < 0) {
			return false;
		}

		remove(searchResult);
		return true;
	}

	public @Nullable T getIfContains(T object) {
		int searchResult = binarySearch(object);
		return searchResult >= 0 ? get(searchResult) : null;
	}

	public T first() {
		return get(0);
	}

	public T last() {
		return get(size - 1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(Object object) {
		return binarySearch((T) object) >= 0;
	}

	@Override
	public Iterator<T> iterator() {
		return new SetIterator();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(elements, size, Object[].class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U> U[] toArray(U[] array) {
		if (array.length < size) {
			return (U[]) Arrays.copyOf(elements, size, array.getClass());
		}

		System.arraycopy(elements, 0, array, 0, size);

		if (array.length > size) {
			array[size] = null;
		}

		return array;
	}

	@Override
	public void clear() {
		Arrays.fill(elements, 0, size, null);
		size = 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof SortedArraySet<?> otherSet && comparator.equals(otherSet.comparator)) {
			return size == otherSet.size && Arrays.equals(elements, otherSet.elements);
		}

		return super.equals(other);
	}

	class SetIterator implements Iterator<T> {

		private int nextIndex;
		private int lastIndex = -1;

		@Override
		public boolean hasNext() {
			return nextIndex < SortedArraySet.this.size;
		}

		@Override
		public T next() {
			if (nextIndex >= SortedArraySet.this.size) {
				throw new NoSuchElementException();
			}

			lastIndex = nextIndex++;
			return SortedArraySet.this.elements[lastIndex];
		}

		@Override
		public void remove() {
			if (lastIndex == -1) {
				throw new IllegalStateException();
			}

			SortedArraySet.this.remove(lastIndex);
			nextIndex--;
			lastIndex = -1;
		}
	}
}
