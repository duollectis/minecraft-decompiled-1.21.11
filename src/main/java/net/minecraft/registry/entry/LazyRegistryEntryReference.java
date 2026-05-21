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
 * {@code LazyRegistryEntryReference}.
 */
public record LazyRegistryEntryReference<T>(Either<RegistryEntry<T>, RegistryKey<T>> contents) {

	public LazyRegistryEntryReference(RegistryEntry<T> entry) {
		this(Either.left(entry));
	}

	public LazyRegistryEntryReference(RegistryKey<T> key) {
		this(Either.right(key));
	}

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
			RegistryKey<Registry<T>> registryRef, PacketCodec<RegistryByteBuf, RegistryEntry<T>> entryPacketCodec
	) {
		return PacketCodec.tuple(
				PacketCodecs.either(entryPacketCodec, RegistryKey.createPacketCodec(registryRef)),
				LazyRegistryEntryReference::contents,
				LazyRegistryEntryReference::new
		);
	}

	/**
	 * Resolve value.
	 *
	 * @param registry registry
	 *
	 * @return Optional — результат операции
	 */
	public Optional<T> resolveValue(Registry<T> registry) {
		return (Optional<T>) this.contents.map(entry -> Optional.of(entry.value()), registry::getOptionalValue);
	}

	/**
	 * Resolve entry.
	 *
	 * @param registries registries
	 *
	 * @return Optional> — результат операции
	 */
	public Optional<RegistryEntry<T>> resolveEntry(RegistryWrapper.WrapperLookup registries) {
		return (Optional<RegistryEntry<T>>) this.contents.map(
				Optional::of,
				key -> registries.getOptionalEntry(key).map(entry -> (RegistryEntry) entry)
		);
	}

	public Optional<RegistryKey<T>> getKey() {
		return (Optional<RegistryKey<T>>) this.contents.map(RegistryEntry::getKey, Optional::of);
	}
}
