package net.minecraft.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsServerList;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.screen.RealmsLongRunningMcoTaskScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.realms.task.RealmsPrepareConnectionTask;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Утилитный класс для запуска игры в режиме быстрого старта (Quick Play).
 * Поддерживает одиночную игру, мультиплеер и Realms.
 */
@Environment(EnvType.CLIENT)
public class QuickPlay {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Text ERROR_TITLE = Text.translatable("quickplay.error.title");

	private static final Text ERROR_INVALID_IDENTIFIER = Text.translatable("quickplay.error.invalid_identifier");
	private static final Text ERROR_REALM_CONNECT = Text.translatable("quickplay.error.realm_connect");
	private static final Text ERROR_REALM_PERMISSION = Text.translatable("quickplay.error.realm_permission");
	private static final Text TO_TITLE = Text.translatable("gui.toTitle");
	private static final Text TO_WORLD = Text.translatable("gui.toWorld");
	private static final Text TO_REALMS = Text.translatable("gui.toRealms");

	/**
	 * Запускает быстрый старт в зависимости от варианта.
	 * При отключённом быстром старте открывает главное меню.
	 *
	 * @param client       экземпляр клиента
	 * @param variant      вариант быстрого старта
	 * @param realmsClient клиент Realms для подключения к серверу Realms
	 */
	public static void startQuickPlay(
		MinecraftClient client,
		RunArgs.QuickPlayVariant variant,
		RealmsClient realmsClient
	) {
		if (!variant.isEnabled()) {
			LOGGER.error("Quick play disabled");
			client.setScreen(new TitleScreen());
			return;
		}

		switch (variant) {
			case RunArgs.MultiplayerQuickPlay multiplayerQuickPlay ->
				startMultiplayer(client, multiplayerQuickPlay.serverAddress());
			case RunArgs.RealmsQuickPlay realmsQuickPlay ->
				startRealms(client, realmsClient, realmsQuickPlay.realmId());
			case RunArgs.SingleplayerQuickPlay singleplayerQuickPlay -> {
				String worldId = singleplayerQuickPlay.worldId();
				if (StringHelper.isBlank(worldId)) {
					worldId = getLatestLevelName(client.getLevelStorage());
				}

				startSingleplayer(client, worldId);
			}
			case RunArgs.DisabledQuickPlay ignored -> {
				LOGGER.error("Quick play disabled");
				client.setScreen(new TitleScreen());
			}
			default -> throw new MatchException(null, null);
		}
	}

	private static @Nullable String getLatestLevelName(LevelStorage storage) {
		try {
			List<LevelSummary> summaries = storage.loadSummaries(storage.getLevelList()).get();
			if (summaries.isEmpty()) {
				LOGGER.warn("no latest singleplayer world found");
				return null;
			}

			return summaries.getFirst().getName();
		} catch (ExecutionException | InterruptedException exception) {
			LOGGER.error("failed to load singleplayer world summaries", exception);
			return null;
		}
	}

	private static void startSingleplayer(MinecraftClient client, @Nullable String levelName) {
		if (!StringHelper.isBlank(levelName) && client.getLevelStorage().levelExists(levelName)) {
			client.createIntegratedServerLoader().start(levelName, () -> client.setScreen(new TitleScreen()));
			return;
		}

		Screen fallback = new SelectWorldScreen(new TitleScreen());
		client.setScreen(new DisconnectedScreen(fallback, ERROR_TITLE, ERROR_INVALID_IDENTIFIER, TO_WORLD));
	}

	private static void startMultiplayer(MinecraftClient client, String serverAddress) {
		ServerList serverList = new ServerList(client);
		serverList.loadFile();
		ServerInfo serverInfo = serverList.get(serverAddress);

		if (serverInfo == null) {
			serverInfo = new ServerInfo(
				I18n.translate("selectServer.defaultName"),
				serverAddress,
				ServerInfo.ServerType.OTHER
			);
			serverList.add(serverInfo, true);
			serverList.saveFile();
		}

		ServerAddress parsedAddress = ServerAddress.parse(serverAddress);
		ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client, parsedAddress, serverInfo, true, null);
	}

	/**
	 * Подключается к серверу Realms по числовому идентификатору.
	 * При ошибке парсинга или недоступности сервера показывает экран с ошибкой.
	 *
	 * @param client       экземпляр клиента
	 * @param realmsClient клиент Realms API
	 * @param realmId      строковый идентификатор сервера Realms (числовой)
	 */
	private static void startRealms(MinecraftClient client, RealmsClient realmsClient, String realmId) {
		long parsedRealmId;
		RealmsServerList serverList;

		try {
			parsedRealmId = Long.parseLong(realmId);
			serverList = realmsClient.listWorlds();
		} catch (NumberFormatException ignored) {
			Screen fallback = new RealmsMainScreen(new TitleScreen());
			client.setScreen(new DisconnectedScreen(fallback, ERROR_TITLE, ERROR_INVALID_IDENTIFIER, TO_REALMS));
			return;
		} catch (RealmsServiceException exception) {
			client.setScreen(new DisconnectedScreen(new TitleScreen(), ERROR_TITLE, ERROR_REALM_CONNECT, TO_TITLE));
			return;
		}

		RealmsServer targetServer = serverList.servers()
			.stream()
			.filter(server -> server.id == parsedRealmId)
			.findFirst()
			.orElse(null);

		if (targetServer == null) {
			Screen fallback = new RealmsMainScreen(new TitleScreen());
			client.setScreen(new DisconnectedScreen(fallback, ERROR_TITLE, ERROR_REALM_PERMISSION, TO_REALMS));
			return;
		}

		TitleScreen titleScreen = new TitleScreen();
		client.setScreen(new RealmsLongRunningMcoTaskScreen(
			titleScreen,
			new RealmsPrepareConnectionTask(titleScreen, targetServer)
		));
	}
}
