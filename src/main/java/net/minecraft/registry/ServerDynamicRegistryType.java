package net.minecraft.registry;

import java.util.List;

/**
 * Перечисляет слои динамических реестров на стороне сервера в порядке
 * их приоритета (от базового к наиболее специфичному).
 * <p>
 * Используется как типовой параметр {@link CombinedDynamicRegistries} для
 * управления многоуровневой системой реестров: статические ванильные данные →
 * генерация мира → измерения → перезагружаемые данные (лут-таблицы и т.д.).
 */
public enum ServerDynamicRegistryType {
	STATIC,
	WORLDGEN,
	DIMENSIONS,
	RELOADABLE;

	private static final List<ServerDynamicRegistryType> VALUES = List.of(values());
	private static final DynamicRegistryManager.Immutable STATIC_REGISTRY_MANAGER =
			DynamicRegistryManager.of(Registries.REGISTRIES);

	/**
	 * Создаёт начальный {@link CombinedDynamicRegistries} со всеми слоями,
	 * предварительно заполнив слой {@link #STATIC} ванильными реестрами.
	 *
	 * @return инициализированный менеджер комбинированных реестров
	 */
	public static CombinedDynamicRegistries<ServerDynamicRegistryType> createCombinedDynamicRegistries() {
		return new CombinedDynamicRegistries<>(VALUES).with(STATIC, STATIC_REGISTRY_MANAGER);
	}
}
