package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenBrain;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * Задача мозга Вардена, реализующая анимацию и логику обнюхивания.
 * По завершении увеличивает гнев к ближайшей атакуемой цели в радиусе и обновляет позицию помехи.
 */
public class SniffTask<E extends WardenEntity> extends MultiTickTask<E> {

	private static final double HORIZONTAL_RADIUS = 6.0;
	private static final double VERTICAL_RADIUS = 20.0;
	private static final float SNIFF_SOUND_VOLUME = 5.0F;
	private static final float SNIFF_SOUND_PITCH = 1.0F;

	public SniffTask(int runTime) {
		super(
				ImmutableMap.of(
						MemoryModuleType.IS_SNIFFING,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.NEAREST_ATTACKABLE,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.DISTURBANCE_LOCATION,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.SNIFF_COOLDOWN,
						MemoryModuleState.REGISTERED
				),
				runTime
		);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
		return true;
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		entity.playSound(SoundEvents.ENTITY_WARDEN_SNIFF, SNIFF_SOUND_VOLUME, SNIFF_SOUND_PITCH);
	}

	@Override
	protected void finishRunning(ServerWorld world, E entity, long time) {
		if (entity.isInPose(EntityPose.SNIFFING)) {
			entity.setPose(EntityPose.STANDING);
		}

		entity.getBrain().forget(MemoryModuleType.IS_SNIFFING);
		entity.getBrain()
		      .getOptionalRegisteredMemory(MemoryModuleType.NEAREST_ATTACKABLE)
		      .filter(entity::isValidTarget)
		      .ifPresent(target -> {
			      if (entity.isInRange(target, HORIZONTAL_RADIUS, VERTICAL_RADIUS)) {
				      entity.increaseAngerAt(target);
			      }

			      if (!entity.getBrain().hasMemoryModule(MemoryModuleType.DISTURBANCE_LOCATION)) {
				      WardenBrain.lookAtDisturbance(entity, target.getBlockPos());
			      }
		      });
	}
}
