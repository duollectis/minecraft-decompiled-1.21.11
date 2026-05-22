package net.minecraft.text;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import java.util.List;

/**
 * Текст, упорядоченный для посимвольного обхода с учётом стиля.
 *
 * <p>Является функциональным интерфейсом: единственный метод {@link #accept(CharacterVisitor)}
 * передаёт каждый символ (кодовую точку Unicode) вместе с его стилем и индексом посетителю.
 * Обход прерывается, если посетитель вернул {@code false}.</p>
 *
 * <p>Фабричные методы {@code styledForwardsVisitedString} / {@code styledBackwardsVisitedString}
 * создают экземпляры для прямого и обратного обхода строк соответственно.
 * Методы {@code concat} объединяют несколько {@link OrderedText} в один.</p>
 */
@FunctionalInterface
public interface OrderedText {

	/** Пустой {@link OrderedText}, не передающий ни одного символа посетителю. */
	OrderedText EMPTY = visitor -> true;

	/**
	 * Передаёт каждый символ текста посетителю.
	 *
	 * @param visitor посетитель, принимающий индекс, стиль и кодовую точку
	 * @return {@code true} если обход завершён полностью, {@code false} если прерван посетителем
	 */
	boolean accept(CharacterVisitor visitor);

	/**
	 * Создаёт {@link OrderedText} для одиночного символа с заданным стилем.
	 *
	 * @param codePoint кодовая точка Unicode
	 * @param style стиль символа
	 */
	static OrderedText styled(int codePoint, Style style) {
		return visitor -> visitor.accept(0, style, codePoint);
	}

	/**
	 * Создаёт {@link OrderedText} для прямого обхода строки с заданным стилем.
	 *
	 * @param string исходная строка
	 * @param style стиль для всей строки
	 */
	static OrderedText styledForwardsVisitedString(String string, Style style) {
		return string.isEmpty() ? EMPTY : visitor -> TextVisitFactory.visitForwards(string, style, visitor);
	}

	/**
	 * Создаёт {@link OrderedText} для прямого обхода строки с маппингом кодовых точек.
	 *
	 * @param string исходная строка
	 * @param style стиль для всей строки
	 * @param codePointMapper функция преобразования кодовых точек (например, для смены регистра)
	 */
	static OrderedText styledForwardsVisitedString(String string, Style style, Int2IntFunction codePointMapper) {
		return string.isEmpty() ? EMPTY : visitor -> TextVisitFactory.visitForwards(
				string,
				style,
				map(visitor, codePointMapper)
		);
	}

	/**
	 * Создаёт {@link OrderedText} для обратного обхода строки с заданным стилем.
	 *
	 * @param string исходная строка
	 * @param style стиль для всей строки
	 */
	static OrderedText styledBackwardsVisitedString(String string, Style style) {
		return string.isEmpty() ? EMPTY : visitor -> TextVisitFactory.visitBackwards(string, style, visitor);
	}

	/**
	 * Создаёт {@link OrderedText} для обратного обхода строки с маппингом кодовых точек.
	 *
	 * @param string исходная строка
	 * @param style стиль для всей строки
	 * @param codePointMapper функция преобразования кодовых точек
	 */
	static OrderedText styledBackwardsVisitedString(String string, Style style, Int2IntFunction codePointMapper) {
		return string.isEmpty() ? EMPTY : visitor -> TextVisitFactory.visitBackwards(
				string,
				style,
				map(visitor, codePointMapper)
		);
	}

	/**
	 * Оборачивает посетителя, пропуская кодовые точки через маппер перед передачей.
	 *
	 * @param visitor оригинальный посетитель
	 * @param codePointMapper функция преобразования кодовых точек
	 */
	static CharacterVisitor map(CharacterVisitor visitor, Int2IntFunction codePointMapper) {
		return (charIndex, style, charPoint) -> visitor.accept(
				charIndex,
				style,
				(Integer) codePointMapper.apply(charPoint)
		);
	}

	static OrderedText empty() {
		return EMPTY;
	}

	static OrderedText of(OrderedText text) {
		return text;
	}

	static OrderedText concat(OrderedText first, OrderedText second) {
		return innerConcat(first, second);
	}

	static OrderedText concat(OrderedText... texts) {
		return innerConcat(ImmutableList.copyOf(texts));
	}

	/**
	 * Объединяет список {@link OrderedText} в один с оптимизацией для малых размеров.
	 *
	 * @param texts список текстов для объединения
	 */
	static OrderedText concat(List<OrderedText> texts) {
		return switch (texts.size()) {
			case 0 -> EMPTY;
			case 1 -> texts.get(0);
			case 2 -> innerConcat(texts.get(0), texts.get(1));
			default -> innerConcat(ImmutableList.copyOf(texts));
		};
	}

	static OrderedText innerConcat(OrderedText text1, OrderedText text2) {
		return visitor -> text1.accept(visitor) && text2.accept(visitor);
	}

	static OrderedText innerConcat(List<OrderedText> texts) {
		return visitor -> {
			for (OrderedText orderedText : texts) {
				if (!orderedText.accept(visitor)) {
					return false;
				}
			}

			return true;
		};
	}
}
