package net.minecraft.server;

import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.world.SaveProperties;

/**
 * Контейнер для всех ресурсов, загруженных при старте сервера: менеджер ресурсов,
 * содержимое датапаков, реестры и свойства сохранения мира.
 * Реализует {@link AutoCloseable} для корректного освобождения ресурсов.
 */
public record SaveLoader(
		LifecycledResourceManager resourceManager,
		DataPackContents dataPackContents,
		CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
		SaveProperties saveProperties
) implements AutoCloseable {

	@Override
	public void close() {
		resourceManager.close();
	}
}
