package net.minecraft.client.realms;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;

/**
 * Единицы измерения размера файла: B, KB, MB, GB.
 * Предоставляет утилиты для автоматического выбора наиболее подходящей единицы
 * и форматирования размера в человекочитаемый вид.
 */
@Environment(EnvType.CLIENT)
public enum SizeUnit {
	B,
	KB,
	MB,
	GB;

	private static final int BASE = 1024;
	private static final String SIZE_PREFIXES = "KMGTPE";

	/**
	 * Определяет наибольшую подходящую единицу измерения для заданного размера в байтах.
	 * Например, для 2 097 152 байт вернёт {@link #MB}.
	 *
	 * @param bytes размер в байтах
	 * @return наибольшая подходящая единица измерения
	 */
	public static SizeUnit getLargestUnit(long bytes) {
		if (bytes < BASE) {
			return B;
		}

		try {
			int exponent = (int) (Math.log(bytes) / Math.log(BASE));
			String prefix = String.valueOf(SIZE_PREFIXES.charAt(exponent - 1));
			return valueOf(prefix + "B");
		} catch (Exception ignored) {
			return GB;
		}
	}

	/**
	 * Конвертирует байты в указанную единицу измерения.
	 *
	 * @param bytes размер в байтах
	 * @param unit целевая единица измерения
	 * @return значение в указанной единице
	 */
	public static double convertToUnit(long bytes, SizeUnit unit) {
		return unit == B ? bytes : bytes / Math.pow(BASE, unit.ordinal());
	}

	/**
	 * Форматирует размер в байтах в строку с автоматически выбранной единицей.
	 * Например: {@code 1536} → {@code "1.5 KB"}.
	 *
	 * @param bytes размер в байтах
	 * @return отформатированная строка
	 */
	public static String getUserFriendlyString(long bytes) {
		if (bytes < BASE) {
			return bytes + " B";
		}

		int exponent = (int) (Math.log(bytes) / Math.log(BASE));
		String prefix = SIZE_PREFIXES.charAt(exponent - 1) + "";
		return String.format(Locale.ROOT, "%.1f %sB", bytes / Math.pow(BASE, exponent), prefix);
	}

	/**
	 * Форматирует размер в байтах в строку с явно указанной единицей измерения.
	 * Для GB используется один знак после запятой, для остальных — целое число.
	 *
	 * @param bytes размер в байтах
	 * @param unit единица измерения для отображения
	 * @return отформатированная строка
	 */
	public static String humanReadableSize(long bytes, SizeUnit unit) {
		return String.format(
				Locale.ROOT,
				"%." + (unit == GB ? "1" : "0") + "f %s",
				convertToUnit(bytes, unit),
				unit.name()
		);
	}
}
