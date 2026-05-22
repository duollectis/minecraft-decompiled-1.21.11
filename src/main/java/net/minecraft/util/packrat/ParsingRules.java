package net.minecraft.util.packrat;

import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Реестр правил разбора, связывающий {@link Symbol} с соответствующими {@link ParsingRule}.
 * Поддерживает ленивую регистрацию через {@link #getOrCreate} для взаимно рекурсивных правил.
 */
public class ParsingRules<S> {

	private final Map<Symbol<?>, RuleEntryImpl<S, ?>> rules = new IdentityHashMap<>();

	public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, ParsingRule<S, T> rule) {
		RuleEntryImpl<S, T> entry = (RuleEntryImpl<S, T>) rules.computeIfAbsent(symbol, RuleEntryImpl::new);

		if (entry.rule != null) {
			throw new IllegalArgumentException("Trying to override rule: " + symbol);
		}

		entry.rule = rule;
		return entry;
	}

	public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, Term<S> term, ParsingRule.RuleAction<S, T> action) {
		return set(symbol, ParsingRule.of(term, action));
	}

	public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, Term<S> term, ParsingRule.StatelessAction<S, T> action) {
		return set(symbol, ParsingRule.of(term, action));
	}

	public void ensureBound() {
		List<? extends Symbol<?>> unbound = rules.entrySet()
				.stream()
				.filter(entry -> entry.getValue().rule == null)
				.map(Entry::getKey)
				.toList();

		if (!unbound.isEmpty()) {
			throw new IllegalStateException("Unbound names: " + unbound);
		}
	}

	public <T> ParsingRuleEntry<S, T> get(Symbol<T> symbol) {
		return (ParsingRuleEntry<S, T>) Objects.requireNonNull(
				rules.get(symbol),
				() -> "No rule called " + symbol
		);
	}

	public <T> ParsingRuleEntry<S, T> getOrCreate(Symbol<T> symbol) {
		return getOrCreateInternal(symbol);
	}

	private <T> RuleEntryImpl<S, T> getOrCreateInternal(Symbol<T> symbol) {
		return (RuleEntryImpl<S, T>) rules.computeIfAbsent(symbol, RuleEntryImpl::new);
	}

	public <T> Term<S> term(Symbol<T> symbol) {
		return new RuleTerm<>(getOrCreateInternal(symbol), symbol);
	}

	public <T> Term<S> term(Symbol<T> symbol, Symbol<T> nameToStore) {
		return new RuleTerm<>(getOrCreateInternal(symbol), nameToStore);
	}

	static class RuleEntryImpl<S, T> implements ParsingRuleEntry<S, T>, Supplier<String> {

		private final Symbol<T> symbol;
		@Nullable ParsingRule<S, T> rule;

		private RuleEntryImpl(Symbol<T> symbol) {
			this.symbol = symbol;
		}

		@Override
		public Symbol<T> getSymbol() {
			return symbol;
		}

		@Override
		public ParsingRule<S, T> getRule() {
			return Objects.requireNonNull(rule, this);
		}

		@Override
		public String get() {
			return "Unbound rule " + symbol;
		}
	}

	record RuleTerm<S, T>(RuleEntryImpl<S, T> ruleToParse, Symbol<T> nameToStore) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			T value = state.parse(ruleToParse);

			if (value == null) {
				return false;
			}

			results.put(nameToStore, value);
			return true;
		}
	}
}
