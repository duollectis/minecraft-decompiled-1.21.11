package net.minecraft.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Создаёт ZIP-архив через временный файл с атомарным переименованием при закрытии.
 * Реализует {@link Closeable}: при вызове {@link #close()} архив финализируется
 * и перемещается на целевой путь.
 */
public class ZipCompressor implements Closeable {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Path file;
	private final Path temp;
	private final FileSystem zip;

	public ZipCompressor(Path file) {
		this.file = file;
		temp = file.resolveSibling(file.getFileName().toString() + "_tmp");

		try {
			zip = Util.JAR_FILE_SYSTEM_PROVIDER.newFileSystem(temp, ImmutableMap.of("create", "true"));
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Записывает строковое содержимое в файл внутри архива по указанному пути.
	 *
	 * @param target  путь внутри архива
	 * @param content содержимое файла в кодировке UTF-8
	 */
	public void write(Path target, String content) {
		try {
			Path root = zip.getPath(File.separator);
			Path destination = root.resolve(target.toString());
			Files.createDirectories(destination.getParent());
			Files.write(destination, content.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Копирует файл {@code source} в архив по указанному пути {@code target}.
	 *
	 * @param target путь внутри архива
	 * @param source исходный файл
	 */
	public void copy(Path target, File source) {
		try {
			Path root = zip.getPath(File.separator);
			Path destination = root.resolve(target.toString());
			Files.createDirectories(destination.getParent());
			Files.copy(source.toPath(), destination);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Рекурсивно копирует все файлы из {@code source} в архив, сохраняя относительную структуру.
	 *
	 * @param source исходный файл или директория
	 */
	public void copyAll(Path source) {
		try {
			Path root = zip.getPath(File.separator);

			if (Files.isRegularFile(source)) {
				Path destination = root.resolve(source.getParent().relativize(source).toString());
				Files.copy(source, destination);
				return;
			}

			try (Stream<Path> stream = Files.find(source, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile())) {
				List<Path> files = stream.toList();

				for (Path file : files) {
					Path destination = root.resolve(source.relativize(file).toString());
					Files.createDirectories(destination.getParent());
					Files.copy(file, destination);
				}
			}
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Закрывает ZIP-файловую систему и атомарно перемещает временный файл на целевой путь.
	 */
	@Override
	public void close() {
		try {
			zip.close();
			Files.move(temp, file);
			LOGGER.info("Compressed to {}", file);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}
