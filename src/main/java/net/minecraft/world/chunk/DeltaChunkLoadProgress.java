package net.minecraft.world.chunk;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Отслеживает прогресс загрузки чанков в виде нормализованного значения [0.0, 1.0].
 * Разбивает общий прогресс на три фазы: начальные чанки, постоянные чанки и чанки игрока.
 */
public class DeltaChunkLoadProgress implements ChunkLoadProgress {

	private static final int INITIAL_CHUNKS = 10;
	private static final int PLAYER_CHUNKS = MathHelper.square(7);

	private final boolean player;
	private int totalChunks;
	private int previousLoadedChunks;
	private int currentPhaseChunks;
	private float fullyLoadedChunksRatio;
	private volatile float loadProgress;

	public DeltaChunkLoadProgress(boolean player) {
		this.player = player;
	}

	@Override
	public void init(ChunkLoadProgress.Stage stage, int chunks) {
		if (!shouldLoad(stage)) {
			return;
		}

		switch (stage) {
			case LOAD_INITIAL_CHUNKS -> {
				int playerChunks = player ? PLAYER_CHUNKS : 0;
				totalChunks = INITIAL_CHUNKS + chunks + playerChunks;
				initPhase(INITIAL_CHUNKS);
				finishPhase();
				initPhase(chunks);
			}
			case LOAD_PLAYER_CHUNKS -> initPhase(PLAYER_CHUNKS);
		}
	}

	private void initPhase(int chunks) {
		currentPhaseChunks = chunks;
		fullyLoadedChunksRatio = 0.0F;
		recalculateLoadProgress();
	}

	@Override
	public void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks) {
		if (!shouldLoad(stage)) {
			return;
		}

		fullyLoadedChunksRatio = totalChunks == 0 ? 0.0F : (float) fullChunks / totalChunks;
		recalculateLoadProgress();
	}

	@Override
	public void finish(ChunkLoadProgress.Stage stage) {
		if (shouldLoad(stage)) {
			finishPhase();
		}
	}

	private void finishPhase() {
		previousLoadedChunks += currentPhaseChunks;
		currentPhaseChunks = 0;
		recalculateLoadProgress();
	}

	private boolean shouldLoad(ChunkLoadProgress.Stage stage) {
		return switch (stage) {
			case LOAD_INITIAL_CHUNKS -> true;
			case LOAD_PLAYER_CHUNKS -> player;
			default -> false;
		};
	}

	private void recalculateLoadProgress() {
		if (totalChunks == 0) {
			loadProgress = 0.0F;
			return;
		}

		float loaded = previousLoadedChunks + fullyLoadedChunksRatio * currentPhaseChunks;
		loadProgress = loaded / totalChunks;
	}

	public float getLoadProgress() {
		return loadProgress;
	}

	@Override
	public void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk) {
	}
}
