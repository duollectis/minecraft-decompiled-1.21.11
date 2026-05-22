package net.minecraft.network.message;

import com.mojang.logging.LogUtils;
import net.minecraft.util.thread.FutureQueue;
import org.slf4j.Logger;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Последовательная очередь задач обработки сообщений чата.
 * Гарантирует порядок выполнения: каждая задача запускается только после завершения предыдущей.
 * При закрытии ({@link #close()}) новые задачи принимаются, но не выполняются.
 */
public class MessageChainTaskQueue implements FutureQueue, AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	private CompletableFuture<?> current = CompletableFuture.completedFuture(null);
	private final Executor executor;
	private volatile boolean closed;

	public MessageChainTaskQueue(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Добавляет задачу в конец очереди. Задача выполнится только после завершения
	 * {@code completableFuture} и всех предыдущих задач в очереди.
	 * Ошибки логируются и не прерывают обработку следующих задач.
	 * {@link CancellationException} пробрасывается без логирования.
	 *
	 * @param completableFuture будущий результат, который нужно дождаться
	 * @param consumer          обработчик результата, вызывается в {@link #executor}
	 */
	@Override
	public <T> void append(CompletableFuture<T> completableFuture, Consumer<T> consumer) {
		current = current
				.<T, Object>thenCombine(completableFuture, (ignored, result) -> result)
				.thenAcceptAsync(
						result -> {
							if (!closed) {
								consumer.accept((T) result);
							}
						},
						executor
				)
				.exceptionally(throwable -> {
					if (throwable instanceof CompletionException completionException) {
						throwable = completionException.getCause();
					}

					if (throwable instanceof CancellationException cancellationException) {
						throw cancellationException;
					}

					LOGGER.error("Chain link failed, continuing to next one", throwable);
					return null;
				});
	}

	@Override
	public void close() {
		closed = true;
	}
}
