package net.minecraft.world.chunk;

import java.util.concurrent.CompletableFuture;
import net.minecraft.util.collection.BoundedRegionArray;

@FunctionalInterface
public interface GenerationTask {
   CompletableFuture<Chunk> doWork(
      ChunkGenerationContext context, ChunkGenerationStep step, BoundedRegionArray<AbstractChunkHolder> boundedRegionArray, Chunk chunk
   );
}
