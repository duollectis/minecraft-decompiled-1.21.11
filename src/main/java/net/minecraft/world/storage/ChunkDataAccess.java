package net.minecraft.world.storage;

import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Контракт асинхронного доступа к данным чанка для конкретного типа объектов {@code T}.
 * Реализуется, например, {@link EntityChunkDataAccess} для сущностей.
 */
public interface ChunkDataAccess<T> extends AutoCloseable {

	CompletableFuture<ChunkDataList<T>> readChunkData(ChunkPos pos);

	void writeChunkData(ChunkDataList<T> dataList);

	void awaitAll(boolean sync);

	@Override
	default void close() throws IOException {
	}
}
