package net.minecraft.resource.metadata;

import com.mojang.serialization.Codec;

import java.util.Optional;

/**
 * Дескриптор секции метаданных ресурса: связывает имя JSON-ключа с кодеком для декодирования.
 *
 * <p>Используется как ключ в {@link ResourceMetadataMap} и как параметр в
 * {@link ResourceMetadata#decode(ResourceMetadataSerializer)}.</p>
 *
 * @param name имя секции в JSON-файле метаданных (например, {@code "pack"}, {@code "filter"})
 * @param codec кодек для декодирования значения секции
 * @param <T> тип декодируемых метаданных
 */
public record ResourceMetadataSerializer<T>(String name, Codec<T> codec) {

	public Value<T> value(T value) {
		return new Value<>(this, value);
	}

	/**
	 * Типизированное значение метаданных, связанное с конкретным сериализатором.
	 *
	 * <p>Позволяет безопасно извлекать значение только если тип сериализатора совпадает.</p>
	 *
	 * @param type сериализатор, которым было декодировано значение
	 * @param value декодированное значение
	 * @param <T> тип значения
	 */
	public record Value<T>(ResourceMetadataSerializer<T> type, T value) {

		/**
		 * Возвращает значение, если переданный сериализатор совпадает с тем, которым оно было создано.
		 *
		 * @param serializer сериализатор для сравнения
		 * @param <U> ожидаемый тип значения
		 * @return значение, приведённое к типу {@code U}, или {@link Optional#empty()} при несовпадении
		 */
		@SuppressWarnings("unchecked")
		public <U> Optional<U> getValueIfMatching(ResourceMetadataSerializer<U> serializer) {
			return serializer == type ? Optional.of((U) value) : Optional.empty();
		}
	}
}
