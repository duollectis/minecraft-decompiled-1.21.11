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
 * {@code Parser}.
 */
public interface Parser<T> {

	T parse(StringReader reader) throws CommandSyntaxException;

	CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder);

	default <S> Parser<S> map(Function<T, S> mapper) {
		return new Parser<S>() {
			@Override
			public S parse(StringReader reader) throws CommandSyntaxException {
				return mapper.apply((T) Parser.this.parse(reader));
			}

			@Override
			public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder) {
				return Parser.this.listSuggestions(builder);
			}
		};
	}

	default <T, O> Parser<T> withDecoding(
			DynamicOps<O> ops,
			Parser<O> encodedParser,
			Codec<T> codec,
			DynamicCommandExceptionType invalidDataError
	) {
		return new Parser<T>() {
			@Override
			public T parse(StringReader reader) throws CommandSyntaxException {
				int i = reader.getCursor();
				O object = encodedParser.parse(reader);
				DataResult<T> dataResult = codec.parse(ops, object);
				return (T) dataResult.getOrThrow(error -> {
					reader.setCursor(i);
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
