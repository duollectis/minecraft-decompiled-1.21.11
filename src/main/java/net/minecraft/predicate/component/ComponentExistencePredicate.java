package net.minecraft.predicate.component;

import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;

/**
 * {@code ComponentExistencePredicate}.
 */
public record ComponentExistencePredicate(ComponentType<?> type) implements ComponentPredicate {

	@Override
	public boolean test(ComponentsAccess components) {
		return components.get(this.type) != null;
	}
}
