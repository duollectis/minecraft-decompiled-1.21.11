package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Задача мозга детёныша моба, заставляющая его прыгать на ближайшей кровати.
 * Прыгает случайное количество раз в диапазоне [{@code MIN_JUMPS}, {@code MIN_JUMPS + JUMP_COUNT_VARIANCE}).
 */
public class JumpInBedTask extends MultiTickTask<MobEntity> {

	private static final int MAX_TICKS_OUT_OF_BED = 100;
	private static final int MIN_JUMPS = 3;
	private static final int JUMP_COUNT_VARIANCE = 4;
	private static final int TICKS_BETWEEN_JUMPS = 5;

	private final float walkSpeed;
	private @Nullable BlockPos bedPos;
	private int ticksOutOfBedUntilStopped;
	private int jumpsRemaining;
	private int ticksToNextJump;

	public JumpInBedTask(float walkSpeed) {
		super(ImmutableMap.of(
				MemoryModuleType.NEAREST_BED,
				MemoryModuleState.VALUE_PRESENT,
				MemoryModuleType.WALK_TARGET,
				MemoryModuleState.VALUE_ABSENT
		));
		this.walkSpeed = walkSpeed;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, MobEntity entity) {
		return entity.isBaby() && shouldStartJumping(world, entity);
	}

	@Override
	protected void run(ServerWorld world, MobEntity entity, long time) {
		super.run(world, entity, time);
		getNearestBed(entity).ifPresent(pos -> {
			bedPos = pos;
			ticksOutOfBedUntilStopped = MAX_TICKS_OUT_OF_BED;
			jumpsRemaining = MIN_JUMPS + world.random.nextInt(JUMP_COUNT_VARIANCE);
			ticksToNextJump = 0;
			setWalkTarget(entity, pos);
		});
	}

	@Override
	protected void finishRunning(ServerWorld world, MobEntity entity, long time) {
		super.finishRunning(world, entity, time);
		bedPos = null;
		ticksOutOfBedUntilStopped = 0;
		jumpsRemaining = 0;
		ticksToNextJump = 0;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, MobEntity entity, long time) {
		return entity.isBaby()
				&& bedPos != null
				&& isBedAt(world, bedPos)
				&& !isBedGoneTooLong(world, entity)
				&& !isDoneJumping(world, entity);
	}

	@Override
	protected boolean isTimeLimitExceeded(long time) {
		return false;
	}

	@Override
	protected void keepRunning(ServerWorld world, MobEntity entity, long time) {
		if (!isAboveBed(world, entity)) {
			ticksOutOfBedUntilStopped--;
		} else if (ticksToNextJump > 0) {
			ticksToNextJump--;
		} else {
			if (isOnBed(world, entity)) {
				entity.getJumpControl().setActive();
				jumpsRemaining--;
				ticksToNextJump = TICKS_BETWEEN_JUMPS;
			}
		}
	}

	private void setWalkTarget(MobEntity mob, BlockPos pos) {
		mob.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, walkSpeed, 0));
	}

	private boolean shouldStartJumping(ServerWorld world, MobEntity mob) {
		return isAboveBed(world, mob) || getNearestBed(mob).isPresent();
	}

	private boolean isAboveBed(ServerWorld world, MobEntity mob) {
		BlockPos pos = mob.getBlockPos();
		return isBedAt(world, pos) || isBedAt(world, pos.down());
	}

	private boolean isOnBed(ServerWorld world, MobEntity mob) {
		return isBedAt(world, mob.getBlockPos());
	}

	private boolean isBedAt(ServerWorld world, BlockPos pos) {
		return world.getBlockState(pos).isIn(BlockTags.BEDS);
	}

	private Optional<BlockPos> getNearestBed(MobEntity mob) {
		return mob.getBrain().getOptionalRegisteredMemory(MemoryModuleType.NEAREST_BED);
	}

	private boolean isBedGoneTooLong(ServerWorld world, MobEntity mob) {
		return !isAboveBed(world, mob) && ticksOutOfBedUntilStopped <= 0;
	}

	private boolean isDoneJumping(ServerWorld world, MobEntity mob) {
		return isAboveBed(world, mob) && jumpsRemaining <= 0;
	}
}
