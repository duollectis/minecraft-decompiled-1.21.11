package net.minecraft.util.packrat;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Состояние парсера: хранит текущую позицию в потоке ввода, результаты разбора
 * и список ошибок. Предоставляет механизм отсечения (cut) для управления откатом.
 */
public interface ParsingState<S> {

	ParseResults getResults();

	ParseErrorList<S> getErrors();

	default <T> Optional<T> startParsing(ParsingRuleEntry<S, T> rule) {
		T result = parse(rule);

		if (result != null) {
			getErrors().setCursor(getCursor());
		}

		if (!getResults().areFramesPlacedCorrectly()) {
			throw new IllegalStateException("Malformed scope: " + getResults());
		}

		return Optional.ofNullable(result);
	}

	<T> @Nullable T parse(ParsingRuleEntry<S, T> rule);

	S getReader();

	int getCursor();

	void setCursor(int cursor);

	Cut pushCutter();

	void popCutter();

	ParsingState<S> getErrorSuppressingState();
}
