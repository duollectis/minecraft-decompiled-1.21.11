package net.minecraft.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Функциональный интерфейс для перекодирования значения из одного контекста реестров в другой.
 * Используется в {@link ContextSwappableRegistryLookup} для переноса данных между
 * контекстом датагена и контекстом полного реестра.
 */
@FunctionalInterface
public interface ContextSwapper {

	/**
	 * Перекодирует значение из текущего контекста в контекст указанного реестра.
	 * Операция выполняется через encode → decode с разными {@link RegistryWrapper.WrapperLookup}.
	 *
	 * @param codec      кодек для сериализации/десериализации значения
	 * @param value      исходное значение в текущем контексте
	 * @param registries целевой контекст реестров для декодирования
	 * @param <T>        тип значения
	 * @return результат перекодирования или ошибка
	 */
	<T> DataResult<T> swapContext(Codec<T> codec, T value, RegistryWrapper.WrapperLookup registries);
}
