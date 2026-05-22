package net.minecraft.predicate.component;

import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;

/**
 * Базовый интерфейс для предикатов, проверяющих значение конкретного компонента.
 * Реализует {@link #test(ComponentsAccess)} через получение компонента по типу
 * и делегирование в типизированный {@link #test(Object)}.
 *
 * @param <T> тип значения компонента
 */
public interface ComponentSubPredicate<T> extends ComponentPredicate {

	@Override
	default boolean test(ComponentsAccess components) {
		T value = components.get(getComponentType());
		return value != null && test(value);
	}

	ComponentType<T> getComponentType();

	boolean test(T component);
}
