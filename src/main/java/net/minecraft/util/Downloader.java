package net.minecraft.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.logging.LogWriter;
import net.minecraft.util.path.CacheFiles;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.thread.SimpleConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Менеджер загрузки файлов с поддержкой кэширования и логирования.
 * <p>
 * Загрузки выполняются последовательно через внутренний исполнитель.
 * Каждая загрузка логируется в файл {@code log.json} в директории кэша.
 * При создании автоматически очищает старые файлы кэша, оставляя не более
 * {@value #MAX_RETAINED_CACHE_FILES} последних.
 */
public class Downloader implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_RETAINED_CACHE_FILES = 20;

	private final Path directory;
	private final LogWriter<Downloader.LogEntry> logWriter;
	private final SimpleConsecutiveExecutor executor = new SimpleConsecutiveExecutor(
		Util.getDownloadWorkerExecutor(), "download-queue"
	);

	public Downloader(Path directory) throws IOException {
		this.directory = directory;
		PathUtil.createDirectories(directory);
		this.logWriter = LogWriter.create(LogEntry.CODEC, directory.resolve("log.json"));
		CacheFiles.clear(directory, MAX_RETAINED_CACHE_FILES);
	}

	/**
	 * Выполняет загрузку всех указанных файлов синхронно.
	 * Результаты и ошибки записываются в лог.
	 *
	 * @param config  конфигурация загрузки (хэш-функция, заголовки, прокси и т.д.)
	 * @param entries карта UUID → параметры загрузки
	 * @return результат с путями к загруженным файлам и множеством UUID с ошибками
	 */
	private Downloader.DownloadResult download(
		Downloader.Config config,
		Map<UUID, Downloader.DownloadEntry> entries
	) {
		Downloader.DownloadResult result = new Downloader.DownloadResult();

		entries.forEach((id, entry) -> {
			Path targetPath = directory.resolve(id.toString());
			Path downloadedPath = null;

			try {
				downloadedPath = NetworkUtils.download(
					targetPath,
					entry.url(),
					config.headers(),
					config.hashFunction(),
					entry.hash(),
					config.maxSize(),
					config.proxy(),
					config.listener()
				);
				result.downloaded().put(id, downloadedPath);
			} catch (Exception exception) {
				LOGGER.error("Failed to download {}", entry.url(), exception);
				result.failed().add(id);
			}

			try {
				logWriter.write(new LogEntry(
					id,
					entry.url().toString(),
					Instant.now(),
					Optional.ofNullable(entry.hash()).map(HashCode::toString),
					downloadedPath != null ? getFileInfo(downloadedPath) : Either.left("download_failed")
				));
			} catch (Exception exception) {
				LOGGER.error("Failed to log download of {}", entry.url(), exception);
			}
		});

		return result;
	}

	private Either<String, Downloader.FileInfo> getFileInfo(Path path) {
		try {
			long size = Files.size(path);
			Path relativePath = directory.relativize(path);
			return Either.right(new FileInfo(relativePath.toString(), size));
		} catch (IOException exception) {
			LOGGER.error("Failed to get file size of {}", path, exception);
			return Either.left("no_access");
		}
	}

	/**
	 * Асинхронно загружает все указанные файлы через внутренний последовательный исполнитель.
	 *
	 * @param config  конфигурация загрузки
	 * @param entries карта UUID → параметры загрузки
	 * @return {@link CompletableFuture} с результатом загрузки
	 */
	public CompletableFuture<Downloader.DownloadResult> downloadAsync(
		Downloader.Config config,
		Map<UUID, Downloader.DownloadEntry> entries
	) {
		return CompletableFuture.supplyAsync(() -> download(config, entries), executor::send);
	}

	@Override
	public void close() throws IOException {
		executor.close();
		logWriter.close();
	}

	/**
	 * Конфигурация загрузки файлов.
	 *
	 * @param hashFunction функция хэширования для проверки целостности
	 * @param maxSize      максимальный допустимый размер файла в байтах
	 * @param headers      HTTP-заголовки запроса
	 * @param proxy        прокси-сервер для соединения
	 * @param listener     слушатель прогресса загрузки
	 */
	public record Config(
		HashFunction hashFunction,
		int maxSize,
		Map<String, String> headers,
		Proxy proxy,
		NetworkUtils.DownloadListener listener
	) {
	}

	/**
	 * Параметры одной загрузки.
	 *
	 * @param url  URL для загрузки
	 * @param hash ожидаемый хэш файла, или {@code null} если проверка не нужна
	 */
	public record DownloadEntry(URL url, @Nullable HashCode hash) {
	}

	/**
	 * Результат пакетной загрузки.
	 *
	 * @param downloaded карта UUID → путь к успешно загруженному файлу
	 * @param failed     множество UUID файлов, загрузка которых завершилась ошибкой
	 */
	public record DownloadResult(Map<UUID, Path> downloaded, Set<UUID> failed) {

		public DownloadResult() {
			this(new HashMap<>(), new HashSet<>());
		}
	}

	record FileInfo(String name, long size) {

		public static final Codec<Downloader.FileInfo> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.STRING.fieldOf("name").forGetter(FileInfo::name),
				Codec.LONG.fieldOf("size").forGetter(FileInfo::size)
			).apply(instance, FileInfo::new)
		);
	}

	record LogEntry(
		UUID id,
		String url,
		Instant time,
		Optional<String> hash,
		Either<String, Downloader.FileInfo> errorOrFileInfo
	) {

		public static final Codec<Downloader.LogEntry> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Uuids.STRING_CODEC.fieldOf("id").forGetter(LogEntry::id),
				Codec.STRING.fieldOf("url").forGetter(LogEntry::url),
				Codecs.INSTANT.fieldOf("time").forGetter(LogEntry::time),
				Codec.STRING.optionalFieldOf("hash").forGetter(LogEntry::hash),
				Codec.mapEither(Codec.STRING.fieldOf("error"), FileInfo.CODEC.fieldOf("file"))
					.forGetter(LogEntry::errorOrFileInfo)
			).apply(instance, LogEntry::new)
		);
	}
}
