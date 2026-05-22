package net.minecraft.text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Процессор переупорядочивания текста для поддержки двунаправленного письма (BiDi).
 *
 * <p>Хранит строку символов и соответствующий список стилей (по одному на каждый char).
 * Метод {@link #process(int, int, boolean)} разбивает диапазон на сегменты с одинаковым стилем
 * и создаёт {@link OrderedText} для каждого сегмента — прямой или обратный в зависимости от флага.
 * Фабричный метод {@link #create(StringVisitable, Int2IntFunction, UnaryOperator)} строит процессор
 * из произвольного {@link StringVisitable}, применяя шейпер строки (например, для арабского текста).</p>
 */
public class TextReorderingProcessor {

	private final String string;
	private final List<Style> styles;
	private final Int2IntFunction reverser;

	private TextReorderingProcessor(String string, List<Style> styles, Int2IntFunction reverser) {
		this.string = string;
		this.styles = ImmutableList.copyOf(styles);
		this.reverser = reverser;
	}

	public String getString() {
		return string;
	}

	/**
	 * Разбивает диапазон символов на сегменты с одинаковым стилем и создаёт список {@link OrderedText}.
	 *
	 * <p>При {@code reverse = true} каждый сегмент обходится в обратном порядке,
	 * а итоговый список сегментов также переворачивается — для корректного отображения RTL-текста.</p>
	 *
	 * @param start начальный индекс в строке (включительно)
	 * @param length количество символов для обработки
	 * @param reverse {@code true} для обратного (RTL) порядка
	 * @return список {@link OrderedText} по одному на каждый стилевой сегмент
	 */
	public List<OrderedText> process(int start, int length, boolean reverse) {
		if (length == 0) {
			return ImmutableList.of();
		}

		List<OrderedText> result = Lists.newArrayList();
		Style currentStyle = styles.get(start);
		int segmentStart = start;

		for (int offset = 1; offset < length; offset++) {
			int pos = start + offset;
			Style styleAtPos = styles.get(pos);

			if (!styleAtPos.equals(currentStyle)) {
				String segment = string.substring(segmentStart, pos);
				result.add(
						reverse
								? OrderedText.styledBackwardsVisitedString(segment, currentStyle, reverser)
								: OrderedText.styledForwardsVisitedString(segment, currentStyle)
				);
				currentStyle = styleAtPos;
				segmentStart = pos;
			}
		}

		if (segmentStart < start + length) {
			String lastSegment = string.substring(segmentStart, start + length);
			result.add(
					reverse
							? OrderedText.styledBackwardsVisitedString(lastSegment, currentStyle, reverser)
							: OrderedText.styledForwardsVisitedString(lastSegment, currentStyle)
			);
		}

		return reverse ? Lists.reverse(result) : result;
	}

	/**
	 * Создаёт процессор с тождественным реверсером и без шейпинга строки.
	 *
	 * @param visitable источник текста
	 */
	public static TextReorderingProcessor create(StringVisitable visitable) {
		return create(visitable, codePoint -> codePoint, string -> string);
	}

	/**
	 * Создаёт процессор из {@link StringVisitable} с заданным реверсером и шейпером строки.
	 *
	 * <p>Обходит весь текст, собирая символы в {@link StringBuilder} и стили в список.
	 * Шейпер применяется к итоговой строке (например, для арабского/иврита через HarfBuzz).</p>
	 *
	 * @param visitable источник текста
	 * @param reverser функция маппинга кодовых точек при обратном обходе
	 * @param shaper функция преобразования итоговой строки (шейпинг)
	 */
	public static TextReorderingProcessor create(
			StringVisitable visitable,
			Int2IntFunction reverser,
			UnaryOperator<String> shaper
	) {
		StringBuilder builder = new StringBuilder();
		List<Style> styleList = Lists.newArrayList();

		visitable.visit(
				(style, text) -> {
					TextVisitFactory.visitFormatted(
							text, style, (charIndex, visitedStyle, codePoint) -> {
								builder.appendCodePoint(codePoint);
								int charCount = Character.charCount(codePoint);

								for (int charUnit = 0; charUnit < charCount; charUnit++) {
									styleList.add(visitedStyle);
								}

								return true;
							}
					);
					return Optional.empty();
				},
				Style.EMPTY
		);

		return new TextReorderingProcessor(shaper.apply(builder.toString()), styleList, reverser);
	}
}
