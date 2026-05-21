package net.minecraft.registry;

import java.util.List;

/**
 * {@code ServerDynamicRegistryType}.
 */
public enum ServerDynamicRegistryType {
	STATIC,
	WORLDGEN,
	DIMENSIONS,
	RELOADABLE;

	private static final List<ServerDynamicRegistryType> VALUES = List.of(values());
	private static final DynamicRegistryManager.Immutable
			STATIC_REGISTRY_MANAGER =
			DynamicRegistryManager.of(Registries.REGISTRIES);

	/**
	 * Создаёт combined dynamic registries.
	 *
	 * @return CombinedDynamicRegistries — результат операции
	 */
	public static CombinedDynamicRegistries<ServerDynamicRegistryType> createCombinedDynamicRegistries() {
		return new CombinedDynamicRegistries<>(VALUES).with(STATIC, STATIC_REGISTRY_MANAGER);
	}
}
