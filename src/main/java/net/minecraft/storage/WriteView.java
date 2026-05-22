package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.serialization.v1.view.FabricWriteView;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс для записи структурированных данных в NBT-подобное хранилище.
 * <p>
 * Предоставляет типобезопасный API для сохранения примитивных значений,
 * вложенных объектов и списков. Ошибки кодирования сообщаются через
 * {@link net.minecraft.util.ErrorReporter}, а не бросаются как исключения.
 */
public interface WriteView extends FabricWriteView {

	/**
	 * Кодирует значение через кодек и записывает его по ключу.
	 *
	 * @param key   ключ поля
	 * @param codec кодек для сериализации
	 * @param value значение для записи
	 * @param <T>   тип значения
	 */
	<T> void put(String key, Codec<T> codec, T value);

	/**
	 * Кодирует и записывает значение по ключу, если оно не {@code null}.
	 *
	 * @param key   ключ поля
	 * @param codec кодек для сериализации
	 * @param value значение для записи, может быть {@code null}
	 * @param <T>   тип значения
	 */
	<T> void putNullable(String key, Codec<T> codec, @Nullable T value);

	/**
	 * Кодирует значение через {@link MapCodec} и сливает поля в текущий объект.
	 *
	 * @param codec кодек для сериализации в плоскую карту полей
	 * @param value значение для записи
	 * @param <T>   тип значения
	 * @deprecated Предпочтительнее использовать {@link #put(String, Codec, Object)}
	 */
	@Deprecated
	<T> void put(MapCodec<T> codec, T value);

	void putBoolean(String key, boolean value);

	void putByte(String key, byte value);

	void putShort(String key, short value);

	void putInt(String key, int value);

	void putLong(String key, long value);

	void putFloat(String key, float value);

	void putDouble(String key, double value);

	void putString(String key, String value);

	void putIntArray(String key, int[] value);

	/**
	 * Создаёт и возвращает дочернее представление для записи вложенного объекта.
	 * Вложенный объект автоматически привязывается к текущему по указанному ключу.
	 *
	 * @param key ключ вложенного объекта
	 * @return дочерний {@link WriteView} для записи полей вложенного объекта
	 */
	WriteView get(String key);

	/**
	 * Создаёт и возвращает представление списка вложенных объектов.
	 * Список автоматически привязывается к текущему объекту по указанному ключу.
	 *
	 * @param key ключ списка
	 * @return {@link ListView} для добавления элементов
	 */
	ListView getList(String key);

	/**
	 * Создаёт и возвращает аппендер для типизированного списка.
	 * Список автоматически привязывается к текущему объекту по указанному ключу.
	 *
	 * @param key   ключ списка
	 * @param codec кодек для сериализации элементов
	 * @param <T>   тип элементов
	 * @return {@link ListAppender} для добавления элементов
	 */
	<T> ListAppender<T> getListAppender(String key, Codec<T> codec);

	void remove(String key);

	boolean isEmpty();

	/**
	 * Аппендер для добавления типизированных элементов в список.
	 * Каждый элемент кодируется через заданный кодек перед записью.
	 *
	 * @param <T> тип элементов списка
	 */
	interface ListAppender<T> {

		void add(T value);

		boolean isEmpty();
	}

	/**
	 * Представление списка вложенных объектов для последовательной записи.
	 * Каждый вызов {@link #add()} создаёт новый вложенный {@link WriteView}.
	 */
	interface ListView {

		/**
		 * Добавляет новый пустой объект в конец списка и возвращает его представление.
		 *
		 * @return {@link WriteView} для записи полей нового элемента
		 */
		WriteView add();

		void removeLast();

		boolean isEmpty();
	}
}
