package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * Правило разбора строки без кавычек через {@link StringReader#readUnquotedString()}.
 * Перед чтением пропускает пробельные символы. Проверяет минимальную длину
 * прочитанной строки и регистрирует ошибку, если строка слишком короткая.
 */
public class UnquotedStringParsingRule implements ParsingRule<StringReader, String> {

	private final int minLength;
	private final CursorExceptionType<CommandSyntaxException> tooShortException;

	public UnquotedStringParsingRule(int minLength, CursorExceptionType<CommandSyntaxException> tooShortException) {
		this.minLength = minLength;
		this.tooShortException = tooShortException;
	}

	@Override
	public @Nullable String parse(ParsingState<StringReader> parsingState) {
		parsingState.getReader().skipWhitespace();
		int startCursor = parsingState.getCursor();
		String value = parsingState.getReader().readUnquotedString();

		if (value.length() < minLength) {
			parsingState.getErrors().add(startCursor, tooShortException);
			return null;
		}

		return value;
	}
}
