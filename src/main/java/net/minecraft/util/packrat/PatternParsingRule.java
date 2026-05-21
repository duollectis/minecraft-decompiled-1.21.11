package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code PatternParsingRule}.
 */
public final class PatternParsingRule implements ParsingRule<StringReader, String> {

	private final Pattern pattern;
	private final CursorExceptionType<CommandSyntaxException> exception;

	public PatternParsingRule(Pattern pattern, CursorExceptionType<CommandSyntaxException> exception) {
		this.pattern = pattern;
		this.exception = exception;
	}

	public String parse(ParsingState<StringReader> parsingState) {
		StringReader stringReader = parsingState.getReader();
		String string = stringReader.getString();
		Matcher matcher = this.pattern.matcher(string).region(stringReader.getCursor(), string.length());
		if (!matcher.lookingAt()) {
			parsingState.getErrors().add(parsingState.getCursor(), this.exception);
			return null;
		}
		else {
			stringReader.setCursor(matcher.end());
			return matcher.group(0);
		}
	}
}
