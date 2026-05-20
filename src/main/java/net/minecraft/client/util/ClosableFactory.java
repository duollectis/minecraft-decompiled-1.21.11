package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ClosableFactory<T> {
   T create();

   default void prepare(T value) {
   }

   void close(T value);

   default boolean equals(ClosableFactory<?> factory) {
      return this.equals(factory);
   }
}
