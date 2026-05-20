package net.minecraft.world.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

public interface EntityLookup<T extends EntityLike> {
   @Nullable T get(int id);

   @Nullable T get(UUID uuid);

   Iterable<T> iterate();

   <U extends T> void forEach(TypeFilter<T, U> filter, LazyIterationConsumer<U> consumer);

   void forEachIntersects(Box box, Consumer<T> action);

   <U extends T> void forEachIntersects(TypeFilter<T, U> filter, Box box, LazyIterationConsumer<U> consumer);
}
