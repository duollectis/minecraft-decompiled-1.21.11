package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * {@code TokenParsingRule}.
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

	/**
	 * Parse.
	 *
	 * @param parsingState parsing state
	 *
	 * @return @Nullable String — результат операции
	 */
	public @Nullable String parse(ParsingState<StringReader> parsingState) {
		StringReader stringReader = parsingState.getReader();
		String string = stringReader.getString();
		int i = stringReader.getCursor();
		int j = i;

		while (j < string.length() && this.isValidChar(string.charAt(j)) && j - i < this.maxLength) {
			j++;
		}

		int k = j - i;
		if (k < this.minLength) {
			parsingState.getErrors().add(parsingState.getCursor(), this.tooShortException);
			return null;
		}
		else {
			stringReader.setCursor(j);
			return string.substring(i, j);
		}
	}

	protected abstract boolean isValidChar(char c);
}
