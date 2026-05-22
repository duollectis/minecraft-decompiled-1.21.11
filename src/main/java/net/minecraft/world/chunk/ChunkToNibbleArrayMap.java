package net.minecraft.world.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Абстрактная карта позиций секций чанков → {@link ChunkNibbleArray} с двухэлементным
 * LRU-кешем для ускорения повторных обращений к одним и тем же позициям.
 *
 * @param <M> конкретный подтип карты (для типобезопасного {@link #copy()})
 */
public abstract class ChunkToNibbleArrayMap<M extends ChunkToNibbleArrayMap<M>> {

	private static final int CACHE_SIZE = 2;

	private final long[] cachePositions = new long[CACHE_SIZE];
	private final @Nullable ChunkNibbleArray[] cacheArrays = new ChunkNibbleArray[CACHE_SIZE];
	private boolean cacheEnabled;
	protected final Long2ObjectOpenHashMap<ChunkNibbleArray> arrays;

	protected ChunkToNibbleArrayMap(Long2ObjectOpenHashMap<ChunkNibbleArray> arrays) {
		this.arrays = arrays;
		clearCache();
		cacheEnabled = true;
	}

	public abstract M copy();

	/**
	 * Заменяет массив по позиции его глубокой копией и инвалидирует кеш.
	 * Используется перед мутацией данных освещения в рамках одного тика.
	 */
	public ChunkNibbleArray replaceWithCopy(long pos) {
		ChunkNibbleArray copied = ((ChunkNibbleArray) arrays.get(pos)).copy();
		arrays.put(pos, copied);
		clearCache();
		return copied;
	}

	public boolean containsKey(long chunkPos) {
		return arrays.containsKey(chunkPos);
	}

	/**
	 * Возвращает {@link ChunkNibbleArray} для указанной позиции секции,
	 * используя двухэлементный LRU-кеш для ускорения повторных обращений.
	 */
	public @Nullable ChunkNibbleArray get(long chunkPos) {
		if (cacheEnabled) {
			for (int i = 0; i < CACHE_SIZE; i++) {
				if (chunkPos == cachePositions[i]) {
					return cacheArrays[i];
				}
			}
		}

		ChunkNibbleArray array = (ChunkNibbleArray) arrays.get(chunkPos);
		if (array == null) {
			return null;
		}

		if (cacheEnabled) {
			// Сдвигаем кеш: новый элемент занимает позицию 0
			for (int i = CACHE_SIZE - 1; i > 0; i--) {
				cachePositions[i] = cachePositions[i - 1];
				cacheArrays[i] = cacheArrays[i - 1];
			}

			cachePositions[0] = chunkPos;
			cacheArrays[0] = array;
		}

		return array;
	}

	public @Nullable ChunkNibbleArray removeChunk(long chunkPos) {
		return (ChunkNibbleArray) arrays.remove(chunkPos);
	}

	public void put(long pos, ChunkNibbleArray data) {
		arrays.put(pos, data);
	}

	public void clearCache() {
		for (int i = 0; i < CACHE_SIZE; i++) {
			cachePositions[i] = Long.MAX_VALUE;
			cacheArrays[i] = null;
		}
	}

	public void disableCache() {
		cacheEnabled = false;
	}
}
