package net.minecraft.util;

import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code StringHelper}.
 */
public class StringHelper {

	private static final Pattern FORMATTING_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
	private static final Pattern LINE_BREAK = Pattern.compile("\\r\\n|\\v");
	private static final Pattern ENDS_WITH_LINE_BREAK = Pattern.compile("(?:\\r\\n|\\v)$");

	/**
	 * Форматирует ticks.
	 *
	 * @param ticks ticks
	 * @param tickRate tick rate
	 *
	 * @return String — результат операции
	 */
	public static String formatTicks(int ticks, float tickRate) {
		int i = MathHelper.floor(ticks / tickRate);
		int j = i / 60;
		i %= 60;
		int k = j / 60;
		j %= 60;
		return k > 0 ? String.format(Locale.ROOT, "%02d:%02d:%02d", k, j, i)
		             : String.format(Locale.ROOT, "%02d:%02d", j, i);
	}

	/**
	 * Strip text format.
	 *
	 * @param text text
	 *
	 * @return String — результат операции
	 */
	public static String stripTextFormat(String text) {
		return FORMATTING_CODE.matcher(text).replaceAll("");
	}

	public static boolean isEmpty(@Nullable String text) {
		return StringUtils.isEmpty(text);
	}

	/**
	 * Truncate.
	 *
	 * @param text text
	 * @param maxLength max length
	 * @param addEllipsis add ellipsis
	 *
	 * @return String — результат операции
	 */
	public static String truncate(String text, int maxLength, boolean addEllipsis) {
		if (text.length() <= maxLength) {
			return text;
		}
		else {
			return addEllipsis && maxLength > 3 ? text.substring(0, maxLength - 3) + "..."
			                                    : text.substring(0, maxLength);
		}
	}

	/**
	 * Count lines.
	 *
	 * @param text text
	 *
	 * @return int — результат операции
	 */
	public static int countLines(String text) {
		if (text.isEmpty()) {
			return 0;
		}
		else {
			Matcher matcher = LINE_BREAK.matcher(text);
			int i = 1;

			while (matcher.find()) {
				i++;
			}

			return i;
		}
	}

	/**
	 * Ends with line break.
	 *
	 * @param text text
	 *
	 * @return boolean — результат операции
	 */
	public static boolean endsWithLineBreak(String text) {
		return ENDS_WITH_LINE_BREAK.matcher(text).find();
	}

	/**
	 * Truncate chat.
	 *
	 * @param text text
	 *
	 * @return String — результат операции
	 */
	public static String truncateChat(String text) {
		return truncate(text, 256, false);
	}

	public static boolean isValidChar(int c) {
		return c != 167 && c >= 32 && c != 127;
	}

	public static boolean isValidPlayerName(String name) {
		return name.length() > 16 ? false : name.chars().filter(c -> c <= 32 || c >= 127).findAny().isEmpty();
	}

	/**
	 * Strip invalid chars.
	 *
	 * @param string string
	 *
	 * @return String — результат операции
	 */
	public static String stripInvalidChars(String string) {
		return stripInvalidChars(string, false);
	}

	/**
	 * Strip invalid chars.
	 *
	 * @param string string
	 * @param allowLinebreak allow linebreak
	 *
	 * @return String — результат операции
	 */
	public static String stripInvalidChars(String string, boolean allowLinebreak) {
		StringBuilder stringBuilder = new StringBuilder();

		for (char c : string.toCharArray()) {
			if (isValidChar(c)) {
				stringBuilder.append(c);
			}
			else if (allowLinebreak && c == '\n') {
				stringBuilder.append(c);
			}
		}

		return stringBuilder.toString();
	}

	public static boolean isWhitespace(int c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}

	public static boolean isBlank(@Nullable String string) {
		return string != null && !string.isEmpty() ? string.chars().allMatch(StringHelper::isWhitespace) : true;
	}
}
