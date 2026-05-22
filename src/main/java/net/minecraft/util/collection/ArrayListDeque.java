package net.minecraft.util.collection;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Реализация {@link ListDeque} на основе кольцевого массива с динамическим расширением.
 * Поддерживает эффективные операции добавления/удаления с обоих концов за O(1) амортизированно.
 */
public class ArrayListDeque<T> extends AbstractList<T> implements ListDeque<T> {

	private @Nullable Object[] array;
	private int startIndex;
	private int size;

	public ArrayListDeque() {
		this(1);
	}

	public ArrayListDeque(int size) {
		array = new Object[size];
		startIndex = 0;
		this.size = 0;
	}

	@Override
	public int size() {
		return size;
	}

	@VisibleForTesting
	public int getArrayLength() {
		return array.length;
	}

	private int wrap(int index) {
		return (index + startIndex) % array.length;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T get(int index) {
		checkBounds(index);
		return getRaw(wrap(index));
	}

	private static void checkBounds(int start, int end) {
		if (start < 0 || start >= end) {
			throw new IndexOutOfBoundsException(start);
		}
	}

	private void checkBounds(int index) {
		checkBounds(index, size);
	}

	@SuppressWarnings("unchecked")
	private T getRaw(int index) {
		return (T) array[index];
	}

	@Override
	public T set(int index, T value) {
		checkBounds(index);
		Objects.requireNonNull(value);
		int wrappedIndex = wrap(index);
		T previous = getRaw(wrappedIndex);
		array[wrappedIndex] = value;
		return previous;
	}

	@Override
	public void add(int index, T value) {
		checkBounds(index, size + 1);
		Objects.requireNonNull(value);

		if (size == array.length) {
			enlarge();
		}

		int wrappedIndex = wrap(index);

		if (index == size) {
			array[wrappedIndex] = value;
		} else if (index == 0) {
			startIndex--;

			if (startIndex < 0) {
				startIndex = startIndex + array.length;
			}

			array[wrap(0)] = value;
		} else {
			for (int j = size - 1; j >= index; j--) {
				array[wrap(j + 1)] = array[wrap(j)];
			}

			array[wrappedIndex] = value;
		}

		modCount++;
		size++;
	}

	private void enlarge() {
		int newLength = array.length + Math.max(array.length >> 1, 1);
		Object[] enlarged = new Object[newLength];
		copyTo(enlarged, size);
		startIndex = 0;
		array = enlarged;
	}

	@Override
	public T remove(int index) {
		checkBounds(index);
		int wrappedIndex = wrap(index);
		T removed = getRaw(wrappedIndex);

		if (index == 0) {
			array[wrappedIndex] = null;
			startIndex++;
		} else if (index == size - 1) {
			array[wrappedIndex] = null;
		} else {
			for (int j = index + 1; j < size; j++) {
				array[wrap(j - 1)] = get(j);
			}

			array[wrap(size - 1)] = null;
		}

		modCount++;
		size--;
		return removed;
	}

	@Override
	public boolean removeIf(Predicate<? super T> predicate) {
		int removed = 0;

		for (int j = 0; j < size; j++) {
			T element = get(j);

			if (predicate.test(element)) {
				removed++;
			} else if (removed != 0) {
				array[wrap(j - removed)] = element;
				array[wrap(j)] = null;
			}
		}

		modCount += removed;
		size -= removed;
		return removed != 0;
	}

	private void copyTo(Object[] dest, int count) {
		for (int i = 0; i < count; i++) {
			dest[i] = get(i);
		}
	}

	@Override
	public void replaceAll(UnaryOperator<T> mapper) {
		for (int i = 0; i < size; i++) {
			int wrappedIndex = wrap(i);
			array[wrappedIndex] = Objects.requireNonNull(mapper.apply(getRaw(i)));
		}
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		for (int i = 0; i < size; i++) {
			consumer.accept(get(i));
		}
	}

	@Override
	public void addFirst(T value) {
		add(0, value);
	}

	@Override
	public void addLast(T value) {
		add(size, value);
	}

	@Override
	public boolean offerFirst(T value) {
		addFirst(value);
		return true;
	}

	@Override
	public boolean offerLast(T value) {
		addLast(value);
		return true;
	}

	@Override
	public T removeFirst() {
		if (size == 0) {
			throw new NoSuchElementException();
		}

		return remove(0);
	}

	@Override
	public T removeLast() {
		if (size == 0) {
			throw new NoSuchElementException();
		}

		return remove(size - 1);
	}

	@Override
	public ListDeque<T> reversed() {
		return new ReversedWrapper(this);
	}

	@Override
	public @Nullable T pollFirst() {
		return size == 0 ? null : removeFirst();
	}

	@Override
	public @Nullable T pollLast() {
		return size == 0 ? null : removeLast();
	}

	@Override
	public T getFirst() {
		if (size == 0) {
			throw new NoSuchElementException();
		}

		return get(0);
	}

	@Override
	public T getLast() {
		if (size == 0) {
			throw new NoSuchElementException();
		}

		return get(size - 1);
	}

	@Override
	public @Nullable T peekFirst() {
		return size == 0 ? null : getFirst();
	}

	@Override
	public @Nullable T peekLast() {
		return size == 0 ? null : getLast();
	}

	@Override
	public boolean removeFirstOccurrence(Object value) {
		for (int i = 0; i < size; i++) {
			if (Objects.equals(value, get(i))) {
				remove(i);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean removeLastOccurrence(Object value) {
		for (int i = size - 1; i >= 0; i--) {
			if (Objects.equals(value, get(i))) {
				remove(i);
				return true;
			}
		}

		return false;
	}

	@Override
	public Iterator<T> descendingIterator() {
		return new IteratorImpl();
	}

	class IteratorImpl implements Iterator<T> {

		private int currentIndex = ArrayListDeque.this.size() - 1;

		@Override
		public boolean hasNext() {
			return currentIndex >= 0;
		}

		@Override
		public T next() {
			return ArrayListDeque.this.get(currentIndex--);
		}

		@Override
		public void remove() {
			ArrayListDeque.this.remove(currentIndex + 1);
		}
	}

	class ReversedWrapper extends AbstractList<T> implements ListDeque<T> {

		private final ArrayListDeque<T> original;

		ReversedWrapper(ArrayListDeque<T> original) {
			this.original = original;
		}

		@Override
		public ListDeque<T> reversed() {
			return original;
		}

		@Override
		public T getFirst() {
			return original.getLast();
		}

		@Override
		public T getLast() {
			return original.getFirst();
		}

		@Override
		public void addFirst(T object) {
			original.addLast(object);
		}

		@Override
		public void addLast(T object) {
			original.addFirst(object);
		}

		@Override
		public boolean offerFirst(T value) {
			return original.offerLast(value);
		}

		@Override
		public boolean offerLast(T value) {
			return original.offerFirst(value);
		}

		@Override
		public @Nullable T pollFirst() {
			return original.pollLast();
		}

		@Override
		public @Nullable T pollLast() {
			return original.pollFirst();
		}

		@Override
		public @Nullable T peekFirst() {
			return original.peekLast();
		}

		@Override
		public @Nullable T peekLast() {
			return original.peekFirst();
		}

		@Override
		public T removeFirst() {
			return original.removeLast();
		}

		@Override
		public T removeLast() {
			return original.removeFirst();
		}

		@Override
		public boolean removeFirstOccurrence(Object value) {
			return original.removeLastOccurrence(value);
		}

		@Override
		public boolean removeLastOccurrence(Object value) {
			return original.removeFirstOccurrence(value);
		}

		@Override
		public Iterator<T> descendingIterator() {
			return original.iterator();
		}

		@Override
		public int size() {
			return original.size();
		}

		@Override
		public boolean isEmpty() {
			return original.isEmpty();
		}

		@Override
		public boolean contains(Object value) {
			return original.contains(value);
		}

		@Override
		public T get(int index) {
			return original.get(getReversedIndex(index));
		}

		@Override
		public T set(int index, T value) {
			return original.set(getReversedIndex(index), value);
		}

		@Override
		public void add(int index, T value) {
			original.add(getReversedIndex(index) + 1, value);
		}

		@Override
		public T remove(int index) {
			return original.remove(getReversedIndex(index));
		}

		@Override
		public int indexOf(Object value) {
			return getReversedIndex(original.lastIndexOf(value));
		}

		@Override
		public int lastIndexOf(Object value) {
			return getReversedIndex(original.indexOf(value));
		}

		@Override
		public List<T> subList(int start, int end) {
			return original
				.subList(getReversedIndex(end) + 1, getReversedIndex(start) + 1)
				.reversed();
		}

		@Override
		public Iterator<T> iterator() {
			return original.descendingIterator();
		}

		@Override
		public void clear() {
			original.clear();
		}

		private int getReversedIndex(int index) {
			return index == -1 ? -1 : original.size() - 1 - index;
		}
	}
}
