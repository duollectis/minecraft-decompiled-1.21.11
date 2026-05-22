package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * Задача мозга стража, запускающая анимацию появления из земли.
 * Требует наличия памяти {@code IS_EMERGING} и выполняется до её истечения.
 */
public class EmergeTask<E extends WardenEntity> extends MultiTickTask<E> {

	private static final float SOUND_VOLUME = 5.0F;
	private static final float SOUND_PITCH = 1.0F;

	public EmergeTask(int duration) {
		super(
				ImmutableMap.of(
						MemoryModuleType.IS_EMERGING,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED
				),
				duration
		);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return true;
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		entity.setPose(EntityPose.EMERGING);
		entity.playSound(SoundEvents.ENTITY_WARDEN_EMERGE, SOUND_VOLUME, SOUND_PITCH);
	}

	@Override
	protected void finishRunning(ServerWorld world, E entity, long time) {
		if (entity.isInPose(EntityPose.EMERGING)) {
			entity.setPose(EntityPose.STANDING);
		}
	}
}
