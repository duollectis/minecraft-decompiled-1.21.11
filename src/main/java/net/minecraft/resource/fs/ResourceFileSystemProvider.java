package net.minecraft.resource.fs;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

/**
 * Реализация {@link FileSystemProvider} для виртуальной файловой системы ресурсов.
 *
 * <p>Файловая система доступна только для чтения. Все операции записи
 * ({@code createDirectory}, {@code delete}, {@code copy}, {@code move}, {@code setAttribute})
 * выбрасывают {@link ReadOnlyFileSystemException}.</p>
 *
 * <p>Схема URI: {@value #SCHEME}.</p>
 */
class ResourceFileSystemProvider extends FileSystemProvider {

	public static final String SCHEME = "x-mc-link";

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path getPath(URI uri) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Открывает байтовый канал для чтения файла.
	 *
	 * <p>Поддерживается только режим чтения. Любые опции записи
	 * ({@code CREATE_NEW}, {@code CREATE}, {@code APPEND}, {@code WRITE}) запрещены.</p>
	 *
	 * @throws UnsupportedOperationException если запрошен режим записи
	 * @throws NoSuchFileException если путь не указывает на реальный файл
	 */
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
	throws IOException {
		if (options.contains(StandardOpenOption.CREATE_NEW)
				|| options.contains(StandardOpenOption.CREATE)
				|| options.contains(StandardOpenOption.APPEND)
				|| options.contains(StandardOpenOption.WRITE)) {
			throw new UnsupportedOperationException();
		}

		Path realPath = toResourcePath(path).toAbsolutePath().toPath();

		if (realPath == null) {
			throw new NoSuchFileException(path.toString());
		}

		return Files.newByteChannel(realPath, options, attrs);
	}

	/**
	 * Открывает поток содержимого директории с применением фильтра.
	 *
	 * @throws NotDirectoryException если путь не является директорией
	 */
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		ResourceFile.Directory directory = toResourcePath(dir).toAbsolutePath().toDirectory();

		if (directory == null) {
			throw new NotDirectoryException(dir.toString());
		}

		return new DirectoryStream<>() {
			@Override
			public java.util.Iterator<Path> iterator() {
				return directory.children().values().stream()
						.filter(child -> {
							try {
								return filter.accept(child);
							} catch (IOException exception) {
								throw new DirectoryIteratorException(exception);
							}
						})
						.map(child -> (Path) child)
						.iterator();
			}

			@Override
			public void close() {
			}
		};
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void delete(Path path) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) {
		throw new ReadOnlyFileSystemException();
	}

	@Override
	public boolean isSameFile(Path path, Path path2) {
		return path instanceof ResourcePath && path2 instanceof ResourcePath && path.equals(path2);
	}

	@Override
	public boolean isHidden(Path path) {
		return false;
	}

	@Override
	public FileStore getFileStore(Path path) {
		return toResourcePath(path).getFileSystem().getStore();
	}

	/**
	 * Проверяет доступность пути для заданных режимов доступа.
	 *
	 * <p>Режимы {@code WRITE} и {@code EXECUTE} всегда запрещены.
	 * Режим {@code READ} разрешён только для существующих (не пустых) путей.</p>
	 *
	 * @throws NoSuchFileException если путь не существует и запрошен режим {@code READ}
	 * @throws AccessDeniedException если запрошен режим {@code WRITE} или {@code EXECUTE}
	 */
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		if (modes.length == 0 && !toResourcePath(path).isReadable()) {
			throw new NoSuchFileException(path.toString());
		}

		for (AccessMode accessMode : modes) {
			switch (accessMode) {
				case READ -> {
					if (!toResourcePath(path).isReadable()) {
						throw new NoSuchFileException(path.toString());
					}
				}
				case EXECUTE, WRITE -> throw new AccessDeniedException(accessMode.toString());
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V extends FileAttributeView> @Nullable V getFileAttributeView(
			Path path,
			Class<V> type,
			LinkOption... options
	) {
		ResourcePath resourcePath = toResourcePath(path);
		return type == BasicFileAttributeView.class ? (V) resourcePath.getAttributeView() : null;
	}

	/**
	 * Читает атрибуты файла по пути.
	 *
	 * <p>Поддерживается только тип {@link BasicFileAttributes}.</p>
	 *
	 * @throws UnsupportedOperationException если запрошен неподдерживаемый тип атрибутов
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
	throws IOException {
		ResourcePath resourcePath = toResourcePath(path).toAbsolutePath();

		if (type == BasicFileAttributes.class) {
			return (A) resourcePath.getAttributes();
		}

		throw new UnsupportedOperationException("Attributes of type " + type.getName() + " not supported");
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
		throw new ReadOnlyFileSystemException();
	}

	private static ResourcePath toResourcePath(@Nullable Path path) {
		if (path == null) {
			throw new NullPointerException();
		}

		if (path instanceof ResourcePath resourcePath) {
			return resourcePath;
		}

		throw new ProviderMismatchException();
	}
}
