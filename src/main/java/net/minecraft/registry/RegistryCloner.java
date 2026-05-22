package net.minecraft.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Клонирует элементы реестра через цикл кодирования/декодирования.
 * Используется в {@link RegistryBuilder} для создания патч-реестров,
 * где элементы из базового реестра должны быть глубоко скопированы
 * в новый контекст с другими зависимостями.
 *
 * @param <T> тип клонируемых элементов
 */
public class RegistryCloner<T> {

	private final Codec<T> elementCodec;

	RegistryCloner(Codec<T> elementCodec) {
		this.elementCodec = elementCodec;
	}

	/**
	 * Клонирует элемент реестра через encode → decode с разными контекстами.
	 * Кодирование происходит в контексте {@code subsetRegistry} (патч),
	 * а декодирование — в контексте {@code fullRegistry} (полный реестр).
	 * Это позволяет перепривязать ссылки на другие реестровые записи.
	 *
	 * @param value          исходный элемент для клонирования
	 * @param subsetRegistry реестр-источник (используется при кодировании)
	 * @param fullRegistry   полный реестр-цель (используется при декодировании)
	 * @return глубокая копия элемента в новом контексте
	 * @throws IllegalStateException если кодирование или декодирование завершилось ошибкой
	 */
	@SuppressWarnings("unchecked")
	public T clone(T value, RegistryWrapper.WrapperLookup subsetRegistry, RegistryWrapper.WrapperLookup fullRegistry) {
		DynamicOps<Object> encodeOps = subsetRegistry.getOps(JavaOps.INSTANCE);
		DynamicOps<Object> decodeOps = fullRegistry.getOps(JavaOps.INSTANCE);

		Object encoded = elementCodec
				.encodeStart(encodeOps, value)
				.getOrThrow(error -> new IllegalStateException("Failed to encode: " + error));

		return (T) elementCodec
				.parse(decodeOps, encoded)
				.getOrThrow(error -> new IllegalStateException("Failed to decode: " + error));
	}

	/**
	 * Реестр клонеров, сопоставляющий ключи реестров с соответствующими клонерами.
	 * Заполняется при инициализации через {@link RegistryLoader#DYNAMIC_REGISTRIES}.
	 */
	public static class CloneableRegistries {

		private final Map<RegistryKey<? extends Registry<?>>, RegistryCloner<?>> registries = new HashMap<>();

		/**
		 * Регистрирует клонер для указанного реестра.
		 *
		 * @param registryRef  ключ реестра
		 * @param elementCodec кодек элементов реестра
		 * @param <T>          тип элементов
		 * @return {@code this} для цепочки вызовов
		 */
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T> RegistryCloner.CloneableRegistries add(
				RegistryKey<? extends Registry<? extends T>> registryRef,
				Codec<T> elementCodec
		) {
			registries.put(registryRef, new RegistryCloner(elementCodec));
			return this;
		}

		/**
		 * Возвращает клонер для указанного реестра, или {@code null} если клонер не зарегистрирован.
		 *
		 * @param registryRef ключ реестра
		 * @param <T>         тип элементов
		 * @return клонер или {@code null}
		 */
		@SuppressWarnings("unchecked")
		public <T> @Nullable RegistryCloner<T> get(RegistryKey<? extends Registry<? extends T>> registryRef) {
			return (RegistryCloner<T>) registries.get(registryRef);
		}
	}
}
