package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.intprovider.UniformIntProvider;

/**
 * Задача мозга, управляющая фазой полёта при прыжке (LONG_JUMPING).
 * При приземлении гасит горизонтальную скорость и воспроизводит звук приземления.
 */
public class LeapingChargeTask extends MultiTickTask<MobEntity> {

	public static final int RUN_TIME = 100;
	private static final float LANDING_VELOCITY_DAMPEN_XZ = 0.1F;
	private static final float LANDING_SOUND_VOLUME = 2.0F;

	private final UniformIntProvider cooldownRange;
	private final SoundEvent sound;

	public LeapingChargeTask(UniformIntProvider cooldownRange, SoundEvent sound) {
		super(
				ImmutableMap.of(
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.LONG_JUMP_MID_JUMP,
						MemoryModuleState.VALUE_PRESENT
				), RUN_TIME
		);
		this.cooldownRange = cooldownRange;
		this.sound = sound;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, MobEntity entity, long time) {
		return !entity.isOnGround();
	}

	@Override
	protected void run(ServerWorld world, MobEntity entity, long time) {
		entity.setNoDrag(true);
		entity.setPose(EntityPose.LONG_JUMPING);
	}

	@Override
	protected void finishRunning(ServerWorld world, MobEntity entity, long time) {
		if (entity.isOnGround()) {
			entity.setVelocity(entity.getVelocity().multiply(LANDING_VELOCITY_DAMPEN_XZ, 1.0, LANDING_VELOCITY_DAMPEN_XZ));
			world.playSoundFromEntity(null, entity, sound, SoundCategory.NEUTRAL, LANDING_SOUND_VOLUME, 1.0F);
		}

		entity.setNoDrag(false);
		entity.setPose(EntityPose.STANDING);
		entity.getBrain().forget(MemoryModuleType.LONG_JUMP_MID_JUMP);
		entity.getBrain().remember(MemoryModuleType.LONG_JUMP_COOLING_DOWN, cooldownRange.get(world.random));
	}
}
