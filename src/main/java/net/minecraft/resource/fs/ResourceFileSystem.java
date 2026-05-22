package net.minecraft.resource.fs;

import com.google.common.base.Splitter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Виртуальная файловая система только для чтения, построенная поверх индекса ресурсов.
 *
 * <p>Используется для предоставления доступа к ресурсам пакетов через стандартный
 * Java NIO {@link FileSystem} API. Дерево путей строится один раз при создании
 * и остаётся иммутабельным на протяжении всего жизненного цикла.</p>
 *
 * <p>Создаётся через {@link Builder}: {@code ResourceFileSystem.builder().withFile(...).build(name)}.</p>
 */
public class ResourceFileSystem extends FileSystem {

	private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = Set.of("basic");
	private static final Splitter SEPARATOR_SPLITTER = Splitter.on('/');

	/** Разделитель путей в этой файловой системе. */
	public static final String SEPARATOR = "/";

	private final FileStore store;
	private final FileSystemProvider fileSystemProvider = new ResourceFileSystemProvider();
	private final ResourcePath root;

	ResourceFileSystem(String name, Directory root) {
		this.store = new ResourceFileStore(name);
		this.root = toResourcePath(root, this, "", null);
	}

	/**
	 * Рекурсивно строит дерево {@link ResourcePath} из структуры {@link Directory}.
	 *
	 * <p>Использует {@link Object2ObjectOpenHashMap} для минимизации потребления памяти
	 * (вызов {@code trim()} после заполнения убирает лишние слоты хэш-таблицы).</p>
	 */
	private static ResourcePath toResourcePath(
			Directory root,
			ResourceFileSystem fileSystem,
			String name,
			@Nullable ResourcePath parent
	) {
		Object2ObjectOpenHashMap<String, ResourcePath> children = new Object2ObjectOpenHashMap<>();
		ResourcePath resourcePath = new ResourcePath(fileSystem, name, parent, new ResourceFile.Directory(children));

		root.files.forEach((fileName, path) ->
				children.put(fileName, new ResourcePath(fileSystem, fileName, resourcePath, new ResourceFile.File(path)))
		);

		root.children.forEach((directoryName, directory) ->
				children.put(directoryName, toResourcePath(directory, fileSystem, directoryName, resourcePath))
		);

		children.trim();
		return resourcePath;
	}

	@Override
	public FileSystemProvider provider() {
		return fileSystemProvider;
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public String getSeparator() {
		return SEPARATOR;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return List.of(root);
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		return List.of(store);
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
	}

	/**
	 * Разрешает путь по строковым компонентам.
	 *
	 * <p>Абсолютные пути (начинающиеся с {@code /}) разрешаются от корня файловой системы.
	 * Относительные пути создают цепочку {@link ResourcePath} с маркером {@link ResourceFile#RELATIVE}.</p>
	 *
	 * @throws IllegalArgumentException если путь содержит пустые сегменты
	 */
	@Override
	public Path getPath(String first, String... more) {
		Stream<String> parts = Stream.of(first);

		if (more.length > 0) {
			parts = Stream.concat(parts, Stream.of(more));
		}

		String fullPath = parts.collect(Collectors.joining(SEPARATOR));

		if (fullPath.equals(SEPARATOR)) {
			return root;
		}

		if (fullPath.startsWith(SEPARATOR)) {
			ResourcePath current = root;

			for (String segment : SEPARATOR_SPLITTER.split(fullPath.substring(1))) {
				if (segment.isEmpty()) {
					throw new IllegalArgumentException("Empty paths not allowed");
				}

				current = current.get(segment);
			}

			return current;
		}

		ResourcePath current = null;

		for (String segment : SEPARATOR_SPLITTER.split(fullPath)) {
			if (segment.isEmpty()) {
				throw new IllegalArgumentException("Empty paths not allowed");
			}

			current = new ResourcePath(this, segment, current, ResourceFile.RELATIVE);
		}

		if (current == null) {
			throw new IllegalArgumentException("Empty paths not allowed");
		}

		return current;
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() {
		throw new UnsupportedOperationException();
	}

	public FileStore getStore() {
		return store;
	}

	public ResourcePath getRoot() {
		return root;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Строитель виртуальной файловой системы.
	 *
	 * <p>Позволяет добавлять файлы по пути из списка директорий и имени файла.
	 * После вызова {@link #build(String)} дерево фиксируется и становится иммутабельным.</p>
	 */
	public static class Builder {

		private final Directory root = new Directory();

		/**
		 * Добавляет файл по явно указанному пути директорий и имени.
		 *
		 * @param directories список сегментов пути директорий
		 * @param name имя файла
		 * @param path физический путь к содержимому
		 */
		public Builder withFile(List<String> directories, String name, Path path) {
			Directory directory = root;

			for (String segment : directories) {
				directory = directory.children.computeIfAbsent(segment, key -> new Directory());
			}

			directory.files.put(name, path);
			return this;
		}

		/**
		 * Добавляет файл, используя последний элемент списка как имя файла.
		 *
		 * @param directories список сегментов пути (последний — имя файла)
		 * @param path физический путь к содержимому
		 * @throws IllegalArgumentException если список пуст
		 */
		public Builder withFile(List<String> directories, Path path) {
			if (directories.isEmpty()) {
				throw new IllegalArgumentException("Path can't be empty");
			}

			int lastIndex = directories.size() - 1;
			return withFile(directories.subList(0, lastIndex), directories.get(lastIndex), path);
		}

		public FileSystem build(String name) {
			return new ResourceFileSystem(name, root);
		}
	}

	/**
	 * Узел директории в дереве виртуальной файловой системы.
	 *
	 * @param children вложенные директории, индексированные по имени
	 * @param files файлы в этой директории, индексированные по имени
	 */
	record Directory(Map<String, Directory> children, Map<String, Path> files) {

		public Directory() {
			this(new HashMap<>(), new HashMap<>());
		}
	}
}
