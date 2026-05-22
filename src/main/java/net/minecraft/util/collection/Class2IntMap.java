package net.minecraft.util.collection;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.Util;

/**
 * Карта, отображающая классы Java на целочисленные значения с поддержкой
 * иерархического поиска по суперклассам. Если точное совпадение не найдено,
 * поиск продолжается вверх по иерархии наследования до {@link Object}.
 */
public class Class2IntMap {

	public static final int MISSING = -1;

	private final Object2IntMap<Class<?>> backingMap = Util.make(
		new Object2IntOpenHashMap<>(),
		map -> map.defaultReturnValue(MISSING)
	);

	/**
	 * Возвращает значение для указанного класса или ближайшего суперкласса.
	 * Поиск идёт вверх по иерархии наследования, исключая {@link Object}.
	 *
	 * @param clazz класс для поиска
	 * @return найденное значение или {@link #MISSING} если ничего не найдено
	 */
	public int get(Class<?> clazz) {
		int direct = backingMap.getInt(clazz);
		if (direct != MISSING) {
			return direct;
		}

		Class<?> current = clazz;

		while ((current = current.getSuperclass()) != Object.class) {
			int inherited = backingMap.getInt(current);
			if (inherited != MISSING) {
				return inherited;
			}
		}

		return MISSING;
	}

	public int getNext(Class<?> clazz) {
		return get(clazz) + 1;
	}

	/**
	 * Регистрирует класс, присваивая ему значение на единицу больше текущего.
	 * Если класс ещё не зарегистрирован, присваивается 0.
	 *
	 * @param clazz класс для регистрации
	 * @return присвоенное значение
	 */
	public int put(Class<?> clazz) {
		int current = get(clazz);
		int next = current == MISSING ? 0 : current + 1;
		backingMap.put(clazz, next);
		return next;
	}
}
