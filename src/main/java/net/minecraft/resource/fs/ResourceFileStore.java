package net.minecraft.resource.fs;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * Реализация {@link FileStore} для виртуальной файловой системы ресурсов.
 *
 * <p>Файловое хранилище доступно только для чтения, не занимает реального дискового
 * пространства и поддерживает только атрибуты типа {@code "basic"}.</p>
 */
class ResourceFileStore extends FileStore {

	private final String name;

	public ResourceFileStore(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String type() {
		return "index";
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public long getTotalSpace() {
		return 0L;
	}

	@Override
	public long getUsableSpace() {
		return 0L;
	}

	@Override
	public long getUnallocatedSpace() {
		return 0L;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return type == BasicFileAttributeView.class;
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return "basic".equals(name);
	}

	@Override
	public <V extends FileStoreAttributeView> @Nullable V getFileStoreAttributeView(Class<V> type) {
		return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		throw new UnsupportedOperationException();
	}
}
