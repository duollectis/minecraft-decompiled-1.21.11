package net.minecraft.resource.fs;

import java.nio.file.Path;
import java.util.Map;

/**
 * Узел виртуальной файловой системы ресурсов.
 *
 * <p>Может представлять реальный файл ({@link File}), директорию ({@link Directory}),
 * либо специальные маркеры {@link #EMPTY} (несуществующий путь) и {@link #RELATIVE}
 * (относительный путь без привязки к реальному содержимому).</p>
 */
interface ResourceFile {

	/** Маркер несуществующего пути (аналог 404 в файловой системе). */
	ResourceFile EMPTY = new ResourceFile() {
		@Override
		public String toString() {
			return "empty";
		}
	};

	/** Маркер относительного пути, не привязанного к реальному содержимому. */
	ResourceFile RELATIVE = new ResourceFile() {
		@Override
		public String toString() {
			return "relative";
		}
	};

	/**
	 * Директория виртуальной файловой системы.
	 *
	 * @param children дочерние узлы (файлы и поддиректории), индексированные по имени
	 */
	record Directory(Map<String, ResourcePath> children) implements ResourceFile {
	}

	/**
	 * Реальный файл, делегирующий чтение к физическому пути на диске.
	 *
	 * @param contents физический путь к содержимому файла
	 */
	record File(Path contents) implements ResourceFile {
	}
}
