package net.minecraft.world.entity;

import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Простая реализация {@link EntityLookup}, делегирующая запросы по ID/UUID в {@link EntityIndex},
 * а пространственные запросы — в {@link SectionedEntityCache}.
 *
 * @param <T> тип сущностей
 */
public class SimpleEntityLookup<T extends EntityLike> implements EntityLookup<T> {

	private final EntityIndex<T> index;
	private final SectionedEntityCache<T> cache;

	public SimpleEntityLookup(EntityIndex<T> index, SectionedEntityCache<T> cache) {
		this.index = index;
		this.cache = cache;
	}

	@Override
	public @Nullable T get(int id) {
		return index.get(id);
	}

	@Override
	public @Nullable T get(UUID uuid) {
		return index.get(uuid);
	}

	@Override
	public Iterable<T> iterate() {
		return index.iterate();
	}

	@Override
	public <U extends T> void forEach(TypeFilter<T, U> filter, LazyIterationConsumer<U> consumer) {
		index.forEach(filter, consumer);
	}

	@Override
	public void forEachIntersects(Box box, Consumer<T> action) {
		cache.forEachIntersects(box, LazyIterationConsumer.forConsumer(action));
	}

	@Override
	public <U extends T> void forEachIntersects(TypeFilter<T, U> filter, Box box, LazyIterationConsumer<U> consumer) {
		cache.forEachIntersects(filter, box, consumer);
	}
}
