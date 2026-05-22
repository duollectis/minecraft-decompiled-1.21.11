package net.minecraft.util;

import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилитарные методы для работы со строками в контексте Minecraft:
 * форматирование времени, обрезка текста, валидация имён игроков и символов.
 */
public class StringHelper {

	private static final Pattern FORMATTING_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
	private static final Pattern LINE_BREAK = Pattern.compile("\\r\\n|\\v");
	private static final Pattern ENDS_WITH_LINE_BREAK = Pattern.compile("(?:\\r\\n|\\v)$");

	/** Максимальная длина сообщения в чате. */
	private static final int MAX_CHAT_LENGTH = 256;

	/** Максимальная длина имени игрока. */
	private static final int MAX_PLAYER_NAME_LENGTH = 16;

	/** Код символа параграфа (§), используемого для форматирования. */
	private static final int SECTION_SIGN = 167;

	/** Код символа DEL, недопустимого в именах и сообщениях. */
	private static final int DEL_CHAR = 127;

	/** Минимальный допустимый код символа (пробел). */
	private static final int MIN_VALID_CHAR = 32;

	/**
	 * Форматирует количество тиков в строку вида {@code ЧЧ:ММ:СС} или {@code ММ:СС}.
	 *
	 * @param ticks количество тиков
	 * @param tickRate тиков в секунду
	 * @return отформатированная строка времени
	 */
	public static String formatTicks(int ticks, float tickRate) {
		int totalSeconds = MathHelper.floor(ticks / tickRate);
		int seconds = totalSeconds % 60;
		int totalMinutes = totalSeconds / 60;
		int minutes = totalMinutes % 60;
		int hours = totalMinutes / 60;

		return hours > 0
			? String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
			: String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
	}

	/**
	 * Удаляет коды форматирования Minecraft (§X) из строки.
	 *
	 * @param text строка с кодами форматирования
	 * @return строка без кодов форматирования
	 */
	public static String stripTextFormat(String text) {
		return FORMATTING_CODE.matcher(text).replaceAll("");
	}

	public static boolean isEmpty(@Nullable String text) {
		return StringUtils.isEmpty(text);
	}

	/**
	 * Обрезает строку до заданной длины с опциональным добавлением многоточия.
	 *
	 * @param text исходная строка
	 * @param maxLength максимальная длина результата
	 * @param addEllipsis добавить «...» в конец (требует maxLength > 3)
	 * @return обрезанная строка
	 */
	public static String truncate(String text, int maxLength, boolean addEllipsis) {
		if (text.length() <= maxLength) {
			return text;
		}

		return addEllipsis && maxLength > 3
			? text.substring(0, maxLength - 3) + "..."
			: text.substring(0, maxLength);
	}

	/**
	 * Подсчитывает количество строк в тексте (минимум 1 для непустой строки).
	 *
	 * @param text текст для подсчёта строк
	 * @return количество строк
	 */
	public static int countLines(String text) {
		if (text.isEmpty()) {
			return 0;
		}

		Matcher matcher = LINE_BREAK.matcher(text);
		int lineCount = 1;

		while (matcher.find()) {
			lineCount++;
		}

		return lineCount;
	}

	public static boolean endsWithLineBreak(String text) {
		return ENDS_WITH_LINE_BREAK.matcher(text).find();
	}

	/**
	 * Обрезает строку до максимальной длины сообщения чата ({@value MAX_CHAT_LENGTH} символов).
	 *
	 * @param text исходная строка
	 * @return обрезанная строка
	 */
	public static String truncateChat(String text) {
		return truncate(text, MAX_CHAT_LENGTH, false);
	}

	/**
	 * Проверяет допустимость символа для ввода в текстовые поля Minecraft.
	 * Запрещены: символ параграфа (§), управляющие символы (< 32) и DEL (127).
	 *
	 * @param c код символа
	 * @return {@code true} если символ допустим
	 */
	public static boolean isValidChar(int c) {
		return c != SECTION_SIGN && c >= MIN_VALID_CHAR && c != DEL_CHAR;
	}

	/**
	 * Проверяет допустимость имени игрока: не длиннее 16 символов и содержит только
	 * символы с кодами от 33 до 126 включительно.
	 *
	 * @param name проверяемое имя
	 * @return {@code true} если имя допустимо
	 */
	public static boolean isValidPlayerName(String name) {
		return name.length() <= MAX_PLAYER_NAME_LENGTH
			&& name.chars().filter(c -> c <= MIN_VALID_CHAR || c >= DEL_CHAR).findAny().isEmpty();
	}

	public static String stripInvalidChars(String string) {
		return stripInvalidChars(string, false);
	}

	/**
	 * Удаляет недопустимые символы из строки с опциональным сохранением переносов строк.
	 *
	 * @param string исходная строка
	 * @param allowLinebreak разрешить символ переноса строки {@code \n}
	 * @return строка без недопустимых символов
	 */
	public static String stripInvalidChars(String string, boolean allowLinebreak) {
		StringBuilder result = new StringBuilder();

		for (char c : string.toCharArray()) {
			if (isValidChar(c)) {
				result.append(c);
			} else if (allowLinebreak && c == '\n') {
				result.append(c);
			}
		}

		return result.toString();
	}

	public static boolean isWhitespace(int c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}

	public static boolean isBlank(@Nullable String string) {
		return string == null || string.isEmpty() || string.chars().allMatch(StringHelper::isWhitespace);
	}
}
