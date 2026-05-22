package net.minecraft.text;

import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.Optional;

/**
 * Фабрика для посимвольного обхода строк с учётом стиля и форматирования Minecraft.
 * Поддерживает прямой и обратный обход, а также обход строк с кодами форматирования (§).
 *
 * <p>Все методы корректно обрабатывают суррогатные пары Unicode,
 * заменяя одиночные суррогаты на символ замены U+FFFD.
 */
public class TextVisitFactory {

	/** Кодовая точка символа замены Unicode (U+FFFD), используется вместо некорректных суррогатов. */
	private static final int REPLACEMENT_CODE_POINT = '\uFFFD';

	/** Код символа-префикса форматирования Minecraft (§, U+00A7). */
	private static final char SECTION_SIGN = '\u00A7';

	private static final Optional<Object> VISIT_TERMINATED = Optional.of(Unit.INSTANCE);

	/**
	 * Обрабатывает одиночный символ: суррогаты заменяются на {@link #REPLACEMENT_CODE_POINT}.
	 */
	private static boolean visitRegularCharacter(Style style, CharacterVisitor visitor, int index, char ch) {
		return Character.isSurrogate(ch)
			? visitor.accept(index, style, REPLACEMENT_CODE_POINT)
			: visitor.accept(index, style, ch);
	}

	/**
	 * Обходит строку посимвольно в прямом направлении, корректно обрабатывая суррогатные пары.
	 * Одиночные суррогаты заменяются на {@link #REPLACEMENT_CODE_POINT}.
	 *
	 * @param text    строка для обхода
	 * @param style   стиль, применяемый ко всем символам
	 * @param visitor посетитель символов
	 * @return {@code true} если обход завершён полностью; {@code false} если прерван посетителем
	 */
	public static boolean visitForwards(String text, Style style, CharacterVisitor visitor) {
		int length = text.length();

		for (int pos = 0; pos < length; pos++) {
			char ch = text.charAt(pos);

			if (Character.isHighSurrogate(ch)) {
				if (pos + 1 >= length) {
					if (!visitor.accept(pos, style, REPLACEMENT_CODE_POINT)) {
						return false;
					}

					break;
				}

				char low = text.charAt(pos + 1);

				if (Character.isLowSurrogate(low)) {
					if (!visitor.accept(pos, style, Character.toCodePoint(ch, low))) {
						return false;
					}

					pos++;
				} else if (!visitor.accept(pos, style, REPLACEMENT_CODE_POINT)) {
					return false;
				}
			} else if (!visitRegularCharacter(style, visitor, pos, ch)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Обходит строку посимвольно в обратном направлении, корректно обрабатывая суррогатные пары.
	 * Одиночные суррогаты заменяются на {@link #REPLACEMENT_CODE_POINT}.
	 *
	 * @param text    строка для обхода
	 * @param style   стиль, применяемый ко всем символам
	 * @param visitor посетитель символов
	 * @return {@code true} если обход завершён полностью; {@code false} если прерван посетителем
	 */
	public static boolean visitBackwards(String text, Style style, CharacterVisitor visitor) {
		int length = text.length();

		for (int pos = length - 1; pos >= 0; pos--) {
			char ch = text.charAt(pos);

			if (Character.isLowSurrogate(ch)) {
				if (pos - 1 < 0) {
					if (!visitor.accept(0, style, REPLACEMENT_CODE_POINT)) {
						return false;
					}

					break;
				}

				char high = text.charAt(pos - 1);

				if (Character.isHighSurrogate(high)) {
					if (!visitor.accept(--pos, style, Character.toCodePoint(high, ch))) {
						return false;
					}
				} else if (!visitor.accept(pos, style, REPLACEMENT_CODE_POINT)) {
					return false;
				}
			} else if (!visitRegularCharacter(style, visitor, pos, ch)) {
				return false;
			}
		}

		return true;
	}

	public static boolean visitFormatted(String text, Style style, CharacterVisitor visitor) {
		return visitFormatted(text, 0, style, visitor);
	}

	public static boolean visitFormatted(String text, int startIndex, Style style, CharacterVisitor visitor) {
		return visitFormatted(text, startIndex, style, style, visitor);
	}

	/**
	 * Обходит строку с учётом кодов форматирования Minecraft (§X).
	 * При встрече {@link Formatting#RESET} стиль сбрасывается до {@code resetStyle}.
	 * Остальные коды применяются через {@link Style#withExclusiveFormatting}.
	 *
	 * @param text          строка с кодами форматирования
	 * @param startIndex    позиция начала обхода
	 * @param startingStyle начальный стиль
	 * @param resetStyle    стиль для сброса при {@code §r}
	 * @param visitor       посетитель символов
	 * @return {@code true} если обход завершён полностью; {@code false} если прерван посетителем
	 */
	public static boolean visitFormatted(
		String text,
		int startIndex,
		Style startingStyle,
		Style resetStyle,
		CharacterVisitor visitor
	) {
		int length = text.length();
		Style currentStyle = startingStyle;

		for (int pos = startIndex; pos < length; pos++) {
			char ch = text.charAt(pos);

			if (ch == SECTION_SIGN) {
				if (pos + 1 >= length) {
					break;
				}

				char code = text.charAt(pos + 1);
				Formatting formatting = Formatting.byCode(code);

				if (formatting != null) {
					currentStyle = formatting == Formatting.RESET
						? resetStyle
						: currentStyle.withExclusiveFormatting(formatting);
				}

				pos++;
			} else if (Character.isHighSurrogate(ch)) {
				if (pos + 1 >= length) {
					if (!visitor.accept(pos, currentStyle, REPLACEMENT_CODE_POINT)) {
						return false;
					}

					break;
				}

				char low = text.charAt(pos + 1);

				if (Character.isLowSurrogate(low)) {
					if (!visitor.accept(pos, currentStyle, Character.toCodePoint(ch, low))) {
						return false;
					}

					pos++;
				} else if (!visitor.accept(pos, currentStyle, REPLACEMENT_CODE_POINT)) {
					return false;
				}
			} else if (!visitRegularCharacter(currentStyle, visitor, pos, ch)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Обходит {@link StringVisitable} с учётом кодов форматирования.
	 *
	 * @return {@code true} если обход завершён полностью; {@code false} если прерван посетителем
	 */
	public static boolean visitFormatted(StringVisitable text, Style style, CharacterVisitor visitor) {
		return text
			.visit(
				(visitStyle, string) -> visitFormatted(string, 0, visitStyle, visitor)
					? Optional.empty()
					: VISIT_TERMINATED,
				style
			)
			.isEmpty();
	}

	/**
	 * Нормализует строку, заменяя некорректные суррогаты на символ замены U+FFFD.
	 * Используется для санитизации пользовательского ввода перед отображением.
	 */
	public static String validateSurrogates(String text) {
		StringBuilder builder = new StringBuilder();

		visitForwards(text, Style.EMPTY, (index, style, codePoint) -> {
			builder.appendCodePoint(codePoint);
			return true;
		});

		return builder.toString();
	}

	/**
	 * Удаляет все коды форматирования Minecraft (§X) из текста,
	 * возвращая чистую строку без управляющих символов.
	 */
	public static String removeFormattingCodes(StringVisitable text) {
		StringBuilder builder = new StringBuilder();

		visitFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
			builder.appendCodePoint(codePoint);
			return true;
		});

		return builder.toString();
	}
}
