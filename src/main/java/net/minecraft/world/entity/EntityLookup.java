package net.minecraft.world.entity;

import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Интерфейс поиска сущностей по идентификатору, UUID, пространственному пересечению
 * и типовому фильтру. Является основным API для запросов к системе сущностей.
 *
 * @param <T> базовый тип сущностей в данном lookup
 */
public interface EntityLookup<T extends EntityLike> {

	@Nullable T get(int id);

	@Nullable T get(UUID uuid);

	Iterable<T> iterate();

	<U extends T> void forEach(TypeFilter<T, U> filter, LazyIterationConsumer<U> consumer);

	void forEachIntersects(Box box, Consumer<T> action);

	<U extends T> void forEachIntersects(TypeFilter<T, U> filter, Box box, LazyIterationConsumer<U> consumer);
}
