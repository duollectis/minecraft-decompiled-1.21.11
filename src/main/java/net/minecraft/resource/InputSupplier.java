package net.minecraft.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Функциональный интерфейс для ленивого получения ресурса, который может бросить {@link IOException}.
 *
 * @param <T> тип возвращаемого ресурса
 */
@FunctionalInterface
public interface InputSupplier<T> {

	/**
	 * Создаёт поставщик, открывающий файл по указанному пути.
	 *
	 * @param path путь к файлу
	 * @return поставщик входного потока
	 */
	static InputSupplier<InputStream> create(Path path) {
		return () -> Files.newInputStream(path);
	}

	/**
	 * Создаёт поставщик, открывающий запись из zip-архива.
	 *
	 * @param zipFile  открытый zip-архив
	 * @param zipEntry запись в архиве
	 * @return поставщик входного потока
	 */
	static InputSupplier<InputStream> create(ZipFile zipFile, ZipEntry zipEntry) {
		return () -> zipFile.getInputStream(zipEntry);
	}

	/**
	 * Получает ресурс.
	 *
	 * @return ресурс
	 * @throws IOException при ошибке ввода-вывода
	 */
	T get() throws IOException;
}
