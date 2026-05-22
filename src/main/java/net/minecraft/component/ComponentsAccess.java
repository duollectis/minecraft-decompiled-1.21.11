package net.minecraft.component;

import org.jspecify.annotations.Nullable;

/**
	 * Базовый интерфейс для доступа к компонентам данных предмета.
	 * Предоставляет методы получения значений по типу компонента.
	 */
public interface ComponentsAccess {

	<T> @Nullable T get(ComponentType<? extends T> type);

	default <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
		T value = get(type);
		return value != null ? value : fallback;
	}

	default <T> @Nullable Component<T> getTyped(ComponentType<T> type) {
		T value = get(type);
		return value != null ? new Component<>(type, value) : null;
	}
}
