package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

public abstract class NumeralParsingRule implements ParsingRule<StringReader, String> {
   private final CursorExceptionType<CommandSyntaxException> invalidCharException;
   private final CursorExceptionType<CommandSyntaxException> unexpectedUnderscoreException;

   public NumeralParsingRule(
      CursorExceptionType<CommandSyntaxException> invalidCharException, CursorExceptionType<CommandSyntaxException> unexpectedUnderscoreException
   ) {
      this.invalidCharException = invalidCharException;
      this.unexpectedUnderscoreException = unexpectedUnderscoreException;
   }

   public @Nullable String parse(ParsingState<StringReader> parsingState) {
      StringReader stringReader = parsingState.getReader();
      stringReader.skipWhitespace();
      String string = stringReader.getString();
      int i = stringReader.getCursor();
      int j = i;

      while (j < string.length() && this.accepts(string.charAt(j))) {
         j++;
      }

      int k = j - i;
      if (k == 0) {
         parsingState.getErrors().add(parsingState.getCursor(), this.invalidCharException);
         return null;
      } else if (string.charAt(i) != '_' && string.charAt(j - 1) != '_') {
         stringReader.setCursor(j);
         return string.substring(i, j);
      } else {
         parsingState.getErrors().add(parsingState.getCursor(), this.unexpectedUnderscoreException);
         return null;
      }
   }

   protected abstract boolean accepts(char c);
}
