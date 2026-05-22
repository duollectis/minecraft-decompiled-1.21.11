package net.minecraft.world.chunk.light;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Базовый интерфейс системы освещения.
 * Определяет контракт для проверки блоков, обновления уровней света
 * и управления состоянием секций и колонок чанков.
 */
public interface LightingView {

	void checkBlock(BlockPos pos);

	boolean hasUpdates();

	int doLightUpdates();

	default void setSectionStatus(BlockPos pos, boolean notReady) {
		setSectionStatus(ChunkSectionPos.from(pos), notReady);
	}

	void setSectionStatus(ChunkSectionPos pos, boolean notReady);

	void setColumnEnabled(ChunkPos pos, boolean retainData);

	void propagateLight(ChunkPos chunkPos);
}
