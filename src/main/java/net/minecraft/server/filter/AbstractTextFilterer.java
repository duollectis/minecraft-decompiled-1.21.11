package net.minecraft.server.filter;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.network.message.FilterMask;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.thread.SimpleConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Базовый класс для фильтрации текстовых сообщений через внешний HTTP-сервис.
 * Управляет пулом потоков, HTTP-соединениями и логикой кодирования/декодирования
 * запросов к API фильтрации чата.
 */
public abstract class AbstractTextFilterer implements AutoCloseable {

	private static final int READ_TIMEOUT_MS = 2000;
	private static final int CONNECT_TIMEOUT_MS = 15000;
	private static final int HTTP_NO_CONTENT = 204;
	private static final int HTTP_SUCCESS_MIN = 200;
	private static final int HTTP_SUCCESS_MAX = 300;

	protected static final Logger LOGGER = LogUtils.getLogger();
	private static final AtomicInteger WORKER_ID = new AtomicInteger(1);
	private static final ThreadFactory THREAD_FACTORY = runnable -> {
		Thread thread = new Thread(runnable);
		thread.setName("Chat-Filter-Worker-" + WORKER_ID.getAndIncrement());
		return thread;
	};

	private final URL url;
	private final AbstractTextFilterer.MessageEncoder messageEncoder;
	final AbstractTextFilterer.HashIgnorer hashIgnorer;
	final ExecutorService threadPool;

	protected static ExecutorService newThreadPool(int threadCount) {
		return Executors.newFixedThreadPool(threadCount, THREAD_FACTORY);
	}

	protected AbstractTextFilterer(
			URL url,
			AbstractTextFilterer.MessageEncoder messageEncoder,
			AbstractTextFilterer.HashIgnorer hashIgnorer,
			ExecutorService threadPool
	) {
		this.hashIgnorer = hashIgnorer;
		this.threadPool = threadPool;
		this.url = url;
		this.messageEncoder = messageEncoder;
	}

	protected static URL resolveEndpoint(URI uri, @Nullable JsonObject endpoints, String key, String defaultPath)
	throws MalformedURLException {
		String string = getEndpointPath(endpoints, key, defaultPath);
		return uri.resolve("/" + string).toURL();
	}

	protected static String getEndpointPath(@Nullable JsonObject endpoints, String key, String defaultPath) {
		return endpoints != null ? JsonHelper.getString(endpoints, key, defaultPath) : defaultPath;
	}

	/**
	 * Создаёт экземпляр фильтра текста на основе конфигурации сервера.
	 * Версия 0 использует Basic-аутентификацию, версия 1 — OAuth2 через MSAL.
	 */
	public static @Nullable AbstractTextFilterer createTextFilter(ServerPropertiesHandler properties) {
		String config = properties.textFilteringConfig;

		if (StringHelper.isBlank(config)) {
			return null;
		}

		return switch (properties.textFilteringVersion) {
			case 0 -> V0TextFilterer.load(config);
			case 1 -> V1TextFilterer.load(config);
			default -> {
				LOGGER.warn("Could not create text filter - unsupported text filtering version used");
				yield null;
			}
		};
	}

	protected CompletableFuture<FilteredMessage> filter(
			GameProfile profile,
			String raw,
			AbstractTextFilterer.HashIgnorer hashIgnorer,
			Executor executor
	) {
		if (raw.isEmpty()) {
			return CompletableFuture.completedFuture(FilteredMessage.EMPTY);
		}

		return CompletableFuture.supplyAsync(
				() -> {
					JsonObject payload = messageEncoder.encode(profile, raw);

					try {
						JsonObject response = request(payload, url);
						return filter(raw, hashIgnorer, response);
					}
					catch (Exception exception) {
						LOGGER.warn("Failed to validate message '{}'", raw, exception);
						return FilteredMessage.censored(raw);
					}
				},
				executor
		);
	}

	protected abstract FilteredMessage filter(
			String raw,
			AbstractTextFilterer.HashIgnorer hashIgnorer,
			JsonObject response
	);

	protected FilterMask createFilterMask(
			String raw,
			JsonArray redactedTextIndex,
			AbstractTextFilterer.HashIgnorer hashIgnorer
	) {
		if (redactedTextIndex.isEmpty()) {
			return FilterMask.PASS_THROUGH;
		}

		if (hashIgnorer.shouldIgnore(raw, redactedTextIndex.size())) {
			return FilterMask.FULLY_FILTERED;
		}

		FilterMask filterMask = new FilterMask(raw.length());

		for (int i = 0; i < redactedTextIndex.size(); i++) {
			filterMask.markFiltered(redactedTextIndex.get(i).getAsInt());
		}

		return filterMask;
	}

	@Override
	public void close() {
		threadPool.shutdownNow();
	}

	protected void discardRestOfInput(InputStream stream) throws IOException {
		byte[] buffer = new byte[1024];

		while (stream.read(buffer) != -1) {
		}
	}

	private JsonObject request(JsonObject requestBody, URL endpoint) throws IOException {
		HttpURLConnection connection = openConnection(requestBody, endpoint);

		try (InputStream inputStream = connection.getInputStream()) {
			if (connection.getResponseCode() == HTTP_NO_CONTENT) {
				return new JsonObject();
			}

			try {
				return LenientJsonParser
						.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
						.getAsJsonObject();
			}
			finally {
				discardRestOfInput(inputStream);
			}
		}
	}

	/**
	 * Открывает HTTP-соединение, записывает JSON-тело запроса и проверяет код ответа.
	 * Бросает {@link FailedHttpRequestException}, если сервер вернул код вне диапазона 2xx.
	 */
	protected HttpURLConnection openConnection(JsonObject requestBody, URL endpoint) throws IOException {
		HttpURLConnection connection = openConnection(endpoint);
		addAuthentication(connection);

		try (
				OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
				JsonWriter jsonWriter = new JsonWriter(writer)
		) {
			Streams.write(requestBody, jsonWriter);
		}

		int responseCode = connection.getResponseCode();

		if (responseCode >= HTTP_SUCCESS_MIN && responseCode < HTTP_SUCCESS_MAX) {
			return connection;
		}

		throw new AbstractTextFilterer.FailedHttpRequestException(
				responseCode + " " + connection.getResponseMessage()
		);
	}

	protected abstract void addAuthentication(HttpURLConnection connection);

	protected int getReadTimeout() {
		return READ_TIMEOUT_MS;
	}

	protected HttpURLConnection openConnection(URL endpoint) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(getReadTimeout());
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty(
				"User-Agent",
				"Minecraft server" + SharedConstants.getGameVersion().name()
		);
		return connection;
	}

	public TextStream createFilterer(GameProfile profile) {
		return new AbstractTextFilterer.StreamImpl(profile);
	}

	protected static class FailedHttpRequestException extends RuntimeException {

		protected FailedHttpRequestException(String message) {
			super(message);
		}
	}

	/**
	 * Стратегия игнорирования хэш-меток при фильтрации.
	 * Позволяет настраивать порог количества хэшей, при котором сообщение считается полностью отфильтрованным.
	 */
	@FunctionalInterface
	public interface HashIgnorer {

		AbstractTextFilterer.HashIgnorer NEVER_IGNORE = (hashes, hashesSize) -> false;

		AbstractTextFilterer.HashIgnorer IGNORE_IF_MATCHES_ALL = (hashes, hashesSize) -> hashes.length() == hashesSize;

		static AbstractTextFilterer.HashIgnorer internalDropHashes(int hashesToDrop) {
			return (hashes, hashesSize) -> hashesSize >= hashesToDrop;
		}

		static AbstractTextFilterer.HashIgnorer dropHashes(int hashesToDrop) {
			return switch (hashesToDrop) {
				case -1 -> NEVER_IGNORE;
				case 0 -> IGNORE_IF_MATCHES_ALL;
				default -> internalDropHashes(hashesToDrop);
			};
		}

		boolean shouldIgnore(String hashes, int hashesSize);
	}

	/**
	 * Кодирует профиль игрока и текст сообщения в JSON-тело запроса к API фильтрации.
	 */
	@FunctionalInterface
	protected interface MessageEncoder {

		JsonObject encode(GameProfile gameProfile, String message);
	}

	/**
	 * Реализация {@link TextStream} для конкретного игрока, направляющая запросы
	 * через последовательный исполнитель для сохранения порядка сообщений.
	 */
	protected class StreamImpl implements TextStream {

		protected final GameProfile gameProfile;
		protected final Executor executor;

		protected StreamImpl(final GameProfile gameProfile) {
			this.gameProfile = gameProfile;
			SimpleConsecutiveExecutor consecutiveExecutor = new SimpleConsecutiveExecutor(
					AbstractTextFilterer.this.threadPool, "chat stream for " + gameProfile.name()
			);
			this.executor = consecutiveExecutor::send;
		}

		@Override
		public CompletableFuture<List<FilteredMessage>> filterTexts(List<String> texts) {
			List<CompletableFuture<FilteredMessage>> futures = texts
					.stream()
					.map(text -> AbstractTextFilterer.this.filter(
							this.gameProfile,
							text,
							AbstractTextFilterer.this.hashIgnorer,
							this.executor
					))
					.collect(ImmutableList.toImmutableList());

			return Util.combine(futures).exceptionally(throwable -> ImmutableList.of());
		}

		@Override
		public CompletableFuture<FilteredMessage> filterText(String text) {
			return AbstractTextFilterer.this.filter(
					this.gameProfile,
					text,
					AbstractTextFilterer.this.hashIgnorer,
					this.executor
			);
		}
	}
}
