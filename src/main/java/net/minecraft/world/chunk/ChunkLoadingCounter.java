package net.minecraft.world.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ChunkLoadingCounter}.
 */
public class ChunkLoadingCounter {

	private final List<ChunkHolder> nonFullChunks = new ArrayList<>();
	private int totalChunks;

	/**
	 * Load.
	 *
	 * @param world world
	 * @param runnable runnable
	 */
	public void load(ServerWorld world, Runnable runnable) {
		ServerChunkManager serverChunkManager = world.getChunkManager();
		LongSet longSet = new LongOpenHashSet();
		serverChunkManager.updateChunks();
		serverChunkManager.chunkLoadingManager
				.getChunkHolders(ChunkStatus.FULL)
				.forEach(holder -> longSet.add(holder.getPos().toLong()));
		runnable.run();
		serverChunkManager.updateChunks();
		serverChunkManager.chunkLoadingManager.getChunkHolders(ChunkStatus.FULL).forEach(holder -> {
			if (!longSet.contains(holder.getPos().toLong())) {
				this.nonFullChunks.add(holder);
				this.totalChunks++;
			}
		});
	}

	public int getFullChunks() {
		return this.totalChunks - this.getNonFullChunks();
	}

	public int getNonFullChunks() {
		this.nonFullChunks.removeIf(holder -> holder.getLatestStatus() == ChunkStatus.FULL);
		return this.nonFullChunks.size();
	}

	public int getTotalChunks() {
		return this.totalChunks;
	}
}
