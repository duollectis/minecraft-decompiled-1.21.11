package net.minecraft.text;

/**
 * Функциональный интерфейс для посимвольного обхода текста с учётом стиля.
 * Используется в {@link TextVisitFactory} для итерации по кодовым точкам Unicode.
 */
@FunctionalInterface
public interface CharacterVisitor {

	/**
	 * Вызывается для каждого символа (кодовой точки) в тексте.
	 *
	 * @param index     позиция символа в исходной строке (в единицах {@code char})
	 * @param style     стиль, применённый к данному символу
	 * @param codePoint кодовая точка Unicode символа
	 * @return {@code true} — продолжить обход; {@code false} — прервать обход досрочно
	 */
	boolean accept(int index, Style style, int codePoint);
}
