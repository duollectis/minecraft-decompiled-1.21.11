package net.minecraft.world.updater;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;

import java.util.function.Supplier;

@FunctionalInterface
/**
 * {@code ChunkUpdater}.
 */
public interface ChunkUpdater {

	Supplier<ChunkUpdater> PASSTHROUGH_FACTORY = () -> nbt -> nbt;

	NbtCompound applyFix(NbtCompound chunkNbt);

	default void markChunkDone(ChunkPos chunkPos) {
	}

	default int targetDataVersion() {
		return -1;
	}
}
