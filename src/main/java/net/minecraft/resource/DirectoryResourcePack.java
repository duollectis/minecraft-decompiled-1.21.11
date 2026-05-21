package net.minecraft.resource;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.path.PathUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code DirectoryResourcePack}.
 */
public class DirectoryResourcePack extends AbstractFileResourcePack {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Joiner SEPARATOR_JOINER = Joiner.on("/");
	private final Path root;

	public DirectoryResourcePack(ResourcePackInfo info, Path root) {
		super(info);
		this.root = root;
	}

	@Override
	public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
		PathUtil.validatePath(segments);
		Path path = PathUtil.getPath(this.root, List.of(segments));
		return Files.exists(path) ? InputSupplier.create(path) : null;
	}

	public static boolean isValidPath(Path path) {
		if (!SharedConstants.VALIDATE_RESOURCE_PATH_CASE) {
			return true;
		}
		else if (path.getFileSystem() != FileSystems.getDefault()) {
			return true;
		}
		else {
			try {
				return path.toRealPath().endsWith(path);
			}
			catch (IOException var2) {
				LOGGER.warn("Failed to resolve real path for {}", path, var2);
				return false;
			}
		}
	}

	@Override
	public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
		Path path = this.root.resolve(type.getDirectory()).resolve(id.getNamespace());
		return open(id, path);
	}

	public static @Nullable InputSupplier<InputStream> open(Identifier id, Path path) {
		return (InputSupplier<InputStream>) PathUtil.split(id.getPath()).mapOrElse(
				segments -> {
					Path path2 = PathUtil.getPath(path, segments);
					return open(path2);
				}, error -> {
					LOGGER.error("Invalid path {}: {}", id, error.message());
					return null;
				}
		);
	}

	private static @Nullable InputSupplier<InputStream> open(Path path) {
		return Files.exists(path) && isValidPath(path) ? InputSupplier.create(path) : null;
	}

	@Override
	public void findResources(
			ResourceType type,
			String namespace,
			String prefix,
			ResourcePack.ResultConsumer consumer
	) {
		PathUtil.split(prefix).ifSuccess(prefixSegments -> {
			Path path = this.root.resolve(type.getDirectory()).resolve(namespace);
			findResources(namespace, path, prefixSegments, consumer);
		}).ifError(error -> LOGGER.error("Invalid path {}: {}", prefix, error.message()));
	}

	public static void findResources(
			String namespace,
			Path path,
			List<String> prefixSegments,
			ResourcePack.ResultConsumer consumer
	) {
		Path path2 = PathUtil.getPath(path, prefixSegments);

		try (Stream<Path> stream = Files.find(path2, Integer.MAX_VALUE, DirectoryResourcePack::isRegularFile)) {
			stream.forEach(foundPath -> {
				String string2 = SEPARATOR_JOINER.join(path.relativize(foundPath));
				Identifier identifier = Identifier.tryParse(namespace, string2);
				if (identifier == null) {
					Util.logErrorOrPause(String.format(
							Locale.ROOT,
							"Invalid path in pack: %s:%s, ignoring",
							namespace,
							string2
					));
				}
				else {
					consumer.accept(identifier, InputSupplier.create(foundPath));
				}
			});
		}
		catch (NotDirectoryException | NoSuchFileException var10) {
		}
		catch (IOException var11) {
			LOGGER.error("Failed to list path {}", path2, var11);
		}
	}

	private static boolean isRegularFile(Path path, BasicFileAttributes fileAttributes) {
		return !SharedConstants.isDevelopment
		       ? fileAttributes.isRegularFile()
		       : fileAttributes.isRegularFile() && !StringUtils.equalsIgnoreCase(
				       path.getFileName().toString(),
				       ".ds_store"
		       );
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		Set<String> set = Sets.newHashSet();
		Path path = this.root.resolve(type.getDirectory());

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
			for (Path path2 : directoryStream) {
				String string = path2.getFileName().toString();
				if (Identifier.isNamespaceValid(string)) {
					set.add(string);
				}
				else {
					LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", string, this.root);
				}
			}
		}
		catch (NotDirectoryException | NoSuchFileException var10) {
		}
		catch (IOException var11) {
			LOGGER.error("Failed to list path {}", path, var11);
		}

		return set;
	}

	@Override
	public void close() {
	}

	/**
	 * {@code DirectoryBackedFactory}.
	 */
	public static class DirectoryBackedFactory implements ResourcePackProfile.PackFactory {

		private final Path path;

		public DirectoryBackedFactory(Path path) {
			this.path = path;
		}

		@Override
		public ResourcePack open(ResourcePackInfo info) {
			return new DirectoryResourcePack(info, this.path);
		}

		@Override
		public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
			ResourcePack resourcePack = this.open(info);
			List<String> list = metadata.overlays();
			if (list.isEmpty()) {
				return resourcePack;
			}
			else {
				List<ResourcePack> list2 = new ArrayList<>(list.size());

				for (String string : list) {
					Path path = this.path.resolve(string);
					list2.add(new DirectoryResourcePack(info, path));
				}

				return new OverlayResourcePack(resourcePack, list2);
			}
		}
	}
}
