package net.minecraft.world.tick;

import net.minecraft.util.math.BlockPos;

/**
 * Базовый контракт планировщика тиков: постановка в очередь, проверка наличия
 * и получение общего количества запланированных тиков.
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public interface TickScheduler<T> {

	/**
	 * Ставит тик в очередь. Если тик с такой же парой (тип, позиция) уже запланирован,
	 * повторная постановка игнорируется.
	 *
	 * @param orderedTick тик для постановки в очередь
	 */
	void scheduleTick(OrderedTick<T> orderedTick);

	/**
	 * @param pos  позиция блока
	 * @param type тип объекта
	 * @return {@code true}, если тик для данной пары (позиция, тип) уже запланирован
	 */
	boolean isQueued(BlockPos pos, T type);

	/** @return общее количество запланированных тиков */
	int getTickCount();
}
