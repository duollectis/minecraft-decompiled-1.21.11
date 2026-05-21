package net.minecraft.world.storage;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.CompletableFuture;

/**
 * {@code NbtScannable}.
 */
public interface NbtScannable {

	CompletableFuture<Void> scanChunk(ChunkPos pos, NbtScanner scanner);
}
