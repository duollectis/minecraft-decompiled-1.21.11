package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Исполнитель задач с именованной очередью. Обеспечивает базовый контракт
 * для постановки задач в очередь и их асинхронного выполнения через {@link CompletableFuture}.
 */
public interface TaskExecutor<R extends Runnable> extends AutoCloseable {

	String getName();

	void send(R runnable);

	@Override
	default void close() {
	}

	R createTask(Runnable runnable);

	/**
	 * Ставит в очередь задачу, которая заполняет возвращаемый {@link CompletableFuture}.
	 * Позволяет вызывающему коду ожидать результата без блокировки текущего потока.
	 */
	default <Source> CompletableFuture<Source> executeAsync(Consumer<CompletableFuture<Source>> future) {
		CompletableFuture<Source> completableFuture = new CompletableFuture<>();
		send(createTask(() -> future.accept(completableFuture)));
		return completableFuture;
	}

	static TaskExecutor<Runnable> of(String name, Executor executor) {
		return new TaskExecutor<>() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public void send(Runnable runnable) {
				executor.execute(runnable);
			}

			@Override
			public Runnable createTask(Runnable runnable) {
				return runnable;
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}
}
