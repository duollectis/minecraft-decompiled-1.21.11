package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenBrain;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;

/**
 * Задача мозга Вардена, реализующая рёв с задержкой звука и увеличением гнева к цели.
 * После запуска устанавливает позу {@code ROARING}, через {@code SOUND_DELAY} тиков воспроизводит звук,
 * а по завершении переводит цель рёва в цель атаки.
 */
public class RoarTask extends MultiTickTask<WardenEntity> {

	private static final int SOUND_DELAY = 25;
	private static final int ANGER_INCREASE = 20;
	private static final float ROAR_SOUND_VOLUME = 3.0F;
	private static final float ROAR_SOUND_PITCH = 1.0F;

	public RoarTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.ROAR_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.ROAR_SOUND_COOLDOWN,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.ROAR_SOUND_DELAY,
						MemoryModuleState.REGISTERED
				),
				WardenBrain.ROAR_DURATION
		);
	}

	@Override
	protected void run(ServerWorld world, WardenEntity entity, long time) {
		Brain<WardenEntity> brain = entity.getBrain();
		brain.remember(MemoryModuleType.ROAR_SOUND_DELAY, Unit.INSTANCE, SOUND_DELAY);
		brain.forget(MemoryModuleType.WALK_TARGET);

		LivingEntity roarTarget = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ROAR_TARGET).get();
		TargetUtil.lookAt(entity, roarTarget);
		entity.setPose(EntityPose.ROARING);
		entity.increaseAngerAt(roarTarget, ANGER_INCREASE, false);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, WardenEntity entity, long time) {
		return true;
	}

	@Override
	protected void keepRunning(ServerWorld world, WardenEntity entity, long time) {
		Brain<WardenEntity> brain = entity.getBrain();
		boolean soundDelayExpired = !brain.hasMemoryModule(MemoryModuleType.ROAR_SOUND_DELAY);
		boolean soundNotOnCooldown = !brain.hasMemoryModule(MemoryModuleType.ROAR_SOUND_COOLDOWN);

		if (soundDelayExpired && soundNotOnCooldown) {
			brain.remember(
					MemoryModuleType.ROAR_SOUND_COOLDOWN,
					Unit.INSTANCE,
					WardenBrain.ROAR_DURATION - SOUND_DELAY
			);
			entity.playSound(SoundEvents.ENTITY_WARDEN_ROAR, ROAR_SOUND_VOLUME, ROAR_SOUND_PITCH);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, WardenEntity entity, long time) {
		if (entity.isInPose(EntityPose.ROARING)) {
			entity.setPose(EntityPose.STANDING);
		}

		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.ROAR_TARGET)
		      .ifPresent(entity::updateAttackTarget);
		entity.getBrain().forget(MemoryModuleType.ROAR_TARGET);
	}
}
