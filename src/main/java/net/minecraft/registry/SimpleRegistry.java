package net.minecraft.registry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Стандартная реализация {@link MutableRegistry}.
 * Хранит элементы в нескольких индексах для быстрого поиска по id, ключу, значению и raw-id.
 *
 * <p>Поддерживает два режима регистрации:
 * <ul>
 *   <li><b>Обычный</b> — ключ создаётся при регистрации через {@link #add}.</li>
 *   <li><b>Intrusive</b> — объект сам создаёт свою запись через {@link #createEntry},
 *       а при регистрации запись привязывается к ключу.</li>
 * </ul>
 *
 * @param <T> тип элементов реестра
 */
public class SimpleRegistry<T> implements MutableRegistry<T> {

	private static final int INITIAL_CAPACITY = 256;

	private final RegistryKey<? extends Registry<T>> key;
	private final ObjectList<RegistryEntry.Reference<T>> rawIdToEntry = new ObjectArrayList<>(INITIAL_CAPACITY);
	private final Reference2IntMap<T> entryToRawId =
			Util.make(new Reference2IntOpenHashMap<>(), map -> map.defaultReturnValue(-1));
	private final Map<Identifier, RegistryEntry.Reference<T>> idToEntry = new HashMap<>();
	private final Map<RegistryKey<T>, RegistryEntry.Reference<T>> keyToEntry = new HashMap<>();
	private final Map<T, RegistryEntry.Reference<T>> valueToEntry = new IdentityHashMap<>();
	private final Map<RegistryKey<T>, RegistryEntryInfo> keyToEntryInfo = new IdentityHashMap<>();
	private final Map<TagKey<T>, RegistryEntryList.Named<T>> tags = new IdentityHashMap<>();
	private Lifecycle lifecycle;
	SimpleRegistry.TagLookup<T> tagLookup = SimpleRegistry.TagLookup.ofUnbound();
	private boolean frozen;
	private @Nullable Map<T, RegistryEntry.Reference<T>> intrusiveValueToEntry;

	@Override
	public Stream<RegistryEntryList.Named<T>> getTags() {
		return streamTags();
	}

	public SimpleRegistry(RegistryKey<? extends Registry<T>> key, Lifecycle lifecycle) {
		this(key, lifecycle, false);
	}

	public SimpleRegistry(RegistryKey<? extends Registry<T>> key, Lifecycle lifecycle, boolean intrusive) {
		this.key = key;
		this.lifecycle = lifecycle;

		if (intrusive) {
			intrusiveValueToEntry = new IdentityHashMap<>();
		}
	}

	@Override
	public RegistryKey<? extends Registry<T>> getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "Registry[" + key + " (" + lifecycle + ")]";
	}

	private void assertNotFrozen() {
		if (frozen) {
			throw new IllegalStateException("Registry is already frozen");
		}
	}

	private void assertNotFrozen(RegistryKey<T> registryKey) {
		if (frozen) {
			throw new IllegalStateException("Registry is already frozen (trying to add key " + registryKey + ")");
		}
	}

	@Override
	public RegistryEntry.Reference<T> add(RegistryKey<T> registryKey, T value, RegistryEntryInfo info) {
		assertNotFrozen(registryKey);
		Objects.requireNonNull(registryKey);
		Objects.requireNonNull(value);

		if (idToEntry.containsKey(registryKey.getValue())) {
			throw (IllegalStateException) Util.getFatalOrPause(
					new IllegalStateException("Adding duplicate key '" + registryKey + "' to registry")
			);
		}

		if (valueToEntry.containsKey(value)) {
			throw (IllegalStateException) Util.getFatalOrPause(
					new IllegalStateException("Adding duplicate value '" + value + "' to registry")
			);
		}

		RegistryEntry.Reference<T> reference;
		if (intrusiveValueToEntry != null) {
			reference = intrusiveValueToEntry.remove(value);
			if (reference == null) {
				throw new AssertionError("Missing intrusive holder for " + registryKey + ":" + value);
			}

			reference.setRegistryKey(registryKey);
		} else {
			reference = keyToEntry.computeIfAbsent(
					registryKey,
					k -> RegistryEntry.Reference.standAlone(this, (RegistryKey<T>) k)
			);
		}

		keyToEntry.put(registryKey, reference);
		idToEntry.put(registryKey.getValue(), reference);
		valueToEntry.put(value, reference);

		int rawId = rawIdToEntry.size();
		rawIdToEntry.add(reference);
		entryToRawId.put(value, rawId);
		keyToEntryInfo.put(registryKey, info);
		lifecycle = lifecycle.add(info.lifecycle());

		return reference;
	}

	@Override
	public @Nullable Identifier getId(T value) {
		RegistryEntry.Reference<T> reference = valueToEntry.get(value);
		return reference != null ? reference.registryKey().getValue() : null;
	}

	@Override
	public Optional<RegistryKey<T>> getKey(T entry) {
		return Optional.ofNullable(valueToEntry.get(entry)).map(RegistryEntry.Reference::registryKey);
	}

	@Override
	public int getRawId(@Nullable T value) {
		return entryToRawId.getInt(value);
	}

	@Override
	public @Nullable T get(@Nullable RegistryKey<T> registryKey) {
		return getValue(keyToEntry.get(registryKey));
	}

	@Override
	public @Nullable T get(int index) {
		return (index >= 0 && index < rawIdToEntry.size())
				? ((RegistryEntry.Reference<T>) rawIdToEntry.get(index)).value()
				: null;
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getEntry(int rawId) {
		return (rawId >= 0 && rawId < rawIdToEntry.size())
				? Optional.ofNullable((RegistryEntry.Reference<T>) rawIdToEntry.get(rawId))
				: Optional.empty();
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getEntry(Identifier id) {
		return Optional.ofNullable(idToEntry.get(id));
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> registryKey) {
		return Optional.ofNullable(keyToEntry.get(registryKey));
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getDefaultEntry() {
		return rawIdToEntry.isEmpty()
				? Optional.empty()
				: Optional.of((RegistryEntry.Reference<T>) rawIdToEntry.getFirst());
	}

	@Override
	public RegistryEntry<T> getEntry(T value) {
		RegistryEntry.Reference<T> reference = valueToEntry.get(value);
		return reference != null ? reference : RegistryEntry.of(value);
	}

	RegistryEntry.Reference<T> getOrCreateEntry(RegistryKey<T> registryKey) {
		return keyToEntry.computeIfAbsent(registryKey, key -> {
			if (intrusiveValueToEntry != null) {
				throw new IllegalStateException("This registry can't create new holders without value");
			}

			assertNotFrozen((RegistryKey<T>) key);
			return RegistryEntry.Reference.standAlone(this, (RegistryKey<T>) key);
		});
	}

	@Override
	public int size() {
		return keyToEntry.size();
	}

	@Override
	public Optional<RegistryEntryInfo> getEntryInfo(RegistryKey<T> registryKey) {
		return Optional.ofNullable(keyToEntryInfo.get(registryKey));
	}

	@Override
	public Lifecycle getLifecycle() {
		return lifecycle;
	}

	@Override
	public Iterator<T> iterator() {
		return Iterators.transform(rawIdToEntry.iterator(), RegistryEntry::value);
	}

	@Override
	public @Nullable T get(@Nullable Identifier id) {
		return getValue(idToEntry.get(id));
	}

	private static <T> @Nullable T getValue(RegistryEntry.@Nullable Reference<T> entry) {
		return entry != null ? entry.value() : null;
	}

	@Override
	public Set<Identifier> getIds() {
		return Collections.unmodifiableSet(idToEntry.keySet());
	}

	@Override
	public Set<RegistryKey<T>> getKeys() {
		return Collections.unmodifiableSet(keyToEntry.keySet());
	}

	@Override
	public Set<Entry<RegistryKey<T>, T>> getEntrySet() {
		return Collections.unmodifiableSet(
				Util.<RegistryKey<T>, RegistryEntry.Reference<T>, T>transformMapValuesLazy(
						keyToEntry,
						RegistryEntry::value
				).entrySet()
		);
	}

	@Override
	public Stream<RegistryEntry.Reference<T>> streamEntries() {
		return rawIdToEntry.stream();
	}

	@Override
	public Stream<RegistryEntryList.Named<T>> streamTags() {
		return tagLookup.stream();
	}

	RegistryEntryList.Named<T> getTag(TagKey<T> tagKey) {
		return tags.computeIfAbsent(tagKey, this::createNamedEntryList);
	}

	private RegistryEntryList.Named<T> createNamedEntryList(TagKey<T> tag) {
		return new RegistryEntryList.Named<>(this, tag);
	}

	@Override
	public boolean isEmpty() {
		return keyToEntry.isEmpty();
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getRandom(Random random) {
		return Util.getRandomOrEmpty(rawIdToEntry, random);
	}

	@Override
	public boolean containsId(Identifier id) {
		return idToEntry.containsKey(id);
	}

	@Override
	public boolean contains(RegistryKey<T> registryKey) {
		return keyToEntry.containsKey(registryKey);
	}

	/**
	 * Замораживает реестр: запрещает добавление новых элементов, привязывает значения
	 * к их записям и инициализирует lookup тегов.
	 *
	 * @throws IllegalStateException если есть незарегистрированные ключи, незаполненные
	 *                               intrusive-записи или несвязанные теги
	 */
	@Override
	public Registry<T> freeze() {
		if (frozen) {
			return this;
		}

		frozen = true;
		valueToEntry.forEach((value, entry) -> entry.setValue((T) value));

		List<Identifier> unboundKeys = keyToEntry.entrySet()
				.stream()
				.filter(entry -> !entry.getValue().hasKeyAndValue())
				.map(entry -> entry.getKey().getValue())
				.sorted()
				.toList();

		if (!unboundKeys.isEmpty()) {
			throw new IllegalStateException("Unbound values in registry " + getKey() + ": " + unboundKeys);
		}

		if (intrusiveValueToEntry != null) {
			if (!intrusiveValueToEntry.isEmpty()) {
				throw new IllegalStateException(
						"Some intrusive holders were not registered: " + intrusiveValueToEntry.values()
				);
			}

			intrusiveValueToEntry = null;
		}

		if (tagLookup.isBound()) {
			throw new IllegalStateException("Tags already present before freezing");
		}

		List<Identifier> unboundTags = tags.entrySet()
				.stream()
				.filter(entry -> !entry.getValue().isBound())
				.map(entry -> entry.getKey().id())
				.sorted()
				.toList();

		if (!unboundTags.isEmpty()) {
			throw new IllegalStateException("Unbound tags in registry " + getKey() + ": " + unboundTags);
		}

		tagLookup = SimpleRegistry.TagLookup.fromMap(tags);
		refreshTags();

		return this;
	}

	@Override
	public RegistryEntry.Reference<T> createEntry(T value) {
		if (intrusiveValueToEntry == null) {
			throw new IllegalStateException("This registry can't create intrusive holders");
		}

		assertNotFrozen();
		return intrusiveValueToEntry.computeIfAbsent(
				value,
				v -> RegistryEntry.Reference.intrusive(this, (T) v)
		);
	}

	@Override
	public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
		return tagLookup.getOptional(tag);
	}

	private RegistryEntry.Reference<T> ensureTagable(TagKey<T> tagKey, RegistryEntry<T> entry) {
		if (!entry.ownerEquals(this)) {
			throw new IllegalStateException(
					"Can't create named set " + tagKey + " containing value " + entry
							+ " from outside registry " + this
			);
		}

		if (entry instanceof RegistryEntry.Reference<T> reference) {
			return reference;
		}

		throw new IllegalStateException("Found direct holder " + entry + " value in tag " + tagKey);
	}

	@Override
	public void setEntries(TagKey<T> tag, List<RegistryEntry<T>> entries) {
		assertNotFrozen();
		getTag(tag).setEntries(entries);
	}

	void refreshTags() {
		Map<RegistryEntry.Reference<T>, List<TagKey<T>>> entryToTags = new IdentityHashMap<>();
		keyToEntry.values().forEach(entry -> entryToTags.put((RegistryEntry.Reference<T>) entry, new ArrayList<>()));

		tagLookup.forEach((tagKey, namedList) -> {
			for (RegistryEntry<T> registryEntry : namedList) {
				RegistryEntry.Reference<T> reference = ensureTagable((TagKey<T>) tagKey, registryEntry);
				entryToTags.get(reference).add((TagKey<T>) tagKey);
			}
		});

		entryToTags.forEach(RegistryEntry.Reference::setTags);
	}

	/**
	 * Сбрасывает все теги реестра в пустые списки.
	 * Вызывается перед перезагрузкой тегов из ресурсов.
	 *
	 * @throws IllegalStateException если реестр заморожен
	 */
	public void resetTagEntries() {
		assertNotFrozen();
		tags.values().forEach(tag -> tag.setEntries(List.of()));
	}

	@Override
	public RegistryEntryLookup<T> createMutableRegistryLookup() {
		assertNotFrozen();
		return new RegistryEntryLookup<T>() {
			@Override
			public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> registryKey) {
				return Optional.of(getOrThrow(registryKey));
			}

			@Override
			public RegistryEntry.Reference<T> getOrThrow(RegistryKey<T> registryKey) {
				return SimpleRegistry.this.getOrCreateEntry(registryKey);
			}

			@Override
			public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
				return Optional.of(getOrThrow(tag));
			}

			@Override
			public RegistryEntryList.Named<T> getOrThrow(TagKey<T> tag) {
				return SimpleRegistry.this.getTag(tag);
			}
		};
	}

	/**
	 * Начинает перезагрузку тегов из уже загруженных данных.
	 * Возвращает {@link Registry.PendingTagLoad}, который применяет теги атомарно через {@link Registry.PendingTagLoad#apply()}.
	 *
	 * @param tags новые теги для применения
	 * @throws IllegalStateException если реестр не заморожен
	 */
	@Override
	public Registry.PendingTagLoad<T> startTagReload(TagGroupLoader.RegistryTags<T> tags) {
		if (!frozen) {
			throw new IllegalStateException("Invalid method used for tag loading");
		}

		Builder<TagKey<T>, RegistryEntryList.Named<T>> builder = ImmutableMap.builder();
		final Map<TagKey<T>, List<RegistryEntry<T>>> pendingEntries = new HashMap<>();

		tags.tags().forEach((tagKey, values) -> {
			RegistryEntryList.Named<T> named = this.tags.get(tagKey);
			if (named == null) {
				named = createNamedEntryList((TagKey<T>) tagKey);
			}

			builder.put(tagKey, named);
			pendingEntries.put((TagKey<T>) tagKey, List.copyOf(values));
		});

		final ImmutableMap<TagKey<T>, RegistryEntryList.Named<T>> newTagMap = builder.build();
		final RegistryWrapper.Impl<T> pendingLookup = new RegistryWrapper.Impl.Delegating<T>() {
			@Override
			public RegistryWrapper.Impl<T> getBase() {
				return SimpleRegistry.this;
			}

			@Override
			public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
				return Optional.ofNullable((RegistryEntryList.Named<T>) newTagMap.get(tag));
			}

			@Override
			public Stream<RegistryEntryList.Named<T>> getTags() {
				return newTagMap.values().stream();
			}
		};

		return new Registry.PendingTagLoad<T>() {
			@Override
			public RegistryKey<? extends Registry<? extends T>> getKey() {
				return SimpleRegistry.this.getKey();
			}

			@Override
			public int size() {
				return pendingEntries.size();
			}

			@Override
			public RegistryWrapper.Impl<T> getLookup() {
				return pendingLookup;
			}

			@Override
			public void apply() {
				newTagMap.forEach((tagKey, named) -> {
					List<RegistryEntry<T>> entries = pendingEntries.getOrDefault(tagKey, List.of());
					named.setEntries(entries);
				});

				SimpleRegistry.this.tagLookup = SimpleRegistry.TagLookup.fromMap(newTagMap);
				SimpleRegistry.this.refreshTags();
			}
		};
	}

	/**
	 * Внутренний интерфейс для доступа к тегам реестра.
	 * Имеет два состояния: несвязанное (до заморозки) и связанное (после заморозки).
	 */
	interface TagLookup<T> {

		static <T> SimpleRegistry.TagLookup<T> ofUnbound() {
			return new SimpleRegistry.TagLookup<T>() {
				@Override
				public boolean isBound() {
					return false;
				}

				@Override
				public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> key) {
					throw new IllegalStateException("Tags not bound, trying to access " + key);
				}

				@Override
				public void forEach(BiConsumer<? super TagKey<T>, ? super RegistryEntryList.Named<T>> consumer) {
					throw new IllegalStateException("Tags not bound");
				}

				@Override
				public Stream<RegistryEntryList.Named<T>> stream() {
					throw new IllegalStateException("Tags not bound");
				}
			};
		}

		static <T> SimpleRegistry.TagLookup<T> fromMap(Map<TagKey<T>, RegistryEntryList.Named<T>> map) {
			return new SimpleRegistry.TagLookup<T>() {
				@Override
				public boolean isBound() {
					return true;
				}

				@Override
				public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> key) {
					return Optional.ofNullable(map.get(key));
				}

				@Override
				public void forEach(BiConsumer<? super TagKey<T>, ? super RegistryEntryList.Named<T>> consumer) {
					map.forEach(consumer);
				}

				@Override
				public Stream<RegistryEntryList.Named<T>> stream() {
					return map.values().stream();
				}
			};
		}

		boolean isBound();

		Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> key);

		void forEach(BiConsumer<? super TagKey<T>, ? super RegistryEntryList.Named<T>> consumer);

		Stream<RegistryEntryList.Named<T>> stream();
	}
}
