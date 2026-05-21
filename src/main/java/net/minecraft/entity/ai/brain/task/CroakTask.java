package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * {@code CroakTask}.
 */
public class CroakTask extends MultiTickTask<FrogEntity> {

	private static final int MAX_RUN_TICK = 60;
	private static final int RUN_TIME = 100;
	private int runningTicks;

	public CroakTask() {
		super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT), 100);
	}

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param frogEntity frog entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, FrogEntity frogEntity) {
		return frogEntity.getPose() == EntityPose.STANDING;
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param frogEntity frog entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, FrogEntity frogEntity, long l) {
		return this.runningTicks < 60;
	}

	/**
	 * Run.
	 *
	 * @param serverWorld server world
	 * @param frogEntity frog entity
	 * @param l l
	 */
	protected void run(ServerWorld serverWorld, FrogEntity frogEntity, long l) {
		if (!frogEntity.isInFluid()) {
			frogEntity.setPose(EntityPose.CROAKING);
			this.runningTicks = 0;
		}
	}

	/**
	 * Finish running.
	 *
	 * @param serverWorld server world
	 * @param frogEntity frog entity
	 * @param l l
	 */
	protected void finishRunning(ServerWorld serverWorld, FrogEntity frogEntity, long l) {
		frogEntity.setPose(EntityPose.STANDING);
	}

	/**
	 * Keep running.
	 *
	 * @param serverWorld server world
	 * @param frogEntity frog entity
	 * @param l l
	 */
	protected void keepRunning(ServerWorld serverWorld, FrogEntity frogEntity, long l) {
		this.runningTicks++;
	}
}
