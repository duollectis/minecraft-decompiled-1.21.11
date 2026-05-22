package net.minecraft.world.chunk;

import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.ChunkPos;

/**
 * Слушатель изменений уровня загрузки чанка.
 */
@FunctionalInterface
public interface ChunkStatusChangeListener {

	void onChunkStatusChange(ChunkPos pos, ChunkLevelType levelType);
}
