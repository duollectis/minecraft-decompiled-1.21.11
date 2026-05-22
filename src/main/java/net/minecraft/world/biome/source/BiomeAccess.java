package net.minecraft.world.biome.source;

import com.google.common.hash.Hashing;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

/**
 * Предоставляет доступ к биомам с учётом шума Вороного для сглаживания границ.
 * Использует алгоритм взвешенного расстояния по 8 угловым точкам биом-ячейки.
 */
public class BiomeAccess {

	public static final int CHUNK_CENTER_OFFSET = BiomeCoords.fromBlock(8);

	private static final int BIOME_COORD_OFFSET = 2;
	private static final int BIOME_COORD_SCALE = 4;
	private static final int BIOME_COORD_MASK = 3;

	// Нормализующий делитель для перевода битов сида в диапазон [-0.45, 0.45]
	private static final double SEED_NORMALIZE_DIVISOR = 1024.0;
	private static final double SEED_NORMALIZE_SCALE = 0.9;
	private static final double SEED_NORMALIZE_OFFSET = 0.5;

	private final BiomeAccess.Storage storage;
	private final long seed;

	public BiomeAccess(BiomeAccess.Storage storage, long seed) {
		this.storage = storage;
		this.seed = seed;
	}

	/**
	 * Хэширует сид мира через SHA-256 для получения детерминированного,
	 * но непредсказуемого значения, используемого при размещении биомов.
	 */
	public static long hashSeed(long seed) {
		return Hashing.sha256().hashLong(seed).asLong();
	}

	public BiomeAccess withSource(BiomeAccess.Storage storage) {
		return new BiomeAccess(storage, seed);
	}

	/**
	 * Возвращает биом для заданной блочной позиции с применением шума Вороного.
	 * Алгоритм перебирает 8 угловых точек биом-ячейки и выбирает ближайшую
	 * с учётом псевдослучайного смещения, зависящего от сида.
	 */
	public RegistryEntry<Biome> getBiome(BlockPos pos) {
		int shiftedX = pos.getX() - BIOME_COORD_OFFSET;
		int shiftedY = pos.getY() - BIOME_COORD_OFFSET;
		int shiftedZ = pos.getZ() - BIOME_COORD_OFFSET;
		int biomeX = shiftedX >> 2;
		int biomeY = shiftedY >> 2;
		int biomeZ = shiftedZ >> 2;
		double fracX = (shiftedX & BIOME_COORD_MASK) / (double) BIOME_COORD_SCALE;
		double fracY = (shiftedY & BIOME_COORD_MASK) / (double) BIOME_COORD_SCALE;
		double fracZ = (shiftedZ & BIOME_COORD_MASK) / (double) BIOME_COORD_SCALE;

		int bestCorner = 0;
		double minDistance = Double.POSITIVE_INFINITY;

		for (int corner = 0; corner < 8; corner++) {
			boolean useNearX = (corner & 4) == 0;
			boolean useNearY = (corner & 2) == 0;
			boolean useNearZ = (corner & 1) == 0;
			int cornerX = useNearX ? biomeX : biomeX + 1;
			int cornerY = useNearY ? biomeY : biomeY + 1;
			int cornerZ = useNearZ ? biomeZ : biomeZ + 1;
			double offsetX = useNearX ? fracX : fracX - 1.0;
			double offsetY = useNearY ? fracY : fracY - 1.0;
			double offsetZ = useNearZ ? fracZ : fracZ - 1.0;
			double distance = computeWeightedDistance(seed, cornerX, cornerY, cornerZ, offsetX, offsetY, offsetZ);

			if (minDistance > distance) {
				bestCorner = corner;
				minDistance = distance;
			}
		}

		int resultX = (bestCorner & 4) == 0 ? biomeX : biomeX + 1;
		int resultY = (bestCorner & 2) == 0 ? biomeY : biomeY + 1;
		int resultZ = (bestCorner & 1) == 0 ? biomeZ : biomeZ + 1;
		return storage.getBiomeForNoiseGen(resultX, resultY, resultZ);
	}

	public RegistryEntry<Biome> getBiomeForNoiseGen(double x, double y, double z) {
		int biomeX = BiomeCoords.fromBlock(MathHelper.floor(x));
		int biomeY = BiomeCoords.fromBlock(MathHelper.floor(y));
		int biomeZ = BiomeCoords.fromBlock(MathHelper.floor(z));
		return getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
	}

	public RegistryEntry<Biome> getBiomeForNoiseGen(BlockPos pos) {
		int biomeX = BiomeCoords.fromBlock(pos.getX());
		int biomeY = BiomeCoords.fromBlock(pos.getY());
		int biomeZ = BiomeCoords.fromBlock(pos.getZ());
		return getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
	}

	public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		return storage.getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
	}

	/**
	 * Вычисляет взвешенное расстояние от дробной позиции до угловой точки биом-ячейки
	 * с псевдослучайным смещением, зависящим от координат и сида мира.
	 * Смешивание сида выполняется дважды по каждой оси для лучшего распределения.
	 */
	private static double computeWeightedDistance(
		long seed,
		int cornerX,
		int cornerY,
		int cornerZ,
		double offsetX,
		double offsetY,
		double offsetZ
	) {
		long mixed = SeedMixer.mixSeed(seed, cornerX);
		mixed = SeedMixer.mixSeed(mixed, cornerY);
		mixed = SeedMixer.mixSeed(mixed, cornerZ);
		mixed = SeedMixer.mixSeed(mixed, cornerX);
		mixed = SeedMixer.mixSeed(mixed, cornerY);
		mixed = SeedMixer.mixSeed(mixed, cornerZ);
		double noiseX = normalizeFromSeed(mixed);
		mixed = SeedMixer.mixSeed(mixed, seed);
		double noiseY = normalizeFromSeed(mixed);
		mixed = SeedMixer.mixSeed(mixed, seed);
		double noiseZ = normalizeFromSeed(mixed);
		return MathHelper.square(offsetZ + noiseZ)
			+ MathHelper.square(offsetY + noiseY)
			+ MathHelper.square(offsetX + noiseX);
	}

	/**
	 * Нормализует значение сида в диапазон [-0.45, 0.45].
	 * Используется для создания псевдослучайного смещения угловых точек Вороного.
	 */
	private static double normalizeFromSeed(long seed) {
		double normalized = Math.floorMod(seed >> 24, 1024) / SEED_NORMALIZE_DIVISOR;
		return (normalized - SEED_NORMALIZE_OFFSET) * SEED_NORMALIZE_SCALE;
	}

	/**
	 * Источник данных о биомах в координатах шума (биом-координатах).
	 */
	public interface Storage {

		RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ);
	}
}
