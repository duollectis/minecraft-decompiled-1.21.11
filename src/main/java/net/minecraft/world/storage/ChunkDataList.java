package net.minecraft.world.storage;

import net.minecraft.util.math.ChunkPos;

import java.util.List;
import java.util.stream.Stream;

/**
 * Контейнер для списка объектов типа {@code T}, привязанных к конкретному чанку.
 * Используется при чтении и записи данных чанка через {@link ChunkDataAccess}.
 */
public class ChunkDataList<T> {

	private final ChunkPos pos;
	private final List<T> backingList;

	public ChunkDataList(ChunkPos pos, List<T> list) {
		this.pos = pos;
		this.backingList = list;
	}

	public ChunkPos getChunkPos() {
		return pos;
	}

	public Stream<T> stream() {
		return backingList.stream();
	}

	public boolean isEmpty() {
		return backingList.isEmpty();
	}
}
