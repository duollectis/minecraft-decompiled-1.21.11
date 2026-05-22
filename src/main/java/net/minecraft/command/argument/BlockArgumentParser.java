package net.minecraft.command.argument;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.*;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Парсер строкового представления блока или тега блоков для команд.
 *
 * <p>Формат: {@code namespace:block_id[property=value]{nbt_data}}.
 * Поддерживает теги ({@code #minecraft:logs}), свойства состояния блока и SNBT-данные блок-энтити.
 * Используется как в режиме разбора, так и в режиме автодополнения.
 */
public class BlockArgumentParser {

	public static final SimpleCommandExceptionType DISALLOWED_TAG_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.block.tag.disallowed"));

	public static final DynamicCommandExceptionType INVALID_BLOCK_ID_EXCEPTION = new DynamicCommandExceptionType(
			block -> Text.stringifiedTranslatable("argument.block.id.invalid", block)
	);

	public static final Dynamic2CommandExceptionType UNKNOWN_PROPERTY_EXCEPTION = new Dynamic2CommandExceptionType(
			(block, property) -> Text.stringifiedTranslatable("argument.block.property.unknown", block, property)
	);

	public static final Dynamic2CommandExceptionType DUPLICATE_PROPERTY_EXCEPTION = new Dynamic2CommandExceptionType(
			(block, property) -> Text.stringifiedTranslatable("argument.block.property.duplicate", property, block)
	);

	public static final Dynamic3CommandExceptionType INVALID_PROPERTY_EXCEPTION = new Dynamic3CommandExceptionType(
			(block, property, value) -> Text.stringifiedTranslatable(
					"argument.block.property.invalid",
					block,
					value,
					property
			)
	);

	public static final Dynamic2CommandExceptionType EMPTY_PROPERTY_EXCEPTION = new Dynamic2CommandExceptionType(
			(block, property) -> Text.stringifiedTranslatable("argument.block.property.novalue", property, block)
	);

	public static final SimpleCommandExceptionType UNCLOSED_PROPERTIES_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("argument.block.property.unclosed"));

	public static final DynamicCommandExceptionType UNKNOWN_BLOCK_TAG_EXCEPTION = new DynamicCommandExceptionType(
			tag -> Text.stringifiedTranslatable("arguments.block.tag.unknown", tag)
	);

	private static final char PROPERTIES_OPENING = '[';
	private static final char NBT_OPENING = '{';
	private static final char PROPERTIES_CLOSING = ']';
	private static final char PROPERTY_DEFINER = '=';
	private static final char PROPERTY_SEPARATOR = ',';
	private static final char TAG_PREFIX = '#';

	private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_DEFAULT =
			SuggestionsBuilder::buildFuture;

	private final RegistryWrapper<Block> registryWrapper;
	private final StringReader reader;
	private final boolean allowTag;
	private final boolean allowSnbt;
	private final Map<Property<?>, Comparable<?>> blockProperties = Maps.newHashMap();
	private final Map<String, String> tagProperties = Maps.newHashMap();
	private Identifier blockId = Identifier.ofVanilla("");
	private @Nullable StateManager<Block, BlockState> stateFactory;
	private @Nullable BlockState blockState;
	private @Nullable NbtCompound data;
	private @Nullable RegistryEntryList<Block> tagId;
	private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_DEFAULT;

	private BlockArgumentParser(
			RegistryWrapper<Block> registryWrapper,
			StringReader reader,
			boolean allowTag,
			boolean allowSnbt
	) {
		this.registryWrapper = registryWrapper;
		this.reader = reader;
		this.allowTag = allowTag;
		this.allowSnbt = allowSnbt;
	}

	public static BlockArgumentParser.BlockResult block(
			RegistryWrapper<Block> registryWrapper,
			String string,
			boolean allowSnbt
	) throws CommandSyntaxException {
		return block(registryWrapper, new StringReader(string), allowSnbt);
	}

	public static BlockArgumentParser.BlockResult block(
			RegistryWrapper<Block> registryWrapper,
			StringReader reader,
			boolean allowSnbt
	) throws CommandSyntaxException {
		int savedCursor = reader.getCursor();

		try {
			BlockArgumentParser parser = new BlockArgumentParser(registryWrapper, reader, false, allowSnbt);
			parser.parse();
			return new BlockArgumentParser.BlockResult(parser.blockState, parser.blockProperties, parser.data);
		}
		catch (CommandSyntaxException ex) {
			reader.setCursor(savedCursor);
			throw ex;
		}
	}

	public static Either<BlockArgumentParser.BlockResult, BlockArgumentParser.TagResult> blockOrTag(
			RegistryWrapper<Block> registryWrapper,
			String string,
			boolean allowSnbt
	) throws CommandSyntaxException {
		return blockOrTag(registryWrapper, new StringReader(string), allowSnbt);
	}

	public static Either<BlockArgumentParser.BlockResult, BlockArgumentParser.TagResult> blockOrTag(
			RegistryWrapper<Block> registryWrapper,
			StringReader reader,
			boolean allowSnbt
	) throws CommandSyntaxException {
		int savedCursor = reader.getCursor();

		try {
			BlockArgumentParser parser = new BlockArgumentParser(registryWrapper, reader, true, allowSnbt);
			parser.parse();

			return parser.tagId != null
					? Either.right(new BlockArgumentParser.TagResult(
							parser.tagId,
							parser.tagProperties,
							parser.data
					))
					: Either.left(new BlockArgumentParser.BlockResult(
							parser.blockState,
							parser.blockProperties,
							parser.data
					));
		}
		catch (CommandSyntaxException ex) {
			reader.setCursor(savedCursor);
			throw ex;
		}
	}

	public static CompletableFuture<Suggestions> getSuggestions(
			RegistryWrapper<Block> registryWrapper,
			SuggestionsBuilder builder,
			boolean allowTag,
			boolean allowSnbt
	) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		BlockArgumentParser parser = new BlockArgumentParser(registryWrapper, reader, allowTag, allowSnbt);

		try {
			parser.parse();
		}
		catch (CommandSyntaxException ignored) {
		}

		return parser.suggestions.apply(builder.createOffset(reader.getCursor()));
	}

	private void parse() throws CommandSyntaxException {
		suggestions = allowTag ? this::suggestBlockOrTagId : this::suggestBlockId;

		if (reader.canRead() && reader.peek() == TAG_PREFIX) {
			parseTagId();
			suggestions = this::suggestSnbtOrTagProperties;

			if (reader.canRead() && reader.peek() == PROPERTIES_OPENING) {
				parseTagProperties();
				suggestions = this::suggestSnbt;
			}

		}
		else {
			parseBlockId();
			suggestions = this::suggestSnbtOrBlockProperties;

			if (reader.canRead() && reader.peek() == PROPERTIES_OPENING) {
				parseBlockProperties();
				suggestions = this::suggestSnbt;
			}

		}

		if (allowSnbt && reader.canRead() && reader.peek() == NBT_OPENING) {
			suggestions = SUGGEST_DEFAULT;
			parseSnbt();
		}

	}

	private CompletableFuture<Suggestions> suggestBlockPropertiesOrEnd(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty()) {
			builder.suggest(String.valueOf(PROPERTIES_CLOSING));
		}

		return suggestBlockProperties(builder);
	}

	private CompletableFuture<Suggestions> suggestTagPropertiesOrEnd(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty()) {
			builder.suggest(String.valueOf(PROPERTIES_CLOSING));
		}

		return suggestTagProperties(builder);
	}

	private CompletableFuture<Suggestions> suggestBlockProperties(SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

		for (Property<?> property : blockState.getProperties()) {
			if (!blockProperties.containsKey(property) && property.getName().startsWith(remaining)) {
				builder.suggest(property.getName() + PROPERTY_DEFINER);
			}

		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestTagProperties(SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

		if (tagId != null) {
			for (RegistryEntry<Block> entry : tagId) {
				for (Property<?> property : entry.value().getStateManager().getProperties()) {
					if (!tagProperties.containsKey(property.getName()) && property.getName().startsWith(remaining)) {
						builder.suggest(property.getName() + PROPERTY_DEFINER);
					}

				}

			}

		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestSnbt(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty() && hasBlockEntity()) {
			builder.suggest(String.valueOf(NBT_OPENING));
		}

		return builder.buildFuture();
	}

	private boolean hasBlockEntity() {
		if (blockState != null) {
			return blockState.hasBlockEntity();
		}

		if (tagId != null) {
			for (RegistryEntry<Block> entry : tagId) {
				if (entry.value().getDefaultState().hasBlockEntity()) {
					return true;
				}

			}

		}

		return false;
	}

	private CompletableFuture<Suggestions> suggestEqualsCharacter(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty()) {
			builder.suggest(String.valueOf(PROPERTY_DEFINER));
		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestCommaOrEnd(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty()) {
			builder.suggest(String.valueOf(PROPERTIES_CLOSING));
		}

		if (builder.getRemaining().isEmpty() && blockProperties.size() < blockState.getProperties().size()) {
			builder.suggest(String.valueOf(PROPERTY_SEPARATOR));
		}

		return builder.buildFuture();
	}

	private static <T extends Comparable<T>> SuggestionsBuilder suggestPropertyValues(
			SuggestionsBuilder builder,
			Property<T> property
	) {
		for (T value : property.getValues()) {
			if (value instanceof Integer integer) {
				builder.suggest(integer);
			}
			else {
				builder.suggest(property.name(value));
			}

		}

		return builder;
	}

	private CompletableFuture<Suggestions> suggestTagPropertyValues(SuggestionsBuilder builder, String name) {
		boolean hasMoreProperties = false;

		if (tagId != null) {
			for (RegistryEntry<Block> entry : tagId) {
				Block block = entry.value();
				Property<?> property = block.getStateManager().getProperty(name);

				if (property != null) {
					suggestPropertyValues(builder, property);
				}

				if (!hasMoreProperties) {
					for (Property<?> prop : block.getStateManager().getProperties()) {
						if (!tagProperties.containsKey(prop.getName())) {
							hasMoreProperties = true;
							break;
						}

					}

				}

			}

		}

		if (hasMoreProperties) {
			builder.suggest(String.valueOf(PROPERTY_SEPARATOR));
		}

		builder.suggest(String.valueOf(PROPERTIES_CLOSING));
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestSnbtOrTagProperties(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty() && tagId != null) {
			boolean hasProperties = false;
			boolean hasBlockEntity = false;

			for (RegistryEntry<Block> entry : tagId) {
				Block block = entry.value();
				hasProperties |= !block.getStateManager().getProperties().isEmpty();
				hasBlockEntity |= block.getDefaultState().hasBlockEntity();

				if (hasProperties && hasBlockEntity) {
					break;
				}

			}

			if (hasProperties) {
				builder.suggest(String.valueOf(PROPERTIES_OPENING));
			}

			if (hasBlockEntity) {
				builder.suggest(String.valueOf(NBT_OPENING));
			}

		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestSnbtOrBlockProperties(SuggestionsBuilder builder) {
		if (builder.getRemaining().isEmpty()) {
			if (!stateFactory.getProperties().isEmpty()) {
				builder.suggest(String.valueOf(PROPERTIES_OPENING));
			}

			if (blockState.hasBlockEntity()) {
				builder.suggest(String.valueOf(NBT_OPENING));
			}

		}

		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestIdentifiers(SuggestionsBuilder builder) {
		return CommandSource.suggestIdentifiers(
				registryWrapper.streamTagKeys().map(TagKey::id),
				builder,
				String.valueOf(TAG_PREFIX)
		);
	}

	private CompletableFuture<Suggestions> suggestBlockId(SuggestionsBuilder builder) {
		return CommandSource.suggestIdentifiers(
				registryWrapper.streamKeys().map(RegistryKey::getValue),
				builder
		);
	}

	private CompletableFuture<Suggestions> suggestBlockOrTagId(SuggestionsBuilder builder) {
		suggestIdentifiers(builder);
		suggestBlockId(builder);
		return builder.buildFuture();
	}

	private void parseBlockId() throws CommandSyntaxException {
		int savedCursor = reader.getCursor();
		blockId = Identifier.fromCommandInput(reader);
		Block block = registryWrapper
				.getOptional(RegistryKey.of(RegistryKeys.BLOCK, blockId))
				.orElseThrow(() -> {
					reader.setCursor(savedCursor);
					return INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString());
				})
				.value();
		stateFactory = block.getStateManager();
		blockState = block.getDefaultState();
	}

	private void parseTagId() throws CommandSyntaxException {
		if (!allowTag) {
			throw DISALLOWED_TAG_EXCEPTION.createWithContext(reader);
		}

		int savedCursor = reader.getCursor();
		reader.expect(TAG_PREFIX);
		suggestions = this::suggestIdentifiers;
		Identifier identifier = Identifier.fromCommandInput(reader);
		tagId = registryWrapper
				.getOptional(TagKey.of(RegistryKeys.BLOCK, identifier))
				.orElseThrow(() -> {
					reader.setCursor(savedCursor);
					return UNKNOWN_BLOCK_TAG_EXCEPTION.createWithContext(reader, identifier.toString());
				});
	}

	private void parseBlockProperties() throws CommandSyntaxException {
		reader.skip();
		suggestions = this::suggestBlockPropertiesOrEnd;
		reader.skipWhitespace();

		while (reader.canRead() && reader.peek() != PROPERTIES_CLOSING) {
			reader.skipWhitespace();
			int propCursor = reader.getCursor();
			String propName = reader.readString();
			Property<?> property = stateFactory.getProperty(propName);

			if (property == null) {
				reader.setCursor(propCursor);
				throw UNKNOWN_PROPERTY_EXCEPTION.createWithContext(reader, blockId.toString(), propName);
			}

			if (blockProperties.containsKey(property)) {
				reader.setCursor(propCursor);
				throw DUPLICATE_PROPERTY_EXCEPTION.createWithContext(reader, blockId.toString(), propName);
			}

			reader.skipWhitespace();
			suggestions = this::suggestEqualsCharacter;

			if (!reader.canRead() || reader.peek() != PROPERTY_DEFINER) {
				throw EMPTY_PROPERTY_EXCEPTION.createWithContext(reader, blockId.toString(), propName);
			}

			reader.skip();
			reader.skipWhitespace();
			suggestions = innerBuilder -> suggestPropertyValues(innerBuilder, property).buildFuture();
			int valueCursor = reader.getCursor();
			parsePropertyValue(property, reader.readString(), valueCursor);
			suggestions = this::suggestCommaOrEnd;
			reader.skipWhitespace();

			if (!reader.canRead()) {
				break;
			}

			if (reader.peek() == PROPERTY_SEPARATOR) {
				reader.skip();
				suggestions = this::suggestBlockProperties;
				continue;
			}

			if (reader.peek() != PROPERTIES_CLOSING) {
				throw UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(reader);
			}

			break;
		}

		if (reader.canRead()) {
			reader.skip();
		}
		else {
			throw UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(reader);
		}

	}

	private void parseTagProperties() throws CommandSyntaxException {
		reader.skip();
		suggestions = this::suggestTagPropertiesOrEnd;
		int lastValueCursor = -1;
		reader.skipWhitespace();

		while (reader.canRead() && reader.peek() != PROPERTIES_CLOSING) {
			reader.skipWhitespace();
			int propCursor = reader.getCursor();
			String propName = reader.readString();

			if (tagProperties.containsKey(propName)) {
				reader.setCursor(propCursor);
				throw DUPLICATE_PROPERTY_EXCEPTION.createWithContext(reader, blockId.toString(), propName);
			}

			reader.skipWhitespace();

			if (!reader.canRead() || reader.peek() != PROPERTY_DEFINER) {
				reader.setCursor(propCursor);
				throw EMPTY_PROPERTY_EXCEPTION.createWithContext(reader, blockId.toString(), propName);
			}

			reader.skip();
			reader.skipWhitespace();
			suggestions = innerBuilder -> suggestTagPropertyValues(innerBuilder, propName);
			lastValueCursor = reader.getCursor();
			String propValue = reader.readString();
			tagProperties.put(propName, propValue);
			reader.skipWhitespace();

			if (!reader.canRead()) {
				break;
			}

			lastValueCursor = -1;

			if (reader.peek() == PROPERTY_SEPARATOR) {
				reader.skip();
				suggestions = this::suggestTagProperties;
				continue;
			}

			if (reader.peek() != PROPERTIES_CLOSING) {
				throw UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(reader);
			}

			break;
		}

		if (reader.canRead()) {
			reader.skip();
		}
		else {
			if (lastValueCursor >= 0) {
				reader.setCursor(lastValueCursor);
			}

			throw UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(reader);
		}

	}

	private void parseSnbt() throws CommandSyntaxException {
		data = StringNbtReader.readCompoundAsArgument(reader);
	}

	private <T extends Comparable<T>> void parsePropertyValue(
			Property<T> property,
			String value,
			int cursor
	) throws CommandSyntaxException {
		Optional<T> parsed = property.parse(value);

		if (parsed.isPresent()) {
			blockState = blockState.with(property, parsed.get());
			blockProperties.put(property, parsed.get());
		}
		else {
			reader.setCursor(cursor);
			throw INVALID_PROPERTY_EXCEPTION.createWithContext(
					reader,
					blockId.toString(),
					property.getName(),
					value
			);
		}

	}

	public static String stringifyBlockState(BlockState state) {
		StringBuilder builder = new StringBuilder(
				state.getRegistryEntry()
						.getKey()
						.map(key -> key.getValue().toString())
						.orElse("air")
		);

		if (!state.getProperties().isEmpty()) {
			builder.append(PROPERTIES_OPENING);
			boolean first = false;

			for (Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
				if (first) {
					builder.append(PROPERTY_SEPARATOR);
				}

				stringifyProperty(builder, entry.getKey(), entry.getValue());
				first = true;
			}

			builder.append(PROPERTIES_CLOSING);
		}

		return builder.toString();
	}

	private static <T extends Comparable<T>> void stringifyProperty(
			StringBuilder builder,
			Property<T> property,
			Comparable<?> value
	) {
		builder.append(property.getName());
		builder.append(PROPERTY_DEFINER);
		builder.append(property.name((T) value));
	}

	/**
	 * Результат разбора конкретного блока: состояние, свойства и опциональные NBT-данные.
	 */
	public record BlockResult(
			BlockState blockState,
			Map<Property<?>, Comparable<?>> properties,
			@Nullable NbtCompound nbt
	) {
	}

	/**
	 * Результат разбора тега блоков: список записей, строковые свойства и опциональные NBT-данные.
	 */
	public record TagResult(
			RegistryEntryList<Block> tag,
			Map<String, String> vagueProperties,
			@Nullable NbtCompound nbt
	) {
	}

}
