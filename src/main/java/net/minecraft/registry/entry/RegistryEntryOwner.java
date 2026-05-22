package net.minecraft.registry.entry;

/**
 * Маркерный интерфейс владельца записей реестра.
 * Используется для проверки принадлежности {@link RegistryEntry} и
 * {@link RegistryEntryList} конкретному реестру при сериализации,
 * предотвращая смешивание записей из разных контекстов реестров.
 *
 * @param <T> тип объектов, хранящихся в реестре
 */
public interface RegistryEntryOwner<T> {

	/**
	 * Проверяет, является ли переданный {@code owner} тем же владельцем,
	 * что и данный объект. По умолчанию — сравнение по ссылке.
	 *
	 * @param owner другой владелец для сравнения
	 * @return {@code true}, если владельцы совпадают
	 */
	default boolean ownerEquals(RegistryEntryOwner<T> owner) {
		return this == owner;
	}
}
