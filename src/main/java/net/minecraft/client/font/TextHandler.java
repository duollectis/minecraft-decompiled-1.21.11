package net.minecraft.client.font;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.TextCollector;
import net.minecraft.text.*;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Обработчик текста: измерение ширины, обрезка по ширине и перенос строк.
 * Делегирует измерение ширины символов через {@link WidthRetriever}.
 */
@Environment(EnvType.CLIENT)
public class TextHandler {

	final TextHandler.WidthRetriever widthRetriever;

	public TextHandler(TextHandler.WidthRetriever widthRetriever) {
		this.widthRetriever = widthRetriever;
	}

	public float getWidth(@Nullable String text) {
		if (text == null) {
			return 0.0F;
		}

		MutableFloat width = new MutableFloat();
		TextVisitFactory.visitFormatted(
				text, Style.EMPTY, (unused, style, codePoint) -> {
					width.add(widthRetriever.getWidth(codePoint, style));
					return true;
				}
		);
		return width.floatValue();
	}

	public float getWidth(StringVisitable text) {
		MutableFloat width = new MutableFloat();
		TextVisitFactory.visitFormatted(
				text, Style.EMPTY, (unused, style, codePoint) -> {
					width.add(widthRetriever.getWidth(codePoint, style));
					return true;
				}
		);
		return width.floatValue();
	}

	public float getWidth(OrderedText text) {
		MutableFloat width = new MutableFloat();
		text.accept((index, style, codePoint) -> {
			width.add(widthRetriever.getWidth(codePoint, style));
			return true;
		});
		return width.floatValue();
	}

	public int getTrimmedLength(String text, int maxWidth, Style style) {
		TextHandler.WidthLimitingVisitor visitor = new TextHandler.WidthLimitingVisitor(maxWidth);
		TextVisitFactory.visitForwards(text, style, visitor);
		return visitor.getLength();
	}

	public String trimToWidth(String text, int maxWidth, Style style) {
		return text.substring(0, getTrimmedLength(text, maxWidth, style));
	}

	public String trimToWidthBackwards(String text, int maxWidth, Style style) {
		MutableFloat totalWidth = new MutableFloat();
		MutableInt startIndex = new MutableInt(text.length());
		TextVisitFactory.visitBackwards(
				text, style, (index, visitStyle, codePoint) -> {
					float accumulated = totalWidth.addAndGet(widthRetriever.getWidth(codePoint, visitStyle));
					if (accumulated > maxWidth) {
						return false;
					}

					startIndex.setValue(index);
					return true;
				}
		);
		return text.substring(startIndex.intValue());
	}

	public StringVisitable trimToWidth(StringVisitable text, int width, Style style) {
		final TextHandler.WidthLimitingVisitor widthLimiter = new TextHandler.WidthLimitingVisitor(width);
		return text.visit(
				new StringVisitable.StyledVisitor<StringVisitable>() {
					private final TextCollector collector = new TextCollector();

					@Override
					public Optional<StringVisitable> accept(Style visitStyle, String string) {
						widthLimiter.resetLength();
						if (!TextVisitFactory.visitFormatted(string, visitStyle, widthLimiter)) {
							String trimmed = string.substring(0, widthLimiter.getLength());
							if (!trimmed.isEmpty()) {
								collector.add(StringVisitable.styled(trimmed, visitStyle));
							}

							return Optional.of(collector.getCombined());
						}

						if (!string.isEmpty()) {
							collector.add(StringVisitable.styled(string, visitStyle));
						}

						return Optional.empty();
					}
				}, style
		).orElse(text);
	}

	public int getEndingIndex(String text, int maxWidth, Style style) {
		TextHandler.LineBreakingVisitor visitor = new TextHandler.LineBreakingVisitor(maxWidth);
		TextVisitFactory.visitFormatted(text, style, visitor);
		return visitor.getEndingIndex();
	}

	/**
	 * Перемещает курсор на заданное количество слов вперёд или назад.
	 * При {@code consumeSpaceOrBreak = true} пропускает пробелы и переносы строк на границах слов.
	 *
	 * @param text исходная строка
	 * @param offset количество слов (отрицательное — назад)
	 * @param cursor начальная позиция курсора
	 * @param consumeSpaceOrBreak поглощать ли разделители на границах слов
	 * @return новая позиция курсора
	 */
	public static int moveCursorByWords(String text, int offset, int cursor, boolean consumeSpaceOrBreak) {
		int position = cursor;
		boolean backwards = offset < 0;
		int steps = Math.abs(offset);

		for (int step = 0; step < steps; step++) {
			if (backwards) {
				while (consumeSpaceOrBreak && position > 0
						&& (text.charAt(position - 1) == ' ' || text.charAt(position - 1) == '\n')
				) {
					position--;
				}

				while (position > 0
						&& text.charAt(position - 1) != ' '
						&& text.charAt(position - 1) != '\n'
				) {
					position--;
				}
			} else {
				int length = text.length();
				int spaceIndex = text.indexOf(32, position);
				int newlineIndex = text.indexOf(10, position);

				if (spaceIndex == -1 && newlineIndex == -1) {
					position = -1;
				} else if (spaceIndex != -1 && newlineIndex != -1) {
					position = Math.min(spaceIndex, newlineIndex);
				} else if (spaceIndex != -1) {
					position = spaceIndex;
				} else {
					position = newlineIndex;
				}

				if (position == -1) {
					position = length;
				} else {
					while (consumeSpaceOrBreak && position < length
							&& (text.charAt(position) == ' ' || text.charAt(position) == '\n')
					) {
						position++;
					}
				}
			}
		}

		return position;
	}

	public void wrapLines(
			String text,
			int maxWidth,
			Style style,
			boolean retainTrailingWordSplit,
			TextHandler.LineWrappingConsumer consumer
	) {
		int start = 0;
		int end = text.length();
		Style currentStyle = style;

		while (start < end) {
			TextHandler.LineBreakingVisitor visitor = new TextHandler.LineBreakingVisitor(maxWidth);
			boolean fits = TextVisitFactory.visitFormatted(text, start, currentStyle, style, visitor);
			if (fits) {
				consumer.accept(currentStyle, start, end);
				break;
			}

			int breakIndex = visitor.getEndingIndex();
			char breakChar = text.charAt(breakIndex);
			int nextStart = breakChar != '\n' && breakChar != ' ' ? breakIndex : breakIndex + 1;
			consumer.accept(currentStyle, start, retainTrailingWordSplit ? nextStart : breakIndex);
			start = nextStart;
			currentStyle = visitor.getEndingStyle();
		}
	}

	public List<StringVisitable> wrapLines(String text, int maxWidth, Style style) {
		List<StringVisitable> lines = Lists.newArrayList();
		wrapLines(
				text,
				maxWidth,
				style,
				false,
				(lineStyle, start, end) -> lines.add(StringVisitable.styled(text.substring(start, end), lineStyle))
		);
		return lines;
	}

	public List<StringVisitable> wrapLines(StringVisitable text, int maxWidth, Style style) {
		List<StringVisitable> lines = Lists.newArrayList();
		wrapLines(text, maxWidth, style, (line, lastLineWrapped) -> lines.add(line));
		return lines;
	}

	/**
	 * Переносит строки {@link StringVisitable} по ширине, уведомляя потребителя о каждой строке.
	 * Второй аргумент потребителя — {@code true} если строка является продолжением предыдущей (не начинается с новой строки).
	 */
	public void wrapLines(
			StringVisitable text,
			int maxWidth,
			Style style,
			BiConsumer<StringVisitable, Boolean> lineConsumer
	) {
		List<TextHandler.StyledString> parts = Lists.newArrayList();
		text.visit(
				(visitStyle, segment) -> {
					if (!segment.isEmpty()) {
						parts.add(new TextHandler.StyledString(segment, visitStyle));
					}

					return Optional.empty();
				}, style
		);
		TextHandler.LineWrappingCollector collector = new TextHandler.LineWrappingCollector(parts);
		boolean hasMore = true;
		boolean lastWasNewline = false;
		boolean isContinuation = false;

		while (hasMore) {
			hasMore = false;
			TextHandler.LineBreakingVisitor visitor = new TextHandler.LineBreakingVisitor(maxWidth);

			for (TextHandler.StyledString styledString : collector.parts) {
				boolean fits = TextVisitFactory.visitFormatted(
						styledString.literal,
						0,
						styledString.style,
						style,
						visitor
				);
				if (fits) {
					visitor.offset(styledString.literal.length());
					continue;
				}

				int breakIndex = visitor.getEndingIndex();
				Style breakStyle = visitor.getEndingStyle();
				char breakChar = collector.charAt(breakIndex);
				boolean isNewline = breakChar == '\n';
				boolean skipChar = isNewline || breakChar == ' ';
				lastWasNewline = isNewline;
				StringVisitable line = collector.collectLine(breakIndex, skipChar ? 1 : 0, breakStyle);
				lineConsumer.accept(line, isContinuation);
				isContinuation = !isNewline;
				hasMore = true;
				break;
			}
		}

		StringVisitable remainder = collector.collectRemainders();
		if (remainder != null) {
			lineConsumer.accept(remainder, isContinuation);
		} else if (lastWasNewline) {
			lineConsumer.accept(StringVisitable.EMPTY, false);
		}
	}

	/**
	 * Посетитель символов, определяющий точку переноса строки по максимальной ширине.
	 * Предпочитает разрыв по пробелу, если он встречался раньше.
	 */
	@Environment(EnvType.CLIENT)
	class LineBreakingVisitor implements CharacterVisitor {

		private final float maxWidth;
		private int endIndex = -1;
		private Style endStyle = Style.EMPTY;
		private boolean nonEmpty;
		private float totalWidth;
		private int lastSpaceBreak = -1;
		private Style lastSpaceStyle = Style.EMPTY;
		private int count;
		private int startOffset;

		public LineBreakingVisitor(final float maxWidth) {
			this.maxWidth = Math.max(maxWidth, 1.0F);
		}

		@Override
		public boolean accept(int index, Style style, int codePoint) {
			int absoluteIndex = index + startOffset;
			switch (codePoint) {
				case 10:
					return breakLine(absoluteIndex, style);
				case 32:
					lastSpaceBreak = absoluteIndex;
					lastSpaceStyle = style;
				default:
					float charWidth = TextHandler.this.widthRetriever.getWidth(codePoint, style);
					totalWidth += charWidth;
					if (nonEmpty && totalWidth > maxWidth) {
						return lastSpaceBreak != -1
								? breakLine(lastSpaceBreak, lastSpaceStyle)
								: breakLine(absoluteIndex, style);
					}

					nonEmpty |= charWidth != 0.0F;
					count = absoluteIndex + Character.charCount(codePoint);
					return true;
			}
		}

		private boolean breakLine(int finishIndex, Style finishStyle) {
			endIndex = finishIndex;
			endStyle = finishStyle;
			return false;
		}

		private boolean hasLineBreak() {
			return endIndex != -1;
		}

		public int getEndingIndex() {
			return hasLineBreak() ? endIndex : count;
		}

		public Style getEndingStyle() {
			return endStyle;
		}

		public void offset(int extraOffset) {
			startOffset += extraOffset;
		}
	}

	@Environment(EnvType.CLIENT)
	static class LineWrappingCollector {

		final List<TextHandler.StyledString> parts;
		private String joined;

		public LineWrappingCollector(List<TextHandler.StyledString> parts) {
			this.parts = parts;
			joined = parts.stream().map(part -> part.literal).collect(Collectors.joining());
		}

		public char charAt(int index) {
			return joined.charAt(index);
		}

		/**
		 * Извлекает строку заданной длины из начала буфера, пропуская {@code skippedLength} символов-разделителей.
		 * Обновляет внутренний список частей и объединённую строку.
		 *
		 * @param lineLength длина извлекаемой строки
		 * @param skippedLength количество символов-разделителей для пропуска после строки
		 * @param style стиль для оставшейся части текущего сегмента
		 * @return собранная строка как {@link StringVisitable}
		 */
		public StringVisitable collectLine(int lineLength, int skippedLength, Style style) {
			TextCollector textCollector = new TextCollector();
			ListIterator<TextHandler.StyledString> iterator = parts.listIterator();
			int remaining = lineLength;
			boolean splitFound = false;

			while (iterator.hasNext()) {
				TextHandler.StyledString styledString = iterator.next();
				String segment = styledString.literal;
				int segmentLength = segment.length();

				if (!splitFound) {
					if (remaining > segmentLength) {
						textCollector.add(styledString);
						iterator.remove();
						remaining -= segmentLength;
					} else {
						String head = segment.substring(0, remaining);
						if (!head.isEmpty()) {
							textCollector.add(StringVisitable.styled(head, styledString.style));
						}

						remaining += skippedLength;
						splitFound = true;
					}
				}

				if (splitFound) {
					if (remaining <= segmentLength) {
						String tail = segment.substring(remaining);
						if (tail.isEmpty()) {
							iterator.remove();
						} else {
							iterator.set(new TextHandler.StyledString(tail, style));
						}

						break;
					}

					iterator.remove();
					remaining -= segmentLength;
				}
			}

			joined = joined.substring(lineLength + skippedLength);
			return textCollector.getCombined();
		}

		public @Nullable StringVisitable collectRemainders() {
			TextCollector textCollector = new TextCollector();
			parts.forEach(textCollector::add);
			parts.clear();
			return textCollector.getRawCombined();
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface LineWrappingConsumer {

		void accept(Style style, int start, int end);
	}

	@Environment(EnvType.CLIENT)
	static class StyledString implements StringVisitable {

		final String literal;
		final Style style;

		public StyledString(String literal, Style style) {
			this.literal = literal;
			this.style = style;
		}

		@Override
		public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
			return visitor.accept(literal);
		}

		@Override
		public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
			return styledVisitor.accept(this.style.withParent(style), literal);
		}
	}

	@Environment(EnvType.CLIENT)
	class WidthLimitingVisitor implements CharacterVisitor {

		private float widthLeft;
		private int length;

		public WidthLimitingVisitor(final float maxWidth) {
			widthLeft = maxWidth;
		}

		@Override
		public boolean accept(int index, Style style, int codePoint) {
			widthLeft -= TextHandler.this.widthRetriever.getWidth(codePoint, style);
			if (widthLeft >= 0.0F) {
				length = index + Character.charCount(codePoint);
				return true;
			}

			return false;
		}

		public int getLength() {
			return length;
		}

		public void resetLength() {
			length = 0;
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface WidthRetriever {

		float getWidth(int codePoint, Style style);
	}
}
