package net.minecraft.util.path;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Утилита для разрешения {@link URI} в {@link Path} с автоматическим монтированием
 * файловой системы (например, ZIP/JAR), если она ещё не зарегистрирована в JVM.
 */
public class PathResolving {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Преобразует {@link URI} в {@link Path}. Если файловая система для данного URI
	 * не зарегистрирована, монтирует её через {@link FileSystems#newFileSystem(URI, java.util.Map)}.
	 * Повторное монтирование уже существующей ФС игнорируется.
	 */
	public static Path toPath(URI uri) throws IOException {
		try {
			return Paths.get(uri);
		} catch (FileSystemNotFoundException ignored) {
		} catch (Throwable ex) {
			LOGGER.warn("Unable to get path for: {}", uri, ex);
		}

		try {
			FileSystems.newFileSystem(uri, Collections.emptyMap());
		} catch (FileSystemAlreadyExistsException ignored) {
		}

		return Paths.get(uri);
	}
}
