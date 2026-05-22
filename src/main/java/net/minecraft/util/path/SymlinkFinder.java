package net.minecraft.util.path;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Обходит файловое дерево и собирает символьные ссылки, цели которых
 * не разрешены заданным {@link PathMatcher}. Используется для защиты
 * от path-traversal атак через симлинки в ресурс-паках.
 */
public class SymlinkFinder {

	private final PathMatcher matcher;

	public SymlinkFinder(PathMatcher matcher) {
		this.matcher = matcher;
	}

	public void validate(Path path, List<SymlinkEntry> results) throws IOException {
		Path target = Files.readSymbolicLink(path);

		if (!matcher.matches(target)) {
			results.add(new SymlinkEntry(path, target));
		}
	}

	public List<SymlinkEntry> validate(Path path) throws IOException {
		List<SymlinkEntry> results = new ArrayList<>();
		validate(path, results);
		return results;
	}

	/**
	 * Собирает все запрещённые симлинки в дереве по заданному пути.
	 * Если {@code resolveSymlink=true} и сам корневой путь является симлинком,
	 * он разыменовывается перед рекурсивным обходом.
	 */
	public List<SymlinkEntry> collect(Path path, boolean resolveSymlink) throws IOException {
		List<SymlinkEntry> results = new ArrayList<>();

		BasicFileAttributes attrs;

		try {
			attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		} catch (NoSuchFileException ex) {
			return results;
		}

		if (attrs.isRegularFile()) {
			throw new IOException("Path " + path + " is not a directory");
		}

		if (attrs.isSymbolicLink()) {
			if (!resolveSymlink) {
				validate(path, results);
				return results;
			}

			path = Files.readSymbolicLink(path);
		}

		validateRecursively(path, results);
		return results;
	}

	public void validateRecursively(Path path, List<SymlinkEntry> results) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			private void checkSymlink(Path entry, BasicFileAttributes attrs) throws IOException {
				if (attrs.isSymbolicLink()) {
					validate(entry, results);
				}
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				checkSymlink(dir, attrs);
				return super.preVisitDirectory(dir, attrs);
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				checkSymlink(file, attrs);
				return super.visitFile(file, attrs);
			}
		});
	}
}
