package net.minecraft.component.type;

import net.minecraft.text.RawFilteredPair;

import java.util.List;

/**
	 * Интерфейс содержимого книги. Параметризован типом страниц {@code T}
	 * и конкретным типом компонента {@code C}, реализующего этот интерфейс.
	 */
public interface BookContent<T, C> {

	/**
		 * Возвращает список страниц книги, каждая из которых может иметь
		 * отфильтрованную (серверную) и нефильтрованную (клиентскую) версию.
		 */
	List<RawFilteredPair<T>> pages();

	/**
		 * Создаёт новый экземпляр компонента с заменённым списком страниц.
		 *
		 * @param pages новый список страниц
		 * @return новый компонент с указанными страницами
		 */
	C withPages(List<RawFilteredPair<T>> pages);
}
