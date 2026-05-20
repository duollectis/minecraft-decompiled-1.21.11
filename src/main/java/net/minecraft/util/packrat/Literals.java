package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Literals {
   static Term<StringReader> string(String string) {
      return new Literals.StringLiteral(string);
   }

   static Term<StringReader> character(char c) {
      return new Literals.CharacterLiteral(CharList.of(c)) {
         @Override
         protected boolean accepts(char c) {
            return c == c;
         }
      };
   }

   static Term<StringReader> character(char c1, char c2) {
      return new Literals.CharacterLiteral(CharList.of(c1, c2)) {
         @Override
         protected boolean accepts(char c) {
            return c == c1 || c == c2;
         }
      };
   }

   static StringReader createReader(String string, int cursor) {
      StringReader stringReader = new StringReader(string);
      stringReader.setCursor(cursor);
      return stringReader;
   }

   public abstract static class CharacterLiteral implements Term<StringReader> {
      private final CursorExceptionType<CommandSyntaxException> exception;
      private final Suggestable<StringReader> suggestions;

      public CharacterLiteral(CharList values) {
         String string = values.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
         this.exception = CursorExceptionType.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), string);
         this.suggestions = state -> values.intStream().mapToObj(Character::toString);
      }

      @Override
      public boolean matches(ParsingState<StringReader> state, ParseResults results, Cut cut) {
         state.getReader().skipWhitespace();
         int i = state.getCursor();
         if (state.getReader().canRead() && this.accepts(state.getReader().read())) {
            return true;
         } else {
            state.getErrors().add(i, this.suggestions, this.exception);
            return false;
         }
      }

      protected abstract boolean accepts(char c);
   }

   public static final class StringLiteral implements Term<StringReader> {
      private final String value;
      private final CursorExceptionType<CommandSyntaxException> exception;
      private final Suggestable<StringReader> suggestions;

      public StringLiteral(String value) {
         this.value = value;
         this.exception = CursorExceptionType.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), value);
         this.suggestions = state -> Stream.of(value);
      }

      @Override
      public boolean matches(ParsingState<StringReader> state, ParseResults results, Cut cut) {
         state.getReader().skipWhitespace();
         int i = state.getCursor();
         String string = state.getReader().readUnquotedString();
         if (!string.equals(this.value)) {
            state.getErrors().add(i, this.suggestions, this.exception);
            return false;
         } else {
            return true;
         }
      }

      @Override
      public String toString() {
         return "terminal[" + this.value + "]";
      }
   }
}
