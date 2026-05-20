package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface LoadedEntityProcessor {
   LoadedEntityProcessor NOOP = entity -> entity;

   @Nullable Entity process(Entity entity);
}
