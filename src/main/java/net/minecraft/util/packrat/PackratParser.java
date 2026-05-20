package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.command.CommandSource;

public record PackratParser<T>(ParsingRules<StringReader> rules, ParsingRuleEntry<StringReader, T> top) implements Parser<T> {
   public PackratParser(ParsingRules<StringReader> rules, ParsingRuleEntry<StringReader, T> top) {
      rules.ensureBound();
      this.rules = rules;
      this.top = top;
   }

   public Optional<T> startParsing(ParsingState<StringReader> state) {
      return state.startParsing(this.top);
   }

   @Override
   public T parse(StringReader reader) throws CommandSyntaxException {
      ParseErrorList.Impl<StringReader> impl = new ParseErrorList.Impl<>();
      ReaderBackedParsingState readerBackedParsingState = new ReaderBackedParsingState(impl, reader);
      Optional<T> optional = this.startParsing(readerBackedParsingState);
      if (optional.isPresent()) {
         return optional.get();
      } else {
         List<ParseError<StringReader>> list = impl.getErrors();
         List<Exception> list2 = list.stream().<Exception>mapMulti((error, callback) -> {
            if (error.reason() instanceof CursorExceptionType<?> cursorExceptionType) {
               callback.accept(cursorExceptionType.create(reader.getString(), error.cursor()));
            } else if (error.reason() instanceof Exception exceptionx) {
               callback.accept(exceptionx);
            }
         }).toList();

         for (Exception exception : list2) {
            if (exception instanceof CommandSyntaxException commandSyntaxException) {
               throw commandSyntaxException;
            }
         }

         if (list2.size() == 1 && list2.get(0) instanceof RuntimeException runtimeException) {
            throw runtimeException;
         } else {
            throw new IllegalStateException("Failed to parse: " + list.stream().map(ParseError::toString).collect(Collectors.joining(", ")));
         }
      }
   }

   @Override
   public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder) {
      StringReader stringReader = new StringReader(builder.getInput());
      stringReader.setCursor(builder.getStart());
      ParseErrorList.Impl<StringReader> impl = new ParseErrorList.Impl<>();
      ReaderBackedParsingState readerBackedParsingState = new ReaderBackedParsingState(impl, stringReader);
      this.startParsing(readerBackedParsingState);
      List<ParseError<StringReader>> list = impl.getErrors();
      if (list.isEmpty()) {
         return builder.buildFuture();
      } else {
         SuggestionsBuilder suggestionsBuilder = builder.createOffset(impl.getCursor());

         for (ParseError<StringReader> parseError : list) {
            if (parseError.suggestions() instanceof IdentifierSuggestable identifierSuggestable) {
               CommandSource.suggestIdentifiers(identifierSuggestable.possibleIds(), suggestionsBuilder);
            } else {
               CommandSource.suggestMatching(parseError.suggestions().possibleValues(readerBackedParsingState), suggestionsBuilder);
            }
         }

         return suggestionsBuilder.buildFuture();
      }
   }
}
