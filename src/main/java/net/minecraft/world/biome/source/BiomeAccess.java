package net.minecraft.world.biome.source;

import com.google.common.hash.Hashing;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

/**
 * {@code BiomeAccess}.
 */
public class BiomeAccess {

	public static final int CHUNK_CENTER_OFFSET = BiomeCoords.fromBlock(8);
	private static final int BIOME_COORD_OFFSET = 2;
	private static final int BIOME_COORD_SCALE = 4;
	private static final int BIOME_COORD_MASK = 3;
	private final BiomeAccess.Storage storage;
	private final long seed;

	public BiomeAccess(BiomeAccess.Storage storage, long seed) {
		this.storage = storage;
		this.seed = seed;
	}

	/**
	 * Проверяет наличие h seed.
	 *
	 * @param seed seed
	 *
	 * @return long — {@code true} если условие выполнено
	 */
	public static long hashSeed(long seed) {
		return Hashing.sha256().hashLong(seed).asLong();
	}

	/**
	 * With source.
	 *
	 * @param storage storage
	 *
	 * @return BiomeAccess — результат операции
	 */
	public BiomeAccess withSource(BiomeAccess.Storage storage) {
		return new BiomeAccess(storage, this.seed);
	}

	public RegistryEntry<Biome> getBiome(BlockPos pos) {
		int i = pos.getX() - 2;
		int j = pos.getY() - 2;
		int k = pos.getZ() - 2;
		int l = i >> 2;
		int m = j >> 2;
		int n = k >> 2;
		double d = (i & 3) / 4.0;
		double e = (j & 3) / 4.0;
		double f = (k & 3) / 4.0;
		int o = 0;
		double g = Double.POSITIVE_INFINITY;

		for (int p = 0; p < 8; p++) {
			boolean bl = (p & 4) == 0;
			boolean bl2 = (p & 2) == 0;
			boolean bl3 = (p & 1) == 0;
			int q = bl ? l : l + 1;
			int r = bl2 ? m : m + 1;
			int s = bl3 ? n : n + 1;
			double h = bl ? d : d - 1.0;
			double t = bl2 ? e : e - 1.0;
			double u = bl3 ? f : f - 1.0;
			double v = computeWeightedDistance(this.seed, q, r, s, h, t, u);
			if (g > v) {
				o = p;
				g = v;
			}
		}

		int px = (o & 4) == 0 ? l : l + 1;
		int w = (o & 2) == 0 ? m : m + 1;
		int x = (o & 1) == 0 ? n : n + 1;
		return this.storage.getBiomeForNoiseGen(px, w, x);
	}

	public RegistryEntry<Biome> getBiomeForNoiseGen(double x, double y, double z) {
		int i = BiomeCoords.fromBlock(MathHelper.floor(x));
		int j = BiomeCoords.fromBlock(MathHelper.floor(y));
		int k = BiomeCoords.fromBlock(MathHelper.floor(z));
		return this.getBiomeForNoiseGen(i, j, k);
	}

	public RegistryEntry<Biome> getBiomeForNoiseGen(BlockPos pos) {
		int i = BiomeCoords.fromBlock(pos.getX());
		int j = BiomeCoords.fromBlock(pos.getY());
		int k = BiomeCoords.fromBlock(pos.getZ());
		return this.getBiomeForNoiseGen(i, j, k);
	}

	public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		return this.storage.getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
	}

	private static double computeWeightedDistance(long l, int i, int j, int k, double d, double e, double f) {
		long m = SeedMixer.mixSeed(l, i);
		m = SeedMixer.mixSeed(m, j);
		m = SeedMixer.mixSeed(m, k);
		m = SeedMixer.mixSeed(m, i);
		m = SeedMixer.mixSeed(m, j);
		m = SeedMixer.mixSeed(m, k);
		double g = normalizeFromSeed(m);
		m = SeedMixer.mixSeed(m, l);
		double h = normalizeFromSeed(m);
		m = SeedMixer.mixSeed(m, l);
		double n = normalizeFromSeed(m);
		return MathHelper.square(f + n) + MathHelper.square(e + h) + MathHelper.square(d + g);
	}

	private static double normalizeFromSeed(long l) {
		double d = Math.floorMod(l >> 24, 1024) / 1024.0;
		return (d - 0.5) * 0.9;
	}

	/**
	 * {@code Storage}.
	 */
	public interface Storage {

		RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ);
	}
}
