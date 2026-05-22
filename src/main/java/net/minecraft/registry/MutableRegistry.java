package net.minecraft.registry;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.tag.TagKey;

import java.util.List;

/**
 * Расширение {@link Registry}, допускающее мутацию: добавление элементов и установку тегов.
 * После вызова {@link #freeze()} реестр становится иммутабельным.
 *
 * @param <T> тип элементов реестра
 */
public interface MutableRegistry<T> extends Registry<T> {

	/**
	 * Добавляет элемент в реестр и возвращает его {@link RegistryEntry.Reference}.
	 *
	 * @param key   ключ регистрируемого элемента
	 * @param value значение элемента
	 * @param info  метаданные записи (lifecycle, pack info)
	 * @return ссылка на зарегистрированный элемент
	 * @throws IllegalStateException если реестр заморожен или ключ/значение уже зарегистрированы
	 */
	RegistryEntry.Reference<T> add(RegistryKey<T> key, T value, RegistryEntryInfo info);

	/**
	 * Устанавливает список элементов для указанного тега.
	 *
	 * @param tag     ключ тега
	 * @param entries список записей реестра, входящих в тег
	 */
	void setEntries(TagKey<T> tag, List<RegistryEntry<T>> entries);

	boolean isEmpty();

	/**
	 * Создаёт {@link RegistryEntryLookup} для использования во время загрузки данных.
	 * Lookup создаёт placeholder-записи для ещё не зарегистрированных ключей.
	 *
	 * @return lookup, допускающий обращение к незарегистрированным ключам
	 * @throws IllegalStateException если реестр уже заморожен
	 */
	RegistryEntryLookup<T> createMutableRegistryLookup();
}
