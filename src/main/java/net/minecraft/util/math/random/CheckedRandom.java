package net.minecraft.util.math.random;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.thread.LockHelper;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Потокобезопасный линейный конгруэнтный генератор (LCG) с проверкой конкурентного доступа.
 * При обнаружении гонки данных выбрасывает исключение через {@link LockHelper#crash},
 * что делает его безопасным для однопоточного использования с явной диагностикой нарушений.
 */
public class CheckedRandom implements BaseRandom {

	private static final int INT_BITS = 48;
	private static final long SEED_MASK = 281474976710655L;
	private static final long MULTIPLIER = 25214903917L;
	private static final long INCREMENT = 11L;

	private final AtomicLong seed = new AtomicLong();
	private final GaussianGenerator gaussianGenerator = new GaussianGenerator(this);

	public CheckedRandom(long seed) {
		setSeed(seed);
	}

	@Override
	public Random split() {
		return new CheckedRandom(nextLong());
	}

	@Override
	public RandomSplitter nextSplitter() {
		return new Splitter(nextLong());
	}

	@Override
	public void setSeed(long seed) {
		if (!this.seed.compareAndSet(this.seed.get(), (seed ^ MULTIPLIER) & SEED_MASK)) {
			throw LockHelper.crash("LegacyRandomSource", null);
		}

		gaussianGenerator.reset();
	}

	@Override
	public int next(int bits) {
		long current = seed.get();
		long next = current * MULTIPLIER + INCREMENT & SEED_MASK;

		if (!seed.compareAndSet(current, next)) {
			throw LockHelper.crash("LegacyRandomSource", null);
		}

		return (int) (next >> INT_BITS - bits);
	}

	@Override
	public double nextGaussian() {
		return gaussianGenerator.next();
	}

	public static class Splitter implements RandomSplitter {

		private final long seed;

		public Splitter(long seed) {
			this.seed = seed;
		}

		@Override
		public Random split(int x, int y, int z) {
			return new CheckedRandom(MathHelper.hashCode(x, y, z) ^ seed);
		}

		@Override
		public Random split(String key) {
			return new CheckedRandom(key.hashCode() ^ seed);
		}

		@Override
		public Random split(long key) {
			return new CheckedRandom(key);
		}

		@VisibleForTesting
		@Override
		public void addDebugInfo(StringBuilder info) {
			info.append("LegacyPositionalRandomFactory{").append(seed).append("}");
		}
	}
}
