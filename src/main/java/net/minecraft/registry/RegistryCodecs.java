package net.minecraft.registry;

import com.mojang.serialization.Codec;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryListCodec;
import net.minecraft.registry.entry.RegistryFixedCodec;

/**
 * Фабрика кодеков для списков записей реестра ({@link RegistryEntryList}).
 * Предоставляет удобные методы для создания кодеков, поддерживающих
 * как ссылки по тегу, так и прямые списки элементов.
 */
public class RegistryCodecs {

	/**
	 * Создаёт кодек для списка записей реестра с поддержкой инлайн-определений элементов.
	 * Список всегда сериализуется как массив (не как одиночный элемент).
	 *
	 * @param registryRef  ключ реестра
	 * @param elementCodec кодек для инлайн-сериализации элементов
	 * @param <E>          тип элементов
	 * @return кодек для {@link RegistryEntryList}
	 */
	public static <E> Codec<RegistryEntryList<E>> entryList(
			RegistryKey<? extends Registry<E>> registryRef,
			Codec<E> elementCodec
	) {
		return entryList(registryRef, elementCodec, false);
	}

	/**
	 * Создаёт кодек для списка записей реестра с поддержкой инлайн-определений элементов.
	 *
	 * @param registryRef          ключ реестра
	 * @param elementCodec         кодек для инлайн-сериализации элементов
	 * @param alwaysSerializeAsList если {@code true}, одиночный элемент тоже сериализуется как массив
	 * @param <E>                  тип элементов
	 * @return кодек для {@link RegistryEntryList}
	 */
	public static <E> Codec<RegistryEntryList<E>> entryList(
			RegistryKey<? extends Registry<E>> registryRef,
			Codec<E> elementCodec,
			boolean alwaysSerializeAsList
	) {
		return RegistryEntryListCodec.create(
				registryRef,
				RegistryElementCodec.of(registryRef, elementCodec),
				alwaysSerializeAsList
		);
	}

	/**
	 * Создаёт кодек для списка записей реестра без поддержки инлайн-определений.
	 * Элементы сериализуются только по идентификатору (ключу реестра).
	 *
	 * @param registryRef ключ реестра
	 * @param <E>         тип элементов
	 * @return кодек для {@link RegistryEntryList}
	 */
	public static <E> Codec<RegistryEntryList<E>> entryList(RegistryKey<? extends Registry<E>> registryRef) {
		return entryList(registryRef, false);
	}

	/**
	 * Создаёт кодек для списка записей реестра без поддержки инлайн-определений.
	 *
	 * @param registryRef          ключ реестра
	 * @param alwaysSerializeAsList если {@code true}, одиночный элемент тоже сериализуется как массив
	 * @param <E>                  тип элементов
	 * @return кодек для {@link RegistryEntryList}
	 */
	public static <E> Codec<RegistryEntryList<E>> entryList(
			RegistryKey<? extends Registry<E>> registryRef,
			boolean alwaysSerializeAsList
	) {
		return RegistryEntryListCodec.create(registryRef, RegistryFixedCodec.of(registryRef), alwaysSerializeAsList);
	}
}
