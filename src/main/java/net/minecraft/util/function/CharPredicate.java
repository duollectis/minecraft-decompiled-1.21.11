package net.minecraft.util.function;

import java.util.Objects;

/**
 * Предикат для одиночного символа {@code char}. Аналог {@link java.util.function.Predicate},
 * но без упаковки примитива в {@link Character}.
 */
@FunctionalInterface
public interface CharPredicate {

	boolean test(char c);

	default CharPredicate and(CharPredicate predicate) {
		Objects.requireNonNull(predicate);
		return c -> test(c) && predicate.test(c);
	}

	default CharPredicate negate() {
		return c -> !test(c);
	}

	default CharPredicate or(CharPredicate predicate) {
		Objects.requireNonNull(predicate);
		return c -> test(c) || predicate.test(c);
	}
}
