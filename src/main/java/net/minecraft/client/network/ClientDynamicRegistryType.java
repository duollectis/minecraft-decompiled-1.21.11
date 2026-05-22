package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;

import java.util.List;

/**
 * Типы динамических реестров на стороне клиента.
 * <p>{@link #STATIC} содержит встроенные реестры игры, {@link #REMOTE} — реестры,
 * полученные от сервера во время фазы конфигурации.
 */
@Environment(EnvType.CLIENT)
public enum ClientDynamicRegistryType {
	STATIC,
	REMOTE;

	private static final List<ClientDynamicRegistryType> VALUES = List.of(values());
	private static final DynamicRegistryManager.Immutable STATIC_REGISTRY_MANAGER =
			DynamicRegistryManager.of(Registries.REGISTRIES);

	/**
	 * Создаёт объединённый менеджер динамических реестров с предзаполненным статическим слоем.
	 *
	 * @return объединённый менеджер реестров
	 */
	public static CombinedDynamicRegistries<ClientDynamicRegistryType> createCombinedDynamicRegistries() {
		return new CombinedDynamicRegistries<>(VALUES).with(STATIC, STATIC_REGISTRY_MANAGER);
	}
}
