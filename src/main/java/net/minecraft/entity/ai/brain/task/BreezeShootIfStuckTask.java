package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;

import java.util.Map;

/**
 * Задача мозга бриза, принудительно активирующая стрельбу когда прыжок невозможен:
 * при езде на транспорте, нахождении в воде или под эффектом левитации.
 */
public class BreezeShootIfStuckTask extends MultiTickTask<BreezeEntity> {

	private static final long SHOOT_DURATION = 60L;

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

	@Override
	protected boolean shouldRun(ServerWorld world, BreezeEntity entity) {
		return entity.hasVehicle()
				|| entity.isTouchingWater()
				|| entity.getStatusEffect(StatusEffects.LEVITATION) != null;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, BreezeEntity entity, long time) {
		return false;
	}

	@Override
	protected void run(ServerWorld world, BreezeEntity entity, long time) {
		entity.getBrain().remember(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, SHOOT_DURATION);
	}
}
