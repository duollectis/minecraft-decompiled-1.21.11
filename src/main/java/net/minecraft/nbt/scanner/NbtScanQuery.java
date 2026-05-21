package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

import java.util.List;

/**
 * {@code NbtScanQuery}.
 */
public record NbtScanQuery(List<String> path, NbtType<?> type, String key) {

	public NbtScanQuery(NbtType<?> type, String key) {
		this(List.of(), type, key);
	}

	public NbtScanQuery(String path, NbtType<?> type, String key) {
		this(List.of(path), type, key);
	}

	public NbtScanQuery(String path1, String path2, NbtType<?> type, String key) {
		this(List.of(path1, path2), type, key);
	}
}
