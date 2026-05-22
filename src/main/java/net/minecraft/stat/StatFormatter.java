package net.minecraft.stat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Функциональный интерфейс для форматирования числового значения статистики
 * в читаемую строку.
 *
 * <p>Предоставляет набор стандартных форматтеров: {@link #DEFAULT}, {@link #DIVIDE_BY_TEN},
 * {@link #DISTANCE} и {@link #TIME}. Каждый форматтер адаптирует сырое целое число
 * к удобочитаемому представлению с нужными единицами измерения.
 */
public interface StatFormatter {

	DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
		"########0.00",
		DecimalFormatSymbols.getInstance(Locale.ROOT)
	);

	/** Форматирует значение как целое число в локали US (например, "1,234"). */
	StatFormatter DEFAULT = NumberFormat.getIntegerInstance(Locale.US)::format;

	/** Форматирует значение, разделив на 10 (используется для урона: 1 единица = 0.5 HP). */
	StatFormatter DIVIDE_BY_TEN = value -> DECIMAL_FORMAT.format(value * 0.1);

	/**
	 * Форматирует расстояние в сантиметрах, автоматически выбирая единицу:
	 * километры (> 500 м), метры (> 0.5 м) или сантиметры.
	 */
	StatFormatter DISTANCE = cm -> {
		double meters = cm / 100.0;
		double kilometers = meters / 1000.0;

		if (kilometers > 0.5) {
			return DECIMAL_FORMAT.format(kilometers) + " km";
		}

		return meters > 0.5
			? DECIMAL_FORMAT.format(meters) + " m"
			: cm + " cm";
	};

	/**
	 * Форматирует время в тиках, автоматически выбирая единицу:
	 * годы, дни, часы, минуты или секунды.
	 */
	StatFormatter TIME = ticks -> {
		double seconds = ticks / 20.0;
		double minutes = seconds / 60.0;
		double hours = minutes / 60.0;
		double days = hours / 24.0;
		double years = days / 365.0;

		if (years > 0.5) {
			return DECIMAL_FORMAT.format(years) + " y";
		}

		if (days > 0.5) {
			return DECIMAL_FORMAT.format(days) + " d";
		}

		if (hours > 0.5) {
			return DECIMAL_FORMAT.format(hours) + " h";
		}

		return minutes > 0.5
			? DECIMAL_FORMAT.format(minutes) + " min"
			: seconds + " s";
	};

	/**
	 * Форматирует сырое целочисленное значение статистики в строку.
	 *
	 * @param value сырое значение статистики
	 * @return отформатированная строка
	 */
	String format(int value);
}
