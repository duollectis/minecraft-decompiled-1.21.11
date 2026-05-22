package net.minecraft.util;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Поставщик текущего времени с возможностью конвертации в произвольные единицы.
 */
@FunctionalInterface
public interface TimeSupplier {

	long get(TimeUnit timeUnit);

	/**
	 * Специализация {@link TimeSupplier} для наносекундного разрешения,
	 * совместимая с {@link LongSupplier}.
	 */
	interface Nanoseconds extends TimeSupplier, LongSupplier {

		@Override
		default long get(TimeUnit timeUnit) {
			return timeUnit.convert(getAsLong(), TimeUnit.NANOSECONDS);
		}
	}
}
