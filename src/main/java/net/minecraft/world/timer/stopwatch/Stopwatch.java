package net.minecraft.world.timer.stopwatch;

/**
 * Иммутабельный секундомер, отсчитывающий время от момента создания.
 * Хранит время создания и накопленное время из предыдущих сессий,
 * что позволяет корректно восстанавливать состояние после перезагрузки сервера.
 *
 * @param creationTime           системное время создания секундомера в миллисекундах
 * @param accumulatedElapsedTime накопленное время из предыдущих сессий в миллисекундах
 */
public record Stopwatch(long creationTime, long accumulatedElapsedTime) {

	/**
	 * Создаёт новый секундомер с нулевым накопленным временем.
	 *
	 * @param creationTimeMs системное время создания в миллисекундах
	 */
	public Stopwatch(long creationTimeMs) {
		this(creationTimeMs, 0L);
	}

	/**
	 * Возвращает суммарное прошедшее время с момента создания секундомера.
	 *
	 * @param currentTimeMs текущее системное время в миллисекундах
	 * @return прошедшее время в миллисекундах
	 */
	public long getElapsedTimeMs(long currentTimeMs) {
		return accumulatedElapsedTime + (currentTimeMs - creationTime);
	}

	/**
	 * Возвращает суммарное прошедшее время в секундах.
	 *
	 * @param currentTimeMs текущее системное время в миллисекундах
	 * @return прошедшее время в секундах
	 */
	public double getElapsedTimeSeconds(long currentTimeMs) {
		return getElapsedTimeMs(currentTimeMs) / 1000.0;
	}
}
