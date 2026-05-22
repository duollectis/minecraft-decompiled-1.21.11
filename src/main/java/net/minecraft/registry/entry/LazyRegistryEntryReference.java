package net.minecraft.registry.entry;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;

import java.util.Optional;
import java.util.function.Function;

/**
 * Ленивая ссылка на запись реестра: хранит либо уже разрешённую {@link RegistryEntry},
 * либо {@link RegistryKey} для отложенного разрешения.
 * <p>
 * Используется в контекстах, где запись реестра может быть задана как inline-значением,
 * так и ключом для последующего поиска в реестре.
 *
 * @param <T> тип хранимого значения
 * @param contents либо готовая запись, либо ключ для разрешения
 */
public record LazyRegistryEntryReference<T>(Either<RegistryEntry<T>, RegistryKey<T>> contents) {

	public LazyRegistryEntryReference(RegistryEntry<T> entry) {
		this(Either.left(entry));
	}

	public LazyRegistryEntryReference(RegistryKey<T> key) {
		this(Either.right(key));
	}

	/**
	 * Создаёт кодек для {@link LazyRegistryEntryReference}.
	 * При декодировании сначала пробует разобрать как {@link RegistryEntry} через {@code entryCodec},
	 * затем как {@link RegistryKey}. Ключи при кодировании всегда сериализуются как ключи.
	 *
	 * @param registryRef ключ реестра для создания {@link RegistryKey}-кодека
	 * @param entryCodec  кодек для разрешённых записей реестра
	 * @return кодек для {@link LazyRegistryEntryReference}
	 */
	public static <T> Codec<LazyRegistryEntryReference<T>> createCodec(
			RegistryKey<Registry<T>> registryRef,
			Codec<RegistryEntry<T>> entryCodec
	) {
		return Codec.either(
						entryCodec,
						RegistryKey.createCodec(registryRef)
								.comapFlatMap(
										registryKey -> DataResult.error(() -> "Cannot parse as key without registry"),
										Function.identity()
								)
				)
				.xmap(LazyRegistryEntryReference::new, LazyRegistryEntryReference::contents);
	}

	public static <T> PacketCodec<RegistryByteBuf, LazyRegistryEntryReference<T>> createPacketCodec(
			RegistryKey<Registry<T>> registryRef,
			PacketCodec<RegistryByteBuf, RegistryEntry<T>> entryPacketCodec
	) {
		return PacketCodec.tuple(
				PacketCodecs.either(entryPacketCodec, RegistryKey.createPacketCodec(registryRef)),
				LazyRegistryEntryReference::contents,
				LazyRegistryEntryReference::new
		);
	}

	/**
	 * Разрешает значение через прямой реестр. Если содержит готовую запись — возвращает её значение.
	 * Если содержит ключ — ищет значение в переданном реестре.
	 *
	 * @param registry реестр для поиска по ключу
	 * @return значение, если найдено
	 */
	public Optional<T> resolveValue(Registry<T> registry) {
		return (Optional<T>) contents.map(entry -> Optional.of(entry.value()), registry::getOptionalValue);
	}

	/**
	 * Разрешает запись реестра через {@link RegistryWrapper.WrapperLookup}.
	 * Если содержит готовую запись — возвращает её напрямую.
	 * Если содержит ключ — ищет запись в lookup.
	 *
	 * @param registries lookup для поиска по ключу
	 * @return запись реестра, если найдена
	 */
	public Optional<RegistryEntry<T>> resolveEntry(RegistryWrapper.WrapperLookup registries) {
		return (Optional<RegistryEntry<T>>) contents.map(
				Optional::of,
				key -> registries.getOptionalEntry(key).map(entry -> (RegistryEntry) entry)
		);
	}

	public Optional<RegistryKey<T>> getKey() {
		return (Optional<RegistryKey<T>>) contents.map(RegistryEntry::getKey, Optional::of);
	}
}
