package net.minecraft.component;

import org.jspecify.annotations.Nullable;

public interface ComponentsAccess {
   <T> @Nullable T get(ComponentType<? extends T> type);

   default <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
      T object = this.get(type);
      return object != null ? object : fallback;
   }

   default <T> @Nullable Component<T> getTyped(ComponentType<T> type) {
      T object = this.get(type);
      return object != null ? new Component<>(type, object) : null;
   }
}
