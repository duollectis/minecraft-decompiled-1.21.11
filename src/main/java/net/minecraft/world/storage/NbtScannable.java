package net.minecraft.world.storage;

import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.CompletableFuture;

/**
 * Контракт для асинхронного сканирования NBT-данных чанка без полной десериализации.
 * Используется для быстрого чтения отдельных полей (например, DataVersion) без загрузки всего чанка.
 */
public interface NbtScannable {

	CompletableFuture<Void> scanChunk(ChunkPos pos, NbtScanner scanner);
}
