package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.chunk.ChunkLoadProgress;
import net.minecraft.world.chunk.ChunkLoadingCounter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Класс Prepare Spawn Task.
 */
public class PrepareSpawnTask implements ServerPlayerConfigurationTask {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final ServerPlayerConfigurationTask.Key KEY = new ServerPlayerConfigurationTask.Key("prepare_spawn");
	public static final int CHUNK_LOAD_RADIUS = 3;
	final MinecraftServer server;
	final PlayerConfigEntry player;
	final ChunkLoadProgress chunkLoadProgress;
	private PrepareSpawnTask.@Nullable Stage stage;

	public PrepareSpawnTask(MinecraftServer server, PlayerConfigEntry player) {
		this.server = server;
		this.player = player;
		this.chunkLoadProgress = server.getChunkLoadProgress();
	}

	@Override
	public void sendPacket(Consumer<Packet<?>> sender) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
			Optional<ReadView> optional = this.server
					.getPlayerManager()
					.loadPlayerData(this.player)
					.map(nbt -> NbtReadView.create(logging, this.server.getRegistryManager(), nbt));
			ServerPlayerEntity.SavePos
					savePos =
					optional.<ServerPlayerEntity.SavePos>flatMap(view -> view.read(ServerPlayerEntity.SavePos.CODEC))
					        .orElse(ServerPlayerEntity.SavePos.EMPTY);
			WorldProperties.SpawnPoint
					spawnPoint =
					this.server.getSaveProperties().getMainWorldProperties().getSpawnPoint();
			ServerWorld serverWorld = savePos.dimension().map(this.server::getWorld).orElseGet(() -> {
				ServerWorld serverWorldx = this.server.getWorld(spawnPoint.getDimension());
				return serverWorldx != null ? serverWorldx : this.server.getOverworld();
			});
			CompletableFuture<Vec3d> completableFuture = savePos.position()
			                                                    .map(CompletableFuture::completedFuture)
			                                                    .orElseGet(() -> SpawnLocating.locateSpawnPos(
					                                                    serverWorld,
					                                                    spawnPoint.getPos()
			                                                    ));
			Vec2f vec2f = savePos.rotation().orElse(new Vec2f(spawnPoint.yaw(), spawnPoint.pitch()));
			this.stage = new PrepareSpawnTask.LoadPlayerChunks(serverWorld, completableFuture, vec2f);
		}
	}

	@Override
	public boolean hasFinished() {
		return switch (this.stage) {
			case null -> false;
			case PrepareSpawnTask.LoadPlayerChunks loadPlayerChunks -> {
				PrepareSpawnTask.PlayerSpawn playerSpawn = loadPlayerChunks.tryFinish();
				if (playerSpawn != null) {
					this.stage = playerSpawn;
					yield true;
				}
				else {
					yield false;
				}
			}
			case PrepareSpawnTask.PlayerSpawn playerSpawn -> true;
			default -> throw new MatchException(null, null);
		};
	}

	/**
	 * Обрабатывает событие ready.
	 *
	 * @param connection connection
	 * @param clientData client data
	 *
	 * @return ServerPlayerEntity — результат операции
	 */
	public ServerPlayerEntity onReady(ClientConnection connection, ConnectedClientData clientData) {
		if (this.stage instanceof PrepareSpawnTask.PlayerSpawn playerSpawn) {
			return playerSpawn.onReady(connection, clientData);
		}
		else {
			throw new IllegalStateException("Player spawn was not ready");
		}
	}

	public void tick() {
		if (this.stage instanceof PrepareSpawnTask.PlayerSpawn playerSpawn) {
			playerSpawn.tick();
		}
	}

	/**
	 * Обрабатывает событие disconnected.
	 */
	public void onDisconnected() {
		if (this.stage instanceof PrepareSpawnTask.LoadPlayerChunks loadPlayerChunks) {
			loadPlayerChunks.cancel();
		}

		this.stage = null;
	}

	@Override
	public ServerPlayerConfigurationTask.Key getKey() {
		return KEY;
	}

	final class LoadPlayerChunks implements PrepareSpawnTask.Stage {

		private final ServerWorld world;
		private final CompletableFuture<Vec3d> spawnPos;
		private final Vec2f rotation;
		private @Nullable CompletableFuture<?> chunkLoadingFuture;
		private final ChunkLoadingCounter chunkCounter = new ChunkLoadingCounter();

		LoadPlayerChunks(final ServerWorld world, final CompletableFuture<Vec3d> spawnPos, final Vec2f rotation) {
			this.world = world;
			this.spawnPos = spawnPos;
			this.rotation = rotation;
		}

		/**
		 * Проверяет возможность cel.
		 */
		public void cancel() {
			this.spawnPos.cancel(false);
		}

		public PrepareSpawnTask.@Nullable PlayerSpawn tryFinish() {
			if (!this.spawnPos.isDone()) {
				return null;
			}
			else {
				Vec3d vec3d = this.spawnPos.join();
				if (this.chunkLoadingFuture == null) {
					ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(vec3d));
					this.chunkCounter
							.load(
									this.world,
									() ->
											this.chunkLoadingFuture =
													this.world
															.getChunkManager()
															.addChunkLoadingTicket(
																	ChunkTicketType.PLAYER_SPAWN,
																	chunkPos,
																	3
															)
							);
					PrepareSpawnTask.this.chunkLoadProgress.init(
							ChunkLoadProgress.Stage.LOAD_PLAYER_CHUNKS,
							this.chunkCounter.getTotalChunks()
					);
					PrepareSpawnTask.this.chunkLoadProgress.initSpawnPos(this.world.getRegistryKey(), chunkPos);
				}

				PrepareSpawnTask.this.chunkLoadProgress
						.progress(
								ChunkLoadProgress.Stage.LOAD_PLAYER_CHUNKS,
								this.chunkCounter.getFullChunks(),
								this.chunkCounter.getTotalChunks()
						);
				if (!this.chunkLoadingFuture.isDone()) {
					return null;
				}
				else {
					PrepareSpawnTask.this.chunkLoadProgress.finish(ChunkLoadProgress.Stage.LOAD_PLAYER_CHUNKS);
					return PrepareSpawnTask.this.new PlayerSpawn(this.world, vec3d, this.rotation);
				}
			}
		}
	}

	final class PlayerSpawn implements PrepareSpawnTask.Stage {

		private final ServerWorld world;
		private final Vec3d spawnPos;
		private final Vec2f rotation;

		PlayerSpawn(final ServerWorld world, final Vec3d spawnPos, final Vec2f rotation) {
			this.world = world;
			this.spawnPos = spawnPos;
			this.rotation = rotation;
		}

		/**
		 * Tick.
		 */
		public void tick() {
			this.world
					.getChunkManager()
					.addTicket(ChunkTicketType.PLAYER_SPAWN, new ChunkPos(BlockPos.ofFloored(this.spawnPos)), 3);
		}

		/**
		 * Обрабатывает событие ready.
		 *
		 * @param connection connection
		 * @param clientData client data
		 *
		 * @return ServerPlayerEntity — результат операции
		 */
		public ServerPlayerEntity onReady(ClientConnection connection, ConnectedClientData clientData) {
			ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(this.spawnPos));
			this.world.loadChunks(chunkPos, 3);
			ServerPlayerEntity serverPlayerEntity = new ServerPlayerEntity(
					PrepareSpawnTask.this.server, this.world, clientData.gameProfile(), clientData.syncedOptions()
			);

			ServerPlayerEntity var7;
			try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
					serverPlayerEntity.getErrorReporterContext(),
					PrepareSpawnTask.LOGGER
			)
			) {
				Optional<ReadView> optional = PrepareSpawnTask.this.server
						.getPlayerManager()
						.loadPlayerData(PrepareSpawnTask.this.player)
						.map(playerData -> NbtReadView.create(
								logging,
								PrepareSpawnTask.this.server.getRegistryManager(),
								playerData
						));
				optional.ifPresent(serverPlayerEntity::readData);
				serverPlayerEntity.refreshPositionAndAngles(this.spawnPos, this.rotation.x, this.rotation.y);
				PrepareSpawnTask.this.server
						.getPlayerManager()
						.onPlayerConnect(connection, serverPlayerEntity, clientData);
				optional.ifPresent(playerData -> {
					serverPlayerEntity.readEnderPearls(playerData);
					serverPlayerEntity.readRootVehicle(playerData);
				});
				var7 = serverPlayerEntity;
			}

			return var7;
		}
	}

	sealed interface Stage permits PrepareSpawnTask.LoadPlayerChunks, PrepareSpawnTask.PlayerSpawn {
	}
}
