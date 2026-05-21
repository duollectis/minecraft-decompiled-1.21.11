package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * {@code AnyIdParsingRule}.
 */
public class AnyIdParsingRule implements ParsingRule<StringReader, Identifier> {

	public static final ParsingRule<StringReader, Identifier> INSTANCE = new AnyIdParsingRule();

	private AnyIdParsingRule() {
	}

	public @Nullable Identifier parse(ParsingState<StringReader> parsingState) {
		parsingState.getReader().skipWhitespace();

		try {
			return Identifier.fromCommandInputNonEmpty(parsingState.getReader());
		}
		catch (CommandSyntaxException var3) {
			return null;
		}
	}
}
