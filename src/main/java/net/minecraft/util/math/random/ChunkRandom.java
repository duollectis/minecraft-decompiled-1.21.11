package net.minecraft.util.math.random;

import java.util.function.LongFunction;

/**
 * Специализированный генератор случайных чисел для генерации чанков.
 * Оборачивает базовый {@link Random} и добавляет счётчик вызовов {@link #sampleCount},
 * а также набор методов для детерминированной инициализации семян по координатам чанка.
 */
public class ChunkRandom extends CheckedRandom {

	private final Random baseRandom;
	private int sampleCount;

	public ChunkRandom(Random baseRandom) {
		super(0L);
		this.baseRandom = baseRandom;
	}

	public int getSampleCount() {
		return sampleCount;
	}

	@Override
	public Random split() {
		return baseRandom.split();
	}

	@Override
	public RandomSplitter nextSplitter() {
		return baseRandom.nextSplitter();
	}

	@Override
	public int next(int bits) {
		sampleCount++;

		return baseRandom instanceof CheckedRandom checkedRandom
			? checkedRandom.next(bits)
			: (int) (baseRandom.nextLong() >>> 64 - bits);
	}

	@Override
	public synchronized void setSeed(long seed) {
		if (baseRandom != null) {
			baseRandom.setSeed(seed);
		}
	}

	/**
	 * Инициализирует генератор для заполнения чанка (population seed).
	 * Семя уникально для каждой пары (worldSeed, blockX, blockZ).
	 *
	 * @param worldSeed глобальное семя мира
	 * @param blockX координата X блока (обычно угол чанка)
	 * @param blockZ координата Z блока (обычно угол чанка)
	 * @return итоговое семя заполнения
	 */
	public long setPopulationSeed(long worldSeed, int blockX, int blockZ) {
		setSeed(worldSeed);
		long factorX = nextLong() | 1L;
		long factorZ = nextLong() | 1L;
		long populationSeed = blockX * factorX + blockZ * factorZ ^ worldSeed;
		setSeed(populationSeed);

		return populationSeed;
	}

	public void setDecoratorSeed(long populationSeed, int index, int step) {
		setSeed(populationSeed + index + 10000 * step);
	}

	public void setCarverSeed(long worldSeed, int chunkX, int chunkZ) {
		setSeed(worldSeed);
		long factorX = nextLong();
		long factorZ = nextLong();
		setSeed(chunkX * factorX ^ chunkZ * factorZ ^ worldSeed);
	}

	public void setRegionSeed(long worldSeed, int regionX, int regionZ, int salt) {
		setSeed(regionX * 341873128712L + regionZ * 132897987541L + worldSeed + salt);
	}

	public static Random getSlimeRandom(int chunkX, int chunkZ, long worldSeed, long scrambler) {
		return Random.create(
			worldSeed + chunkX * chunkX * 4987142 + chunkX * 5947611 + chunkZ * chunkZ * 4392871L + chunkZ * 389711
				^ scrambler
		);
	}

	public enum RandomProvider {
		LEGACY(CheckedRandom::new),
		XOROSHIRO(Xoroshiro128PlusPlusRandom::new);

		private final LongFunction<Random> provider;

		RandomProvider(final LongFunction<Random> provider) {
			this.provider = provider;
		}

		public Random create(long seed) {
			return provider.apply(seed);
		}
	}
}
