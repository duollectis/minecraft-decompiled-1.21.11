package net.minecraft.resource;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.minecraft.resource.metadata.ResourceMetadataMap;
import net.minecraft.util.Util;
import net.minecraft.util.path.PathResolving;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Строитель (Builder) для создания {@link DefaultResourcePack}.
 * Позволяет гибко настраивать корневые пути, пути по типу ресурса,
 * метаданные и пространства имён.
 */
public class DefaultResourcePackBuilder {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String JAR_SCHEME = "jar";
	private static final String FILE_SCHEME = "file";

	/** Внешний callback для расширения путей (используется Fabric API). */
	public static Consumer<DefaultResourcePackBuilder> callback = builder -> {};

	private static final Map<ResourceType, Path> RESOURCE_TYPE_TO_PATH = Util.make(() -> {
		synchronized (DefaultResourcePack.class) {
			ImmutableMap.Builder<ResourceType, Path> builder = ImmutableMap.builder();

			for (ResourceType resourceType : ResourceType.values()) {
				String markerPath = "/" + resourceType.getDirectory() + "/.mcassetsroot";
				URL url = DefaultResourcePack.class.getResource(markerPath);
				if (url == null) {
					LOGGER.error("File {} does not exist in classpath", markerPath);
					continue;
				}

				try {
					URI uri = url.toURI();
					String scheme = uri.getScheme();
					if (!JAR_SCHEME.equals(scheme) && !FILE_SCHEME.equals(scheme)) {
						LOGGER.warn("Assets URL '{}' uses unexpected schema", uri);
					}

					Path path = PathResolving.toPath(uri);
					builder.put(resourceType, path.getParent());
				} catch (Exception exception) {
					LOGGER.error("Couldn't resolve path to vanilla assets", exception);
				}
			}

			return builder.build();
		}
	});

	private final Set<Path> rootPaths = new LinkedHashSet<>();
	private final Map<ResourceType, Set<Path>> paths = new EnumMap<>(ResourceType.class);
	private ResourceMetadataMap metadataMap = ResourceMetadataMap.of();
	private final Set<String> namespaces = new HashSet<>();

	private boolean exists(Path path) {
		if (!Files.exists(path)) {
			return false;
		}

		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Path " + path.toAbsolutePath() + " is not directory");
		}

		return true;
	}

	private void addRootPath(Path path) {
		if (exists(path)) {
			rootPaths.add(path);
		}
	}

	private void addPath(ResourceType type, Path path) {
		if (exists(path)) {
			paths.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(path);
		}
	}

	/**
	 * Добавляет стандартные пути из classpath, соответствующие маркерным файлам
	 * {@code .mcassetsroot} для каждого типа ресурса.
	 *
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder withDefaultPaths() {
		RESOURCE_TYPE_TO_PATH.forEach((type, path) -> {
			addRootPath(path.getParent());
			addPath(type, path);
		});
		return this;
	}

	/**
	 * Сканирует classpath указанного класса и добавляет все найденные директории
	 * для данного типа ресурса.
	 *
	 * @param type  тип ресурса
	 * @param clazz класс, чей classloader используется для поиска
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder withPaths(ResourceType type, Class<?> clazz) {
		Enumeration<URL> enumeration = null;

		try {
			enumeration = clazz.getClassLoader().getResources(type.getDirectory() + "/");
		} catch (IOException ignored) {
		}

		while (enumeration != null && enumeration.hasMoreElements()) {
			URL url = enumeration.nextElement();

			try {
				URI uri = url.toURI();
				if (!FILE_SCHEME.equals(uri.getScheme())) {
					continue;
				}

				Path path = Paths.get(uri);
				addRootPath(path.getParent());
				addPath(type, path);
			} catch (Exception exception) {
				LOGGER.error("Failed to extract path from {}", url, exception);
			}
		}

		return this;
	}

	/**
	 * Запускает внешний callback, позволяя сторонним модам добавить свои пути.
	 *
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder runCallback() {
		callback.accept(this);
		return this;
	}

	/**
	 * Добавляет корневую директорию и все поддиректории по типам ресурсов.
	 *
	 * @param root корневая директория пака
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder withRoot(Path root) {
		addRootPath(root);

		for (ResourceType resourceType : ResourceType.values()) {
			addPath(resourceType, root.resolve(resourceType.getDirectory()));
		}

		return this;
	}

	/**
	 * Добавляет конкретный путь для указанного типа ресурса.
	 *
	 * @param type тип ресурса
	 * @param path путь к директории ресурсов
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder withPath(ResourceType type, Path path) {
		addRootPath(path);
		addPath(type, path);
		return this;
	}

	/**
	 * Устанавливает карту метаданных, используемую как запасной источник при отсутствии
	 * файла {@code pack.mcmeta}.
	 *
	 * @param metadataMap карта метаданных
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder withMetadataMap(ResourceMetadataMap metadataMap) {
		this.metadataMap = metadataMap;
		return this;
	}

	/**
	 * Добавляет пространства имён, которые будет обслуживать данный пак.
	 *
	 * @param namespaces пространства имён
	 * @return этот строитель
	 */
	public DefaultResourcePackBuilder withNamespaces(String... namespaces) {
		this.namespaces.addAll(Arrays.asList(namespaces));
		return this;
	}

	/**
	 * Собирает и возвращает готовый {@link DefaultResourcePack}.
	 *
	 * @param info информация о паке
	 * @return новый экземпляр {@link DefaultResourcePack}
	 */
	public DefaultResourcePack build(ResourcePackInfo info) {
		return new DefaultResourcePack(
			info,
			metadataMap,
			Set.copyOf(namespaces),
			reverse(rootPaths),
			Util.mapEnum(ResourceType.class, type -> reverse(paths.getOrDefault(type, Set.of())))
		);
	}

	private static List<Path> reverse(java.util.Collection<Path> collection) {
		List<Path> list = new ArrayList<>(collection);
		Collections.reverse(list);
		return List.copyOf(list);
	}
}
