package net.minecraft.registry;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.tag.TagKey;

import java.util.List;

/**
 * {@code MutableRegistry}.
 */
public interface MutableRegistry<T> extends Registry<T> {

	RegistryEntry.Reference<T> add(RegistryKey<T> key, T value, RegistryEntryInfo info);

	void setEntries(TagKey<T> tag, List<RegistryEntry<T>> entries);

	boolean isEmpty();

	RegistryEntryLookup<T> createMutableRegistryLookup();
}
