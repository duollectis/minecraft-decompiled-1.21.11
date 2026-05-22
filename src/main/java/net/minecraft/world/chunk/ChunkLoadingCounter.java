package net.minecraft.world.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Отслеживает количество чанков, добавленных в мир после вызова {@link #load}.
 * Разделяет чанки на «полностью загруженные» (достигшие {@link ChunkStatus#FULL})
 * и «ещё загружающиеся».
 */
public class ChunkLoadingCounter {

	private final List<ChunkHolder> pendingChunks = new ArrayList<>();
	private int totalChunks;

	/**
	 * Запускает {@code runnable}, затем фиксирует все новые чанки, появившиеся
	 * в менеджере после его выполнения, для последующего отслеживания прогресса.
	 */
	public void load(ServerWorld world, Runnable runnable) {
		ServerChunkManager chunkManager = world.getChunkManager();
		LongSet existingFullChunks = new LongOpenHashSet();

		chunkManager.updateChunks();
		chunkManager.chunkLoadingManager
				.getChunkHolders(ChunkStatus.FULL)
				.forEach(holder -> existingFullChunks.add(holder.getPos().toLong()));

		runnable.run();

		chunkManager.updateChunks();
		chunkManager.chunkLoadingManager.getChunkHolders(ChunkStatus.FULL).forEach(holder -> {
			if (!existingFullChunks.contains(holder.getPos().toLong())) {
				pendingChunks.add(holder);
				totalChunks++;
			}
		});
	}

	public int getFullChunks() {
		return totalChunks - getNonFullChunks();
	}

	public int getNonFullChunks() {
		pendingChunks.removeIf(holder -> holder.getLatestStatus() == ChunkStatus.FULL);
		return pendingChunks.size();
	}

	public int getTotalChunks() {
		return totalChunks;
	}
}
