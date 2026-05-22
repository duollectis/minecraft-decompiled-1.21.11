package net.minecraft.util.math;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.NoSuchElementException;

/**
 * Итератор, равномерно распределяющий {@code dividend} по {@code divisor} частям.
 * Каждый вызов {@link #nextInt()} возвращает очередную часть (quotient ± 1),
 * гарантируя, что сумма всех возвращённых значений равна {@code dividend}.
 */
public class Divider implements IntIterator {

	private final int divisor;
	private final int quotient;
	private final int mod;
	private int returnedCount;
	private int remainder;

	public Divider(int dividend, int divisor) {
		this.divisor = divisor;

		if (divisor > 0) {
			quotient = dividend / divisor;
			mod = dividend % divisor;
		} else {
			quotient = 0;
			mod = 0;
		}
	}

	public boolean hasNext() {
		return returnedCount < divisor;
	}

	public int nextInt() {
		if (hasNext() == false) {
			throw new NoSuchElementException();
		}

		int value = quotient;
		remainder += mod;

		if (remainder >= divisor) {
			remainder -= divisor;
			value++;
		}

		returnedCount++;
		return value;
	}

	@VisibleForTesting
	public static Iterable<Integer> asIterable(int dividend, int divisor) {
		return () -> new Divider(dividend, divisor);
	}
}
