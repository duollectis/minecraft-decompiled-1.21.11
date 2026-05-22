package net.minecraft.resource;

import net.minecraft.util.Unit;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Синхронный перезагрузчик ресурсов: выполняет всю работу в фазе применения (основной поток).
 * Фаза подготовки пропускается — сразу сигнализирует о готовности через синхронизатор.
 */
public interface SynchronousResourceReloader extends ResourceReloader {

	@Override
	default CompletableFuture<Void> reload(
		ResourceReloader.Store store,
		Executor prepareExecutor,
		ResourceReloader.Synchronizer synchronizer,
		Executor applyExecutor
	) {
		ResourceManager resourceManager = store.getResourceManager();
		return synchronizer.whenPrepared(Unit.INSTANCE).thenRunAsync(
			() -> {
				Profiler profiler = Profilers.get();
				profiler.push("listener");
				reload(resourceManager);
				profiler.pop();
			},
			applyExecutor
		);
	}

	/**
	 * Выполняет перезагрузку синхронно в основном потоке.
	 *
	 * @param manager менеджер ресурсов
	 */
	void reload(ResourceManager manager);
}
