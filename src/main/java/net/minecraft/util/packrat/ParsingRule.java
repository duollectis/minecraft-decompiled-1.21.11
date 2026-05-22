package net.minecraft.util.packrat;

import org.jspecify.annotations.Nullable;

/**
 * Правило разбора, которое принимает состояние парсера и возвращает
 * типизированный результат или {@code null} при неудаче.
 */
public interface ParsingRule<S, T> {

	@Nullable T parse(ParsingState<S> state);

	static <S, T> ParsingRule<S, T> of(Term<S> term, RuleAction<S, T> action) {
		return new SimpleRule<>(action, term);
	}

	static <S, T> ParsingRule<S, T> of(Term<S> term, StatelessAction<S, T> action) {
		return new SimpleRule<>(action, term);
	}

	/**
	 * Действие правила, имеющее доступ к полному состоянию парсера.
	 */
	@FunctionalInterface
	interface RuleAction<S, T> {

		@Nullable T run(ParsingState<S> state);
	}

	/**
	 * Правило, объединяющее терминальный символ и действие.
	 * Управляет фреймом результатов: открывает перед проверкой и закрывает после.
	 */
	record SimpleRule<S, T>(RuleAction<S, T> action, Term<S> child) implements ParsingRule<S, T> {

		@Override
		public @Nullable T parse(ParsingState<S> state) {
			ParseResults results = state.getResults();
			results.pushFrame();

			try {
				if (!child.matches(state, results, Cut.NOOP)) {
					return null;
				}

				return action.run(state);
			} finally {
				results.popFrame();
			}
		}
	}

	/**
	 * Действие правила, работающее только с результатами разбора без доступа к состоянию.
	 */
	@FunctionalInterface
	interface StatelessAction<S, T> extends RuleAction<S, T> {

		T run(ParseResults results);

		@Override
		default T run(ParsingState<S> state) {
			return run(state.getResults());
		}
	}
}
