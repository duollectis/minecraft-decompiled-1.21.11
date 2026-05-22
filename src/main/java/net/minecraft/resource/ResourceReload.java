package net.minecraft.resource;

import java.util.concurrent.CompletableFuture;

/**
 * Отслеживает прогресс асинхронной перезагрузки ресурсов.
 * Предоставляет доступ к итоговому {@link CompletableFuture} и текущему прогрессу.
 */
public interface ResourceReload {

	/**
	 * Возвращает {@link CompletableFuture}, завершающийся по окончании перезагрузки.
	 *
	 * @return future перезагрузки
	 */
	CompletableFuture<?> whenComplete();

	/**
	 * Возвращает прогресс перезагрузки в диапазоне {@code [0.0, 1.0]}.
	 *
	 * @return прогресс
	 */
	float getProgress();

	default boolean isComplete() {
		return whenComplete().isDone();
	}

	/**
	 * Пробрасывает исключение, если перезагрузка завершилась с ошибкой.
	 * Вызов {@code join()} на упавшем future выбросит {@link java.util.concurrent.CompletionException}.
	 */
	default void throwException() {
		CompletableFuture<?> future = whenComplete();
		if (future.isCompletedExceptionally()) {
			future.join();
		}
	}
}
