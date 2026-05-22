package net.minecraft.util.collection;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.RandomAccess;

/**
 * Комбинированный интерфейс, объединяющий {@link List}, {@link Deque} и {@link RandomAccess}.
 * Предоставляет доступ к элементам как по индексу, так и через операции очереди/стека.
 *
 * @param <T> тип элементов
 */
public interface ListDeque<T> extends Serializable, Cloneable, Deque<T>, List<T>, RandomAccess {

	ListDeque<T> reversed();

	@Override
	T getFirst();

	@Override
	T getLast();

	@Override
	void addFirst(T value);

	@Override
	void addLast(T value);

	@Override
	T removeFirst();

	@Override
	T removeLast();

	@Override
	default boolean offer(T object) {
		return offerLast(object);
	}

	@Override
	default T remove() {
		return removeFirst();
	}

	@Override
	default @Nullable T poll() {
		return pollFirst();
	}

	@Override
	default T element() {
		return getFirst();
	}

	@Override
	default @Nullable T peek() {
		return peekFirst();
	}

	@Override
	default void push(T object) {
		addFirst(object);
	}

	@Override
	default T pop() {
		return removeFirst();
	}
}
