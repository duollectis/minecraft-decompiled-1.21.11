package net.minecraft.world.debug;

import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

/**
 * Маркерный интерфейс для объектов, поддерживающих отладочное отслеживание.
 * <p>
 * Реализующий класс регистрирует поставщиков данных через {@link Tracker},
 * которые затем опрашиваются сервером и отправляются клиенту по соответствующим
 * каналам {@link DebugSubscriptionType}.
 */
public interface DebugTrackable {

	/**
	 * Регистрирует поставщиков отладочных данных для данного объекта.
	 *
	 * @param world   серверный мир, в котором существует объект
	 * @param tracker трекер, принимающий регистрацию поставщиков данных
	 */
	void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker);

	/**
	 * Поставщик отладочных данных конкретного типа.
	 * <p>
	 * Вызывается сервером при необходимости отправить актуальное состояние клиенту.
	 * Возврат {@code null} означает, что данные в данный момент недоступны.
	 *
	 * @param <T> тип предоставляемых данных
	 */
	interface DebugDataSupplier<T> {

		@Nullable T get();
	}

	/**
	 * Трекер, принимающий регистрацию поставщиков отладочных данных.
	 * <p>
	 * Связывает тип подписки с конкретным поставщиком данных объекта.
	 */
	interface Tracker {

		/**
		 * Регистрирует поставщика данных для заданного типа подписки.
		 *
		 * @param type         тип отладочной подписки
		 * @param dataSupplier поставщик актуальных данных
		 */
		<T> void track(DebugSubscriptionType<T> type, DebugTrackable.DebugDataSupplier<T> dataSupplier);
	}
}
