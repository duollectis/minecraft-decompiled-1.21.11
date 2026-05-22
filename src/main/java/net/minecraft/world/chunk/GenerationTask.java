package net.minecraft.world.chunk;

import net.minecraft.util.collection.BoundedRegionArray;

import java.util.concurrent.CompletableFuture;

/**
 * Функциональный интерфейс одного шага генерации чанка.
 * Реализации находятся в {@link ChunkGenerating}.
 */
@FunctionalInterface
public interface GenerationTask {

	CompletableFuture<Chunk> doWork(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	);
}
