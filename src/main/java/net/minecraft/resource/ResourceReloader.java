package net.minecraft.resource;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface ResourceReloader {
   CompletableFuture<Void> reload(
      ResourceReloader.Store store, Executor prepareExecutor, ResourceReloader.Synchronizer reloadSynchronizer, Executor applyExecutor
   );

   default void prepareSharedState(ResourceReloader.Store store) {
   }

   default String getName() {
      return this.getClass().getSimpleName();
   }

   public static final class Key<T> {
   }

   public static final class Store {
      private final ResourceManager resourceManager;
      private final Map<ResourceReloader.Key<?>, Object> store = new IdentityHashMap<>();

      public Store(ResourceManager resourceManager) {
         this.resourceManager = resourceManager;
      }

      public ResourceManager getResourceManager() {
         return this.resourceManager;
      }

      public <T> void put(ResourceReloader.Key<T> key, T value) {
         this.store.put(key, value);
      }

      public <T> T getOrThrow(ResourceReloader.Key<T> key) {
         return Objects.requireNonNull((T)this.store.get(key));
      }
   }

   @FunctionalInterface
   public interface Synchronizer {
      <T> CompletableFuture<T> whenPrepared(T preparedObject);
   }
}
