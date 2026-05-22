package net.minecraft.predicate.component;

import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;

/**
 * Предикат, проверяющий наличие компонента заданного типа в наборе компонентов.
 */
public record ComponentExistencePredicate(ComponentType<?> type) implements ComponentPredicate {

	@Override
	public boolean test(ComponentsAccess components) {
		return components.get(type) != null;
	}
}
