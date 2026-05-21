package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.GameProfileResolver;

import java.util.Optional;
import java.util.UUID;

/**
 * Резолвер игровых профилей на стороне клиента.
 * <p>Сначала ищет профиль в списке игроков текущей сессии через {@link ClientPlayNetworkHandler},
 * что позволяет избежать лишних сетевых запросов. Если игрок не найден в списке —
 * делегирует запрос базовому {@link GameProfileResolver}.
 */
@Environment(EnvType.CLIENT)
public class ClientPlayerProfileResolver implements GameProfileResolver {

	private final MinecraftClient client;
	private final GameProfileResolver profileResolver;

	/**
	 * Создаёт резолвер профилей.
	 *
	 * @param client          клиент Minecraft для доступа к сетевому обработчику
	 * @param profileResolver базовый резолвер для запросов к серверу аутентификации
	 */
	public ClientPlayerProfileResolver(MinecraftClient client, GameProfileResolver profileResolver) {
		this.client = client;
		this.profileResolver = profileResolver;
	}

	@Override
	public Optional<GameProfile> getProfileByName(String name) {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

		if (networkHandler != null) {
			PlayerListEntry entry = networkHandler.getCaseInsensitivePlayerInfo(name);

			if (entry != null) {
				return Optional.of(entry.getProfile());
			}
		}

		return profileResolver.getProfileByName(name);
	}

	@Override
	public Optional<GameProfile> getProfileById(UUID id) {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

		if (networkHandler != null) {
			PlayerListEntry entry = networkHandler.getPlayerListEntry(id);

			if (entry != null) {
				return Optional.of(entry.getProfile());
			}
		}

		return profileResolver.getProfileById(id);
	}
}
