package net.minecraft.text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Language;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Содержимое переводимого текстового компонента.
 * Хранит ключ перевода, опциональный fallback и массив аргументов.
 * Переводы кешируются и инвалидируются при смене активного языка.
 *
 * <p>Поддерживает формат {@code %s} и позиционные аргументы {@code %N$s}.
 * Двойной знак процента {@code %%} заменяется на литеральный {@code %}.
 */
public class TranslatableTextContent implements TextContent {

	public static final Object[] EMPTY_ARGUMENTS = new Object[0];

	/** Паттерн для разбора аргументов формата: {@code %s}, {@code %1$s}, {@code %%}. */
	private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

	private static final StringVisitable LITERAL_PERCENT_SIGN = StringVisitable.plain("%");
	private static final StringVisitable NULL_ARGUMENT = StringVisitable.plain("null");

	private static final Codec<Object> OBJECT_ARGUMENT_CODEC =
		Codec.withAlternative(
			Codec.withAlternative(
				Codec.withAlternative(
					Codec.STRING.xmap(s -> (Object) s, Object::toString),
					Codec.BOOL.xmap(b -> (Object) b, o -> (Boolean) o)
				),
				Codec.withAlternative(
					Codec.INT.xmap(i -> (Object) i, o -> (Integer) o),
					Codec.FLOAT.xmap(f -> (Object) f, o -> (Float) o)
				)
			),
			Codec.DOUBLE.xmap(d -> (Object) d, o -> (Double) o)
		).validate(TranslatableTextContent::validateArgument);

	private static final Codec<Object> ARGUMENT_CODEC = Codec.either(OBJECT_ARGUMENT_CODEC, TextCodecs.CODEC)
		.xmap(
			either -> either.map(
				object -> object,
				text -> Objects.requireNonNullElse(text.getLiteralString(), text)
			),
			argument -> argument instanceof Text text
				? Either.right(text)
				: Either.left(argument)
		);

	public static final MapCodec<TranslatableTextContent> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.STRING.fieldOf("translate").forGetter(content -> content.key),
			Codec.STRING.lenientOptionalFieldOf("fallback")
				.forGetter(content -> Optional.ofNullable(content.fallback)),
			ARGUMENT_CODEC.listOf().optionalFieldOf("with")
				.forGetter(content -> toOptionalList(content.args))
		).apply(instance, TranslatableTextContent::of)
	);

	private final String key;
	private final @Nullable String fallback;
	private final Object[] args;
	private @Nullable Language languageCache;
	private List<StringVisitable> translations = ImmutableList.of();

	private static DataResult<Object> validateArgument(@Nullable Object object) {
		return isPrimitive(object)
			? DataResult.success(object)
			: DataResult.error(() -> "This value needs to be parsed as component");
	}

	public static boolean isPrimitive(@Nullable Object argument) {
		return argument instanceof Number || argument instanceof Boolean || argument instanceof String;
	}

	private static Optional<List<Object>> toOptionalList(Object[] args) {
		return args.length == 0 ? Optional.empty() : Optional.of(Arrays.asList(args));
	}

	private static Object[] toArray(Optional<List<Object>> args) {
		return args.<Object[]>map(list -> list.isEmpty() ? EMPTY_ARGUMENTS : list.toArray()).orElse(EMPTY_ARGUMENTS);
	}

	private static TranslatableTextContent of(String key, Optional<String> fallback, Optional<List<Object>> args) {
		return new TranslatableTextContent(key, fallback.orElse(null), toArray(args));
	}

	public TranslatableTextContent(String key, @Nullable String fallback, Object[] args) {
		this.key = key;
		this.fallback = fallback;
		this.args = args;
	}

	@Override
	public MapCodec<TranslatableTextContent> getCodec() {
		return CODEC;
	}

	/**
	 * Обновляет кеш переводов, если активный язык изменился.
	 * Разбирает строку перевода на части (литералы и аргументы) через {@link #forEachPart}.
	 */
	private void updateTranslations() {
		Language language = Language.getInstance();

		if (language == languageCache) {
			return;
		}

		languageCache = language;
		String translation = fallback != null ? language.get(key, fallback) : language.get(key);

		try {
			Builder<StringVisitable> builder = ImmutableList.builder();
			forEachPart(translation, builder::add);
			translations = builder.build();
		} catch (TranslationException e) {
			translations = ImmutableList.of(StringVisitable.plain(translation));
		}
	}

	/**
	 * Разбирает строку перевода на части: литеральные фрагменты и аргументы-подстановки.
	 * Поддерживает {@code %s} (последовательный аргумент), {@code %N$s} (позиционный) и {@code %%} (литеральный %).
	 *
	 * @param translation   строка перевода с форматными спецификаторами
	 * @param partsConsumer получатель разобранных частей
	 * @throws TranslationException если формат строки некорректен
	 */
	private void forEachPart(String translation, Consumer<StringVisitable> partsConsumer) {
		Matcher matcher = ARG_FORMAT.matcher(translation);

		try {
			int sequentialArgIndex = 0;
			int searchStart = 0;

			while (matcher.find(searchStart)) {
				int matchStart = matcher.start();
				int matchEnd = matcher.end();

				if (matchStart > searchStart) {
					String literal = translation.substring(searchStart, matchStart);

					if (literal.indexOf('%') != -1) {
						throw new IllegalArgumentException();
					}

					partsConsumer.accept(StringVisitable.plain(literal));
				}

				String formatType = matcher.group(2);
				String fullMatch = translation.substring(matchStart, matchEnd);

				if ("%".equals(formatType) && "%%".equals(fullMatch)) {
					partsConsumer.accept(LITERAL_PERCENT_SIGN);
				} else {
					if (!"s".equals(formatType)) {
						throw new TranslationException(this, "Unsupported format: '" + fullMatch + "'");
					}

					String positionGroup = matcher.group(1);
					int argIndex = positionGroup != null
						? Integer.parseInt(positionGroup) - 1
						: sequentialArgIndex++;

					partsConsumer.accept(getArg(argIndex));
				}

				searchStart = matchEnd;
			}

			if (searchStart < translation.length()) {
				String tail = translation.substring(searchStart);

				if (tail.indexOf('%') != -1) {
					throw new IllegalArgumentException();
				}

				partsConsumer.accept(StringVisitable.plain(tail));
			}
		} catch (IllegalArgumentException e) {
			throw new TranslationException(this, e);
		}
	}

	/**
	 * Возвращает аргумент по индексу как {@link StringVisitable}.
	 * Если аргумент является {@link Text} — возвращает его напрямую.
	 * {@code null}-аргументы заменяются на строку {@code "null"}.
	 *
	 * @param index индекс аргумента (0-based)
	 * @throws TranslationException если индекс выходит за пределы массива аргументов
	 */
	public final StringVisitable getArg(int index) {
		if (index < 0 || index >= args.length) {
			throw new TranslationException(this, index);
		}

		Object arg = args[index];

		if (arg instanceof Text text) {
			return text;
		}

		return arg == null ? NULL_ARGUMENT : StringVisitable.plain(arg.toString());
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		updateTranslations();

		for (StringVisitable part : translations) {
			Optional<T> result = part.visit(visitor, style);

			if (result.isPresent()) {
				return result;
			}
		}

		return Optional.empty();
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		updateTranslations();

		for (StringVisitable part : translations) {
			Optional<T> result = part.visit(visitor);

			if (result.isPresent()) {
				return result;
			}
		}

		return Optional.empty();
	}

	@Override
	public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth)
	throws CommandSyntaxException {
		Object[] parsedArgs = new Object[args.length];

		for (int index = 0; index < parsedArgs.length; index++) {
			Object arg = args[index];
			parsedArgs[index] = arg instanceof Text text ? Texts.parse(source, text, sender, depth) : arg;
		}

		return MutableText.of(new TranslatableTextContent(key, fallback, parsedArgs));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof TranslatableTextContent other
			&& Objects.equals(key, other.key)
			&& Objects.equals(fallback, other.fallback)
			&& Arrays.equals(args, other.args);
	}

	@Override
	public int hashCode() {
		int hash = Objects.hashCode(key);
		hash = 31 * hash + Objects.hashCode(fallback);
		return 31 * hash + Arrays.hashCode(args);
	}

	@Override
	public String toString() {
		return "translation{key='"
			+ key
			+ "'"
			+ (fallback != null ? ", fallback='" + fallback + "'" : "")
			+ ", args="
			+ Arrays.toString(args)
			+ "}";
	}

	public String getKey() {
		return key;
	}

	public @Nullable String getFallback() {
		return fallback;
	}

	public Object[] getArgs() {
		return args;
	}
}
