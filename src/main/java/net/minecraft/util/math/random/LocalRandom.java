package net.minecraft.util.math.random;

/**
 * Однопоточная реализация генератора псевдослучайных чисел на основе
 * линейного конгруэнтного алгоритма (LCG). Аналог {@link CheckedRandom},
 * но без атомарных операций — предназначен исключительно для использования
 * в одном потоке, что даёт прирост производительности.
 */
public class LocalRandom implements BaseRandom {

	private static final int INT_BITS = 48;
	private static final long SEED_MASK = 281474976710655L;
	private static final long MULTIPLIER = 25214903917L;
	private static final long INCREMENT = 11L;

	private long seed;
	private final GaussianGenerator gaussianGenerator = new GaussianGenerator(this);

	public LocalRandom(long seed) {
		setSeed(seed);
	}

	@Override
	public Random split() {
		return new LocalRandom(nextLong());
	}

	@Override
	public RandomSplitter nextSplitter() {
		return new CheckedRandom.Splitter(nextLong());
	}

	@Override
	public void setSeed(long seed) {
		this.seed = (seed ^ MULTIPLIER) & SEED_MASK;
		gaussianGenerator.reset();
	}

	@Override
	public int next(int bits) {
		long next = seed * MULTIPLIER + INCREMENT & SEED_MASK;
		seed = next;
		return (int) (next >> INT_BITS - bits);
	}

	@Override
	public double nextGaussian() {
		return gaussianGenerator.next();
	}
}
