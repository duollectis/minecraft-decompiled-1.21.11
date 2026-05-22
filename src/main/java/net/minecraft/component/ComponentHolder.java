package net.minecraft.component;

import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
	 * Интерфейс для объектов, хранящих набор компонентов данных ({@link ComponentMap}).
	 * Предоставляет удобные методы доступа к компонентам через делегирование к {@link #getComponents()}.
	 */
public interface ComponentHolder extends ComponentsAccess {

	ComponentMap getComponents();

	@Override
	default <T> @Nullable T get(ComponentType<? extends T> type) {
		return getComponents().get(type);
	}

	/**
		 * Возвращает поток всех значений компонентов, являющихся экземплярами указанного класса.
		 *
		 * @param valueClass класс для фильтрации значений компонентов
		 * @return поток значений совместимого типа
		 */
	default <T> Stream<T> streamAll(Class<? extends T> valueClass) {
		return getComponents()
				.stream()
				.map(Component::value)
				.filter(value -> valueClass.isAssignableFrom(value.getClass()))
				.map(value -> (T) value);
	}

	@Override
	default <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
		return getComponents().getOrDefault(type, fallback);
	}

	default boolean contains(ComponentType<?> type) {
		return getComponents().contains(type);
	}
}
