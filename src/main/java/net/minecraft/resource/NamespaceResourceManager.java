package net.minecraft.resource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.io.FilterInputStream;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Менеджер ресурсов для одного пространства имён.
 * Хранит упорядоченный список паков с опциональными фильтрами и реализует
 * логику приоритетного поиска ресурсов (последний пак имеет наивысший приоритет).
 */
public class NamespaceResourceManager implements ResourceManager {

	static final Logger LOGGER = LogUtils.getLogger();
	protected final List<FilterablePack> packList = new ArrayList<>();
	private final ResourceType type;
	private final String namespace;

	public NamespaceResourceManager(ResourceType type, String namespace) {
		this.type = type;
		this.namespace = namespace;
	}

	public void addPack(ResourcePack pack) {
		addPack(pack.getId(), pack, null);
	}

	public void addPack(ResourcePack pack, Predicate<Identifier> filter) {
		addPack(pack.getId(), pack, filter);
	}

	public void addPack(String id, Predicate<Identifier> filter) {
		addPack(id, null, filter);
	}

	private void addPack(String id, @Nullable ResourcePack underlyingPack, @Nullable Predicate<Identifier> filter) {
		packList.add(new FilterablePack(id, underlyingPack, filter));
	}

	@Override
	public Set<String> getAllNamespaces() {
		return ImmutableSet.of(namespace);
	}

	@Override
	public Optional<Resource> getResource(Identifier identifier) {
		for (int index = packList.size() - 1; index >= 0; index--) {
			FilterablePack filterablePack = packList.get(index);
			ResourcePack pack = filterablePack.underlying;

			if (pack != null) {
				InputSupplier<InputStream> supplier = pack.open(type, identifier);
				if (supplier != null) {
					InputSupplier<ResourceMetadata> metadataSupplier = createMetadataSupplier(identifier, index);
					return Optional.of(createResource(pack, identifier, supplier, metadataSupplier));
				}
			}

			if (filterablePack.isFiltered(identifier)) {
				LOGGER.warn("Resource {} not found, but was filtered by pack {}", identifier, filterablePack.name);
				return Optional.empty();
			}
		}

		return Optional.empty();
	}

	private static Resource createResource(
		ResourcePack pack,
		Identifier id,
		InputSupplier<InputStream> supplier,
		InputSupplier<ResourceMetadata> metadataSupplier
	) {
		return new Resource(pack, wrapForDebug(id, pack, supplier), metadataSupplier);
	}

	private static InputSupplier<InputStream> wrapForDebug(
		Identifier id,
		ResourcePack pack,
		InputSupplier<InputStream> supplier
	) {
		return LOGGER.isDebugEnabled()
			? () -> new DebugInputStream(supplier.get(), id, pack.getId())
			: supplier;
	}

	@Override
	public List<Resource> getAllResources(Identifier id) {
		Identifier metadataId = getMetadataPath(id);
		List<Resource> resources = new ArrayList<>();
		boolean metadataFiltered = false;
		String filteringPackName = null;

		for (int index = packList.size() - 1; index >= 0; index--) {
			FilterablePack filterablePack = packList.get(index);
			ResourcePack pack = filterablePack.underlying;

			if (pack != null) {
				InputSupplier<InputStream> supplier = pack.open(type, id);
				if (supplier != null) {
					InputSupplier<ResourceMetadata> metadataSupplier;
					if (metadataFiltered) {
						metadataSupplier = ResourceMetadata.NONE_SUPPLIER;
					} else {
						metadataSupplier = () -> {
							InputSupplier<InputStream> metaStream = pack.open(type, metadataId);
							return metaStream != null ? loadMetadata(metaStream) : ResourceMetadata.NONE;
						};
					}

					resources.add(new Resource(pack, supplier, metadataSupplier));
				}
			}

			if (filterablePack.isFiltered(id)) {
				filteringPackName = filterablePack.name;
				break;
			}

			if (filterablePack.isFiltered(metadataId)) {
				metadataFiltered = true;
			}
		}

		if (resources.isEmpty() && filteringPackName != null) {
			LOGGER.warn("Resource {} not found, but was filtered by pack {}", id, filteringPackName);
		}

		return Lists.reverse(resources);
	}

	private static boolean isMcmeta(Identifier id) {
		return id.getPath().endsWith(".mcmeta");
	}

	private static Identifier getMetadataFileName(Identifier id) {
		String path = id.getPath().substring(0, id.getPath().length() - ".mcmeta".length());
		return id.withPath(path);
	}

	static Identifier getMetadataPath(Identifier id) {
		return id.withPath(id.getPath() + ".mcmeta");
	}

	@Override
	public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
		record Result(ResourcePack pack, InputSupplier<InputStream> supplier, int packIndex) {}

		Map<Identifier, Result> resources = new HashMap<>();
		Map<Identifier, Result> metaResources = new HashMap<>();
		int packCount = packList.size();

		for (int index = 0; index < packCount; index++) {
			FilterablePack filterablePack = packList.get(index);
			filterablePack.removeFiltered(resources.keySet());
			filterablePack.removeFiltered(metaResources.keySet());

			ResourcePack pack = filterablePack.underlying;
			if (pack == null) {
				continue;
			}

			int packIndex = index;
			pack.findResources(type, namespace, startingPath, (foundId, supplier) -> {
				if (isMcmeta(foundId)) {
					if (allowedPathPredicate.test(getMetadataFileName(foundId))) {
						metaResources.put(foundId, new Result(pack, supplier, packIndex));
					}
				} else if (allowedPathPredicate.test(foundId)) {
					resources.put(foundId, new Result(pack, supplier, packIndex));
				}
			});
		}

		Map<Identifier, Resource> result = Maps.newTreeMap();
		resources.forEach((id, res) -> {
			Identifier metaId = getMetadataPath(id);
			Result metaResult = metaResources.get(metaId);
			InputSupplier<ResourceMetadata> metadataSupplier = (metaResult != null && metaResult.packIndex >= res.packIndex)
				? getMetadataSupplier(metaResult.supplier)
				: ResourceMetadata.NONE_SUPPLIER;

			result.put(id, createResource(res.pack, id, res.supplier, metadataSupplier));
		});

		return result;
	}

	private InputSupplier<ResourceMetadata> createMetadataSupplier(Identifier id, int index) {
		return () -> {
			Identifier metadataId = getMetadataPath(id);

			for (int packIndex = packList.size() - 1; packIndex >= index; packIndex--) {
				FilterablePack filterablePack = packList.get(packIndex);
				ResourcePack pack = filterablePack.underlying;

				if (pack != null) {
					InputSupplier<InputStream> supplier = pack.open(type, metadataId);
					if (supplier != null) {
						return loadMetadata(supplier);
					}
				}

				if (filterablePack.isFiltered(metadataId)) {
					break;
				}
			}

			return ResourceMetadata.NONE;
		};
	}

	private static InputSupplier<ResourceMetadata> getMetadataSupplier(InputSupplier<InputStream> supplier) {
		return () -> loadMetadata(supplier);
	}

	private static ResourceMetadata loadMetadata(InputSupplier<InputStream> supplier) throws IOException {
		try (InputStream inputStream = supplier.get()) {
			return ResourceMetadata.create(inputStream);
		}
	}

	private static void applyFilter(FilterablePack pack, Map<Identifier, EntryList> idToEntryList) {
		for (EntryList entryList : idToEntryList.values()) {
			if (pack.isFiltered(entryList.id)) {
				entryList.fileSources.clear();
			} else if (pack.isFiltered(entryList.metadataId())) {
				entryList.metaSources.clear();
			}
		}
	}

	private void findAndAdd(
		FilterablePack pack,
		String startingPath,
		Predicate<Identifier> allowedPathPredicate,
		Map<Identifier, EntryList> idToEntryList
	) {
		ResourcePack underlying = pack.underlying;
		if (underlying == null) {
			return;
		}

		underlying.findResources(type, namespace, startingPath, (foundId, supplier) -> {
			if (isMcmeta(foundId)) {
				Identifier baseId = getMetadataFileName(foundId);
				if (!allowedPathPredicate.test(baseId)) {
					return;
				}

				idToEntryList.computeIfAbsent(baseId, EntryList::new)
					.metaSources.put(underlying, supplier);
			} else {
				if (!allowedPathPredicate.test(foundId)) {
					return;
				}

				idToEntryList.computeIfAbsent(foundId, EntryList::new)
					.fileSources.add(new FileSource(underlying, supplier));
			}
		});
	}

	@Override
	public Map<Identifier, List<Resource>> findAllResources(
		String startingPath,
		Predicate<Identifier> allowedPathPredicate
	) {
		Map<Identifier, EntryList> entryMap = Maps.newHashMap();

		for (FilterablePack filterablePack : packList) {
			applyFilter(filterablePack, entryMap);
			findAndAdd(filterablePack, startingPath, allowedPathPredicate, entryMap);
		}

		TreeMap<Identifier, List<Resource>> result = Maps.newTreeMap();

		for (EntryList entryList : entryMap.values()) {
			if (entryList.fileSources.isEmpty()) {
				continue;
			}

			List<Resource> resources = new ArrayList<>();
			for (FileSource fileSource : entryList.fileSources) {
				ResourcePack pack = fileSource.sourcePack;
				InputSupplier<InputStream> metaStream = entryList.metaSources.get(pack);
				InputSupplier<ResourceMetadata> metadataSupplier = metaStream != null
					? getMetadataSupplier(metaStream)
					: ResourceMetadata.NONE_SUPPLIER;

				resources.add(createResource(pack, entryList.id, fileSource.supplier, metadataSupplier));
			}

			result.put(entryList.id, resources);
		}

		return result;
	}

	@Override
	public Stream<ResourcePack> streamResourcePacks() {
		return packList.stream().map(pack -> pack.underlying).filter(Objects::nonNull);
	}

	/**
	 * Обёртка над {@link InputStream} для отладки утечек ресурсов.
	 * При финализации незакрытого потока выводит предупреждение со стектрейсом.
	 */
	static class DebugInputStream extends FilterInputStream {

		private final Supplier<String> leakMessage;
		private boolean closed;

		public DebugInputStream(InputStream parent, Identifier id, String packId) {
			super(parent);
			Exception stackTrace = new Exception("Stacktrace");
			leakMessage = () -> {
				StringWriter writer = new StringWriter();
				stackTrace.printStackTrace(new PrintWriter(writer));
				return "Leaked resource: '" + id + "' loaded from pack: '" + packId + "'\n" + writer;
			};
		}

		@Override
		public void close() throws IOException {
			super.close();
			closed = true;
		}

		@Override
		protected void finalize() throws Throwable {
			if (!closed) {
				LOGGER.warn("{}", leakMessage.get());
			}

			super.finalize();
		}
	}

	/**
	 * Список источников для одного идентификатора ресурса: файловые источники и источники метаданных.
	 */
	record EntryList(
		Identifier id,
		Identifier metadataId,
		List<FileSource> fileSources,
		Map<ResourcePack, InputSupplier<InputStream>> metaSources
	) {

		EntryList(Identifier id) {
			this(id, getMetadataPath(id), new ArrayList<>(), new Object2ObjectArrayMap<>());
		}
	}

	/**
	 * Источник файла ресурса: пак и поставщик потока.
	 */
	record FileSource(ResourcePack sourcePack, InputSupplier<InputStream> supplier) {}

	/**
	 * Пак с опциональным фильтром идентификаторов.
	 */
	record FilterablePack(String name, @Nullable ResourcePack underlying, @Nullable Predicate<Identifier> filter) {

		public void removeFiltered(Collection<Identifier> ids) {
			if (filter != null) {
				ids.removeIf(filter);
			}
		}

		public boolean isFiltered(Identifier id) {
			return filter != null && filter.test(id);
		}
	}
}
