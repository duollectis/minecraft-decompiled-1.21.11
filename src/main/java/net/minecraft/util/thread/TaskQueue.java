package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Интерфейс очереди задач для {@link ConsecutiveExecutor}.
 * Предоставляет две реализации: {@link Simple} (FIFO без приоритетов)
 * и {@link Prioritized} (несколько очередей с числовым приоритетом).
 */
public interface TaskQueue<T extends Runnable> {

	@Nullable Runnable poll();

	boolean add(T runnable);

	boolean isEmpty();

	int getSize();

	/**
	 * Приоритетная очередь задач. Хранит {@code priorityCount} отдельных очередей;
	 * задачи с меньшим индексом приоритета извлекаются первыми.
	 */
	final class Prioritized implements TaskQueue<TaskQueue.PrioritizedTask> {

		private final Queue<Runnable>[] queue;
		private final AtomicInteger queueSize = new AtomicInteger();

		public Prioritized(int priorityCount) {
			queue = new Queue[priorityCount];

			for (int i = 0; i < priorityCount; i++) {
				queue[i] = Queues.newConcurrentLinkedQueue();
			}
		}

		@Override
		public @Nullable Runnable poll() {
			for (Queue<Runnable> bucket : queue) {
				Runnable runnable = bucket.poll();

				if (runnable != null) {
					queueSize.decrementAndGet();
					return runnable;
				}
			}

			return null;
		}

		@Override
		public boolean add(PrioritizedTask prioritizedTask) {
			int priority = prioritizedTask.priority();

			if (priority < 0 || priority >= queue.length) {
				throw new IndexOutOfBoundsException(String.format(
					Locale.ROOT,
					"Priority %d not supported. Expected range [0-%d]",
					priority,
					queue.length - 1
				));
			}

			queue[priority].add(prioritizedTask);
			queueSize.incrementAndGet();
			return true;
		}

		@Override
		public boolean isEmpty() {
			return queueSize.get() == 0;
		}

		@Override
		public int getSize() {
			return queueSize.get();
		}
	}

	/**
	 * Задача с числовым приоритетом для использования в {@link Prioritized}.
	 * Меньшее значение {@code priority} означает более высокий приоритет.
	 */
	record PrioritizedTask(int priority, Runnable runnable) implements Runnable {

		@Override
		public void run() {
			runnable.run();
		}
	}

	/**
	 * Простая FIFO-очередь без приоритетов, делегирующая к произвольной {@link Queue}.
	 */
	final class Simple implements TaskQueue<Runnable> {

		private final Queue<Runnable> queue;

		public Simple(Queue<Runnable> queue) {
			this.queue = queue;
		}

		@Override
		public @Nullable Runnable poll() {
			return queue.poll();
		}

		@Override
		public boolean add(Runnable runnable) {
			return queue.add(runnable);
		}

		@Override
		public boolean isEmpty() {
			return queue.isEmpty();
		}

		@Override
		public int getSize() {
			return queue.size();
		}
	}
}
