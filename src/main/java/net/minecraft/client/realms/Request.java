package net.minecraft.client.realms;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.exception.RealmsHttpException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Базовый HTTP-запрос к серверу Realms через {@link HttpURLConnection}.
 * Реализует паттерн «Fluent Builder»: подклассы {@link Get}, {@link Post}, {@link Put}, {@link Delete}
 * настраивают соединение в {@link #doConnect()}, а общая логика чтения ответа — в {@link #text()}.
 *
 * @param <T> конкретный тип запроса для цепочки вызовов
 */
@Environment(EnvType.CLIENT)
public abstract class Request<T extends Request<T>> {

	private static final int READ_TIMEOUT_MS = 60000;
	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int DEFAULT_RETRY_AFTER_SECONDS = 5;
	private static final int DRAIN_BUFFER_SIZE = 1024;
	private static final int HTTP_ERROR_THRESHOLD = 400;
	private static final String HEADER_COOKIE = "Cookie";
	private static final String HEADER_IS_PRERELEASE = "Is-Prerelease";
	private static final String HEADER_RETRY_AFTER = "Retry-After";

	protected HttpURLConnection connection;
	protected String url;
	private boolean connected;

	public Request(String url, int connectTimeout, int readTimeout) {
		try {
			this.url = url;
			Proxy proxy = RealmsClientConfig.getProxy();
			this.connection = proxy != null
					? (HttpURLConnection) new URL(url).openConnection(proxy)
					: (HttpURLConnection) new URL(url).openConnection();
			this.connection.setConnectTimeout(connectTimeout);
			this.connection.setReadTimeout(readTimeout);
		} catch (MalformedURLException ex) {
			throw new RealmsHttpException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new RealmsHttpException(ex.getMessage(), ex);
		}
	}

	public void cookie(String key, String value) {
		cookie(connection, key, value);
	}

	public static void cookie(HttpURLConnection connection, String key, String value) {
		String existing = connection.getRequestProperty(HEADER_COOKIE);
		connection.setRequestProperty(
				HEADER_COOKIE,
				existing == null ? key + "=" + value : existing + ";" + key + "=" + value
		);
	}

	public void prerelease(boolean prerelease) {
		connection.addRequestProperty(HEADER_IS_PRERELEASE, String.valueOf(prerelease));
	}

	public int getRetryAfterHeader() {
		return getRetryAfterHeader(connection);
	}

	public static int getRetryAfterHeader(HttpURLConnection connection) {
		String value = connection.getHeaderField(HEADER_RETRY_AFTER);

		try {
			return Integer.parseInt(value);
		} catch (Exception ignored) {
			return DEFAULT_RETRY_AFTER_SECONDS;
		}
	}

	public int responseCode() {
		try {
			this.connect();
			return connection.getResponseCode();
		} catch (Exception ex) {
			throw new RealmsHttpException(ex.getMessage(), ex);
		}
	}

	/**
	 * Выполняет запрос и возвращает тело ответа как строку.
	 * При HTTP-статусе ≥ 400 читает поток ошибок вместо основного потока.
	 *
	 * @return тело ответа
	 * @throws RealmsHttpException при ошибке ввода-вывода
	 */
	public String text() {
		try {
			this.connect();
			String body = responseCode() >= HTTP_ERROR_THRESHOLD
					? read(connection.getErrorStream())
					: read(connection.getInputStream());
			dispose();
			return body;
		} catch (IOException ex) {
			throw new RealmsHttpException(ex.getMessage(), ex);
		}
	}

	private String read(@Nullable InputStream in) throws IOException {
		if (in == null) {
			return "";
		}

		try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			StringBuilder builder = new StringBuilder();

			for (int ch = reader.read(); ch != -1; ch = reader.read()) {
				builder.append((char) ch);
			}

			return builder.toString();
		}
	}

	// Дренирует и закрывает соединение для корректного возврата в пул keep-alive
	private void dispose() {
		byte[] drainBuffer = new byte[DRAIN_BUFFER_SIZE];

		try {
			InputStream inputStream = connection.getInputStream();

			while (inputStream.read(drainBuffer) > 0) {
			}

			inputStream.close();
			return;
		} catch (Exception ignored) {
			try {
				InputStream errorStream = connection.getErrorStream();

				if (errorStream != null) {
					while (errorStream.read(drainBuffer) > 0) {
					}

					errorStream.close();
				}
			} catch (IOException ignored2) {
				// соединение уже закрыто
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	protected T connect() {
		if (connected) {
			return (T) this;
		}

		T request = doConnect();
		connected = true;
		return request;
	}

	protected abstract T doConnect();

	public static Request<?> get(String url) {
		return new Get(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
	}

	public static Request<?> get(String url, int connectTimeoutMillis, int readTimeoutMillis) {
		return new Get(url, connectTimeoutMillis, readTimeoutMillis);
	}

	public static Request<?> post(String uri, String content) {
		return new Post(uri, content, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
	}

	public static Request<?> post(String uri, String content, int connectTimeoutMillis, int readTimeoutMillis) {
		return new Post(uri, content, connectTimeoutMillis, readTimeoutMillis);
	}

	public static Request<?> delete(String url) {
		return new Delete(url, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
	}

	public static Request<?> put(String url, String content) {
		return new Put(url, content, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
	}

	public static Request<?> put(String url, String content, int connectTimeoutMillis, int readTimeoutMillis) {
		return new Put(url, content, connectTimeoutMillis, readTimeoutMillis);
	}

	public String getHeader(String header) {
		return getHeader(connection, header);
	}

	public static String getHeader(HttpURLConnection connection, String header) {
		try {
			return connection.getHeaderField(header);
		} catch (Exception ignored) {
			return "";
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Delete extends Request<Request.Delete> {

		public Delete(String url, int connectTimeout, int readTimeout) {
			super(url, connectTimeout, readTimeout);
		}

		@Override
		public Request.Delete doConnect() {
			try {
				connection.setDoOutput(true);
				connection.setRequestMethod("DELETE");
				connection.connect();
				return this;
			} catch (Exception ex) {
				throw new RealmsHttpException(ex.getMessage(), ex);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Get extends Request<Request.Get> {

		public Get(String url, int connectTimeout, int readTimeout) {
			super(url, connectTimeout, readTimeout);
		}

		@Override
		public Request.Get doConnect() {
			try {
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestMethod("GET");
				return this;
			} catch (Exception ex) {
				throw new RealmsHttpException(ex.getMessage(), ex);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Post extends Request<Request.Post> {

		private final String content;

		public Post(String uri, String content, int connectTimeout, int readTimeout) {
			super(uri, connectTimeout, readTimeout);
			this.content = content;
		}

		@Override
		public Request.Post doConnect() {
			try {
				if (content != null) {
					connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				}

				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestMethod("POST");

				OutputStream outputStream = connection.getOutputStream();

				try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
					writer.write(content);
				}

				outputStream.flush();
				return this;
			} catch (Exception ex) {
				throw new RealmsHttpException(ex.getMessage(), ex);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class Put extends Request<Request.Put> {

		private final String content;

		public Put(String uri, String content, int connectTimeout, int readTimeout) {
			super(uri, connectTimeout, readTimeout);
			this.content = content;
		}

		@Override
		public Request.Put doConnect() {
			try {
				if (content != null) {
					connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				}

				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod("PUT");

				OutputStream outputStream = connection.getOutputStream();

				try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
					writer.write(content);
				}

				outputStream.flush();
				return this;
			} catch (Exception ex) {
				throw new RealmsHttpException(ex.getMessage(), ex);
			}
		}
	}
}
