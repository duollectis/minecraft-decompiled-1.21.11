package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.resource.metadata.ResourceFilter;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Реализация {@link LifecycledResourceManager}, объединяющая несколько ресурс-паков
 * в единый менеджер с поддержкой фильтрации по пространствам имён.
 * При создании строит карту {@link NamespaceResourceManager} по каждому пространству имён.
 */
public class LifecycledResourceManagerImpl implements LifecycledResourceManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final Map<String, NamespaceResourceManager> subManagers;
	private final List<ResourcePack> packs;

	/**
	 * Создаёт менеджер ресурсов из списка паков для указанного типа ресурса.
	 * Для каждого пространства имён создаётся отдельный {@link NamespaceResourceManager}.
	 *
	 * @param type  тип ресурса (клиентский или серверный)
	 * @param packs список паков в порядке приоритета (от низшего к высшему)
	 */
	public LifecycledResourceManagerImpl(ResourceType type, List<ResourcePack> packs) {
		this.packs = List.copyOf(packs);
		Map<String, NamespaceResourceManager> managers = new HashMap<>();
		List<String> allNamespaces = packs.stream()
			.flatMap(pack -> pack.getNamespaces(type).stream())
			.distinct()
			.toList();

		for (ResourcePack pack : packs) {
			ResourceFilter filter = parseResourceFilter(pack);
			Set<String> packNamespaces = pack.getNamespaces(type);
			Predicate<Identifier> pathFilter = filter != null
				? id -> filter.isPathBlocked(id.getPath())
				: null;

			for (String namespace : allNamespaces) {
				boolean hasNamespace = packNamespaces.contains(namespace);
				boolean isNamespaceBlocked = filter != null && filter.isNamespaceBlocked(namespace);

				if (!hasNamespace && !isNamespaceBlocked) {
					continue;
				}

				NamespaceResourceManager manager = managers.computeIfAbsent(
					namespace,
					ns -> new NamespaceResourceManager(type, ns)
				);

				if (hasNamespace && isNamespaceBlocked) {
					manager.addPack(pack, pathFilter);
				} else if (hasNamespace) {
					manager.addPack(pack);
				} else {
					manager.addPack(pack.getId(), pathFilter);
				}
			}
		}

		subManagers = managers;
	}

	private @Nullable ResourceFilter parseResourceFilter(ResourcePack pack) {
		try {
			return pack.parseMetadata(ResourceFilter.SERIALIZER);
		} catch (IOException exception) {
			LOGGER.error("Failed to get filter section from pack {}", pack.getId());
			return null;
		}
	}

	@Override
	public Set<String> getAllNamespaces() {
		return subManagers.keySet();
	}

	@Override
	public Optional<Resource> getResource(Identifier identifier) {
		ResourceManager manager = subManagers.get(identifier.getNamespace());
		return manager != null ? manager.getResource(identifier) : Optional.empty();
	}

	@Override
	public List<Resource> getAllResources(Identifier id) {
		ResourceManager manager = subManagers.get(id.getNamespace());
		return manager != null ? manager.getAllResources(id) : List.of();
	}

	@Override
	public Map<Identifier, Resource> findResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
		validateStartingPath(startingPath);
		Map<Identifier, Resource> results = new TreeMap<>();

		for (NamespaceResourceManager manager : subManagers.values()) {
			results.putAll(manager.findResources(startingPath, allowedPathPredicate));
		}

		return results;
	}

	@Override
	public Map<Identifier, List<Resource>> findAllResources(
		String startingPath,
		Predicate<Identifier> allowedPathPredicate
	) {
		validateStartingPath(startingPath);
		Map<Identifier, List<Resource>> results = new TreeMap<>();

		for (NamespaceResourceManager manager : subManagers.values()) {
			results.putAll(manager.findAllResources(startingPath, allowedPathPredicate));
		}

		return results;
	}

	private static void validateStartingPath(String startingPath) {
		if (startingPath.endsWith("/")) {
			throw new IllegalArgumentException("Trailing slash in path " + startingPath);
		}
	}

	@Override
	public Stream<ResourcePack> streamResourcePacks() {
		return packs.stream();
	}

	@Override
	public void close() {
		packs.forEach(ResourcePack::close);
	}
}
