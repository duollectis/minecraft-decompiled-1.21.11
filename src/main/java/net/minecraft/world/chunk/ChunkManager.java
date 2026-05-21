package net.minecraft.world.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.function.BooleanSupplier;

/**
 * {@code ChunkManager}.
 */
public abstract class ChunkManager implements ChunkProvider, AutoCloseable {

	public @Nullable WorldChunk getWorldChunk(int chunkX, int chunkZ, boolean create) {
		return (WorldChunk) this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, create);
	}

	public @Nullable WorldChunk getWorldChunk(int chunkX, int chunkZ) {
		return this.getWorldChunk(chunkX, chunkZ, false);
	}

	@Override
	public @Nullable LightSourceView getChunk(int chunkX, int chunkZ) {
		return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
	}

	public boolean isChunkLoaded(int x, int z) {
		return this.getChunk(x, z, ChunkStatus.FULL, false) != null;
	}

	public abstract @Nullable Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create);

	public abstract void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks);

	public void onSectionStatusChanged(int x, int sectionY, int z, boolean previouslyEmpty) {
	}

	public abstract String getDebugString();

	public abstract int getLoadedChunkCount();

	@Override
	public void close() throws IOException {
	}

	public abstract LightingProvider getLightingProvider();

	public void setMobSpawnOptions(boolean spawnMonsters) {
	}

	public boolean setChunkForced(ChunkPos pos, boolean forced) {
		return false;
	}

	public LongSet getForcedChunks() {
		return LongSet.of();
	}
}
