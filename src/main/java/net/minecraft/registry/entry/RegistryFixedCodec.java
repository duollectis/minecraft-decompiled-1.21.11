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
 * Кодек для {@link RegistryEntry}, работающий исключительно через идентификаторы реестра.
 * В отличие от {@link RegistryElementCodec}, не поддерживает inline-определения —
 * значение всегда должно быть зарегистрировано и доступно по ключу.
 * <p>
 * Требует наличия {@link RegistryOps} в контексте сериализации. Без него
 * возвращает ошибку, так как не может получить доступ к реестру.
 *
 * @param <E> тип элементов реестра
 */
public final class RegistryFixedCodec<E> implements Codec<RegistryEntry<E>> {

	private final RegistryKey<? extends Registry<E>> registry;

	public static <E> RegistryFixedCodec<E> of(RegistryKey<? extends Registry<E>> registry) {
		return new RegistryFixedCodec<>(registry);
	}

	private RegistryFixedCodec(RegistryKey<? extends Registry<E>> registry) {
		this.registry = registry;
	}

	@Override
	public <T> DataResult<T> encode(RegistryEntry<E> registryEntry, DynamicOps<T> dynamicOps, T object) {
		if (dynamicOps instanceof RegistryOps<?> registryOps) {
			Optional<RegistryEntryOwner<E>> optional = registryOps.getOwner(registry);
			if (optional.isPresent()) {
				if (!registryEntry.ownerEquals(optional.get())) {
					return DataResult.error(() -> "Element " + registryEntry + " is not valid in current registry set");
				}

				return (DataResult<T>) registryEntry.getKeyOrValue()
						.map(
								registryKey -> Identifier.CODEC.encode(registryKey.getValue(), dynamicOps, object),
								value -> DataResult.error(
										() -> "Elements from registry " + registry + " can't be serialized to a value"
								)
						);
			}
		}

		return DataResult.error(() -> "Can't access registry " + registry);
	}

	@Override
	public <T> DataResult<Pair<RegistryEntry<E>, T>> decode(DynamicOps<T> ops, T input) {
		if (ops instanceof RegistryOps<?> registryOps) {
			Optional<RegistryEntryLookup<E>> optional = registryOps.getEntryLookup(registry);
			if (optional.isPresent()) {
				return Identifier.CODEC
						.decode(ops, input)
						.flatMap(pair -> {
							Identifier identifier = pair.getFirst();
							return optional.get()
									.getOptional(RegistryKey.of(registry, identifier))
									.<DataResult>map(DataResult::success)
									.orElseGet(() -> DataResult.error(() -> "Failed to get element " + identifier))
									.map(value -> Pair.of(value, pair.getSecond()))
									.setLifecycle(Lifecycle.stable());
						});
			}
		}

		return DataResult.error(() -> "Can't access registry " + registry);
	}

	@Override
	public String toString() {
		return "RegistryFixedCodec[" + registry + "]";
	}
}
