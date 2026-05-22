package net.minecraft.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Тип аргумента команды для выбора записи реестра или тега реестра в качестве предиката.
 *
 * <p>Принимает либо конкретный идентификатор записи ({@code minecraft:zombie}),
 * либо тег ({@code #minecraft:undead}). Результат реализует {@link EntryPredicate},
 * позволяя проверять {@link RegistryEntry} на соответствие.
 *
 * @param <T> тип элементов реестра
 */
public class RegistryEntryPredicateArgumentType<T>
		implements ArgumentType<RegistryEntryPredicateArgumentType.EntryPredicate<T>> {

	private static final Collection<String> EXAMPLES =
			Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");

	private static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType(
			(tag, type) -> Text.stringifiedTranslatable("argument.resource_tag.not_found", tag, type)
	);

	private static final Dynamic3CommandExceptionType WRONG_TYPE_EXCEPTION = new Dynamic3CommandExceptionType(
			(tag, type, expectedType) -> Text.stringifiedTranslatable(
					"argument.resource_tag.invalid_type",
					tag,
					type,
					expectedType
			)
	);

	private final RegistryWrapper<T> registryWrapper;
	final RegistryKey<? extends Registry<T>> registryRef;

	public RegistryEntryPredicateArgumentType(
			CommandRegistryAccess registryAccess,
			RegistryKey<? extends Registry<T>> registryRef
	) {
		this.registryRef = registryRef;
		registryWrapper = registryAccess.getOrThrow(registryRef);
	}

	public static <T> RegistryEntryPredicateArgumentType<T> registryEntryPredicate(
			CommandRegistryAccess registryAccess,
			RegistryKey<? extends Registry<T>> registryRef
	) {
		return new RegistryEntryPredicateArgumentType<>(registryAccess, registryRef);
	}

	/**
	 * Извлекает {@link EntryPredicate} из контекста команды и проверяет совместимость типа реестра.
	 *
	 * @param context     контекст выполнения команды
	 * @param name        имя аргумента
	 * @param registryRef ключ ожидаемого реестра
	 * @throws CommandSyntaxException если тип реестра не совпадает
	 */
	public static <T> RegistryEntryPredicateArgumentType.EntryPredicate<T> getRegistryEntryPredicate(
			CommandContext<ServerCommandSource> context,
			String name,
			RegistryKey<Registry<T>> registryRef
	) throws CommandSyntaxException {
		RegistryEntryPredicateArgumentType.EntryPredicate<?> predicate =
				(RegistryEntryPredicateArgumentType.EntryPredicate<?>) context.getArgument(
						name, RegistryEntryPredicateArgumentType.EntryPredicate.class
				);
		Optional<RegistryEntryPredicateArgumentType.EntryPredicate<T>> cast = predicate.tryCast(registryRef);

		return cast.orElseThrow(() -> (CommandSyntaxException) predicate.getEntry().map(
				entry -> {
					RegistryKey<?> key = entry.registryKey();
					return RegistryEntryReferenceArgumentType.INVALID_TYPE_EXCEPTION.create(
							key.getValue(),
							key.getRegistry(),
							registryRef.getValue()
					);
				},
				entryList -> {
					TagKey<?> tagKey = entryList.getTag();
					return WRONG_TYPE_EXCEPTION.create(
							tagKey.id(),
							tagKey.registryRef(),
							registryRef.getValue()
					);
				}
		));
	}

	@Override
	public RegistryEntryPredicateArgumentType.EntryPredicate<T> parse(StringReader reader)
	throws CommandSyntaxException {
		if (reader.canRead() && reader.peek() == '#') {
			int savedCursor = reader.getCursor();

			try {
				reader.skip();
				Identifier tagId = Identifier.fromCommandInput(reader);
				TagKey<T> tagKey = TagKey.of(registryRef, tagId);
				RegistryEntryList.Named<T> named = registryWrapper
						.getOptional(tagKey)
						.orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(
								reader,
								tagId,
								registryRef.getValue()
						));
				return new RegistryEntryPredicateArgumentType.TagBased<>(named);
			}
			catch (CommandSyntaxException ex) {
				reader.setCursor(savedCursor);
				throw ex;
			}
		}

		Identifier entryId = Identifier.fromCommandInput(reader);
		RegistryKey<T> registryKey = RegistryKey.of(registryRef, entryId);
		RegistryEntry.Reference<T> reference = registryWrapper
				.getOptional(registryKey)
				.orElseThrow(() -> RegistryEntryReferenceArgumentType.NOT_FOUND_EXCEPTION.createWithContext(
						reader,
						entryId,
						registryRef.getValue()
				));
		return new RegistryEntryPredicateArgumentType.EntryBased<>(reference);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.listSuggestions(context, builder, registryRef, CommandSource.SuggestedIdType.ALL);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/**
	 * Предикат, основанный на конкретной записи реестра.
	 */
	record EntryBased<T>(RegistryEntry.Reference<T> value)
			implements RegistryEntryPredicateArgumentType.EntryPredicate<T> {

		@Override
		public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
			return Either.left(value);
		}

		@Override
		public <E> Optional<RegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(
				RegistryKey<? extends Registry<E>> registryRef
		) {
			return value.registryKey().isOf(registryRef)
					? Optional.of((RegistryEntryPredicateArgumentType.EntryPredicate<E>) this)
					: Optional.empty();
		}

		@Override
		public boolean test(RegistryEntry<T> entry) {
			return entry.equals(value);
		}

		@Override
		public String asString() {
			return value.registryKey().getValue().toString();
		}

	}

	/**
	 * Предикат, основанный на теге реестра — проверяет вхождение записи в тег.
	 */
	record TagBased<T>(RegistryEntryList.Named<T> tag)
			implements RegistryEntryPredicateArgumentType.EntryPredicate<T> {

		@Override
		public Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry() {
			return Either.right(tag);
		}

		@Override
		public <E> Optional<RegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(
				RegistryKey<? extends Registry<E>> registryRef
		) {
			return tag.getTag().isOf(registryRef)
					? Optional.of((RegistryEntryPredicateArgumentType.EntryPredicate<E>) this)
					: Optional.empty();
		}

		@Override
		public boolean test(RegistryEntry<T> entry) {
			return tag.contains(entry);
		}

		@Override
		public String asString() {
			return "#" + tag.getTag().id();
		}

	}

	/**
	 * Предикат для проверки {@link RegistryEntry} — либо по конкретной записи, либо по тегу.
	 */
	public interface EntryPredicate<T> extends Predicate<RegistryEntry<T>> {

		Either<RegistryEntry.Reference<T>, RegistryEntryList.Named<T>> getEntry();

		<E> Optional<RegistryEntryPredicateArgumentType.EntryPredicate<E>> tryCast(
				RegistryKey<? extends Registry<E>> registryRef
		);

		String asString();

	}

	/**
	 * Сериализатор для передачи ключа реестра по сети.
	 */
	public static class Serializer<T>
			implements ArgumentSerializer<
					RegistryEntryPredicateArgumentType<T>,
					RegistryEntryPredicateArgumentType.Serializer<T>.Properties
			> {

		@Override
		public void writePacket(
				RegistryEntryPredicateArgumentType.Serializer<T>.Properties properties,
				PacketByteBuf buf
		) {
			buf.writeRegistryKey(properties.registryRef);
		}

		@Override
		public RegistryEntryPredicateArgumentType.Serializer<T>.Properties fromPacket(PacketByteBuf buf) {
			return new RegistryEntryPredicateArgumentType.Serializer.Properties(buf.readRegistryRefKey());
		}

		@Override
		public void writeJson(
				RegistryEntryPredicateArgumentType.Serializer<T>.Properties properties,
				JsonObject jsonObject
		) {
			jsonObject.addProperty("registry", properties.registryRef.getValue().toString());
		}

		@Override
		public RegistryEntryPredicateArgumentType.Serializer<T>.Properties getArgumentTypeProperties(
				RegistryEntryPredicateArgumentType<T> type
		) {
			return new RegistryEntryPredicateArgumentType.Serializer.Properties(type.registryRef);
		}

		public final class Properties
				implements ArgumentSerializer.ArgumentTypeProperties<RegistryEntryPredicateArgumentType<T>> {

			final RegistryKey<? extends Registry<T>> registryRef;

			Properties(final RegistryKey<? extends Registry<T>> registryRef) {
				this.registryRef = registryRef;
			}

			@Override
			public RegistryEntryPredicateArgumentType<T> createType(CommandRegistryAccess registryAccess) {
				return new RegistryEntryPredicateArgumentType<>(registryAccess, registryRef);
			}

			@Override
			public ArgumentSerializer<RegistryEntryPredicateArgumentType<T>, ?> getSerializer() {
				return Serializer.this;
			}

		}

	}

}
