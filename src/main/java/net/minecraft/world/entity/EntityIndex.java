package net.minecraft.world.entity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * {@code EntityIndex}.
 */
public class EntityIndex<T extends EntityLike> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final Int2ObjectMap<T> idToEntity = new Int2ObjectLinkedOpenHashMap();
	private final Map<UUID, T> uuidToEntity = Maps.newHashMap();

	/**
	 * For each.
	 *
	 * @param filter filter
	 * @param consumer consumer
	 *
	 * @return void — результат операции
	 */
	public <U extends T> void forEach(TypeFilter<T, U> filter, LazyIterationConsumer<U> consumer) {
		ObjectIterator var3 = this.idToEntity.values().iterator();

		while (var3.hasNext()) {
			T entityLike = (T) var3.next();
			U entityLike2 = (U) filter.downcast(entityLike);
			if (entityLike2 != null && consumer.accept(entityLike2).shouldAbort()) {
				return;
			}
		}
	}

	/**
	 * Iterate.
	 *
	 * @return Iterable — результат операции
	 */
	public Iterable<T> iterate() {
		return Iterables.unmodifiableIterable(this.idToEntity.values());
	}

	/**
	 * Add.
	 *
	 * @param entity entity
	 */
	public void add(T entity) {
		UUID uUID = entity.getUuid();
		if (this.uuidToEntity.containsKey(uUID)) {
			LOGGER.warn("Duplicate entity UUID {}: {}", uUID, entity);
		}
		else {
			this.uuidToEntity.put(uUID, entity);
			this.idToEntity.put(entity.getId(), entity);
		}
	}

	/**
	 * Remove.
	 *
	 * @param entity entity
	 */
	public void remove(T entity) {
		this.uuidToEntity.remove(entity.getUuid());
		this.idToEntity.remove(entity.getId());
	}

	/**
	 * Get.
	 *
	 * @param id id
	 *
	 * @return @Nullable T — 
	 */
	public @Nullable T get(int id) {
		return (T) this.idToEntity.get(id);
	}

	/**
	 * Get.
	 *
	 * @param uuid uuid
	 *
	 * @return @Nullable T — 
	 */
	public @Nullable T get(UUID uuid) {
		return this.uuidToEntity.get(uuid);
	}

	/**
	 * Size.
	 *
	 * @return int — результат операции
	 */
	public int size() {
		return this.uuidToEntity.size();
	}
}
