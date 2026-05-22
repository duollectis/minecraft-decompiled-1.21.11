package net.minecraft.util.packrat;

import java.util.stream.Stream;

/**
 * Источник подсказок автодополнения для правила разбора.
 * Реализации возвращают поток строк-кандидатов, которые могут быть
 * допустимы в текущей позиции курсора — используется для подсветки
 * возможных значений в командной строке.
 */
public interface Suggestable<S> {

	Stream<String> possibleValues(ParsingState<S> state);

	static <S> Suggestable<S> empty() {
		return state -> Stream.empty();
	}
}
