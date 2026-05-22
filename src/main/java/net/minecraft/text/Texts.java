package net.minecraft.text;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import net.minecraft.entity.Entity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Утилитарный класс для работы с текстовыми компонентами:
 * объединение, форматирование, парсинг с учётом команд и проверка переводов.
 */
public class Texts {

	public static final String DEFAULT_SEPARATOR = ", ";
	public static final Text GRAY_DEFAULT_SEPARATOR_TEXT = Text.literal(", ").formatted(Formatting.GRAY);
	public static final Text DEFAULT_SEPARATOR_TEXT = Text.literal(", ");

	/**
	 * Устанавливает стиль для текста, только если он ещё не задан.
	 * Если текущий стиль пустой — заменяет его. Если совпадает — возвращает без изменений.
	 * Иначе объединяет через {@link Style#withParent}.
	 *
	 * @return изменённый текст (тот же объект или с обновлённым стилем)
	 */
	@CheckReturnValue
	public static MutableText setStyleIfAbsent(MutableText text, Style style) {
		if (style.isEmpty()) {
			return text;
		}

		Style currentStyle = text.getStyle();

		if (currentStyle.isEmpty()) {
			return text.setStyle(style);
		}

		return currentStyle.equals(style) ? text : text.setStyle(currentStyle.withParent(style));
	}

	/**
	 * Возвращает копию текста с применённым стилем.
	 * Если стиль пустой — возвращает оригинал без копирования.
	 * Если текущий стиль совпадает — возвращает оригинал.
	 * Иначе создаёт копию с объединённым стилем.
	 */
	@CheckReturnValue
	public static Text withStyle(Text text, Style style) {
		if (style.isEmpty()) {
			return text;
		}

		Style currentStyle = text.getStyle();

		if (currentStyle.isEmpty()) {
			return text.copy().setStyle(style);
		}

		return currentStyle.equals(style) ? text : text.copy().setStyle(currentStyle.withParent(style));
	}

	public static Optional<MutableText> parse(
		@Nullable ServerCommandSource source,
		Optional<Text> text,
		@Nullable Entity sender,
		int depth
	) throws CommandSyntaxException {
		return text.isPresent() ? Optional.of(parse(source, text.get(), sender, depth)) : Optional.empty();
	}

	/**
	 * Рекурсивно разбирает текстовый компонент, подставляя данные из источника команды.
	 * Глубина рекурсии ограничена 100 уровнями для защиты от бесконечных циклов.
	 *
	 * @param source источник команды (может быть null)
	 * @param text   текст для разбора
	 * @param sender сущность-отправитель (может быть null)
	 * @param depth  текущая глубина рекурсии
	 */
	public static MutableText parse(
		@Nullable ServerCommandSource source,
		Text text,
		@Nullable Entity sender,
		int depth
	) throws CommandSyntaxException {
		if (depth > 100) {
			return text.copy();
		}

		MutableText result = text.getContent().parse(source, sender, depth + 1);

		for (Text sibling : text.getSiblings()) {
			result.append(parse(source, sibling, sender, depth + 1));
		}

		return result.fillStyle(parseStyle(source, text.getStyle(), sender, depth));
	}

	private static Style parseStyle(
		@Nullable ServerCommandSource source,
		Style style,
		@Nullable Entity sender,
		int depth
	) throws CommandSyntaxException {
		if (style.getHoverEvent() instanceof HoverEvent.ShowText(Text hoverText)) {
			HoverEvent parsedHover = new HoverEvent.ShowText(parse(source, hoverText, sender, depth + 1));
			return style.withHoverEvent(parsedHover);
		}

		return style;
	}

	/**
	 * Объединяет коллекцию строк в текст, отсортировав их и выделив зелёным цветом.
	 */
	public static Text joinOrdered(Collection<String> strings) {
		return joinOrdered(strings, string -> Text.literal(string).formatted(Formatting.GREEN));
	}

	/**
	 * Объединяет коллекцию элементов в текст, предварительно отсортировав их.
	 * Если элемент один — возвращает его без разделителя.
	 *
	 * @param elements    коллекция сравниваемых элементов
	 * @param transformer функция преобразования элемента в текст
	 */
	public static <T extends Comparable<T>> Text joinOrdered(Collection<T> elements, Function<T, Text> transformer) {
		if (elements.isEmpty()) {
			return ScreenTexts.EMPTY;
		}

		if (elements.size() == 1) {
			return transformer.apply(elements.iterator().next());
		}

		List<T> sorted = Lists.newArrayList(elements);
		sorted.sort(Comparable::compareTo);
		return join(sorted, transformer);
	}

	public static <T> Text join(Collection<? extends T> elements, Function<T, Text> transformer) {
		return join(elements, GRAY_DEFAULT_SEPARATOR_TEXT, transformer);
	}

	public static <T> MutableText join(
		Collection<? extends T> elements,
		Optional<? extends Text> separator,
		Function<T, Text> transformer
	) {
		return join(elements, (Text) DataFixUtils.orElse(separator, GRAY_DEFAULT_SEPARATOR_TEXT), transformer);
	}

	public static Text join(Collection<? extends Text> texts, Text separator) {
		return join(texts, separator, Function.identity());
	}

	/**
	 * Объединяет коллекцию элементов в один текст, вставляя разделитель между ними.
	 * Если коллекция пустая — возвращает пустой текст.
	 * Если один элемент — возвращает его копию без разделителя.
	 */
	public static <T> MutableText join(
		Collection<? extends T> elements,
		Text separator,
		Function<T, Text> transformer
	) {
		if (elements.isEmpty()) {
			return Text.empty();
		}

		if (elements.size() == 1) {
			return transformer.apply((T) elements.iterator().next()).copy();
		}

		MutableText result = Text.empty();
		boolean first = true;

		for (T element : elements) {
			if (!first) {
				result.append(separator);
			}

			result.append(transformer.apply(element));
			first = false;
		}

		return result;
	}

	/**
	 * Оборачивает текст в квадратные скобки через ключ перевода {@code chat.square_brackets}.
	 */
	public static MutableText bracketed(Text text) {
		return Text.translatable("chat.square_brackets", text);
	}

	public static Text toText(Message message) {
		return message instanceof Text text ? text : Text.literal(message.getString());
	}

	/**
	 * Проверяет, имеет ли текст доступный перевод.
	 * Возвращает {@code true} для нетранслируемых текстов, текстов с fallback
	 * или если ключ перевода присутствует в активном языке.
	 */
	public static boolean hasTranslation(@Nullable Text text) {
		if (text == null || !(text.getContent() instanceof TranslatableTextContent translatable)) {
			return true;
		}

		String key = translatable.getKey();
		String fallback = translatable.getFallback();
		return fallback != null || Language.getInstance().hasTranslation(key);
	}

	/**
	 * Создаёт кликабельный текст в квадратных скобках, который копирует строку в буфер обмена.
	 * При наведении показывает подсказку {@code chat.copy.click}.
	 */
	public static MutableText bracketedCopyable(String string) {
		return bracketed(
			Text.literal(string)
				.styled(style -> style
					.withColor(Formatting.GREEN)
					.withClickEvent(new ClickEvent.CopyToClipboard(string))
					.withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.copy.click")))
					.withInsertion(string)
				)
		);
	}
}
