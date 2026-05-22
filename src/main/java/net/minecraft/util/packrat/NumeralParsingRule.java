package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для правил разбора числовых литералов.
 * Считывает последовательность допустимых символов и запрещает
 * начало или конец токена символом подчёркивания (разделитель разрядов).
 */
public abstract class NumeralParsingRule implements ParsingRule<StringReader, String> {

	private final CursorExceptionType<CommandSyntaxException> invalidCharException;
	private final CursorExceptionType<CommandSyntaxException> unexpectedUnderscoreException;

	public NumeralParsingRule(
			CursorExceptionType<CommandSyntaxException> invalidCharException,
			CursorExceptionType<CommandSyntaxException> unexpectedUnderscoreException
	) {
		this.invalidCharException = invalidCharException;
		this.unexpectedUnderscoreException = unexpectedUnderscoreException;
	}

	@Override
	public @Nullable String parse(ParsingState<StringReader> state) {
		StringReader reader = state.getReader();
		reader.skipWhitespace();

		String input = reader.getString();
		int start = reader.getCursor();
		int end = start;

		while (end < input.length() && accepts(input.charAt(end))) {
			end++;
		}

		int length = end - start;

		if (length == 0) {
			state.getErrors().add(state.getCursor(), invalidCharException);
			return null;
		}

		if (input.charAt(start) == '_' || input.charAt(end - 1) == '_') {
			state.getErrors().add(state.getCursor(), unexpectedUnderscoreException);
			return null;
		}

		reader.setCursor(end);
		return input.substring(start, end);
	}

	protected abstract boolean accepts(char c);
}
