package net.minecraft.world.entity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * Двойной индекс сущностей: по числовому ID и по UUID.
 * Обеспечивает O(1) поиск по обоим ключам и итерацию по всем сущностям.
 *
 * @param <T> тип сущностей в индексе
 */
public class EntityIndex<T extends EntityLike> {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Int2ObjectMap<T> idToEntity = new Int2ObjectLinkedOpenHashMap<>();
	private final Map<UUID, T> uuidToEntity = Maps.newHashMap();

	/**
	 * Итерирует все сущности, применяя типовой фильтр и передавая подходящие в consumer.
	 * Прерывает итерацию досрочно, если consumer вернул {@code ABORT}.
	 *
	 * @param filter   фильтр типа для downcasting
	 * @param consumer получатель отфильтрованных сущностей
	 * @param <U>      целевой тип после фильтрации
	 */
	public <U extends T> void forEach(TypeFilter<T, U> filter, LazyIterationConsumer<U> consumer) {
		for (T entity : idToEntity.values()) {
			U cast = filter.downcast(entity);
			if (cast != null && consumer.accept(cast).shouldAbort()) {
				return;
			}
		}
	}

	public Iterable<T> iterate() {
		return Iterables.unmodifiableIterable(idToEntity.values());
	}

	/**
	 * Добавляет сущность в индекс. Логирует предупреждение при дублировании UUID,
	 * но не перезаписывает существующую запись.
	 *
	 * @param entity добавляемая сущность
	 */
	public void add(T entity) {
		UUID uuid = entity.getUuid();
		if (uuidToEntity.containsKey(uuid)) {
			LOGGER.warn("Duplicate entity UUID {}: {}", uuid, entity);
			return;
		}

		uuidToEntity.put(uuid, entity);
		idToEntity.put(entity.getId(), entity);
	}

	public void remove(T entity) {
		uuidToEntity.remove(entity.getUuid());
		idToEntity.remove(entity.getId());
	}

	public @Nullable T get(int id) {
		return idToEntity.get(id);
	}

	public @Nullable T get(UUID uuid) {
		return uuidToEntity.get(uuid);
	}

	public int size() {
		return uuidToEntity.size();
	}
}
