package net.minecraft.registry;

/**
 * Функциональный интерфейс для получения значения по ключу реестра.
 * Используется там, где значение может зависеть от конкретного ключа
 * (например, разные параметры для разных записей реестра).
 *
 * @param <T> тип объекта, которому принадлежит ключ реестра
 * @param <V> тип возвращаемого значения
 */
@FunctionalInterface
public interface RegistryKeyedValue<T, V> {

	V get(RegistryKey<T> registryKey);

	/**
	 * Создаёт {@link RegistryKeyedValue}, всегда возвращающий одно и то же значение
	 * независимо от переданного ключа.
	 *
	 * @param value фиксированное значение
	 * @return константный {@link RegistryKeyedValue}
	 */
	static <T, V> RegistryKeyedValue<T, V> fixed(V value) {
		return registryKey -> value;
	}
}
