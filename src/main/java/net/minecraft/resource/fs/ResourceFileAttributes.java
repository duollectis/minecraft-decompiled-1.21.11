package net.minecraft.resource.fs;

import org.jspecify.annotations.Nullable;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Базовая реализация {@link BasicFileAttributes} для виртуальной файловой системы ресурсов.
 *
 * <p>Все временны́е метки возвращают эпоху (0 мс), размер — 0, символические ссылки
 * и «другие» типы не поддерживаются. Подклассы обязаны реализовать
 * {@link #isRegularFile()} и {@link #isDirectory()}.</p>
 */
abstract class ResourceFileAttributes implements BasicFileAttributes {

	private static final FileTime EPOCH = FileTime.fromMillis(0L);

	@Override
	public FileTime lastModifiedTime() {
		return EPOCH;
	}

	@Override
	public FileTime lastAccessTime() {
		return EPOCH;
	}

	@Override
	public FileTime creationTime() {
		return EPOCH;
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {
		return 0L;
	}

	@Override
	public @Nullable Object fileKey() {
		return null;
	}
}
