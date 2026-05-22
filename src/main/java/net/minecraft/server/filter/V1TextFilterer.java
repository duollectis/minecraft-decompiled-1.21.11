package net.minecraft.server.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.*;
import com.microsoft.aad.msal4j.ConfidentialClientApplication.Builder;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Реализация фильтра текста версии 1, использующая OAuth2-аутентификацию через Microsoft MSAL
 * с сертификатом клиента (ConfidentialClientApplication).
 */
public class V1TextFilterer extends AbstractTextFilterer {

	private final ConfidentialClientApplication application;
	private final ClientCredentialParameters credentialParameters;
	private final Set<String> fullyFilteredEvents;
	private final int readTimeoutMs;

	private V1TextFilterer(
			URL url,
			AbstractTextFilterer.MessageEncoder messageEncoder,
			AbstractTextFilterer.HashIgnorer hashIgnorer,
			ExecutorService threadPool,
			ConfidentialClientApplication application,
			ClientCredentialParameters credentialParameters,
			Set<String> fullyFilteredEvents,
			int readTimeoutMs
	) {
		super(url, messageEncoder, hashIgnorer, threadPool);
		this.application = application;
		this.credentialParameters = credentialParameters;
		this.fullyFilteredEvents = fullyFilteredEvents;
		this.readTimeoutMs = readTimeoutMs;
	}

	/**
	 * Загружает и создаёт экземпляр фильтра из JSON-конфигурации.
	 * Использует сертификат для OAuth2-аутентификации через Azure AD.
	 */
	public static @Nullable AbstractTextFilterer load(String config) {
		JsonObject configJson = JsonHelper.deserialize(config);
		URI serverUri = URI.create(JsonHelper.getString(configJson, "apiServer"));
		String apiPath = JsonHelper.getString(configJson, "apiPath");
		String scope = JsonHelper.getString(configJson, "scope");
		String serverId = JsonHelper.getString(configJson, "serverId", "");
		String applicationId = JsonHelper.getString(configJson, "applicationId");
		String tenantId = JsonHelper.getString(configJson, "tenantId");
		String roomId = JsonHelper.getString(configJson, "roomId", "Java:Chat");
		String certificatePath = JsonHelper.getString(configJson, "certificatePath");
		String certificatePassword = JsonHelper.getString(configJson, "certificatePassword", "");
		int hashesToDrop = JsonHelper.getInt(configJson, "hashesToDrop", -1);
		int maxConcurrentRequests = JsonHelper.getInt(configJson, "maxConcurrentRequests", 7);
		JsonArray filteredEventsArray = JsonHelper.getArray(configJson, "fullyFilteredEvents");
		Set<String> fullyFilteredEvents = new HashSet<>();
		filteredEventsArray.forEach(json -> fullyFilteredEvents.add(JsonHelper.asString(json, "filteredEvent")));
		int readTimeoutMs = JsonHelper.getInt(configJson, "connectionReadTimeoutMs", 2000);

		URL chatUrl;
		try {
			chatUrl = serverUri.resolve(apiPath).toURL();
		}
		catch (MalformedURLException exception) {
			throw new RuntimeException(exception);
		}

		AbstractTextFilterer.MessageEncoder messageEncoder = (profile, message) -> {
			JsonObject body = new JsonObject();
			body.addProperty("userId", profile.id().toString());
			body.addProperty("userDisplayName", profile.name());
			body.addProperty("server", serverId);
			body.addProperty("room", roomId);
			body.addProperty("area", "JavaChatRealms");
			body.addProperty("data", message);
			body.addProperty("language", "*");
			return body;
		};

		AbstractTextFilterer.HashIgnorer hashIgnorer = AbstractTextFilterer.HashIgnorer.dropHashes(hashesToDrop);
		ExecutorService threadPool = newThreadPool(maxConcurrentRequests);

		IClientCertificate certificate;
		try (InputStream inputStream = Files.newInputStream(Path.of(certificatePath))) {
			certificate = ClientCredentialFactory.createFromCertificate(inputStream, certificatePassword);
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to open certificate file");
			return null;
		}

		ConfidentialClientApplication clientApplication;
		try {
			String authorityUrl = String.format(Locale.ROOT, "https://login.microsoftonline.com/%s/", tenantId);
			clientApplication = ConfidentialClientApplication
					.builder(applicationId, certificate)
					.sendX5c(true)
					.executorService(threadPool)
					.authority(authorityUrl)
					.build();
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to create confidential client application");
			return null;
		}

		ClientCredentialParameters credentialParameters = ClientCredentialParameters
				.builder(Set.of(scope))
				.build();

		return new V1TextFilterer(
				chatUrl,
				messageEncoder,
				hashIgnorer,
				threadPool,
				clientApplication,
				credentialParameters,
				fullyFilteredEvents,
				readTimeoutMs
		);
	}

	private IAuthenticationResult getAuthToken() {
		return (IAuthenticationResult) application.acquireToken(credentialParameters).join();
	}

	@Override
	protected void addAuthentication(HttpURLConnection connection) {
		IAuthenticationResult authResult = getAuthToken();
		connection.setRequestProperty("Authorization", "Bearer " + authResult.accessToken());
	}

	@Override
	protected FilteredMessage filter(String raw, AbstractTextFilterer.HashIgnorer hashIgnorer, JsonObject response) {
		JsonObject result = JsonHelper.getObject(response, "result", null);

		if (result == null) {
			return FilteredMessage.censored(raw);
		}

		boolean isFiltered = JsonHelper.getBoolean(result, "filtered", true);

		if (!isFiltered) {
			return FilteredMessage.permitted(raw);
		}

		for (JsonElement element : JsonHelper.getArray(result, "events", new JsonArray())) {
			JsonObject event = element.getAsJsonObject();
			String eventId = JsonHelper.getString(event, "id", "");

			if (fullyFilteredEvents.contains(eventId)) {
				return FilteredMessage.censored(raw);
			}
		}

		JsonArray redactedIndex = JsonHelper.getArray(result, "redactedTextIndex", new JsonArray());
		return new FilteredMessage(raw, createFilterMask(raw, redactedIndex, hashIgnorer));
	}

	@Override
	protected int getReadTimeout() {
		return readTimeoutMs;
	}
}
