package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Планировщик тиков для отдельного чанка.
 * Хранит очередь {@link OrderedTick} и опциональный список «замороженных» тиков,
 * которые ещё не были активированы (чанк не загружен).
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public class ChunkTickScheduler<T> implements SerializableTickScheduler<T>, BasicTickScheduler<T> {

	private final Queue<OrderedTick<T>> tickQueue = new PriorityQueue<>(OrderedTick.TRIGGER_TICK_COMPARATOR);
	private final Set<OrderedTick<?>> queuedTicks = new ObjectOpenCustomHashSet<>(OrderedTick.HASH_STRATEGY);
	private @Nullable List<Tick<T>> pendingTicks;
	private @Nullable BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> tickConsumer;

	public ChunkTickScheduler() {
	}

	/**
	 * Создаёт планировщик с предзагруженным списком тиков из сохранения.
	 * Тики остаются «замороженными» до вызова {@link #disable(long)}.
	 *
	 * @param ticks список тиков, восстановленных из NBT
	 */
	public ChunkTickScheduler(List<Tick<T>> ticks) {
		this.pendingTicks = ticks;

		for (Tick<T> tick : ticks) {
			queuedTicks.add(OrderedTick.create(tick.type(), tick.pos()));
		}
	}

	public void setTickConsumer(@Nullable BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> tickConsumer) {
		this.tickConsumer = tickConsumer;
	}

	/** @return следующий тик в очереди без его извлечения, или {@code null} если очередь пуста */
	public @Nullable OrderedTick<T> peekNextTick() {
		return tickQueue.peek();
	}

	/**
	 * Извлекает и возвращает следующий тик из очереди, удаляя его из набора отслеживания.
	 *
	 * @return извлечённый тик, или {@code null} если очередь пуста
	 */
	public @Nullable OrderedTick<T> pollNextTick() {
		OrderedTick<T> tick = tickQueue.poll();

		if (tick != null) {
			queuedTicks.remove(tick);
		}

		return tick;
	}

	@Override
	public void scheduleTick(OrderedTick<T> orderedTick) {
		if (queuedTicks.add(orderedTick)) {
			enqueueTick(orderedTick);
		}
	}

	private void enqueueTick(OrderedTick<T> orderedTick) {
		tickQueue.add(orderedTick);

		if (tickConsumer != null) {
			tickConsumer.accept(this, orderedTick);
		}
	}

	@Override
	public boolean isQueued(BlockPos pos, T type) {
		return queuedTicks.contains(OrderedTick.create(type, pos));
	}

	/**
	 * Удаляет из очереди все тики, удовлетворяющие предикату.
	 * Используется, например, при выгрузке чанка или взрыве.
	 *
	 * @param predicate условие удаления тика
	 */
	public void removeTicksIf(Predicate<OrderedTick<T>> predicate) {
		Iterator<OrderedTick<T>> iterator = tickQueue.iterator();

		while (iterator.hasNext()) {
			OrderedTick<T> tick = iterator.next();

			if (predicate.test(tick)) {
				iterator.remove();
				queuedTicks.remove(tick);
			}
		}
	}

	public Stream<OrderedTick<T>> getQueueAsStream() {
		return tickQueue.stream();
	}

	@Override
	public int getTickCount() {
		return tickQueue.size() + (pendingTicks != null ? pendingTicks.size() : 0);
	}

	@Override
	public List<Tick<T>> collectTicks(long time) {
		List<Tick<T>> result = new ArrayList<>(tickQueue.size());

		if (pendingTicks != null) {
			result.addAll(pendingTicks);
		}

		for (OrderedTick<T> tick : tickQueue) {
			result.add(tick.toTick(time));
		}

		return result;
	}

	/**
	 * Активирует «замороженные» тики, переводя их из списка ожидания в активную очередь.
	 * Вызывается при загрузке чанка в мир.
	 *
	 * @param time текущее игровое время в тиках
	 */
	public void disable(long time) {
		if (pendingTicks != null) {
			int subTickOffset = -pendingTicks.size();

			for (Tick<T> tick : pendingTicks) {
				enqueueTick(tick.createOrderedTick(time, subTickOffset++));
			}
		}

		pendingTicks = null;
	}
}
