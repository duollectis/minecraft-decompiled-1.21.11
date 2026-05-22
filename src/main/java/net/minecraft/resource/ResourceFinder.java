package net.minecraft.resource;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Вспомогательный класс для поиска ресурсов в директории с заданным расширением файлов.
 * Преобразует пути ресурсов в идентификаторы и обратно.
 */
public class ResourceFinder {

	private final String directoryName;
	private final String fileExtension;

	public ResourceFinder(String directoryName, String fileExtension) {
		this.directoryName = directoryName;
		this.fileExtension = fileExtension;
	}

	/**
	 * Создаёт поисковик JSON-файлов в указанной директории.
	 *
	 * @param directoryName имя директории
	 * @return поисковик JSON-файлов
	 */
	public static ResourceFinder json(String directoryName) {
		return new ResourceFinder(directoryName, ".json");
	}

	/**
	 * Создаёт поисковик JSON-файлов для директории, соответствующей ключу реестра.
	 *
	 * @param registryRef ключ реестра
	 * @return поисковик JSON-файлов
	 */
	public static ResourceFinder json(RegistryKey<? extends Registry<?>> registryRef) {
		return json(RegistryKeys.getPath(registryRef));
	}

	/**
	 * Преобразует идентификатор ресурса в путь к файлу ресурса.
	 * Например: {@code minecraft:recipes/iron_sword} → {@code minecraft:recipes/iron_sword.json}
	 *
	 * @param id идентификатор ресурса
	 * @return путь к файлу ресурса
	 */
	public Identifier toResourcePath(Identifier id) {
		return id.withPath(directoryName + "/" + id.getPath() + fileExtension);
	}

	/**
	 * Преобразует путь к файлу ресурса обратно в идентификатор ресурса.
	 * Например: {@code minecraft:recipes/iron_sword.json} → {@code minecraft:iron_sword}
	 *
	 * @param path путь к файлу ресурса
	 * @return идентификатор ресурса
	 */
	public Identifier toResourceId(Identifier path) {
		String pathStr = path.getPath();
		return path.withPath(pathStr.substring(
			directoryName.length() + 1,
			pathStr.length() - fileExtension.length()
		));
	}

	/**
	 * Находит все ресурсы в директории данного поисковика.
	 *
	 * @param resourceManager менеджер ресурсов
	 * @return карта путей к ресурсам
	 */
	public Map<Identifier, Resource> findResources(ResourceManager resourceManager) {
		return resourceManager.findResources(directoryName, path -> path.getPath().endsWith(fileExtension));
	}

	/**
	 * Находит все ресурсы (включая оверлеи) в директории данного поисковика.
	 *
	 * @param resourceManager менеджер ресурсов
	 * @return карта путей к спискам ресурсов из разных паков
	 */
	public Map<Identifier, List<Resource>> findAllResources(ResourceManager resourceManager) {
		return resourceManager.findAllResources(directoryName, path -> path.getPath().endsWith(fileExtension));
	}
}
