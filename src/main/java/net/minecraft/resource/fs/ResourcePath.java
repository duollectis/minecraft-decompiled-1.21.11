package net.minecraft.resource.fs;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link Path} для виртуальной файловой системы ресурсов.
 *
 * <p>Каждый путь хранит ссылку на свой {@link ResourceFile}-узел, который определяет
 * тип пути: директория ({@link ResourceFile.Directory}), файл ({@link ResourceFile.File}),
 * несуществующий путь ({@link ResourceFile#EMPTY}) или относительный маркер
 * ({@link ResourceFile#RELATIVE}).</p>
 *
 * <p>Список имён сегментов и строковое представление пути кэшируются лениво.</p>
 */
class ResourcePath implements Path {

	private static final BasicFileAttributes DIRECTORY_ATTRIBUTES = new ResourceFileAttributes() {
		@Override
		public boolean isRegularFile() {
			return false;
		}

		@Override
		public boolean isDirectory() {
			return true;
		}
	};

	private static final BasicFileAttributes FILE_ATTRIBUTES = new ResourceFileAttributes() {
		@Override
		public boolean isRegularFile() {
			return true;
		}

		@Override
		public boolean isDirectory() {
			return false;
		}
	};

	private static final Comparator<ResourcePath> COMPARATOR = Comparator.comparing(ResourcePath::getPathString);

	private final String name;
	private final ResourceFileSystem fileSystem;
	private final @Nullable ResourcePath parent;
	private final ResourceFile file;

	// Ленивые кэши — вычисляются при первом обращении
	private @Nullable List<String> names;
	private @Nullable String pathString;

	public ResourcePath(ResourceFileSystem fileSystem, String name, @Nullable ResourcePath parent, ResourceFile file) {
		this.fileSystem = fileSystem;
		this.name = name;
		this.parent = parent;
		this.file = file;
	}

	private ResourcePath relativize(@Nullable ResourcePath parent, String segmentName) {
		return new ResourcePath(fileSystem, segmentName, parent, ResourceFile.RELATIVE);
	}

	public ResourceFileSystem getFileSystem() {
		return fileSystem;
	}

	@Override
	public boolean isAbsolute() {
		return file != ResourceFile.RELATIVE;
	}

	@Override
	public File toFile() {
		if (file instanceof ResourceFile.File realFile) {
			return realFile.contents().toFile();
		}

		throw new UnsupportedOperationException("Path " + getPathString() + " does not represent file");
	}

	public @Nullable ResourcePath getRoot() {
		return isAbsolute() ? fileSystem.getRoot() : null;
	}

	public ResourcePath getFileName() {
		return relativize(null, name);
	}

	public @Nullable ResourcePath getParent() {
		return parent;
	}

	@Override
	public int getNameCount() {
		return getNames().size();
	}

	private List<String> getNames() {
		if (name.isEmpty()) {
			return List.of();
		}

		if (names == null) {
			Builder<String> builder = ImmutableList.builder();

			if (parent != null) {
				builder.addAll(parent.getNames());
			}

			builder.add(name);
			names = builder.build();
		}

		return names;
	}

	public ResourcePath getName(int index) {
		List<String> segments = getNames();

		if (index < 0 || index >= segments.size()) {
			throw new IllegalArgumentException("Invalid index: " + index);
		}

		return relativize(null, segments.get(index));
	}

	public ResourcePath subpath(int from, int to) {
		List<String> segments = getNames();

		if (from < 0 || to > segments.size() || from >= to) {
			throw new IllegalArgumentException();
		}

		ResourcePath result = null;

		for (int index = from; index < to; index++) {
			result = relativize(result, segments.get(index));
		}

		return result;
	}

	@Override
	public boolean startsWith(Path other) {
		if (other.isAbsolute() != isAbsolute()) {
			return false;
		}

		if (!(other instanceof ResourcePath otherPath)) {
			return false;
		}

		if (otherPath.fileSystem != fileSystem) {
			return false;
		}

		List<String> myNames = getNames();
		List<String> otherNames = otherPath.getNames();
		int otherSize = otherNames.size();

		if (otherSize > myNames.size()) {
			return false;
		}

		for (int index = 0; index < otherSize; index++) {
			if (!otherNames.get(index).equals(myNames.get(index))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean endsWith(Path other) {
		if (other.isAbsolute() && !isAbsolute()) {
			return false;
		}

		if (!(other instanceof ResourcePath otherPath)) {
			return false;
		}

		if (otherPath.fileSystem != fileSystem) {
			return false;
		}

		List<String> myNames = getNames();
		List<String> otherNames = otherPath.getNames();
		int otherSize = otherNames.size();
		int offset = myNames.size() - otherSize;

		if (offset < 0) {
			return false;
		}

		for (int index = otherSize - 1; index >= 0; index--) {
			if (!otherNames.get(index).equals(myNames.get(offset + index))) {
				return false;
			}
		}

		return true;
	}

	public ResourcePath normalize() {
		return this;
	}

	public ResourcePath resolve(Path path) {
		ResourcePath resolved = toResourcePath(path);
		return path.isAbsolute() ? resolved : get(resolved.getNames());
	}

	private ResourcePath get(List<String> segments) {
		ResourcePath current = this;

		for (String segment : segments) {
			current = current.get(segment);
		}

		return current;
	}

	ResourcePath get(String segmentName) {
		if (isSpecial(file)) {
			return new ResourcePath(fileSystem, segmentName, this, file);
		}

		if (file instanceof ResourceFile.Directory directory) {
			ResourcePath child = directory.children().get(segmentName);
			return child != null
					? child
					: new ResourcePath(fileSystem, segmentName, this, ResourceFile.EMPTY);
		}

		if (file instanceof ResourceFile.File) {
			return new ResourcePath(fileSystem, segmentName, this, ResourceFile.EMPTY);
		}

		throw new AssertionError("All content types should be already handled");
	}

	private static boolean isSpecial(ResourceFile file) {
		return file == ResourceFile.EMPTY || file == ResourceFile.RELATIVE;
	}

	/**
	 * Вычисляет относительный путь от этого пути до {@code path}.
	 *
	 * @throws IllegalArgumentException если пути несовместимы по абсолютности или не имеют общего префикса
	 */
	public ResourcePath relativize(Path path) {
		ResourcePath target = toResourcePath(path);

		if (isAbsolute() != target.isAbsolute()) {
			throw new IllegalArgumentException("absolute mismatch");
		}

		List<String> myNames = getNames();
		List<String> targetNames = target.getNames();

		if (myNames.size() >= targetNames.size()) {
			throw new IllegalArgumentException();
		}

		for (int index = 0; index < myNames.size(); index++) {
			if (!myNames.get(index).equals(targetNames.get(index))) {
				throw new IllegalArgumentException();
			}
		}

		return target.subpath(myNames.size(), targetNames.size());
	}

	@Override
	public URI toUri() {
		try {
			return new URI(ResourceFileSystemProvider.SCHEME, fileSystem.getStore().name(), getPathString(), null);
		} catch (URISyntaxException exception) {
			throw new AssertionError("Failed to create URI", exception);
		}
	}

	public ResourcePath toAbsolutePath() {
		return isAbsolute() ? this : fileSystem.getRoot().resolve(this);
	}

	public ResourcePath toRealPath(LinkOption... linkOptions) {
		return toAbsolutePath();
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(Path path) {
		return COMPARATOR.compare(this, toResourcePath(path));
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (!(o instanceof ResourcePath other)) {
			return false;
		}

		if (fileSystem != other.fileSystem) {
			return false;
		}

		boolean normal = isNormal();

		if (normal != other.isNormal()) {
			return false;
		}

		return normal
				? file == other.file
				: Objects.equals(parent, other.parent) && Objects.equals(name, other.name);
	}

	private boolean isNormal() {
		return !isSpecial(file);
	}

	@Override
	public int hashCode() {
		return isNormal() ? file.hashCode() : name.hashCode();
	}

	@Override
	public String toString() {
		return getPathString();
	}

	private String getPathString() {
		if (pathString == null) {
			StringBuilder builder = new StringBuilder();

			if (isAbsolute()) {
				builder.append("/");
			}

			Joiner.on("/").appendTo(builder, getNames());
			pathString = builder.toString();
		}

		return pathString;
	}

	private ResourcePath toResourcePath(@Nullable Path path) {
		if (path == null) {
			throw new NullPointerException();
		}

		if (path instanceof ResourcePath resourcePath && resourcePath.fileSystem == fileSystem) {
			return resourcePath;
		}

		throw new ProviderMismatchException();
	}

	public boolean isReadable() {
		return isNormal();
	}

	public @Nullable Path toPath() {
		return file instanceof ResourceFile.File realFile ? realFile.contents() : null;
	}

	public ResourceFile.@Nullable Directory toDirectory() {
		return file instanceof ResourceFile.Directory directory ? directory : null;
	}

	/**
	 * Возвращает представление атрибутов этого пути в виде {@link BasicFileAttributeView}.
	 *
	 * <p>Операция {@code setTimes} запрещена — файловая система только для чтения.</p>
	 */
	public BasicFileAttributeView getAttributeView() {
		return new BasicFileAttributeView() {
			@Override
			public String name() {
				return "basic";
			}

			@Override
			public BasicFileAttributes readAttributes() throws IOException {
				return ResourcePath.this.getAttributes();
			}

			@Override
			public void setTimes(FileTime lastModifiedTime, FileTime lastAccessFile, FileTime createTime) {
				throw new ReadOnlyFileSystemException();
			}
		};
	}

	/**
	 * Возвращает атрибуты файла или директории.
	 *
	 * @throws NoSuchFileException если путь не существует (маркер {@link ResourceFile#EMPTY} или {@link ResourceFile#RELATIVE})
	 */
	public BasicFileAttributes getAttributes() throws IOException {
		if (file instanceof ResourceFile.Directory) {
			return DIRECTORY_ATTRIBUTES;
		}

		if (file instanceof ResourceFile.File) {
			return FILE_ATTRIBUTES;
		}

		throw new NoSuchFileException(getPathString());
	}
}
