package net.minecraft.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Исключение для ошибок в иерархических файлах конфигурации (например, JSON-файлах ресурс-паков).
 * <p>
 * Поддерживает накопление контекста через {@link #addInvalidKey(String)} и {@link #addInvalidFile(String)},
 * что позволяет формировать читаемое сообщение об ошибке с полным путём к проблемному элементу.
 */
public class InvalidHierarchicalFileException extends IOException {

	private static final String FILE_NOT_FOUND_MESSAGE = "File not found";

	private final List<File> invalidFiles = Lists.newArrayList();
	private final String errorMessage;

	public InvalidHierarchicalFileException(String message) {
		invalidFiles.add(new File());
		this.errorMessage = message;
	}

	public InvalidHierarchicalFileException(String message, Throwable cause) {
		super(cause);
		invalidFiles.add(new File());
		this.errorMessage = message;
	}

	/**
	 * Добавляет ключ к пути текущего файла в цепочке контекста.
	 *
	 * @param key ключ поля или элемента, в котором обнаружена ошибка
	 */
	public void addInvalidKey(String key) {
		invalidFiles.get(0).addKey(key);
	}

	/**
	 * Добавляет имя файла в цепочку контекста и создаёт новый уровень для дальнейшего накопления.
	 *
	 * @param fileName имя файла, в котором обнаружена ошибка
	 */
	public void addInvalidFile(String fileName) {
		invalidFiles.get(0).name = fileName;
		invalidFiles.add(0, new File());
	}

	@Override
	public String getMessage() {
		return "Invalid " + invalidFiles.get(invalidFiles.size() - 1) + ": " + errorMessage;
	}

	/**
	 * Оборачивает произвольное исключение в {@link InvalidHierarchicalFileException}.
	 * Если исключение уже является {@link InvalidHierarchicalFileException}, возвращает его без изменений.
	 * {@link FileNotFoundException} преобразуется в сообщение "File not found".
	 *
	 * @param cause исходное исключение
	 * @return обёрнутое исключение
	 */
	public static InvalidHierarchicalFileException wrap(Exception cause) {
		if (cause instanceof InvalidHierarchicalFileException hierarchical) {
			return hierarchical;
		}

		String message = cause instanceof FileNotFoundException ? FILE_NOT_FOUND_MESSAGE : cause.getMessage();
		return new InvalidHierarchicalFileException(message, cause);
	}

	/**
	 * Узел иерархии файла с именем и цепочкой ключей.
	 */
	public static class File {

		@Nullable String name;
		private final List<String> keys = Lists.newArrayList();

		File() {
		}

		void addKey(String key) {
			keys.add(0, key);
		}

		public @Nullable String getName() {
			return name;
		}

		/** @return цепочка ключей, разделённых {@code ->} */
		public String joinKeys() {
			return StringUtils.join(keys, "->");
		}

		@Override
		public String toString() {
			String keyPart = keys.isEmpty() ? "" : " " + joinKeys();
			return name != null
				? name + keyPart
				: "(Unknown file)" + keyPart;
		}
	}
}
