package net.minecraft.registry;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.*;
import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.entry.RegistryEntryInfo;

import java.util.Optional;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Function;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Базовый интерфейс реестра Minecraft. Реестр — это двунаправленное отображение между
 * идентификаторами ({@link Identifier}, {@link RegistryKey}) и значениями типа {@code T}.
 * Каждый элемент имеет числовой raw-id для сетевой передачи.
 *
 * <p>Реестры могут быть заморожены ({@link #freeze()}), после чего добавление новых элементов
 * запрещено. Теги загружаются отдельно через {@link #startTagReload}.
 *
 * @param <T> тип элементов реестра
 */
public interface Registry<T> extends Keyable, RegistryWrapper.Impl<T>, IndexedIterable<T>, FabricRegistry {

	@Override
	RegistryKey<? extends Registry<T>> getKey();

	/**
	 * Возвращает codec для сериализации значений реестра по их идентификатору.
	 * При декодировании ищет элемент в реестре по {@link Identifier}.
	 */
	default Codec<T> getCodec() {
		return getReferenceEntryCodec()
				.flatComapMap(
						RegistryEntry.Reference::value,
						value -> validateReference(getEntry((T) value))
				);
	}

	/**
	 * Возвращает codec для сериализации {@link RegistryEntry} по идентификатору элемента.
	 */
	default Codec<RegistryEntry<T>> getEntryCodec() {
		return getReferenceEntryCodec().flatComapMap(entry -> entry, this::validateReference);
	}

	private Codec<RegistryEntry.Reference<T>> getReferenceEntryCodec() {
		Codec<RegistryEntry.Reference<T>> codec = Identifier.CODEC
				.comapFlatMap(
						id -> getEntry(id)
								.map(DataResult::success)
								.orElseGet(() -> DataResult.error(
										() -> "Unknown registry key in " + getKey() + ": " + id
								)),
						entry -> entry.getKey().orElseThrow().getValue()
				);

		Function<RegistryEntry.Reference<T>, Lifecycle> lifecycleGetter = entry ->
				getEntryInfo(entry.getKey().orElseThrow())
						.map(RegistryEntryInfo::lifecycle)
						.orElse(Lifecycle.experimental());

		return Codecs.withLifecycle(codec, lifecycleGetter, lifecycleGetter);
	}

	private DataResult<RegistryEntry.Reference<T>> validateReference(RegistryEntry<T> entry) {
		return entry instanceof RegistryEntry.Reference<T> reference
				? DataResult.success(reference)
				: DataResult.error(() -> "Unregistered holder in " + getKey() + ": " + entry);
	}

	@Override
	default <U> Stream<U> keys(DynamicOps<U> ops) {
		return getIds().stream().map(id -> (U) ops.createString(id.toString()));
	}

	@Nullable Identifier getId(T value);

	Optional<RegistryKey<T>> getKey(T entry);

	@Override
	int getRawId(@Nullable T value);

	@Nullable T get(@Nullable RegistryKey<T> key);

	@Nullable T get(@Nullable Identifier id);

	Optional<RegistryEntryInfo> getEntryInfo(RegistryKey<T> key);

	default Optional<T> getOptionalValue(@Nullable Identifier id) {
		return Optional.ofNullable(get(id));
	}

	default Optional<T> getOptionalValue(@Nullable RegistryKey<T> key) {
		return Optional.ofNullable(get(key));
	}

	Optional<RegistryEntry.Reference<T>> getDefaultEntry();

	/**
	 * Возвращает значение по ключу или бросает {@link IllegalStateException}, если ключ не найден.
	 *
	 * @param key ключ реестра
	 * @return значение, гарантированно не null
	 * @throws IllegalStateException если ключ отсутствует в реестре
	 */
	default T getValueOrThrow(RegistryKey<T> key) {
		T value = get(key);
		if (value == null) {
			throw new IllegalStateException("Missing key in " + getKey() + ": " + key);
		}

		return value;
	}

	Set<Identifier> getIds();

	Set<Entry<RegistryKey<T>, T>> getEntrySet();

	Set<RegistryKey<T>> getKeys();

	Optional<RegistryEntry.Reference<T>> getRandom(Random random);

	default Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
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
		((MutableRegistry<V>) registry).add(key, entry, new RegistryEntryInfo(Optional.empty(), Lifecycle.stable()));
		return entry;
	}

	@SuppressWarnings("unchecked")
	static <R, T extends R> RegistryEntry.Reference<T> registerReference(
			Registry<R> registry,
			RegistryKey<R> key,
			T entry
	) {
		return (RegistryEntry.Reference<T>) ((MutableRegistry<R>) registry).add(key, entry, new RegistryEntryInfo(Optional.empty(), Lifecycle.stable()));
	}

	static <R, T extends R> RegistryEntry.Reference<T> registerReference(
			Registry<R> registry,
			Identifier id,
			T entry
	) {
		return registerReference(registry, RegistryKey.of(registry.getKey(), id), entry);
	}

	Registry<T> freeze();

	RegistryEntry.Reference<T> createEntry(T value);

	Optional<RegistryEntry.Reference<T>> getEntry(int rawId);

	Optional<RegistryEntry.Reference<T>> getEntry(Identifier id);

	RegistryEntry<T> getEntry(T value);

	default Iterable<RegistryEntry<T>> iterateEntries(TagKey<T> tag) {
		return (Iterable<RegistryEntry<T>>) DataFixUtils.orElse(getOptional(tag), List.of());
	}

	Stream<RegistryEntryList.Named<T>> streamTags();

	/**
	 * Возвращает {@link IndexedIterable} по {@link RegistryEntry}, где raw-id соответствует
	 * raw-id значения в реестре. Используется для сетевой синхронизации.
	 */
	default IndexedIterable<RegistryEntry<T>> getIndexedEntries() {
		return new IndexedIterable<>() {
			@Override
			public int getRawId(RegistryEntry<T> registryEntry) {
				return Registry.this.getRawId(registryEntry.value());
			}

			@Override
			public @Nullable RegistryEntry<T> get(int rawId) {
				return (RegistryEntry<T>) Registry.this.getEntry(rawId).orElse(null);
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
	 * Представляет незавершённую загрузку тегов для реестра.
	 * Теги применяются атомарно через {@link #apply()}.
	 */
	interface PendingTagLoad<T> {

		RegistryKey<? extends Registry<? extends T>> getKey();

		RegistryWrapper.Impl<T> getLookup();

		void apply();

		int size();
	}
}
