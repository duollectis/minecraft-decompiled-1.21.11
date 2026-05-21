package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * {@code DigTask}.
 */
public class DigTask<E extends WardenEntity> extends MultiTickTask<E> {

	public DigTask(int duration) {
		super(
				ImmutableMap.of(
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT
				),
				duration
		);
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param wardenEntity warden entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, E wardenEntity, long l) {
		return wardenEntity.getRemovalReason() == null;
	}

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param wardenEntity warden entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, E wardenEntity) {
		return wardenEntity.isOnGround() || wardenEntity.isTouchingWater() || wardenEntity.isInLava();
	}

	/**
	 * Run.
	 *
	 * @param serverWorld server world
	 * @param wardenEntity warden entity
	 * @param l l
	 */
	protected void run(ServerWorld serverWorld, E wardenEntity, long l) {
		if (wardenEntity.isOnGround()) {
			wardenEntity.setPose(EntityPose.DIGGING);
			wardenEntity.playSound(SoundEvents.ENTITY_WARDEN_DIG, 5.0F, 1.0F);
		}
		else {
			wardenEntity.playSound(SoundEvents.ENTITY_WARDEN_AGITATED, 5.0F, 1.0F);
			this.finishRunning(serverWorld, wardenEntity, l);
		}
	}

	/**
	 * Finish running.
	 *
	 * @param serverWorld server world
	 * @param wardenEntity warden entity
	 * @param l l
	 */
	protected void finishRunning(ServerWorld serverWorld, E wardenEntity, long l) {
		if (wardenEntity.getRemovalReason() == null) {
			wardenEntity.remove(Entity.RemovalReason.DISCARDED);
		}
	}
}
