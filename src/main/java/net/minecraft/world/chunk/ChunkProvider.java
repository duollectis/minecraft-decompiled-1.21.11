package net.minecraft.world.chunk;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightSourceView;
import org.jspecify.annotations.Nullable;

/**
 * Поставщик чанков для системы освещения. Предоставляет доступ к чанкам
 * по координатам и уведомляет об изменениях освещения в секциях.
 */
public interface ChunkProvider {

	@Nullable LightSourceView getChunk(int chunkX, int chunkZ);

	default void onLightUpdate(LightType type, ChunkSectionPos pos) {
	}

	BlockView getWorld();
}
