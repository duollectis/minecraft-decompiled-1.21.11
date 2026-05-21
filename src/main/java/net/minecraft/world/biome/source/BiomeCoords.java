package net.minecraft.world.biome.source;

/**
 * {@code BiomeCoords}.
 */
public final class BiomeCoords {

	public static final int BIOME_SECTION_BITS = 2;
	public static final int BIOME_SECTION_SIZE = 4;
	public static final int NOISE_BIOME_SECTION_BITS = 3;
	private static final int BIOME_COORD_BITS = 2;

	private BiomeCoords() {
	}

	/**
	 * From block.
	 *
	 * @param blockCoord block coord
	 *
	 * @return int — результат операции
	 */
	public static int fromBlock(int blockCoord) {
		return blockCoord >> 2;
	}

	public static int getLocalCoord(int i) {
		return i & 3;
	}

	/**
	 * To block.
	 *
	 * @param biomeCoord biome coord
	 *
	 * @return int — результат операции
	 */
	public static int toBlock(int biomeCoord) {
		return biomeCoord << 2;
	}

	/**
	 * From chunk.
	 *
	 * @param chunkCoord chunk coord
	 *
	 * @return int — результат операции
	 */
	public static int fromChunk(int chunkCoord) {
		return chunkCoord << 2;
	}

	/**
	 * To chunk.
	 *
	 * @param biomeCoord biome coord
	 *
	 * @return int — результат операции
	 */
	public static int toChunk(int biomeCoord) {
		return biomeCoord >> 2;
	}
}
