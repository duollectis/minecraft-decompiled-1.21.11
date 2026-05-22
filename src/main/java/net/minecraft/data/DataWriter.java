package net.minecraft.data;

import com.google.common.hash.HashCode;
import net.minecraft.util.path.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Интерфейс записи сгенерированных данных на диск.
 * Реализации могут использовать кэш для пропуска неизменившихся файлов.
 */
public interface DataWriter {

	/**
	 * Реализация без кэша — всегда записывает файл на диск.
	 * Используется в тестах и при принудительной перегенерации.
	 */
	DataWriter UNCACHED = (path, data, hashCode) -> {
		PathUtil.createDirectories(path.getParent());
		Files.write(path, data);
	};

	void write(Path path, byte[] data, HashCode hashCode) throws IOException;
}
