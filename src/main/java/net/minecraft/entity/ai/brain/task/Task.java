package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Базовый интерфейс задачи мозга сущности.
 * Задача может быть запущена, тикаться и остановлена в рамках цикла обновления {@link net.minecraft.entity.ai.brain.Brain}.
 *
 * @param <E> тип сущности, для которой выполняется задача
 */
public interface Task<E extends LivingEntity> {

	MultiTickTask.Status getStatus();

	boolean tryStarting(ServerWorld world, E entity, long time);

	void tick(ServerWorld world, E entity, long time);

	void stop(ServerWorld world, E entity, long time);

	String getName();
}
