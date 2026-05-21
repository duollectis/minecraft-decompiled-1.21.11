package net.minecraft.world.chunk;

import net.minecraft.util.collection.BoundedRegionArray;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
/**
 * {@code GenerationTask}.
 */
public interface GenerationTask {

	CompletableFuture<Chunk> doWork(
			ChunkGenerationContext context,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> boundedRegionArray,
			Chunk chunk
	);
}
