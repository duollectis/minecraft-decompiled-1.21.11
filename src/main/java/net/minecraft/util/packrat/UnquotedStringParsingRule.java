package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * {@code UnquotedStringParsingRule}.
 */
public class UnquotedStringParsingRule implements ParsingRule<StringReader, String> {

	private final int minLength;
	private final CursorExceptionType<CommandSyntaxException> tooShortException;

	public UnquotedStringParsingRule(int minLength, CursorExceptionType<CommandSyntaxException> tooShortException) {
		this.minLength = minLength;
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
		parsingState.getReader().skipWhitespace();
		int i = parsingState.getCursor();
		String string = parsingState.getReader().readUnquotedString();
		if (string.length() < this.minLength) {
			parsingState.getErrors().add(i, this.tooShortException);
			return null;
		}
		else {
			return string;
		}
	}
}
