package net.minecraft.world.tick;

import java.util.List;

/**
 * Планировщик тиков, поддерживающий сериализацию своего состояния в список {@link Tick}.
 * Используется при сохранении чанка — все запланированные тики конвертируются
 * в относительные задержки и записываются в NBT.
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public interface SerializableTickScheduler<T> {

	/**
	 * Собирает все запланированные тики, конвертируя абсолютное время срабатывания
	 * в относительную задержку {@code triggerTick - currentTime}.
	 *
	 * @param currentTime текущее игровое время в тиках
	 * @return список тиков для записи в NBT
	 */
	List<Tick<T>> collectTicks(long currentTime);
}
