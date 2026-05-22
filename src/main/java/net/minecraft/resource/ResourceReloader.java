package net.minecraft.resource;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Перезагрузчик ресурсов: выполняет двухфазную перезагрузку (подготовка + применение).
 * Фаза подготовки выполняется асинхронно, фаза применения — в основном потоке.
 */
@FunctionalInterface
public interface ResourceReloader {

	/**
	 * Запускает перезагрузку ресурсов.
	 *
	 * @param store              хранилище общего состояния между перезагрузчиками
	 * @param prepareExecutor    исполнитель фазы подготовки (фоновый поток)
	 * @param reloadSynchronizer синхронизатор между фазами
	 * @param applyExecutor      исполнитель фазы применения (основной поток)
	 * @return future, завершающийся по окончании обеих фаз
	 */
	CompletableFuture<Void> reload(
		ResourceReloader.Store store,
		Executor prepareExecutor,
		ResourceReloader.Synchronizer reloadSynchronizer,
		Executor applyExecutor
	);

	default void prepareSharedState(ResourceReloader.Store store) {}

	default String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Типизированный ключ для хранения общего состояния в {@link Store}.
	 *
	 * @param <T> тип значения
	 */
	final class Key<T> {}

	/**
	 * Хранилище общего состояния, передаваемого между перезагрузчиками в рамках одной перезагрузки.
	 */
	final class Store {

		private final ResourceManager resourceManager;
		private final Map<ResourceReloader.Key<?>, Object> store = new IdentityHashMap<>();

		public Store(ResourceManager resourceManager) {
			this.resourceManager = resourceManager;
		}

		public ResourceManager getResourceManager() {
			return resourceManager;
		}

		public <T> void put(ResourceReloader.Key<T> key, T value) {
			store.put(key, value);
		}

		@SuppressWarnings("unchecked")
		public <T> T getOrThrow(ResourceReloader.Key<T> key) {
			return Objects.requireNonNull((T) store.get(key));
		}
	}

	/**
	 * Синхронизатор между фазой подготовки и фазой применения.
	 * Вызов {@link #whenPrepared} сигнализирует о завершении подготовки
	 * и возвращает future, который разрешится, когда все перезагрузчики будут готовы.
	 */
	@FunctionalInterface
	interface Synchronizer {

		<T> CompletableFuture<T> whenPrepared(T preparedObject);
	}
}
