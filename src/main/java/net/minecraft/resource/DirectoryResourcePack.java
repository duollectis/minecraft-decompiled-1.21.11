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
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Реализация {@link ResourcePack}, загружающая ресурсы из директории файловой системы.
 */
public class DirectoryResourcePack extends AbstractFileResourcePack {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Joiner SEPARATOR_JOINER = Joiner.on("/");
	private static final String DS_STORE = ".ds_store";

	private final Path root;

	public DirectoryResourcePack(ResourcePackInfo info, Path root) {
		super(info);
		this.root = root;
	}

	@Override
	public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
		PathUtil.validatePath(segments);
		Path path = PathUtil.getPath(root, List.of(segments));
		return Files.exists(path) ? InputSupplier.create(path) : null;
	}

	/**
	 * Проверяет, является ли путь допустимым с учётом регистра символов.
	 * В режиме разработки дополнительно фильтрует системные файлы macOS.
	 *
	 * @param path проверяемый путь
	 * @return {@code true}, если путь допустим
	 */
	public static boolean isValidPath(Path path) {
		if (!SharedConstants.VALIDATE_RESOURCE_PATH_CASE) {
			return true;
		}

		if (path.getFileSystem() != FileSystems.getDefault()) {
			return true;
		}

		try {
			return path.toRealPath().endsWith(path);
		} catch (IOException exception) {
			LOGGER.warn("Failed to resolve real path for {}", path, exception);
			return false;
		}
	}

	@Override
	public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
		Path namespacePath = root.resolve(type.getDirectory()).resolve(id.getNamespace());
		return open(id, namespacePath);
	}

	/**
	 * Открывает ресурс по идентификатору относительно указанного базового пути.
	 *
	 * @param id   идентификатор ресурса
	 * @param path базовый путь (директория пространства имён)
	 * @return поставщик потока или {@code null}, если файл не найден
	 */
	public static @Nullable InputSupplier<InputStream> open(Identifier id, Path path) {
		return PathUtil.split(id.getPath()).mapOrElse(
			segments -> openIfValid(PathUtil.getPath(path, segments)),
			error -> {
				LOGGER.error("Invalid path {}: {}", id, error.message());
				return null;
			}
		);
	}

	private static @Nullable InputSupplier<InputStream> openIfValid(Path path) {
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
			Path namespacePath = root.resolve(type.getDirectory()).resolve(namespace);
			findResources(namespace, namespacePath, prefixSegments, consumer);
		}).ifError(error -> LOGGER.error("Invalid path {}: {}", prefix, error.message()));
	}

	/**
	 * Рекурсивно находит все ресурсы в директории, соответствующие префиксу,
	 * и передаёт их в {@code consumer}.
	 *
	 * @param namespace      пространство имён
	 * @param path           базовая директория пространства имён
	 * @param prefixSegments сегменты пути-префикса
	 * @param consumer       получатель найденных ресурсов
	 */
	public static void findResources(
		String namespace,
		Path path,
		List<String> prefixSegments,
		ResourcePack.ResultConsumer consumer
	) {
		Path searchRoot = PathUtil.getPath(path, prefixSegments);

		try (Stream<Path> stream = Files.find(searchRoot, Integer.MAX_VALUE, DirectoryResourcePack::isRegularFile)) {
			stream.forEach(foundPath -> {
				String relativePath = SEPARATOR_JOINER.join(path.relativize(foundPath));
				Identifier identifier = Identifier.tryParse(namespace, relativePath);
				if (identifier == null) {
					Util.logErrorOrPause(String.format(
						Locale.ROOT,
						"Invalid path in pack: %s:%s, ignoring",
						namespace,
						relativePath
					));
				} else {
					consumer.accept(identifier, InputSupplier.create(foundPath));
				}
			});
		} catch (NotDirectoryException | NoSuchFileException ignored) {
		} catch (IOException exception) {
			LOGGER.error("Failed to list path {}", searchRoot, exception);
		}
	}

	private static boolean isRegularFile(Path path, BasicFileAttributes attributes) {
		if (!attributes.isRegularFile()) {
			return false;
		}

		return !SharedConstants.isDevelopment
			|| !StringUtils.equalsIgnoreCase(path.getFileName().toString(), DS_STORE);
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		Set<String> namespaces = Sets.newHashSet();
		Path typePath = root.resolve(type.getDirectory());

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(typePath)) {
			for (Path entry : directoryStream) {
				String name = entry.getFileName().toString();
				if (Identifier.isNamespaceValid(name)) {
					namespaces.add(name);
				} else {
					LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", name, root);
				}
			}
		} catch (NotDirectoryException | NoSuchFileException ignored) {
		} catch (IOException exception) {
			LOGGER.error("Failed to list path {}", typePath, exception);
		}

		return namespaces;
	}

	@Override
	public void close() {
	}

	/**
	 * Фабрика паков на основе директории файловой системы.
	 * Поддерживает создание паков с оверлеями.
	 */
	public static class DirectoryBackedFactory implements ResourcePackProfile.PackFactory {

		private final Path path;

		public DirectoryBackedFactory(Path path) {
			this.path = path;
		}

		@Override
		public ResourcePack open(ResourcePackInfo info) {
			return new DirectoryResourcePack(info, path);
		}

		@Override
		public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
			ResourcePack base = open(info);
			List<String> overlays = metadata.overlays();
			if (overlays.isEmpty()) {
				return base;
			}

			List<ResourcePack> overlayPacks = new ArrayList<>(overlays.size());
			for (String overlay : overlays) {
				overlayPacks.add(new DirectoryResourcePack(info, path.resolve(overlay)));
			}

			return new OverlayResourcePack(base, overlayPacks);
		}
	}
}
