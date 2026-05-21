package net.minecraft.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.chars.CharList;
import net.minecraft.text.Text;
import net.minecraft.util.packrat.*;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * {@code SnbtParsing}.
 */
public class SnbtParsing {

	private static final DynamicCommandExceptionType NUMBER_PARSE_FAILURE_EXCEPTION = new DynamicCommandExceptionType(
			value -> Text.stringifiedTranslatable("snbt.parser.number_parse_failure", value)
	);
	static final DynamicCommandExceptionType EXPECTED_HEX_ESCAPE_EXCEPTION = new DynamicCommandExceptionType(
			length -> Text.stringifiedTranslatable("snbt.parser.expected_hex_escape", length)
	);
	private static final DynamicCommandExceptionType INVALID_CODEPOINT_EXCEPTION = new DynamicCommandExceptionType(
			value -> Text.stringifiedTranslatable("snbt.parser.invalid_codepoint", value)
	);
	private static final DynamicCommandExceptionType NO_SUCH_OPERATION_EXCEPTION = new DynamicCommandExceptionType(
			operation -> Text.stringifiedTranslatable("snbt.parser.no_such_operation", operation)
	);
	static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_INTEGER_TYPE_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_integer_type"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_FLOAT_TYPE_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_float_type"))
			);
	static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_NON_NEGATIVE_NUMBER_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_non_negative_number"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			INVALID_CHARACTER_NAME_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_character_name"))
			);
	static final CursorExceptionType<CommandSyntaxException>
			INVALID_ARRAY_ELEMENT_TYPE_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_array_element_type"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			INVALID_UNQUOTED_START_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_unquoted_start"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_UNQUOTED_STRING_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_unquoted_string"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			INVALID_STRING_CONTENTS_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_string_contents"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_BINARY_NUMERAL_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_binary_numeral"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			UNDERSCORE_NOT_ALLOWED_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.underscore_not_allowed"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_DECIMAL_NUMERAL_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_decimal_numeral"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			EXPECTED_HEX_NUMERAL_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_hex_numeral"))
			);
	private static final CursorExceptionType<CommandSyntaxException> EMPTY_KEY_EXCEPTION = CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.empty_key"))
	);
	private static final CursorExceptionType<CommandSyntaxException>
			LEADING_ZERO_NOT_ALLOWED_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.leading_zero_not_allowed"))
			);
	private static final CursorExceptionType<CommandSyntaxException>
			INFINITY_NOT_ALLOWED_EXCEPTION =
			CursorExceptionType.create(
					new SimpleCommandExceptionType(Text.translatable("snbt.parser.infinity_not_allowed"))
			);
	private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
	private static final NumeralParsingRule
			BINARY_RULE =
			new NumeralParsingRule(EXPECTED_BINARY_NUMERAL_EXCEPTION, UNDERSCORE_NOT_ALLOWED_EXCEPTION) {
				@Override
				protected boolean accepts(char c) {
					return switch (c) {
						case '0', '1', '_' -> true;
						default -> false;
					};
				}
			};
	private static final NumeralParsingRule
			DECIMAL_RULE =
			new NumeralParsingRule(EXPECTED_DECIMAL_NUMERAL_EXCEPTION, UNDERSCORE_NOT_ALLOWED_EXCEPTION) {
				@Override
				protected boolean accepts(char c) {
					return switch (c) {
						case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
						default -> false;
					};
				}
			};
	private static final NumeralParsingRule
			HEX_RULE =
			new NumeralParsingRule(EXPECTED_HEX_NUMERAL_EXCEPTION, UNDERSCORE_NOT_ALLOWED_EXCEPTION) {
				@Override
				protected boolean accepts(char c) {
					return switch (c) {
						case '0',
						     '1',
						     '2',
						     '3',
						     '4',
						     '5',
						     '6',
						     '7',
						     '8',
						     '9',
						     'A',
						     'B',
						     'C',
						     'D',
						     'E',
						     'F',
						     '_',
						     'a',
						     'b',
						     'c',
						     'd',
						     'e',
						     'f' -> true;
						default -> false;
					};
				}
			};
	private static final TokenParsingRule
			UNQUOTED_STRING_RULE =
			new TokenParsingRule(1, INVALID_STRING_CONTENTS_EXCEPTION) {
				@Override
				protected boolean isValidChar(char c) {
					return switch (c) {
						case '"', '\'', '\\' -> false;
						default -> true;
					};
				}
			};
	private static final Literals.CharacterLiteral DECIMAL_CHAR = new Literals.CharacterLiteral(CharList.of()) {
		@Override
		protected boolean accepts(char c) {
			return SnbtParsing.isPartOfDecimal(c);
		}
	};
	private static final Pattern UNICODE_NAME_PATTERN = Pattern.compile("[-a-zA-Z0-9 ]+");

	static CursorExceptionType<CommandSyntaxException> toNumberParseFailure(NumberFormatException exception) {
		return CursorExceptionType.create(NUMBER_PARSE_FAILURE_EXCEPTION, exception.getMessage());
	}

	/**
	 * Escape special char.
	 *
	 * @param c c
	 *
	 * @return @Nullable String — результат операции
	 */
	public static @Nullable String escapeSpecialChar(char c) {
		return switch (c) {
			case '\b' -> "b";
			case '\t' -> "t";
			case '\n' -> "n";
			default -> c < ' ' ? "x" + HEX_FORMAT.toHexDigits((byte) c) : null;
			case '\f' -> "f";
			case '\r' -> "r";
		};
	}

	private static boolean canUnquotedStringStartWith(char c) {
		return !isPartOfDecimal(c);
	}

	static boolean isPartOfDecimal(char c) {
		return switch (c) {
			case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
			default -> false;
		};
	}

	static boolean containsUnderscore(String string) {
		return string.indexOf(95) != -1;
	}

	private static void skipUnderscoreAndAppend(StringBuilder builder, String value) {
		append(builder, value, containsUnderscore(value));
	}

	static void append(StringBuilder builder, String value, boolean skipUnderscore) {
		if (skipUnderscore) {
			for (char c : value.toCharArray()) {
				if (c != '_') {
					builder.append(c);
				}
			}
		}
		else {
			builder.append(value);
		}
	}

	static short parseUnsignedShort(String value, int radix) {
		int i = Integer.parseInt(value, radix);
		if (i >> 16 == 0) {
			return (short) i;
		}
		else {
			throw new NumberFormatException("out of range: " + i);
		}
	}

	private static <T> @Nullable T decodeFloat(
			DynamicOps<T> ops,
			SnbtParsing.Sign sign,
			@Nullable String intPart,
			@Nullable String fractionalPart,
			SnbtParsing.@Nullable SignedValue<String> exponent,
			SnbtParsing.@Nullable NumericType type,
			ParsingState<?> state
	) {
		StringBuilder stringBuilder = new StringBuilder();
		sign.append(stringBuilder);
		if (intPart != null) {
			skipUnderscoreAndAppend(stringBuilder, intPart);
		}

		if (fractionalPart != null) {
			stringBuilder.append('.');
			skipUnderscoreAndAppend(stringBuilder, fractionalPart);
		}

		if (exponent != null) {
			stringBuilder.append('e');
			exponent.sign().append(stringBuilder);
			skipUnderscoreAndAppend(stringBuilder, exponent.value);
		}

		try {
			String string = stringBuilder.toString();

			return (T) (switch (type) {
				case null -> (Object) parseFiniteDouble(ops, state, string);
				case FLOAT -> (Object) parseFiniteFloat(ops, state, string);
				case DOUBLE -> (Object) parseFiniteDouble(ops, state, string);
				default -> {
					state.getErrors().add(state.getCursor(), EXPECTED_FLOAT_TYPE_EXCEPTION);
					yield null;
				}
			}
			);
		}
		catch (NumberFormatException var11) {
			state.getErrors().add(state.getCursor(), toNumberParseFailure(var11));
			return null;
		}
	}

	private static <T> @Nullable T parseFiniteFloat(DynamicOps<T> ops, ParsingState<?> state, String value) {
		float f = Float.parseFloat(value);
		if (!Float.isFinite(f)) {
			state.getErrors().add(state.getCursor(), INFINITY_NOT_ALLOWED_EXCEPTION);
			return null;
		}
		else {
			return (T) ops.createFloat(f);
		}
	}

	private static <T> @Nullable T parseFiniteDouble(DynamicOps<T> ops, ParsingState<?> state, String value) {
		double d = Double.parseDouble(value);
		if (!Double.isFinite(d)) {
			state.getErrors().add(state.getCursor(), INFINITY_NOT_ALLOWED_EXCEPTION);
			return null;
		}
		else {
			return (T) ops.createDouble(d);
		}
	}

	private static String join(List<String> values) {
		return switch (values.size()) {
			case 0 -> "";
			case 1 -> (String) values.getFirst();
			default -> String.join("", values);
		};
	}

	/**
	 * Создаёт parser.
	 *
	 * @param ops ops
	 *
	 * @return PackratParser — результат операции
	 */
	public static <T> PackratParser<T> createParser(DynamicOps<T> ops) {
		T object = (T) ops.createBoolean(true);
		T object2 = (T) ops.createBoolean(false);
		T object3 = (T) ops.emptyMap();
		T object4 = (T) ops.emptyList();
		ParsingRules<StringReader> parsingRules = new ParsingRules<>();
		Symbol<SnbtParsing.Sign> symbol = Symbol.of("sign");
		parsingRules.set(
				symbol,
				Term.anyOf(
						Term.sequence(Literals.character('+'), Term.always(symbol, SnbtParsing.Sign.PLUS)),
						Term.sequence(Literals.character('-'), Term.always(symbol, SnbtParsing.Sign.MINUS))
				),
				results -> results.getOrThrow(symbol)
		);
		Symbol<SnbtParsing.NumberSuffix> symbol2 = Symbol.of("integer_suffix");
		parsingRules.set(
				symbol2,
				Term.anyOf(
						Term.sequence(
								Literals.character('u', 'U'),
								Term.anyOf(
										Term.sequence(
												Literals.character('b', 'B'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.UNSIGNED,
																SnbtParsing.NumericType.BYTE
														)
												)
										),
										Term.sequence(
												Literals.character('s', 'S'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.UNSIGNED,
																SnbtParsing.NumericType.SHORT
														)
												)
										),
										Term.sequence(
												Literals.character('i', 'I'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.UNSIGNED,
																SnbtParsing.NumericType.INT
														)
												)
										),
										Term.sequence(
												Literals.character('l', 'L'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.UNSIGNED,
																SnbtParsing.NumericType.LONG
														)
												)
										)
								)
						),
						Term.sequence(
								Literals.character('s', 'S'),
								Term.anyOf(
										Term.sequence(
												Literals.character('b', 'B'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.SIGNED,
																SnbtParsing.NumericType.BYTE
														)
												)
										),
										Term.sequence(
												Literals.character('s', 'S'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.SIGNED,
																SnbtParsing.NumericType.SHORT
														)
												)
										),
										Term.sequence(
												Literals.character('i', 'I'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.SIGNED,
																SnbtParsing.NumericType.INT
														)
												)
										),
										Term.sequence(
												Literals.character('l', 'L'),
												Term.always(
														symbol2,
														new SnbtParsing.NumberSuffix(
																SnbtParsing.Signedness.SIGNED,
																SnbtParsing.NumericType.LONG
														)
												)
										)
								)
						),
						Term.sequence(
								Literals.character('b', 'B'),
								Term.always(symbol2, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.BYTE))
						),
						Term.sequence(
								Literals.character('s', 'S'),
								Term.always(symbol2, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.SHORT))
						),
						Term.sequence(
								Literals.character('i', 'I'),
								Term.always(symbol2, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.INT))
						),
						Term.sequence(
								Literals.character('l', 'L'),
								Term.always(symbol2, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.LONG))
						)
				),
				results -> results.getOrThrow(symbol2)
		);
		Symbol<String> symbol3 = Symbol.of("binary_numeral");
		parsingRules.set(symbol3, BINARY_RULE);
		Symbol<String> symbol4 = Symbol.of("decimal_numeral");
		parsingRules.set(symbol4, DECIMAL_RULE);
		Symbol<String> symbol5 = Symbol.of("hex_numeral");
		parsingRules.set(symbol5, HEX_RULE);
		Symbol<SnbtParsing.IntValue> symbol6 = Symbol.of("integer_literal");
		ParsingRuleEntry<StringReader, SnbtParsing.IntValue> parsingRuleEntry = parsingRules.set(
				symbol6,
				Term.sequence(
						Term.optional(parsingRules.term(symbol)),
						Term.anyOf(
								Term.sequence(
										Literals.character('0'),
										Term.cutting(),
										Term.anyOf(
												Term.sequence(
														Literals.character('x', 'X'),
														Term.cutting(),
														parsingRules.term(symbol5)
												),
												Term.sequence(Literals.character('b', 'B'), parsingRules.term(symbol3)),
												Term.sequence(
														parsingRules.term(symbol4),
														Term.cutting(),
														Term.fail(LEADING_ZERO_NOT_ALLOWED_EXCEPTION)
												),
												Term.always(symbol4, "0")
										)
								),
								parsingRules.term(symbol4)
						),
						Term.optional(parsingRules.term(symbol2))
				),
				results -> {
					SnbtParsing.NumberSuffix
							numberSuffix =
							results.getOrDefault(symbol2, SnbtParsing.NumberSuffix.DEFAULT);
					SnbtParsing.Sign sign = results.getOrDefault(symbol, SnbtParsing.Sign.PLUS);
					String string = results.get(symbol4);
					if (string != null) {
						return new SnbtParsing.IntValue(sign, SnbtParsing.Radix.DECIMAL, string, numberSuffix);
					}
					else {
						String string2 = results.get(symbol5);
						if (string2 != null) {
							return new SnbtParsing.IntValue(sign, SnbtParsing.Radix.HEX, string2, numberSuffix);
						}
						else {
							String string3 = results.getOrThrow(symbol3);
							return new SnbtParsing.IntValue(sign, SnbtParsing.Radix.BINARY, string3, numberSuffix);
						}
					}
				}
		);
		Symbol<SnbtParsing.NumericType> symbol7 = Symbol.of("float_type_suffix");
		parsingRules.set(
				symbol7,
				Term.anyOf(
						Term.sequence(
								Literals.character('f', 'F'),
								Term.always(symbol7, SnbtParsing.NumericType.FLOAT)
						),
						Term.sequence(
								Literals.character('d', 'D'),
								Term.always(symbol7, SnbtParsing.NumericType.DOUBLE)
						)
				),
				results -> results.getOrThrow(symbol7)
		);
		Symbol<SnbtParsing.SignedValue<String>> symbol8 = Symbol.of("float_exponent_part");
		parsingRules.set(
				symbol8,
				Term.sequence(
						Literals.character('e', 'E'),
						Term.optional(parsingRules.term(symbol)),
						parsingRules.term(symbol4)
				),
				results -> new SnbtParsing.SignedValue<>(
						results.getOrDefault(symbol, SnbtParsing.Sign.PLUS),
						results.getOrThrow(symbol4)
				)
		);
		Symbol<String> symbol9 = Symbol.of("float_whole_part");
		Symbol<String> symbol10 = Symbol.of("float_fraction_part");
		Symbol<T> symbol11 = Symbol.of("float_literal");
		parsingRules.set(
				symbol11,
				Term.sequence(
						Term.optional(parsingRules.term(symbol)),
						Term.anyOf(
								Term.sequence(
										parsingRules.term(symbol4, symbol9),
										Literals.character('.'),
										Term.cutting(),
										Term.optional(parsingRules.term(symbol4, symbol10)),
										Term.optional(parsingRules.term(symbol8)),
										Term.optional(parsingRules.term(symbol7))
								),
								Term.sequence(
										Literals.character('.'),
										Term.cutting(),
										parsingRules.term(symbol4, symbol10),
										Term.optional(parsingRules.term(symbol8)),
										Term.optional(parsingRules.term(symbol7))
								),
								Term.sequence(
										parsingRules.term(symbol4, symbol9),
										parsingRules.term(symbol8),
										Term.cutting(),
										Term.optional(parsingRules.term(symbol7))
								),
								Term.sequence(
										parsingRules.term(symbol4, symbol9),
										Term.optional(parsingRules.term(symbol8)),
										parsingRules.term(symbol7)
								)
						)
				),
				(ParsingRule.RuleAction<StringReader, T>) state -> {
					ParseResults parseResults = state.getResults();
					SnbtParsing.Sign sign = parseResults.getOrDefault(symbol, SnbtParsing.Sign.PLUS);
					String string = parseResults.get(symbol9);
					String string2 = parseResults.get(symbol10);
					SnbtParsing.SignedValue<String> signedValue = parseResults.get(symbol8);
					SnbtParsing.NumericType numericType = parseResults.get(symbol7);
					return decodeFloat(ops, sign, string, string2, signedValue, numericType, state);
				}
		);
		Symbol<String> symbol12 = Symbol.of("string_hex_2");
		parsingRules.set(symbol12, new SnbtParsing.HexParsingRule(2));
		Symbol<String> symbol13 = Symbol.of("string_hex_4");
		parsingRules.set(symbol13, new SnbtParsing.HexParsingRule(4));
		Symbol<String> symbol14 = Symbol.of("string_hex_8");
		parsingRules.set(symbol14, new SnbtParsing.HexParsingRule(8));
		Symbol<String> symbol15 = Symbol.of("string_unicode_name");
		parsingRules.set(symbol15, new PatternParsingRule(UNICODE_NAME_PATTERN, INVALID_CHARACTER_NAME_EXCEPTION));
		Symbol<String> symbol16 = Symbol.of("string_escape_sequence");
		parsingRules.set(
				symbol16,
				Term.anyOf(
						Term.sequence(Literals.character('b'), Term.always(symbol16, "\b")),
						Term.sequence(Literals.character('s'), Term.always(symbol16, " ")),
						Term.sequence(Literals.character('t'), Term.always(symbol16, "\t")),
						Term.sequence(Literals.character('n'), Term.always(symbol16, "\n")),
						Term.sequence(Literals.character('f'), Term.always(symbol16, "\f")),
						Term.sequence(Literals.character('r'), Term.always(symbol16, "\r")),
						Term.sequence(Literals.character('\\'), Term.always(symbol16, "\\")),
						Term.sequence(Literals.character('\''), Term.always(symbol16, "'")),
						Term.sequence(Literals.character('"'), Term.always(symbol16, "\"")),
						Term.sequence(Literals.character('x'), parsingRules.term(symbol12)),
						Term.sequence(Literals.character('u'), parsingRules.term(symbol13)),
						Term.sequence(Literals.character('U'), parsingRules.term(symbol14)),
						Term.sequence(
								Literals.character('N'),
								Literals.character('{'),
								parsingRules.term(symbol15),
								Literals.character('}')
						)
				),
				(ParsingRule.RuleAction<StringReader, String>) state -> {
					ParseResults parseResults = state.getResults();
					String string = parseResults.getAny(symbol16);
					if (string != null) {
						return string;
					}
					else {
						String string2 = parseResults.getAny(symbol12, symbol13, symbol14);
						if (string2 != null) {
							int i = HexFormat.fromHexDigits(string2);
							if (!Character.isValidCodePoint(i)) {
								state
										.getErrors()
										.add(
												state.getCursor(),
												CursorExceptionType.create(
														INVALID_CODEPOINT_EXCEPTION,
														String.format(Locale.ROOT, "U+%08X", i)
												)
										);
								return null;
							}
							else {
								return Character.toString(i);
							}
						}
						else {
							String string3 = parseResults.getOrThrow(symbol15);

							int j;
							try {
								j = Character.codePointOf(string3);
							}
							catch (IllegalArgumentException var12x) {
								state.getErrors().add(state.getCursor(), INVALID_CHARACTER_NAME_EXCEPTION);
								return null;
							}

							return Character.toString(j);
						}
					}
				}
		);
		Symbol<String> symbol17 = Symbol.of("string_plain_contents");
		parsingRules.set(symbol17, UNQUOTED_STRING_RULE);
		Symbol<List<String>> symbol18 = Symbol.of("string_chunks");
		Symbol<String> symbol19 = Symbol.of("string_contents");
		Symbol<String> symbol20 = Symbol.of("single_quoted_string_chunk");
		ParsingRuleEntry<StringReader, String> parsingRuleEntry2 = parsingRules.set(
				symbol20,
				Term.anyOf(
						parsingRules.term(symbol17, symbol19),
						Term.sequence(Literals.character('\\'), parsingRules.term(symbol16, symbol19)),
						Term.sequence(Literals.character('"'), Term.always(symbol19, "\""))
				),
				results -> results.getOrThrow(symbol19)
		);
		Symbol<String> symbol21 = Symbol.of("single_quoted_string_contents");
		parsingRules.set(
				symbol21,
				Term.repeated(parsingRuleEntry2, symbol18),
				results -> join(results.getOrThrow(symbol18))
		);
		Symbol<String> symbol22 = Symbol.of("double_quoted_string_chunk");
		ParsingRuleEntry<StringReader, String> parsingRuleEntry3 = parsingRules.set(
				symbol22,
				Term.anyOf(
						parsingRules.term(symbol17, symbol19),
						Term.sequence(Literals.character('\\'), parsingRules.term(symbol16, symbol19)),
						Term.sequence(Literals.character('\''), Term.always(symbol19, "'"))
				),
				results -> results.getOrThrow(symbol19)
		);
		Symbol<String> symbol23 = Symbol.of("double_quoted_string_contents");
		parsingRules.set(
				symbol23,
				Term.repeated(parsingRuleEntry3, symbol18),
				results -> join(results.getOrThrow(symbol18))
		);
		Symbol<String> symbol24 = Symbol.of("quoted_string_literal");
		parsingRules.set(
				symbol24,
				Term.anyOf(
						Term.sequence(
								Literals.character('"'),
								Term.cutting(),
								Term.optional(parsingRules.term(symbol23, symbol19)),
								Literals.character('"')
						),
						Term.sequence(
								Literals.character('\''),
								Term.optional(parsingRules.term(symbol21, symbol19)),
								Literals.character('\'')
						)
				),
				results -> results.getOrThrow(symbol19)
		);
		Symbol<String> symbol25 = Symbol.of("unquoted_string");
		parsingRules.set(symbol25, new UnquotedStringParsingRule(1, EXPECTED_UNQUOTED_STRING_EXCEPTION));
		Symbol<T> symbol26 = Symbol.of("literal");
		Symbol<List<T>> symbol27 = Symbol.of("arguments");
		parsingRules.set(
				symbol27,
				Term.repeatWithPossiblyTrailingSeparator(
						parsingRules.getOrCreate(symbol26),
						symbol27,
						Literals.character(',')
				),
				parseResults -> parseResults.getOrThrow(symbol27)
		);
		Symbol<T> symbol28 = Symbol.of("unquoted_string_or_builtin");
		parsingRules.set(
				symbol28,
				Term.sequence(
						parsingRules.term(symbol25),
						Term.optional(Term.sequence(
								Literals.character('('),
								parsingRules.term(symbol27),
								Literals.character(')')
						))
				),
				(ParsingRule.RuleAction<StringReader, T>) state -> {
					ParseResults parseResults = state.getResults();
					String string = parseResults.getOrThrow(symbol25);
					if (!string.isEmpty() && canUnquotedStringStartWith(string.charAt(0))) {
						List<T> list = parseResults.get(symbol27);
						if (list != null) {
							SnbtOperation.Type type = new SnbtOperation.Type(string, list.size());
							SnbtOperation.Operator operator = SnbtOperation.OPERATIONS.get(type);
							if (operator != null) {
								return operator.apply(ops, list, state);
							}
							else {
								state
										.getErrors()
										.add(
												state.getCursor(),
												CursorExceptionType.create(NO_SUCH_OPERATION_EXCEPTION, type.toString())
										);
								return null;
							}
						}
						else if (string.equalsIgnoreCase("true")) {
							return object;
						}
						else {
							return (T) (string.equalsIgnoreCase("false") ? object2 : ops.createString(string));
						}
					}
					else {
						state
								.getErrors()
								.add(state.getCursor(), SnbtOperation.SUGGESTIONS, INVALID_UNQUOTED_START_EXCEPTION);
						return null;
					}
				}
		);
		Symbol<String> symbol29 = Symbol.of("map_key");
		parsingRules.set(
				symbol29,
				Term.anyOf(parsingRules.term(symbol24), parsingRules.term(symbol25)),
				results -> results.getAnyOrThrow(symbol24, symbol25)
		);
		Symbol<Entry<String, T>> symbol30 = Symbol.of("map_entry");
		ParsingRuleEntry<StringReader, Entry<String, T>> parsingRuleEntry4 = parsingRules.set(
				symbol30,
				Term.sequence(parsingRules.term(symbol29), Literals.character(':'), parsingRules.term(symbol26)),
				(ParsingRule.RuleAction<StringReader, Entry<String, T>>) state -> {
					ParseResults parseResults = state.getResults();
					String string = parseResults.getOrThrow(symbol29);
					if (string.isEmpty()) {
						state.getErrors().add(state.getCursor(), EMPTY_KEY_EXCEPTION);
						return null;
					}
					else {
						T objectx = parseResults.getOrThrow(symbol26);
						return Map.entry(string, objectx);
					}
				}
		);
		Symbol<List<Entry<String, T>>> symbol31 = Symbol.of("map_entries");
		parsingRules.set(
				symbol31,
				Term.repeatWithPossiblyTrailingSeparator(parsingRuleEntry4, symbol31, Literals.character(',')),
				results -> results.getOrThrow(symbol31)
		);
		Symbol<T> symbol32 = Symbol.of("map_literal");
		parsingRules.set(
				symbol32,
				Term.sequence(Literals.character('{'), parsingRules.term(symbol31), Literals.character('}')),
				results -> {
					List<Entry<String, T>> list = results.getOrThrow(symbol31);
					if (list.isEmpty()) {
						return object3;
					}
					else {
						Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(list.size());

						for (Entry<String, T> entry : list) {
							builder.put(ops.createString(entry.getKey()), entry.getValue());
						}

						return (T) ops.createMap(builder.buildKeepingLast());
					}
				}
		);
		Symbol<List<T>> symbol33 = Symbol.of("list_entries");
		parsingRules.set(
				symbol33,
				Term.repeatWithPossiblyTrailingSeparator(
						parsingRules.getOrCreate(symbol26),
						symbol33,
						Literals.character(',')
				),
				results -> results.getOrThrow(symbol33)
		);
		Symbol<SnbtParsing.ArrayType> symbol34 = Symbol.of("array_prefix");
		parsingRules.set(
				symbol34,
				Term.anyOf(
						Term.sequence(Literals.character('B'), Term.always(symbol34, SnbtParsing.ArrayType.BYTE)),
						Term.sequence(Literals.character('L'), Term.always(symbol34, SnbtParsing.ArrayType.LONG)),
						Term.sequence(Literals.character('I'), Term.always(symbol34, SnbtParsing.ArrayType.INT))
				),
				results -> results.getOrThrow(symbol34)
		);
		Symbol<List<SnbtParsing.IntValue>> symbol35 = Symbol.of("int_array_entries");
		parsingRules.set(
				symbol35,
				Term.repeatWithPossiblyTrailingSeparator(parsingRuleEntry, symbol35, Literals.character(',')),
				results -> results.getOrThrow(symbol35)
		);
		Symbol<T> symbol36 = Symbol.of("list_literal");
		parsingRules.set(
				symbol36,
				Term.sequence(
						Literals.character('['),
						Term.anyOf(
								Term.sequence(
										parsingRules.term(symbol34),
										Literals.character(';'),
										parsingRules.term(symbol35)
								), parsingRules.term(symbol33)
						),
						Literals.character(']')
				),
				(ParsingRule.RuleAction<StringReader, T>) state -> {
					ParseResults parseResults = state.getResults();
					SnbtParsing.ArrayType arrayType = parseResults.get(symbol34);
					if (arrayType != null) {
						List<SnbtParsing.IntValue> list = parseResults.getOrThrow(symbol35);
						return list.isEmpty() ? arrayType.createEmpty(ops) : arrayType.decode(ops, list, state);
					}
					else {
						List<T> list = parseResults.getOrThrow(symbol33);
						return (T) (list.isEmpty() ? object4 : ops.createList(list.stream()));
					}
				}
		);
		ParsingRuleEntry<StringReader, T> parsingRuleEntry5 = parsingRules.set(
				symbol26,
				Term.anyOf(
						Term.sequence(
								Term.positiveLookahead(DECIMAL_CHAR),
								Term.anyOf(parsingRules.term(symbol11, symbol26), parsingRules.term(symbol6))
						),
						Term.sequence(
								Term.positiveLookahead(Literals.character('"', '\'')),
								Term.cutting(),
								parsingRules.term(symbol24)
						),
						Term.sequence(
								Term.positiveLookahead(Literals.character('{')),
								Term.cutting(),
								parsingRules.term(symbol32, symbol26)
						),
						Term.sequence(
								Term.positiveLookahead(Literals.character('[')),
								Term.cutting(),
								parsingRules.term(symbol36, symbol26)
						),
						parsingRules.term(symbol28, symbol26)
				),
				(ParsingRule.RuleAction<StringReader, T>) state -> {
					ParseResults parseResults = state.getResults();
					String string = parseResults.get(symbol24);
					if (string != null) {
						return (T) ops.createString(string);
					}
					else {
						SnbtParsing.IntValue intValue = parseResults.get(symbol6);
						return intValue != null ? intValue.decode(ops, state) : parseResults.getOrThrow(symbol26);
					}
				}
		);
		return new PackratParser<>(parsingRules, parsingRuleEntry5);
	}

	/**
	 * {@code ArrayType}.
	 */
	static enum ArrayType {
		BYTE(SnbtParsing.NumericType.BYTE) {
			private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

			@Override
			public <T> T createEmpty(DynamicOps<T> ops) {
				return (T) ops.createByteList(EMPTY_BUFFER);
			}

			@Override
			public <T> @Nullable T decode(DynamicOps<T> ops, List<SnbtParsing.IntValue> values, ParsingState<?> state) {
				ByteList byteList = new ByteArrayList();

				for (SnbtParsing.IntValue intValue : values) {
					Number number = this.decode(intValue, state);
					if (number == null) {
						return null;
					}

					byteList.add(number.byteValue());
				}

				return (T) ops.createByteList(ByteBuffer.wrap(byteList.toByteArray()));
			}
		},
		INT(SnbtParsing.NumericType.INT, SnbtParsing.NumericType.BYTE, SnbtParsing.NumericType.SHORT) {
			@Override
			public <T> T createEmpty(DynamicOps<T> ops) {
				return (T) ops.createIntList(IntStream.empty());
			}

			@Override
			public <T> @Nullable T decode(DynamicOps<T> ops, List<SnbtParsing.IntValue> values, ParsingState<?> state) {
				java.util.stream.IntStream.Builder builder = IntStream.builder();

				for (SnbtParsing.IntValue intValue : values) {
					Number number = this.decode(intValue, state);
					if (number == null) {
						return null;
					}

					builder.add(number.intValue());
				}

				return (T) ops.createIntList(builder.build());
			}
		},
		LONG(
				SnbtParsing.NumericType.LONG,
				SnbtParsing.NumericType.BYTE,
				SnbtParsing.NumericType.SHORT,
				SnbtParsing.NumericType.INT
		) {
			@Override
			public <T> T createEmpty(DynamicOps<T> ops) {
				return (T) ops.createLongList(LongStream.empty());
			}

			@Override
			public <T> @Nullable T decode(DynamicOps<T> ops, List<SnbtParsing.IntValue> values, ParsingState<?> state) {
				java.util.stream.LongStream.Builder builder = LongStream.builder();

				for (SnbtParsing.IntValue intValue : values) {
					Number number = this.decode(intValue, state);
					if (number == null) {
						return null;
					}

					builder.add(number.longValue());
				}

				return (T) ops.createLongList(builder.build());
			}
		};

		private final SnbtParsing.NumericType elementType;
		private final Set<SnbtParsing.NumericType> castableTypes;

		ArrayType(final SnbtParsing.NumericType elementType, final SnbtParsing.NumericType... castableTypes) {
			this.castableTypes = Set.of(castableTypes);
			this.elementType = elementType;
		}

		public boolean isTypeAllowed(SnbtParsing.NumericType type) {
			return type == this.elementType || this.castableTypes.contains(type);
		}

		/**
		 * Создаёт empty.
		 *
		 * @param ops ops
		 *
		 * @return T — результат операции
		 */
		public abstract <T> T createEmpty(DynamicOps<T> ops);

		public abstract <T> @Nullable T decode(
				DynamicOps<T> ops,
				List<SnbtParsing.IntValue> values,
				ParsingState<?> state
		);

		/**
		 * Decode.
		 *
		 * @param value value
		 * @param state state
		 *
		 * @return @Nullable Number — результат операции
		 */
		protected @Nullable Number decode(SnbtParsing.IntValue value, ParsingState<?> state) {
			SnbtParsing.NumericType numericType = this.getType(value.suffix);
			if (numericType == null) {
				state.getErrors().add(state.getCursor(), SnbtParsing.INVALID_ARRAY_ELEMENT_TYPE_EXCEPTION);
				return null;
			}
			else {
				return (Number) value.decode(JavaOps.INSTANCE, numericType, state);
			}
		}

		private SnbtParsing.@Nullable NumericType getType(SnbtParsing.NumberSuffix suffix) {
			SnbtParsing.NumericType numericType = suffix.type();
			if (numericType == null) {
				return this.elementType;
			}
			else {
				return !this.isTypeAllowed(numericType) ? null : numericType;
			}
		}
	}

	/**
	 * {@code HexParsingRule}.
	 */
	static class HexParsingRule extends TokenParsingRule {

		public HexParsingRule(int length) {
			super(
					length,
					length,
					CursorExceptionType.create(SnbtParsing.EXPECTED_HEX_ESCAPE_EXCEPTION, String.valueOf(length))
			);
		}

		@Override
		protected boolean isValidChar(char c) {
			return switch (c) {
				case '0',
				     '1',
				     '2',
				     '3',
				     '4',
				     '5',
				     '6',
				     '7',
				     '8',
				     '9',
				     'A',
				     'B',
				     'C',
				     'D',
				     'E',
				     'F',
				     'a',
				     'b',
				     'c',
				     'd',
				     'e',
				     'f' -> true;
				default -> false;
			};
		}
	}

	/**
	 * {@code IntValue}.
	 */
	record IntValue(SnbtParsing.Sign sign, SnbtParsing.Radix base, String digits, SnbtParsing.NumberSuffix suffix) {

		private SnbtParsing.Signedness getSignedness() {
			if (this.suffix.signed != null) {
				return this.suffix.signed;
			}
			else {
				return switch (this.base) {
					case BINARY, HEX -> SnbtParsing.Signedness.UNSIGNED;
					case DECIMAL -> SnbtParsing.Signedness.SIGNED;
				};
			}
		}

		private String toString(SnbtParsing.Sign sign) {
			boolean bl = SnbtParsing.containsUnderscore(this.digits);
			if (sign != SnbtParsing.Sign.MINUS && !bl) {
				return this.digits;
			}
			else {
				StringBuilder stringBuilder = new StringBuilder();
				sign.append(stringBuilder);
				SnbtParsing.append(stringBuilder, this.digits, bl);
				return stringBuilder.toString();
			}
		}

		/**
		 * Decode.
		 *
		 * @param ops ops
		 * @param state state
		 *
		 * @return @Nullable T — результат операции
		 */
		public <T> @Nullable T decode(DynamicOps<T> ops, ParsingState<?> state) {
			return this.decode(ops, Objects.requireNonNullElse(this.suffix.type, SnbtParsing.NumericType.INT), state);
		}

		/**
		 * Decode.
		 *
		 * @param ops ops
		 * @param type type
		 * @param state state
		 *
		 * @return @Nullable T — результат операции
		 */
		public <T> @Nullable T decode(DynamicOps<T> ops, SnbtParsing.NumericType type, ParsingState<?> state) {
			boolean bl = this.getSignedness() == SnbtParsing.Signedness.SIGNED;
			if (!bl && this.sign == SnbtParsing.Sign.MINUS) {
				state.getErrors().add(state.getCursor(), SnbtParsing.EXPECTED_NON_NEGATIVE_NUMBER_EXCEPTION);
				return null;
			}
			else {
				String string = this.toString(this.sign);

				int i = switch (this.base) {
					case BINARY -> 2;
					case DECIMAL -> 10;
					case HEX -> 16;
				};

				try {
					if (bl) {
						return (T) (switch (type) {
							case BYTE -> (Object) ops.createByte(Byte.parseByte(string, i));
							case SHORT -> (Object) ops.createShort(Short.parseShort(string, i));
							case INT -> (Object) ops.createInt(Integer.parseInt(string, i));
							case LONG -> (Object) ops.createLong(Long.parseLong(string, i));
							default -> {
								state.getErrors().add(state.getCursor(), SnbtParsing.EXPECTED_INTEGER_TYPE_EXCEPTION);
								yield null;
							}
						}
						);
					}
					else {
						return (T) (switch (type) {
							case BYTE -> (Object) ops.createByte(UnsignedBytes.parseUnsignedByte(string, i));
							case SHORT -> (Object) ops.createShort(SnbtParsing.parseUnsignedShort(string, i));
							case INT -> (Object) ops.createInt(Integer.parseUnsignedInt(string, i));
							case LONG -> (Object) ops.createLong(Long.parseUnsignedLong(string, i));
							default -> {
								state.getErrors().add(state.getCursor(), SnbtParsing.EXPECTED_INTEGER_TYPE_EXCEPTION);
								yield null;
							}
						}
						);
					}
				}
				catch (NumberFormatException var8) {
					state.getErrors().add(state.getCursor(), SnbtParsing.toNumberParseFailure(var8));
					return null;
				}
			}
		}
	}

	/**
	 * {@code NumberSuffix}.
	 */
	record NumberSuffix(SnbtParsing.@Nullable Signedness signed, SnbtParsing.@Nullable NumericType type) {

		public static final SnbtParsing.NumberSuffix DEFAULT = new SnbtParsing.NumberSuffix(null, null);
	}

	/**
	 * {@code NumericType}.
	 */
	static enum NumericType {
		FLOAT,
		DOUBLE,
		BYTE,
		SHORT,
		INT,
		LONG;
	}

	/**
	 * {@code Radix}.
	 */
	static enum Radix {
		BINARY,
		DECIMAL,
		HEX;
	}

	/**
	 * {@code Sign}.
	 */
	static enum Sign {
		PLUS,
		MINUS;

		/**
		 * Append.
		 *
		 * @param builder builder
		 */
		public void append(StringBuilder builder) {
			if (this == MINUS) {
				builder.append("-");
			}
		}
	}

	/**
	 * {@code SignedValue}.
	 */
	record SignedValue<T>(SnbtParsing.Sign sign, T value) {
	}

	/**
	 * {@code Signedness}.
	 */
	static enum Signedness {
		SIGNED,
		UNSIGNED;
	}
}
