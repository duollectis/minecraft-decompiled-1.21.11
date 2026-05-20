package net.minecraft.util.packrat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class ParsingRules<S> {
   private final Map<Symbol<?>, ParsingRules.RuleEntryImpl<S, ?>> rules = new IdentityHashMap<>();

   public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, ParsingRule<S, T> rule) {
      ParsingRules.RuleEntryImpl<S, T> ruleEntryImpl = (ParsingRules.RuleEntryImpl<S, T>)this.rules.computeIfAbsent(symbol, ParsingRules.RuleEntryImpl::new);
      if (ruleEntryImpl.rule != null) {
         throw new IllegalArgumentException("Trying to override rule: " + symbol);
      } else {
         ruleEntryImpl.rule = rule;
         return ruleEntryImpl;
      }
   }

   public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, Term<S> term, ParsingRule.RuleAction<S, T> action) {
      return this.set(symbol, ParsingRule.of(term, action));
   }

   public <T> ParsingRuleEntry<S, T> set(Symbol<T> symbol, Term<S> term, ParsingRule.StatelessAction<S, T> action) {
      return this.set(symbol, ParsingRule.of(term, action));
   }

   public void ensureBound() {
      List<? extends Symbol<?>> list = this.rules.entrySet().stream().filter(entry -> entry.getValue().rule == null).map(Entry::getKey).toList();
      if (!list.isEmpty()) {
         throw new IllegalStateException("Unbound names: " + list);
      }
   }

   public <T> ParsingRuleEntry<S, T> get(Symbol<T> symbol) {
      return (ParsingRuleEntry<S, T>)Objects.requireNonNull(this.rules.get(symbol), () -> "No rule called " + symbol);
   }

   public <T> ParsingRuleEntry<S, T> getOrCreate(Symbol<T> symbol) {
      return this.getOrCreateInternal(symbol);
   }

   private <T> ParsingRules.RuleEntryImpl<S, T> getOrCreateInternal(Symbol<T> symbol) {
      return (ParsingRules.RuleEntryImpl<S, T>)this.rules.computeIfAbsent(symbol, ParsingRules.RuleEntryImpl::new);
   }

   public <T> Term<S> term(Symbol<T> symbol) {
      return new ParsingRules.RuleTerm<>(this.getOrCreateInternal(symbol), symbol);
   }

   public <T> Term<S> term(Symbol<T> symbol, Symbol<T> nameToStore) {
      return new ParsingRules.RuleTerm<>(this.getOrCreateInternal(symbol), nameToStore);
   }

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

      public String get() {
         return "Unbound rule " + this.symbol;
      }
   }

   record RuleTerm<S, T>(ParsingRules.RuleEntryImpl<S, T> ruleToParse, Symbol<T> nameToStore) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         T object = state.parse(this.ruleToParse);
         if (object == null) {
            return false;
         } else {
            results.put(this.nameToStore, object);
            return true;
         }
      }
   }
}
