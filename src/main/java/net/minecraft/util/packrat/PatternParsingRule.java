package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Правило разбора, сопоставляющее текущую позицию в {@link StringReader}
 * с регулярным выражением. Использует {@link Matcher#lookingAt()} для проверки
 * совпадения начиная с текущего курсора без сдвига всего ввода.
 */
public final class PatternParsingRule implements ParsingRule<StringReader, String> {

	private final Pattern pattern;
	private final CursorExceptionType<CommandSyntaxException> exception;

	public PatternParsingRule(Pattern pattern, CursorExceptionType<CommandSyntaxException> exception) {
		this.pattern = pattern;
		this.exception = exception;
	}

	@Override
	public String parse(ParsingState<StringReader> parsingState) {
		StringReader reader = parsingState.getReader();
		String input = reader.getString();
		Matcher matcher = pattern.matcher(input).region(reader.getCursor(), input.length());

		if (!matcher.lookingAt()) {
			parsingState.getErrors().add(parsingState.getCursor(), exception);
			return null;
		}

		reader.setCursor(matcher.end());
		return matcher.group(0);
	}
}
