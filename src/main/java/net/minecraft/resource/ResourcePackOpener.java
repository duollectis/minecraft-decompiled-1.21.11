package net.minecraft.resource;

import net.minecraft.util.path.SymlinkEntry;
import net.minecraft.util.path.SymlinkFinder;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Абстрактный открыватель ресурс-паков из файловой системы.
 * Определяет тип записи (директория или zip-архив) и делегирует открытие
 * соответствующему методу. Проверяет символические ссылки перед открытием.
 *
 * @param <T> тип результата открытия пака
 */
public abstract class ResourcePackOpener<T> {

	private static final String ZIP_EXTENSION = ".zip";
	private static final String PACK_META_FILE = "pack.mcmeta";

	private final SymlinkFinder symlinkFinder;

	protected ResourcePackOpener(SymlinkFinder symlinkFinder) {
		this.symlinkFinder = symlinkFinder;
	}

	/**
	 * Открывает пак по указанному пути.
	 * Разрешает символические ссылки, проверяет их безопасность,
	 * затем определяет тип (директория или zip) и открывает соответственно.
	 *
	 * @param path          путь к паку
	 * @param foundSymlinks список для записи найденных символических ссылок
	 * @return открытый пак или {@code null}, если путь не является паком
	 * @throws IOException при ошибке чтения файловой системы
	 */
	public @Nullable T open(Path path, List<SymlinkEntry> foundSymlinks) throws IOException {
		BasicFileAttributes attributes;
		try {
			attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		} catch (NoSuchFileException ignored) {
			return null;
		}

		Path resolvedPath = path;
		if (attributes.isSymbolicLink()) {
			symlinkFinder.validate(path, foundSymlinks);
			if (!foundSymlinks.isEmpty()) {
				return null;
			}

			resolvedPath = Files.readSymbolicLink(path);
			attributes = Files.readAttributes(resolvedPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		}

		if (attributes.isDirectory()) {
			symlinkFinder.validateRecursively(resolvedPath, foundSymlinks);
			if (!foundSymlinks.isEmpty()) {
				return null;
			}

			return Files.isRegularFile(resolvedPath.resolve(PACK_META_FILE))
				? openDirectory(resolvedPath)
				: null;
		}

		return attributes.isRegularFile() && resolvedPath.getFileName().toString().endsWith(ZIP_EXTENSION)
			? openZip(resolvedPath)
			: null;
	}

	/**
	 * Открывает пак из zip-архива.
	 *
	 * @param path путь к zip-файлу
	 * @return открытый пак или {@code null}
	 * @throws IOException при ошибке чтения
	 */
	protected abstract @Nullable T openZip(Path path) throws IOException;

	/**
	 * Открывает пак из директории.
	 *
	 * @param path путь к директории
	 * @return открытый пак или {@code null}
	 * @throws IOException при ошибке чтения
	 */
	protected abstract @Nullable T openDirectory(Path path) throws IOException;
}
