package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * {@code PlayDeadTask}.
 */
public class PlayDeadTask extends MultiTickTask<AxolotlEntity> {

	public PlayDeadTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.PLAY_DEAD_TICKS,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.HURT_BY_ENTITY,
						MemoryModuleState.VALUE_PRESENT
				),
				200
		);
	}

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param axolotlEntity axolotl entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, AxolotlEntity axolotlEntity) {
		return axolotlEntity.isTouchingWater();
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param axolotlEntity axolotl entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, AxolotlEntity axolotlEntity, long l) {
		return axolotlEntity.isTouchingWater() && axolotlEntity
				.getBrain()
				.hasMemoryModule(MemoryModuleType.PLAY_DEAD_TICKS);
	}

	/**
	 * Run.
	 *
	 * @param serverWorld server world
	 * @param axolotlEntity axolotl entity
	 * @param l l
	 */
	protected void run(ServerWorld serverWorld, AxolotlEntity axolotlEntity, long l) {
		Brain<AxolotlEntity> brain = axolotlEntity.getBrain();
		brain.forget(MemoryModuleType.WALK_TARGET);
		brain.forget(MemoryModuleType.LOOK_TARGET);
		axolotlEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
	}
}
