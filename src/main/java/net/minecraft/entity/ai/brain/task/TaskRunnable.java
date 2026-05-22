package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Функциональный интерфейс для запускаемой логики задачи мозга.
 * Используется как строительный блок в {@link TaskTriggerer} для создания однотиковых задач.
 *
 * @param <E> тип сущности
 */
public interface TaskRunnable<E extends LivingEntity> {

	boolean trigger(ServerWorld world, E entity, long time);
}
