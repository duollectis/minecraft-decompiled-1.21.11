package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга, блокирующая выполнение других задач на случайный промежуток времени
 * в диапазоне [{@code minRunTime}, {@code maxRunTime}] тиков.
 */
public class WaitTask implements Task<LivingEntity> {

	private final int minRunTime;
	private final int maxRunTime;
	private MultiTickTask.Status status = MultiTickTask.Status.STOPPED;
	private long waitUntil;

	public WaitTask(int minRunTime, int maxRunTime) {
		this.minRunTime = minRunTime;
		this.maxRunTime = maxRunTime;
	}

	@Override
	public MultiTickTask.Status getStatus() {
		return status;
	}

	@Override
	public final boolean tryStarting(ServerWorld world, LivingEntity entity, long time) {
		status = MultiTickTask.Status.RUNNING;
		int duration = minRunTime + world.getRandom().nextInt(maxRunTime + 1 - minRunTime);
		waitUntil = time + duration;
		return true;
	}

	@Override
	public final void tick(ServerWorld world, LivingEntity entity, long time) {
		if (time > waitUntil) {
			stop(world, entity, time);
		}
	}

	@Override
	public final void stop(ServerWorld world, LivingEntity entity, long time) {
		status = MultiTickTask.Status.STOPPED;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
}
