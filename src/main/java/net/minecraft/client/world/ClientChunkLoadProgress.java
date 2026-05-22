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

/**
 * Клиентская реализация {@link ChunkLoadProgress}, управляющая конечным автоматом загрузки мира.
 *
 * <p>Жизненный цикл состояний: {@link Start} → {@link LoadChunks} → {@link Wait}.
 * Переход из {@code Start} в {@code LoadChunks} происходит при получении пакета
 * {@code INITIAL_CHUNKS_COMING} от сервера. Если пакет не пришёл за 30 секунд —
 * срабатывает таймаут и автомат принудительно переходит в {@code Wait}.
 */
@Environment(EnvType.CLIENT)
public class ClientChunkLoadProgress implements ChunkLoadProgress {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30L);
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
		chunkLoadMap = map;
	}

	/**
	 * Инициализирует загрузку мира, переводя автомат в начальное состояние {@link Start}.
	 * Устанавливает таймаут ожидания пакета {@code INITIAL_CHUNKS_COMING}.
	 *
	 * @param player   игрок, для которого загружается мир
	 * @param world    загружаемый клиентский мир
	 * @param renderer рендерер мира
	 */
	public void startWorldLoading(ClientPlayerEntity player, ClientWorld world, WorldRenderer renderer) {
		LOGGER.debug("[ChunkLoadProgress] startWorldLoading — переход в состояние Start");
		state = new ClientChunkLoadProgress.Start(
				player,
				world,
				renderer,
				Util.getMeasuringTimeMs() + TIMEOUT_MILLIS
		);
	}

	/**
	 * Обновляет состояние автомата за один тик.
	 * Если автомат завис в {@link Start} дольше {@link #TIMEOUT_MILLIS} мс,
	 * принудительно переходит в {@link Wait}.
	 */
	public void tick() {
		if (state == null) {
			return;
		}

		ClientChunkLoadProgress.State nextState = state.next();

		if (nextState != state) {
			LOGGER.debug(
					"[ChunkLoadProgress] tick() — смена состояния: {} -> {}",
					state.getClass().getSimpleName(),
					nextState.getClass().getSimpleName()
			);
		}

		state = nextState;

		// Таймаут-фолбэк: сервер не прислал INITIAL_CHUNKS_COMING вовремя
		if (state instanceof ClientChunkLoadProgress.Start startState
				&& Util.getMeasuringTimeMs() > startState.timeoutAfter()
		) {
			LOGGER.warn(
					"[ChunkLoadProgress] Таймаут ожидания INITIAL_CHUNKS_COMING ({}мс). Принудительный переход в Wait.",
					TIMEOUT_MILLIS
			);
			state = new ClientChunkLoadProgress.Wait(Util.getMeasuringTimeMs() + extraWaitMillis);
		}

	}

	/**
	 * Возвращает {@code true}, если загрузка завершена и можно показывать мир.
	 * Условие: автомат находится в состоянии {@link Wait} и время ожидания истекло.
	 */
	public boolean isDone() {
		if (state instanceof ClientChunkLoadProgress.Wait waitState) {
			boolean ready = Util.getMeasuringTimeMs() >= waitState.readyAt();

			if (ready) {
				LOGGER.debug("[ChunkLoadProgress] isDone() = true, readyAt достигнут");
			}

			return ready;
		}

		return false;
	}

	/**
	 * Вызывается при получении пакета {@code INITIAL_CHUNKS_COMING} от сервера.
	 * Переводит автомат из {@link Start} в {@link LoadChunks}.
	 */
	public void initialChunksComing() {
		if (state == null) {
			return;
		}

		ClientChunkLoadProgress.State nextState = state.initialChunksComing();

		if (nextState != state) {
			LOGGER.debug(
					"[ChunkLoadProgress] initialChunksComing() — смена состояния: {} -> {}",
					state.getClass().getSimpleName(),
					nextState.getClass().getSimpleName()
			);
		}

		state = nextState;
	}

	@Override
	public void init(ChunkLoadProgress.Stage stage, int chunks) {
		delegate.init(stage, chunks);
		this.stage = stage;
		LOGGER.debug("[ChunkLoadProgress] init() — стадия: {}, чанков: {}", stage, chunks);
	}

	@Override
	public void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks) {
		delegate.progress(stage, fullChunks, totalChunks);
	}

	@Override
	public void finish(ChunkLoadProgress.Stage stage) {
		delegate.finish(stage);
		LOGGER.debug("[ChunkLoadProgress] finish() — стадия: {}", stage);
	}

	@Override
	public void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk) {
		if (chunkLoadMap != null) {
			chunkLoadMap.initSpawnPos(worldKey, spawnChunk);
		}

	}

	public @Nullable ChunkLoadMap getChunkLoadMap() {
		return chunkLoadMap;
	}

	public float getLoadProgress() {
		return delegate.getLoadProgress();
	}

	public boolean hasProgress() {
		return stage != null;
	}

	// -------------------------------------------------------------------------
	// Состояния конечного автомата
	// -------------------------------------------------------------------------

	/**
	 * Интерфейс состояния конечного автомата загрузки чанков.
	 * Допустимые реализации: {@link Start}, {@link LoadChunks}, {@link Wait}.
	 */
	@Environment(EnvType.CLIENT)
	sealed interface State permits ClientChunkLoadProgress.Start, ClientChunkLoadProgress.LoadChunks, ClientChunkLoadProgress.Wait {

		default ClientChunkLoadProgress.State next() {
			return this;
		}

		default ClientChunkLoadProgress.State initialChunksComing() {
			return this;
		}

	}

	/**
	 * Начальное состояние: ожидание пакета {@code INITIAL_CHUNKS_COMING} от сервера.
	 * При получении пакета переходит в {@link LoadChunks}.
	 *
	 * @param timeoutAfter абсолютная метка времени (мс), после которой срабатывает таймаут
	 */
	@Environment(EnvType.CLIENT)
	record Start(
			ClientPlayerEntity player,
			ClientWorld world,
			WorldRenderer worldRenderer,
			long timeoutAfter
	) implements ClientChunkLoadProgress.State {

		@Override
		public ClientChunkLoadProgress.State initialChunksComing() {
			return new ClientChunkLoadProgress.LoadChunks(player, world, worldRenderer, timeoutAfter);
		}

	}

	/**
	 * Состояние активной загрузки чанков.
	 * При следующем тике немедленно переходит в {@link Wait}.
	 */
	@Environment(EnvType.CLIENT)
	record LoadChunks(
			ClientPlayerEntity player,
			ClientWorld world,
			WorldRenderer worldRenderer,
			long timeoutAfter
	) implements ClientChunkLoadProgress.State {

		@Override
		public ClientChunkLoadProgress.State next() {
			return new ClientChunkLoadProgress.Wait(Util.getMeasuringTimeMs());
		}

	}

	/**
	 * Финальное состояние ожидания перед показом мира.
	 *
	 * @param readyAt абсолютная метка времени (мс), после которой {@link #isDone()} вернёт {@code true}
	 */
	@Environment(EnvType.CLIENT)
	record Wait(long readyAt) implements ClientChunkLoadProgress.State {
	}

}
