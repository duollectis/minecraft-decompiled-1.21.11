package net.minecraft.registry.entry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Кодек для {@link RegistryEntry}, поддерживающий как ссылки по идентификатору,
 * так и inline-определения значений (если {@code allowInlineDefinitions = true}).
 * <p>
 * При наличии {@link RegistryOps} в контексте сериализации:
 * <ul>
 *   <li>Кодирует именованные записи как {@link Identifier}</li>
 *   <li>Декодирует идентификаторы в {@link RegistryEntry.Reference} через lookup реестра</li>
 * </ul>
 * Без контекста реестра — работает как обычный кодек элемента, оборачивая результат в {@link RegistryEntry.Direct}.
 *
 * @param <E> тип элементов реестра
 */
public final class RegistryElementCodec<E> implements Codec<RegistryEntry<E>> {

	private final RegistryKey<? extends Registry<E>> registryRef;
	private final Codec<E> elementCodec;
	private final boolean allowInlineDefinitions;

	public static <E> RegistryElementCodec<E> of(
			RegistryKey<? extends Registry<E>> registryRef,
			Codec<E> elementCodec
	) {
		return of(registryRef, elementCodec, true);
	}

	public static <E> RegistryElementCodec<E> of(
			RegistryKey<? extends Registry<E>> registryRef,
			Codec<E> elementCodec,
			boolean allowInlineDefinitions
	) {
		return new RegistryElementCodec<>(registryRef, elementCodec, allowInlineDefinitions);
	}

	private RegistryElementCodec(
			RegistryKey<? extends Registry<E>> registryRef,
			Codec<E> elementCodec,
			boolean allowInlineDefinitions
	) {
		this.registryRef = registryRef;
		this.elementCodec = elementCodec;
		this.allowInlineDefinitions = allowInlineDefinitions;
	}

	@Override
	public <T> DataResult<T> encode(RegistryEntry<E> registryEntry, DynamicOps<T> dynamicOps, T object) {
		if (dynamicOps instanceof RegistryOps<?> registryOps) {
			Optional<RegistryEntryOwner<E>> optional = registryOps.getOwner(registryRef);
			if (optional.isPresent()) {
				if (!registryEntry.ownerEquals(optional.get())) {
					return DataResult.error(() -> "Element " + registryEntry + " is not valid in current registry set");
				}

				return (DataResult<T>) registryEntry.getKeyOrValue()
						.map(
								key -> Identifier.CODEC.encode(key.getValue(), dynamicOps, object),
								value -> elementCodec.encode(value, dynamicOps, object)
						);
			}
		}

		return elementCodec.encode(registryEntry.value(), dynamicOps, object);
	}

	@Override
	public <T> DataResult<Pair<RegistryEntry<E>, T>> decode(DynamicOps<T> ops, T input) {
		if (ops instanceof RegistryOps<?> registryOps) {
			Optional<RegistryEntryLookup<E>> optional = registryOps.getEntryLookup(registryRef);
			if (optional.isEmpty()) {
				return DataResult.error(() -> "Registry does not exist: " + registryRef);
			}

			RegistryEntryLookup<E> registryEntryLookup = optional.get();
			DataResult<Pair<Identifier, T>> dataResult = Identifier.CODEC.decode(ops, input);
			if (dataResult.result().isEmpty()) {
				return allowInlineDefinitions
						? elementCodec.decode(ops, input).map(pair -> pair.mapFirst(RegistryEntry::of))
						: DataResult.error(() -> "Inline definitions not allowed here");
			}

			Pair<Identifier, T> pair = (Pair<Identifier, T>) dataResult.result().get();
			RegistryKey<E> registryKey = RegistryKey.of(registryRef, pair.getFirst());
			return registryEntryLookup.getOptional(registryKey)
					.<DataResult>map(DataResult::success)
					.orElseGet(() -> DataResult.error(() -> "Failed to get element " + registryKey))
					.map(reference -> Pair.of(reference, pair.getSecond()))
					.setLifecycle(Lifecycle.stable());
		}

		return elementCodec.decode(ops, input).map(pair -> pair.mapFirst(RegistryEntry::of));
	}

	@Override
	public String toString() {
		return "RegistryFileCodec[" + registryRef + " " + elementCodec + "]";
	}
}
