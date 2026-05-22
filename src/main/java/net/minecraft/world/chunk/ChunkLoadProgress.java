package net.minecraft.world.chunk;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * Слушатель прогресса загрузки чанков при старте сервера.
 * Позволяет отслеживать инициализацию, прогресс и завершение каждого этапа загрузки.
 */
public interface ChunkLoadProgress {

	/**
	 * Создаёт составной слушатель, делегирующий все вызовы двум переданным слушателям.
	 */
	static ChunkLoadProgress compose(ChunkLoadProgress first, ChunkLoadProgress second) {
		return new ChunkLoadProgress() {
			@Override
			public void init(Stage stage, int chunks) {
				first.init(stage, chunks);
				second.init(stage, chunks);
			}

			@Override
			public void progress(Stage stage, int fullChunks, int totalChunks) {
				first.progress(stage, fullChunks, totalChunks);
				second.progress(stage, fullChunks, totalChunks);
			}

			@Override
			public void finish(Stage stage) {
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

	void init(Stage stage, int chunks);

	void progress(Stage stage, int fullChunks, int totalChunks);

	void finish(Stage stage);

	void initSpawnPos(RegistryKey<World> worldKey, ChunkPos spawnChunk);

	enum Stage {
		START_SERVER,
		PREPARE_GLOBAL_SPAWN,
		LOAD_INITIAL_CHUNKS,
		LOAD_PLAYER_CHUNKS;
	}
}
