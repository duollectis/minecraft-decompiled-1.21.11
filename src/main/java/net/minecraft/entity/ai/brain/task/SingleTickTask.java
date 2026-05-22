package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга, выполняющаяся ровно один тик.
 * После успешного запуска через {@link #tryStarting} немедленно останавливается на следующем тике.
 *
 * @param <E> тип сущности
 */
public abstract class SingleTickTask<E extends LivingEntity> implements Task<E>, TaskRunnable<E> {

	private MultiTickTask.Status status = MultiTickTask.Status.STOPPED;

	@Override
	public final MultiTickTask.Status getStatus() {
		return status;
	}

	@Override
	public final boolean tryStarting(ServerWorld world, E entity, long time) {
		if (!trigger(world, entity, time)) {
			return false;
		}

		status = MultiTickTask.Status.RUNNING;
		return true;
	}

	@Override
	public final void tick(ServerWorld world, E entity, long time) {
		stop(world, entity, time);
	}

	@Override
	public final void stop(ServerWorld world, E entity, long time) {
		status = MultiTickTask.Status.STOPPED;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
}
