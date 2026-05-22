package net.minecraft.util.packrat;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый элемент PEG-грамматики (Parsing Expression Grammar).
 * Каждый терм проверяет, соответствует ли текущая позиция в потоке ввода
 * некоторому шаблону, и при успехе записывает результаты в {@link ParseResults}.
 * Термы комбинируются в правила через фабричные методы этого интерфейса.
 */
public interface Term<S> {

	boolean matches(ParsingState<S> state, ParseResults results, Cut cut);

	static <S, T> Term<S> always(Symbol<T> symbol, T value) {
		return new Term.AlwaysTerm<>(symbol, value);
	}

	@SafeVarargs
	static <S> Term<S> sequence(Term<S>... terms) {
		return new Term.SequenceTerm<>(terms);
	}

	@SafeVarargs
	static <S> Term<S> anyOf(Term<S>... terms) {
		return new Term.AnyOfTerm<>(terms);
	}

	static <S> Term<S> optional(Term<S> term) {
		return new Term.OptionalTerm<>(term);
	}

	static <S, T> Term<S> repeated(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName) {
		return repeated(element, listName, 0);
	}

	static <S, T> Term<S> repeated(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, int minRepetitions) {
		return new Term.RepeatedTerm<>(element, listName, minRepetitions);
	}

	static <S, T> Term<S> repeatWithPossiblyTrailingSeparator(
			ParsingRuleEntry<S, T> element,
			Symbol<List<T>> listName,
			Term<S> separator
	) {
		return repeatWithPossiblyTrailingSeparator(element, listName, separator, 0);
	}

	static <S, T> Term<S> repeatWithPossiblyTrailingSeparator(
			ParsingRuleEntry<S, T> element,
			Symbol<List<T>> listName,
			Term<S> separator,
			int minRepetitions
	) {
		return new Term.RepeatWithSeparatorTerm<>(element, listName, separator, minRepetitions, true);
	}

	static <S, T> Term<S> repeatWithSeparator(
			ParsingRuleEntry<S, T> element,
			Symbol<List<T>> listName,
			Term<S> separator
	) {
		return repeatWithSeparator(element, listName, separator, 0);
	}

	static <S, T> Term<S> repeatWithSeparator(
			ParsingRuleEntry<S, T> element,
			Symbol<List<T>> listName,
			Term<S> separator,
			int minRepetitions
	) {
		return new Term.RepeatWithSeparatorTerm<>(element, listName, separator, minRepetitions, false);
	}

	static <S> Term<S> positiveLookahead(Term<S> term) {
		return new Term.LookaheadTerm<>(term, true);
	}

	static <S> Term<S> negativeLookahead(Term<S> term) {
		return new Term.LookaheadTerm<>(term, false);
	}

	static <S> Term<S> cutting() {
		return new Term<S>() {
			@Override
			public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
				cut.cut();
				return true;
			}

			@Override
			public String toString() {
				return "↑";
			}
		};
	}

	static <S> Term<S> epsilon() {
		return new Term<S>() {
			@Override
			public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
				return true;
			}

			@Override
			public String toString() {
				return "ε";
			}
		};
	}

	static <S> Term<S> fail(Object reason) {
		return new Term<S>() {
			@Override
			public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
				state.getErrors().add(state.getCursor(), reason);
				return false;
			}

			@Override
			public String toString() {
				return "fail";
			}
		};
	}

	/**
	 * Терм, безусловно записывающий фиксированное значение в результаты разбора.
	 */
	public record AlwaysTerm<S, T>(Symbol<T> name, T value) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			results.put(name, value);
			return true;
		}
	}

	/**
	 * Терм выбора (PEG-оператор «/»): пробует каждый вариант по порядку,
	 * возвращает первый успешный. Поддерживает отсечение через {@link Cut}.
	 */
	public record AnyOfTerm<S>(Term<S>[] elements) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			Cut innerCut = state.pushCutter();

			try {
				int savedCursor = state.getCursor();
				results.duplicateFrames();

				for (Term<S> term : elements) {
					if (term.matches(state, results, innerCut)) {
						results.chooseCurrentFrame();
						return true;
					}

					results.clearFrameValues();
					state.setCursor(savedCursor);

					if (innerCut.isCut()) {
						break;
					}
				}

				results.popFrame();
				return false;
			} finally {
				state.popCutter();
			}
		}
	}

	/**
	 * Lookahead-терм: проверяет совпадение без продвижения курсора.
	 * {@code positive=true} — позитивный lookahead (&amp;term), {@code false} — негативный (!term).
	 */
	public record LookaheadTerm<S>(Term<S> term, boolean positive) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			int savedCursor = state.getCursor();
			boolean matched = term.matches(state.getErrorSuppressingState(), results, cut);
			state.setCursor(savedCursor);
			return positive == matched;
		}
	}

	/**
	 * Опциональный терм: всегда возвращает {@code true}, откатывая курсор при неудаче.
	 */
	public record OptionalTerm<S>(Term<S> term) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			int savedCursor = state.getCursor();

			if (!term.matches(state, results, cut)) {
				state.setCursor(savedCursor);
			}

			return true;
		}
	}

	/**
	 * Терм повторения с разделителем. Параметр {@code allowTrailingSeparator} управляет
	 * допустимостью завершающего разделителя (например, запятой в конце списка).
	 */
	public record RepeatWithSeparatorTerm<S, T>(
			ParsingRuleEntry<S, T> element,
			Symbol<List<T>> listName,
			Term<S> separator,
			int minRepetitions,
			boolean allowTrailingSeparator
	) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			int startCursor = state.getCursor();
			List<T> collected = new ArrayList<>(minRepetitions);
			boolean isFirst = true;

			while (true) {
				int beforeSeparator = state.getCursor();

				if (!isFirst && !separator.matches(state, results, cut)) {
					state.setCursor(beforeSeparator);
					break;
				}

				int beforeElement = state.getCursor();
				T parsed = state.parse(element);

				if (parsed == null) {
					if (isFirst) {
						state.setCursor(beforeElement);
					} else if (!allowTrailingSeparator) {
						state.setCursor(startCursor);
						return false;
					} else {
						state.setCursor(beforeElement);
					}

					break;
				}

				collected.add(parsed);
				isFirst = false;
			}

			if (collected.size() < minRepetitions) {
				state.setCursor(startCursor);
				return false;
			}

			results.put(listName, collected);
			return true;
		}
	}

	/**
	 * Терм безразделительного повторения: собирает элементы до первой неудачи.
	 */
	public record RepeatedTerm<S, T>(
			ParsingRuleEntry<S, T> element,
			Symbol<List<T>> listName,
			int minRepetitions
	) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			int startCursor = state.getCursor();
			List<T> collected = new ArrayList<>(minRepetitions);

			while (true) {
				int beforeElement = state.getCursor();
				T parsed = state.parse(element);

				if (parsed == null) {
					state.setCursor(beforeElement);

					if (collected.size() < minRepetitions) {
						state.setCursor(startCursor);
						return false;
					}

					results.put(listName, collected);
					return true;
				}

				collected.add(parsed);
			}
		}
	}

	/**
	 * Терм последовательности (PEG-конкатенация): все элементы должны совпасть по порядку.
	 * При неудаче любого элемента курсор откатывается к началу последовательности.
	 */
	public record SequenceTerm<S>(Term<S>[] elements) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			int savedCursor = state.getCursor();

			for (Term<S> term : elements) {
				if (!term.matches(state, results, cut)) {
					state.setCursor(savedCursor);
					return false;
				}
			}

			return true;
		}
	}
}
