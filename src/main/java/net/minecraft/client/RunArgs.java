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

@Environment(EnvType.CLIENT)
/**
 * {@code RunArgs}.
 */
public class RunArgs {

	public final RunArgs.Network network;
	public final WindowSettings windowSettings;
	public final RunArgs.Directories directories;
	public final RunArgs.Game game;
	public final RunArgs.QuickPlay quickPlay;

	public RunArgs(
			RunArgs.Network network,
			WindowSettings windowSettings,
			RunArgs.Directories dirs,
			RunArgs.Game game,
			RunArgs.QuickPlay quickPlay
	) {
		this.network = network;
		this.windowSettings = windowSettings;
		this.directories = dirs;
		this.game = game;
		this.quickPlay = quickPlay;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Directories}.
	 */
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

		public Path getAssetDir() {
			return this.assetIndex == null ? this.assetDir.toPath()
			                               : ResourceIndex.buildFileSystem(this.assetDir.toPath(), this.assetIndex);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DisabledQuickPlay}.
	 */
	public record DisabledQuickPlay() implements RunArgs.QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return false;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Game}.
	 */
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code MultiplayerQuickPlay}.
	 */
	public record MultiplayerQuickPlay(String serverAddress) implements RunArgs.QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return !StringHelper.isBlank(this.serverAddress);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Network}.
	 */
	public static class Network {

		public final Session session;
		public final Proxy netProxy;

		public Network(Session session, Proxy proxy) {
			this.session = session;
			this.netProxy = proxy;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code QuickPlay}.
	 */
	public record QuickPlay(@Nullable String logPath, RunArgs.QuickPlayVariant variant) {

		public boolean isEnabled() {
			return this.variant.isEnabled();
		}
	}

	@Environment(EnvType.CLIENT)
	public sealed interface QuickPlayVariant
			permits RunArgs.SingleplayerQuickPlay,
			RunArgs.MultiplayerQuickPlay,
			RunArgs.RealmsQuickPlay,
			RunArgs.DisabledQuickPlay {

		RunArgs.QuickPlayVariant DEFAULT = new RunArgs.DisabledQuickPlay();

		boolean isEnabled();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RealmsQuickPlay}.
	 */
	public record RealmsQuickPlay(String realmId) implements RunArgs.QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return !StringHelper.isBlank(this.realmId);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SingleplayerQuickPlay}.
	 */
	public record SingleplayerQuickPlay(@Nullable String worldId) implements RunArgs.QuickPlayVariant {

		@Override
		public boolean isEnabled() {
			return true;
		}
	}
}
