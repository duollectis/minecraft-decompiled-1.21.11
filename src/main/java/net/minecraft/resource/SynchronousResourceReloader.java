package net.minecraft.resource;

import net.minecraft.util.Unit;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@code SynchronousResourceReloader}.
 */
public interface SynchronousResourceReloader extends ResourceReloader {

	@Override
	default CompletableFuture<Void> reload(
			ResourceReloader.Store store,
			Executor executor,
			ResourceReloader.Synchronizer synchronizer,
			Executor executor2
	) {
		ResourceManager resourceManager = store.getResourceManager();
		return synchronizer.whenPrepared(Unit.INSTANCE).thenRunAsync(
				() -> {
					Profiler profiler = Profilers.get();
					profiler.push("listener");
					this.reload(resourceManager);
					profiler.pop();
				}, executor2
		);
	}

	void reload(ResourceManager manager);
}
