package net.minecraft.text;

import com.google.common.collect.Lists;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Изменяемый текстовый компонент Minecraft.
 * Содержит {@link TextContent} (тип содержимого), список дочерних компонентов
 * и {@link Style}. В отличие от {@link Text}, допускает мутацию стиля и добавление
 * дочерних элементов через fluent-методы.
 *
 * <p>Кеш {@link OrderedText} инвалидируется при смене активного языка.
 */
public final class MutableText implements Text {

	private final TextContent content;
	private final List<Text> siblings;
	private Style style;
	private OrderedText ordered = OrderedText.EMPTY;
	private @Nullable Language language;

	MutableText(TextContent content, List<Text> siblings, Style style) {
		this.content = content;
		this.siblings = siblings;
		this.style = style;
	}

	public static MutableText of(TextContent content) {
		return new MutableText(content, Lists.newArrayList(), Style.EMPTY);
	}

	@Override
	public TextContent getContent() {
		return content;
	}

	@Override
	public List<Text> getSiblings() {
		return siblings;
	}

	public MutableText setStyle(Style style) {
		this.style = style;
		return this;
	}

	@Override
	public Style getStyle() {
		return style;
	}

	public MutableText append(String text) {
		return text.isEmpty() ? this : append(Text.literal(text));
	}

	public MutableText append(Text text) {
		siblings.add(text);
		return this;
	}

	/**
	 * Применяет функцию-трансформер к текущему стилю и устанавливает результат.
	 *
	 * @param styleUpdater функция преобразования стиля
	 */
	public MutableText styled(UnaryOperator<Style> styleUpdater) {
		setStyle(styleUpdater.apply(getStyle()));
		return this;
	}

	/**
	 * Заполняет незаданные атрибуты стиля значениями из {@code styleOverride}.
	 * Эквивалентно {@code setStyle(styleOverride.withParent(currentStyle))}.
	 *
	 * @param styleOverride стиль-источник для незаданных атрибутов
	 */
	public MutableText fillStyle(Style styleOverride) {
		setStyle(styleOverride.withParent(getStyle()));
		return this;
	}

	public MutableText formatted(Formatting... formattings) {
		setStyle(getStyle().withFormatting(formattings));
		return this;
	}

	public MutableText formatted(Formatting formatting) {
		setStyle(getStyle().withFormatting(formatting));
		return this;
	}

	public MutableText withColor(int color) {
		setStyle(getStyle().withColor(color));
		return this;
	}

	public MutableText withoutShadow() {
		setStyle(getStyle().withoutShadow());
		return this;
	}

	/**
	 * Возвращает упорядоченное представление текста для рендеринга.
	 * Кеш инвалидируется при смене активного экземпляра {@link Language}.
	 */
	@Override
	public OrderedText asOrderedText() {
		Language currentLanguage = Language.getInstance();

		if (language != currentLanguage) {
			ordered = currentLanguage.reorder(this);
			language = currentLanguage;
		}

		return ordered;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof MutableText other
			&& content.equals(other.content)
			&& style.equals(other.style)
			&& siblings.equals(other.siblings);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = 31 * hash + content.hashCode();
		hash = 31 * hash + style.hashCode();
		return 31 * hash + siblings.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(content.toString());
		boolean hasStyle = !style.isEmpty();
		boolean hasSiblings = !siblings.isEmpty();

		if (hasStyle || hasSiblings) {
			builder.append('[');

			if (hasStyle) {
				builder.append("style=");
				builder.append(style);
			}

			if (hasStyle && hasSiblings) {
				builder.append(", ");
			}

			if (hasSiblings) {
				builder.append("siblings=");
				builder.append(siblings);
			}

			builder.append(']');
		}

		return builder.toString();
	}
}
