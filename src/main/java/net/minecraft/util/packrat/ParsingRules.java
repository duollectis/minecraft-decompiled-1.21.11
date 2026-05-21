package net.minecraft.util.packrat;

import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code ParsingRules}.
 */
public class ParsingRules<S> {

	private final Map<Symbol<?>, ParsingRules.RuleEntryImpl<S, ?>> rules = new IdentityHashMap<>();

	/**
	 * Set.
	 *
	 * @param symbol symbol
	 * @param rule rule
	 *
	 * @return ParsingRuleEntry — результат операции
	 */
	public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, ParsingRule<S, T> rule) {
		ParsingRules.RuleEntryImpl<S, T>
				ruleEntryImpl =
				(ParsingRules.RuleEntryImpl<S, T>) this.rules.computeIfAbsent(symbol, ParsingRules.RuleEntryImpl::new);
		if (ruleEntryImpl.rule != null) {
			throw new IllegalArgumentException("Trying to override rule: " + symbol);
		}
		else {
			ruleEntryImpl.rule = rule;
			return ruleEntryImpl;
		}
	}

	/**
	 * Set.
	 *
	 * @param symbol symbol
	 * @param term term
	 * @param action action
	 *
	 * @return ParsingRuleEntry — результат операции
	 */
	public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, Term<S> term, ParsingRule.RuleAction<S, T> action) {
		return this.set(symbol, ParsingRule.of(term, action));
	}

	/**
	 * Set.
	 *
	 * @param symbol symbol
	 * @param term term
	 * @param action action
	 *
	 * @return ParsingRuleEntry — результат операции
	 */
	public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, Term<S> term, ParsingRule.StatelessAction<S, T> action) {
		return this.set(symbol, ParsingRule.of(term, action));
	}

	/**
	 * Ensure bound.
	 */
	public void ensureBound() {
		List<? extends Symbol<?>>
				list =
				this.rules
						.entrySet()
						.stream()
						.filter(entry -> entry.getValue().rule == null)
						.map(Entry::getKey)
						.toList();
		if (!list.isEmpty()) {
			throw new IllegalStateException("Unbound names: " + list);
		}
	}

	/**
	 * Get.
	 *
	 * @param symbol symbol
	 *
	 * @return ParsingRuleEntry — 
	 */
	public <T> ParsingRuleEntry<S, T> get(Symbol<T> symbol) {
		return (ParsingRuleEntry<S, T>) Objects.requireNonNull(
				this.rules.get(symbol),
				() -> "No rule called " + symbol
		);
	}

	public <T> ParsingRuleEntry<S, T> getOrCreate(Symbol<T> symbol) {
		return this.getOrCreateInternal(symbol);
	}

	private <T> ParsingRules.RuleEntryImpl<S, T> getOrCreateInternal(Symbol<T> symbol) {
		return (ParsingRules.RuleEntryImpl<S, T>) this.rules.computeIfAbsent(symbol, ParsingRules.RuleEntryImpl::new);
	}

	/**
	 * Term.
	 *
	 * @param symbol symbol
	 *
	 * @return Term — результат операции
	 */
	public <T> Term<S> term(Symbol<T> symbol) {
		return new ParsingRules.RuleTerm<>(this.getOrCreateInternal(symbol), symbol);
	}

	/**
	 * Term.
	 *
	 * @param symbol symbol
	 * @param nameToStore name to store
	 *
	 * @return Term — результат операции
	 */
	public <T> Term<S> term(Symbol<T> symbol, Symbol<T> nameToStore) {
		return new ParsingRules.RuleTerm<>(this.getOrCreateInternal(symbol), nameToStore);
	}

	/**
	 * {@code RuleEntryImpl}.
	 */
	static class RuleEntryImpl<S, T> implements ParsingRuleEntry<S, T>, Supplier<String> {

		private final Symbol<T> symbol;
		@Nullable ParsingRule<S, T> rule;

		private RuleEntryImpl(Symbol<T> symbol) {
			this.symbol = symbol;
		}

		@Override
		public Symbol<T> getSymbol() {
			return this.symbol;
		}

		@Override
		public ParsingRule<S, T> getRule() {
			return Objects.requireNonNull(this.rule, this);
		}

		/**
		 * Get.
		 *
		 * @return String — 
		 */
		public String get() {
			return "Unbound rule " + this.symbol;
		}
	}

	/**
	 * {@code RuleTerm}.
	 */
	record RuleTerm<S, T>(ParsingRules.RuleEntryImpl<S, T> ruleToParse, Symbol<T> nameToStore) implements Term<S> {

		@Override
		public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
			T object = state.parse(this.ruleToParse);
			if (object == null) {
				return false;
			}
			else {
				results.put(this.nameToStore, object);
				return true;
			}
		}
	}
}
