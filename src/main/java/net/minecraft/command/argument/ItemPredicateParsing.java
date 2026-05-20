package net.minecraft.command.argument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtParsingRule;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.packrat.AnyIdParsingRule;
import net.minecraft.util.packrat.IdentifiableParsingRule;
import net.minecraft.util.packrat.Literals;
import net.minecraft.util.packrat.PackratParser;
import net.minecraft.util.packrat.ParseResults;
import net.minecraft.util.packrat.ParsingRule;
import net.minecraft.util.packrat.ParsingRuleEntry;
import net.minecraft.util.packrat.ParsingRules;
import net.minecraft.util.packrat.ParsingState;
import net.minecraft.util.packrat.Symbol;
import net.minecraft.util.packrat.Term;

public class ItemPredicateParsing {
   public static <T, C, P> PackratParser<List<T>> createParser(ItemPredicateParsing.Callbacks<T, C, P> callbacks) {
      Symbol<List<T>> symbol = Symbol.of("top");
      Symbol<Optional<T>> symbol2 = Symbol.of("type");
      Symbol<Unit> symbol3 = Symbol.of("any_type");
      Symbol<T> symbol4 = Symbol.of("element_type");
      Symbol<T> symbol5 = Symbol.of("tag_type");
      Symbol<List<T>> symbol6 = Symbol.of("conditions");
      Symbol<List<T>> symbol7 = Symbol.of("alternatives");
      Symbol<T> symbol8 = Symbol.of("term");
      Symbol<T> symbol9 = Symbol.of("negation");
      Symbol<T> symbol10 = Symbol.of("test");
      Symbol<C> symbol11 = Symbol.of("component_type");
      Symbol<P> symbol12 = Symbol.of("predicate_type");
      Symbol<Identifier> symbol13 = Symbol.of("id");
      Symbol<Dynamic<?>> symbol14 = Symbol.of("tag");
      ParsingRules<StringReader> parsingRules = new ParsingRules<>();
      ParsingRuleEntry<StringReader, Identifier> parsingRuleEntry = parsingRules.set(symbol13, AnyIdParsingRule.INSTANCE);
      ParsingRuleEntry<StringReader, List<T>> parsingRuleEntry2 = parsingRules.set(
         symbol,
         Term.anyOf(
            Term.sequence(
               parsingRules.term(symbol2), Literals.character('['), Term.cutting(), Term.optional(parsingRules.term(symbol6)), Literals.character(']')
            ),
            parsingRules.term(symbol2)
         ),
         results -> {
            Builder<T> builder = ImmutableList.builder();
            results.getOrThrow(symbol2).ifPresent(builder::add);
            List<T> list = results.get(symbol6);
            if (list != null) {
               builder.addAll(list);
            }

            return builder.build();
         }
      );
      parsingRules.set(
         symbol2,
         Term.anyOf(parsingRules.term(symbol4), Term.sequence(Literals.character('#'), Term.cutting(), parsingRules.term(symbol5)), parsingRules.term(symbol3)),
         results -> Optional.ofNullable(results.getAny(symbol4, symbol5))
      );
      parsingRules.set(symbol3, Literals.character('*'), results -> Unit.INSTANCE);
      parsingRules.set(symbol4, new ItemPredicateParsing.ItemParsingRule<>(parsingRuleEntry, callbacks));
      parsingRules.set(symbol5, new ItemPredicateParsing.TagParsingRule<>(parsingRuleEntry, callbacks));
      parsingRules.set(
         symbol6, Term.sequence(parsingRules.term(symbol7), Term.optional(Term.sequence(Literals.character(','), parsingRules.term(symbol6)))), results -> {
            T object = callbacks.anyOf(results.getOrThrow(symbol7));
            return Optional.ofNullable(results.get(symbol6)).map(predicates -> Util.withPrepended(object, (List<T>)predicates)).orElse(List.of(object));
         }
      );
      parsingRules.set(
         symbol7, Term.sequence(parsingRules.term(symbol8), Term.optional(Term.sequence(Literals.character('|'), parsingRules.term(symbol7)))), results -> {
            T object = results.getOrThrow(symbol8);
            return Optional.ofNullable(results.get(symbol7)).map(predicates -> Util.withPrepended(object, (List<T>)predicates)).orElse(List.of(object));
         }
      );
      parsingRules.set(
         symbol8,
         Term.anyOf(parsingRules.term(symbol10), Term.sequence(Literals.character('!'), parsingRules.term(symbol9))),
         results -> results.getAnyOrThrow(symbol10, symbol9)
      );
      parsingRules.set(symbol9, parsingRules.term(symbol10), results -> callbacks.negate(results.getOrThrow(symbol10)));
      parsingRules.set(
         symbol10,
         Term.anyOf(
            Term.sequence(parsingRules.term(symbol11), Literals.character('='), Term.cutting(), parsingRules.term(symbol14)),
            Term.sequence(parsingRules.term(symbol12), Literals.character('~'), Term.cutting(), parsingRules.term(symbol14)),
            parsingRules.term(symbol11)
         ),
         (ParsingRule.RuleAction<StringReader, T>) state -> {
            ParseResults parseResults = state.getResults();
            P object = parseResults.get(symbol12);

            try {
               if (object != null) {
                  Dynamic<?> dynamic = parseResults.getOrThrow(symbol14);
                  return callbacks.subPredicatePredicate((ImmutableStringReader)state.getReader(), object, dynamic);
               } else {
                  C object2 = parseResults.getOrThrow(symbol11);
                  Dynamic<?> dynamic2 = parseResults.get(symbol14);
                  return dynamic2 != null
                     ? callbacks.componentMatchPredicate((ImmutableStringReader)state.getReader(), object2, dynamic2)
                     : callbacks.componentPresencePredicate((ImmutableStringReader)state.getReader(), object2);
               }
            } catch (CommandSyntaxException var9x) {
               state.getErrors().add(state.getCursor(), var9x);
               return null;
            }
         }
      );
      parsingRules.set(symbol11, new ItemPredicateParsing.ComponentParsingRule<>(parsingRuleEntry, callbacks));
      parsingRules.set(symbol12, new ItemPredicateParsing.SubPredicateParsingRule<>(parsingRuleEntry, callbacks));
      parsingRules.set(symbol14, new NbtParsingRule(NbtOps.INSTANCE));
      return new PackratParser<>(parsingRules, parsingRuleEntry2);
   }

   public interface Callbacks<T, C, P> {
      T itemMatchPredicate(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> streamItemIds();

      T tagMatchPredicate(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> streamTags();

      C componentCheck(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> streamComponentIds();

      T componentMatchPredicate(ImmutableStringReader reader, C check, Dynamic<?> dynamic) throws CommandSyntaxException;

      T componentPresencePredicate(ImmutableStringReader reader, C check);

      P subPredicateCheck(ImmutableStringReader reader, Identifier id) throws CommandSyntaxException;

      Stream<Identifier> streamSubPredicateIds();

      T subPredicatePredicate(ImmutableStringReader reader, P check, Dynamic<?> dynamic) throws CommandSyntaxException;

      T negate(T predicate);

      T anyOf(List<T> predicates);
   }

   static class ComponentParsingRule<T, C, P> extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, C> {
      ComponentParsingRule(ParsingRuleEntry<StringReader, Identifier> idParsingRule, ItemPredicateParsing.Callbacks<T, C, P> callbacks) {
         super(idParsingRule, callbacks);
      }

      @Override
      protected C parse(ImmutableStringReader reader, Identifier id) throws Exception {
         return this.callbacks.componentCheck(reader, id);
      }

      @Override
      public Stream<Identifier> possibleIds() {
         return this.callbacks.streamComponentIds();
      }
   }

   static class ItemParsingRule<T, C, P> extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, T> {
      ItemParsingRule(ParsingRuleEntry<StringReader, Identifier> idParsingRule, ItemPredicateParsing.Callbacks<T, C, P> callbacks) {
         super(idParsingRule, callbacks);
      }

      @Override
      protected T parse(ImmutableStringReader reader, Identifier id) throws Exception {
         return this.callbacks.itemMatchPredicate(reader, id);
      }

      @Override
      public Stream<Identifier> possibleIds() {
         return this.callbacks.streamItemIds();
      }
   }

   static class SubPredicateParsingRule<T, C, P> extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, P> {
      SubPredicateParsingRule(ParsingRuleEntry<StringReader, Identifier> idParsingRule, ItemPredicateParsing.Callbacks<T, C, P> callbacks) {
         super(idParsingRule, callbacks);
      }

      @Override
      protected P parse(ImmutableStringReader reader, Identifier id) throws Exception {
         return this.callbacks.subPredicateCheck(reader, id);
      }

      @Override
      public Stream<Identifier> possibleIds() {
         return this.callbacks.streamSubPredicateIds();
      }
   }

   static class TagParsingRule<T, C, P> extends IdentifiableParsingRule<ItemPredicateParsing.Callbacks<T, C, P>, T> {
      TagParsingRule(ParsingRuleEntry<StringReader, Identifier> idParsingRule, ItemPredicateParsing.Callbacks<T, C, P> callbacks) {
         super(idParsingRule, callbacks);
      }

      @Override
      protected T parse(ImmutableStringReader reader, Identifier id) throws Exception {
         return this.callbacks.tagMatchPredicate(reader, id);
      }

      @Override
      public Stream<Identifier> possibleIds() {
         return this.callbacks.streamTags();
      }
   }
}
