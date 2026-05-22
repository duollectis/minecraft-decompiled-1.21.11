package net.minecraft.world.timer;

import com.mojang.serialization.MapCodec;

/**
 * Callback, вызываемый при срабатывании события {@link Timer}.
 * Реализации должны предоставлять {@link MapCodec} для сериализации в NBT.
 *
 * @param <T> тип сервера, передаваемого при вызове
 */
public interface TimerCallback<T> {

	/**
	 * Выполняет действие при срабатывании события таймера.
	 *
	 * @param server объект сервера
	 * @param timer  таймер, из которого было вызвано событие
	 * @param time   текущее игровое время в тиках
	 */
	void call(T server, Timer<T> timer, long time);

	MapCodec<? extends TimerCallback<T>> getCodec();
}
