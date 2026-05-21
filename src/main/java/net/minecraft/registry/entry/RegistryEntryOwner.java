package net.minecraft.registry.entry;

/**
 * {@code RegistryEntryOwner}.
 */
public interface RegistryEntryOwner<T> {

	default boolean ownerEquals(RegistryEntryOwner<T> other) {
		return other == this;
	}
}
