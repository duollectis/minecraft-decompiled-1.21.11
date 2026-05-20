package net.minecraft.component;

import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface ComponentHolder extends ComponentsAccess {
   ComponentMap getComponents();

   @Override
   default <T> @Nullable T get(ComponentType<? extends T> type) {
      return this.getComponents().get(type);
   }

   default <T> Stream<T> streamAll(Class<? extends T> valueClass) {
      return this.getComponents().stream().map(Component::value).filter(value -> valueClass.isAssignableFrom(value.getClass())).map(value -> (T)value);
   }

   @Override
   default <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
      return this.getComponents().getOrDefault(type, fallback);
   }

   default boolean contains(ComponentType<?> type) {
      return this.getComponents().contains(type);
   }
}
