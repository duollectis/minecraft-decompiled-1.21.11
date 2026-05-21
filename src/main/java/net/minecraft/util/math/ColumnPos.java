package net.minecraft.util.math;

/**
 * {@code ColumnPos}.
 */
public record ColumnPos(int x, int z) {

	private static final long COLUMN_COORD_BITS = 32L;
	private static final long COLUMN_COORD_MASK = 4294967295L;

	public ChunkPos toChunkPos() {
		return new ChunkPos(ChunkSectionPos.getSectionCoord(this.x), ChunkSectionPos.getSectionCoord(this.z));
	}

	public long pack() {
		return pack(this.x, this.z);
	}

	public static long pack(int x, int z) {
		return x & 4294967295L | (z & 4294967295L) << 32;
	}

	public static int getX(long packed) {
		return (int) (packed & 4294967295L);
	}

	public static int getZ(long packed) {
		return (int) (packed >>> 32 & 4294967295L);
	}

	@Override
	public String toString() {
		return "[" + this.x + ", " + this.z + "]";
	}

	@Override
	public int hashCode() {
		return ChunkPos.hashCode(this.x, this.z);
	}
}
