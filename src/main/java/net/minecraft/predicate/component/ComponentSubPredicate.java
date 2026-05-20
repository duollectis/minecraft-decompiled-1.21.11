package net.minecraft.predicate.component;

import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;

public interface ComponentSubPredicate<T> extends ComponentPredicate {
   @Override
   default boolean test(ComponentsAccess components) {
      T object = components.get(this.getComponentType());
      return object != null && this.test(object);
   }

   ComponentType<T> getComponentType();

   boolean test(T component);
}
