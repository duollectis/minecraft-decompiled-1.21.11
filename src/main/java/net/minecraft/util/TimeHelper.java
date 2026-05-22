package net.minecraft.util;

import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.concurrent.TimeUnit;

/**
 * Константы и утилиты для работы со временем.
 */
public class TimeHelper {

	public static final long SECOND_IN_NANOS = TimeUnit.SECONDS.toNanos(1L);
	public static final long MILLI_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);
	public static final long SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1L);
	public static final long HOUR_IN_SECONDS = TimeUnit.HOURS.toSeconds(1L);
	public static final int MINUTE_IN_SECONDS = (int) TimeUnit.MINUTES.toSeconds(1L);

	private static final int TICKS_PER_SECOND = 20;

	/**
	 * Создаёт провайдер случайного интервала в тиках в диапазоне {@code [min*20, max*20]}.
	 *
	 * @param min минимальное количество секунд
	 * @param max максимальное количество секунд
	 * @return провайдер случайного числа тиков
	 */
	public static UniformIntProvider betweenSeconds(int min, int max) {
		return UniformIntProvider.create(min * TICKS_PER_SECOND, max * TICKS_PER_SECOND);
	}
}
