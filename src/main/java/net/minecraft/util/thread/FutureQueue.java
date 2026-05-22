package net.minecraft.util.thread;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Очередь для последовательного выполнения колбэков, привязанных к {@link CompletableFuture}.
 * Позволяет откладывать обработку результата до момента его готовности,
 * направляя выполнение в нужный {@link Executor}.
 */
@FunctionalInterface
public interface FutureQueue {

	Logger LOGGER = LogUtils.getLogger();

	/**
	 * Создаёт реализацию, немедленно планирующую колбэк в переданный {@code executor}
	 * при завершении {@link CompletableFuture}. Ошибки логируются и не пробрасываются.
	 */
	static FutureQueue immediate(Executor executor) {
		return new FutureQueue() {
			@Override
			public <T> void append(CompletableFuture<T> completableFuture, Consumer<T> consumer) {
				completableFuture
					.thenAcceptAsync(consumer, executor)
					.exceptionally(throwable -> {
						LOGGER.error("Task failed", throwable);
						return null;
					});
			}
		};
	}

	default void append(Runnable callback) {
		append(CompletableFuture.completedFuture(null), current -> callback.run());
	}

	<T> void append(CompletableFuture<T> future, Consumer<T> callback);
}
