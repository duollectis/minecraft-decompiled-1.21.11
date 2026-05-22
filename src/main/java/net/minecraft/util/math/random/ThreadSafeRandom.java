package net.minecraft.util.math.random;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Потокобезопасная реализация LCG-генератора на основе {@link AtomicLong}.
 * Использует CAS-цикл для атомарного обновления сида, что гарантирует корректность
 * при конкурентном доступе из нескольких потоков.
 *
 * @deprecated Предпочтительнее использовать {@link CheckedRandom} или {@link Xoroshiro128PlusPlusRandom}.
 */
@Deprecated
public class ThreadSafeRandom implements BaseRandom {

	private static final int INT_BITS = 48;
	private static final long SEED_MASK = 281474976710655L;
	private static final long MULTIPLIER = 25214903917L;
	private static final long INCREMENT = 11L;

	private final AtomicLong seed = new AtomicLong();
	private final GaussianGenerator gaussianGenerator = new GaussianGenerator(this);

	public ThreadSafeRandom(long seed) {
		setSeed(seed);
	}

	@Override
	public Random split() {
		return new ThreadSafeRandom(nextLong());
	}

	@Override
	public RandomSplitter nextSplitter() {
		return new CheckedRandom.Splitter(nextLong());
	}

	@Override
	public void setSeed(long seed) {
		this.seed.set((seed ^ MULTIPLIER) & SEED_MASK);
	}

	@Override
	public int next(int bits) {
		long current;
		long next;

		do {
			current = seed.get();
			next = current * MULTIPLIER + INCREMENT & SEED_MASK;
		} while (!seed.compareAndSet(current, next));

		return (int) (next >>> INT_BITS - bits);
	}

	@Override
	public double nextGaussian() {
		return gaussianGenerator.next();
	}
}
