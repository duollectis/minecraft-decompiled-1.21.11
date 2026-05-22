package net.minecraft.util.thread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Простейшая реализация {@link ConsecutiveExecutor} с неприоритетной очередью задач.
 * Задачи выполняются в порядке FIFO через {@link ConcurrentLinkedQueue}.
 */
public class SimpleConsecutiveExecutor extends ConsecutiveExecutor<Runnable> {

	public SimpleConsecutiveExecutor(Executor executor, String name) {
		super(new TaskQueue.Simple(new ConcurrentLinkedQueue<>()), executor, name);
	}

	@Override
	public Runnable createTask(Runnable runnable) {
		return runnable;
	}
}
