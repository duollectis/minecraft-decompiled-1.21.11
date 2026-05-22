package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Packrat-парсер с мемоизацией для грамматик PEG (Parsing Expression Grammar).
 * Гарантирует линейное время разбора за счёт кэширования результатов промежуточных правил.
 * Поддерживает автодополнение через {@link #listSuggestions}.
 */
public record PackratParser<T>(
		ParsingRules<StringReader> rules,
		ParsingRuleEntry<StringReader, T> top
) implements Parser<T> {

	public PackratParser(ParsingRules<StringReader> rules, ParsingRuleEntry<StringReader, T> top) {
		rules.ensureBound();
		this.rules = rules;
		this.top = top;
	}

	public Optional<T> startParsing(ParsingState<StringReader> state) {
		return state.startParsing(top);
	}

	@Override
	public T parse(StringReader reader) throws CommandSyntaxException {
		ParseErrorList.Impl<StringReader> errors = new ParseErrorList.Impl<>();
		ReaderBackedParsingState state = new ReaderBackedParsingState(errors, reader);
		Optional<T> result = startParsing(state);

		if (result.isPresent()) {
			return result.get();
		}

		List<ParseError<StringReader>> parseErrors = errors.getErrors();
		List<Exception> exceptions = parseErrors.stream()
				.<Exception>mapMulti((error, callback) -> {
					if (error.reason() instanceof CursorExceptionType<?> cursorExceptionType) {
						callback.accept(cursorExceptionType.create(reader.getString(), error.cursor()));
					} else if (error.reason() instanceof Exception exception) {
						callback.accept(exception);
					}
				})
				.toList();

		for (Exception exception : exceptions) {
			if (exception instanceof CommandSyntaxException commandSyntaxException) {
				throw commandSyntaxException;
			}
		}

		if (exceptions.size() == 1 && exceptions.get(0) instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}

		throw new IllegalStateException(
				"Failed to parse: " + parseErrors.stream()
						.map(ParseError::toString)
						.collect(Collectors.joining(", "))
		);
	}

	@Override
	public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());

		ParseErrorList.Impl<StringReader> errors = new ParseErrorList.Impl<>();
		ReaderBackedParsingState state = new ReaderBackedParsingState(errors, reader);
		startParsing(state);

		List<ParseError<StringReader>> parseErrors = errors.getErrors();

		if (parseErrors.isEmpty()) {
			return builder.buildFuture();
		}

		SuggestionsBuilder offsetBuilder = builder.createOffset(errors.getCursor());

		for (ParseError<StringReader> parseError : parseErrors) {
			if (parseError.suggestions() instanceof IdentifierSuggestable identifierSuggestable) {
				CommandSource.suggestIdentifiers(identifierSuggestable.possibleIds(), offsetBuilder);
			} else {
				CommandSource.suggestMatching(
						parseError.suggestions().possibleValues(state),
						offsetBuilder
				);
			}
		}

		return offsetBuilder.buildFuture();
	}
}
