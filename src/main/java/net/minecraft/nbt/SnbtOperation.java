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
 * {@code SnbtOperation}.
 */
public class SnbtOperation {

	static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_STRING_UUID_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_string_uuid"))
			);
	static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_NUMBER_OR_BOOLEAN_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_number_or_boolean"))
			);
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final Map<SnbtOperation.Type, SnbtOperation.Operator> OPERATIONS = Map.of(
			new SnbtOperation.Type("bool", 1), new SnbtOperation.Operator() {
				@Override
				public <T> T apply(DynamicOps<T> ops, List<T> args, ParsingState<StringReader> state) {
					Boolean boolean_ = asBoolean(ops, args.getFirst());
					if (boolean_ == null) {
						state.getErrors().add(state.getCursor(), SnbtOperation.EXPECTED_NUMBER_OR_BOOLEAN_EXCEPTION);
						return null;
					}
					else {
						return (T) ops.createBoolean(boolean_);
					}
				}

				private static <T> @Nullable Boolean asBoolean(DynamicOps<T> ops, T value) {
					Optional<Boolean> optional = ops.getBooleanValue(value).result();
					if (optional.isPresent()) {
						return optional.get();
					}
					else {
						Optional<Number> optional2 = ops.getNumberValue(value).result();
						return optional2.isPresent() ? optional2.get().doubleValue() != 0.0 : null;
					}
				}
			}, new SnbtOperation.Type("uuid", 1), new SnbtOperation.Operator() {
				@Override
				public <T> T apply(DynamicOps<T> ops, List<T> args, ParsingState<StringReader> state) {
					Optional<String> optional = ops.getStringValue(args.getFirst()).result();
					if (optional.isEmpty()) {
						state.getErrors().add(state.getCursor(), SnbtOperation.EXPECTED_STRING_UUID_EXCEPTION);
						return null;
					}
					else {
						UUID uUID;
						try {
							uUID = UUID.fromString(optional.get());
						}
						catch (IllegalArgumentException var7) {
							state.getErrors().add(state.getCursor(), SnbtOperation.EXPECTED_STRING_UUID_EXCEPTION);
							return null;
						}

						return (T) ops.createIntList(IntStream.of(Uuids.toIntArray(uUID)));
					}
				}
			}
	);
	public static final Suggestable<StringReader> SUGGESTIONS = new Suggestable<StringReader>() {
		private final Set<String>
				values =
				Stream
						.concat(
								Stream.of("false", "true"),
								SnbtOperation.OPERATIONS.keySet().stream().map(SnbtOperation.Type::id)
						)
						.collect(Collectors.toSet());

		@Override
		public Stream<String> possibleValues(ParsingState<StringReader> parsingState) {
			return this.values.stream();
		}
	};

	/**
	 * {@code Operator}.
	 */
	public interface Operator {

		<T> @Nullable T apply(DynamicOps<T> ops, List<T> args, ParsingState<StringReader> state);
	}

	/**
	 * {@code Type}.
	 */
	public record Type(String id, int argCount) {

		@Override
		public String toString() {
			return this.id + "/" + this.argCount;
		}
	}
}
