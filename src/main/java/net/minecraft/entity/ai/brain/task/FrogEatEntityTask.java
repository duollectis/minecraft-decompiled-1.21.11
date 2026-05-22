package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Задача мозга лягушки, реализующая многофазный процесс поедания добычи:
 * движение к цели → анимация захвата языком → анимация поедания.
 */
public class FrogEatEntityTask extends MultiTickTask<FrogEntity> {

	public static final int RUN_TIME = 100;
	public static final int CATCH_DURATION = 6;
	public static final int EAT_DURATION = 10;
	public static final int UNREACHABLE_TONGUE_TARGETS_START_TIME = 100;
	public static final int MAX_UNREACHABLE_TONGUE_TARGETS = 5;
	private static final float MAX_TONGUE_DISTANCE = 1.75F;
	private static final float PULL_VELOCITY_MULTIPLIER = 0.75F;
	private static final float APPROACH_SPEED = 2.0F;
	private static final float SOUND_VOLUME = 2.0F;
	private static final float SOUND_PITCH = 1.0F;

	private int eatTick;
	private int moveToTargetTick;
	private final SoundEvent tongueSound;
	private final SoundEvent eatSound;
	private Phase phase = Phase.DONE;

	public FrogEatEntityTask(SoundEvent tongueSound, SoundEvent eatSound) {
		super(
				ImmutableMap.of(
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.IS_PANICKING,
						MemoryModuleState.VALUE_ABSENT
				),
				RUN_TIME
		);
		this.tongueSound = tongueSound;
		this.eatSound = eatSound;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, FrogEntity entity) {
		LivingEntity prey = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get();
		boolean reachable = isTargetReachable(entity, prey);

		if (!reachable) {
			entity.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
			markTargetAsUnreachable(entity, prey);
		}

		return reachable && entity.getPose() != EntityPose.CROAKING && FrogEntity.isValidFrogFood(prey);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, FrogEntity entity, long time) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET)
				&& phase != Phase.DONE
				&& !entity.getBrain().hasMemoryModule(MemoryModuleType.IS_PANICKING);
	}

	@Override
	protected void run(ServerWorld world, FrogEntity entity, long time) {
		LivingEntity prey = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get();
		TargetUtil.lookAt(entity, prey);
		entity.setFrogTarget(prey);
		entity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(prey.getEntityPos(), APPROACH_SPEED, 0));
		moveToTargetTick = EAT_DURATION;
		phase = Phase.MOVE_TO_TARGET;
	}

	@Override
	protected void finishRunning(ServerWorld world, FrogEntity entity, long time) {
		entity.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
		entity.clearFrogTarget();
		entity.setPose(EntityPose.STANDING);
	}

	@Override
	protected void keepRunning(ServerWorld world, FrogEntity entity, long time) {
		LivingEntity prey = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).get();
		entity.setFrogTarget(prey);

		switch (phase) {
			case MOVE_TO_TARGET -> {
				if (prey.distanceTo(entity) < MAX_TONGUE_DISTANCE) {
					world.playSoundFromEntity(null, entity, tongueSound, SoundCategory.NEUTRAL, SOUND_VOLUME, SOUND_PITCH);
					entity.setPose(EntityPose.USING_TONGUE);
					prey.setVelocity(
							prey.getEntityPos()
									.relativize(entity.getEntityPos())
									.normalize()
									.multiply(PULL_VELOCITY_MULTIPLIER)
					);
					eatTick = 0;
					phase = Phase.CATCH_ANIMATION;
				} else if (moveToTargetTick <= 0) {
					entity.getBrain().remember(
							MemoryModuleType.WALK_TARGET,
							new WalkTarget(prey.getEntityPos(), APPROACH_SPEED, 0)
					);
					moveToTargetTick = EAT_DURATION;
				} else {
					moveToTargetTick--;
				}
			}
			case CATCH_ANIMATION -> {
				if (eatTick++ >= CATCH_DURATION) {
					phase = Phase.EAT_ANIMATION;
					eat(world, entity);
				}
			}
			case EAT_ANIMATION -> {
				if (eatTick >= EAT_DURATION) {
					phase = Phase.DONE;
				} else {
					eatTick++;
				}
			}
			case DONE -> {}
		}
	}

	private void eat(ServerWorld world, FrogEntity frog) {
		world.playSoundFromEntity(null, frog, eatSound, SoundCategory.NEUTRAL, SOUND_VOLUME, SOUND_PITCH);
		frog.getFrogTarget().ifPresent(prey -> {
			if (prey.isAlive()) {
				frog.tryAttack(world, prey);

				if (!prey.isAlive()) {
					prey.remove(Entity.RemovalReason.KILLED);
				}
			}
		});
	}

	private boolean isTargetReachable(FrogEntity entity, LivingEntity target) {
		Path path = entity.getNavigation().findPathTo(target, 0);
		return path != null && path.getManhattanDistanceFromTarget() < MAX_TONGUE_DISTANCE;
	}

	private void markTargetAsUnreachable(FrogEntity entity, LivingEntity target) {
		List<UUID> unreachable = entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS)
				.orElseGet(ArrayList::new);
		boolean notYetMarked = !unreachable.contains(target.getUuid());

		if (unreachable.size() == MAX_UNREACHABLE_TONGUE_TARGETS && notYetMarked) {
			unreachable.remove(0);
		}

		if (notYetMarked) {
			unreachable.add(target.getUuid());
		}

		entity.getBrain().remember(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS, unreachable, UNREACHABLE_TONGUE_TARGETS_START_TIME);
	}

	enum Phase {
		MOVE_TO_TARGET,
		CATCH_ANIMATION,
		EAT_ANIMATION,
		DONE
	}
}
