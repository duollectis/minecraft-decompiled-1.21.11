package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Правило разбора, которое считывает любой валидный {@link Identifier} из входного потока.
 * Является синглтоном — используйте {@link #INSTANCE} вместо создания новых экземпляров.
 */
public class AnyIdParsingRule implements ParsingRule<StringReader, Identifier> {

	public static final ParsingRule<StringReader, Identifier> INSTANCE = new AnyIdParsingRule();

	private AnyIdParsingRule() {
	}

	@Override
	public @Nullable Identifier parse(ParsingState<StringReader> state) {
		state.getReader().skipWhitespace();

		try {
			return Identifier.fromCommandInputNonEmpty(state.getReader());
		} catch (CommandSyntaxException e) {
			return null;
		}
	}
}
