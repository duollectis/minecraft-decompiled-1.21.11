package net.minecraft.entity.data;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Обработчик типа отслеживаемых данных сущности.
 * Отвечает за сериализацию/десериализацию значения через {@link PacketCodec}
 * и за создание {@link TrackedData} с уникальным идентификатором.
 *
 * @param <T> тип отслеживаемого значения
 */
public interface TrackedDataHandler<T> {

	PacketCodec<? super RegistryByteBuf, T> codec();

	default TrackedData<T> create(int id) {
		return new TrackedData<>(id, this);
	}

	T copy(T value);

	/**
	 * Создаёт иммутабельный обработчик на основе готового кодека.
	 * Метод {@code copy} возвращает то же самое значение без копирования,
	 * что безопасно только для неизменяемых типов.
	 */
	static <T> TrackedDataHandler<T> create(PacketCodec<? super RegistryByteBuf, T> codec) {
		return new ImmutableHandler<>() {
			@Override
			public PacketCodec<? super RegistryByteBuf, T> codec() {
				return codec;
			}
		};
	}

	/**
	 * Специализация для иммутабельных типов: {@code copy} возвращает оригинал без клонирования.
	 */
	interface ImmutableHandler<T> extends TrackedDataHandler<T> {

		@Override
		default T copy(T object) {
			return object;
		}
	}
}
