package net.minecraft.util.math;

/**
 * Двумерная позиция колонки блоков (X, Z) без учёта высоты.
 * Поддерживает упаковку в {@code long} для эффективного хранения в хэш-множествах.
 */
public record ColumnPos(int x, int z) {

	private static final long COLUMN_COORD_BITS = 32L;
	private static final long COLUMN_COORD_MASK = 4294967295L;

	public ChunkPos toChunkPos() {
		return new ChunkPos(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
	}

	public long pack() {
		return pack(x, z);
	}

	public static long pack(int x, int z) {
		return x & COLUMN_COORD_MASK | (z & COLUMN_COORD_MASK) << COLUMN_COORD_BITS;
	}

	public static int getX(long packed) {
		return (int) (packed & COLUMN_COORD_MASK);
	}

	public static int getZ(long packed) {
		return (int) (packed >>> COLUMN_COORD_BITS & COLUMN_COORD_MASK);
	}

	@Override
	public String toString() {
		return "[" + x + ", " + z + "]";
	}

	@Override
	public int hashCode() {
		return ChunkPos.hashCode(x, z);
	}
}
