package net.minecraft.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.resource.metadata.ResourceMetadataMap;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.path.PathUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Встроенный ресурс-пак по умолчанию, загружаемый из classpath или файловой системы.
 * Поддерживает несколько корневых путей и путей по типу ресурса.
 * Создаётся через {@link DefaultResourcePackBuilder}.
 */
public class DefaultResourcePack implements ResourcePack {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final ResourcePackInfo info;
	private final ResourceMetadataMap metadata;
	private final Set<String> namespaces;
	private final List<Path> rootPaths;
	private final Map<ResourceType, List<Path>> namespacePaths;

	DefaultResourcePack(
		ResourcePackInfo info,
		ResourceMetadataMap metadata,
		Set<String> namespaces,
		List<Path> rootPaths,
		Map<ResourceType, List<Path>> namespacePaths
	) {
		this.info = info;
		this.metadata = metadata;
		this.namespaces = namespaces;
		this.rootPaths = rootPaths;
		this.namespacePaths = namespacePaths;
	}

	@Override
	public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
		PathUtil.validatePath(segments);
		List<String> segmentList = List.of(segments);

		for (Path root : rootPaths) {
			Path resolved = PathUtil.getPath(root, segmentList);
			if (Files.exists(resolved) && DirectoryResourcePack.isValidPath(resolved)) {
				return InputSupplier.create(resolved);
			}
		}

		return null;
	}

	/**
	 * Вызывает {@code consumer} для каждого пути, соответствующего данному идентификатору
	 * в рамках указанного типа ресурса.
	 *
	 * @param type     тип ресурса (клиентский или серверный)
	 * @param path     идентификатор ресурса
	 * @param consumer получатель найденных путей
	 */
	public void forEachNamespacedPath(ResourceType type, Identifier path, Consumer<Path> consumer) {
		PathUtil.split(path.getPath()).ifSuccess(segments -> {
			String namespace = path.getNamespace();

			for (Path namespacePath : namespacePaths.get(type)) {
				consumer.accept(PathUtil.getPath(namespacePath.resolve(namespace), segments));
			}
		}).ifError(error -> LOGGER.error("Invalid path {}: {}", path, error.message()));
	}

	@Override
	public void findResources(
		ResourceType type,
		String namespace,
		String prefix,
		ResourcePack.ResultConsumer consumer
	) {
		PathUtil.split(prefix).ifSuccess(segments -> {
			List<Path> paths = namespacePaths.get(type);
			int count = paths.size();

			if (count == 1) {
				collectIdentifiers(consumer, namespace, paths.get(0), segments);
			} else if (count > 1) {
				Map<Identifier, InputSupplier<InputStream>> merged = new java.util.HashMap<>();

				for (int index = 0; index < count - 1; index++) {
					collectIdentifiers(merged::putIfAbsent, namespace, paths.get(index), segments);
				}

				Path lastPath = paths.get(count - 1);
				if (merged.isEmpty()) {
					collectIdentifiers(consumer, namespace, lastPath, segments);
				} else {
					collectIdentifiers(merged::putIfAbsent, namespace, lastPath, segments);
					merged.forEach(consumer);
				}
			}
		}).ifError(error -> LOGGER.error("Invalid path {}: {}", prefix, error.message()));
	}

	private static void collectIdentifiers(
		ResourcePack.ResultConsumer consumer,
		String namespace,
		Path root,
		List<String> prefixSegments
	) {
		Path namespacedRoot = root.resolve(namespace);
		DirectoryResourcePack.findResources(namespace, namespacedRoot, prefixSegments, consumer);
	}

	@Override
	public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
		return PathUtil.split(id.getPath()).mapOrElse(
			segments -> {
				String namespace = id.getNamespace();

				for (Path namespacePath : namespacePaths.get(type)) {
					Path resolved = PathUtil.getPath(namespacePath.resolve(namespace), segments);
					if (Files.exists(resolved) && DirectoryResourcePack.isValidPath(resolved)) {
						return InputSupplier.create(resolved);
					}
				}

				return null;
			},
			error -> {
				LOGGER.error("Invalid path {}: {}", id, error.message());
				return null;
			}
		);
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		return namespaces;
	}

	@Override
	public <T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) {
		InputSupplier<InputStream> inputSupplier = openRoot("pack.mcmeta");
		if (inputSupplier != null) {
			try (InputStream inputStream = inputSupplier.get()) {
				T parsed = AbstractFileResourcePack.parseMetadata(metadataSerializer, inputStream, info);
				if (parsed != null) {
					return parsed;
				}

				return metadata.get(metadataSerializer);
			} catch (IOException ignored) {
			}
		}

		return metadata.get(metadataSerializer);
	}

	@Override
	public ResourcePackInfo getInfo() {
		return info;
	}

	@Override
	public void close() {
	}

	/**
	 * Возвращает {@link ResourceFactory}, открывающую ресурсы этого пака как клиентские.
	 *
	 * @return фабрика ресурсов на основе данного пака
	 */
	public ResourceFactory getFactory() {
		return id -> Optional
			.ofNullable(open(ResourceType.CLIENT_RESOURCES, id))
			.map(stream -> new Resource(this, stream));
	}
}
