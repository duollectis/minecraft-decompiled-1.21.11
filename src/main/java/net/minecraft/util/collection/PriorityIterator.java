package net.minecraft.util.collection;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Deque;

/**
 * Итератор с приоритетами: элементы с более высоким приоритетом возвращаются первыми.
 * Внутри одного приоритета порядок FIFO. Поддерживает добавление элементов
 * в процессе итерации.
 *
 * @param <T> тип элементов
 */
public final class PriorityIterator<T> extends AbstractIterator<T> {

	private static final int LOWEST_PRIORITY = Integer.MIN_VALUE;

	private @Nullable Deque<T> maxPriorityQueue = null;
	private int maxPriority = LOWEST_PRIORITY;
	private final Int2ObjectMap<Deque<T>> queuesByPriority = new Int2ObjectOpenHashMap<>();

	/**
	 * Добавляет элемент с указанным приоритетом.
	 * Элементы с большим значением приоритета будут возвращены раньше.
	 *
	 * @param value    добавляемый элемент
	 * @param priority числовой приоритет
	 */
	public void enqueue(T value, int priority) {
		if (priority == maxPriority && maxPriorityQueue != null) {
			maxPriorityQueue.addLast(value);
			return;
		}

		Deque<T> deque = queuesByPriority.computeIfAbsent(priority, p -> Queues.newArrayDeque());
		deque.addLast(value);

		if (priority >= maxPriority) {
			maxPriorityQueue = deque;
			maxPriority = priority;
		}
	}

	@Override
	protected @Nullable T computeNext() {
		if (maxPriorityQueue == null) {
			return endOfData();
		}

		T value = maxPriorityQueue.removeFirst();

		if (value == null) {
			return endOfData();
		}

		if (maxPriorityQueue.isEmpty()) {
			refreshMaxPriority();
		}

		return value;
	}

	private void refreshMaxPriority() {
		int bestPriority = LOWEST_PRIORITY;
		Deque<T> bestQueue = null;

		for (Entry<Deque<T>> entry : Int2ObjectMaps.fastIterable(queuesByPriority)) {
			Deque<T> queue = entry.getValue();
			int priority = entry.getIntKey();

			if (priority > bestPriority && !queue.isEmpty()) {
				bestPriority = priority;
				bestQueue = queue;

				if (priority == maxPriority - 1) {
					break;
				}
			}
		}

		maxPriority = bestPriority;
		maxPriorityQueue = bestQueue;
	}
}
