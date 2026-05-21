package net.minecraft.resource;

import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code SimpleResourceReload}.
 */
public class SimpleResourceReload<S> implements ResourceReload {

	private static final int FIRST_PREPARE_APPLY_WEIGHT = 2;
	private static final int SECOND_PREPARE_APPLY_WEIGHT = 2;
	private static final int RELOADER_WEIGHT = 1;
	final CompletableFuture<Unit> prepareStageFuture = new CompletableFuture<>();
	private @Nullable CompletableFuture<List<S>> applyStageFuture;
	final Set<ResourceReloader> waitingReloaders;
	private final int reloaderCount;
	private final AtomicInteger toPrepareCount = new AtomicInteger();
	private final AtomicInteger preparedCount = new AtomicInteger();
	private final AtomicInteger toApplyCount = new AtomicInteger();
	private final AtomicInteger appliedCount = new AtomicInteger();

	public static ResourceReload create(
			ResourceManager manager,
			List<ResourceReloader> reloaders,
			Executor prepareExecutor,
			Executor applyExecutor,
			CompletableFuture<Unit> initialStage
	) {
		SimpleResourceReload<Void> simpleResourceReload = new SimpleResourceReload<>(reloaders);
		simpleResourceReload.start(
				prepareExecutor,
				applyExecutor,
				manager,
				reloaders,
				SimpleResourceReload.Factory.SIMPLE,
				initialStage
		);
		return simpleResourceReload;
	}

	protected SimpleResourceReload(List<ResourceReloader> waitingReloaders) {
		this.reloaderCount = waitingReloaders.size();
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
		this.applyStageFuture =
				this.startAsync(prepareExecutor, applyExecutor, manager, reloaders, factory, initialStage);
	}

	protected CompletableFuture<List<S>> startAsync(
			Executor prepareExecutor,
			Executor applyExecutor,
			ResourceManager manager,
			List<ResourceReloader> reloaders,
			SimpleResourceReload.Factory<S> factory,
			CompletableFuture<?> initialStage
	) {
		Executor executor = runnable -> {
			this.toPrepareCount.incrementAndGet();
			prepareExecutor.execute(() -> {
				runnable.run();
				this.preparedCount.incrementAndGet();
			});
		};
		Executor executor2 = runnable -> {
			this.toApplyCount.incrementAndGet();
			applyExecutor.execute(() -> {
				runnable.run();
				this.appliedCount.incrementAndGet();
			});
		};
		this.toPrepareCount.incrementAndGet();
		initialStage.thenRun(this.preparedCount::incrementAndGet);
		ResourceReloader.Store store = new ResourceReloader.Store(manager);
		reloaders.forEach(reloader -> reloader.prepareSharedState(store));
		CompletableFuture<?> completableFuture = initialStage;
		List<CompletableFuture<S>> list = new ArrayList<>();

		for (ResourceReloader resourceReloader : reloaders) {
			ResourceReloader.Synchronizer
					synchronizer =
					this.createSynchronizer(resourceReloader, completableFuture, applyExecutor);
			CompletableFuture<S>
					completableFuture2 =
					factory.create(store, synchronizer, resourceReloader, executor, executor2);
			list.add(completableFuture2);
			completableFuture = completableFuture2;
		}

		return Util.combine(list);
	}

	private ResourceReloader.Synchronizer createSynchronizer(
			ResourceReloader reloader,
			CompletableFuture<?> future,
			Executor applyExecutor
	) {
		return new ResourceReloader.Synchronizer() {
			@Override
			public <T> CompletableFuture<T> whenPrepared(T preparedObject) {
				applyExecutor.execute(() -> {
					SimpleResourceReload.this.waitingReloaders.remove(reloader);
					if (SimpleResourceReload.this.waitingReloaders.isEmpty()) {
						SimpleResourceReload.this.prepareStageFuture.complete(Unit.INSTANCE);
					}
				});
				return SimpleResourceReload.this.prepareStageFuture.thenCombine(
						(CompletionStage<? extends T>) future,
						(unit, object2) -> preparedObject
				);
			}
		};
	}

	@Override
	public CompletableFuture<?> whenComplete() {
		return Objects.requireNonNull(this.applyStageFuture, "not started");
	}

	@Override
	public float getProgress() {
		int i = this.reloaderCount - this.waitingReloaders.size();
		float f = toWeighted(this.preparedCount.get(), this.appliedCount.get(), i);
		float g = toWeighted(this.toPrepareCount.get(), this.toApplyCount.get(), this.reloaderCount);
		return f / g;
	}

	private static int toWeighted(int prepare, int apply, int total) {
		return prepare * 2 + apply * 2 + total * 1;
	}

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

	@FunctionalInterface
	/**
	 * {@code Factory}.
	 */
	protected interface Factory<S> {

		SimpleResourceReload.Factory<Void>
				SIMPLE =
				(store, reloadSynchronizer, reloader, prepareExecutor, applyExecutor) -> reloader.reload(
						store, prepareExecutor, reloadSynchronizer, applyExecutor
				);

		CompletableFuture<S> create(
				ResourceReloader.Store store,
				ResourceReloader.Synchronizer reloadSynchronizer,
				ResourceReloader reloader,
				Executor prepareExecutor,
				Executor applyExecutor
		);
	}
}
