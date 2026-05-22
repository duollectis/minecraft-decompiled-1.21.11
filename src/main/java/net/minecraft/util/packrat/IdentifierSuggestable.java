package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import net.minecraft.util.Identifier;

import java.util.stream.Stream;

/**
 * Расширение {@link Suggestable} для правил, которые могут предлагать
 * варианты автодополнения в виде {@link Identifier}.
 */
public interface IdentifierSuggestable extends Suggestable<StringReader> {

	Stream<Identifier> possibleIds();

	@Override
	default Stream<String> possibleValues(ParsingState<StringReader> state) {
		return possibleIds().map(Identifier::toString);
	}
}
