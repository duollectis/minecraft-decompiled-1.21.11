package net.minecraft.world.tick;

import net.minecraft.util.math.BlockPos;

/**
 * Расширение {@link TickScheduler}, позволяющее проверять, выполняется ли тик
 * прямо сейчас (в текущем игровом тике). Используется на серверной стороне
 * для предотвращения двойного срабатывания.
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public interface QueryableTickScheduler<T> extends TickScheduler<T> {

	/**
	 * @param pos  позиция блока
	 * @param type тип объекта
	 * @return {@code true}, если тик для данной пары (позиция, тип) выполняется прямо сейчас
	 */
	boolean isTicking(BlockPos pos, T type);
}
