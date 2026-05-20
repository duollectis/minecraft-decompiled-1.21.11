package net.minecraft.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

public abstract class SinglePreparationResourceReloader<T> implements ResourceReloader {
   @Override
   public final CompletableFuture<Void> reload(ResourceReloader.Store store, Executor executor, ResourceReloader.Synchronizer synchronizer, Executor executor2) {
      ResourceManager resourceManager = store.getResourceManager();
      return CompletableFuture.<T>supplyAsync(() -> this.prepare(resourceManager, Profilers.get()), executor)
         .thenCompose(synchronizer::whenPrepared)
         .thenAcceptAsync(prepared -> this.apply((T)prepared, resourceManager, Profilers.get()), executor2);
   }

   protected abstract T prepare(ResourceManager manager, Profiler profiler);

   protected abstract void apply(T prepared, ResourceManager manager, Profiler profiler);
}
