package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * {@code NoPenaltyTargeting}.
 */
public class NoPenaltyTargeting {

	/**
	 * Find.
	 *
	 * @param entity entity
	 * @param horizontalRange horizontal range
	 * @param verticalRange vertical range
	 *
	 * @return @Nullable Vec3d — 
	 */
	public static @Nullable Vec3d find(PathAwareEntity entity, int horizontalRange, int verticalRange) {
		boolean bl = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);
		return FuzzyPositions.guessBestPathTarget(
				entity, () -> {
					BlockPos blockPos = FuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange);
					return tryMake(entity, horizontalRange, bl, blockPos);
				}
		);
	}

	public static @Nullable Vec3d findTo(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			Vec3d end,
			double angleRange
	) {
		Vec3d vec3d = end.subtract(entity.getX(), entity.getY(), entity.getZ());
		boolean bl = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);
		return FuzzyPositions.guessBestPathTarget(
				entity, () -> {
					BlockPos
							blockPos =
							FuzzyPositions.localFuzz(
									entity.getRandom(),
									0.0,
									horizontalRange,
									verticalRange,
									0,
									vec3d.x,
									vec3d.z,
									angleRange
							);
					return blockPos == null ? null : tryMake(entity, horizontalRange, bl, blockPos);
				}
		);
	}

	public static @Nullable Vec3d findFrom(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			Vec3d start
	) {
		Vec3d vec3d = entity.getEntityPos().subtract(start);
		boolean bl = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);
		return FuzzyPositions.guessBestPathTarget(
				entity, () -> {
					BlockPos
							blockPos =
							FuzzyPositions.localFuzz(
									entity.getRandom(),
									0.0,
									horizontalRange,
									verticalRange,
									0,
									vec3d.x,
									vec3d.z,
									(float) (Math.PI / 2)
							);
					return blockPos == null ? null : tryMake(entity, horizontalRange, bl, blockPos);
				}
		);
	}

	private static @Nullable BlockPos tryMake(
			PathAwareEntity entity,
			int horizontalRange,
			boolean posTargetInRange,
			BlockPos fuzz
	) {
		BlockPos blockPos = FuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), fuzz);
		return !NavigationConditions.isHeightInvalid(blockPos, entity)
				       && !NavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, blockPos)
				       && !NavigationConditions.isInvalidPosition(entity.getNavigation(), blockPos)
				       && !NavigationConditions.hasPathfindingPenalty(entity, blockPos)
		       ? blockPos
		       : null;
	}
}
