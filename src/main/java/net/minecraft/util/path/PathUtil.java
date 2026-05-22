package net.minecraft.util.path;

import com.mojang.serialization.DataResult;
import net.minecraft.SharedConstants;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилиты для работы с путями файловой системы: валидация имён, генерация уникальных имён,
 * разбиение путей на сегменты и нормализация к POSIX-формату.
 */
public class PathUtil {

	private static final Pattern FILE_NAME_WITH_COUNT = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
	private static final int MAX_NAME_LENGTH = 255;
	private static final Pattern RESERVED_WINDOWS_NAMES = Pattern.compile(
			".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?",
			2
	);
	private static final Pattern VALID_FILE_NAME = Pattern.compile("[-._a-z0-9]+");
	private static final char SLASH = '/';

	public static String replaceInvalidChars(String fileName) {
		for (char c : SharedConstants.INVALID_CHARS_LEVEL_NAME) {
			fileName = fileName.replace(c, '_');
		}

		return fileName.replaceAll("[./\"]", "_");
	}

	/**
	 * Генерирует уникальное имя файла/директории в заданном пути, добавляя числовой суффикс
	 * {@code (N)} при конфликте. Имя очищается от недопустимых символов и зарезервированных
	 * Windows-имён перед проверкой.
	 */
	public static String getNextUniqueName(Path path, String name, String extension) throws IOException {
		name = replaceInvalidChars(name);

		if (!isNotReservedWindowsName(name)) {
			name = "_" + name + "_";
		}

		Matcher matcher = FILE_NAME_WITH_COUNT.matcher(name);
		int counter = 0;

		if (matcher.matches()) {
			name = matcher.group("name");
			counter = Integer.parseInt(matcher.group("count"));
		}

		if (name.length() > MAX_NAME_LENGTH - extension.length()) {
			name = name.substring(0, MAX_NAME_LENGTH - extension.length());
		}

		while (true) {
			String candidate = name;

			if (counter != 0) {
				String suffix = " (" + counter + ")";
				int maxBase = MAX_NAME_LENGTH - suffix.length();

				if (name.length() > maxBase) {
					candidate = name.substring(0, maxBase);
				}

				candidate = candidate + suffix;
			}

			candidate = candidate + extension;
			Path resolved = path.resolve(candidate);

			try {
				Path created = Files.createDirectory(resolved);
				Files.deleteIfExists(created);
				return path.relativize(created).toString();
			} catch (FileAlreadyExistsException ex) {
				counter++;
			}
		}
	}

	public static boolean isNormal(Path path) {
		return path.normalize().equals(path);
	}

	public static boolean isAllowedName(Path path) {
		for (Path segment : path) {
			if (!isNotReservedWindowsName(segment.toString())) {
				return false;
			}
		}

		return true;
	}

	public static boolean isNotReservedWindowsName(String path) {
		return !RESERVED_WINDOWS_NAMES.matcher(path).matches();
	}

	public static Path getResourcePath(Path path, String resourceName, String extension) {
		String fullName = resourceName + extension;
		Path resourcePath = Paths.get(fullName);

		if (resourcePath.endsWith(extension)) {
			throw new InvalidPathException(fullName, "empty resource name");
		}

		return path.resolve(resourcePath);
	}

	public static String getPosixFullPath(String path) {
		return FilenameUtils.getFullPath(path).replace(File.separator, "/");
	}

	public static String normalizeToPosix(String path) {
		return FilenameUtils.normalize(path).replace(File.separator, "/");
	}

	/**
	 * Разбивает строку пути (разделитель {@code /}) на список сегментов с валидацией каждого.
	 * Возвращает {@link DataResult#error} если путь содержит пустые сегменты, {@code .} или {@code ..},
	 * либо символы, не соответствующие паттерну {@code [-._a-z0-9]+}.
	 */
	public static DataResult<List<String>> split(String path) {
		int slashIndex = path.indexOf(SLASH);

		if (slashIndex == -1) {
			return switch (path) {
				case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + path + "'");
				default -> !isFileNameValid(path)
						? DataResult.error(() -> "Invalid path '" + path + "'")
						: DataResult.success(List.of(path));
			};
		}

		List<String> segments = new ArrayList<>();
		int start = 0;
		boolean reachedEnd = false;

		while (true) {
			String segment = path.substring(start, slashIndex);

			switch (segment) {
				case "", ".", ".." -> {
					return DataResult.error(() -> "Invalid segment '" + segment + "' in path '" + path + "'");
				}
			}

			if (!isFileNameValid(segment)) {
				return DataResult.error(() -> "Invalid segment '" + segment + "' in path '" + path + "'");
			}

			segments.add(segment);

			if (reachedEnd) {
				return DataResult.success(segments);
			}

			start = slashIndex + 1;
			slashIndex = path.indexOf(SLASH, start);

			if (slashIndex == -1) {
				slashIndex = path.length();
				reachedEnd = true;
			}
		}
	}

	public static Path getPath(Path root, List<String> paths) {
		int size = paths.size();

		return switch (size) {
			case 0 -> root;
			case 1 -> root.resolve(paths.get(0));
			default -> {
				String[] rest = new String[size - 1];

				for (int i = 1; i < size; i++) {
					rest[i - 1] = paths.get(i);
				}

				yield root.resolve(root.getFileSystem().getPath(paths.get(0), rest));
			}
		};
	}

	private static boolean isFileNameValid(String name) {
		return VALID_FILE_NAME.matcher(name).matches();
	}

	public static boolean isPathSegmentValid(String name) {
		return !name.equals("..") && !name.equals(".") && isFileNameValid(name);
	}

	public static void validatePath(String... paths) {
		if (paths.length == 0) {
			throw new IllegalArgumentException("Path must have at least one element");
		}

		for (String segment : paths) {
			if (!isPathSegmentValid(segment)) {
				throw new IllegalArgumentException(
						"Illegal segment " + segment + " in path " + Arrays.toString((Object[]) paths));
			}
		}
	}

	public static void createDirectories(Path path) throws IOException {
		Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
	}
}
