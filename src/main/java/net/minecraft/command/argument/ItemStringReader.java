package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.command.CommandSource;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Парсер строкового представления предмета с компонентами.
 *
 * <p>Формат: {@code namespace:item_id[component=value, !removed_component]}.
 * Поддерживает добавление ({@code component=value}) и удаление ({@code !component}) компонентов.
 * Результат валидируется через {@link ItemStack#validateComponents}.
 */
public class ItemStringReader {

	static final DynamicCommandExceptionType INVALID_ITEM_ID_EXCEPTION = new DynamicCommandExceptionType(
			id -> Text.stringifiedTranslatable("argument.item.id.invalid", id)
	);

	static final DynamicCommandExceptionType UNKNOWN_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
			id -> Text.stringifiedTranslatable("arguments.item.component.unknown", id)
	);

	static final Dynamic2CommandExceptionType MALFORMED_COMPONENT_EXCEPTION = new Dynamic2CommandExceptionType(
			(type, error) -> Text.stringifiedTranslatable("arguments.item.component.malformed", type, error)
	);

	static final SimpleCommandExceptionType COMPONENT_EXPECTED_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("arguments.item.component.expected"));

	static final DynamicCommandExceptionType REPEATED_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(
			type -> Text.stringifiedTranslatable("arguments.item.component.repeated", type)
	);

	private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
			error -> Text.stringifiedTranslatable("arguments.item.malformed", error)
	);

	public static final char OPEN_SQUARE_BRACKET = '[';
	public static final char CLOSED_SQUARE_BRACKET = ']';
	public static final char COMMA = ',';
	public static final char EQUAL_SIGN = '=';
	public static final char EXCLAMATION_MARK = '!';

	static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_DEFAULT =
			SuggestionsBuilder::buildFuture;

	final RegistryWrapper.Impl<Item> itemRegistry;
	final RegistryOps<NbtElement> ops;
	final StringNbtReader<NbtElement> snbtReader;

	public ItemStringReader(RegistryWrapper.WrapperLookup registries) {
		itemRegistry = registries.getOrThrow(RegistryKeys.ITEM);
		ops = registries.getOps(NbtOps.INSTANCE);
		snbtReader = StringNbtReader.fromOps(ops);
	}

	/**
	 * Разбирает строку предмета и возвращает результат с записью реестра и изменениями компонентов.
	 * Валидирует итоговый набор компонентов через {@link ItemStack#validateComponents}.
	 *
	 * @param reader входной поток символов
	 * @return разобранный результат {@link ItemResult}
	 * @throws CommandSyntaxException при неверном идентификаторе, неизвестном или повторном компоненте
	 */
	public ItemStringReader.ItemResult consume(StringReader reader) throws CommandSyntaxException {
		final MutableObject<RegistryEntry<Item>> itemHolder = new MutableObject<>();
		final ComponentChanges.Builder changesBuilder = ComponentChanges.builder();

		consume(
				reader, new ItemStringReader.Callbacks() {
					@Override
					public void onItem(RegistryEntry<Item> item) {
						itemHolder.setValue(item);
					}

					@Override
					public <T> void onComponentAdded(ComponentType<T> type, T value) {
						changesBuilder.add(type, value);
					}

					@Override
					public <T> void onComponentRemoved(ComponentType<T> type) {
						changesBuilder.remove(type);
					}
				}
		);

		RegistryEntry<Item> item = Objects.requireNonNull(
				(RegistryEntry<Item>) itemHolder.get(),
				"Parser gave no item"
		);
		ComponentChanges changes = changesBuilder.build();
		validate(reader, item, changes);

		return new ItemStringReader.ItemResult(item, changes);
	}

	private static void validate(StringReader reader, RegistryEntry<Item> item, ComponentChanges components)
	throws CommandSyntaxException {
		ComponentMap merged = MergedComponentMap.create(item.value().getComponents(), components);
		DataResult<Unit> result = ItemStack.validateComponents(merged);
		result.getOrThrow(error -> MALFORMED_ITEM_EXCEPTION.createWithContext(reader, error));
	}

	public void consume(StringReader reader, ItemStringReader.Callbacks callbacks) throws CommandSyntaxException {
		int savedCursor = reader.getCursor();

		try {
			new ItemStringReader.Reader(reader, callbacks).read();
		}
		catch (CommandSyntaxException ex) {
			reader.setCursor(savedCursor);
			throw ex;
		}
	}

	public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		ItemStringReader.SuggestionCallbacks suggestionCallbacks = new ItemStringReader.SuggestionCallbacks();
		ItemStringReader.Reader itemReader = new ItemStringReader.Reader(reader, suggestionCallbacks);

		try {
			itemReader.read();
		}
		catch (CommandSyntaxException ignored) {
		}

		return suggestionCallbacks.getSuggestions(builder, reader);
	}

	/**
	 * Колбэки, вызываемые при разборе предмета и его компонентов.
	 */
	public interface Callbacks {

		default void onItem(RegistryEntry<Item> item) {
		}

		default <T> void onComponentAdded(ComponentType<T> type, T value) {
		}

		default <T> void onComponentRemoved(ComponentType<T> type) {
		}

		default void setSuggestor(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor) {
		}

	}

	/**
	 * Иммутабельный результат разбора: запись реестра предмета и изменения компонентов.
	 */
	public record ItemResult(RegistryEntry<Item> item, ComponentChanges components) {
	}

	class Reader {

		private final StringReader reader;
		private final ItemStringReader.Callbacks callbacks;

		Reader(final StringReader reader, final ItemStringReader.Callbacks callbacks) {
			this.reader = reader;
			this.callbacks = callbacks;
		}

		public void read() throws CommandSyntaxException {
			callbacks.setSuggestor(this::suggestItems);
			readItem();
			callbacks.setSuggestor(this::suggestBracket);

			if (reader.canRead() && reader.peek() == '[') {
				callbacks.setSuggestor(ItemStringReader.SUGGEST_DEFAULT);
				readComponents();
			}

		}

		private void readItem() throws CommandSyntaxException {
			int savedCursor = reader.getCursor();
			Identifier identifier = Identifier.fromCommandInput(reader);
			callbacks.onItem(
					ItemStringReader.this.itemRegistry
							.getOptional(RegistryKey.of(RegistryKeys.ITEM, identifier))
							.orElseThrow(() -> {
								reader.setCursor(savedCursor);
								return ItemStringReader.INVALID_ITEM_ID_EXCEPTION.createWithContext(reader, identifier);
							})
			);
		}

		private void readComponents() throws CommandSyntaxException {
			reader.expect('[');
			callbacks.setSuggestor(this::suggestComponents);
			Set<ComponentType<?>> seen = new ReferenceArraySet<>();

			while (reader.canRead() && reader.peek() != ']') {
				reader.skipWhitespace();

				if (reader.canRead() && reader.peek() == '!') {
					reader.skip();
					callbacks.setSuggestor(this::suggestComponentsToRemove);
					ComponentType<?> type = readComponentType(reader);

					if (!seen.add(type)) {
						throw ItemStringReader.REPEATED_COMPONENT_EXCEPTION.create(type);
					}

					callbacks.onComponentRemoved(type);
					callbacks.setSuggestor(ItemStringReader.SUGGEST_DEFAULT);
					reader.skipWhitespace();
				}
				else {
					ComponentType<?> type = readComponentType(reader);

					if (!seen.add(type)) {
						throw ItemStringReader.REPEATED_COMPONENT_EXCEPTION.create(type);
					}

					callbacks.setSuggestor(this::suggestEqual);
					reader.skipWhitespace();
					reader.expect('=');
					callbacks.setSuggestor(ItemStringReader.SUGGEST_DEFAULT);
					reader.skipWhitespace();
					readComponentValue(ItemStringReader.this.snbtReader, ItemStringReader.this.ops, type);
					reader.skipWhitespace();
				}

				callbacks.setSuggestor(this::suggestEndOfComponent);

				if (!reader.canRead() || reader.peek() != ',') {
					break;
				}

				reader.skip();
				reader.skipWhitespace();
				callbacks.setSuggestor(this::suggestComponents);

				if (!reader.canRead()) {
					throw ItemStringReader.COMPONENT_EXPECTED_EXCEPTION.createWithContext(reader);
				}

			}

			reader.expect(']');
			callbacks.setSuggestor(ItemStringReader.SUGGEST_DEFAULT);
		}

		public static ComponentType<?> readComponentType(StringReader reader) throws CommandSyntaxException {
			if (!reader.canRead()) {
				throw ItemStringReader.COMPONENT_EXPECTED_EXCEPTION.createWithContext(reader);
			}

			int savedCursor = reader.getCursor();
			Identifier identifier = Identifier.fromCommandInput(reader);
			ComponentType<?> type = Registries.DATA_COMPONENT_TYPE.get(identifier);

			if (type != null && !type.shouldSkipSerialization()) {
				return type;
			}

			reader.setCursor(savedCursor);
			throw ItemStringReader.UNKNOWN_COMPONENT_EXCEPTION.createWithContext(reader, identifier);
		}

		private <T, O> void readComponentValue(
				StringNbtReader<O> snbtReader,
				RegistryOps<O> registryOps,
				ComponentType<T> type
		) throws CommandSyntaxException {
			int savedCursor = reader.getCursor();
			O raw = snbtReader.readAsArgument(reader);
			DataResult<T> result = type.getCodecOrThrow().parse(registryOps, raw);
			callbacks.onComponentAdded(
					type, (T) result.getOrThrow(error -> {
						reader.setCursor(savedCursor);
						return ItemStringReader.MALFORMED_COMPONENT_EXCEPTION.createWithContext(
								reader,
								type.toString(),
								error
						);
					})
			);
		}

		private CompletableFuture<Suggestions> suggestBracket(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest(String.valueOf('['));
			}

			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestEndOfComponent(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest(String.valueOf(','));
				builder.suggest(String.valueOf(']'));
			}

			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestEqual(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest(String.valueOf('='));
			}

			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestItems(SuggestionsBuilder builder) {
			return CommandSource.suggestIdentifiers(
					ItemStringReader.this.itemRegistry
							.streamKeys()
							.map(RegistryKey::getValue),
					builder
			);
		}

		private CompletableFuture<Suggestions> suggestComponents(SuggestionsBuilder builder) {
			builder.suggest(String.valueOf('!'));
			return suggestComponents(builder, String.valueOf('='));
		}

		private CompletableFuture<Suggestions> suggestComponentsToRemove(SuggestionsBuilder builder) {
			return suggestComponents(builder, "");
		}

		private CompletableFuture<Suggestions> suggestComponents(SuggestionsBuilder builder, String suffix) {
			String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
			CommandSource.forEachMatching(
					Registries.DATA_COMPONENT_TYPE.getEntrySet(),
					remaining,
					entry -> entry.getKey().getValue(),
					entry -> {
						ComponentType<?> type = entry.getValue();

						if (type.getCodec() != null) {
							Identifier identifier = entry.getKey().getValue();
							builder.suggest(identifier + suffix);
						}

					}
			);
			return builder.buildFuture();
		}

	}

	static class SuggestionCallbacks implements ItemStringReader.Callbacks {

		private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor =
				ItemStringReader.SUGGEST_DEFAULT;

		@Override
		public void setSuggestor(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor) {
			this.suggestor = suggestor;
		}

		public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder, StringReader reader) {
			return suggestor.apply(builder.createOffset(reader.getCursor()));
		}

	}

}
