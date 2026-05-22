package net.minecraft.world.tick;

import net.minecraft.util.math.BlockPos;

import java.util.function.Function;

/**
 * Планировщик тиков, делегирующий операции конкретному {@link BasicTickScheduler}
 * на основе позиции блока. Используется для маршрутизации тиков по чанкам.
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public class MultiTickScheduler<T> implements QueryableTickScheduler<T> {

	private final Function<BlockPos, BasicTickScheduler<T>> schedulerByPos;

	/**
	 * @param schedulerByPos функция, возвращающая планировщик для заданной позиции блока
	 */
	public MultiTickScheduler(Function<BlockPos, BasicTickScheduler<T>> schedulerByPos) {
		this.schedulerByPos = schedulerByPos;
	}

	@Override
	public boolean isQueued(BlockPos pos, T type) {
		return schedulerByPos.apply(pos).isQueued(pos, type);
	}

	@Override
	public void scheduleTick(OrderedTick<T> orderedTick) {
		schedulerByPos.apply(orderedTick.pos()).scheduleTick(orderedTick);
	}

	@Override
	public boolean isTicking(BlockPos pos, T type) {
		return false;
	}

	@Override
	public int getTickCount() {
		return 0;
	}
}
