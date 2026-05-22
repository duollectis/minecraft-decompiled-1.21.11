package net.minecraft.world.biome.source;

/**
 * Утилитарный класс для конвертации между блочными, биомными и чанковыми координатами.
 * Биомные координаты — это блочные координаты, сдвинутые вправо на 2 бита (делённые на 4).
 * Один биом-сэмпл соответствует квадрату 4×4 блока.
 */
public final class BiomeCoords {

	/** Количество бит сдвига для перевода блочных координат в биомные (log2(4) = 2). */
	public static final int BIOME_SECTION_BITS = 2;

	/** Размер одной биомной ячейки в блоках (2^BIOME_SECTION_BITS = 4). */
	public static final int BIOME_SECTION_SIZE = 4;

	/** Количество бит сдвига для шумовых биомных секций (log2(8) = 3). */
	public static final int NOISE_BIOME_SECTION_BITS = 3;

	private static final int BIOME_COORD_BITS = 2;

	private BiomeCoords() {
	}

	/** Переводит блочную координату в биомную (деление на 4 через сдвиг). */
	public static int fromBlock(int blockCoord) {
		return blockCoord >> BIOME_COORD_BITS;
	}

	/** Возвращает локальную координату внутри биомной ячейки (остаток от деления на 4). */
	public static int getLocalCoord(int biomeCoord) {
		return biomeCoord & (BIOME_SECTION_SIZE - 1);
	}

	/** Переводит биомную координату в блочную (умножение на 4 через сдвиг). */
	public static int toBlock(int biomeCoord) {
		return biomeCoord << BIOME_COORD_BITS;
	}

	/** Переводит координату чанка в биомную (умножение на 4 через сдвиг). */
	public static int fromChunk(int chunkCoord) {
		return chunkCoord << BIOME_COORD_BITS;
	}

	/** Переводит биомную координату в координату чанка (деление на 4 через сдвиг). */
	public static int toChunk(int biomeCoord) {
		return biomeCoord >> BIOME_COORD_BITS;
	}
}
