package net.minecraft.registry;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.*;
import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@code Registry}.
 */
public interface Registry<T> extends Keyable, RegistryWrapper.Impl<T>, IndexedIterable<T>, FabricRegistry {

	@Override
	RegistryKey<? extends Registry<T>> getKey();

	default Codec<T> getCodec() {
		return this
				.getReferenceEntryCodec()
				.flatComapMap(
						RegistryEntry.Reference::value,
						value -> this.validateReference(this.getEntry((T) value))
				);
	}

	default Codec<RegistryEntry<T>> getEntryCodec() {
		return this.getReferenceEntryCodec().flatComapMap(entry -> entry, this::validateReference);
	}

	private Codec<RegistryEntry.Reference<T>> getReferenceEntryCodec() {
		Codec<RegistryEntry.Reference<T>> codec = Identifier.CODEC
				.comapFlatMap(
						id -> this.getEntry(id)
						          .map(DataResult::success)
						          .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.getKey()
								          + ": " + id)),
						entry -> entry.getKey().orElseThrow().getValue()
				);
		return Codecs.withLifecycle(
				codec,
				entry -> this
						.getEntryInfo(entry.getKey().orElseThrow())
						.map(RegistryEntryInfo::lifecycle)
						.orElse(Lifecycle.experimental())
		);
	}

	private DataResult<RegistryEntry.Reference<T>> validateReference(RegistryEntry<T> entry) {
		return entry instanceof RegistryEntry.Reference<T> reference
		       ? DataResult.success(reference)
		       : DataResult.error(() -> "Unregistered holder in " + this.getKey() + ": " + entry);
	}

	default <U> Stream<U> keys(DynamicOps<U> ops) {
		return this.getIds().stream().map(id -> (U) ops.createString(id.toString()));
	}

	@Nullable Identifier getId(T value);

	Optional<RegistryKey<T>> getKey(T entry);

	@Override
	int getRawId(@Nullable T value);

	@Nullable T get(@Nullable RegistryKey<T> key);

	@Nullable T get(@Nullable Identifier id);

	Optional<RegistryEntryInfo> getEntryInfo(RegistryKey<T> key);

	default Optional<T> getOptionalValue(@Nullable Identifier id) {
		return Optional.ofNullable(this.get(id));
	}

	default Optional<T> getOptionalValue(@Nullable RegistryKey<T> key) {
		return Optional.ofNullable(this.get(key));
	}

	Optional<RegistryEntry.Reference<T>> getDefaultEntry();

	default T getValueOrThrow(RegistryKey<T> key) {
		T object = this.get(key);
		if (object == null) {
			throw new IllegalStateException("Missing key in " + this.getKey() + ": " + key);
		}
		else {
			return object;
		}
	}

	Set<Identifier> getIds();

	Set<Entry<RegistryKey<T>, T>> getEntrySet();

	Set<RegistryKey<T>> getKeys();

	Optional<RegistryEntry.Reference<T>> getRandom(Random random);

	default Stream<T> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	boolean containsId(Identifier id);

	boolean contains(RegistryKey<T> key);

	static <T> T register(Registry<? super T> registry, String id, T entry) {
		return register(registry, Identifier.of(id), entry);
	}

	static <V, T extends V> T register(Registry<V> registry, Identifier id, T entry) {
		return register(registry, RegistryKey.of(registry.getKey(), id), entry);
	}

	static <V, T extends V> T register(Registry<V> registry, RegistryKey<V> key, T entry) {
		((MutableRegistry) registry).add(key, (V) entry, RegistryEntryInfo.DEFAULT);
		return entry;
	}

	static <R, T extends R> RegistryEntry.Reference<T> registerReference(
			Registry<R> registry,
			RegistryKey<R> key,
			T entry
	) {
		return ((MutableRegistry) registry).add(key, (R) entry, RegistryEntryInfo.DEFAULT);
	}

	static <R, T extends R> RegistryEntry.Reference<T> registerReference(Registry<R> registry, Identifier id, T entry) {
		return registerReference(registry, RegistryKey.of(registry.getKey(), id), entry);
	}

	Registry<T> freeze();

	RegistryEntry.Reference<T> createEntry(T value);

	Optional<RegistryEntry.Reference<T>> getEntry(int rawId);

	Optional<RegistryEntry.Reference<T>> getEntry(Identifier id);

	RegistryEntry<T> getEntry(T value);

	default Iterable<RegistryEntry<T>> iterateEntries(TagKey<T> tag) {
		return (Iterable<RegistryEntry<T>>) DataFixUtils.orElse(this.getOptional(tag), List.of());
	}

	Stream<RegistryEntryList.Named<T>> streamTags();

	default IndexedIterable<RegistryEntry<T>> getIndexedEntries() {
		return new IndexedIterable<RegistryEntry<T>>() {
			public int getRawId(RegistryEntry<T> registryEntry) {
				return Registry.this.getRawId(registryEntry.value());
			}

			public @Nullable RegistryEntry<T> get(int i) {
				return (RegistryEntry<T>) Registry.this.getEntry(i).orElse(null);
			}

			@Override
			public int size() {
				return Registry.this.size();
			}

			@Override
			public Iterator<RegistryEntry<T>> iterator() {
				return Registry.this.streamEntries().map(entry -> (RegistryEntry<T>) entry).iterator();
			}
		};
	}

	Registry.PendingTagLoad<T> startTagReload(TagGroupLoader.RegistryTags<T> tags);

	/**
	 * {@code PendingTagLoad}.
	 */
	public interface PendingTagLoad<T> {

		RegistryKey<? extends Registry<? extends T>> getKey();

		RegistryWrapper.Impl<T> getLookup();

		void apply();

		int size();
	}
}
