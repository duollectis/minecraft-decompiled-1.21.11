package net.minecraft.world.storage;

import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * {@code ChunkDataAccess}.
 */
public interface ChunkDataAccess<T> extends AutoCloseable {

	CompletableFuture<ChunkDataList<T>> readChunkData(ChunkPos pos);

	void writeChunkData(ChunkDataList<T> dataList);

	void awaitAll(boolean sync);

	@Override
	default void close() throws IOException {
	}
}
