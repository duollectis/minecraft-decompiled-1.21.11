package net.minecraft.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Контекст регистрации элементов в реестре во время bootstrap-фазы.
 * Используется в {@link RegistryBuilder.BootstrapFunction} для добавления
 * элементов в строящийся реестр и получения lookup-ов на другие реестры.
 *
 * @param <T> тип регистрируемых элементов
 */
public interface Registerable<T> {

	/**
	 * Регистрирует элемент с явно указанным жизненным циклом.
	 *
	 * @param key       ключ регистрируемого элемента
	 * @param value     значение элемента
	 * @param lifecycle жизненный цикл (stable/experimental)
	 * @return ссылка на зарегистрированный элемент
	 */
	RegistryEntry.Reference<T> register(RegistryKey<T> key, T value, Lifecycle lifecycle);

	/**
	 * Регистрирует элемент со стабильным жизненным циклом.
	 *
	 * @param key   ключ регистрируемого элемента
	 * @param value значение элемента
	 * @return ссылка на зарегистрированный элемент
	 */
	default RegistryEntry.Reference<T> register(RegistryKey<T> key, T value) {
		return register(key, value, Lifecycle.stable());
	}

	/**
	 * Возвращает lookup для другого реестра, доступного в текущем контексте bootstrap.
	 *
	 * @param registryRef ключ запрашиваемого реестра
	 * @param <S>         тип элементов запрашиваемого реестра
	 * @return lookup для поиска элементов и тегов в указанном реестре
	 */
	<S> RegistryEntryLookup<S> getRegistryLookup(RegistryKey<? extends Registry<? extends S>> registryRef);
}
