package net.minecraft.resource.metadata;

import java.util.Map;

/**
 * Иммутабельная карта предварительно декодированных метаданных ресурса.
 *
 * <p>Используется для хранения метаданных встроенных пакетов (например, ванильного пакета данных),
 * где метаданные известны заранее и не требуют парсинга JSON.
 * Доступ к значениям осуществляется через {@link ResourceMetadataSerializer} как ключ.</p>
 */
public class ResourceMetadataMap {

	private static final ResourceMetadataMap EMPTY = new ResourceMetadataMap(Map.of());

	private final Map<ResourceMetadataSerializer<?>, ?> values;

	private ResourceMetadataMap(Map<ResourceMetadataSerializer<?>, ?> values) {
		this.values = values;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(ResourceMetadataSerializer<T> serializer) {
		return (T) values.get(serializer);
	}

	public static ResourceMetadataMap of() {
		return EMPTY;
	}

	public static <T> ResourceMetadataMap of(ResourceMetadataSerializer<T> serializer, T value) {
		return new ResourceMetadataMap(Map.of(serializer, value));
	}

	@SuppressWarnings("unchecked")
	public static <T1, T2> ResourceMetadataMap of(
			ResourceMetadataSerializer<T1> serializer1,
			T1 value1,
			ResourceMetadataSerializer<T2> serializer2,
			T2 value2
	) {
		return new ResourceMetadataMap(Map.of(serializer1, value1, serializer2, (T1) value2));
	}
}
