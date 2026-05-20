package net.minecraft.util.packrat;

import org.jspecify.annotations.Nullable;

public interface ParsingRule<S, T> {
   @Nullable T parse(ParsingState<S> state);

   static <S, T> ParsingRule<S, T> of(Term<S> term, ParsingRule.RuleAction<S, T> action) {
      return new ParsingRule.SimpleRule<>(action, term);
   }

   static <S, T> ParsingRule<S, T> of(Term<S> term, ParsingRule.StatelessAction<S, T> action) {
      return new ParsingRule.SimpleRule<>(action, term);
   }

   @FunctionalInterface
   public interface RuleAction<S, T> {
      @Nullable T run(ParsingState<S> parsingState);
   }

   public record SimpleRule<S, T>(ParsingRule.RuleAction<S, T> action, Term<S> child) implements ParsingRule<S, T> {
      @Override
      public @Nullable T parse(ParsingState<S> state) {
         ParseResults parseResults = state.getResults();
         parseResults.pushFrame();

         Object var3;
         try {
            if (!this.child.matches(state, parseResults, Cut.NOOP)) {
               return null;
            }

            var3 = this.action.run(state);
         } finally {
            parseResults.popFrame();
         }

         return (T)var3;
      }
   }

   @FunctionalInterface
   public interface StatelessAction<S, T> extends ParsingRule.RuleAction<S, T> {
      T run(ParseResults parseResults);

      @Override
      default T run(ParsingState<S> parsingState) {
         return this.run(parsingState.getResults());
      }
   }
}
