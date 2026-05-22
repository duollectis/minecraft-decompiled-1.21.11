package net.minecraft.registry;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.random.Random;

import java.util.Optional;

/**
 * Интерфейс для поиска записей реестра по ключу или тегу.
 * Используется в контексте сериализации и генерации данных.
 *
 * @param <T> тип элементов реестра
 */
public interface RegistryEntryLookup<T> {

	Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> key);

	default RegistryEntry.Reference<T> getOrThrow(RegistryKey<T> key) {
		return getOptional(key).orElseThrow(() -> new IllegalStateException("Missing element " + key));
	}

	Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag);

	default RegistryEntryList.Named<T> getOrThrow(TagKey<T> tag) {
		return getOptional(tag).orElseThrow(() -> new IllegalStateException("Missing tag " + tag));
	}

	default Optional<RegistryEntry<T>> getRandomEntry(TagKey<T> tag, Random random) {
		return getOptional(tag).flatMap(entryList -> entryList.getRandom(random));
	}

	/**
	 * Агрегированный lookup по нескольким реестрам.
	 * Позволяет получить {@link RegistryEntryLookup} для любого реестра по его ключу.
	 */
	interface RegistryLookup {

		<T> Optional<? extends RegistryEntryLookup<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryRef);

		default <T> RegistryEntryLookup<T> getOrThrow(RegistryKey<? extends Registry<? extends T>> registryRef) {
			return (RegistryEntryLookup<T>) getOptional(registryRef)
					.orElseThrow(() -> new IllegalStateException("Registry " + registryRef.getValue() + " not found"));
		}

		default <T> Optional<RegistryEntry.Reference<T>> getOptionalEntry(RegistryKey<T> registryRef) {
			return getOptional(registryRef.getRegistryRef())
					.flatMap(registryEntryLookup -> registryEntryLookup.getOptional(registryRef));
		}

		default <T> RegistryEntry.Reference<T> getEntryOrThrow(RegistryKey<T> key) {
			return getOptional(key.getRegistryRef())
					.flatMap(registry -> registry.getOptional(key))
					.orElseThrow(() -> new IllegalStateException("Missing element " + key));
		}
	}
}
