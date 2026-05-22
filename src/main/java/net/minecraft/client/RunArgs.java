package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.ResourceIndex;
import net.minecraft.client.session.Session;
import net.minecraft.util.StringHelper;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;

/**
 * Аргументы запуска клиента, передаваемые в {@link MinecraftClient}.
 * Группирует параметры по категориям: сеть, окно, директории, игра и быстрый старт.
 */
@Environment(EnvType.CLIENT)
public class RunArgs {

	public final Network network;
	public final WindowSettings windowSettings;
	public final Directories directories;
	public final Game game;
	public final QuickPlay quickPlay;

	public RunArgs(
		Network network,
		WindowSettings windowSettings,
		Directories dirs,
		Game game,
		QuickPlay quickPlay
	) {
		this.network = network;
		this.windowSettings = windowSettings;
		this.directories = dirs;
		this.game = game;
		this.quickPlay = quickPlay;
	}

	/**
	 * Сетевые параметры: сессия игрока и прокси-сервер.
	 */
	@Environment(EnvType.CLIENT)
	public static class Network {

		public final Session session;
		public final Proxy netProxy;

		public Network(Session session, Proxy proxy) {
			this.session = session;
			this.netProxy = proxy;
		}
	}

	/**
	 * Директории игры: рабочая, ресурс-паки, ассеты и индекс ассетов.
	 */
	@Environment(EnvType.CLIENT)
	public static class Directories {

		public final File runDir;
		public final File resourcePackDir;
		public final File assetDir;
		public final @Nullable String assetIndex;

		public Directories(File runDir, File resPackDir, File assetDir, @Nullable String assetIndex) {
			this.runDir = runDir;
			this.resourcePackDir = resPackDir;
			this.assetDir = assetDir;
			this.assetIndex = assetIndex;
		}

		/**
		 * Возвращает путь к директории ассетов.
		 * Если задан индекс ассетов, строит виртуальную файловую систему через {@link ResourceIndex}.
		 *
		 * @return путь к директории ассетов или виртуальной ФС
		 */
		public Path getAssetDir() {
			return assetIndex == null
				? assetDir.toPath()
				: ResourceIndex.buildFileSystem(assetDir.toPath(), assetIndex);
		}
	}

	/**
	 * Игровые параметры: версия, режим демо, флаги функций.
	 */
	@Environment(EnvType.CLIENT)
	public static class Game {

		public final boolean demo;
		public final String version;
		public final String versionType;
		public final boolean multiplayerDisabled;
		public final boolean onlineChatDisabled;
		public final boolean tracyEnabled;
		public final boolean renderDebugLabels;
		public final boolean offlineDeveloperMode;

		public Game(
			boolean demo,
			String version,
			String versionType,
			boolean multiplayerDisabled,
			boolean onlineChatDisabled,
			boolean tracyEnabled,
			boolean renderDebugLabels,
			boolean offlineDeveloperMode
		) {
			this.demo = demo;
			this.version = version;
			this.versionType = versionType;
			this.multiplayerDisabled = multiplayerDisabled;
			this.onlineChatDisabled = onlineChatDisabled;
			this.tracyEnabled = tracyEnabled;
			this.renderDebugLabels = renderDebugLabels;
			this.offlineDeveloperMode = offlineDeveloperMode;
		}
	}

	/**
	 * Параметры быстрого старта: путь к лог-файлу и вариант запуска.
	 */
	@Environment(EnvType.CLIENT)
	public record QuickPlay(@Nullable String logPath, QuickPlayVariant variant) {

		public boolean isEnabled() {
			return variant.isEnabled();
		}
	}

	/**
	 * Запечатанный интерфейс вариантов быстрого старта.
	 * Каждый вариант определяет, активен ли быстрый старт.
	 */
	@Environment(EnvType.CLIENT)
	public sealed interface QuickPlayVariant
		permits SingleplayerQuickPlay, MultiplayerQuickPlay, RealmsQuickPlay, DisabledQuickPlay {

		QuickPlayVariant DEFAULT = new DisabledQuickPlay();

		boolean isEnabled();
	}

	@Environment(EnvType.CLIENT)
	public record DisabledQuickPlay() implements QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return false;
		}
	}

	@Environment(EnvType.CLIENT)
	public record SingleplayerQuickPlay(@Nullable String worldId) implements QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return true;
		}
	}

	@Environment(EnvType.CLIENT)
	public record MultiplayerQuickPlay(String serverAddress) implements QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return !StringHelper.isBlank(serverAddress);
		}
	}

	@Environment(EnvType.CLIENT)
	public record RealmsQuickPlay(String realmId) implements QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return !StringHelper.isBlank(realmId);
		}
	}
}
