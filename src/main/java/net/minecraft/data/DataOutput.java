package net.minecraft.data;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

/**
 * Управляет корневым путём вывода генератора данных и предоставляет
 * вспомогательные методы для построения путей к конкретным файлам.
 */
public class DataOutput {

	private final Path path;

	public DataOutput(Path path) {
		this.path = path;
	}

	public Path getPath() {
		return path;
	}

	public Path resolvePath(OutputType outputType) {
		return path.resolve(outputType.path);
	}

	public PathResolver getResolver(OutputType outputType, String directoryName) {
		return new PathResolver(this, outputType, directoryName);
	}

	public PathResolver getResolver(RegistryKey<? extends Registry<?>> registryRef) {
		return getResolver(OutputType.DATA_PACK, RegistryKeys.getPath(registryRef));
	}

	public PathResolver getTagResolver(RegistryKey<? extends Registry<?>> registryRef) {
		return getResolver(OutputType.DATA_PACK, RegistryKeys.getTagPath(registryRef));
	}

	/**
	 * Тип выходной директории: пак данных, пак ресурсов или отчёты.
	 */
	public enum OutputType {
		DATA_PACK("data"),
		RESOURCE_PACK("assets"),
		REPORTS("reports");

		final String path;

		OutputType(String path) {
			this.path = path;
		}
	}

	/**
	 * Резолвер путей для конкретного типа вывода и директории.
	 * Строит пути вида {@code <outputType>/<namespace>/<directory>/<path>.json}.
	 */
	public static class PathResolver {

		private final Path rootPath;
		private final String directoryName;

		PathResolver(DataOutput dataOutput, OutputType outputType, String directoryName) {
			this.rootPath = dataOutput.resolvePath(outputType);
			this.directoryName = directoryName;
		}

		public Path resolve(Identifier id, String fileExtension) {
			return rootPath
					.resolve(id.getNamespace())
					.resolve(directoryName)
					.resolve(id.getPath() + "." + fileExtension);
		}

		public Path resolveJson(Identifier id) {
			return rootPath
					.resolve(id.getNamespace())
					.resolve(directoryName)
					.resolve(id.getPath() + ".json");
		}

		public Path resolveJson(RegistryKey<?> key) {
			return rootPath
					.resolve(key.getValue().getNamespace())
					.resolve(directoryName)
					.resolve(key.getValue().getPath() + ".json");
		}
	}
}
