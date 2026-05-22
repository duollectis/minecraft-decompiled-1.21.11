package net.minecraft.util.math.random;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import net.minecraft.util.math.MathHelper;

/**
 * Высокоуровневая реализация {@link Random} на основе алгоритма Xoroshiro128++.
 * Делегирует генерацию битов в {@link Xoroshiro128PlusPlusRandomImpl} и предоставляет
 * полный набор методов для получения случайных значений различных типов.
 * Является предпочтительным генератором для большинства задач в Minecraft.
 */
public class Xoroshiro128PlusPlusRandom implements Random {

	private static final float FLOAT_MULTIPLIER = 5.9604645E-8F;
	private static final double DOUBLE_MULTIPLIER = 1.110223E-16F;

	public static final Codec<Xoroshiro128PlusPlusRandom> CODEC = Xoroshiro128PlusPlusRandomImpl.CODEC
			.xmap(Xoroshiro128PlusPlusRandom::new, random -> random.implementation);

	private Xoroshiro128PlusPlusRandomImpl implementation;
	private final GaussianGenerator gaussianGenerator = new GaussianGenerator(this);

	public Xoroshiro128PlusPlusRandom(long seed) {
		implementation = new Xoroshiro128PlusPlusRandomImpl(RandomSeed.createXoroshiroSeed(seed));
	}

	public Xoroshiro128PlusPlusRandom(RandomSeed.XoroshiroSeed seed) {
		implementation = new Xoroshiro128PlusPlusRandomImpl(seed);
	}

	public Xoroshiro128PlusPlusRandom(long seedLo, long seedHi) {
		implementation = new Xoroshiro128PlusPlusRandomImpl(seedLo, seedHi);
	}

	private Xoroshiro128PlusPlusRandom(Xoroshiro128PlusPlusRandomImpl implementation) {
		this.implementation = implementation;
	}

	@Override
	public Random split() {
		return new Xoroshiro128PlusPlusRandom(implementation.next(), implementation.next());
	}

	@Override
	public RandomSplitter nextSplitter() {
		return new Splitter(implementation.next(), implementation.next());
	}

	@Override
	public void setSeed(long seed) {
		implementation = new Xoroshiro128PlusPlusRandomImpl(RandomSeed.createXoroshiroSeed(seed));
		gaussianGenerator.reset();
	}

	@Override
	public int nextInt() {
		return (int) implementation.next();
	}

	/**
	 * Возвращает равномерно распределённое целое число в диапазоне [0, bound).
	 * Использует алгоритм Лемира (Lemire's fast bounded random) для устранения
	 * модульного смещения без деления.
	 */
	@Override
	public int nextInt(int bound) {
		if (bound <= 0) {
			throw new IllegalArgumentException("Bound must be positive");
		}

		long raw = Integer.toUnsignedLong(nextInt());
		long product = raw * bound;
		long remainder = product & 0xFFFFFFFFL;

		if (remainder < bound) {
			long threshold = Integer.remainderUnsigned(~bound + 1, bound);

			while (remainder < threshold) {
				raw = Integer.toUnsignedLong(nextInt());
				product = raw * bound;
				remainder = product & 0xFFFFFFFFL;
			}
		}

		return (int) (product >> 32);
	}

	@Override
	public long nextLong() {
		return implementation.next();
	}

	@Override
	public boolean nextBoolean() {
		return (implementation.next() & 1L) != 0L;
	}

	@Override
	public float nextFloat() {
		return (float) next(24) * FLOAT_MULTIPLIER;
	}

	@Override
	public double nextDouble() {
		return next(53) * DOUBLE_MULTIPLIER;
	}

	@Override
	public double nextGaussian() {
		return gaussianGenerator.next();
	}

	@Override
	public void skip(int count) {
		for (int i = 0; i < count; i++) {
			implementation.next();
		}
	}

	private long next(int bits) {
		return implementation.next() >>> 64 - bits;
	}

	/**
	 * Реализация {@link RandomSplitter} для алгоритма Xoroshiro128++.
	 * Порождает дочерние генераторы путём XOR-смешивания базового сида
	 * с хешем входного ключа.
	 */
	public static class Splitter implements RandomSplitter {

		private final long seedLo;
		private final long seedHi;

		public Splitter(long seedLo, long seedHi) {
			this.seedLo = seedLo;
			this.seedHi = seedHi;
		}

		@Override
		public Random split(int x, int y, int z) {
			long hash = MathHelper.hashCode(x, y, z);
			return new Xoroshiro128PlusPlusRandom(hash ^ seedLo, seedHi);
		}

		@Override
		public Random split(String seed) {
			RandomSeed.XoroshiroSeed xoroshiroSeed = RandomSeed.createXoroshiroSeed(seed);
			return new Xoroshiro128PlusPlusRandom(xoroshiroSeed.split(seedLo, seedHi));
		}

		@Override
		public Random split(long seed) {
			return new Xoroshiro128PlusPlusRandom(seed ^ seedLo, seed ^ seedHi);
		}

		@VisibleForTesting
		@Override
		public void addDebugInfo(StringBuilder info) {
			info.append("seedLo: ").append(seedLo).append(", seedHi: ").append(seedHi);
		}
	}
}
