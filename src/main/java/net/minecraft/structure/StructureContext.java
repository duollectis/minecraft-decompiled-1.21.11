package net.minecraft.structure;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Контекст, передаваемый при сериализации и десериализации структурных фрагментов.
 * Объединяет менеджер ресурсов, реестр и менеджер шаблонов структур в единый объект,
 * чтобы не передавать их по отдельности в каждый метод чтения/записи NBT.
 */
public record StructureContext(
	ResourceManager resourceManager,
	DynamicRegistryManager registryManager,
	StructureTemplateManager structureTemplateManager
) {

	/**
	 * Создаёт контекст из серверного мира, извлекая все необходимые зависимости
	 * из {@link MinecraftServer}, привязанного к этому миру.
	 *
	 * @param world серверный мир, из которого берётся сервер
	 * @return готовый контекст для работы со структурами
	 */
	public static StructureContext from(ServerWorld world) {
		MinecraftServer server = world.getServer();
		return new StructureContext(
			server.getResourceManager(),
			server.getRegistryManager(),
			server.getStructureTemplateManager()
		);
	}
}
