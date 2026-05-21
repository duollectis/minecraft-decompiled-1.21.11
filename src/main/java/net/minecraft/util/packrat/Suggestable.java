package net.minecraft.util.packrat;

import java.util.stream.Stream;

/**
 * {@code Suggestable}.
 */
public interface Suggestable<S> {

	Stream<String> possibleValues(ParsingState<S> state);

	static <S> Suggestable<S> empty() {
		return state -> Stream.empty();
	}
}
