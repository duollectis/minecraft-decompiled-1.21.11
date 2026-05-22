package net.minecraft.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.serialization.v1.view.FabricReadView;
import net.minecraft.registry.RegistryWrapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Интерфейс для чтения структурированных данных из NBT-подобного хранилища.
 * <p>
 * Предоставляет типобезопасный API для извлечения примитивных значений,
 * вложенных объектов и списков. Реализации обязаны сообщать об ошибках
 * декодирования через {@link net.minecraft.util.ErrorReporter}, а не бросать исключения.
 */
public interface ReadView extends FabricReadView {

	/**
	 * Читает значение по ключу, декодируя его через указанный кодек.
	 *
	 * @param key   ключ поля в хранилище
	 * @param codec кодек для десериализации значения
	 * @param <T>   тип результата
	 * @return {@link Optional} с декодированным значением, либо пустой при отсутствии ключа или ошибке
	 */
	<T> Optional<T> read(String key, Codec<T> codec);

	/**
	 * Читает значение из текущего объекта целиком через {@link MapCodec}.
	 *
	 * @param mapCodec кодек для десериализации из плоской карты полей
	 * @param <T>      тип результата
	 * @return {@link Optional} с декодированным значением
	 * @deprecated Предпочтительнее использовать {@link #read(String, Codec)}
	 */
	@Deprecated
	<T> Optional<T> read(MapCodec<T> mapCodec);

	/**
	 * Возвращает дочернее представление для вложенного объекта, если ключ существует.
	 *
	 * @param key ключ вложенного объекта
	 * @return {@link Optional} с дочерним {@link ReadView}, либо пустой
	 */
	Optional<ReadView> getOptionalReadView(String key);

	/**
	 * Возвращает дочернее представление для вложенного объекта.
	 * При отсутствии ключа возвращает пустое представление (null-object).
	 *
	 * @param key ключ вложенного объекта
	 * @return дочерний {@link ReadView}, никогда не {@code null}
	 */
	ReadView getReadView(String key);

	/**
	 * Возвращает представление списка вложенных объектов, если ключ существует.
	 *
	 * @param key ключ списка
	 * @return {@link Optional} с {@link ListReadView}, либо пустой
	 */
	Optional<ListReadView> getOptionalListReadView(String key);

	/**
	 * Возвращает представление списка вложенных объектов.
	 * При отсутствии ключа возвращает пустое представление (null-object).
	 *
	 * @param key ключ списка
	 * @return {@link ListReadView}, никогда не {@code null}
	 */
	ListReadView getListReadView(String key);

	/**
	 * Возвращает типизированное представление списка, если ключ существует.
	 *
	 * @param key       ключ списка
	 * @param typeCodec кодек для элементов списка
	 * @param <T>       тип элементов
	 * @return {@link Optional} с {@link TypedListReadView}, либо пустой
	 */
	<T> Optional<TypedListReadView<T>> getOptionalTypedListView(String key, Codec<T> typeCodec);

	/**
	 * Возвращает типизированное представление списка.
	 * При отсутствии ключа возвращает пустое представление (null-object).
	 *
	 * @param key       ключ списка
	 * @param typeCodec кодек для элементов списка
	 * @param <T>       тип элементов
	 * @return {@link TypedListReadView}, никогда не {@code null}
	 */
	<T> TypedListReadView<T> getTypedListView(String key, Codec<T> typeCodec);

	boolean getBoolean(String key, boolean fallback);

	byte getByte(String key, byte fallback);

	int getShort(String key, short fallback);

	Optional<Integer> getOptionalInt(String key);

	int getInt(String key, int fallback);

	long getLong(String key, long fallback);

	Optional<Long> getOptionalLong(String key);

	float getFloat(String key, float fallback);

	double getDouble(String key, double fallback);

	Optional<String> getOptionalString(String key);

	String getString(String key, String fallback);

	Optional<int[]> getOptionalIntArray(String key);

	/**
	 * @deprecated Используй {@link net.minecraft.registry.RegistryWrapper.WrapperLookup} напрямую
	 */
	@Deprecated
	RegistryWrapper.WrapperLookup getRegistries();

	/**
	 * Представление списка вложенных объектов ({@link ReadView}).
	 * Поддерживает итерацию и потоковую обработку.
	 */
	interface ListReadView extends Iterable<ReadView> {

		boolean isEmpty();

		Stream<ReadView> stream();
	}

	/**
	 * Типизированное представление списка, элементы которого декодируются через кодек.
	 *
	 * @param <T> тип элементов списка
	 */
	interface TypedListReadView<T> extends Iterable<T> {

		boolean isEmpty();

		Stream<T> stream();
	}
}
