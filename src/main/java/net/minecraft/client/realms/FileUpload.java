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

@Environment(EnvType.CLIENT)
/**
 * {@code FileUpload}.
 */
public class FileUpload implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_ATTEMPTS = 5;
	private static final String UPLOAD_ENDPOINT = "/upload";
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
		this.httpClient =
				HttpClient
						.newBuilder()
						.executor(Util.getDownloadWorkerExecutor())
						.connectTimeout(Duration.ofSeconds(15L))
						.build();
	}

	@Override
	public void close() {
		this.httpClient.close();
	}

	public CompletableFuture<UploadResult> upload() {
		long l = this.file.length();
		this.uploadStatus.setTotalBytes(l);
		return this.requestUpload(0, l);
	}

	private CompletableFuture<UploadResult> requestUpload(int currentAttempt, long size) {
		BodyPublisher bodyPublisher = getPublisher(
				() -> {
					try {
						return new FileUpload.ProgressInputStream(new FileInputStream(this.file), this.uploadStatus);
					}
					catch (IOException var2) {
						LOGGER.warn("Failed to open file {}", this.file, var2);
						return null;
					}
				}, size
		);
		HttpRequest
				httpRequest =
				HttpRequest
						.newBuilder(this.uploadInfo
								.uploadEndpoint()
								.resolve("/upload/" + this.worldId + "/" + this.slotId))
						.timeout(Duration.ofMinutes(10L))
						.setHeader("Cookie", this.getCookie())
						.setHeader("Content-Type", "application/octet-stream")
						.POST(bodyPublisher)
						.build();
		return this.httpClient
				.sendAsync(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenCompose(response -> {
					long m = this.getRetryDelaySeconds((HttpResponse<?>) response);
					if (this.shouldRetry(m, currentAttempt)) {
						this.uploadStatus.clear();

						try {
							Thread.sleep(Duration.ofSeconds(m));
						}
						catch (InterruptedException var8) {
						}

						return this.requestUpload(currentAttempt + 1, size);
					}
					else {
						return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>) response));
					}
				});
	}

	private static BodyPublisher getPublisher(Supplier<@Nullable InputStream> inputSupplier, long size) {
		return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(inputSupplier), size);
	}

	private String getCookie() {
		return "sid="
				+ this.sessionId
				+ ";token="
				+ this.uploadInfo.token()
				+ ";user="
				+ this.username
				+ ";version="
				+ this.clientVersion
				+ ";worldVersion="
				+ this.worldVersion;
	}

	private UploadResult handleResponse(HttpResponse<String> response) {
		int i = response.statusCode();
		if (i == 401) {
			LOGGER.debug("Realms server returned 401: {}", response.headers().firstValue("WWW-Authenticate"));
		}

		String string = null;
		String string2 = response.body();
		if (string2 != null && !string2.isBlank()) {
			try {
				JsonElement jsonElement = LenientJsonParser.parse(string2).getAsJsonObject().get("errorMsg");
				if (jsonElement != null) {
					string = jsonElement.getAsString();
				}
			}
			catch (Exception var6) {
				LOGGER.warn("Failed to parse response {}", string2, var6);
			}
		}

		return new UploadResult(i, string);
	}

	private boolean shouldRetry(long retryDelaySeconds, int currentAttempt) {
		return retryDelaySeconds > 0L && currentAttempt + 1 < 5;
	}

	private long getRetryDelaySeconds(HttpResponse<?> response) {
		return response.headers().firstValueAsLong("Retry-After").orElse(0L);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ProgressInputStream}.
	 */
	static class ProgressInputStream extends CountingInputStream {

		private final UploadProgress progress;

		ProgressInputStream(InputStream stream, UploadProgress progress) {
			super(stream);
			this.progress = progress;
		}

		protected void afterRead(int n) throws IOException {
			super.afterRead(n);
			this.progress.addBytesWritten(this.getByteCount());
		}
	}
}
