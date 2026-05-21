package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.Hash.Strategy;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * {@code OrderedTick}.
 */
public record OrderedTick<T>(T type, BlockPos pos, long triggerTick, TickPriority priority, long subTickOrder) {

	public static final Comparator<OrderedTick<?>> TRIGGER_TICK_COMPARATOR = (first, second) -> {
		int i = Long.compare(first.triggerTick, second.triggerTick);
		if (i != 0) {
			return i;
		}
		else {
			i = first.priority.compareTo(second.priority);
			return i != 0 ? i : Long.compare(first.subTickOrder, second.subTickOrder);
		}
	};
	public static final Comparator<OrderedTick<?>> BASIC_COMPARATOR = (first, second) -> {
		int i = first.priority.compareTo(second.priority);
		return i != 0 ? i : Long.compare(first.subTickOrder, second.subTickOrder);
	};
	public static final Strategy<OrderedTick<?>> HASH_STRATEGY = new Strategy<OrderedTick<?>>() {
		/**
		 * Проверяет наличие h code.
		 *
		 * @param orderedTick ordered tick
		 *
		 * @return int — {@code true} если условие выполнено
		 */
		public int hashCode(OrderedTick<?> orderedTick) {
			return 31 * orderedTick.pos().hashCode() + orderedTick.type().hashCode();
		}

		/**
		 * Equals.
		 *
		 * @param orderedTick ordered tick
		 * @param orderedTick2 ordered tick2
		 *
		 * @return boolean — результат операции
		 */
		public boolean equals(@Nullable OrderedTick<?> orderedTick, @Nullable OrderedTick<?> orderedTick2) {
			if (orderedTick == orderedTick2) {
				return true;
			}
			else {
				return orderedTick != null && orderedTick2 != null
				       ? orderedTick.type() == orderedTick2.type() && orderedTick.pos().equals(orderedTick2.pos())
				       : false;
			}
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
	 * Create.
	 *
	 * @param type type
	 * @param pos pos
	 *
	 * @return OrderedTick — результат операции
	 */
	public static <T> OrderedTick<T> create(T type, BlockPos pos) {
		return new OrderedTick<>(type, pos, 0L, TickPriority.NORMAL, 0L);
	}

	/**
	 * To tick.
	 *
	 * @param time time
	 *
	 * @return Tick — результат операции
	 */
	public Tick<T> toTick(long time) {
		return new Tick<>(this.type, this.pos, (int) (this.triggerTick - time), this.priority);
	}
}
