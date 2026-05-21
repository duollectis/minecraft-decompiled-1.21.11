package net.minecraft.util.collection;

import com.google.common.collect.Lists;
import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * {@code DefaultedList}.
 */
public class DefaultedList<E> extends AbstractList<E> {

	private final List<E> delegate;
	private final @Nullable E initialElement;

	/**
	 * Of.
	 *
	 * @return DefaultedList — результат операции
	 */
	public static <E> DefaultedList<E> of() {
		return new DefaultedList<>(Lists.newArrayList(), null);
	}

	/**
	 * Of size.
	 *
	 * @param size size
	 *
	 * @return DefaultedList — результат операции
	 */
	public static <E> DefaultedList<E> ofSize(int size) {
		return new DefaultedList<>(Lists.newArrayListWithCapacity(size), null);
	}

	/**
	 * Of size.
	 *
	 * @param size size
	 * @param defaultValue default value
	 *
	 * @return DefaultedList — результат операции
	 */
	public static <E> DefaultedList<E> ofSize(int size, E defaultValue) {
		Objects.requireNonNull(defaultValue);
		Object[] objects = new Object[size];
		Arrays.fill(objects, defaultValue);
		return new DefaultedList<>(Arrays.asList((E[]) objects), defaultValue);
	}

	@SafeVarargs
	/**
	 * Создаёт копию of.
	 *
	 * @param defaultValue default value
	 * @param values values
	 *
	 * @return DefaultedList — результат операции
	 */
	public static <E> DefaultedList<E> copyOf(E defaultValue, E... values) {
		return new DefaultedList<>(Arrays.asList(values), defaultValue);
	}

	protected DefaultedList(List<E> delegate, @Nullable E initialElement) {
		this.delegate = delegate;
		this.initialElement = initialElement;
	}

	@Override
	public E get(int index) {
		return this.delegate.get(index);
	}

	@Override
	public E set(int index, E element) {
		Objects.requireNonNull(element);
		return this.delegate.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		Objects.requireNonNull(element);
		this.delegate.add(index, element);
	}

	@Override
	public E remove(int index) {
		return this.delegate.remove(index);
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public void clear() {
		if (this.initialElement == null) {
			super.clear();
		}
		else {
			for (int i = 0; i < this.size(); i++) {
				this.set(i, this.initialElement);
			}
		}
	}
}
