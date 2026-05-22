package net.minecraft.text;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.datafixers.util.Either;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.object.TextObjectContents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.*;

/**
 * Базовый интерфейс текстового компонента Minecraft.
 * Текст состоит из {@link TextContent} (тип содержимого), {@link Style} и списка дочерних компонентов.
 * Реализует {@link StringVisitable} для обхода строкового содержимого и {@link Message} для Brigadier.
 *
 * <p>Для создания экземпляров используйте статические фабричные методы:
 * {@link #literal}, {@link #translatable}, {@link #empty} и другие.
 */
public interface Text extends Message, StringVisitable {

	Style getStyle();

	TextContent getContent();

	@Override
	default String getString() {
		return StringVisitable.super.getString();
	}

	/**
	 * Возвращает строковое представление текста, обрезанное до {@code length} символов.
	 * Обход прекращается досрочно, как только накоплено достаточно символов.
	 *
	 * @param length максимальное количество символов в результате
	 */
	default String asTruncatedString(int length) {
		StringBuilder builder = new StringBuilder();

		visit(string -> {
			int remaining = length - builder.length();

			if (remaining <= 0) {
				return TERMINATE_VISIT;
			}

			builder.append(string.length() <= remaining ? string : string.substring(0, remaining));
			return Optional.empty();
		});

		return builder.toString();
	}

	List<Text> getSiblings();

	/**
	 * Возвращает строку, если текст является простым литералом без стиля и дочерних элементов.
	 * Используется для оптимизации сериализации в {@link TextCodecs}.
	 */
	default @Nullable String getLiteralString() {
		return getContent() instanceof PlainTextContent plain
			&& getSiblings().isEmpty()
			&& getStyle().isEmpty()
			? plain.string()
			: null;
	}

	default MutableText copyContentOnly() {
		return MutableText.of(getContent());
	}

	default MutableText copy() {
		return new MutableText(getContent(), new ArrayList<>(getSiblings()), getStyle());
	}

	OrderedText asOrderedText();

	/**
	 * Обходит текст со стилем, объединяя стиль текущего компонента с переданным {@code style}.
	 * Обход прекращается, если посетитель вернул непустой {@link Optional}.
	 */
	@Override
	default <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
		Style mergedStyle = getStyle().withParent(style);
		Optional<T> result = getContent().visit(styledVisitor, mergedStyle);

		if (result.isPresent()) {
			return result;
		}

		for (Text sibling : getSiblings()) {
			Optional<T> siblingResult = sibling.visit(styledVisitor, mergedStyle);

			if (siblingResult.isPresent()) {
				return siblingResult;
			}
		}

		return Optional.empty();
	}

	@Override
	default <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		Optional<T> result = getContent().visit(visitor);

		if (result.isPresent()) {
			return result;
		}

		for (Text sibling : getSiblings()) {
			Optional<T> siblingResult = sibling.visit(visitor);

			if (siblingResult.isPresent()) {
				return siblingResult;
			}
		}

		return Optional.empty();
	}

	default List<Text> withoutStyle() {
		return getWithStyle(Style.EMPTY);
	}

	/**
	 * Разбивает текст на плоский список литеральных фрагментов с применённым стилем.
	 *
	 * @param style базовый стиль для обхода
	 */
	default List<Text> getWithStyle(Style style) {
		List<Text> list = Lists.newArrayList();

		visit(
			(styleOverride, fragment) -> {
				if (!fragment.isEmpty()) {
					list.add(literal(fragment).fillStyle(styleOverride));
				}

				return Optional.empty();
			}, style
		);

		return list;
	}

	/**
	 * Проверяет, содержит ли данный текст {@code text} как подпоследовательность
	 * при сравнении плоских фрагментов без стиля.
	 */
	default boolean contains(Text text) {
		if (equals(text)) {
			return true;
		}

		List<Text> thisFlat = withoutStyle();
		List<Text> otherFlat = text.getWithStyle(getStyle());
		return Collections.indexOfSubList(thisFlat, otherFlat) != -1;
	}

	static Text of(@Nullable String string) {
		return string != null ? literal(string) : ScreenTexts.EMPTY;
	}

	static MutableText literal(String string) {
		return MutableText.of(PlainTextContent.of(string));
	}

	static MutableText translatable(String key) {
		return MutableText.of(new TranslatableTextContent(key, null, TranslatableTextContent.EMPTY_ARGUMENTS));
	}

	static MutableText translatable(String key, Object... args) {
		return MutableText.of(new TranslatableTextContent(key, null, args));
	}

	/**
	 * Создаёт переводимый текст, предварительно преобразуя не-примитивные аргументы в строки.
	 * Это предотвращает попытку сериализации произвольных объектов как {@link Text}.
	 */
	static MutableText stringifiedTranslatable(String key, Object... args) {
		for (int index = 0; index < args.length; index++) {
			Object arg = args[index];

			if (!TranslatableTextContent.isPrimitive(arg) && !(arg instanceof Text)) {
				args[index] = String.valueOf(arg);
			}
		}

		return translatable(key, args);
	}

	static MutableText translatableWithFallback(String key, @Nullable String fallback) {
		return MutableText.of(new TranslatableTextContent(key, fallback, TranslatableTextContent.EMPTY_ARGUMENTS));
	}

	static MutableText translatableWithFallback(String key, @Nullable String fallback, Object... args) {
		return MutableText.of(new TranslatableTextContent(key, fallback, args));
	}

	static MutableText empty() {
		return MutableText.of(PlainTextContent.EMPTY);
	}

	static MutableText keybind(String string) {
		return MutableText.of(new KeybindTextContent(string));
	}

	static MutableText nbt(String rawPath, boolean interpret, Optional<Text> separator, NbtDataSource dataSource) {
		return MutableText.of(new NbtTextContent(rawPath, interpret, separator, dataSource));
	}

	static MutableText score(ParsedSelector selector, String objective) {
		return MutableText.of(new ScoreTextContent(Either.left(selector), objective));
	}

	static MutableText score(String name, String objective) {
		return MutableText.of(new ScoreTextContent(Either.right(name), objective));
	}

	static MutableText selector(ParsedSelector selector, Optional<Text> separator) {
		return MutableText.of(new SelectorTextContent(selector, separator));
	}

	static MutableText object(TextObjectContents object) {
		return MutableText.of(new ObjectTextContent(object));
	}

	static Text of(Date date) {
		return literal(date.toString());
	}

	static Text of(Message message) {
		return message instanceof Text text ? text : literal(message.getString());
	}

	static Text of(UUID uuid) {
		return literal(uuid.toString());
	}

	static Text of(Identifier id) {
		return literal(id.toString());
	}

	static Text of(ChunkPos pos) {
		return literal(pos.toString());
	}

	static Text of(URI uri) {
		return literal(uri.toString());
	}
}
