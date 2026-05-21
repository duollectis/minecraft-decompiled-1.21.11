package net.minecraft.registry;

@FunctionalInterface
/**
 * {@code RegistryKeyedValue}.
 */
public interface RegistryKeyedValue<T, V> {

	V get(RegistryKey<T> registryKey);

	static <T, V> RegistryKeyedValue<T, V> fixed(V value) {
		return registryKey -> value;
	}
}
