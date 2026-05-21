package net.minecraft.client.world;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkLoadMap;
import net.minecraft.world.chunk.ChunkLoadProgress;
import net.minecraft.world.chunk.DeltaChunkLoadProgress;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
/**
 * {@code ClientChunkLoadProgress}.
 */
public class ClientChunkLoadProgress implements ChunkLoadProgress {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final long THIRTY_SECONDS = TimeUnit.SECONDS.toMillis(30L);
	public static final long WAIT_UNTIL_READY_MILLIS = 500L;

	private final DeltaChunkLoadProgress delegate = new DeltaChunkLoadProgress(true);
	private @Nullable ChunkLoadMap chunkLoadMap;
	private volatile ChunkLoadProgress.@Nullable Stage stage;
	private ClientChunkLoadProgress.@Nullable State state;
	private final long extraWaitMillis;

	public ClientChunkLoadProgress() {
		this(0L);
	}

	public ClientChunkLoadProgress(long extraWaitMillis) {
		this.extraWaitMillis = extraWaitMillis;
	}

	public void setChunkLoadMap(ChunkLoadMap map) {
		this.chunkLoadMap = map;
	}

	/**
	 * Запускает world loading.
	 *
	 * @param player player
	 * @param world world
	 * @param renderer renderer
	 */
	public void startWorldLoading(ClientPlayerEntity player, ClientWorld world, WorldRenderer renderer) {
		LOGGER.debug("[ChunkLoadProgress] startWorldLoading — переход в состояние Start");
		this.state =
				new ClientChunkLoadProgress.Start(player, world, renderer, Util.getMeasuringTimeMs() + THIRTY_SECONDS);
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (this.state == null) {
			return;
		}

		ClientChunkLoadProgress.State nextState = this.state.next();

		if (nextState != this.state) {
			LOGGER.debug(
					"[ChunkLoadProgress] tick() — смена состояния: {} -> {}",
					this.state.getClass().getSimpleName(),
					nextState.getClass().getSimpleName()
			);
		}

		this.state = nextState;

		// Таймаут-фолбэк: если сервер так и не прислал INITIAL_CHUNKS_COMING и мы застряли в Start
		if (this.state instanceof ClientChunkLoadProgress.Start startState
				&& Util.getMeasuringTimeMs() > startState.timeoutAfter()
		) {
			LOGGER.warn(
					"[ChunkLoadProgress] Таймаут ожидания INITIAL_CHUNKS_COMING ({}мс). Принудительный переход в LoadChunks -> Wait.",
					THIRTY_SECONDS
			);
			this.state = new ClientChunkLoadProgress.Wait(Util.getMeasuringTimeMs() + this.extraWaitMillis);
		}
	}

	public boolean isDone() {
		if (this.state instanceof ClientChunkLoadProgress.Wait waitState) {
			boolean ready = Util.getMeasuringTimeMs() >= waitState.readyAt();

			if (ready) {
				LOGGER.debug("[ChunkLoadProgress] isDone() = true, readyAt достигнут");
			}

			return ready;
		}

		return false;
	}

	/**
	 * Инициализирует ial chunks coming.
	 */
	public void initialChunksComing() {
		if (this.state == null) {
			return;
		}

		ClientChunkLoadProgress.State nextState = this.state.initialChunksComing();

		if (nextState != this.state) {
			LOGGER.debug(
					"[ChunkLoadProgress] initialChunksComing() — смена состояния: {} -> {}",
					this.state.getClass().getSimpleName(),
					nextState.getClass().getSimpleName()
			);
		}

		this.state = nextState;
	}

	@Override
	public void init(ChunkLoadProgress.Stage stage, int chunks) {
		this.delegate.init(stage, chunks);
		this.stage = stage;
		LOGGER.debug("[ChunkLoadProgress] init() — стадия: {}, чанков: {}", stage, chunks);
	}

	@Override
	public void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks) {
		this.delegate.progress(stage, fullChunks, totalChunks);
	}

	@Override
	public void finish(ChunkLoadProgress.Stage stage) {
		this.delegate.finish(stage);
		LOGGER.debug("[ChunkLoadProgress] finish() — стадия: {}", stage);
	}

	@Override
	public void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk) {
		if (this.chunkLoadMap != null) {
			this.chunkLoadMap.initSpawnPos(worldKey, spawnChunk);
		}
	}

	public @Nullable ChunkLoadMap getChunkLoadMap() {
		return this.chunkLoadMap;
	}

	public float getLoadProgress() {
		return this.delegate.getLoadProgress();
	}

	public boolean hasProgress() {
		return this.stage != null;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LoadChunks}.
	 */
	record LoadChunks(
			ClientPlayerEntity player,
			ClientWorld world,
			WorldRenderer worldRenderer,
			long timeoutAfter
	) implements ClientChunkLoadProgress.State {

		@Override
		public ClientChunkLoadProgress.State next() {
			// Переходим в Wait только после истечения таймаута или немедленно если timeoutAfter уже прошёл
			return new ClientChunkLoadProgress.Wait(Util.getMeasuringTimeMs());
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Start}.
	 */
	record Start(
			ClientPlayerEntity player,
			ClientWorld world,
			WorldRenderer worldRenderer,
			long timeoutAfter
	) implements ClientChunkLoadProgress.State {

		@Override
		public ClientChunkLoadProgress.State initialChunksComing() {
			return new ClientChunkLoadProgress.LoadChunks(
					this.player,
					this.world,
					this.worldRenderer,
					this.timeoutAfter
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code State}.
	 */
	sealed interface State permits ClientChunkLoadProgress.Start, ClientChunkLoadProgress.LoadChunks, ClientChunkLoadProgress.Wait {

		default ClientChunkLoadProgress.State next() {
			return this;
		}

		default ClientChunkLoadProgress.State initialChunksComing() {
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Wait}.
	 */
	record Wait(long readyAt) implements ClientChunkLoadProgress.State {
	}
}
