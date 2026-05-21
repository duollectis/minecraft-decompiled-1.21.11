package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.packrat.Parser;

import java.util.concurrent.CompletableFuture;

/**
 * {@code ParserBackedArgumentType}.
 */
public abstract class ParserBackedArgumentType<T> implements ArgumentType<T> {

	private final Parser<T> parser;

	public ParserBackedArgumentType(Parser<T> parser) {
		this.parser = parser;
	}

	public T parse(StringReader reader) throws CommandSyntaxException {
		return this.parser.parse(reader);
	}

	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return this.parser.listSuggestions(builder);
	}
}
