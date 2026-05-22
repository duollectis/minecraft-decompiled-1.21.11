package net.minecraft.util;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.mojang.logging.LogUtils;
import net.minecraft.util.path.PathUtil;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Утилиты для сетевых операций: загрузка файлов с проверкой хэша,
 * поиск свободных портов и проверка доступности портов.
 */
public class NetworkUtils {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Размер буфера чтения при загрузке файлов (8 КБ). */
	private static final int READ_BUFFER_SIZE = 8196;

	/** Порт-заглушка, возвращаемый при невозможности найти свободный порт. */
	private static final int FALLBACK_PORT = 25564;

	/** Минимальный допустимый номер порта. */
	private static final int PORT_MIN = 0;

	/** Максимальный допустимый номер порта. */
	private static final int PORT_MAX = 65535;

	private NetworkUtils() {
	}

	/**
	 * Загружает файл по URL с проверкой хэша и кэшированием.
	 * Если файл с совпадающим хэшем уже существует — возвращает его без повторной загрузки.
	 * Если {@code hashCode} равен null — загружает файл и вычисляет хэш на лету.
	 *
	 * @param path директория для сохранения файла
	 * @param url URL для загрузки
	 * @param headers HTTP-заголовки запроса
	 * @param hashFunction функция хэширования для проверки целостности
	 * @param hashCode ожидаемый хэш файла, или null если хэш неизвестен
	 * @param maxBytes максимально допустимый размер файла в байтах
	 * @param proxy прокси для HTTP-соединения
	 * @param listener слушатель прогресса загрузки
	 * @return путь к загруженному файлу
	 * @throws IllegalStateException при ошибке загрузки
	 */
	public static Path download(
		Path path,
		URL url,
		Map<String, String> headers,
		HashFunction hashFunction,
		@Nullable HashCode hashCode,
		int maxBytes,
		Proxy proxy,
		DownloadListener listener
	) {
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		listener.onStart();

		Path cachedPath;

		if (hashCode != null) {
			cachedPath = resolve(path, hashCode);

			try {
				if (validateHash(cachedPath, hashFunction, hashCode)) {
					LOGGER.info("Returning cached file since actual hash matches requested");
					listener.onFinish(true);
					updateModificationTime(cachedPath);
					return cachedPath;
				}
			} catch (IOException exception) {
				LOGGER.warn("Failed to check cached file {}", cachedPath, exception);
			}

			try {
				LOGGER.warn("Existing file {} not found or had mismatched hash", cachedPath);
				Files.deleteIfExists(cachedPath);
			} catch (IOException exception) {
				listener.onFinish(false);
				throw new UncheckedIOException("Failed to remove existing file " + cachedPath, exception);
			}
		} else {
			cachedPath = null;
		}

		Path resultPath;

		try {
			connection = (HttpURLConnection) url.openConnection(proxy);
			connection.setInstanceFollowRedirects(true);
			headers.forEach(connection::setRequestProperty);
			inputStream = connection.getInputStream();

			long contentLength = connection.getContentLengthLong();
			OptionalLong optionalLength = contentLength != -1L
				? OptionalLong.of(contentLength)
				: OptionalLong.empty();

			PathUtil.createDirectories(path);
			listener.onContentLength(optionalLength);

			if (optionalLength.isPresent() && optionalLength.getAsLong() > maxBytes) {
				throw new IOException(
					"Filesize is bigger than maximum allowed (file is " + optionalLength + ", limit is " + maxBytes + ")"
				);
			}

			if (cachedPath == null) {
				Path tempPath = Files.createTempFile(path, "download", ".tmp");

				try {
					HashCode downloadedHash = write(hashFunction, maxBytes, listener, inputStream, tempPath);
					Path finalPath = resolve(path, downloadedHash);

					if (!validateHash(finalPath, hashFunction, downloadedHash)) {
						Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
					} else {
						updateModificationTime(finalPath);
					}

					listener.onFinish(true);
					return finalPath;
				} finally {
					Files.deleteIfExists(tempPath);
				}
			}

			HashCode downloadedHash = write(hashFunction, maxBytes, listener, inputStream, cachedPath);

			if (!downloadedHash.equals(hashCode)) {
				throw new IOException(
					"Hash of downloaded file (" + downloadedHash + ") did not match requested (" + hashCode + ")"
				);
			}

			listener.onFinish(true);
			resultPath = cachedPath;
		} catch (Throwable throwable) {
			if (connection != null) {
				InputStream errorStream = connection.getErrorStream();

				if (errorStream != null) {
					try {
						LOGGER.error("HTTP response error: {}", IOUtils.toString(errorStream, StandardCharsets.UTF_8));
					} catch (Exception exception) {
						LOGGER.error("Failed to read response from server");
					}
				}
			}

			listener.onFinish(false);
			throw new IllegalStateException("Failed to download file " + url, throwable);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		return resultPath;
	}

	private static void updateModificationTime(Path path) {
		try {
			Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
		} catch (IOException exception) {
			LOGGER.warn("Failed to update modification time of {}", path, exception);
		}
	}

	private static HashCode hash(Path path, HashFunction hashFunction) throws IOException {
		Hasher hasher = hashFunction.newHasher();

		try (
			OutputStream outputStream = Funnels.asOutputStream(hasher);
			InputStream inputStream = Files.newInputStream(path)
		) {
			inputStream.transferTo(outputStream);
		}

		return hasher.hash();
	}

	private static boolean validateHash(Path path, HashFunction hashFunction, HashCode expected) throws IOException {
		if (!Files.exists(path)) {
			return false;
		}

		HashCode actual = hash(path, hashFunction);

		if (actual.equals(expected)) {
			return true;
		}

		LOGGER.warn("Mismatched hash of file {}, expected {} but found {}", path, expected, actual);
		return false;
	}

	private static Path resolve(Path directory, HashCode hashCode) {
		return directory.resolve(hashCode.toString());
	}

	private static HashCode write(
		HashFunction hashFunction,
		int maxBytes,
		DownloadListener listener,
		InputStream stream,
		Path path
	) throws IOException {
		try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
			Hasher hasher = hashFunction.newHasher();
			byte[] buffer = new byte[READ_BUFFER_SIZE];
			long totalWritten = 0L;

			int bytesRead;
			while ((bytesRead = stream.read(buffer)) >= 0) {
				totalWritten += bytesRead;
				listener.onProgress(totalWritten);

				if (totalWritten > maxBytes) {
					throw new IOException(
						"Filesize was bigger than maximum allowed (got >= " + totalWritten + ", limit was " + maxBytes + ")"
					);
				}

				if (Thread.interrupted()) {
					LOGGER.error("INTERRUPTED");
					throw new IOException("Download interrupted");
				}

				outputStream.write(buffer, 0, bytesRead);
				hasher.putBytes(buffer, 0, bytesRead);
			}

			return hasher.hash();
		}
	}

	/**
	 * Находит свободный локальный порт, открывая временный серверный сокет.
	 *
	 * @return номер свободного порта, или {@value FALLBACK_PORT} при ошибке
	 */
	public static int findLocalPort() {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			return serverSocket.getLocalPort();
		} catch (IOException exception) {
			return FALLBACK_PORT;
		}
	}

	/**
	 * Проверяет, доступен ли указанный порт для прослушивания.
	 *
	 * @param port номер порта для проверки
	 * @return {@code true} если порт свободен и находится в допустимом диапазоне
	 */
	public static boolean isPortAvailable(int port) {
		if (port < PORT_MIN || port > PORT_MAX) {
			return false;
		}

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			return serverSocket.getLocalPort() == port;
		} catch (IOException exception) {
			return false;
		}
	}

	/**
	 * Слушатель событий загрузки файла по сети.
	 */
	public interface DownloadListener {

		void onStart();

		void onContentLength(OptionalLong contentLength);

		void onProgress(long writtenBytes);

		void onFinish(boolean success);
	}
}
