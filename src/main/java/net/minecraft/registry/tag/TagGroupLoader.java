package net.minecraft.registry.tag;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.DependencyTracker;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Загружает и строит группы тегов из ресурсов датапаков.
 *
 * <p>Процесс двухэтапный: сначала {@link #loadTags} читает JSON-файлы тегов
 * из всех датапаков (с поддержкой {@code replace: true}), затем {@link #buildGroup}
 * разрешает зависимости между тегами через {@link DependencyTracker} и возвращает
 * финальную карту {@code tagId -> List<T>}.</p>
 *
 * @param <T> тип элементов реестра, которые хранятся в тегах
 */
public class TagGroupLoader<T> {

	private static final Logger LOGGER = LogUtils.getLogger();

	final EntrySupplier<T> entrySupplier;
	private final String dataType;

	public TagGroupLoader(EntrySupplier<T> entrySupplier, String dataType) {
		this.entrySupplier = entrySupplier;
		this.dataType = dataType;
	}

	/**
	 * Читает все JSON-файлы тегов из датапаков и собирает их в карту.
	 *
	 * <p>Если файл тега содержит {@code "replace": true}, предыдущие записи
	 * для этого тега очищаются — это стандартный механизм переопределения тегов
	 * в датапаках с более высоким приоритетом.</p>
	 *
	 * @param resourceManager менеджер ресурсов для поиска файлов тегов
	 * @return карта {@code tagId -> список отслеживаемых записей} со всеми загруженными тегами
	 */
	public Map<Identifier, List<TrackedEntry>> loadTags(ResourceManager resourceManager) {
		Map<Identifier, List<TrackedEntry>> result = new HashMap<>();
		ResourceFinder resourceFinder = ResourceFinder.json(dataType);

		for (Entry<Identifier, List<Resource>> entry : resourceFinder.findAllResources(resourceManager).entrySet()) {
			Identifier resourcePath = entry.getKey();
			Identifier tagId = resourceFinder.toResourceId(resourcePath);

			for (Resource resource : entry.getValue()) {
				try (Reader reader = resource.getReader()) {
					JsonElement jsonElement = StrictJsonParser.parse(reader);
					List<TrackedEntry> tagEntries = result.computeIfAbsent(tagId, id -> new ArrayList<>());
					TagFile tagFile = (TagFile) TagFile.CODEC
							.parse(new Dynamic<>(JsonOps.INSTANCE, jsonElement))
							.getOrThrow();

					if (tagFile.replace()) {
						tagEntries.clear();
					}

					String packId = resource.getPackId();
					tagFile.entries().forEach(tagEntry -> tagEntries.add(new TrackedEntry(tagEntry, packId)));
				} catch (Exception exception) {
					LOGGER.error(
							"Couldn't read tag list {} from {} in data pack {}",
							tagId,
							resourcePath,
							resource.getPackId(),
							exception
					);
				}
			}
		}

		return result;
	}

	/**
	 * Разрешает все записи тега, рекурсивно подставляя значения из уже построенных тегов.
	 *
	 * @param valueGetter поставщик значений по идентификатору
	 * @param entries     список отслеживаемых записей тега
	 * @return {@code Either.right} со списком значений при успехе,
	 *         {@code Either.left} со списком нерезолвленных записей при ошибке
	 */
	private Either<List<TrackedEntry>, List<T>> resolveAll(
			TagEntry.ValueGetter<T> valueGetter,
			List<TrackedEntry> entries
	) {
		SequencedSet<T> resolved = new LinkedHashSet<>();
		List<TrackedEntry> missing = new ArrayList<>();

		for (TrackedEntry trackedEntry : entries) {
			if (!trackedEntry.entry().resolve(valueGetter, resolved::add)) {
				missing.add(trackedEntry);
			}
		}

		return missing.isEmpty() ? Either.right(List.copyOf(resolved)) : Either.left(missing);
	}

	/**
	 * Строит финальную карту тегов, разрешая все зависимости между ними.
	 *
	 * <p>Использует {@link DependencyTracker} для топологической сортировки тегов
	 * по зависимостям. Теги, ссылающиеся на другие теги, обрабатываются после
	 * тех, на которые они ссылаются. Нерезолвленные ссылки логируются как ошибки.</p>
	 *
	 * @param tags карта {@code tagId -> список записей}, полученная из {@link #loadTags}
	 * @return финальная карта {@code tagId -> список значений реестра}
	 */
	public Map<Identifier, List<T>> buildGroup(Map<Identifier, List<TrackedEntry>> tags) {
		final Map<Identifier, List<T>> builtTags = new HashMap<>();
		TagEntry.ValueGetter<T> valueGetter = new TagEntry.ValueGetter<T>() {
			@Override
			public @Nullable T direct(Identifier id, boolean required) {
				return (T) TagGroupLoader.this.entrySupplier.get(id, required).orElse(null);
			}

			@Override
			public @Nullable Collection<T> tag(Identifier id) {
				return builtTags.get(id);
			}
		};

		DependencyTracker<Identifier, TagDependencies> dependencyTracker = new DependencyTracker<>();
		tags.forEach((id, entries) -> dependencyTracker.add(id, new TagDependencies(entries)));

		dependencyTracker.traverse(
				(id, dependencies) -> resolveAll(valueGetter, dependencies.entries)
						.ifLeft(missingReferences -> LOGGER.error(
								"Couldn't load tag {} as it is missing following references: {}",
								id,
								missingReferences
										.stream()
										.map(Objects::toString)
										.collect(Collectors.joining(", "))
						))
						.ifRight(values -> builtTags.put(id, values))
		);

		return builtTags;
	}

	/**
	 * Применяет теги из сетевого пакета к изменяемому реестру.
	 *
	 * @param tags     сериализованные теги из сетевого пакета
	 * @param registry целевой изменяемый реестр
	 */
	public static <T> void loadFromNetwork(TagPacketSerializer.Serialized tags, MutableRegistry<T> registry) {
		tags.toRegistryTags(registry).tags.forEach(registry::setEntries);
	}

	/**
	 * Запускает асинхронную перезагрузку тегов для всех реестров в менеджере.
	 *
	 * @param resourceManager менеджер ресурсов
	 * @param registryManager менеджер динамических реестров
	 * @return список объектов ожидающей загрузки тегов для каждого реестра
	 */
	public static List<Registry.PendingTagLoad<?>> startReload(
			ResourceManager resourceManager,
			DynamicRegistryManager registryManager
	) {
		return registryManager.streamAllRegistries()
				.map(registry -> startReload(resourceManager, registry.value()))
				.flatMap(Optional::stream)
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Загружает и применяет теги к реестру при первоначальной инициализации сервера.
	 *
	 * <p>В отличие от {@link #startReload}, этот метод применяет теги немедленно,
	 * без создания объекта ожидающей загрузки.</p>
	 *
	 * @param resourceManager менеджер ресурсов
	 * @param registry        целевой изменяемый реестр
	 */
	public static <T> void loadInitial(ResourceManager resourceManager, MutableRegistry<T> registry) {
		RegistryKey<? extends Registry<T>> registryKey = registry.getKey();
		TagGroupLoader<RegistryEntry<T>> loader = new TagGroupLoader<>(
				EntrySupplier.forInitial(registry),
				RegistryKeys.getTagPath(registryKey)
		);

		loader.buildGroup(loader.loadTags(resourceManager))
				.forEach((id, entries) -> registry.setEntries(
						TagKey.of(registryKey, id),
						(List<RegistryEntry<T>>) entries
				));
	}

	private static <T> Map<TagKey<T>, List<RegistryEntry<T>>> toTagKeyedMap(
			RegistryKey<? extends Registry<T>> registryRef,
			Map<Identifier, List<RegistryEntry<T>>> tags
	) {
		return tags.entrySet()
				.stream()
				.collect(Collectors.toUnmodifiableMap(
						entry -> TagKey.of(registryRef, entry.getKey()),
						Entry::getValue
				));
	}

	private static <T> Optional<Registry.PendingTagLoad<T>> startReload(
			ResourceManager resourceManager,
			Registry<T> registry
	) {
		RegistryKey<? extends Registry<T>> registryKey = registry.getKey();
		TagGroupLoader<RegistryEntry<T>> loader = new TagGroupLoader<>(
				(EntrySupplier<RegistryEntry<T>>) EntrySupplier.forReload(registry),
				RegistryKeys.getTagPath(registryKey)
		);

		RegistryTags<T> registryTags = new RegistryTags<>(
				registryKey,
				toTagKeyedMap(registry.getKey(), loader.buildGroup(loader.loadTags(resourceManager)))
		);

		return registryTags.tags().isEmpty()
				? Optional.empty()
				: Optional.of(registry.startTagReload(registryTags));
	}

	/**
	 * Собирает список обёрток реестров, подставляя ожидающие загрузки тегов там, где они есть.
	 *
	 * @param registryManager менеджер иммутабельных реестров
	 * @param tagLoads        список ожидающих загрузок тегов
	 * @return список обёрток реестров с актуальными тегами
	 */
	public static List<RegistryWrapper.Impl<?>> collectRegistries(
			DynamicRegistryManager.Immutable registryManager,
			List<Registry.PendingTagLoad<?>> tagLoads
	) {
		List<RegistryWrapper.Impl<?>> wrappers = new ArrayList<>();

		registryManager.streamAllRegistries().forEach(registry -> {
			Registry.PendingTagLoad<?> pendingTagLoad = find(tagLoads, registry.key());
			wrappers.add((RegistryWrapper.Impl<?>) (pendingTagLoad != null
					? pendingTagLoad.getLookup()
					: registry.value()));
		});

		return wrappers;
	}

	private static Registry.@Nullable PendingTagLoad<?> find(
			List<Registry.PendingTagLoad<?>> pendingTags,
			RegistryKey<? extends Registry<?>> registryRef
	) {
		for (Registry.PendingTagLoad<?> pendingTagLoad : pendingTags) {
			if (pendingTagLoad.getKey() == registryRef) {
				return pendingTagLoad;
			}
		}

		return null;
	}

	/**
	 * Поставщик элементов реестра по идентификатору.
	 *
	 * <p>Используется в {@link TagEntry.ValueGetter} для разрешения прямых ссылок
	 * на элементы реестра при построении тегов.</p>
	 */
	public interface EntrySupplier<T> {

		Optional<? extends T> get(Identifier id, boolean required);

		static <T> EntrySupplier<? extends RegistryEntry<T>> forReload(Registry<T> registry) {
			return (id, required) -> registry.getEntry(id);
		}

		static <T> EntrySupplier<RegistryEntry<T>> forInitial(MutableRegistry<T> registry) {
			RegistryEntryLookup<T> mutableLookup = registry.createMutableRegistryLookup();
			return (id, required) -> ((RegistryEntryLookup<T>) (required ? mutableLookup : registry))
					.getOptional(RegistryKey.of(registry.getKey(), id));
		}
	}

	/**
	 * Результат загрузки тегов для конкретного реестра.
	 *
	 * @param key  ключ реестра
	 * @param tags карта тегов с их содержимым
	 */
	public record RegistryTags<T>(
			RegistryKey<? extends Registry<T>> key,
			Map<TagKey<T>, List<RegistryEntry<T>>> tags
	) {
	}

	/**
	 * Зависимости тега — список его записей с информацией об обязательных ссылках на другие теги.
	 */
	record TagDependencies(List<TrackedEntry> entries) implements DependencyTracker.Dependencies<Identifier> {

		@Override
		public void forDependencies(Consumer<Identifier> callback) {
			entries.forEach(entry -> entry.entry.forEachRequiredTagId(callback));
		}

		@Override
		public void forOptionalDependencies(Consumer<Identifier> callback) {
			entries.forEach(entry -> entry.entry.forEachOptionalTagId(callback));
		}
	}

	/**
	 * Запись тега с информацией об источнике (имя датапака).
	 *
	 * @param entry  запись тега
	 * @param source идентификатор датапака-источника
	 */
	public record TrackedEntry(TagEntry entry, String source) {

		@Override
		public String toString() {
			return entry + " (from " + source + ")";
		}
	}
}
