package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Интерфейс парсера, преобразующего строку в типизированный результат.
 * Поддерживает автодополнение и декодирование через {@link Codec}.
 */
public interface Parser<T> {

	T parse(StringReader reader) throws CommandSyntaxException;

	CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder);

	default <S> Parser<S> map(Function<T, S> mapper) {
		return new Parser<>() {
			@Override
			public S parse(StringReader reader) throws CommandSyntaxException {
				return mapper.apply(Parser.this.parse(reader));
			}

			@Override
			public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder) {
				return Parser.this.listSuggestions(builder);
			}
		};
	}

	default <R, O> Parser<R> withDecoding(
			DynamicOps<O> ops,
			Parser<O> encodedParser,
			Codec<R> codec,
			DynamicCommandExceptionType invalidDataError
	) {
		return new Parser<>() {
			@Override
			public R parse(StringReader reader) throws CommandSyntaxException {
				int cursor = reader.getCursor();
				O encoded = encodedParser.parse(reader);
				DataResult<R> dataResult = codec.parse(ops, encoded);
				return dataResult.getOrThrow(error -> {
					reader.setCursor(cursor);
					return invalidDataError.createWithContext(reader, error);
				});
			}

			@Override
			public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder) {
				return Parser.this.listSuggestions(builder);
			}
		};
	}
}
