package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
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
 * Задача мозга, управляющая навигацией моба к цели из памяти {@code WALK_TARGET}.
 * Отслеживает путь, обновляет его при смещении цели и фиксирует недостижимость
 * через память {@code CANT_REACH_WALK_TARGET_SINCE}.
 */
public class MoveToTargetTask extends MultiTickTask<MobEntity> {

	private static final int MAX_UPDATE_COUNTDOWN = 40;
	private static final double LOOK_TARGET_MOVED_DIST_SQ = 4.0;
	private static final int FALLBACK_SEARCH_RADIUS = 10;
	private static final int FALLBACK_SEARCH_HEIGHT = 7;
	private static final float FALLBACK_HALF_PI = (float) (Math.PI / 2);

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

	@Override
	protected boolean shouldRun(ServerWorld world, MobEntity entity) {
		if (pathUpdateCountdownTicks > 0) {
			pathUpdateCountdownTicks--;
			return false;
		}

		Brain<?> brain = entity.getBrain();
		WalkTarget walkTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).get();
		boolean reached = hasReached(entity, walkTarget);

		if (!reached && hasFinishedPath(entity, walkTarget, world.getTime())) {
			lookTargetPos = walkTarget.getLookTarget().getBlockPos();
			return true;
		}

		brain.forget(MemoryModuleType.WALK_TARGET);

		if (reached) {
			brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		}

		return false;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, MobEntity entity, long time) {
		if (path == null || lookTargetPos == null) {
			return false;
		}

		Optional<WalkTarget> walkTargetOpt = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET);
		EntityNavigation navigation = entity.getNavigation();
		boolean isSpectator = walkTargetOpt.map(MoveToTargetTask::isTargetSpectator).orElse(false);

		return !navigation.isIdle()
				&& walkTargetOpt.isPresent()
				&& !hasReached(entity, walkTargetOpt.get())
				&& !isSpectator;
	}

	@Override
	protected void finishRunning(ServerWorld world, MobEntity entity, long time) {
		if (entity.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)
				&& !hasReached(entity, entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).get())
				&& entity.getNavigation().isNearPathStartPos()
		) {
			pathUpdateCountdownTicks = world.getRandom().nextInt(MAX_UPDATE_COUNTDOWN);
		}

		entity.getNavigation().stop();
		entity.getBrain().forget(MemoryModuleType.WALK_TARGET);
		entity.getBrain().forget(MemoryModuleType.PATH);
		path = null;
	}

	@Override
	protected void run(ServerWorld world, MobEntity entity, long time) {
		entity.getBrain().remember(MemoryModuleType.PATH, path);
		entity.getNavigation().startMovingAlong(path, speed);
	}

	@Override
	protected void keepRunning(ServerWorld world, MobEntity entity, long time) {
		Path currentPath = entity.getNavigation().getCurrentPath();
		Brain<?> brain = entity.getBrain();

		if (path != currentPath) {
			path = currentPath;
			brain.remember(MemoryModuleType.PATH, currentPath);
		}

		if (currentPath == null || lookTargetPos == null) {
			return;
		}

		WalkTarget walkTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).get();
		boolean targetMoved = walkTarget.getLookTarget().getBlockPos().getSquaredDistance(lookTargetPos)
				> LOOK_TARGET_MOVED_DIST_SQ;

		if (targetMoved && hasFinishedPath(entity, walkTarget, world.getTime())) {
			lookTargetPos = walkTarget.getLookTarget().getBlockPos();
			run(world, entity, time);
		}
	}

	private boolean hasFinishedPath(MobEntity entity, WalkTarget walkTarget, long time) {
		BlockPos targetPos = walkTarget.getLookTarget().getBlockPos();
		path = entity.getNavigation().findPathTo(targetPos, 0);
		speed = walkTarget.getSpeed();
		Brain<?> brain = entity.getBrain();

		if (hasReached(entity, walkTarget)) {
			brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
			return false;
		}

		boolean pathReachesTarget = path != null && path.reachesTarget();

		if (pathReachesTarget) {
			brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		} else if (!brain.hasMemoryModule(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
			brain.remember(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
		}

		if (path != null) {
			return true;
		}

		Vec3d fallbackPos = NoPenaltyTargeting.findTo(
				(PathAwareEntity) entity,
				FALLBACK_SEARCH_RADIUS,
				FALLBACK_SEARCH_HEIGHT,
				Vec3d.ofBottomCenter(targetPos),
				FALLBACK_HALF_PI
		);

		if (fallbackPos == null) {
			return false;
		}

		path = entity.getNavigation().findPathTo(fallbackPos.x, fallbackPos.y, fallbackPos.z, 0);
		return path != null;
	}

	private boolean hasReached(MobEntity entity, WalkTarget walkTarget) {
		return walkTarget.getLookTarget().getBlockPos().getManhattanDistance(entity.getBlockPos())
				<= walkTarget.getCompletionRange();
	}

	private static boolean isTargetSpectator(WalkTarget target) {
		return target.getLookTarget() instanceof EntityLookTarget entityLookTarget
				&& entityLookTarget.getEntity().isSpectator();
	}
}
