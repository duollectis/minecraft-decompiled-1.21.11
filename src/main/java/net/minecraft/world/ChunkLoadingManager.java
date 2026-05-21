package net.minecraft.world;

import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;

import java.util.concurrent.CompletableFuture;

/**
 * {@code ChunkLoadingManager}.
 */
public interface ChunkLoadingManager {

	AbstractChunkHolder acquire(long pos);

	void release(AbstractChunkHolder chunkHolder);

	CompletableFuture<Chunk> generate(
			AbstractChunkHolder chunkHolder,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks
	);

	ChunkLoader createLoader(ChunkStatus requestedStatus, ChunkPos pos);

	void updateChunks();
}
