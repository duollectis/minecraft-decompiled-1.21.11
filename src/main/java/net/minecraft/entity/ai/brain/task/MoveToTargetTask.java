package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * {@code MoveToTargetTask}.
 */
public class MoveToTargetTask extends MultiTickTask<MobEntity> {

	private static final int MAX_UPDATE_COUNTDOWN = 40;
	private int pathUpdateCountdownTicks;
	private @Nullable Path path;
	private @Nullable BlockPos lookTargetPos;
	private float speed;

	public MoveToTargetTask() {
		this(150, 250);
	}

	public MoveToTargetTask(int minRunTime, int maxRunTime) {
		super(
				ImmutableMap.of(
						MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.PATH,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_PRESENT
				),
				minRunTime,
				maxRunTime
		);
	}

	/**
	 * Определяет, следует ли run.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldRun(ServerWorld serverWorld, MobEntity mobEntity) {
		if (this.pathUpdateCountdownTicks > 0) {
			this.pathUpdateCountdownTicks--;
			return false;
		}
		else {
			Brain<?> brain = mobEntity.getBrain();
			WalkTarget walkTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).get();
			boolean bl = this.hasReached(mobEntity, walkTarget);
			if (!bl && this.hasFinishedPath(mobEntity, walkTarget, serverWorld.getTime())) {
				this.lookTargetPos = walkTarget.getLookTarget().getBlockPos();
				return true;
			}
			else {
				brain.forget(MemoryModuleType.WALK_TARGET);
				if (bl) {
					brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
				}

				return false;
			}
		}
	}

	/**
	 * Определяет, следует ли keep running.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 * @param l l
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldKeepRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
		if (this.path != null && this.lookTargetPos != null) {
			Optional<WalkTarget>
					optional =
					mobEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET);
			boolean bl = optional.map(MoveToTargetTask::isTargetSpectator).orElse(false);
			EntityNavigation entityNavigation = mobEntity.getNavigation();
			return !entityNavigation.isIdle() && optional.isPresent() && !this.hasReached(mobEntity, optional.get())
					&& !bl;
		}
		else {
			return false;
		}
	}

	/**
	 * Finish running.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 * @param l l
	 */
	protected void finishRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
		if (mobEntity.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)
				&& !this.hasReached(
				mobEntity,
				mobEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).get()
		)
				&& mobEntity.getNavigation().isNearPathStartPos()) {
			this.pathUpdateCountdownTicks = serverWorld.getRandom().nextInt(40);
		}

		mobEntity.getNavigation().stop();
		mobEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);
		mobEntity.getBrain().forget(MemoryModuleType.PATH);
		this.path = null;
	}

	/**
	 * Run.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 * @param l l
	 */
	protected void run(ServerWorld serverWorld, MobEntity mobEntity, long l) {
		mobEntity.getBrain().remember(MemoryModuleType.PATH, this.path);
		mobEntity.getNavigation().startMovingAlong(this.path, this.speed);
	}

	/**
	 * Keep running.
	 *
	 * @param serverWorld server world
	 * @param mobEntity mob entity
	 * @param l l
	 */
	protected void keepRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
		Path path = mobEntity.getNavigation().getCurrentPath();
		Brain<?> brain = mobEntity.getBrain();
		if (this.path != path) {
			this.path = path;
			brain.remember(MemoryModuleType.PATH, path);
		}

		if (path != null && this.lookTargetPos != null) {
			WalkTarget walkTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).get();
			if (walkTarget.getLookTarget().getBlockPos().getSquaredDistance(this.lookTargetPos) > 4.0
					&& this.hasFinishedPath(mobEntity, walkTarget, serverWorld.getTime())) {
				this.lookTargetPos = walkTarget.getLookTarget().getBlockPos();
				this.run(serverWorld, mobEntity, l);
			}
		}
	}

	private boolean hasFinishedPath(MobEntity entity, WalkTarget walkTarget, long time) {
		BlockPos blockPos = walkTarget.getLookTarget().getBlockPos();
		this.path = entity.getNavigation().findPathTo(blockPos, 0);
		this.speed = walkTarget.getSpeed();
		Brain<?> brain = entity.getBrain();
		if (this.hasReached(entity, walkTarget)) {
			brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		}
		else {
			boolean bl = this.path != null && this.path.reachesTarget();
			if (bl) {
				brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
			}
			else if (!brain.hasMemoryModule(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
				brain.remember(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
			}

			if (this.path != null) {
				return true;
			}

			Vec3d
					vec3d =
					NoPenaltyTargeting.findTo(
							(PathAwareEntity) entity,
							10,
							7,
							Vec3d.ofBottomCenter(blockPos),
							(float) (Math.PI / 2)
					);
			if (vec3d != null) {
				this.path = entity.getNavigation().findPathTo(vec3d.x, vec3d.y, vec3d.z, 0);
				return this.path != null;
			}
		}

		return false;
	}

	private boolean hasReached(MobEntity entity, WalkTarget walkTarget) {
		return walkTarget.getLookTarget().getBlockPos().getManhattanDistance(entity.getBlockPos())
				<= walkTarget.getCompletionRange();
	}

	private static boolean isTargetSpectator(WalkTarget target) {
		return target.getLookTarget() instanceof EntityLookTarget entityLookTarget ? entityLookTarget
		                                                                             .getEntity()
		                                                                             .isSpectator() : false;
	}
}
