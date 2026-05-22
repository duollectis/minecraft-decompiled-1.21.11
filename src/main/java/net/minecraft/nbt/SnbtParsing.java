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
 * Парсер формата SNBT (Stringified NBT) на основе packrat-алгоритма.
 * <p>
 * Поддерживает полный синтаксис SNBT: числа с суффиксами типов и основаниями (двоичное, шестнадцатеричное),
 * строки с экранированием Unicode, массивы ({@code [B;}, {@code [I;}, {@code [L;}),
 * списки, compound-теги, а также встроенные операции {@code bool()} и {@code uuid()}.
 * <p>
 * Точка входа — {@link #createParser(DynamicOps)}, возвращающий готовый {@link PackratParser}.
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
	static final CursorExceptionType<CommandSyntaxException> EXPECTED_INTEGER_TYPE_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_integer_type"))
		);
	private static final CursorExceptionType<CommandSyntaxException> EXPECTED_FLOAT_TYPE_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_float_type"))
		);
	static final CursorExceptionType<CommandSyntaxException> EXPECTED_NON_NEGATIVE_NUMBER_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_non_negative_number"))
		);
	private static final CursorExceptionType<CommandSyntaxException> INVALID_CHARACTER_NAME_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_character_name"))
		);
	static final CursorExceptionType<CommandSyntaxException> INVALID_ARRAY_ELEMENT_TYPE_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_array_element_type"))
		);
	private static final CursorExceptionType<CommandSyntaxException> INVALID_UNQUOTED_START_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_unquoted_start"))
		);
	private static final CursorExceptionType<CommandSyntaxException> EXPECTED_UNQUOTED_STRING_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_unquoted_string"))
		);
	private static final CursorExceptionType<CommandSyntaxException> INVALID_STRING_CONTENTS_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.invalid_string_contents"))
		);
	private static final CursorExceptionType<CommandSyntaxException> EXPECTED_BINARY_NUMERAL_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_binary_numeral"))
		);
	private static final CursorExceptionType<CommandSyntaxException> UNDERSCORE_NOT_ALLOWED_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.underscore_not_allowed"))
		);
	private static final CursorExceptionType<CommandSyntaxException> EXPECTED_DECIMAL_NUMERAL_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_decimal_numeral"))
		);
	private static final CursorExceptionType<CommandSyntaxException> EXPECTED_HEX_NUMERAL_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.expected_hex_numeral"))
		);
	private static final CursorExceptionType<CommandSyntaxException> EMPTY_KEY_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.empty_key"))
		);
	private static final CursorExceptionType<CommandSyntaxException> LEADING_ZERO_NOT_ALLOWED_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.leading_zero_not_allowed"))
		);
	private static final CursorExceptionType<CommandSyntaxException> INFINITY_NOT_ALLOWED_EXCEPTION =
		CursorExceptionType.create(
			new SimpleCommandExceptionType(Text.translatable("snbt.parser.infinity_not_allowed"))
		);

	private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
	private static final char UNDERSCORE_CHAR = '_';
	private static final Pattern UNICODE_NAME_PATTERN = Pattern.compile("[-a-zA-Z0-9 ]+");

	private static final NumeralParsingRule BINARY_RULE =
		new NumeralParsingRule(EXPECTED_BINARY_NUMERAL_EXCEPTION, UNDERSCORE_NOT_ALLOWED_EXCEPTION) {
			@Override
			protected boolean accepts(char c) {
				return switch (c) {
					case '0', '1', '_' -> true;
					default -> false;
				};
			}
		};
	private static final NumeralParsingRule DECIMAL_RULE =
		new NumeralParsingRule(EXPECTED_DECIMAL_NUMERAL_EXCEPTION, UNDERSCORE_NOT_ALLOWED_EXCEPTION) {
			@Override
			protected boolean accepts(char c) {
				return switch (c) {
					case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
					default -> false;
				};
			}
		};
	private static final NumeralParsingRule HEX_RULE =
		new NumeralParsingRule(EXPECTED_HEX_NUMERAL_EXCEPTION, UNDERSCORE_NOT_ALLOWED_EXCEPTION) {
			@Override
			protected boolean accepts(char c) {
				return switch (c) {
					case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
					     'A', 'B', 'C', 'D', 'E', 'F',
					     'a', 'b', 'c', 'd', 'e', 'f',
					     '_' -> true;
					default -> false;
				};
			}
		};
	private static final TokenParsingRule UNQUOTED_STRING_RULE =
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

	static CursorExceptionType<CommandSyntaxException> toNumberParseFailure(NumberFormatException exception) {
		return CursorExceptionType.create(NUMBER_PARSE_FAILURE_EXCEPTION, exception.getMessage());
	}

	/**
	 * Возвращает строку экранирования для управляющего символа, или {@code null} если экранирование не нужно.
	 *
	 * @param c символ для проверки
	 * @return строка экранирования (например {@code "n"} для {@code '\n'}) или {@code null}
	 */
	public static @Nullable String escapeSpecialChar(char c) {
		return switch (c) {
			case '\b' -> "b";
			case '\t' -> "t";
			case '\n' -> "n";
			case '\f' -> "f";
			case '\r' -> "r";
			default -> c < ' ' ? "x" + HEX_FORMAT.toHexDigits((byte) c) : null;
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
		return string.indexOf(UNDERSCORE_CHAR) != -1;
	}

	private static void skipUnderscoreAndAppend(StringBuilder builder, String value) {
		append(builder, value, containsUnderscore(value));
	}

	static void append(StringBuilder builder, String value, boolean skipUnderscore) {
		if (skipUnderscore) {
			for (char c : value.toCharArray()) {
				if (c != UNDERSCORE_CHAR) {
					builder.append(c);
				}
			}
		}
		else {
			builder.append(value);
		}
	}

	static short parseUnsignedShort(String value, int radix) {
		int parsed = Integer.parseInt(value, radix);
		if (parsed >> 16 == 0) {
			return (short) parsed;
		}

		throw new NumberFormatException("out of range: " + parsed);
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
		StringBuilder builder = new StringBuilder();
		sign.append(builder);

		if (intPart != null) {
			skipUnderscoreAndAppend(builder, intPart);
		}

		if (fractionalPart != null) {
			builder.append('.');
			skipUnderscoreAndAppend(builder, fractionalPart);
		}

		if (exponent != null) {
			builder.append('e');
			exponent.sign().append(builder);
			skipUnderscoreAndAppend(builder, exponent.value);
		}

		try {
			String numberStr = builder.toString();
			return (T) (switch (type) {
				case null -> (Object) parseFiniteDouble(ops, state, numberStr);
				case FLOAT -> (Object) parseFiniteFloat(ops, state, numberStr);
				case DOUBLE -> (Object) parseFiniteDouble(ops, state, numberStr);
				default -> {
					state.getErrors().add(state.getCursor(), EXPECTED_FLOAT_TYPE_EXCEPTION);
					yield null;
				}
			});
		}
		catch (NumberFormatException exception) {
			state.getErrors().add(state.getCursor(), toNumberParseFailure(exception));
			return null;
		}
	}

	private static <T> @Nullable T parseFiniteFloat(DynamicOps<T> ops, ParsingState<?> state, String value) {
		float parsed = Float.parseFloat(value);
		if (!Float.isFinite(parsed)) {
			state.getErrors().add(state.getCursor(), INFINITY_NOT_ALLOWED_EXCEPTION);
			return null;
		}

		return (T) ops.createFloat(parsed);
	}

	private static <T> @Nullable T parseFiniteDouble(DynamicOps<T> ops, ParsingState<?> state, String value) {
		double parsed = Double.parseDouble(value);
		if (!Double.isFinite(parsed)) {
			state.getErrors().add(state.getCursor(), INFINITY_NOT_ALLOWED_EXCEPTION);
			return null;
		}

		return (T) ops.createDouble(parsed);
	}

	private static String join(List<String> values) {
		return switch (values.size()) {
			case 0 -> "";
			case 1 -> (String) values.getFirst();
			default -> String.join("", values);
		};
	}

	/**
	 * Создаёт packrat-парсер для разбора SNBT-строк в объекты целевого типа.
	 * <p>
	 * Парсер строится один раз и может использоваться многократно через {@link PackratParser#parse}.
	 * Поддерживает все типы NBT: числа, строки, массивы, списки, compound-теги и встроенные операции.
	 *
	 * @param ops операции сериализации целевого типа
	 * @param <T> целевой тип данных
	 * @return готовый парсер
	 */
	public static <T> PackratParser<T> createParser(DynamicOps<T> ops) {
		T trueValue = (T) ops.createBoolean(true);
		T falseValue = (T) ops.createBoolean(false);
		T emptyMap = (T) ops.emptyMap();
		T emptyList = (T) ops.emptyList();
		ParsingRules<StringReader> parsingRules = new ParsingRules<>();

		Symbol<SnbtParsing.Sign> signSymbol = Symbol.of("sign");
		parsingRules.set(
			signSymbol,
			Term.anyOf(
				Term.sequence(Literals.character('+'), Term.always(signSymbol, SnbtParsing.Sign.PLUS)),
				Term.sequence(Literals.character('-'), Term.always(signSymbol, SnbtParsing.Sign.MINUS))
			),
			results -> results.getOrThrow(signSymbol)
		);

		Symbol<SnbtParsing.NumberSuffix> intSuffixSymbol = Symbol.of("integer_suffix");
		parsingRules.set(
			intSuffixSymbol,
			Term.anyOf(
				Term.sequence(
					Literals.character('u', 'U'),
					Term.anyOf(
						Term.sequence(
							Literals.character('b', 'B'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.UNSIGNED, SnbtParsing.NumericType.BYTE))
						),
						Term.sequence(
							Literals.character('s', 'S'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.UNSIGNED, SnbtParsing.NumericType.SHORT))
						),
						Term.sequence(
							Literals.character('i', 'I'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.UNSIGNED, SnbtParsing.NumericType.INT))
						),
						Term.sequence(
							Literals.character('l', 'L'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.UNSIGNED, SnbtParsing.NumericType.LONG))
						)
					)
				),
				Term.sequence(
					Literals.character('s', 'S'),
					Term.anyOf(
						Term.sequence(
							Literals.character('b', 'B'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.SIGNED, SnbtParsing.NumericType.BYTE))
						),
						Term.sequence(
							Literals.character('s', 'S'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.SIGNED, SnbtParsing.NumericType.SHORT))
						),
						Term.sequence(
							Literals.character('i', 'I'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.SIGNED, SnbtParsing.NumericType.INT))
						),
						Term.sequence(
							Literals.character('l', 'L'),
							Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(SnbtParsing.Signedness.SIGNED, SnbtParsing.NumericType.LONG))
						)
					)
				),
				Term.sequence(Literals.character('b', 'B'), Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.BYTE))),
				Term.sequence(Literals.character('s', 'S'), Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.SHORT))),
				Term.sequence(Literals.character('i', 'I'), Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.INT))),
				Term.sequence(Literals.character('l', 'L'), Term.always(intSuffixSymbol, new SnbtParsing.NumberSuffix(null, SnbtParsing.NumericType.LONG)))
			),
			results -> results.getOrThrow(intSuffixSymbol)
		);

		Symbol<String> binaryNumeralSymbol = Symbol.of("binary_numeral");
		parsingRules.set(binaryNumeralSymbol, BINARY_RULE);
		Symbol<String> decimalNumeralSymbol = Symbol.of("decimal_numeral");
		parsingRules.set(decimalNumeralSymbol, DECIMAL_RULE);
		Symbol<String> hexNumeralSymbol = Symbol.of("hex_numeral");
		parsingRules.set(hexNumeralSymbol, HEX_RULE);

		Symbol<SnbtParsing.IntValue> intLiteralSymbol = Symbol.of("integer_literal");
		ParsingRuleEntry<StringReader, SnbtParsing.IntValue> intLiteralEntry = parsingRules.set(
			intLiteralSymbol,
			Term.sequence(
				Term.optional(parsingRules.term(signSymbol)),
				Term.anyOf(
					Term.sequence(
						Literals.character('0'),
						Term.cutting(),
						Term.anyOf(
							Term.sequence(
								Literals.character('x', 'X'),
								Term.cutting(),
								parsingRules.term(hexNumeralSymbol)
							),
							Term.sequence(Literals.character('b', 'B'), parsingRules.term(binaryNumeralSymbol)),
							Term.sequence(
								parsingRules.term(decimalNumeralSymbol),
								Term.cutting(),
								Term.fail(LEADING_ZERO_NOT_ALLOWED_EXCEPTION)
							),
							Term.always(decimalNumeralSymbol, "0")
						)
					),
					parsingRules.term(decimalNumeralSymbol)
				),
				Term.optional(parsingRules.term(intSuffixSymbol))
			),
			results -> {
				SnbtParsing.NumberSuffix suffix = results.getOrDefault(intSuffixSymbol, SnbtParsing.NumberSuffix.DEFAULT);
				SnbtParsing.Sign sign = results.getOrDefault(signSymbol, SnbtParsing.Sign.PLUS);
				String decimal = results.get(decimalNumeralSymbol);

				if (decimal != null) {
					return new SnbtParsing.IntValue(sign, SnbtParsing.Radix.DECIMAL, decimal, suffix);
				}

				String hex = results.get(hexNumeralSymbol);
				if (hex != null) {
					return new SnbtParsing.IntValue(sign, SnbtParsing.Radix.HEX, hex, suffix);
				}

				String binary = results.getOrThrow(binaryNumeralSymbol);
				return new SnbtParsing.IntValue(sign, SnbtParsing.Radix.BINARY, binary, suffix);
			}
		);

		Symbol<SnbtParsing.NumericType> floatSuffixSymbol = Symbol.of("float_type_suffix");
		parsingRules.set(
			floatSuffixSymbol,
			Term.anyOf(
				Term.sequence(Literals.character('f', 'F'), Term.always(floatSuffixSymbol, SnbtParsing.NumericType.FLOAT)),
				Term.sequence(Literals.character('d', 'D'), Term.always(floatSuffixSymbol, SnbtParsing.NumericType.DOUBLE))
			),
			results -> results.getOrThrow(floatSuffixSymbol)
		);

		Symbol<SnbtParsing.SignedValue<String>> floatExponentSymbol = Symbol.of("float_exponent_part");
		parsingRules.set(
			floatExponentSymbol,
			Term.sequence(
				Literals.character('e', 'E'),
				Term.optional(parsingRules.term(signSymbol)),
				parsingRules.term(decimalNumeralSymbol)
			),
			results -> new SnbtParsing.SignedValue<>(
				results.getOrDefault(signSymbol, SnbtParsing.Sign.PLUS),
				results.getOrThrow(decimalNumeralSymbol)
			)
		);

		Symbol<String> floatWholeSymbol = Symbol.of("float_whole_part");
		Symbol<String> floatFractionSymbol = Symbol.of("float_fraction_part");
		Symbol<T> floatLiteralSymbol = Symbol.of("float_literal");
		parsingRules.set(
			floatLiteralSymbol,
			Term.sequence(
				Term.optional(parsingRules.term(signSymbol)),
				Term.anyOf(
					Term.sequence(
						parsingRules.term(decimalNumeralSymbol, floatWholeSymbol),
						Literals.character('.'),
						Term.cutting(),
						Term.optional(parsingRules.term(decimalNumeralSymbol, floatFractionSymbol)),
						Term.optional(parsingRules.term(floatExponentSymbol)),
						Term.optional(parsingRules.term(floatSuffixSymbol))
					),
					Term.sequence(
						Literals.character('.'),
						Term.cutting(),
						parsingRules.term(decimalNumeralSymbol, floatFractionSymbol),
						Term.optional(parsingRules.term(floatExponentSymbol)),
						Term.optional(parsingRules.term(floatSuffixSymbol))
					),
					Term.sequence(
						parsingRules.term(decimalNumeralSymbol, floatWholeSymbol),
						parsingRules.term(floatExponentSymbol),
						Term.cutting(),
						Term.optional(parsingRules.term(floatSuffixSymbol))
					),
					Term.sequence(
						parsingRules.term(decimalNumeralSymbol, floatWholeSymbol),
						Term.optional(parsingRules.term(floatExponentSymbol)),
						parsingRules.term(floatSuffixSymbol)
					)
				)
			),
			(ParsingRule.RuleAction<StringReader, T>) state -> {
				ParseResults parseResults = state.getResults();
				SnbtParsing.Sign sign = parseResults.getOrDefault(signSymbol, SnbtParsing.Sign.PLUS);
				String wholePart = parseResults.get(floatWholeSymbol);
				String fractionPart = parseResults.get(floatFractionSymbol);
				SnbtParsing.SignedValue<String> exponent = parseResults.get(floatExponentSymbol);
				SnbtParsing.NumericType numericType = parseResults.get(floatSuffixSymbol);
				return decodeFloat(ops, sign, wholePart, fractionPart, exponent, numericType, state);
			}
		);

		Symbol<String> hex2Symbol = Symbol.of("string_hex_2");
		parsingRules.set(hex2Symbol, new SnbtParsing.HexParsingRule(2));
		Symbol<String> hex4Symbol = Symbol.of("string_hex_4");
		parsingRules.set(hex4Symbol, new SnbtParsing.HexParsingRule(4));
		Symbol<String> hex8Symbol = Symbol.of("string_hex_8");
		parsingRules.set(hex8Symbol, new SnbtParsing.HexParsingRule(8));
		Symbol<String> unicodeNameSymbol = Symbol.of("string_unicode_name");
		parsingRules.set(unicodeNameSymbol, new PatternParsingRule(UNICODE_NAME_PATTERN, INVALID_CHARACTER_NAME_EXCEPTION));

		Symbol<String> escapeSequenceSymbol = Symbol.of("string_escape_sequence");
		parsingRules.set(
			escapeSequenceSymbol,
			Term.anyOf(
				Term.sequence(Literals.character('b'), Term.always(escapeSequenceSymbol, "\b")),
				Term.sequence(Literals.character('s'), Term.always(escapeSequenceSymbol, " ")),
				Term.sequence(Literals.character('t'), Term.always(escapeSequenceSymbol, "\t")),
				Term.sequence(Literals.character('n'), Term.always(escapeSequenceSymbol, "\n")),
				Term.sequence(Literals.character('f'), Term.always(escapeSequenceSymbol, "\f")),
				Term.sequence(Literals.character('r'), Term.always(escapeSequenceSymbol, "\r")),
				Term.sequence(Literals.character('\\'), Term.always(escapeSequenceSymbol, "\\")),
				Term.sequence(Literals.character('\''), Term.always(escapeSequenceSymbol, "'")),
				Term.sequence(Literals.character('"'), Term.always(escapeSequenceSymbol, "\"")),
				Term.sequence(Literals.character('x'), parsingRules.term(hex2Symbol)),
				Term.sequence(Literals.character('u'), parsingRules.term(hex4Symbol)),
				Term.sequence(Literals.character('U'), parsingRules.term(hex8Symbol)),
				Term.sequence(
					Literals.character('N'),
					Literals.character('{'),
					parsingRules.term(unicodeNameSymbol),
					Literals.character('}')
				)
			),
			(ParsingRule.RuleAction<StringReader, String>) state -> {
				ParseResults parseResults = state.getResults();
				String directEscape = parseResults.getAny(escapeSequenceSymbol);

				if (directEscape != null) {
					return directEscape;
				}

				String hexDigits = parseResults.getAny(hex2Symbol, hex4Symbol, hex8Symbol);
				if (hexDigits != null) {
					int codePoint = HexFormat.fromHexDigits(hexDigits);
					if (!Character.isValidCodePoint(codePoint)) {
						state.getErrors().add(
							state.getCursor(),
							CursorExceptionType.create(
								INVALID_CODEPOINT_EXCEPTION,
								String.format(Locale.ROOT, "U+%08X", codePoint)
							)
						);
						return null;
					}

					return Character.toString(codePoint);
				}

				String unicodeName = parseResults.getOrThrow(unicodeNameSymbol);
				int codePoint;
				try {
					codePoint = Character.codePointOf(unicodeName);
				}
				catch (IllegalArgumentException ignored) {
					state.getErrors().add(state.getCursor(), INVALID_CHARACTER_NAME_EXCEPTION);
					return null;
				}

				return Character.toString(codePoint);
			}
		);

		Symbol<String> plainContentsSymbol = Symbol.of("string_plain_contents");
		parsingRules.set(plainContentsSymbol, UNQUOTED_STRING_RULE);
		Symbol<List<String>> stringChunksSymbol = Symbol.of("string_chunks");
		Symbol<String> stringContentsSymbol = Symbol.of("string_contents");

		Symbol<String> singleQuotedChunkSymbol = Symbol.of("single_quoted_string_chunk");
		ParsingRuleEntry<StringReader, String> singleQuotedChunkEntry = parsingRules.set(
			singleQuotedChunkSymbol,
			Term.anyOf(
				parsingRules.term(plainContentsSymbol, stringContentsSymbol),
				Term.sequence(Literals.character('\\'), parsingRules.term(escapeSequenceSymbol, stringContentsSymbol)),
				Term.sequence(Literals.character('"'), Term.always(stringContentsSymbol, "\""))
			),
			results -> results.getOrThrow(stringContentsSymbol)
		);
		Symbol<String> singleQuotedContentsSymbol = Symbol.of("single_quoted_string_contents");
		parsingRules.set(
			singleQuotedContentsSymbol,
			Term.repeated(singleQuotedChunkEntry, stringChunksSymbol),
			results -> join(results.getOrThrow(stringChunksSymbol))
		);

		Symbol<String> doubleQuotedChunkSymbol = Symbol.of("double_quoted_string_chunk");
		ParsingRuleEntry<StringReader, String> doubleQuotedChunkEntry = parsingRules.set(
			doubleQuotedChunkSymbol,
			Term.anyOf(
				parsingRules.term(plainContentsSymbol, stringContentsSymbol),
				Term.sequence(Literals.character('\\'), parsingRules.term(escapeSequenceSymbol, stringContentsSymbol)),
				Term.sequence(Literals.character('\''), Term.always(stringContentsSymbol, "'"))
			),
			results -> results.getOrThrow(stringContentsSymbol)
		);
		Symbol<String> doubleQuotedContentsSymbol = Symbol.of("double_quoted_string_contents");
		parsingRules.set(
			doubleQuotedContentsSymbol,
			Term.repeated(doubleQuotedChunkEntry, stringChunksSymbol),
			results -> join(results.getOrThrow(stringChunksSymbol))
		);

		Symbol<String> quotedStringSymbol = Symbol.of("quoted_string_literal");
		parsingRules.set(
			quotedStringSymbol,
			Term.anyOf(
				Term.sequence(
					Literals.character('"'),
					Term.cutting(),
					Term.optional(parsingRules.term(doubleQuotedContentsSymbol, stringContentsSymbol)),
					Literals.character('"')
				),
				Term.sequence(
					Literals.character('\''),
					Term.optional(parsingRules.term(singleQuotedContentsSymbol, stringContentsSymbol)),
					Literals.character('\'')
				)
			),
			results -> results.getOrThrow(stringContentsSymbol)
		);

		Symbol<String> unquotedStringSymbol = Symbol.of("unquoted_string");
		parsingRules.set(unquotedStringSymbol, new UnquotedStringParsingRule(1, EXPECTED_UNQUOTED_STRING_EXCEPTION));

		Symbol<T> literalSymbol = Symbol.of("literal");
		Symbol<List<T>> argumentsSymbol = Symbol.of("arguments");
		parsingRules.set(
			argumentsSymbol,
			Term.repeatWithPossiblyTrailingSeparator(
				parsingRules.getOrCreate(literalSymbol),
				argumentsSymbol,
				Literals.character(',')
			),
			parseResults -> parseResults.getOrThrow(argumentsSymbol)
		);

		Symbol<T> unquotedOrBuiltinSymbol = Symbol.of("unquoted_string_or_builtin");
		parsingRules.set(
			unquotedOrBuiltinSymbol,
			Term.sequence(
				parsingRules.term(unquotedStringSymbol),
				Term.optional(Term.sequence(
					Literals.character('('),
					parsingRules.term(argumentsSymbol),
					Literals.character(')')
				))
			),
			(ParsingRule.RuleAction<StringReader, T>) state -> {
				ParseResults parseResults = state.getResults();
				String name = parseResults.getOrThrow(unquotedStringSymbol);

				if (name.isEmpty() || !canUnquotedStringStartWith(name.charAt(0))) {
					state.getErrors().add(state.getCursor(), SnbtOperation.SUGGESTIONS, INVALID_UNQUOTED_START_EXCEPTION);
					return null;
				}

				List<T> args = parseResults.get(argumentsSymbol);
				if (args != null) {
					SnbtOperation.Type operationType = new SnbtOperation.Type(name, args.size());
					SnbtOperation.Operator operator = SnbtOperation.OPERATIONS.get(operationType);
					if (operator != null) {
						return operator.apply(ops, args, state);
					}

					state.getErrors().add(
						state.getCursor(),
						CursorExceptionType.create(NO_SUCH_OPERATION_EXCEPTION, operationType.toString())
					);
					return null;
				}

				if (name.equalsIgnoreCase(SnbtOperation.TRUE)) {
					return trueValue;
				}

				return (T) (name.equalsIgnoreCase(SnbtOperation.FALSE) ? falseValue : ops.createString(name));
			}
		);

		Symbol<String> mapKeySymbol = Symbol.of("map_key");
		parsingRules.set(
			mapKeySymbol,
			Term.anyOf(parsingRules.term(quotedStringSymbol), parsingRules.term(unquotedStringSymbol)),
			results -> results.getAnyOrThrow(quotedStringSymbol, unquotedStringSymbol)
		);

		Symbol<Entry<String, T>> mapEntrySymbol = Symbol.of("map_entry");
		ParsingRuleEntry<StringReader, Entry<String, T>> mapEntryEntry = parsingRules.set(
			mapEntrySymbol,
			Term.sequence(parsingRules.term(mapKeySymbol), Literals.character(':'), parsingRules.term(literalSymbol)),
			(ParsingRule.RuleAction<StringReader, Entry<String, T>>) state -> {
				ParseResults parseResults = state.getResults();
				String key = parseResults.getOrThrow(mapKeySymbol);

				if (key.isEmpty()) {
					state.getErrors().add(state.getCursor(), EMPTY_KEY_EXCEPTION);
					return null;
				}

				T value = parseResults.getOrThrow(literalSymbol);
				return Map.entry(key, value);
			}
		);

		Symbol<List<Entry<String, T>>> mapEntriesSymbol = Symbol.of("map_entries");
		parsingRules.set(
			mapEntriesSymbol,
			Term.repeatWithPossiblyTrailingSeparator(mapEntryEntry, mapEntriesSymbol, Literals.character(',')),
			results -> results.getOrThrow(mapEntriesSymbol)
		);

		Symbol<T> mapLiteralSymbol = Symbol.of("map_literal");
		parsingRules.set(
			mapLiteralSymbol,
			Term.sequence(Literals.character('{'), parsingRules.term(mapEntriesSymbol), Literals.character('}')),
			results -> {
				List<Entry<String, T>> entries = results.getOrThrow(mapEntriesSymbol);
				if (entries.isEmpty()) {
					return emptyMap;
				}

				Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(entries.size());
				for (Entry<String, T> entry : entries) {
					builder.put(ops.createString(entry.getKey()), entry.getValue());
				}

				return (T) ops.createMap(builder.buildKeepingLast());
			}
		);

		Symbol<List<T>> listEntriesSymbol = Symbol.of("list_entries");
		parsingRules.set(
			listEntriesSymbol,
			Term.repeatWithPossiblyTrailingSeparator(
				parsingRules.getOrCreate(literalSymbol),
				listEntriesSymbol,
				Literals.character(',')
			),
			results -> results.getOrThrow(listEntriesSymbol)
		);

		Symbol<SnbtParsing.ArrayType> arrayPrefixSymbol = Symbol.of("array_prefix");
		parsingRules.set(
			arrayPrefixSymbol,
			Term.anyOf(
				Term.sequence(Literals.character('B'), Term.always(arrayPrefixSymbol, SnbtParsing.ArrayType.BYTE)),
				Term.sequence(Literals.character('L'), Term.always(arrayPrefixSymbol, SnbtParsing.ArrayType.LONG)),
				Term.sequence(Literals.character('I'), Term.always(arrayPrefixSymbol, SnbtParsing.ArrayType.INT))
			),
			results -> results.getOrThrow(arrayPrefixSymbol)
		);

		Symbol<List<SnbtParsing.IntValue>> intArrayEntriesSymbol = Symbol.of("int_array_entries");
		parsingRules.set(
			intArrayEntriesSymbol,
			Term.repeatWithPossiblyTrailingSeparator(intLiteralEntry, intArrayEntriesSymbol, Literals.character(',')),
			results -> results.getOrThrow(intArrayEntriesSymbol)
		);

		Symbol<T> listLiteralSymbol = Symbol.of("list_literal");
		parsingRules.set(
			listLiteralSymbol,
			Term.sequence(
				Literals.character('['),
				Term.anyOf(
					Term.sequence(
						parsingRules.term(arrayPrefixSymbol),
						Literals.character(';'),
						parsingRules.term(intArrayEntriesSymbol)
					),
					parsingRules.term(listEntriesSymbol)
				),
				Literals.character(']')
			),
			(ParsingRule.RuleAction<StringReader, T>) state -> {
				ParseResults parseResults = state.getResults();
				SnbtParsing.ArrayType arrayType = parseResults.get(arrayPrefixSymbol);

				if (arrayType != null) {
					List<SnbtParsing.IntValue> values = parseResults.getOrThrow(intArrayEntriesSymbol);
					return values.isEmpty() ? arrayType.createEmpty(ops) : arrayType.decode(ops, values, state);
				}

				List<T> values = parseResults.getOrThrow(listEntriesSymbol);
				return (T) (values.isEmpty() ? emptyList : ops.createList(values.stream()));
			}
		);

		ParsingRuleEntry<StringReader, T> rootEntry = parsingRules.set(
			literalSymbol,
			Term.anyOf(
				Term.sequence(
					Term.positiveLookahead(DECIMAL_CHAR),
					Term.anyOf(parsingRules.term(floatLiteralSymbol, literalSymbol), parsingRules.term(intLiteralSymbol))
				),
				Term.sequence(
					Term.positiveLookahead(Literals.character('"', '\'')),
					Term.cutting(),
					parsingRules.term(quotedStringSymbol)
				),
				Term.sequence(
					Term.positiveLookahead(Literals.character('{')),
					Term.cutting(),
					parsingRules.term(mapLiteralSymbol, literalSymbol)
				),
				Term.sequence(
					Term.positiveLookahead(Literals.character('[')),
					Term.cutting(),
					parsingRules.term(listLiteralSymbol, literalSymbol)
				),
				parsingRules.term(unquotedOrBuiltinSymbol, literalSymbol)
			),
			(ParsingRule.RuleAction<StringReader, T>) state -> {
				ParseResults parseResults = state.getResults();
				String quotedStr = parseResults.get(quotedStringSymbol);

				if (quotedStr != null) {
					return (T) ops.createString(quotedStr);
				}

				SnbtParsing.IntValue intValue = parseResults.get(intLiteralSymbol);
				return intValue != null ? intValue.decode(ops, state) : parseResults.getOrThrow(literalSymbol);
			}
		);

		return new PackratParser<>(parsingRules, rootEntry);
	}

	/**
	 * Тип массива SNBT: байтовый, целочисленный или длинный.
	 */
	enum ArrayType {
		BYTE(SnbtParsing.NumericType.BYTE) {
			private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

			@Override
			public <T> T createEmpty(DynamicOps<T> ops) {
				return (T) ops.createByteList(EMPTY_BUFFER);
			}

			@Override
			public <T> @Nullable T decode(DynamicOps<T> ops, List<SnbtParsing.IntValue> values, ParsingState<?> state) {
				ByteList byteList = new ByteArrayList();

				for (SnbtParsing.IntValue value : values) {
					Number number = this.decode(value, state);
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

				for (SnbtParsing.IntValue value : values) {
					Number number = this.decode(value, state);
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

				for (SnbtParsing.IntValue value : values) {
					Number number = this.decode(value, state);
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
			return type == elementType || castableTypes.contains(type);
		}

		public abstract <T> T createEmpty(DynamicOps<T> ops);

		public abstract <T> @Nullable T decode(
			DynamicOps<T> ops,
			List<SnbtParsing.IntValue> values,
			ParsingState<?> state
		);

		protected @Nullable Number decode(SnbtParsing.IntValue value, ParsingState<?> state) {
			SnbtParsing.NumericType numericType = getType(value.suffix);
			if (numericType == null) {
				state.getErrors().add(state.getCursor(), SnbtParsing.INVALID_ARRAY_ELEMENT_TYPE_EXCEPTION);
				return null;
			}

			return (Number) value.decode(JavaOps.INSTANCE, numericType, state);
		}

		private SnbtParsing.@Nullable NumericType getType(SnbtParsing.NumberSuffix suffix) {
			SnbtParsing.NumericType numericType = suffix.type();
			if (numericType == null) {
				return elementType;
			}

			return !isTypeAllowed(numericType) ? null : numericType;
		}
	}

	/**
		* Правило парсинга шестнадцатеричных escape-последовательностей фиксированной длины.
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
				case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				     'A', 'B', 'C', 'D', 'E', 'F',
				     'a', 'b', 'c', 'd', 'e', 'f' -> true;
				default -> false;
			};
		}
	}

	/**
		* Представляет целочисленный литерал SNBT с его знаком, основанием, цифрами и суффиксом типа.
		*/
	record IntValue(SnbtParsing.Sign sign, SnbtParsing.Radix base, String digits, SnbtParsing.NumberSuffix suffix) {

		private SnbtParsing.Signedness getSignedness() {
			if (suffix.signed != null) {
				return suffix.signed;
			}

			return switch (base) {
				case BINARY, HEX -> SnbtParsing.Signedness.UNSIGNED;
				case DECIMAL -> SnbtParsing.Signedness.SIGNED;
			};
		}

		private String toSignedString(SnbtParsing.Sign effectiveSign) {
			boolean hasUnderscore = SnbtParsing.containsUnderscore(digits);
			if (effectiveSign != SnbtParsing.Sign.MINUS && !hasUnderscore) {
				return digits;
			}

			StringBuilder builder = new StringBuilder();
			effectiveSign.append(builder);
			SnbtParsing.append(builder, digits, hasUnderscore);
			return builder.toString();
		}

		public <T> @Nullable T decode(DynamicOps<T> ops, ParsingState<?> state) {
			return decode(ops, Objects.requireNonNullElse(suffix.type, SnbtParsing.NumericType.INT), state);
		}

		/**
		 * Декодирует целочисленный литерал в значение целевого типа.
		 * Учитывает знаковость (signed/unsigned) и основание системы счисления.
		 *
		 * @param ops   операции сериализации
		 * @param type  целевой числовой тип
		 * @param state состояние парсера для записи ошибок
		 * @param <T>   целевой тип данных
		 * @return декодированное значение или {@code null} при ошибке
		 */
		public <T> @Nullable T decode(DynamicOps<T> ops, SnbtParsing.NumericType type, ParsingState<?> state) {
			boolean isSigned = getSignedness() == SnbtParsing.Signedness.SIGNED;
			if (!isSigned && sign == SnbtParsing.Sign.MINUS) {
				state.getErrors().add(state.getCursor(), SnbtParsing.EXPECTED_NON_NEGATIVE_NUMBER_EXCEPTION);
				return null;
			}

			String numberStr = toSignedString(sign);
			int radix = switch (base) {
				case BINARY -> 2;
				case DECIMAL -> 10;
				case HEX -> 16;
			};

			try {
				if (isSigned) {
					return (T) (switch (type) {
						case BYTE -> (Object) ops.createByte(Byte.parseByte(numberStr, radix));
						case SHORT -> (Object) ops.createShort(Short.parseShort(numberStr, radix));
						case INT -> (Object) ops.createInt(Integer.parseInt(numberStr, radix));
						case LONG -> (Object) ops.createLong(Long.parseLong(numberStr, radix));
						default -> {
							state.getErrors().add(state.getCursor(), SnbtParsing.EXPECTED_INTEGER_TYPE_EXCEPTION);
							yield null;
						}
					});
				}
				else {
					return (T) (switch (type) {
						case BYTE -> (Object) ops.createByte(UnsignedBytes.parseUnsignedByte(numberStr, radix));
						case SHORT -> (Object) ops.createShort(SnbtParsing.parseUnsignedShort(numberStr, radix));
						case INT -> (Object) ops.createInt(Integer.parseUnsignedInt(numberStr, radix));
						case LONG -> (Object) ops.createLong(Long.parseUnsignedLong(numberStr, radix));
						default -> {
							state.getErrors().add(state.getCursor(), SnbtParsing.EXPECTED_INTEGER_TYPE_EXCEPTION);
							yield null;
						}
					});
				}
			}
			catch (NumberFormatException exception) {
				state.getErrors().add(state.getCursor(), SnbtParsing.toNumberParseFailure(exception));
				return null;
			}
		}
	}

	/**
		* Суффикс числового литерала: знаковость и тип.
		*/
	record NumberSuffix(SnbtParsing.@Nullable Signedness signed, SnbtParsing.@Nullable NumericType type) {

		public static final SnbtParsing.NumberSuffix DEFAULT = new SnbtParsing.NumberSuffix(null, null);
	}

	/**
	 * Числовой тип SNBT-литерала.
	 */
	enum NumericType {
		FLOAT,
		DOUBLE,
		BYTE,
		SHORT,
		INT,
		LONG;
	}

	/**
	 * Основание системы счисления числового литерала.
	 */
	enum Radix {
		BINARY,
		DECIMAL,
		HEX;
	}

	/**
	 * Знак числового литерала.
	 */
	enum Sign {
		PLUS,
		MINUS;

		public void append(StringBuilder builder) {
			if (this == MINUS) {
				builder.append('-');
			}
		}
	}

	/**
		* Значение с явно указанным знаком (используется для экспоненты числа с плавающей точкой).
		*/
	record SignedValue<T>(SnbtParsing.Sign sign, T value) {
	}

	/**
	 * Явная знаковость числового литерала.
	 */
	enum Signedness {
		SIGNED,
		UNSIGNED;
	}
}
