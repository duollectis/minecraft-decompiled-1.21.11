package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * Абстрактное правило разбора токена — непрерывной последовательности символов,
 * удовлетворяющих условию {@link #isValidChar(char)}. Поддерживает ограничение
 * минимальной и максимальной длины токена.
 */
public abstract class TokenParsingRule implements ParsingRule<StringReader, String> {

	private final int minLength;
	private final int maxLength;
	private final CursorExceptionType<CommandSyntaxException> tooShortException;

	public TokenParsingRule(int minLength, CursorExceptionType<CommandSyntaxException> tooShortException) {
		this(minLength, Integer.MAX_VALUE, tooShortException);
	}

	public TokenParsingRule(
			int minLength,
			int maxLength,
			CursorExceptionType<CommandSyntaxException> tooShortException
	) {
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.tooShortException = tooShortException;
	}

	@Override
	public @Nullable String parse(ParsingState<StringReader> parsingState) {
		StringReader reader = parsingState.getReader();
		String input = reader.getString();
		int start = reader.getCursor();
		int end = start;

		while (end < input.length() && isValidChar(input.charAt(end)) && end - start < maxLength) {
			end++;
		}

		int length = end - start;

		if (length < minLength) {
			parsingState.getErrors().add(parsingState.getCursor(), tooShortException);
			return null;
		}

		reader.setCursor(end);
		return input.substring(start, end);
	}

	protected abstract boolean isValidChar(char c);
}
