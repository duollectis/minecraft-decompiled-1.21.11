package net.minecraft.util;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

@FunctionalInterface
/**
 * {@code TimeSupplier}.
 */
public interface TimeSupplier {

	long get(TimeUnit timeUnit);

	/**
	 * {@code Nanoseconds}.
	 */
	public interface Nanoseconds extends TimeSupplier, LongSupplier {

		@Override
		default long get(TimeUnit timeUnit) {
			return timeUnit.convert(this.getAsLong(), TimeUnit.NANOSECONDS);
		}
	}
}
