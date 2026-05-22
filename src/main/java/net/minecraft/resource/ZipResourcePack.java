package net.minecraft.resource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Реализация {@link ResourcePack}, загружающая ресурсы из zip-архива.
 * Поддерживает оверлеи через префикс пути внутри архива.
 */
public class ZipResourcePack extends AbstractFileResourcePack {

	static final Logger LOGGER = LogUtils.getLogger();

	/** Символ-разделитель пути внутри zip-архива. */
	private static final char ZIP_PATH_SEPARATOR = '/';

	private final ZipResourcePack.ZipFileWrapper zipFile;
	private final String overlay;

	ZipResourcePack(ResourcePackInfo info, ZipResourcePack.ZipFileWrapper zipFile, String overlay) {
		super(info);
		this.zipFile = zipFile;
		this.overlay = overlay;
	}

	private static String toPath(ResourceType type, Identifier id) {
		return String.format(Locale.ROOT, "%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getPath());
	}

	@Override
	public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
		return openFile(String.join("/", segments));
	}

	@Override
	public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
		return openFile(toPath(type, id));
	}

	private String appendOverlayPrefix(String path) {
		return overlay.isEmpty() ? path : overlay + "/" + path;
	}

	private @Nullable InputSupplier<InputStream> openFile(String path) {
		ZipFile zip = zipFile.open();
		if (zip == null) {
			return null;
		}

		ZipEntry entry = zip.getEntry(appendOverlayPrefix(path));
		return entry == null ? null : InputSupplier.create(zip, entry);
	}

	@Override
	public Set<String> getNamespaces(ResourceType type) {
		ZipFile zip = zipFile.open();
		if (zip == null) {
			return Set.of();
		}

		Enumeration<? extends ZipEntry> entries = zip.entries();
		Set<String> namespaces = Sets.newHashSet();
		String prefix = appendOverlayPrefix(type.getDirectory() + "/");

		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			String namespace = getNamespace(prefix, entry.getName());
			if (namespace.isEmpty()) {
				continue;
			}

			if (Identifier.isNamespaceValid(namespace)) {
				namespaces.add(namespace);
			} else {
				LOGGER.warn(
					"Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring",
					namespace,
					zipFile.file
				);
			}
		}

		return namespaces;
	}

	/**
	 * Извлекает пространство имён из имени записи zip-архива по заданному префиксу.
	 * Возвращает пустую строку, если запись не соответствует префиксу.
	 *
	 * @param prefix    ожидаемый префикс (например, {@code "assets/"})
	 * @param entryName имя записи в архиве
	 * @return пространство имён или пустая строка
	 */
	@VisibleForTesting
	public static String getNamespace(String prefix, String entryName) {
		if (!entryName.startsWith(prefix)) {
			return "";
		}

		int start = prefix.length();
		int separatorIndex = entryName.indexOf(ZIP_PATH_SEPARATOR, start);
		return separatorIndex == -1
			? entryName.substring(start)
			: entryName.substring(start, separatorIndex);
	}

	@Override
	public void close() {
		zipFile.close();
	}

	@Override
	public void findResources(
		ResourceType type,
		String namespace,
		String prefix,
		ResourcePack.ResultConsumer consumer
	) {
		ZipFile zip = zipFile.open();
		if (zip == null) {
			return;
		}

		Enumeration<? extends ZipEntry> entries = zip.entries();
		String namespacePath = appendOverlayPrefix(type.getDirectory() + "/" + namespace + "/");
		String searchPrefix = namespacePath + prefix + "/";

		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.isDirectory()) {
				continue;
			}

			String entryName = entry.getName();
			if (!entryName.startsWith(searchPrefix)) {
				continue;
			}

			String relativePath = entryName.substring(namespacePath.length());
			Identifier identifier = Identifier.tryParse(namespace, relativePath);
			if (identifier != null) {
				consumer.accept(identifier, InputSupplier.create(zip, entry));
			} else {
				LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", namespace, relativePath);
			}
		}
	}

	/**
	 * Фабрика паков на основе zip-архива.
	 * Поддерживает создание паков с оверлеями.
	 */
	public static class ZipBackedFactory implements ResourcePackProfile.PackFactory {

		private final File file;

		public ZipBackedFactory(Path path) {
			this(path.toFile());
		}

		public ZipBackedFactory(File file) {
			this.file = file;
		}

		@Override
		public ResourcePack open(ResourcePackInfo info) {
			return new ZipResourcePack(info, new ZipResourcePack.ZipFileWrapper(file), "");
		}

		@Override
		public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
			ZipResourcePack.ZipFileWrapper wrapper = new ZipResourcePack.ZipFileWrapper(file);
			ResourcePack base = new ZipResourcePack(info, wrapper, "");
			List<String> overlays = metadata.overlays();
			if (overlays.isEmpty()) {
				return base;
			}

			List<ResourcePack> overlayPacks = new ArrayList<>(overlays.size());
			for (String overlay : overlays) {
				overlayPacks.add(new ZipResourcePack(info, wrapper, overlay));
			}

			return new OverlayResourcePack(base, overlayPacks);
		}
	}

	/**
	 * Обёртка над {@link ZipFile} с ленивым открытием и защитой от повторного открытия после закрытия.
	 */
	static class ZipFileWrapper implements AutoCloseable {

		final File file;
		private @Nullable ZipFile zip;
		private boolean closed;

		ZipFileWrapper(File file) {
			this.file = file;
		}

		@Nullable ZipFile open() {
			if (closed) {
				return null;
			}

			if (zip == null) {
				try {
					zip = new ZipFile(file);
				} catch (IOException exception) {
					LOGGER.error("Failed to open pack {}", file, exception);
					closed = true;
					return null;
				}
			}

			return zip;
		}

		@Override
		public void close() {
			if (zip != null) {
				IOUtils.closeQuietly(zip);
				zip = null;
			}
		}

		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
	}
}
