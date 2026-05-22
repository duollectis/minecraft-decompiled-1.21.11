package net.minecraft.util.path;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Утилита для управления кэш-файлами в директории: удаляет старые файлы,
 * оставляя не более {@code maxRetained} самых свежих. Файлы в поддиректориях
 * имеют приоритет удаления выше, чем файлы в корне кэша.
 */
public class CacheFiles {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Удаляет устаревшие кэш-файлы из директории, оставляя не более {@code maxRetained} штук.
	 * Сначала удаляются файлы из поддиректорий (по убыванию приоритета), затем пустые
	 * поддиректории очищаются автоматически.
	 */
	public static void clear(Path directory, int maxRetained) {
		try {
			List<CacheFiles.CacheFile> cacheFiles = findCacheFiles(directory);
			int toDelete = cacheFiles.size() - maxRetained;

			if (toDelete <= 0) {
				return;
			}

			cacheFiles.sort(CacheFiles.CacheFile.COMPARATOR);
			List<CacheFiles.CacheEntry> entries = toCacheEntries(cacheFiles);
			Collections.reverse(entries);
			entries.sort(CacheFiles.CacheEntry.COMPARATOR);
			Set<Path> emptyDirCandidates = new HashSet<>();

			for (int i = 0; i < toDelete; i++) {
				CacheFiles.CacheEntry entry = entries.get(i);
				Path filePath = entry.path;

				try {
					Files.delete(filePath);

					if (entry.removalPriority == 0) {
						emptyDirCandidates.add(filePath.getParent());
					}
				} catch (IOException ex) {
					LOGGER.warn("Failed to delete cache file {}", filePath, ex);
				}
			}

			emptyDirCandidates.remove(directory);

			for (Path dir : emptyDirCandidates) {
				try {
					Files.delete(dir);
				} catch (DirectoryNotEmptyException ignored) {
				} catch (IOException ex) {
					LOGGER.warn("Failed to delete empty(?) cache directory {}", dir, ex);
				}
			}
		} catch (UncheckedIOException | IOException ex) {
			LOGGER.error("Failed to vacuum cache dir {}", directory, ex);
		}
	}

	private static List<CacheFiles.CacheFile> findCacheFiles(Path directory) throws IOException {
		try {
			List<CacheFiles.CacheFile> result = new ArrayList<>();

			Files.walkFileTree(directory, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
					if (attrs.isRegularFile() && !path.getParent().equals(directory)) {
						result.add(new CacheFiles.CacheFile(path, attrs.lastModifiedTime()));
					}

					return FileVisitResult.CONTINUE;
				}
			});

			return result;
		} catch (NoSuchFileException ex) {
			return List.of();
		}
	}

	private static List<CacheFiles.CacheEntry> toCacheEntries(List<CacheFiles.CacheFile> files) {
		List<CacheFiles.CacheEntry> entries = new ArrayList<>();
		Object2IntOpenHashMap<Path> dirFileCounts = new Object2IntOpenHashMap<>();

		for (CacheFiles.CacheFile file : files) {
			int countInDir = dirFileCounts.addTo(file.path.getParent(), 1);
			entries.add(new CacheFiles.CacheEntry(file.path, countInDir));
		}

		return entries;
	}

	/**
	 * Запись кэш-файла с приоритетом удаления: чем выше значение {@code removalPriority},
	 * тем раньше файл будет удалён (файлы из поддиректорий с большим числом файлов удаляются первыми).
	 */
	record CacheEntry(Path path, int removalPriority) {

		public static final Comparator<CacheFiles.CacheEntry> COMPARATOR =
				Comparator.comparing(CacheFiles.CacheEntry::removalPriority).reversed();
	}

	record CacheFile(Path path, FileTime modifiedTime) {

		public static final Comparator<CacheFiles.CacheFile> COMPARATOR =
				Comparator.comparing(CacheFiles.CacheFile::modifiedTime).reversed();
	}
}
