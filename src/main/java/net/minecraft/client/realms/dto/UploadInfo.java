package net.minecraft.client.realms.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Содержит параметры загрузки мира на сервер Realms: адрес конечной точки,
 * токен авторизации и флаг закрытия мира на время загрузки.
 * <p>
 * Парсится из JSON-ответа Realms API. Если в ответе отсутствует явный порт,
 * используется порт из URL или дефолтный {@code 8080}.
 */
@Environment(EnvType.CLIENT)
public record UploadInfo(boolean worldClosed, @Nullable String token, URI uploadEndpoint) {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String HTTP_PROTOCOL = "http://";
	private static final int DEFAULT_PORT = 8080;
	private static final int NO_PORT = -1;
	private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");

	public static @Nullable UploadInfo parse(String json) {
		try {
			JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
			String endpoint = JsonUtils.getNullableStringOr("uploadEndpoint", jsonObject, null);

			if (endpoint == null) {
				return null;
			}

			int port = JsonUtils.getIntOr("port", jsonObject, NO_PORT);
			URI uri = getUrl(endpoint, port);

			if (uri == null) {
				return null;
			}

			boolean worldClosed = JsonUtils.getBooleanOr("worldClosed", jsonObject, false);
			String token = JsonUtils.getNullableStringOr("token", jsonObject, null);

			return new UploadInfo(worldClosed, token, uri);
		} catch (Exception ex) {
			LOGGER.error("Could not parse UploadInfo", ex);
		}

		return null;
	}

	/**
	 * Строит {@link URI} из строки адреса и явного порта.
	 * <p>
	 * Если строка не содержит схему протокола, автоматически добавляется {@code http://}.
	 * Если явный порт задан (не {@code -1}), он заменяет порт из URL.
	 * Если ни явный порт, ни порт из URL не заданы, используется {@link #DEFAULT_PORT}.
	 *
	 * @param url  строка адреса, возможно без схемы протокола
	 * @param port явный порт из JSON или {@code -1}, если не задан
	 * @return готовый {@link URI} или {@code null} при ошибке синтаксиса
	 */
	@VisibleForTesting
	public static @Nullable URI getUrl(String url, int port) {
		Matcher matcher = PROTOCOL_PATTERN.matcher(url);
		String urlWithProtocol = getUrlWithProtocol(url, matcher);

		try {
			URI uri = new URI(urlWithProtocol);
			int resolvedPort = getPort(port, uri.getPort());

			return resolvedPort != uri.getPort()
				? new URI(
					uri.getScheme(),
					uri.getUserInfo(),
					uri.getHost(),
					resolvedPort,
					uri.getPath(),
					uri.getQuery(),
					uri.getFragment()
				)
				: uri;
		} catch (URISyntaxException ex) {
			LOGGER.warn("Failed to parse URI {}", urlWithProtocol, ex);
			return null;
		}
	}

	private static int getPort(int explicitPort, int urlPort) {
		if (explicitPort != NO_PORT) {
			return explicitPort;
		}

		return urlPort != NO_PORT ? urlPort : DEFAULT_PORT;
	}

	private static String getUrlWithProtocol(String url, Matcher matcher) {
		return matcher.find() ? url : HTTP_PROTOCOL + url;
	}

	/**
	 * Формирует тело HTTP-запроса для инициализации загрузки.
	 * Если токен задан, он включается в JSON-объект.
	 *
	 * @param token токен авторизации или {@code null}
	 * @return JSON-строка с токеном или пустой объект {@code {}}
	 */
	public static String createRequestContent(@Nullable String token) {
		JsonObject jsonObject = new JsonObject();

		if (token != null) {
			jsonObject.addProperty("token", token);
		}

		return jsonObject.toString();
	}
}
