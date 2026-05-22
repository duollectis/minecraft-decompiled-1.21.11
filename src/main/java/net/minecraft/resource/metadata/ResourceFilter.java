package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Фильтр ресурсов пакета, описывающий список блокируемых идентификаторов.
 *
 * <p>Определяется в секции {@code "filter"} файла {@code pack.mcmeta}.
 * Каждая запись {@link BlockEntry} может блокировать ресурсы по пространству имён
 * и/или пути через регулярные выражения.</p>
 */
public class ResourceFilter {

	private static final Codec<ResourceFilter> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(Codec.list(BlockEntry.CODEC).fieldOf("block").forGetter(filter -> filter.blocks))
					.apply(instance, ResourceFilter::new)
	);

	public static final ResourceMetadataSerializer<ResourceFilter>
			SERIALIZER = new ResourceMetadataSerializer<>("filter", CODEC);

	private final List<BlockEntry> blocks;

	public ResourceFilter(List<BlockEntry> blocks) {
		this.blocks = List.copyOf(blocks);
	}

	/**
	 * Проверяет, заблокировано ли пространство имён хотя бы одной записью фильтра.
	 *
	 * @param namespace пространство имён для проверки
	 * @return {@code true}, если пространство имён совпадает с паттерном хотя бы одной записи
	 */
	public boolean isNamespaceBlocked(String namespace) {
		return blocks.stream().anyMatch(block -> block.getNamespacePredicate().test(namespace));
	}

	/**
	 * Проверяет, заблокирован ли путь хотя бы одной записью фильтра.
	 *
	 * @param path путь ресурса для проверки
	 * @return {@code true}, если путь совпадает с паттерном хотя бы одной записи
	 */
	public boolean isPathBlocked(String path) {
		return blocks.stream().anyMatch(block -> block.getPathPredicate().test(path));
	}
}
