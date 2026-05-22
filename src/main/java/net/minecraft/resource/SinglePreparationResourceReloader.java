package net.minecraft.resource;

import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Базовый перезагрузчик с единственной фазой подготовки.
 * Подготовка выполняется асинхронно, применение — в основном потоке.
 *
 * @param <T> тип подготовленных данных
 */
public abstract class SinglePreparationResourceReloader<T> implements ResourceReloader {

	@Override
	public final CompletableFuture<Void> reload(
		ResourceReloader.Store store,
		Executor prepareExecutor,
		ResourceReloader.Synchronizer synchronizer,
		Executor applyExecutor
	) {
		ResourceManager resourceManager = store.getResourceManager();
		return CompletableFuture.<T>supplyAsync(() -> prepare(resourceManager, Profilers.get()), prepareExecutor)
			.thenCompose(synchronizer::whenPrepared)
			.thenAcceptAsync(
				prepared -> apply(prepared, resourceManager, Profilers.get()),
				applyExecutor
			);
	}

	/**
	 * Фаза подготовки: загружает и обрабатывает данные асинхронно.
	 *
	 * @param manager  менеджер ресурсов
	 * @param profiler профилировщик
	 * @return подготовленные данные
	 */
	protected abstract T prepare(ResourceManager manager, Profiler profiler);

	/**
	 * Фаза применения: применяет подготовленные данные в основном потоке.
	 *
	 * @param prepared подготовленные данные
	 * @param manager  менеджер ресурсов
	 * @param profiler профилировщик
	 */
	protected abstract void apply(T prepared, ResourceManager manager, Profiler profiler);
}
