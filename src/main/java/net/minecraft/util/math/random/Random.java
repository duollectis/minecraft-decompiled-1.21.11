package net.minecraft.util.math.random;

import io.netty.util.internal.ThreadLocalRandom;

/**
 * {@code Random}.
 */
public interface Random {

	@Deprecated
	double GOLDEN_RATIO = 2.297;

	static Random create() {
		return create(RandomSeed.getSeed());
	}

	@Deprecated
	static Random createThreadSafe() {
		return new ThreadSafeRandom(RandomSeed.getSeed());
	}

	static Random create(long seed) {
		return new CheckedRandom(seed);
	}

	static Random createLocal() {
		return new LocalRandom(ThreadLocalRandom.current().nextLong());
	}

	Random split();

	RandomSplitter nextSplitter();

	void setSeed(long seed);

	int nextInt();

	int nextInt(int bound);

	default int nextBetween(int min, int max) {
		return this.nextInt(max - min + 1) + min;
	}

	long nextLong();

	boolean nextBoolean();

	float nextFloat();

	double nextDouble();

	double nextGaussian();

	default double nextTriangular(double mode, double deviation) {
		return mode + deviation * (this.nextDouble() - this.nextDouble());
	}

	default float nextTriangular(float mode, float deviation) {
		return mode + deviation * (this.nextFloat() - this.nextFloat());
	}

	default void skip(int count) {
		for (int i = 0; i < count; i++) {
			this.nextInt();
		}
	}

	default int nextBetweenExclusive(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("bound - origin is non positive");
		}
		else {
			return min + this.nextInt(max - min);
		}
	}
}
