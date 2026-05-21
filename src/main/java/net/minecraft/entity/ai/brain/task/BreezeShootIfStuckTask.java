package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;

import java.util.Map;

/**
 * {@code BreezeShootIfStuckTask}.
 */
public class BreezeShootIfStuckTask extends MultiTickTask<BreezeEntity> {

	public BreezeShootIfStuckTask() {
		super(
				Map.of(
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.BREEZE_JUMP_INHALING,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_JUMP_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_SHOOT,
						MemoryModuleState.VALUE_ABSENT
				)
		);
	}

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param breezeEntity breeze entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, BreezeEntity breezeEntity) {
		return breezeEntity.hasVehicle() || breezeEntity.isTouchingWater()
				|| breezeEntity.getStatusEffect(StatusEffects.LEVITATION) != null;
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param breezeEntity breeze entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, BreezeEntity breezeEntity, long l) {
		return false;
	}

	/**
	 * Run.
	 *
	 * @param serverWorld server world
	 * @param breezeEntity breeze entity
	 * @param l l
	 */
	protected void run(ServerWorld serverWorld, BreezeEntity breezeEntity, long l) {
		breezeEntity.getBrain().remember(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 60L);
	}
}
