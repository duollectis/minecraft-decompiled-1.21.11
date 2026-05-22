package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Простой планировщик тиков без учёта времени срабатывания.
 * Используется для структур и фич, где тики должны выполниться при следующей загрузке чанка.
 * Все тики хранятся с нулевой задержкой.
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public class SimpleTickScheduler<T> implements SerializableTickScheduler<T>, BasicTickScheduler<T> {

	private final List<Tick<T>> scheduledTicks = new ArrayList<>();
	private final Set<Tick<?>> scheduledTicksSet = new ObjectOpenCustomHashSet<>(Tick.HASH_STRATEGY);

	@Override
	public void scheduleTick(OrderedTick<T> orderedTick) {
		Tick<T> tick = new Tick<>(orderedTick.type(), orderedTick.pos(), 0, orderedTick.priority());
		scheduleTick(tick);
	}

	private void scheduleTick(Tick<T> tick) {
		if (scheduledTicksSet.add(tick)) {
			scheduledTicks.add(tick);
		}
	}

	@Override
	public boolean isQueued(BlockPos pos, T type) {
		return scheduledTicksSet.contains(Tick.create(type, pos));
	}

	@Override
	public int getTickCount() {
		return scheduledTicks.size();
	}

	@Override
	public List<Tick<T>> collectTicks(long currentTime) {
		return scheduledTicks;
	}

	public List<Tick<T>> getTicks() {
		return List.copyOf(scheduledTicks);
	}

	/**
	 * Создаёт планировщик, предзаполненный переданным списком тиков.
	 *
	 * @param ticks список тиков для загрузки
	 */
	public static <T> SimpleTickScheduler<T> tick(List<Tick<T>> ticks) {
		SimpleTickScheduler<T> scheduler = new SimpleTickScheduler<>();
		ticks.forEach(scheduler::scheduleTick);
		return scheduler;
	}
}
