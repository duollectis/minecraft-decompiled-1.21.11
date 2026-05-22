package net.minecraft.server.dedicated.management;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Интерфейс для асинхронной отправки задач на выполнение.
 * <p>
 * Используется в серверной инфраструктуре для делегирования работы
 * в определённый поток или пул потоков (например, главный серверный поток).
 * Позволяет безопасно передавать задачи между потоками через {@link java.util.concurrent.CompletableFuture}.
 */
public interface Submitter {

	<V> CompletableFuture<V> submit(Supplier<V> task);

	CompletableFuture<Void> submit(Runnable task);
}
