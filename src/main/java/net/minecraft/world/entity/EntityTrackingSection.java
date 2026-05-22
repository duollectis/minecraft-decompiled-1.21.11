package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Секция отслеживания сущностей, соответствующая одной чанк-секции (16×16×16 блоков).
 * Хранит коллекцию сущностей и текущий статус отслеживания секции.
 * Поддерживает пространственные запросы по AABB и типовую фильтрацию.
 *
 * @param <T> тип сущностей в секции
 */
public class EntityTrackingSection<T extends EntityLike> {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final TypeFilterableList<T> collection;
	private EntityTrackingStatus status;

	public EntityTrackingSection(Class<T> entityClass, EntityTrackingStatus status) {
		this.status = status;
		this.collection = new TypeFilterableList<>(entityClass);
	}

	public void add(T entity) {
		collection.add(entity);
	}

	public boolean remove(T entity) {
		return collection.remove(entity);
	}

	/**
	 * Итерирует сущности, чьи bounding box пересекаются с указанным box.
	 * Прерывает итерацию досрочно при получении {@code ABORT} от consumer.
	 *
	 * @param box      область поиска
	 * @param consumer получатель найденных сущностей
	 * @return {@code ABORT}, если итерация была прервана, иначе {@code CONTINUE}
	 */
	public LazyIterationConsumer.NextIteration forEach(Box box, LazyIterationConsumer<T> consumer) {
		for (T entity : collection) {
			if (entity.getBoundingBox().intersects(box) && consumer.accept(entity).shouldAbort()) {
				return LazyIterationConsumer.NextIteration.ABORT;
			}
		}

		return LazyIterationConsumer.NextIteration.CONTINUE;
	}

	/**
	 * Итерирует сущности указанного типа, чьи bounding box пересекаются с box.
	 * Использует {@link TypeFilterableList} для эффективной фильтрации по типу.
	 *
	 * @param type     фильтр типа
	 * @param box      область поиска
	 * @param consumer получатель найденных сущностей
	 * @param <U>      целевой тип после фильтрации
	 * @return {@code ABORT}, если итерация была прервана, иначе {@code CONTINUE}
	 */
	public <U extends T> LazyIterationConsumer.NextIteration forEach(
		TypeFilter<T, U> type,
		Box box,
		LazyIterationConsumer<? super U> consumer
	) {
		Collection<? extends T> typed = collection.getAllOfType(type.getBaseClass());
		if (typed.isEmpty()) {
			return LazyIterationConsumer.NextIteration.CONTINUE;
		}

		for (T entity : typed) {
			U cast = type.downcast(entity);
			if (cast != null && entity.getBoundingBox().intersects(box) && consumer.accept(cast).shouldAbort()) {
				return LazyIterationConsumer.NextIteration.ABORT;
			}
		}

		return LazyIterationConsumer.NextIteration.CONTINUE;
	}

	public boolean isEmpty() {
		return collection.isEmpty();
	}

	public Stream<T> stream() {
		return collection.stream();
	}

	public EntityTrackingStatus getStatus() {
		return status;
	}

	/**
	 * Атомарно заменяет статус секции и возвращает предыдущий.
	 * Используется при изменении состояния загрузки чанка.
	 *
	 * @param newStatus новый статус отслеживания
	 * @return предыдущий статус отслеживания
	 */
	public EntityTrackingStatus swapStatus(EntityTrackingStatus newStatus) {
		EntityTrackingStatus previous = status;
		status = newStatus;
		return previous;
	}

	@Debug
	public int size() {
		return collection.size();
	}
}
