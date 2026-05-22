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
 * Задача мозга стража, запускающая анимацию закапывания и удаляющая сущность из мира.
 * При нахождении в воздухе или жидкости воспроизводит звук возбуждения и немедленно завершается.
 */
public class DigTask<E extends WardenEntity> extends MultiTickTask<E> {

	private static final float SOUND_VOLUME = 5.0F;
	private static final float SOUND_PITCH = 1.0F;

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

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return entity.getRemovalReason() == null;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, E entity) {
		return entity.isOnGround() || entity.isTouchingWater() || entity.isInLava();
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		if (entity.isOnGround()) {
			entity.setPose(EntityPose.DIGGING);
			entity.playSound(SoundEvents.ENTITY_WARDEN_DIG, SOUND_VOLUME, SOUND_PITCH);
		} else {
			entity.playSound(SoundEvents.ENTITY_WARDEN_AGITATED, SOUND_VOLUME, SOUND_PITCH);
			finishRunning(world, entity, time);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, E entity, long time) {
		if (entity.getRemovalReason() == null) {
			entity.remove(Entity.RemovalReason.DISCARDED);
		}
	}
}
