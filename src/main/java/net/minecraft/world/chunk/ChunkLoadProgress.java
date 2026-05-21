package net.minecraft.world.chunk;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * {@code ChunkLoadProgress}.
 */
public interface ChunkLoadProgress {

	static ChunkLoadProgress compose(ChunkLoadProgress first, ChunkLoadProgress second) {
		return new ChunkLoadProgress() {
			@Override
			public void init(ChunkLoadProgress.Stage stage, int chunks) {
				first.init(stage, chunks);
				second.init(stage, chunks);
			}

			@Override
			public void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks) {
				first.progress(stage, fullChunks, totalChunks);
				second.progress(stage, fullChunks, totalChunks);
			}

			@Override
			public void finish(ChunkLoadProgress.Stage stage) {
				first.finish(stage);
				second.finish(stage);
			}

			@Override
			public void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk) {
				first.initSpawnPos(worldKey, spawnChunk);
				second.initSpawnPos(worldKey, spawnChunk);
			}
		};
	}

	void init(ChunkLoadProgress.Stage stage, int chunks);

	void progress(ChunkLoadProgress.Stage stage, int fullChunks, int totalChunks);

	void finish(ChunkLoadProgress.Stage stage);

	void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk);

	/**
	 * {@code Stage}.
	 */
	public static enum Stage {
		START_SERVER,
		PREPARE_GLOBAL_SPAWN,
		LOAD_INITIAL_CHUNKS,
		LOAD_PLAYER_CHUNKS;
	}
}
