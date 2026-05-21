package net.minecraft.util.packrat;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code ParsingState}.
 */
public interface ParsingState<S> {

	ParseResults getResults();

	ParseErrorList<S> getErrors();

	default <T> Optional<T> startParsing(ParsingRuleEntry<S, T> rule) {
		T object = this.parse(rule);
		if (object != null) {
			this.getErrors().setCursor(this.getCursor());
		}

		if (!this.getResults().areFramesPlacedCorrectly()) {
			throw new IllegalStateException("Malformed scope: " + this.getResults());
		}
		else {
			return Optional.ofNullable(object);
		}
	}

	<T> @Nullable T parse(ParsingRuleEntry<S, T> rule);

	S getReader();

	int getCursor();

	void setCursor(int cursor);

	Cut pushCutter();

	void popCutter();

	ParsingState<S> getErrorSuppressingState();
}
