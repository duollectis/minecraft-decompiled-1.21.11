package net.minecraft.registry.entry;

public interface RegistryEntryOwner<T> {
   default boolean ownerEquals(RegistryEntryOwner<T> other) {
      return other == this;
   }
}
