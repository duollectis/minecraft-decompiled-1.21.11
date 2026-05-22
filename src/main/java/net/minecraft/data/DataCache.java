package net.minecraft.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import net.minecraft.GameVersion;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Инкрементальный кэш данных генератора.
 * Хранит хэши сгенерированных файлов по каждому провайдеру и позволяет
 * пропускать повторную генерацию неизменившихся данных между запусками.
 */
public class DataCache {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final String CACHE_FILE_HEADER = "// ";
	private static final char TAB_CHAR = '\t';
	private static final char SPACE_CHAR = ' ';

	private final Path root;
	private final Path cachePath;
	private final String versionName;
	private final Map<String, CachedData> cachedDatas;
	private final Set<String> dataWriters = new HashSet<>();
	final Set<Path> paths = new HashSet<>();
	private final int totalSize;
	private int totalCacheMissCount;

	public DataCache(Path root, Collection<String> providerNames, GameVersion gameVersion) throws IOException {
		this.versionName = gameVersion.id();
		this.root = root;
		this.cachePath = root.resolve(".cache");
		Files.createDirectories(this.cachePath);

		Map<String, CachedData> map = new HashMap<>();
		int totalEntries = 0;

		for (String providerName : providerNames) {
			Path cachePath = getPath(providerName);
			this.paths.add(cachePath);
			CachedData cachedData = parseOrCreateCache(root, cachePath);
			map.put(providerName, cachedData);
			totalEntries += cachedData.size();
		}

		this.cachedDatas = map;
		this.totalSize = totalEntries;
	}

	private Path getPath(String providerName) {
		return cachePath.resolve(Hashing.sha1().hashString(providerName, StandardCharsets.UTF_8).toString());
	}

	private static CachedData parseOrCreateCache(Path root, Path dataProviderPath) {
		if (Files.isReadable(dataProviderPath)) {
			try {
				return CachedData.parseCache(root, dataProviderPath);
			} catch (Exception exception) {
				LOGGER.warn("Failed to parse cache {}, discarding", dataProviderPath, exception);
			}
		}

		return new CachedData("unknown", ImmutableMap.of());
	}

	public boolean isVersionDifferent(String providerName) {
		CachedData cachedData = cachedDatas.get(providerName);
		return cachedData == null || !cachedData.version.equals(versionName);
	}

	/**
	 * Запускает провайдер данных через {@link Runner} и возвращает результат выполнения.
	 * Создаёт {@link CachedDataWriter}, который отслеживает промахи кэша.
	 *
	 * @param providerName имя зарегистрированного провайдера
	 * @param runner       функция, выполняющая генерацию данных
	 * @return future с результатом, содержащим новый кэш и количество промахов
	 */
	public CompletableFuture<RunResult> run(String providerName, Runner runner) {
		CachedData cachedData = cachedDatas.get(providerName);
		if (cachedData == null) {
			throw new IllegalStateException("Provider not registered: " + providerName);
		}

		CachedDataWriter cachedDataWriter = new CachedDataWriter(providerName, versionName, cachedData);
		return runner.update(cachedDataWriter).thenApply(void_ -> cachedDataWriter.finish());
	}

	public void store(RunResult runResult) {
		cachedDatas.put(runResult.providerName(), runResult.cache());
		dataWriters.add(runResult.providerName());
		totalCacheMissCount = totalCacheMissCount + runResult.cacheMissCount();
	}

	/**
	 * Записывает обновлённые кэш-файлы на диск и удаляет устаревшие сгенерированные файлы,
	 * которые больше не присутствуют ни в одном кэше провайдеров.
	 */
	public void write() throws IOException {
		final Set<Path> knownPaths = new HashSet<>();

		cachedDatas.forEach((providerName, cachedData) -> {
			if (dataWriters.contains(providerName)) {
				Path providerCachePath = getPath(providerName);
				cachedData.write(
						root,
						providerCachePath,
						DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now()) + TAB_CHAR + providerName
				);
			}

			knownPaths.addAll(cachedData.data().keySet());
		});

		knownPaths.add(root.resolve("version.json"));

		final MutableInt visitedCount = new MutableInt();
		final MutableInt deletedCount = new MutableInt();

		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) {
				if (paths.contains(path)) {
					return FileVisitResult.CONTINUE;
				}

				visitedCount.increment();

				if (knownPaths.contains(path)) {
					return FileVisitResult.CONTINUE;
				}

				try {
					Files.delete(path);
				} catch (IOException exception) {
					LOGGER.warn("Failed to delete file {}", path, exception);
				}

				deletedCount.increment();
				return FileVisitResult.CONTINUE;
			}
		});

		LOGGER.info(
				"Caching: total files: {}, old count: {}, new count: {}, removed stale: {}, written: {}",
				new Object[]{visitedCount, totalSize, knownPaths.size(), deletedCount, totalCacheMissCount}
		);
	}

	/**
	 * Снимок кэша одного провайдера: версия игры и карта путей к хэшам файлов.
	 * Используется для сравнения с предыдущим запуском и пропуска неизменившихся файлов.
	 */
	record CachedData(String version, ImmutableMap<Path, HashCode> data) {

		public @Nullable HashCode get(Path path) {
			return data.get(path);
		}

		public int size() {
			return data.size();
		}

		/**
		 * Читает кэш-файл с диска. Формат: первая строка — заголовок {@code "// version\tdescription"},
		 * далее строки вида {@code "hashcode relative/path"}.
		 */
		public static CachedData parseCache(Path root, Path dataProviderPath) throws IOException {
			CachedData result;

			try (BufferedReader reader = Files.newBufferedReader(dataProviderPath, StandardCharsets.UTF_8)) {
				String headerLine = reader.readLine();
				if (!headerLine.startsWith(CACHE_FILE_HEADER)) {
					throw new IllegalStateException("Missing cache file header");
				}

				String[] headerParts = headerLine.substring(CACHE_FILE_HEADER.length()).split("\t", 2);
				String version = headerParts[0];
				Builder<Path, HashCode> builder = ImmutableMap.builder();

				reader.lines().forEach(line -> {
					int separatorIndex = line.indexOf(SPACE_CHAR);
					builder.put(
							root.resolve(line.substring(separatorIndex + 1)),
							HashCode.fromString(line.substring(0, separatorIndex))
					);
				});

				result = new CachedData(version, builder.build());
			}

			return result;
		}

		public void write(Path root, Path dataProviderPath, String description) {
			try (BufferedWriter writer = Files.newBufferedWriter(dataProviderPath, StandardCharsets.UTF_8)) {
				writer.write(CACHE_FILE_HEADER);
				writer.write(version);
				writer.write(TAB_CHAR);
				writer.write(description);
				writer.newLine();

				for (Entry<Path, HashCode> entry : data.entrySet()) {
					writer.write(entry.getValue().toString());
					writer.write(SPACE_CHAR);
					writer.write(root.relativize(entry.getKey()).toString());
					writer.newLine();
				}
			} catch (IOException exception) {
				LOGGER.warn("Unable write cachefile {}: {}", dataProviderPath, exception);
			}
		}
	}

	/**
	 * Реализация {@link DataWriter}, которая отслеживает промахи кэша
	 * и записывает файлы только при изменении их содержимого.
	 */
	static class CachedDataWriter implements DataWriter {

		private final String providerName;
		private final CachedData oldCache;
		private final IntermediaryCache newCache;
		private final AtomicInteger cacheMissCount = new AtomicInteger();
		private volatile boolean closed;

		CachedDataWriter(String providerName, String version, CachedData oldCache) {
			this.providerName = providerName;
			this.oldCache = oldCache;
			this.newCache = new IntermediaryCache(version);
		}

		private boolean isCacheInvalid(Path path, HashCode hashCode) {
			return !Objects.equals(oldCache.get(path), hashCode) || !Files.exists(path);
		}

		@Override
		public void write(Path path, byte[] data, HashCode hashCode) throws IOException {
			if (closed) {
				throw new IllegalStateException("Cannot write to cache as it has already been closed");
			}

			if (isCacheInvalid(path, hashCode)) {
				cacheMissCount.incrementAndGet();
				Files.createDirectories(path.getParent());
				Files.write(path, data);
			}

			newCache.put(path, hashCode);
		}

		public RunResult finish() {
			closed = true;
			return new RunResult(providerName, newCache.toCachedData(), cacheMissCount.get());
		}
	}

	/**
	 * Потокобезопасный промежуточный кэш, накапливающий хэши файлов
	 * в процессе параллельной генерации данных.
	 */
	record IntermediaryCache(String version, ConcurrentMap<Path, HashCode> data) {

		IntermediaryCache(String version) {
			this(version, new ConcurrentHashMap<>());
		}

		public void put(Path path, HashCode hashCode) {
			data.put(path, hashCode);
		}

		public CachedData toCachedData() {
			return new CachedData(version, ImmutableMap.copyOf(data));
		}
	}

	/**
	 * Итоговый результат выполнения одного провайдера данных:
	 * имя провайдера, новый снимок кэша и количество промахов кэша (записанных файлов).
	 */
	public record RunResult(String providerName, CachedData cache, int cacheMissCount) {
	}

	/**
	 * Функциональный интерфейс для запуска провайдера данных.
	 * Принимает {@link DataWriter} и возвращает future завершения генерации.
	 */
	@FunctionalInterface
	public interface Runner {

		CompletableFuture<?> update(DataWriter writer);
	}
}
