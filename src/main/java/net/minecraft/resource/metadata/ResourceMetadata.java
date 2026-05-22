package net.minecraft.resource.metadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.minecraft.resource.InputSupplier;
import net.minecraft.util.JsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Метаданные ресурса, хранящиеся в файле {@code pack.mcmeta} или {@code .mcmeta}-файлах ресурсов.
 *
 * <p>Предоставляет типобезопасный доступ к секциям метаданных через {@link ResourceMetadataSerializer}.
 * Константа {@link #NONE} используется как заглушка для ресурсов без метаданных.</p>
 */
public interface ResourceMetadata {

	/** Пустые метаданные — все запросы возвращают {@link Optional#empty()}. */
	ResourceMetadata NONE = new ResourceMetadata() {
		@Override
		public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
			return Optional.empty();
		}
	};

	/** Поставщик пустых метаданных для использования в {@link InputSupplier}-контекстах. */
	InputSupplier<ResourceMetadata> NONE_SUPPLIER = () -> NONE;

	/**
	 * Парсит метаданные из JSON-потока.
	 *
	 * <p>Возвращает реализацию, которая при каждом вызове {@link #decode} ищет
	 * нужную секцию в уже распарсенном {@link JsonObject} и декодирует её через кодек сериализатора.</p>
	 *
	 * @param stream входной поток с JSON-содержимым файла метаданных
	 * @return распарсенные метаданные
	 * @throws IOException если чтение потока завершилось ошибкой
	 * @throws JsonParseException если JSON некорректен
	 */
	static ResourceMetadata create(InputStream stream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			final JsonObject jsonObject = JsonHelper.deserialize(reader);

			return new ResourceMetadata() {
				@Override
				@SuppressWarnings("unchecked")
				public <T> Optional<T> decode(ResourceMetadataSerializer<T> serializer) {
					String sectionName = serializer.name();

					if (!jsonObject.has(sectionName)) {
						return Optional.empty();
					}

					T value = (T) serializer
							.codec()
							.parse(JsonOps.INSTANCE, jsonObject.get(sectionName))
							.getOrThrow(JsonParseException::new);

					return Optional.of(value);
				}
			};
		}
	}

	/**
	 * Декодирует секцию метаданных по заданному сериализатору.
	 *
	 * @param serializer сериализатор нужной секции
	 * @param <T> тип декодируемых метаданных
	 * @return декодированное значение, или {@link Optional#empty()} если секция отсутствует
	 */
	<T> Optional<T> decode(ResourceMetadataSerializer<T> serializer);

	default <T> Optional<ResourceMetadataSerializer.Value<T>> decodeAsValue(ResourceMetadataSerializer<T> serializer) {
		return decode(serializer).map(serializer::value);
	}

	/**
	 * Декодирует несколько секций метаданных и возвращает список успешно декодированных значений.
	 *
	 * @param serializers коллекция сериализаторов для декодирования
	 * @return неизменяемый список декодированных значений (секции без данных пропускаются)
	 */
	default List<ResourceMetadataSerializer.Value<?>> decode(Collection<ResourceMetadataSerializer<?>> serializers) {
		return serializers
				.stream()
				.map(this::decodeAsValue)
				.flatMap(Optional::stream)
				.collect(Collectors.toUnmodifiableList());
	}
}
