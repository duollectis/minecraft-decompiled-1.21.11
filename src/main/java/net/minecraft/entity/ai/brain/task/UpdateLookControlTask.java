package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга, направляющей взгляд существа на цель из памяти {@code LOOK_TARGET}.
 * Продолжает работу пока цель видима; по завершении забывает цель взгляда.
 */
public class UpdateLookControlTask extends MultiTickTask<MobEntity> {

	public UpdateLookControlTask(int minRunTime, int maxRunTime) {
		super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleState.VALUE_PRESENT), minRunTime, maxRunTime);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, MobEntity entity, long time) {
		return entity.getBrain()
		             .getOptionalRegisteredMemory(MemoryModuleType.LOOK_TARGET)
		             .filter(lookTarget -> lookTarget.isSeenBy(entity))
		             .isPresent();
	}

	@Override
	protected void finishRunning(ServerWorld world, MobEntity entity, long time) {
		entity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
	}

	@Override
	protected void keepRunning(ServerWorld world, MobEntity entity, long time) {
		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.LOOK_TARGET)
		      .ifPresent(lookTarget -> entity.getLookControl().lookAt(lookTarget.getPos()));
	}
}
