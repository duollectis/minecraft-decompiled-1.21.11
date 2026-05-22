package net.minecraft.resource;

import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Базовая реализация {@link ResourceReload}, выполняющая двухфазную перезагрузку
 * всех зарегистрированных {@link ResourceReloader} с отслеживанием прогресса.
 *
 * <p>Прогресс вычисляется как взвешенное отношение выполненных задач к общему числу,
 * где фазы подготовки и применения имеют вес {@value PHASE_WEIGHT},
 * а каждый перезагрузчик — вес {@value RELOADER_WEIGHT}.
 *
 * @param <S> тип результата каждого перезагрузчика
 */
public class SimpleResourceReload<S> implements ResourceReload {

	private static final int PHASE_WEIGHT = 2;
	private static final int RELOADER_WEIGHT = 1;

	final CompletableFuture<Unit> prepareStageFuture = new CompletableFuture<>();
	private @Nullable CompletableFuture<List<S>> applyStageFuture;
	final Set<ResourceReloader> waitingReloaders;
	private final int reloaderCount;
	private final AtomicInteger toPrepareCount = new AtomicInteger();
	private final AtomicInteger preparedCount = new AtomicInteger();
	private final AtomicInteger toApplyCount = new AtomicInteger();
	private final AtomicInteger appliedCount = new AtomicInteger();

	/**
	 * Создаёт и запускает простую (непрофилируемую) перезагрузку.
	 *
	 * @param manager         менеджер ресурсов
	 * @param reloaders       список перезагрузчиков
	 * @param prepareExecutor исполнитель фазы подготовки
	 * @param applyExecutor   исполнитель фазы применения
	 * @param initialStage    начальная стадия (барьер синхронизации)
	 * @return объект отслеживания прогресса
	 */
	public static ResourceReload create(
		ResourceManager manager,
		List<ResourceReloader> reloaders,
		Executor prepareExecutor,
		Executor applyExecutor,
		CompletableFuture<Unit> initialStage
	) {
		SimpleResourceReload<Void> reload = new SimpleResourceReload<>(reloaders);
		reload.start(prepareExecutor, applyExecutor, manager, reloaders, Factory.SIMPLE, initialStage);
		return reload;
	}

	/**
	 * Создаёт и запускает перезагрузку — простую или профилируемую в зависимости от флага.
	 *
	 * @param manager         менеджер ресурсов
	 * @param reloaders       список перезагрузчиков
	 * @param prepareExecutor исполнитель фазы подготовки
	 * @param applyExecutor   исполнитель фазы применения
	 * @param initialStage    начальная стадия
	 * @param profiled        {@code true} для профилируемой перезагрузки
	 * @return объект отслеживания прогресса
	 */
	public static ResourceReload start(
		ResourceManager manager,
		List<ResourceReloader> reloaders,
		Executor prepareExecutor,
		Executor applyExecutor,
		CompletableFuture<Unit> initialStage,
		boolean profiled
	) {
		return profiled
			? ProfiledResourceReload.start(manager, reloaders, prepareExecutor, applyExecutor, initialStage)
			: create(manager, reloaders, prepareExecutor, applyExecutor, initialStage);
	}

	protected SimpleResourceReload(List<ResourceReloader> waitingReloaders) {
		reloaderCount = waitingReloaders.size();
		this.waitingReloaders = new HashSet<>(waitingReloaders);
	}

	protected void start(
		Executor prepareExecutor,
		Executor applyExecutor,
		ResourceManager manager,
		List<ResourceReloader> reloaders,
		SimpleResourceReload.Factory<S> factory,
		CompletableFuture<?> initialStage
	) {
		applyStageFuture = startAsync(prepareExecutor, applyExecutor, manager, reloaders, factory, initialStage);
	}

	/**
	 * Запускает асинхронную перезагрузку и возвращает future со списком результатов.
	 * Оборачивает исполнители для подсчёта задач (прогресс).
	 */
	protected CompletableFuture<List<S>> startAsync(
		Executor prepareExecutor,
		Executor applyExecutor,
		ResourceManager manager,
		List<ResourceReloader> reloaders,
		SimpleResourceReload.Factory<S> factory,
		CompletableFuture<?> initialStage
	) {
		Executor countingPrepareExecutor = runnable -> {
			toPrepareCount.incrementAndGet();
			prepareExecutor.execute(() -> {
				runnable.run();
				preparedCount.incrementAndGet();
			});
		};

		Executor countingApplyExecutor = runnable -> {
			toApplyCount.incrementAndGet();
			applyExecutor.execute(() -> {
				runnable.run();
				appliedCount.incrementAndGet();
			});
		};

		toPrepareCount.incrementAndGet();
		initialStage.thenRun(preparedCount::incrementAndGet);

		ResourceReloader.Store store = new ResourceReloader.Store(manager);
		reloaders.forEach(reloader -> reloader.prepareSharedState(store));

		CompletableFuture<?> barrier = initialStage;
		List<CompletableFuture<S>> futures = new ArrayList<>();

		for (ResourceReloader reloader : reloaders) {
			ResourceReloader.Synchronizer synchronizer = createSynchronizer(reloader, barrier, applyExecutor);
			CompletableFuture<S> future = factory.create(
				store,
				synchronizer,
				reloader,
				countingPrepareExecutor,
				countingApplyExecutor
			);
			futures.add(future);
			barrier = future;
		}

		return Util.combine(futures);
	}

	private ResourceReloader.Synchronizer createSynchronizer(
		ResourceReloader reloader,
		CompletableFuture<?> barrier,
		Executor applyExecutor
	) {
		return new ResourceReloader.Synchronizer() {
			@Override
			public <T> CompletableFuture<T> whenPrepared(T preparedObject) {
				applyExecutor.execute(() -> {
					waitingReloaders.remove(reloader);
					if (waitingReloaders.isEmpty()) {
						prepareStageFuture.complete(Unit.INSTANCE);
					}
				});

				return prepareStageFuture.thenCombine(
					(CompletionStage<? extends T>) barrier,
					(unit, ignored) -> preparedObject
				);
			}
		};
	}

	@Override
	public CompletableFuture<?> whenComplete() {
		return Objects.requireNonNull(applyStageFuture, "not started");
	}

	@Override
	public float getProgress() {
		int completed = reloaderCount - waitingReloaders.size();
		float done = toWeighted(preparedCount.get(), appliedCount.get(), completed);
		float total = toWeighted(toPrepareCount.get(), toApplyCount.get(), reloaderCount);
		return done / total;
	}

	private static int toWeighted(int prepare, int apply, int reloaders) {
		return prepare * PHASE_WEIGHT + apply * PHASE_WEIGHT + reloaders * RELOADER_WEIGHT;
	}

	/**
	 * Фабрика для создания {@link CompletableFuture} одного перезагрузчика.
	 *
	 * @param <S> тип результата
	 */
	@FunctionalInterface
	protected interface Factory<S> {

		Factory<Void> SIMPLE = (store, synchronizer, reloader, prepareExecutor, applyExecutor) ->
			reloader.reload(store, prepareExecutor, synchronizer, applyExecutor);

		CompletableFuture<S> create(
			ResourceReloader.Store store,
			ResourceReloader.Synchronizer reloadSynchronizer,
			ResourceReloader reloader,
			Executor prepareExecutor,
			Executor applyExecutor
		);
	}
}
