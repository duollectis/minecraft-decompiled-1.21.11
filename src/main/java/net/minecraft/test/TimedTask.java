package net.minecraft.test;

import org.jspecify.annotations.Nullable;

/**
 * Задача с опциональной длительностью для использования в {@link TimedTaskRunner}.
 * Если {@code duration} задана — после выполнения задачи проверяется,
 * что прошло ровно столько тиков, сколько указано.
 */
class TimedTask {

	public final @Nullable Long duration;
	public final Runnable task;

	private TimedTask(@Nullable Long duration, Runnable task) {
		this.duration = duration;
		this.task = task;
	}

	static TimedTask create(Runnable task) {
		return new TimedTask(null, task);
	}

	static TimedTask create(long duration, Runnable task) {
		return new TimedTask(duration, task);
	}
}
