package net.minecraft.client.network;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Управляет социальными взаимодействиями: скрытием игроков и списком блокировок.
 * <p>Скрытые игроки ({@link #hidePlayer}) не видны только локально в текущей сессии.
 * Заблокированные игроки определяются через {@link UserApiService} и загружаются
 * асинхронно при вызове {@link #loadBlockList}.
 */
@Environment(EnvType.CLIENT)
public class SocialInteractionsManager {

	private final MinecraftClient client;
	private final UserApiService userApiService;
	private final Set<UUID> hiddenPlayers = Sets.newHashSet();
	private final Map<String, UUID> playerNameByUuid = Maps.newHashMap();
	private boolean blockListLoaded;
	private CompletableFuture<?> blockListLoader = CompletableFuture.completedFuture(null);

	/**
	 * Создаёт менеджер социальных взаимодействий.
	 *
	 * @param client         клиент Minecraft
	 * @param userApiService сервис API пользователей для проверки блокировок
	 */
	public SocialInteractionsManager(MinecraftClient client, UserApiService userApiService) {
		this.client = client;
		this.userApiService = userApiService;
	}

	/**
	 * Скрывает игрока локально (только в текущей сессии).
	 *
	 * @param uuid UUID игрока для скрытия
	 */
	public void hidePlayer(UUID uuid) {
		hiddenPlayers.add(uuid);
	}

	/**
	 * Отменяет скрытие игрока.
	 *
	 * @param uuid UUID игрока
	 */
	public void showPlayer(UUID uuid) {
		hiddenPlayers.remove(uuid);
	}

	/**
	 * Проверяет, заглушен ли игрок (скрыт или заблокирован).
	 *
	 * @param uuid UUID игрока
	 * @return {@code true} если игрок скрыт или заблокирован
	 */
	public boolean isPlayerMuted(UUID uuid) {
		return isPlayerHidden(uuid) || isPlayerBlocked(uuid);
	}

	/**
	 * Проверяет, скрыт ли игрок локально.
	 *
	 * @param uuid UUID игрока
	 * @return {@code true} если игрок скрыт
	 */
	public boolean isPlayerHidden(UUID uuid) {
		return hiddenPlayers.contains(uuid);
	}

	/**
	 * Асинхронно загружает список блокировок из {@link UserApiService}.
	 */
	public void loadBlockList() {
		blockListLoaded = true;
		blockListLoader = blockListLoader.thenRunAsync(
				userApiService::refreshBlockList,
				Util.getIoWorkerExecutor()
		);
	}

	/**
	 * Помечает список блокировок как выгруженный.
	 */
	public void unloadBlockList() {
		blockListLoaded = false;
	}

	/**
	 * Проверяет, заблокирован ли игрок через {@link UserApiService}.
	 * Блокирует поток до завершения загрузки списка блокировок.
	 *
	 * @param uuid UUID игрока
	 * @return {@code true} если игрок заблокирован
	 */
	public boolean isPlayerBlocked(UUID uuid) {
		if (blockListLoaded == false) {
			return false;
		}

		blockListLoader.join();
		return userApiService.isBlockedPlayer(uuid);
	}

	/**
	 * Возвращает множество UUID скрытых игроков.
	 *
	 * @return неизменяемое представление множества скрытых игроков
	 */
	public Set<UUID> getHiddenPlayers() {
		return hiddenPlayers;
	}

	/**
	 * Возвращает UUID игрока по его имени.
	 *
	 * @param playerName имя игрока
	 * @return UUID или {@link Util#NIL_UUID} если игрок не найден
	 */
	public UUID getUuid(String playerName) {
		return playerNameByUuid.getOrDefault(playerName, Util.NIL_UUID);
	}

	/**
	 * Регистрирует игрока как онлайн и уведомляет экран социальных взаимодействий.
	 *
	 * @param player запись игрока из списка игроков
	 */
	public void setPlayerOnline(PlayerListEntry player) {
		GameProfile profile = player.getProfile();
		playerNameByUuid.put(profile.name(), profile.id());

		if (client.currentScreen instanceof SocialInteractionsScreen screen) {
			screen.setPlayerOnline(player);
		}
	}

	/**
	 * Уведомляет экран социальных взаимодействий об отключении игрока.
	 *
	 * @param uuid UUID отключившегося игрока
	 */
	public void setPlayerOffline(UUID uuid) {
		if (client.currentScreen instanceof SocialInteractionsScreen screen) {
			screen.setPlayerOffline(uuid);
		}
	}
}
