package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.packrat.CursorExceptionType;
import net.minecraft.util.packrat.ParsingState;
import net.minecraft.util.packrat.Suggestable;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Реестр встроенных операций SNBT-парсера, таких как {@code bool(...)} и {@code uuid(...)}.
 * <p>
 * Каждая операция идентифицируется по имени и количеству аргументов ({@link Type}).
 * Операции вызываются парсером при обнаружении конструкции вида {@code имя(аргументы)}.
 */
public class SnbtOperation {

	static final CursorExceptionType<CommandSyntaxException> EXPECTED_STRING_UUID_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_string_uuid"))
		);
	static final CursorExceptionType<CommandSyntaxException> EXPECTED_NUMBER_OR_BOOLEAN_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_number_or_boolean"))
		);

	public static final String TRUE = "true";
	public static final String FALSE = "false";

	public static final Map<SnbtOperation.Type, SnbtOperation.Operator> OPERATIONS = Map.of(
		new SnbtOperation.Type("bool", 1), new SnbtOperation.Operator() {
			@Override
			public <T> T apply(DynamicOps<T> ops, List<T> args, ParsingState<StringReader> state) {
				Boolean result = asBoolean(ops, args.getFirst());
				if (result == null) {
					state.getErrors().add(state.getCursor(), SnbtOperation.EXPECTED_NUMBER_OR_BOOLEAN_EXCEPTION);
					return null;
				}

				return (T) ops.createBoolean(result);
			}

			private static <T> @Nullable Boolean asBoolean(DynamicOps<T> ops, T value) {
				Optional<Boolean> boolResult = ops.getBooleanValue(value).result();
				if (boolResult.isPresent()) {
					return boolResult.get();
				}

				Optional<Number> numberResult = ops.getNumberValue(value).result();
				return numberResult.isPresent() ? numberResult.get().doubleValue() != 0.0 : null;
			}
		},
		new SnbtOperation.Type("uuid", 1), new SnbtOperation.Operator() {
			@Override
			public <T> T apply(DynamicOps<T> ops, List<T> args, ParsingState<StringReader> state) {
				Optional<String> stringValue = ops.getStringValue(args.getFirst()).result();
				if (stringValue.isEmpty()) {
					state.getErrors().add(state.getCursor(), SnbtOperation.EXPECTED_STRING_UUID_EXCEPTION);
					return null;
				}

				UUID uuid;
				try {
					uuid = UUID.fromString(stringValue.get());
				}
				catch (IllegalArgumentException ignored) {
					state.getErrors().add(state.getCursor(), SnbtOperation.EXPECTED_STRING_UUID_EXCEPTION);
					return null;
				}

				return (T) ops.createIntList(IntStream.of(Uuids.toIntArray(uuid)));
			}
		}
	);

	public static final Suggestable<StringReader> SUGGESTIONS = new Suggestable<StringReader>() {
		private final Set<String> values = Stream
			.concat(
				Stream.of(FALSE, TRUE),
				SnbtOperation.OPERATIONS.keySet().stream().map(SnbtOperation.Type::id)
			)
			.collect(Collectors.toSet());

		@Override
		public Stream<String> possibleValues(ParsingState<StringReader> parsingState) {
			return values.stream();
		}
	};

	/**
	 * Функциональный интерфейс для реализации встроенной операции SNBT.
	 */
	public interface Operator {

		<T> @Nullable T apply(DynamicOps<T> ops, List<T> args, ParsingState<StringReader> state);
	}

	/**
	 * Идентификатор операции: имя и количество аргументов.
	 */
	public record Type(String id, int argCount) {

		@Override
		public String toString() {
			return id + "/" + argCount;
		}
	}
}
