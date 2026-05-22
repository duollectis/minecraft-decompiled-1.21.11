package net.minecraft.resource;

import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Ресурс-пак: источник ресурсов и метаданных для игры.
 * Реализации могут читать ресурсы из директорий, zip-архивов или других источников.
 */
public interface ResourcePack extends AutoCloseable {

	String METADATA_PATH_SUFFIX = ".mcmeta";
	String PACK_METADATA_NAME = "pack.mcmeta";

	/**
	 * Открывает файл из корня пака по сегментам пути.
	 *
	 * @param segments сегменты пути
	 * @return поставщик потока или {@code null}, если файл не найден
	 */
	@Nullable InputSupplier<InputStream> openRoot(String... segments);

	/**
	 * Открывает ресурс по типу и идентификатору.
	 *
	 * @param type тип ресурса
	 * @param id   идентификатор ресурса
	 * @return поставщик потока или {@code null}, если ресурс не найден
	 */
	@Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id);

	/**
	 * Перебирает все ресурсы в директории {@code prefix} для данного пространства имён.
	 *
	 * @param type      тип ресурса
	 * @param namespace пространство имён
	 * @param prefix    префикс пути
	 * @param consumer  получатель найденных ресурсов
	 */
	void findResources(ResourceType type, String namespace, String prefix, ResourcePack.ResultConsumer consumer);

	/**
	 * Возвращает все пространства имён, доступные в данном паке для указанного типа ресурса.
	 *
	 * @param type тип ресурса
	 * @return множество пространств имён
	 */
	Set<String> getNamespaces(ResourceType type);

	/**
	 * Разбирает метаданные пака из файла {@code pack.mcmeta}.
	 *
	 * @param metadataSerializer сериализатор нужной секции метаданных
	 * @return декодированный объект или {@code null}
	 * @throws IOException при ошибке чтения
	 */
	<T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException;

	/**
	 * Возвращает информацию о паке.
	 *
	 * @return информация о паке
	 */
	ResourcePackInfo getInfo();

	default String getId() {
		return getInfo().id();
	}

	default Optional<VersionedIdentifier> getKnownPackInfo() {
		return getInfo().knownPackInfo();
	}

	@Override
	void close();

	/**
	 * Функциональный интерфейс для получения результатов поиска ресурсов.
	 */
	@FunctionalInterface
	interface ResultConsumer extends BiConsumer<Identifier, InputSupplier<InputStream>> {}
}
