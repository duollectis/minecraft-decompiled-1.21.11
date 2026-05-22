package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.LongJumpUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Задача мозга, реализующая дальний прыжок моба к случайной цели в заданном радиусе.
 * Перебирает кандидатов по весу, проверяет проходимость и вычисляет вектор прыжка через {@link LongJumpUtil}.
 */
public class LongJumpTask<E extends MobEntity> extends MultiTickTask<E> {

	protected static final int MAX_TARGET_SEARCH_TIME = 20;
	private static final int JUMP_WINDUP_TIME = 40;
	protected static final int PATHING_DISTANCE = 8;
	private static final int RUN_TIME = 200;
	private static final List<Integer> JUMP_ANGLES = new ArrayList<>(List.of(65, 70, 75, 80));

	private final UniformIntProvider cooldownRange;
	protected final int verticalRange;
	protected final int horizontalRange;
	protected final float maxRange;
	protected List<LongJumpTask.Target> potentialTargets = new ArrayList<>();
	protected Optional<Vec3d> startPos = Optional.empty();
	protected @Nullable Vec3d currentTarget;
	protected int targetSearchTime;
	protected long targetPickedTime;
	private final Function<E, SoundEvent> entityToSound;
	private final BiPredicate<E, BlockPos> jumpToPredicate;

	public LongJumpTask(
			UniformIntProvider cooldownRange,
			int verticalRange,
			int horizontalRange,
			float maxRange,
			Function<E, SoundEvent> entityToSound
	) {
		this(cooldownRange, verticalRange, horizontalRange, maxRange, entityToSound, LongJumpTask::shouldJumpTo);
	}

	public LongJumpTask(
			UniformIntProvider cooldownRange,
			int verticalRange,
			int horizontalRange,
			float maxRange,
			Function<E, SoundEvent> entityToSound,
			BiPredicate<E, BlockPos> jumpToPredicate
	) {
		super(
				ImmutableMap.of(
						MemoryModuleType.LOOK_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.LONG_JUMP_COOLING_DOWN,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.LONG_JUMP_MID_JUMP,
						MemoryModuleState.VALUE_ABSENT
				),
				RUN_TIME
		);
		this.cooldownRange = cooldownRange;
		this.verticalRange = verticalRange;
		this.horizontalRange = horizontalRange;
		this.maxRange = maxRange;
		this.entityToSound = entityToSound;
		this.jumpToPredicate = jumpToPredicate;
	}

	public static <E extends MobEntity> boolean shouldJumpTo(E entity, BlockPos pos) {
		World world = entity.getEntityWorld();
		return world.getBlockState(pos.down()).isOpaqueFullCube()
				&& entity.getPathfindingPenalty(LandPathNodeMaker.getLandNodeType(entity, pos)) == 0.0F;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, MobEntity entity) {
		boolean canJump = entity.isOnGround()
				&& !entity.isTouchingWater()
				&& !entity.isInLava()
				&& !world.getBlockState(entity.getBlockPos()).isOf(Blocks.HONEY_BLOCK);

		if (!canJump) {
			entity.getBrain().remember(MemoryModuleType.LONG_JUMP_COOLING_DOWN, cooldownRange.get(world.random) / 2);
		}

		return canJump;
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, MobEntity entity, long time) {
		boolean stillSearching = startPos.isPresent()
				&& startPos.get().equals(entity.getEntityPos())
				&& targetSearchTime > 0
				&& !entity.isTouchingWater()
				&& (currentTarget != null || !potentialTargets.isEmpty());

		if (!stillSearching && entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
			entity.getBrain().remember(MemoryModuleType.LONG_JUMP_COOLING_DOWN, cooldownRange.get(world.random) / 2);
			entity.getBrain().forget(MemoryModuleType.LOOK_TARGET);
		}

		return stillSearching;
	}

	@Override
	protected void run(ServerWorld world, E entity, long time) {
		currentTarget = null;
		targetSearchTime = MAX_TARGET_SEARCH_TIME;
		startPos = Optional.of(entity.getEntityPos());
		BlockPos origin = entity.getBlockPos();
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		potentialTargets = BlockPos.stream(
				                          x - horizontalRange,
				                          y - verticalRange,
				                          z - horizontalRange,
				                          x + horizontalRange,
				                          y + verticalRange,
				                          z + horizontalRange
		                          )
		                          .filter(pos -> !pos.equals(origin))
		                          .map(pos -> new LongJumpTask.Target(
				                          pos.toImmutable(),
				                          MathHelper.ceil(origin.getSquaredDistance(pos))
		                          ))
		                          .collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	protected void keepRunning(ServerWorld world, E entity, long time) {
		if (currentTarget != null) {
			if (time - targetPickedTime >= JUMP_WINDUP_TIME) {
				entity.setYaw(entity.bodyYaw);
				entity.setNoDrag(true);
				double length = currentTarget.length();
				double boostedLength = length + entity.getJumpBoostVelocityModifier();
				entity.setVelocity(currentTarget.multiply(boostedLength / length));
				entity.getBrain().remember(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
				world.playSoundFromEntity(null, entity, entityToSound.apply(entity), SoundCategory.NEUTRAL, 1.0F, 1.0F);
			}
		} else {
			targetSearchTime--;
			pickTarget(world, entity, time);
		}
	}

	protected void pickTarget(ServerWorld world, E entity, long time) {
		while (!potentialTargets.isEmpty()) {
			Optional<LongJumpTask.Target> candidate = removeRandomTarget(world);

			if (candidate.isEmpty()) {
				continue;
			}

			BlockPos targetPos = candidate.get().pos();

			if (!canJumpTo(world, entity, targetPos)) {
				continue;
			}

			Vec3d targetCenter = Vec3d.ofCenter(targetPos);
			Vec3d jumpVelocity = getJumpingVelocity(entity, targetCenter);

			if (jumpVelocity == null) {
				continue;
			}

			entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new BlockPosLookTarget(targetPos));
			Path path = entity.getNavigation().findPathTo(targetPos, 0, PATHING_DISTANCE);

			if (path == null || !path.reachesTarget()) {
				currentTarget = jumpVelocity;
				targetPickedTime = time;
				return;
			}
		}
	}

	protected Optional<LongJumpTask.Target> removeRandomTarget(ServerWorld world) {
		Optional<LongJumpTask.Target> candidate = Weighting.getRandom(world.random, potentialTargets, LongJumpTask.Target::weight);
		candidate.ifPresent(potentialTargets::remove);
		return candidate;
	}

	private boolean canJumpTo(ServerWorld world, E entity, BlockPos pos) {
		BlockPos origin = entity.getBlockPos();
		return (origin.getX() != pos.getX() || origin.getZ() != pos.getZ())
				&& jumpToPredicate.test(entity, pos);
	}

	protected @Nullable Vec3d getJumpingVelocity(MobEntity entity, Vec3d targetPos) {
		List<Integer> angles = new ArrayList<>(JUMP_ANGLES);
		Collections.shuffle(angles);
		float jumpStrength = (float) (entity.getAttributeValue(EntityAttributes.JUMP_STRENGTH) * maxRange);

		for (int angle : angles) {
			Optional<Vec3d> velocity = LongJumpUtil.getJumpingVelocity(entity, targetPos, jumpStrength, angle, true);

			if (velocity.isPresent()) {
				return velocity.get();
			}
		}

		return null;
	}

	public record Target(BlockPos pos, int weight) {
	}
}
