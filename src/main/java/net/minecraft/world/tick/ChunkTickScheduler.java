package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@code ChunkTickScheduler}.
 */
public class ChunkTickScheduler<T> implements SerializableTickScheduler<T>, BasicTickScheduler<T> {

	private final Queue<OrderedTick<T>> tickQueue = new PriorityQueue<>(OrderedTick.TRIGGER_TICK_COMPARATOR);
	private @Nullable List<Tick<T>> ticks;
	private final Set<OrderedTick<?>> queuedTicks = new ObjectOpenCustomHashSet(OrderedTick.HASH_STRATEGY);
	private @Nullable BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> tickConsumer;

	public ChunkTickScheduler() {
	}

	public ChunkTickScheduler(List<Tick<T>> ticks) {
		this.ticks = ticks;

		for (Tick<T> tick : ticks) {
			this.queuedTicks.add(OrderedTick.create(tick.type(), tick.pos()));
		}
	}

	public void setTickConsumer(@Nullable BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> tickConsumer) {
		this.tickConsumer = tickConsumer;
	}

	public @Nullable OrderedTick<T> peekNextTick() {
		return this.tickQueue.peek();
	}

	public @Nullable OrderedTick<T> pollNextTick() {
		OrderedTick<T> orderedTick = this.tickQueue.poll();
		if (orderedTick != null) {
			this.queuedTicks.remove(orderedTick);
		}

		return orderedTick;
	}

	@Override
	public void scheduleTick(OrderedTick<T> orderedTick) {
		if (this.queuedTicks.add(orderedTick)) {
			this.queueTick(orderedTick);
		}
	}

	private void queueTick(OrderedTick<T> orderedTick) {
		this.tickQueue.add(orderedTick);
		if (this.tickConsumer != null) {
			this.tickConsumer.accept(this, orderedTick);
		}
	}

	@Override
	public boolean isQueued(BlockPos pos, T type) {
		return this.queuedTicks.contains(OrderedTick.create(type, pos));
	}

	public void removeTicksIf(Predicate<OrderedTick<T>> predicate) {
		Iterator<OrderedTick<T>> iterator = this.tickQueue.iterator();

		while (iterator.hasNext()) {
			OrderedTick<T> orderedTick = iterator.next();
			if (predicate.test(orderedTick)) {
				iterator.remove();
				this.queuedTicks.remove(orderedTick);
			}
		}
	}

	public Stream<OrderedTick<T>> getQueueAsStream() {
		return this.tickQueue.stream();
	}

	@Override
	public int getTickCount() {
		return this.tickQueue.size() + (this.ticks != null ? this.ticks.size() : 0);
	}

	@Override
	public List<Tick<T>> collectTicks(long time) {
		List<Tick<T>> list = new ArrayList<>(this.tickQueue.size());
		if (this.ticks != null) {
			list.addAll(this.ticks);
		}

		for (OrderedTick<T> orderedTick : this.tickQueue) {
			list.add(orderedTick.toTick(time));
		}

		return list;
	}

	public void disable(long time) {
		if (this.ticks != null) {
			int i = -this.ticks.size();

			for (Tick<T> tick : this.ticks) {
				this.queueTick(tick.createOrderedTick(time, i++));
			}
		}

		this.ticks = null;
	}
}
