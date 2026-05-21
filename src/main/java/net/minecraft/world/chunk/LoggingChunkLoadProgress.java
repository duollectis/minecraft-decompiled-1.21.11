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
 * {@code LoggingChunkLoadProgress}.
 */
public class LoggingChunkLoadProgress implements ChunkLoadProgress {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final boolean player;
	private final DeltaChunkLoadProgress delegate;
	private boolean done;
	private long startTimeMs = Long.MAX_VALUE;
	private long nextLogTimeMs = Long.MAX_VALUE;

	public LoggingChunkLoadProgress(boolean player) {
		this.player = player;
		this.delegate = new DeltaChunkLoadProgress(player);
	}

	/**
	 * Without player.
	 *
	 * @return LoggingChunkLoadProgress — результат операции
	 */
	public static LoggingChunkLoadProgress withoutPlayer() {
		return new LoggingChunkLoadProgress(false);
	}

	/**
	 * With player.
	 *
	 * @return LoggingChunkLoadProgress — результат операции
	 */
	public static LoggingChunkLoadProgress withPlayer() {
		return new LoggingChunkLoadProgress(true);
	}

	@Override
	public void init(ChunkLoadProgress.Stage stage, int chunks) {
		if (!this.done) {
			if (this.startTimeMs == Long.MAX_VALUE) {
				long l = Util.getMeasuringTimeMs();
				this.startTimeMs = l;
				this.nextLogTimeMs = l;
			}

			this.delegate.init(stage, chunks);
			switch (stage) {
				case PREPARE_GLOBAL_SPAWN:
					LOGGER.info("Selecting global world spawn...");
					break;
				case LOAD_INITIAL_CHUNKS:
					LOGGER.info("Loading {} persistent chunks...", chunks);
					break;
				case LOAD_PLAYER_CHUNKS:
					LOGGER.info("Loading {} chunks for player spawn...", chunks);
			}
		}
	}

	@Override
	public void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks) {
		if (!this.done) {
			this.delegate.progress(stage, fullChunks, totalChunks);
			if (Util.getMeasuringTimeMs() > this.nextLogTimeMs) {
				this.nextLogTimeMs += 500L;
				int i = MathHelper.floor(this.delegate.getLoadProgress() * 100.0F);
				LOGGER.info(Text.translatable("menu.preparingSpawn", i).getString());
			}
		}
	}

	@Override
	public void finish(ChunkLoadProgress.Stage stage) {
		if (!this.done) {
			this.delegate.finish(stage);
			ChunkLoadProgress.Stage
					stage2 =
					this.player ? ChunkLoadProgress.Stage.LOAD_PLAYER_CHUNKS
					            : ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS;
			if (stage == stage2) {
				LOGGER.info("Time elapsed: {} ms", Util.getMeasuringTimeMs() - this.startTimeMs);
				this.nextLogTimeMs = Long.MAX_VALUE;
				this.done = true;
			}
		}
	}

	@Override
	public void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk) {
	}
}
