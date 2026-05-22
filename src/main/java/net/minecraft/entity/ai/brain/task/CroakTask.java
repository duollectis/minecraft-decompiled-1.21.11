package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Задача мозга лягушки, переводящая её в позу квакания на ограниченное время.
 * Активируется только на суше в позе стояния.
 */
public class CroakTask extends MultiTickTask<FrogEntity> {

	private static final int MAX_RUN_TICK = 60;
	private static final int RUN_TIME = 100;
	private int runningTicks;

	public CroakTask() {
		super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT), RUN_TIME);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, FrogEntity entity) {
		return entity.getPose() == EntityPose.STANDING;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, FrogEntity entity, long time) {
		return runningTicks < MAX_RUN_TICK;
	}

	@Override
	protected void run(ServerWorld world, FrogEntity entity, long time) {
		if (!entity.isInFluid()) {
			entity.setPose(EntityPose.CROAKING);
			runningTicks = 0;
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, FrogEntity entity, long time) {
		entity.setPose(EntityPose.STANDING);
	}

	@Override
	protected void keepRunning(ServerWorld world, FrogEntity entity, long time) {
		runningTicks++;
	}
}
