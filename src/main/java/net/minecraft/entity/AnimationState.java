package net.minecraft.entity;

import java.util.function.Consumer;

/**
 * Хранит состояние анимации сущности — запущена она или нет.
 * Использует {@code Integer.MIN_VALUE} как сигнальное значение «остановлена»,
 * что позволяет избежать отдельного булевого флага.
 */
public class AnimationState {

	private static final int STOPPED = Integer.MIN_VALUE;

	private int startTick = STOPPED;

	public void start(int tick) {
		startTick = tick;
	}

	public void startIfNotRunning(int tick) {
		if (isRunning()) {
			return;
		}

		start(tick);
	}

	public void setRunning(boolean running, int tick) {
		if (running) {
			startIfNotRunning(tick);
		} else {
			stop();
		}
	}

	public void stop() {
		startTick = STOPPED;
	}

	public void run(Consumer<AnimationState> consumer) {
		if (isRunning()) {
			consumer.accept(this);
		}
	}

	/**
	 * Сдвигает стартовый тик назад, имитируя пропуск {@code ticks} тиков
	 * с учётом множителя скорости воспроизведения.
	 *
	 * @param ticks           количество тиков для пропуска
	 * @param speedMultiplier множитель скорости анимации
	 */
	public void skip(int ticks, float speedMultiplier) {
		if (isRunning()) {
			startTick -= (int) (ticks * speedMultiplier);
		}
	}

	/**
	 * Возвращает время в миллисекундах, прошедшее с момента запуска анимации.
	 * Один тик = 50 мс (20 TPS).
	 *
	 * @param age текущий возраст сущности в тиках (может быть дробным для интерполяции)
	 * @return время в миллисекундах
	 */
	public long getTimeInMilliseconds(float age) {
		float elapsed = age - startTick;
		return (long) (elapsed * 50.0F);
	}

	public boolean isRunning() {
		return startTick != STOPPED;
	}

	public void copyFrom(AnimationState state) {
		startTick = state.startTick;
	}
}
