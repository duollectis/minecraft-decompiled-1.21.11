package net.minecraft.util.math.random;

import java.util.function.LongFunction;

/**
 * {@code ChunkRandom}.
 */
public class ChunkRandom extends CheckedRandom {

	private final Random baseRandom;
	private int sampleCount;

	public ChunkRandom(Random baseRandom) {
		super(0L);
		this.baseRandom = baseRandom;
	}

	public int getSampleCount() {
		return this.sampleCount;
	}

	@Override
	public Random split() {
		return this.baseRandom.split();
	}

	@Override
	public RandomSplitter nextSplitter() {
		return this.baseRandom.nextSplitter();
	}

	@Override
	public int next(int bits) {
		this.sampleCount++;
		return this.baseRandom instanceof CheckedRandom checkedRandom ? checkedRandom.next(bits)
		                                                              : (int) (this.baseRandom.nextLong() >>> 64 - bits
		                                                              );
	}

	@Override
	public synchronized void setSeed(long seed) {
		if (this.baseRandom != null) {
			this.baseRandom.setSeed(seed);
		}
	}

	public long setPopulationSeed(long worldSeed, int blockX, int blockZ) {
		this.setSeed(worldSeed);
		long l = this.nextLong() | 1L;
		long m = this.nextLong() | 1L;
		long n = blockX * l + blockZ * m ^ worldSeed;
		this.setSeed(n);
		return n;
	}

	public void setDecoratorSeed(long populationSeed, int index, int step) {
		long l = populationSeed + index + 10000 * step;
		this.setSeed(l);
	}

	public void setCarverSeed(long worldSeed, int chunkX, int chunkZ) {
		this.setSeed(worldSeed);
		long l = this.nextLong();
		long m = this.nextLong();
		long n = chunkX * l ^ chunkZ * m ^ worldSeed;
		this.setSeed(n);
	}

	public void setRegionSeed(long worldSeed, int regionX, int regionZ, int salt) {
		long l = regionX * 341873128712L + regionZ * 132897987541L + worldSeed + salt;
		this.setSeed(l);
	}

	public static Random getSlimeRandom(int chunkX, int chunkZ, long worldSeed, long scrambler) {
		return Random.create(
				worldSeed + chunkX * chunkX * 4987142 + chunkX * 5947611 + chunkZ * chunkZ * 4392871L + chunkZ * 389711
						^ scrambler);
	}

	/**
	 * {@code RandomProvider}.
	 */
	public static enum RandomProvider {
		LEGACY(CheckedRandom::new),
		XOROSHIRO(Xoroshiro128PlusPlusRandom::new);

		private final LongFunction<Random> provider;

		private RandomProvider(final LongFunction<Random> provider) {
			this.provider = provider;
		}

		/**
		 * Create.
		 *
		 * @param seed seed
		 *
		 * @return Random — результат операции
		 */
		public Random create(long seed) {
			return this.provider.apply(seed);
		}
	}
}
