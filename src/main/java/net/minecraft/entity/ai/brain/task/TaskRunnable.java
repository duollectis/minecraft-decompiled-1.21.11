package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * {@code TaskRunnable}.
 */
public interface TaskRunnable<E extends LivingEntity> {

	boolean trigger(ServerWorld world, E entity, long time);
}
