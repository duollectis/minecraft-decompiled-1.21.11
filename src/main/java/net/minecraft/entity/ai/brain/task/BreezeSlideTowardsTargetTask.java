package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

/**
 * Задача мозга бриза, управляющая скольжением к цели или от неё.
 * При слишком близком расстоянии убегает; иначе перемещается за спину цели или в средний диапазон.
 */
public class BreezeSlideTowardsTargetTask extends MultiTickTask<BreezeEntity> {

	private static final float SLIDE_WALK_SPEED = 0.6F;
	private static final int SLIDE_COMPLETION_RANGE = 1;
	private static final int FLEE_HORIZONTAL_RANGE = 5;
	private static final int FLEE_VERTICAL_RANGE = 5;
	private static final double MEDIUM_RANGE_MIN = 4.0;
	private static final double MEDIUM_RANGE_MAX = 8.0;

	public BreezeSlideTowardsTargetTask() {
		super(
				Map.of(
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_JUMP_COOLDOWN,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_SHOOT,
						MemoryModuleState.VALUE_ABSENT
				)
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, BreezeEntity entity) {
		return entity.isOnGround()
				&& !entity.isTouchingWater()
				&& entity.getPose() == EntityPose.STANDING;
	}

	@Override
	protected void run(ServerWorld world, BreezeEntity entity, long time) {
		LivingEntity target = entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET)
				.orElse(null);
		if (target == null) {
			return;
		}

		Vec3d destination = null;
		if (entity.isWithinShortRange(target.getEntityPos())) {
			Vec3d fleePos = NoPenaltyTargeting.findFrom(entity, FLEE_HORIZONTAL_RANGE, FLEE_VERTICAL_RANGE, target.getEntityPos());
			if (fleePos != null
					&& BreezeMovementUtil.canMoveTo(entity, fleePos)
					&& target.squaredDistanceTo(fleePos.x, fleePos.y, fleePos.z) > target.squaredDistanceTo(entity)) {
				destination = fleePos;
			}
		}

		if (destination == null) {
			destination = entity.getRandom().nextBoolean()
					? BreezeMovementUtil.getRandomPosBehindTarget(target, entity.getRandom())
					: getRandomPosInMediumRange(entity, target);
		}

		entity.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(BlockPos.ofFloored(destination), SLIDE_WALK_SPEED, SLIDE_COMPLETION_RANGE));
	}

	private static Vec3d getRandomPosInMediumRange(BreezeEntity breeze, LivingEntity target) {
		Vec3d toTarget = target.getEntityPos().subtract(breeze.getEntityPos());
		double distance = toTarget.length() - MathHelper.lerp(breeze.getRandom().nextDouble(), MEDIUM_RANGE_MIN, MEDIUM_RANGE_MAX);
		Vec3d offset = toTarget.normalize().multiply(distance, distance, distance);
		return breeze.getEntityPos().add(offset);
	}
}
