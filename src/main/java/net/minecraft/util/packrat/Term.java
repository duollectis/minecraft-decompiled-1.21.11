package net.minecraft.util.packrat;

import java.util.ArrayList;
import java.util.List;

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

   static <S, T> Term<S> repeatWithPossiblyTrailingSeparator(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, Term<S> separator) {
      return repeatWithPossiblyTrailingSeparator(element, listName, separator, 0);
   }

   static <S, T> Term<S> repeatWithPossiblyTrailingSeparator(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, Term<S> separator, int minRepetitions) {
      return new Term.RepeatWithSeparatorTerm<>(element, listName, separator, minRepetitions, true);
   }

   static <S, T> Term<S> repeatWithSeparator(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, Term<S> separator) {
      return repeatWithSeparator(element, listName, separator, 0);
   }

   static <S, T> Term<S> repeatWithSeparator(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, Term<S> separator, int minRepetitions) {
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

   public record AlwaysTerm<S, T>(Symbol<T> name, T value) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         results.put(this.name, this.value);
         return true;
      }
   }

   public record AnyOfTerm<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         Cut cut2 = state.pushCutter();

         try {
            int i = state.getCursor();
            results.duplicateFrames();

            for (Term<S> term : this.elements) {
               if (term.matches(state, results, cut2)) {
                  results.chooseCurrentFrame();
                  return true;
               }

               results.clearFrameValues();
               state.setCursor(i);
               if (cut2.isCut()) {
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

   public record LookaheadTerm<S>(Term<S> term, boolean positive) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         int i = state.getCursor();
         boolean bl = this.term.matches(state.getErrorSuppressingState(), results, cut);
         state.setCursor(i);
         return this.positive == bl;
      }
   }

   public record OptionalTerm<S>(Term<S> term) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         int i = state.getCursor();
         if (!this.term.matches(state, results, cut)) {
            state.setCursor(i);
         }

         return true;
      }
   }

   public record RepeatWithSeparatorTerm<S, T>(
      ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, Term<S> separator, int minRepetitions, boolean allowTrailingSeparator
   ) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         int i = state.getCursor();
         List<T> list = new ArrayList<>(this.minRepetitions);
         boolean bl = true;

         while (true) {
            int j = state.getCursor();
            if (!bl && !this.separator.matches(state, results, cut)) {
               state.setCursor(j);
               break;
            }

            int k = state.getCursor();
            T object = state.parse(this.element);
            if (object == null) {
               if (bl) {
                  state.setCursor(k);
               } else {
                  if (!this.allowTrailingSeparator) {
                     state.setCursor(i);
                     return false;
                  }

                  state.setCursor(k);
               }
               break;
            }

            list.add(object);
            bl = false;
         }

         if (list.size() < this.minRepetitions) {
            state.setCursor(i);
            return false;
         } else {
            results.put(this.listName, list);
            return true;
         }
      }
   }

   public record RepeatedTerm<S, T>(ParsingRuleEntry<S, T> element, Symbol<List<T>> listName, int minRepetitions) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         int i = state.getCursor();
         List<T> list = new ArrayList<>(this.minRepetitions);

         while (true) {
            int j = state.getCursor();
            T object = state.parse(this.element);
            if (object == null) {
               state.setCursor(j);
               if (list.size() < this.minRepetitions) {
                  state.setCursor(i);
                  return false;
               } else {
                  results.put(this.listName, list);
                  return true;
               }
            }

            list.add(object);
         }
      }
   }

   public record SequenceTerm<S>(Term<S>[] elements) implements Term<S> {
      @Override
      public boolean matches(ParsingState<S> state, ParseResults results, Cut cut) {
         int i = state.getCursor();

         for (Term<S> term : this.elements) {
            if (!term.matches(state, results, cut)) {
               state.setCursor(i);
               return false;
            }
         }

         return true;
      }
   }
}
