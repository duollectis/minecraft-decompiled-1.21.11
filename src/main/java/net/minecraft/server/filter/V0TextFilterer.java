package net.minecraft.server.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.message.FilterMask;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Реализация фильтра текста версии 0, использующая Basic-аутентификацию (Base64 API-ключ).
 * Поддерживает два формата API: legacy (v1/chat) и новый (с произвольным путём).
 */
public class V0TextFilterer extends AbstractTextFilterer {

	private static final String LEGACY_CHAT_ENDPOINT = "v1/chat";

	final URL joinEndpoint;
	final V0TextFilterer.ProfileEncoder joinEncoder;
	final URL leaveEndpoint;
	final V0TextFilterer.ProfileEncoder leaveEncoder;
	private final String encodedApiKey;

	private V0TextFilterer(
			URL chatEndpoint,
			AbstractTextFilterer.MessageEncoder messageEncoder,
			URL joinEndpoint,
			V0TextFilterer.ProfileEncoder joinEncoder,
			URL leaveEndpoint,
			V0TextFilterer.ProfileEncoder leaveEncoder,
			String encodedApiKey,
			AbstractTextFilterer.HashIgnorer ignorer,
			ExecutorService threadPool
	) {
		super(chatEndpoint, messageEncoder, ignorer, threadPool);
		this.joinEndpoint = joinEndpoint;
		this.joinEncoder = joinEncoder;
		this.leaveEndpoint = leaveEndpoint;
		this.leaveEncoder = leaveEncoder;
		this.encodedApiKey = encodedApiKey;
	}

	/**
	 * Загружает и создаёт экземпляр фильтра из JSON-конфигурации.
	 * Поддерживает legacy-формат (endpoint "v1/chat") и новый формат с произвольными путями.
	 */
	public static @Nullable AbstractTextFilterer load(String config) {
		try {
			JsonObject configJson = JsonHelper.deserialize(config);
			URI serverUri = new URI(JsonHelper.getString(configJson, "apiServer"));
			String apiKey = JsonHelper.getString(configJson, "apiKey");

			if (apiKey.isEmpty()) {
				throw new IllegalArgumentException("Missing API key");
			}

			int ruleId = JsonHelper.getInt(configJson, "ruleId", 1);
			String serverId = JsonHelper.getString(configJson, "serverId", "");
			String roomId = JsonHelper.getString(configJson, "roomId", "Java:Chat");
			int hashesToDrop = JsonHelper.getInt(configJson, "hashesToDrop", -1);
			int maxConcurrentRequests = JsonHelper.getInt(configJson, "maxConcurrentRequests", 7);
			JsonObject endpoints = JsonHelper.getObject(configJson, "endpoints", null);
			String chatEndpointPath = getEndpointPath(endpoints, "chat", LEGACY_CHAT_ENDPOINT);
			boolean isLegacyChatEndpoint = chatEndpointPath.equals(LEGACY_CHAT_ENDPOINT);
			URL chatUrl = serverUri.resolve("/" + chatEndpointPath).toURL();
			URL joinUrl = resolveEndpoint(serverUri, endpoints, "join", "v1/join");
			URL leaveUrl = resolveEndpoint(serverUri, endpoints, "leave", "v1/leave");

			V0TextFilterer.ProfileEncoder profileEncoder = profile -> {
				JsonObject body = new JsonObject();
				body.addProperty("server", serverId);
				body.addProperty("room", roomId);
				body.addProperty("user_id", profile.id().toString());
				body.addProperty("user_display_name", profile.name());
				return body;
			};

			AbstractTextFilterer.MessageEncoder messageEncoder;

			if (isLegacyChatEndpoint) {
				messageEncoder = (profile, message) -> {
					JsonObject body = new JsonObject();
					body.addProperty("rule", ruleId);
					body.addProperty("server", serverId);
					body.addProperty("room", roomId);
					body.addProperty("player", profile.id().toString());
					body.addProperty("player_display_name", profile.name());
					body.addProperty("text", message);
					body.addProperty("language", "*");
					return body;
				};
			}
			else {
				String ruleIdStr = String.valueOf(ruleId);
				messageEncoder = (profile, message) -> {
					JsonObject body = new JsonObject();
					body.addProperty("rule_id", ruleIdStr);
					body.addProperty("category", serverId);
					body.addProperty("subcategory", roomId);
					body.addProperty("user_id", profile.id().toString());
					body.addProperty("user_display_name", profile.name());
					body.addProperty("text", message);
					body.addProperty("language", "*");
					return body;
				};
			}

			AbstractTextFilterer.HashIgnorer hashIgnorer = AbstractTextFilterer.HashIgnorer.dropHashes(hashesToDrop);
			ExecutorService threadPool = newThreadPool(maxConcurrentRequests);
			String encodedApiKey = Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.US_ASCII));

			return new V0TextFilterer(
					chatUrl,
					messageEncoder,
					joinUrl,
					profileEncoder,
					leaveUrl,
					profileEncoder,
					encodedApiKey,
					hashIgnorer,
					threadPool
			);
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to parse chat filter config {}", config, exception);
			return null;
		}
	}

	@Override
	public TextStream createFilterer(GameProfile profile) {
		return new AbstractTextFilterer.StreamImpl(profile) {
			@Override
			public void onConnect() {
				V0TextFilterer.this.sendJoinOrLeaveRequest(
						this.gameProfile,
						V0TextFilterer.this.joinEndpoint,
						V0TextFilterer.this.joinEncoder,
						this.executor
				);
			}

			@Override
			public void onDisconnect() {
				V0TextFilterer.this.sendJoinOrLeaveRequest(
						this.gameProfile,
						V0TextFilterer.this.leaveEndpoint,
						V0TextFilterer.this.leaveEncoder,
						this.executor
				);
			}
		};
	}

	void sendJoinOrLeaveRequest(
			GameProfile gameProfile,
			URL endpoint,
			V0TextFilterer.ProfileEncoder profileEncoder,
			Executor executor
	) {
		executor.execute(() -> {
			JsonObject payload = profileEncoder.encode(gameProfile);

			try {
				sendRequest(payload, endpoint);
			}
			catch (Exception exception) {
				LOGGER.warn(
						"Failed to send join/leave packet to {} for player {}",
						new Object[]{endpoint, gameProfile, exception}
				);
			}
		});
	}

	private void sendRequest(JsonObject payload, URL endpoint) throws IOException {
		HttpURLConnection connection = openConnection(payload, endpoint);

		try (InputStream inputStream = connection.getInputStream()) {
			discardRestOfInput(inputStream);
		}
	}

	@Override
	protected void addAuthentication(HttpURLConnection connection) {
		connection.setRequestProperty("Authorization", "Basic " + encodedApiKey);
	}

	@Override
	protected FilteredMessage filter(String raw, AbstractTextFilterer.HashIgnorer hashIgnorer, JsonObject response) {
		boolean isPermitted = JsonHelper.getBoolean(response, "response", false);

		if (isPermitted) {
			return FilteredMessage.permitted(raw);
		}

		String hashedText = JsonHelper.getString(response, "hashed", null);

		if (hashedText == null) {
			return FilteredMessage.censored(raw);
		}

		JsonArray hashes = JsonHelper.getArray(response, "hashes");
		FilterMask filterMask = createFilterMask(raw, hashes, hashIgnorer);
		return new FilteredMessage(raw, filterMask);
	}

	/**
	 * Кодирует профиль игрока в JSON-тело запроса join/leave к API фильтрации.
	 */
	@FunctionalInterface
	interface ProfileEncoder {

		JsonObject encode(GameProfile gameProfile);
	}
}
