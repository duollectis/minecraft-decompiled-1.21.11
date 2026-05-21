package net.minecraft.resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
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
import java.util.*;
import java.util.function.Consumer;

/**
 * {@code DefaultResourcePackBuilder}.
 */
public class DefaultResourcePackBuilder {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static Consumer<DefaultResourcePackBuilder> callback = builder -> {};
	private static final Map<ResourceType, Path> RESOURCE_TYPE_TO_PATH = Util.make(() -> {
		synchronized (DefaultResourcePack.class) {
			Builder<ResourceType, Path> builder = ImmutableMap.builder();

			for (ResourceType resourceType : ResourceType.values()) {
				String string = "/" + resourceType.getDirectory() + "/.mcassetsroot";
				URL uRL = DefaultResourcePack.class.getResource(string);
				if (uRL == null) {
					LOGGER.error("File {} does not exist in classpath", string);
				}
				else {
					try {
						URI uRI = uRL.toURI();
						String string2 = uRI.getScheme();
						if (!"jar".equals(string2) && !"file".equals(string2)) {
							LOGGER.warn("Assets URL '{}' uses unexpected schema", uRI);
						}

						Path path = PathResolving.toPath(uRI);
						builder.put(resourceType, path.getParent());
					}
					catch (Exception var12) {
						LOGGER.error("Couldn't resolve path to vanilla assets", var12);
					}
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
		else if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Path " + path.toAbsolutePath() + " is not directory");
		}
		else {
			return true;
		}
	}

	private void addRootPath(Path path) {
		if (this.exists(path)) {
			this.rootPaths.add(path);
		}
	}

	private void addPath(ResourceType type, Path path) {
		if (this.exists(path)) {
			this.paths.computeIfAbsent(type, type2 -> new LinkedHashSet<>()).add(path);
		}
	}

	public DefaultResourcePackBuilder withDefaultPaths() {
		RESOURCE_TYPE_TO_PATH.forEach((type, path) -> {
			this.addRootPath(path.getParent());
			this.addPath(type, path);
		});
		return this;
	}

	public DefaultResourcePackBuilder withPaths(ResourceType type, Class<?> clazz) {
		Enumeration<URL> enumeration = null;

		try {
			enumeration = clazz.getClassLoader().getResources(type.getDirectory() + "/");
		}
		catch (IOException var8) {
		}

		while (enumeration != null && enumeration.hasMoreElements()) {
			URL uRL = enumeration.nextElement();

			try {
				URI uRI = uRL.toURI();
				if ("file".equals(uRI.getScheme())) {
					Path path = Paths.get(uRI);
					this.addRootPath(path.getParent());
					this.addPath(type, path);
				}
			}
			catch (Exception var7) {
				LOGGER.error("Failed to extract path from {}", uRL, var7);
			}
		}

		return this;
	}

	public DefaultResourcePackBuilder runCallback() {
		callback.accept(this);
		return this;
	}

	public DefaultResourcePackBuilder withRoot(Path root) {
		this.addRootPath(root);

		for (ResourceType resourceType : ResourceType.values()) {
			this.addPath(resourceType, root.resolve(resourceType.getDirectory()));
		}

		return this;
	}

	public DefaultResourcePackBuilder withPath(ResourceType type, Path path) {
		this.addRootPath(path);
		this.addPath(type, path);
		return this;
	}

	public DefaultResourcePackBuilder withMetadataMap(ResourceMetadataMap metadataMap) {
		this.metadataMap = metadataMap;
		return this;
	}

	public DefaultResourcePackBuilder withNamespaces(String... namespaces) {
		this.namespaces.addAll(Arrays.asList(namespaces));
		return this;
	}

	public DefaultResourcePack build(ResourcePackInfo info) {
		return new DefaultResourcePack(
				info,
				this.metadataMap,
				Set.copyOf(this.namespaces),
				reverse(this.rootPaths),
				Util.mapEnum(ResourceType.class, type -> reverse(this.paths.getOrDefault(type, Set.of())))
		);
	}

	private static List<Path> reverse(Collection<Path> paths) {
		List<Path> list = new ArrayList<>(paths);
		Collections.reverse(list);
		return List.copyOf(list);
	}
}
