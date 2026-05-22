package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.Hash.Strategy;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * Упорядоченный тик — единица работы в очереди планировщика.
 * Содержит тип объекта, позицию, время срабатывания, приоритет и порядковый номер
 * для детерминированной сортировки при одинаковом времени.
 *
 * @param <T>          тип объекта (блок, жидкость и т.д.)
 * @param type         объект, которому нужно выполнить тик
 * @param pos          позиция блока
 * @param triggerTick  игровой тик, в который должен сработать этот тик
 * @param priority     приоритет выполнения
 * @param subTickOrder порядковый номер для детерминированной сортировки
 */
public record OrderedTick<T>(T type, BlockPos pos, long triggerTick, TickPriority priority, long subTickOrder) {

	/**
	 * Компаратор с учётом времени срабатывания, затем приоритета, затем порядкового номера.
	 * Используется в {@link java.util.PriorityQueue} планировщика.
	 */
	public static final Comparator<OrderedTick<?>> TRIGGER_TICK_COMPARATOR = (first, second) -> {
		int cmp = Long.compare(first.triggerTick, second.triggerTick);

		if (cmp != 0) {
			return cmp;
		}

		cmp = first.priority.compareTo(second.priority);
		return cmp != 0 ? cmp : Long.compare(first.subTickOrder, second.subTickOrder);
	};

	/**
	 * Компаратор без учёта времени срабатывания — только приоритет и порядковый номер.
	 * Используется при сравнении тиков внутри одного временного слота.
	 */
	public static final Comparator<OrderedTick<?>> BASIC_COMPARATOR = (first, second) -> {
		int cmp = first.priority.compareTo(second.priority);
		return cmp != 0 ? cmp : Long.compare(first.subTickOrder, second.subTickOrder);
	};

	/**
	 * Стратегия хэширования по паре (тип, позиция) для {@code ObjectOpenCustomHashSet}.
	 * Позволяет проверять наличие тика без учёта времени и приоритета.
	 */
	public static final Strategy<OrderedTick<?>> HASH_STRATEGY = new Strategy<>() {
		@Override
		public int hashCode(OrderedTick<?> tick) {
			return 31 * tick.pos().hashCode() + tick.type().hashCode();
		}

		@Override
		public boolean equals(@Nullable OrderedTick<?> a, @Nullable OrderedTick<?> b) {
			if (a == b) {
				return true;
			}

			return a != null && b != null
				? a.type() == b.type() && a.pos().equals(b.pos())
				: false;
		}
	};

	public OrderedTick(T type, BlockPos pos, long triggerTick, long subTickOrder) {
		this(type, pos, triggerTick, TickPriority.NORMAL, subTickOrder);
	}

	public OrderedTick(T type, BlockPos pos, long triggerTick, TickPriority priority, long subTickOrder) {
		pos = pos.toImmutable();
		this.type = type;
		this.pos = pos;
		this.triggerTick = triggerTick;
		this.priority = priority;
		this.subTickOrder = subTickOrder;
	}

	/**
	 * Создаёт «ключевой» тик с нулевым временем и нормальным приоритетом.
	 * Используется исключительно для поиска в хэш-наборе по паре (тип, позиция).
	 *
	 * @param type тип объекта
	 * @param pos  позиция блока
	 */
	public static <T> OrderedTick<T> create(T type, BlockPos pos) {
		return new OrderedTick<>(type, pos, 0L, TickPriority.NORMAL, 0L);
	}

	/**
	 * Конвертирует в {@link Tick} для сериализации, вычисляя относительную задержку.
	 *
	 * @param currentTime текущее игровое время в тиках
	 * @return тик с относительной задержкой {@code triggerTick - currentTime}
	 */
	public Tick<T> toTick(long currentTime) {
		return new Tick<>(type, pos, (int) (triggerTick - currentTime), priority);
	}
}
