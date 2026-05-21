package net.minecraft.world.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

/**
 * {@code ChunkToNibbleArrayMap}.
 */
public abstract class ChunkToNibbleArrayMap<M extends ChunkToNibbleArrayMap<M>> {

	private static final int COPY_THRESHOLD = 2;
	private final long[] cachePositions = new long[2];
	private final @Nullable ChunkNibbleArray[] cacheArrays = new ChunkNibbleArray[2];
	private boolean cacheEnabled;
	protected final Long2ObjectOpenHashMap<ChunkNibbleArray> arrays;

	protected ChunkToNibbleArrayMap(Long2ObjectOpenHashMap<ChunkNibbleArray> arrays) {
		this.arrays = arrays;
		this.clearCache();
		this.cacheEnabled = true;
	}

	/**
	 * Copy.
	 *
	 * @return M — результат операции
	 */
	public abstract M copy();

	/**
	 * Replace with copy.
	 *
	 * @param pos pos
	 *
	 * @return ChunkNibbleArray — результат операции
	 */
	public ChunkNibbleArray replaceWithCopy(long pos) {
		ChunkNibbleArray chunkNibbleArray = ((ChunkNibbleArray) this.arrays.get(pos)).copy();
		this.arrays.put(pos, chunkNibbleArray);
		this.clearCache();
		return chunkNibbleArray;
	}

	/**
	 * Contains key.
	 *
	 * @param chunkPos chunk pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean containsKey(long chunkPos) {
		return this.arrays.containsKey(chunkPos);
	}

	/**
	 * Get.
	 *
	 * @param chunkPos chunk pos
	 *
	 * @return @Nullable ChunkNibbleArray — 
	 */
	public @Nullable ChunkNibbleArray get(long chunkPos) {
		if (this.cacheEnabled) {
			for (int i = 0; i < 2; i++) {
				if (chunkPos == this.cachePositions[i]) {
					return this.cacheArrays[i];
				}
			}
		}

		ChunkNibbleArray chunkNibbleArray = (ChunkNibbleArray) this.arrays.get(chunkPos);
		if (chunkNibbleArray == null) {
			return null;
		}
		else {
			if (this.cacheEnabled) {
				for (int j = 1; j > 0; j--) {
					this.cachePositions[j] = this.cachePositions[j - 1];
					this.cacheArrays[j] = this.cacheArrays[j - 1];
				}

				this.cachePositions[0] = chunkPos;
				this.cacheArrays[0] = chunkNibbleArray;
			}

			return chunkNibbleArray;
		}
	}

	/**
	 * Удаляет chunk.
	 *
	 * @param chunkPos chunk pos
	 *
	 * @return @Nullable ChunkNibbleArray — результат операции
	 */
	public @Nullable ChunkNibbleArray removeChunk(long chunkPos) {
		return (ChunkNibbleArray) this.arrays.remove(chunkPos);
	}

	/**
	 * Put.
	 *
	 * @param pos pos
	 * @param data data
	 */
	public void put(long pos, ChunkNibbleArray data) {
		this.arrays.put(pos, data);
	}

	/**
	 * Очищает cache.
	 */
	public void clearCache() {
		for (int i = 0; i < 2; i++) {
			this.cachePositions[i] = Long.MAX_VALUE;
			this.cacheArrays[i] = null;
		}
	}

	/**
	 * Отключает cache.
	 */
	public void disableCache() {
		this.cacheEnabled = false;
	}
}
