package net.minecraft.command.argument;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtParsing;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.packrat.*;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code RegistryEntryArgumentType}.
 */
public class RegistryEntryArgumentType<T> implements ArgumentType<RegistryEntry<T>> {

	private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
	public static final DynamicCommandExceptionType FAILED_TO_PARSE_EXCEPTION = new DynamicCommandExceptionType(
			argument -> Text.stringifiedTranslatable("argument.resource_or_id.failed_to_parse", argument)
	);
	public static final Dynamic2CommandExceptionType NO_SUCH_ELEMENT_EXCEPTION = new Dynamic2CommandExceptionType(
			(key, registryRef) -> Text.stringifiedTranslatable(
					"argument.resource_or_id.no_such_element",
					key,
					registryRef
			)
	);
	public static final DynamicOps<NbtElement> OPS = NbtOps.INSTANCE;
	private final RegistryWrapper.WrapperLookup registries;
	private final Optional<? extends RegistryWrapper.Impl<T>> registry;
	private final Codec<T> entryCodec;
	private final PackratParser<RegistryEntryArgumentType.EntryParser<T, NbtElement>> parser;
	private final RegistryKey<? extends Registry<T>> registryRef;

	protected RegistryEntryArgumentType(
			CommandRegistryAccess registryAccess,
			RegistryKey<? extends Registry<T>> registry,
			Codec<T> entryCodec
	) {
		this.registries = registryAccess;
		this.registry = registryAccess.getOptional(registry);
		this.registryRef = registry;
		this.entryCodec = entryCodec;
		this.parser = createParser(registry, OPS);
	}

	public static <T, O> PackratParser<RegistryEntryArgumentType.EntryParser<T, O>> createParser(
			RegistryKey<? extends Registry<T>> key,
			DynamicOps<O> ops
	) {
		PackratParser<O> packratParser = SnbtParsing.createParser((DynamicOps<O>) ops);
		ParsingRules<StringReader> parsingRules = new ParsingRules<>();
		Symbol<RegistryEntryArgumentType.EntryParser<T, O>> symbol = Symbol.of("result");
		Symbol<Identifier> symbol2 = Symbol.of("id");
		Symbol<O> symbol3 = Symbol.of("value");
		parsingRules.set(symbol2, AnyIdParsingRule.INSTANCE);
		parsingRules.set(symbol3, packratParser.top().getRule());
		ParsingRuleEntry<StringReader, RegistryEntryArgumentType.EntryParser<T, O>> parsingRuleEntry = parsingRules.set(
				symbol, Term.anyOf(parsingRules.term(symbol2), parsingRules.term(symbol3)), results -> {
					Identifier identifier = results.get(symbol2);
					if (identifier != null) {
						return new RegistryEntryArgumentType.ReferenceParser<>(RegistryKey.of(key, identifier));
					}
					else {
						O object = results.getOrThrow(symbol3);
						return new RegistryEntryArgumentType.DirectParser<>(object);
					}
				}
		);
		return new PackratParser<>(parsingRules, parsingRuleEntry);
	}

	public static RegistryEntryArgumentType.LootTableArgumentType lootTable(CommandRegistryAccess registryAccess) {
		return new RegistryEntryArgumentType.LootTableArgumentType(registryAccess);
	}

	public static RegistryEntry<LootTable> getLootTable(CommandContext<ServerCommandSource> context, String argument)
	throws CommandSyntaxException {
		return getArgument(context, argument);
	}

	public static RegistryEntryArgumentType.LootFunctionArgumentType lootFunction(CommandRegistryAccess registryAccess) {
		return new RegistryEntryArgumentType.LootFunctionArgumentType(registryAccess);
	}

	public static RegistryEntry<LootFunction> getLootFunction(
			CommandContext<ServerCommandSource> context,
			String argument
	) {
		return getArgument(context, argument);
	}

	public static RegistryEntryArgumentType.LootConditionArgumentType lootCondition(CommandRegistryAccess registryAccess) {
		return new RegistryEntryArgumentType.LootConditionArgumentType(registryAccess);
	}

	public static RegistryEntry<LootCondition> getLootCondition(
			CommandContext<ServerCommandSource> context,
			String argument
	) {
		return getArgument(context, argument);
	}

	public static RegistryEntryArgumentType.DialogArgumentType dialog(CommandRegistryAccess registryAccess) {
		return new RegistryEntryArgumentType.DialogArgumentType(registryAccess);
	}

	public static RegistryEntry<Dialog> getDialog(CommandContext<ServerCommandSource> context, String argument) {
		return getArgument(context, argument);
	}

	private static <T> RegistryEntry<T> getArgument(CommandContext<ServerCommandSource> context, String argument) {
		return (RegistryEntry<T>) context.getArgument(argument, RegistryEntry.class);
	}

	public @Nullable RegistryEntry<T> parse(StringReader stringReader) throws CommandSyntaxException {
		return this.parse(stringReader, this.parser, OPS);
	}

	private <O> @Nullable RegistryEntry<T> parse(
			StringReader reader,
			PackratParser<RegistryEntryArgumentType.EntryParser<T, O>> parser,
			DynamicOps<O> ops
	) throws CommandSyntaxException {
		RegistryEntryArgumentType.EntryParser<T, O> entryParser = parser.parse(reader);
		return this.registry.isEmpty() ? null : entryParser.parse(
				reader,
				this.registries,
				ops,
				this.entryCodec,
				(RegistryWrapper.Impl<T>) this.registry.get()
		);
	}

	public <S> CompletableFuture<Suggestions> listSuggestions(
			CommandContext<S> context,
			SuggestionsBuilder suggestionsBuilder
	) {
		return CommandSource.listSuggestions(
				context,
				suggestionsBuilder,
				this.registryRef,
				CommandSource.SuggestedIdType.ELEMENTS
		);
	}

	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/**
	 * {@code DialogArgumentType}.
	 */
	public static class DialogArgumentType extends RegistryEntryArgumentType<Dialog> {

		protected DialogArgumentType(CommandRegistryAccess registryAccess) {
			super(registryAccess, RegistryKeys.DIALOG, Dialog.CODEC);
		}
	}

	/**
	 * {@code DirectParser}.
	 */
	public record DirectParser<T, O>(O value) implements RegistryEntryArgumentType.EntryParser<T, O> {

		@Override
		public RegistryEntry<T> parse(
				ImmutableStringReader reader,
				RegistryWrapper.WrapperLookup registries,
				DynamicOps<O> ops,
				Codec<T> codec,
				RegistryWrapper.Impl<T> registryAccess
		) throws CommandSyntaxException {
			return RegistryEntry.of(
					(T) codec.parse(registries.getOps(ops), this.value)
					         .getOrThrow(error -> RegistryEntryArgumentType.FAILED_TO_PARSE_EXCEPTION.createWithContext(
							         reader,
							         error
					         ))
			);
		}
	}

	/**
	 * {@code EntryParser}.
	 */
	public sealed interface EntryParser<T, O> permits RegistryEntryArgumentType.DirectParser, RegistryEntryArgumentType.ReferenceParser {

		RegistryEntry<T> parse(
				ImmutableStringReader reader,
				RegistryWrapper.WrapperLookup registries,
				DynamicOps<O> ops,
				Codec<T> codec,
				RegistryWrapper.Impl<T> registryAccess
		) throws CommandSyntaxException;
	}

	/**
	 * {@code LootConditionArgumentType}.
	 */
	public static class LootConditionArgumentType extends RegistryEntryArgumentType<LootCondition> {

		protected LootConditionArgumentType(CommandRegistryAccess registryAccess) {
			super(registryAccess, RegistryKeys.PREDICATE, LootCondition.CODEC);
		}
	}

	/**
	 * {@code LootFunctionArgumentType}.
	 */
	public static class LootFunctionArgumentType extends RegistryEntryArgumentType<LootFunction> {

		protected LootFunctionArgumentType(CommandRegistryAccess registryAccess) {
			super(registryAccess, RegistryKeys.ITEM_MODIFIER, LootFunctionTypes.CODEC);
		}
	}

	/**
	 * {@code LootTableArgumentType}.
	 */
	public static class LootTableArgumentType extends RegistryEntryArgumentType<LootTable> {

		protected LootTableArgumentType(CommandRegistryAccess registryAccess) {
			super(registryAccess, RegistryKeys.LOOT_TABLE, LootTable.CODEC);
		}
	}

	/**
	 * {@code ReferenceParser}.
	 */
	public record ReferenceParser<T, O>(RegistryKey<T> key) implements RegistryEntryArgumentType.EntryParser<T, O> {

		@Override
		public RegistryEntry<T> parse(
				ImmutableStringReader reader,
				RegistryWrapper.WrapperLookup registries,
				DynamicOps<O> ops,
				Codec<T> codec,
				RegistryWrapper.Impl<T> registryAccess
		) throws CommandSyntaxException {
			return registryAccess.getOptional(this.key)
			                     .orElseThrow(() -> RegistryEntryArgumentType.NO_SUCH_ELEMENT_EXCEPTION.createWithContext(
					                     reader,
					                     this.key.getValue(),
					                     this.key.getRegistry()
			                     ));
		}
	}
}
