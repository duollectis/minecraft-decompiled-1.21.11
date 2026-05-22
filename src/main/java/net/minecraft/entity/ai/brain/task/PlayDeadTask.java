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
 * Задача мозга аксолотля, реализующая притворство мёртвым в воде.
 * При запуске применяет эффект регенерации и сбрасывает цели движения и взгляда.
 */
public class PlayDeadTask extends MultiTickTask<AxolotlEntity> {

	private static final int REGENERATION_DURATION = 200;
	private static final int REGENERATION_AMPLIFIER = 0;

	public PlayDeadTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.PLAY_DEAD_TICKS,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.HURT_BY_ENTITY,
						MemoryModuleState.VALUE_PRESENT
				),
				REGENERATION_DURATION
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, AxolotlEntity entity) {
		return entity.isTouchingWater();
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, AxolotlEntity entity, long time) {
		return entity.isTouchingWater() && entity.getBrain().hasMemoryModule(MemoryModuleType.PLAY_DEAD_TICKS);
	}

	@Override
	protected void run(ServerWorld world, AxolotlEntity entity, long time) {
		Brain<AxolotlEntity> brain = entity.getBrain();
		brain.forget(MemoryModuleType.WALK_TARGET);
		brain.forget(MemoryModuleType.LOOK_TARGET);
		entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, REGENERATION_DURATION, REGENERATION_AMPLIFIER));
	}
}
