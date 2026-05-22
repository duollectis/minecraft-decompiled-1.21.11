package net.minecraft.util.collection;

import com.google.common.collect.Lists;
import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Список с поддержкой значения по умолчанию. При вызове {@link #clear()}
 * все элементы заменяются на {@code initialElement} вместо удаления,
 * если начальное значение было задано при создании.
 *
 * @param <E> тип элементов списка
 */
public class DefaultedList<E> extends AbstractList<E> {

	private final List<E> delegate;
	private final @Nullable E initialElement;

	public static <E> DefaultedList<E> of() {
		return new DefaultedList<>(Lists.newArrayList(), null);
	}

	public static <E> DefaultedList<E> ofSize(int size) {
		return new DefaultedList<>(Lists.newArrayListWithCapacity(size), null);
	}

	public static <E> DefaultedList<E> ofSize(int size, E defaultValue) {
		Objects.requireNonNull(defaultValue);
		Object[] objects = new Object[size];
		Arrays.fill(objects, defaultValue);
		return new DefaultedList<>(Arrays.asList((E[]) objects), defaultValue);
	}

	/**
	 * Создаёт список из переданных значений с указанным значением по умолчанию.
	 *
	 * @param defaultValue значение, которым заполняется список при {@link #clear()}
	 * @param values       начальные элементы списка
	 */
	@SafeVarargs
	public static <E> DefaultedList<E> copyOf(E defaultValue, E... values) {
		return new DefaultedList<>(Arrays.asList(values), defaultValue);
	}

	protected DefaultedList(List<E> delegate, @Nullable E initialElement) {
		this.delegate = delegate;
		this.initialElement = initialElement;
	}

	@Override
	public E get(int index) {
		return delegate.get(index);
	}

	@Override
	public E set(int index, E element) {
		Objects.requireNonNull(element);
		return delegate.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		Objects.requireNonNull(element);
		delegate.add(index, element);
	}

	@Override
	public E remove(int index) {
		return delegate.remove(index);
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public void clear() {
		if (initialElement == null) {
			super.clear();
			return;
		}

		for (int i = 0; i < size(); i++) {
			set(i, initialElement);
		}
	}
}
