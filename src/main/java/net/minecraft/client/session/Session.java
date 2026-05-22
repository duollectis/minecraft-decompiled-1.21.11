package net.minecraft.client.session;

import com.mojang.util.UndashedUuid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Optional;
import java.util.UUID;

/**
 * Данные текущей игровой сессии: имя пользователя, UUID и токен доступа.
 * Используется для аутентификации при подключении к серверам и сервисам Mojang.
 */
@Environment(EnvType.CLIENT)
public class Session {

	private final String username;
	private final UUID uuid;
	private final String accessToken;
	private final Optional<String> xuid;
	private final Optional<String> clientId;

	public Session(String username, UUID uuid, String accessToken, Optional<String> xuid, Optional<String> clientId) {
		this.username = username;
		this.uuid = uuid;
		this.accessToken = accessToken;
		this.xuid = xuid;
		this.clientId = clientId;
	}

	public String getSessionId() {
		return "token:" + accessToken + ":" + UndashedUuid.toString(uuid);
	}

	public UUID getUuidOrNull() {
		return uuid;
	}

	public String getUsername() {
		return username;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public Optional<String> getClientId() {
		return clientId;
	}

	public Optional<String> getXuid() {
		return xuid;
	}
}
