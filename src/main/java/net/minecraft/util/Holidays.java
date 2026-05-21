package net.minecraft.util;

import java.time.Month;
import java.time.MonthDay;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * {@code Holidays}.
 */
public class Holidays {

	public static final MonthDay HALLOWEEN = MonthDay.of(Month.OCTOBER, 31);
	public static final List<MonthDay> CHRISTMAS_PERIOD = List.of(
			MonthDay.of(Month.DECEMBER, 24), MonthDay.of(Month.DECEMBER, 25), MonthDay.of(Month.DECEMBER, 26)
	);
	public static final MonthDay CHRISTMAS_EVE = MonthDay.of(Month.DECEMBER, 24);
	public static final MonthDay NEW_YEARS_DAY = MonthDay.of(Month.JANUARY, 1);

	/**
	 * Now.
	 *
	 * @return MonthDay — результат операции
	 */
	public static MonthDay now() {
		return MonthDay.from(ZonedDateTime.now());
	}

	public static boolean isHalloween() {
		return HALLOWEEN.equals(now());
	}

	public static boolean isAroundChristmas() {
		return CHRISTMAS_PERIOD.contains(now());
	}
}
