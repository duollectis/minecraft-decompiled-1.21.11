package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Многострочный текст с поддержкой переноса строк, ограничения по ширине и количеству строк.
 * Кешируется по текущему экземпляру {@link Language} для корректной работы с RTL-языками.
 */
@Environment(EnvType.CLIENT)
public interface MultilineText {

	MultilineText EMPTY = new MultilineText() {
		@Override
		public int draw(Alignment alignment, int x, int y, int lineHeight, DrawnTextConsumer consumer) {
			return y;
		}

		@Override
		public int getLineCount() {
			return 0;
		}

		@Override
		public int getMaxWidth() {
			return 0;
		}
	};

	static MultilineText create(TextRenderer renderer, Text... texts) {
		return create(renderer, Integer.MAX_VALUE, Integer.MAX_VALUE, texts);
	}

	static MultilineText create(TextRenderer renderer, int maxWidth, Text... texts) {
		return create(renderer, maxWidth, Integer.MAX_VALUE, texts);
	}

	static MultilineText create(TextRenderer renderer, Text text, int maxWidth) {
		return create(renderer, maxWidth, Integer.MAX_VALUE, text);
	}

	/**
	 * Создаёт многострочный текст с ограничением по ширине и количеству строк.
	 * Если строк больше {@code maxLines}, последняя строка обрезается с добавлением многоточия.
	 *
	 * @param textRenderer рендерер для измерения ширины строк
	 * @param maxWidth максимальная ширина строки в пикселях
	 * @param maxLines максимальное количество строк
	 * @param texts исходные тексты для отображения
	 * @return многострочный текст, или {@link #EMPTY} если массив пуст
	 */
	static MultilineText create(TextRenderer textRenderer, int maxWidth, int maxLines, Text... texts) {
		return texts.length == 0
				? EMPTY
				: new MultilineText() {
					private @Nullable List<MultilineText.Line> lines;
					private @Nullable Language language;

					@Override
					public int draw(Alignment alignment, int x, int y, int lineHeight, DrawnTextConsumer consumer) {
						int currentY = y;

						for (MultilineText.Line line : getLines()) {
							int adjustedX = alignment.getAdjustedX(x, line.width);
							consumer.text(adjustedX, currentY, line.text);
							currentY += lineHeight;
						}

						return currentY;
					}

					private List<MultilineText.Line> getLines() {
						Language currentLanguage = Language.getInstance();
						if (lines != null && currentLanguage == language) {
							return lines;
						}

						language = currentLanguage;
						List<StringVisitable> wrapped = new ArrayList<>();

						for (Text text : texts) {
							wrapped.addAll(textRenderer.wrapLinesWithoutLanguage(text, maxWidth));
						}

						lines = new ArrayList<>();
						int visibleCount = Math.min(wrapped.size(), maxLines);
						List<StringVisitable> visible = wrapped.subList(0, visibleCount);

						for (int lineIndex = 0; lineIndex < visible.size(); lineIndex++) {
							StringVisitable visitable = visible.get(lineIndex);
							OrderedText ordered = Language.getInstance().reorder(visitable);
							boolean isLastTruncated = lineIndex == visible.size() - 1
									&& visibleCount == maxLines
									&& visibleCount != wrapped.size();

							if (isLastTruncated) {
								StringVisitable trimmed = textRenderer.trimToWidth(
										visitable,
										textRenderer.getWidth(visitable) - textRenderer.getWidth(ScreenTexts.ELLIPSIS)
								);
								StringVisitable withEllipsis = StringVisitable.concat(
										trimmed,
										ScreenTexts.ELLIPSIS.copy().fillStyle(texts[texts.length - 1].getStyle())
								);
								lines.add(new MultilineText.Line(
										Language.getInstance().reorder(withEllipsis),
										textRenderer.getWidth(withEllipsis)
								));
							} else {
								lines.add(new MultilineText.Line(ordered, textRenderer.getWidth(ordered)));
							}
						}

						return lines;
					}

					@Override
					public int getLineCount() {
						return getLines().size();
					}

					@Override
					public int getMaxWidth() {
						return Math.min(
								maxWidth,
								getLines().stream().mapToInt(MultilineText.Line::width).max().orElse(0)
						);
					}
				};
	}

	int draw(Alignment alignment, int x, int y, int lineHeight, DrawnTextConsumer consumer);

	int getLineCount();

	int getMaxWidth();

	@Environment(EnvType.CLIENT)
	record Line(OrderedText text, int width) {
	}
}
