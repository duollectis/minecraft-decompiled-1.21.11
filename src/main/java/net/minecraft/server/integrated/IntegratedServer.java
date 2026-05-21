package net.minecraft.server.integrated;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.LanServerPinger;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ApiServices;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.ModStatus;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.ChunkLoadProgress;
import net.minecraft.world.debug.gizmo.GizmoCollectorImpl;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.storage.StorageKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
/**
 * {@code IntegratedServer}.
 */
public class IntegratedServer extends MinecraftServer {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int AUTOSAVE_INTERVAL_MINUTES = 2;
	public static final int LAN_PORT_RANGE = 8;
	private final MinecraftClient client;
	private boolean paused = true;
	private int lanPort = -1;
	private @Nullable GameMode forcedGameMode;
	private @Nullable LanServerPinger lanPinger;
	private @Nullable UUID localPlayerUuid;
	private int simulationDistance = 0;
	private volatile List<GizmoCollectorImpl.Entry> gizmoEntries = new ArrayList<>();
	private final GizmoCollectorImpl gizmoCollector = new GizmoCollectorImpl();

	public IntegratedServer(
			Thread serverThread,
			MinecraftClient client,
			LevelStorage.Session session,
			ResourcePackManager dataPackManager,
			SaveLoader saveLoader,
			ApiServices apiServices,
			ChunkLoadProgress chunkLoadProgress
	) {
		super(
				serverThread,
				session,
				dataPackManager,
				saveLoader,
				client.getNetworkProxy(),
				client.getDataFixer(),
				apiServices,
				chunkLoadProgress
		);
		this.setHostProfile(client.getGameProfile());
		this.setDemo(client.isDemo());
		this.setPlayerManager(new IntegratedPlayerManager(this, this.getCombinedDynamicRegistries(), this.saveHandler));
		this.client = client;
	}

	@Override
	public boolean setupServer() {
		LOGGER.info("Starting integrated minecraft server version {}", SharedConstants.getGameVersion().name());
		this.setOnlineMode(true);
		this.generateKeyPair();
		this.loadWorld();
		GameProfile gameProfile = this.getHostProfile();
		String string = this.getSaveProperties().getLevelName();
		this.setMotd(gameProfile != null ? gameProfile.name() + " - " + string : string);
		return true;
	}

	@Override
	public boolean isPaused() {
		return this.paused;
	}

	@Override
	public void processPacketsAndTick(boolean sprint) {
		try (GizmoDrawing.CollectorScope collectorScope = GizmoDrawing.using(this.gizmoCollector)) {
			super.processPacketsAndTick(sprint);
		}

		if (this.getTickManager().shouldTick()) {
			this.gizmoEntries = this.gizmoCollector.extractGizmos();
		}
	}

	@Override
	public void tick(BooleanSupplier shouldKeepTicking) {
		boolean bl = this.paused;
		this.paused = MinecraftClient.getInstance().isPaused() || this.getPlayerManager().getPlayerList().isEmpty();
		Profiler profiler = Profilers.get();
		if (!bl && this.paused) {
			profiler.push("autoSave");
			LOGGER.info("Saving and pausing game...");
			this.saveAll(false, false, false);
			profiler.pop();
		}

		if (this.paused) {
			this.incrementTotalWorldTimeStat();
		}
		else {
			if (bl) {
				this.sendTimeUpdatePackets();
			}

			super.tick(shouldKeepTicking);
			int i = Math.max(2, this.client.options.getViewDistance().getValue());
			if (i != this.getPlayerManager().getViewDistance()) {
				LOGGER.info("Changing view distance to {}, from {}", i, this.getPlayerManager().getViewDistance());
				this.getPlayerManager().setViewDistance(i);
			}

			int j = Math.max(2, this.client.options.getSimulationDistance().getValue());
			if (j != this.simulationDistance) {
				LOGGER.info("Changing simulation distance to {}, from {}", j, this.simulationDistance);
				this.getPlayerManager().setSimulationDistance(j);
				this.simulationDistance = j;
			}
		}
	}

	protected MultiValueDebugSampleLogImpl getDebugSampleLog() {
		return this.client.getDebugHud().getTickNanosLog();
	}

	@Override
	public boolean shouldPushTickTimeLog() {
		return true;
	}

	private void incrementTotalWorldTimeStat() {
		this.tickNetworkIo();

		for (ServerPlayerEntity serverPlayerEntity : this.getPlayerManager().getPlayerList()) {
			serverPlayerEntity.incrementStat(Stats.TOTAL_WORLD_TIME);
		}
	}

	@Override
	public boolean shouldBroadcastRconToOps() {
		return true;
	}

	@Override
	public boolean shouldBroadcastConsoleToOps() {
		return true;
	}

	@Override
	public Path getRunDirectory() {
		return this.client.runDirectory.toPath();
	}

	@Override
	public boolean isDedicated() {
		return false;
	}

	@Override
	public int getRateLimit() {
		return 0;
	}

	@Override
	public boolean isUsingNativeTransport() {
		return this.client.options.shouldUseNativeTransport();
	}

	@Override
	public void setCrashReport(CrashReport report) {
		this.client.setCrashReportSupplier(report);
	}

	@Override
	public SystemDetails addExtraSystemDetails(SystemDetails details) {
		details.addSection("Type", "Integrated Server (map_client.txt)");
		details.addSection("Is Modded", () -> this.getModStatus().getMessage());
		details.addSection("Launched Version", this.client::getGameVersion);
		return details;
	}

	@Override
	public ModStatus getModStatus() {
		return MinecraftClient.getModStatus().combine(super.getModStatus());
	}

	@Override
	public boolean openToLan(@Nullable GameMode gameMode, boolean cheatsAllowed, int port) {
		try {
			this.client.loadBlockList();
			this.client.getNetworkHandler().fetchProfileKey();
			this.getNetworkIo().bind(null, port);
			LOGGER.info("Started serving on {}", port);
			this.lanPort = port;
			this.lanPinger = new LanServerPinger(this.getServerMotd(), port + "");
			this.lanPinger.start();
			this.forcedGameMode = gameMode;
			this.getPlayerManager().setCheatsAllowed(cheatsAllowed);
			PermissionPredicate
					permissionPredicate =
					this.getPermissionLevel(this.client.player.getPlayerConfigEntry());
			this.client.player.setPermissions(permissionPredicate);

			for (ServerPlayerEntity serverPlayerEntity : this.getPlayerManager().getPlayerList()) {
				this.getCommandManager().sendCommandTree(serverPlayerEntity);
			}

			return true;
		}
		catch (IOException var7) {
			return false;
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (this.lanPinger != null) {
			this.lanPinger.interrupt();
			this.lanPinger = null;
		}
	}

	@Override
	public void stop(boolean waitForShutdown) {
		this.submitAndJoin(() -> {
			for (ServerPlayerEntity serverPlayerEntity : Lists.newArrayList(this.getPlayerManager().getPlayerList())) {
				if (!serverPlayerEntity.getUuid().equals(this.localPlayerUuid)) {
					this.getPlayerManager().remove(serverPlayerEntity);
				}
			}
		});
		super.stop(waitForShutdown);
		if (this.lanPinger != null) {
			this.lanPinger.interrupt();
			this.lanPinger = null;
		}
	}

	@Override
	public boolean isRemote() {
		return this.lanPort > -1;
	}

	@Override
	public int getServerPort() {
		return this.lanPort;
	}

	@Override
	public void setDefaultGameMode(GameMode gameMode) {
		super.setDefaultGameMode(gameMode);
		this.forcedGameMode = null;
	}

	@Override
	public LeveledPermissionPredicate getOpPermissionLevel() {
		return LeveledPermissionPredicate.GAMEMASTERS;
	}

	public LeveledPermissionPredicate getFunctionPermissions() {
		return LeveledPermissionPredicate.GAMEMASTERS;
	}

	public void setLocalPlayerUuid(UUID localPlayerUuid) {
		this.localPlayerUuid = localPlayerUuid;
	}

	@Override
	public boolean isHost(PlayerConfigEntry player) {
		return this.getHostProfile() != null && player.name().equalsIgnoreCase(this.getHostProfile().name());
	}

	@Override
	public int adjustTrackingDistance(int initialDistance) {
		return (int) (this.client.options.getEntityDistanceScaling().getValue() * initialDistance);
	}

	@Override
	public boolean syncChunkWrites() {
		return this.client.options.syncChunkWrites;
	}

	@Override
	public @Nullable GameMode getForcedGameMode() {
		return this.isRemote() && !this.isHardcore() ? (GameMode) MoreObjects.firstNonNull(
				this.forcedGameMode,
				this.saveProperties.getGameMode()
		) : null;
	}

	@Override
	public GlobalPos getSpawnPos() {
		NbtCompound nbtCompound = this.saveProperties.getPlayerData();
		if (nbtCompound == null) {
			return super.getSpawnPos();
		}
		else {
			try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
				ReadView readView = NbtReadView.create(logging, this.getRegistryManager(), nbtCompound);
				ServerPlayerEntity.SavePos
						savePos =
						readView.<ServerPlayerEntity.SavePos>read(ServerPlayerEntity.SavePos.CODEC)
						        .orElse(ServerPlayerEntity.SavePos.EMPTY);
				if (savePos.dimension().isPresent() && savePos.position().isPresent()) {
					return new GlobalPos(savePos.dimension().get(), BlockPos.ofFloored(savePos.position().get()));
				}
			}

			return super.getSpawnPos();
		}
	}

	@Override
	public boolean saveAll(boolean suppressLogs, boolean flush, boolean force) {
		boolean bl = super.saveAll(suppressLogs, flush, force);
		this.checkLowDiskSpaceWarning();
		return bl;
	}

	private void checkLowDiskSpaceWarning() {
		if (this.session.shouldShowLowDiskSpaceWarning()) {
			this.client.execute(() -> SystemToast.addLowDiskSpace(this.client));
		}
	}

	@Override
	public void onChunkLoadFailure(Throwable exception, StorageKey key, ChunkPos chunkPos) {
		super.onChunkLoadFailure(exception, key, chunkPos);
		this.checkLowDiskSpaceWarning();
		this.client.execute(() -> SystemToast.addChunkLoadFailure(this.client, chunkPos));
	}

	@Override
	public void onChunkSaveFailure(Throwable exception, StorageKey key, ChunkPos chunkPos) {
		super.onChunkSaveFailure(exception, key, chunkPos);
		this.checkLowDiskSpaceWarning();
		this.client.execute(() -> SystemToast.addChunkSaveFailure(this.client, chunkPos));
	}

	@Override
	public int getMaxPlayerCount() {
		return 8;
	}

	public Collection<GizmoCollectorImpl.Entry> getGizmoEntries() {
		return this.gizmoEntries;
	}
}
