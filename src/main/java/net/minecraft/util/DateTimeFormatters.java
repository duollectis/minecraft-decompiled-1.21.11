package net.minecraft.util;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * Предопределённые форматтеры дат и времени для использования в игре.
 * Все форматтеры используют локаль {@link Locale#ROOT} для стабильного вывода.
 */
public class DateTimeFormatters {

	/**
	 * Форматтер с точностью до минут в формате {@code YYYY-MM-DD_HH-MM-SS}.
	 * Используется для именования файлов скриншотов, логов и резервных копий.
	 */
	public static final DateTimeFormatter MINUTES = new DateTimeFormatterBuilder()
		.appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
		.appendLiteral('-')
		.appendValue(ChronoField.MONTH_OF_YEAR, 2)
		.appendLiteral('-')
		.appendValue(ChronoField.DAY_OF_MONTH, 2)
		.appendLiteral('_')
		.appendValue(ChronoField.HOUR_OF_DAY, 2)
		.appendLiteral('-')
		.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
		.appendLiteral('-')
		.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
		.toFormatter(Locale.ROOT);
}
