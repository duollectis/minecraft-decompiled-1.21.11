package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Реализация {@link ConsecutiveExecutor} с приоритетной очередью задач.
 * Задачи с меньшим числовым значением приоритета выполняются раньше.
 */
public class PrioritizedConsecutiveExecutor extends ConsecutiveExecutor<TaskQueue.PrioritizedTask> {

	public PrioritizedConsecutiveExecutor(int priorityCount, Executor executor, String name) {
		super(new TaskQueue.Prioritized(priorityCount), executor, name);
		ExecutorSampling.INSTANCE.add(this);
	}

	public TaskQueue.PrioritizedTask createTask(Runnable runnable) {
		return new TaskQueue.PrioritizedTask(0, runnable);
	}

	/**
	 * Ставит в очередь задачу с заданным приоритетом, которая заполняет переданный {@link CompletableFuture}.
	 * Позволяет вызывающему коду ожидать завершения задачи асинхронно.
	 */
	public <Source> CompletableFuture<Source> executeAsync(int priority, Consumer<CompletableFuture<Source>> future) {
		CompletableFuture<Source> completableFuture = new CompletableFuture<>();
		send(new TaskQueue.PrioritizedTask(priority, () -> future.accept(completableFuture)));
		return completableFuture;
	}
}
