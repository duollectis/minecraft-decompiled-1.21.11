package net.minecraft.world.chunk;

import com.mojang.logging.LogUtils;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.slf4j.Logger;

/**
 * Реализация {@link ChunkLoadProgress}, которая логирует прогресс загрузки чанков
 * в консоль с интервалом 500 мс. Делегирует вычисление прогресса {@link DeltaChunkLoadProgress}.
 */
public class LoggingChunkLoadProgress implements ChunkLoadProgress {

	private static final long LOG_INTERVAL_MS = 500L;

	private static final Logger LOGGER = LogUtils.getLogger();

	private final boolean player;
	private final DeltaChunkLoadProgress delegate;
	private boolean done;
	private long startTimeMs = Long.MAX_VALUE;
	private long nextLogTimeMs = Long.MAX_VALUE;

	public LoggingChunkLoadProgress(boolean player) {
		this.player = player;
		delegate = new DeltaChunkLoadProgress(player);
	}

	public static LoggingChunkLoadProgress withoutPlayer() {
		return new LoggingChunkLoadProgress(false);
	}

	public static LoggingChunkLoadProgress withPlayer() {
		return new LoggingChunkLoadProgress(true);
	}

	@Override
	public void init(ChunkLoadProgress.Stage stage, int chunks) {
		if (done) {
			return;
		}

		if (startTimeMs == Long.MAX_VALUE) {
			long now = Util.getMeasuringTimeMs();
			startTimeMs = now;
			nextLogTimeMs = now;
		}

		delegate.init(stage, chunks);

		switch (stage) {
			case PREPARE_GLOBAL_SPAWN -> LOGGER.info("Selecting global world spawn...");
			case LOAD_INITIAL_CHUNKS -> LOGGER.info("Loading {} persistent chunks...", chunks);
			case LOAD_PLAYER_CHUNKS -> LOGGER.info("Loading {} chunks for player spawn...", chunks);
		}
	}

	@Override
	public void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks) {
		if (done) {
			return;
		}

		delegate.progress(stage, fullChunks, totalChunks);

		if (Util.getMeasuringTimeMs() > nextLogTimeMs) {
			nextLogTimeMs += LOG_INTERVAL_MS;
			int percent = MathHelper.floor(delegate.getLoadProgress() * 100.0F);
			LOGGER.info(Text.translatable("menu.preparingSpawn", percent).getString());
		}
	}

	@Override
	public void finish(ChunkLoadProgress.Stage stage) {
		if (done) {
			return;
		}

		delegate.finish(stage);

		ChunkLoadProgress.Stage finalStage = player
				? ChunkLoadProgress.Stage.LOAD_PLAYER_CHUNKS
				: ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS;

		if (stage == finalStage) {
			LOGGER.info("Time elapsed: {} ms", Util.getMeasuringTimeMs() - startTimeMs);
			nextLogTimeMs = Long.MAX_VALUE;
			done = true;
		}
	}

	@Override
	public void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk) {
	}
}
