package net.minecraft.client.realms;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.dto.UploadInfo;
import net.minecraft.client.realms.util.UploadProgress;
import net.minecraft.client.realms.util.UploadResult;
import net.minecraft.client.session.Session;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.apache.commons.io.input.CountingInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Загружает файл мира на сервер Realms через HTTP multipart-запрос.
 * Поддерживает автоматические повторные попытки при получении заголовка {@code Retry-After}.
 * Реализует {@link AutoCloseable} для корректного закрытия {@link HttpClient}.
 */
@Environment(EnvType.CLIENT)
public class FileUpload implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_ATTEMPTS = 5;
	private static final long CONNECT_TIMEOUT_SECONDS = 15L;
	private static final long REQUEST_TIMEOUT_MINUTES = 10L;
	private static final int HTTP_UNAUTHORIZED = 401;
	private static final String UPLOAD_PATH = "/upload/";
	private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

	private final File file;
	private final long worldId;
	private final int slotId;
	private final UploadInfo uploadInfo;
	private final String sessionId;
	private final String username;
	private final String clientVersion;
	private final String worldVersion;
	private final UploadProgress uploadStatus;
	private final HttpClient httpClient;

	public FileUpload(
			File file,
			long worldId,
			int slotId,
			UploadInfo uploadInfo,
			Session session,
			String clientVersion,
			String worldVersion,
			UploadProgress uploadStatus
	) {
		this.file = file;
		this.worldId = worldId;
		this.slotId = slotId;
		this.uploadInfo = uploadInfo;
		this.sessionId = session.getSessionId();
		this.username = session.getUsername();
		this.clientVersion = clientVersion;
		this.worldVersion = worldVersion;
		this.uploadStatus = uploadStatus;
		this.httpClient = HttpClient
				.newBuilder()
				.executor(Util.getDownloadWorkerExecutor())
				.connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
				.build();
	}

	@Override
	public void close() {
		httpClient.close();
	}

	/**
	 * Запускает асинхронную загрузку файла на сервер Realms.
	 * Перед отправкой устанавливает общий размер файла в {@link UploadProgress}.
	 *
	 * @return будущий результат загрузки с HTTP-кодом и возможным сообщением об ошибке
	 */
	public CompletableFuture<UploadResult> upload() {
		long fileSize = file.length();
		uploadStatus.setTotalBytes(fileSize);
		return requestUpload(0, fileSize);
	}

	private CompletableFuture<UploadResult> requestUpload(int currentAttempt, long size) {
		BodyPublisher bodyPublisher = getPublisher(
				() -> {
					try {
						return new FileUpload.ProgressInputStream(new FileInputStream(file), uploadStatus);
					} catch (IOException ex) {
						LOGGER.warn("Failed to open file {}", file, ex);
						return null;
					}
				},
				size
		);

		HttpRequest request = HttpRequest
				.newBuilder(uploadInfo.uploadEndpoint().resolve(UPLOAD_PATH + worldId + "/" + slotId))
				.timeout(Duration.ofMinutes(REQUEST_TIMEOUT_MINUTES))
				.setHeader("Cookie", getCookie())
				.setHeader("Content-Type", CONTENT_TYPE_OCTET_STREAM)
				.POST(bodyPublisher)
				.build();

		return httpClient
				.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenCompose(response -> {
					long retryDelay = getRetryDelaySeconds(response);

					if (shouldRetry(retryDelay, currentAttempt)) {
						uploadStatus.clear();

						try {
							Thread.sleep(Duration.ofSeconds(retryDelay));
						} catch (InterruptedException ignored) {
							Thread.currentThread().interrupt();
						}

						return requestUpload(currentAttempt + 1, size);
					}

					return CompletableFuture.completedFuture(handleResponse((HttpResponse<String>) response));
				});
	}

	private static BodyPublisher getPublisher(Supplier<@Nullable InputStream> inputSupplier, long size) {
		return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(inputSupplier), size);
	}

	private String getCookie() {
		return "sid=" + sessionId
				+ ";token=" + uploadInfo.token()
				+ ";user=" + username
				+ ";version=" + clientVersion
				+ ";worldVersion=" + worldVersion;
	}

	private UploadResult handleResponse(HttpResponse<String> response) {
		int statusCode = response.statusCode();

		if (statusCode == HTTP_UNAUTHORIZED) {
			LOGGER.debug("Realms server returned 401: {}", response.headers().firstValue("WWW-Authenticate"));
		}

		String errorMessage = null;
		String body = response.body();

		if (body != null && !body.isBlank()) {
			try {
				JsonElement errorMsg = LenientJsonParser.parse(body).getAsJsonObject().get("errorMsg");
				if (errorMsg != null) {
					errorMessage = errorMsg.getAsString();
				}
			} catch (Exception ex) {
				LOGGER.warn("Failed to parse response {}", body, ex);
			}
		}

		return new UploadResult(statusCode, errorMessage);
	}

	private boolean shouldRetry(long retryDelaySeconds, int currentAttempt) {
		return retryDelaySeconds > 0L && currentAttempt + 1 < MAX_ATTEMPTS;
	}

	private long getRetryDelaySeconds(HttpResponse<?> response) {
		return response.headers().firstValueAsLong("Retry-After").orElse(0L);
	}

	/**
	 * Обёртка над {@link CountingInputStream}, уведомляющая {@link UploadProgress}
	 * о количестве переданных байт после каждого чтения.
	 */
	@Environment(EnvType.CLIENT)
	static class ProgressInputStream extends CountingInputStream {

		private final UploadProgress progress;

		ProgressInputStream(InputStream stream, UploadProgress progress) {
			super(stream);
			this.progress = progress;
		}

		@Override
		protected void afterRead(int bytesRead) throws IOException {
			super.afterRead(bytesRead);
			progress.addBytesWritten(getByteCount());
		}
	}
}
