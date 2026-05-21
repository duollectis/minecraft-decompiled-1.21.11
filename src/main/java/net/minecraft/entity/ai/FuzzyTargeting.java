package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.function.ToDoubleFunction;

/**
 * {@code FuzzyTargeting}.
 */
public class FuzzyTargeting {

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
		return find(entity, horizontalRange, verticalRange, entity::getPathfindingFavor);
	}

	public static @Nullable Vec3d find(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			ToDoubleFunction<BlockPos> scorer
	) {
		boolean bl = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);
		return FuzzyPositions.guessBest(
				() -> {
					BlockPos blockPos = FuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange);
					BlockPos blockPos2 = towardTarget(entity, horizontalRange, bl, blockPos);
					return blockPos2 == null ? null : validate(entity, blockPos2);
				}, scorer
		);
	}

	/**
	 * Ищет to.
	 *
	 * @param entity entity
	 * @param horizontalRange horizontal range
	 * @param verticalRange vertical range
	 * @param end end
	 *
	 * @return @Nullable Vec3d — to
	 */
	public static @Nullable Vec3d findTo(PathAwareEntity entity, int horizontalRange, int verticalRange, Vec3d end) {
		Vec3d vec3d = end.subtract(entity.getX(), entity.getY(), entity.getZ());
		boolean bl = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);
		return findValid(entity, 0.0, horizontalRange, verticalRange, vec3d, bl);
	}

	public static @Nullable Vec3d findFrom(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			Vec3d start
	) {
		return findFrom(entity, 0.0, horizontalRange, verticalRange, start);
	}

	public static @Nullable Vec3d findFrom(
			PathAwareEntity entity,
			double minHorizontalRange,
			double maxHorizontalRange,
			int verticalRange,
			Vec3d start
	) {
		Vec3d vec3d = entity.getEntityPos().subtract(start);
		if (vec3d.length() == 0.0) {
			vec3d = new Vec3d(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
		}

		boolean bl = NavigationConditions.isPositionTargetInRange(entity, maxHorizontalRange);
		return findValid(entity, minHorizontalRange, maxHorizontalRange, verticalRange, vec3d, bl);
	}

	private static @Nullable Vec3d findValid(
			PathAwareEntity entity,
			double minHorizontalRange,
			double maxHorizontalRange,
			int verticalRange,
			Vec3d direction,
			boolean posTargetInRange
	) {
		return FuzzyPositions.guessBestPathTarget(
				entity,
				() -> {
					BlockPos blockPos = FuzzyPositions.localFuzz(
							entity.getRandom(),
							minHorizontalRange,
							maxHorizontalRange,
							verticalRange,
							0,
							direction.x,
							direction.z,
							(float) (Math.PI / 2)
					);
					if (blockPos == null) {
						return null;
					}
					else {
						BlockPos blockPos2 = towardTarget(entity, maxHorizontalRange, posTargetInRange, blockPos);
						return blockPos2 == null ? null : validate(entity, blockPos2);
					}
				}
		);
	}

	/**
	 * Validate.
	 *
	 * @param entity entity
	 * @param pos pos
	 *
	 * @return @Nullable BlockPos — результат операции
	 */
	public static @Nullable BlockPos validate(PathAwareEntity entity, BlockPos pos) {
		pos =
				FuzzyPositions.upWhile(
						pos,
						entity.getEntityWorld().getTopYInclusive(),
						currentPos -> NavigationConditions.isSolidAt(entity, currentPos)
				);
		return !NavigationConditions.isWaterAt(entity, pos) && !NavigationConditions.hasPathfindingPenalty(entity, pos)
		       ? pos : null;
	}

	public static @Nullable BlockPos towardTarget(
			PathAwareEntity entity,
			double horizontalRange,
			boolean posTargetInRange,
			BlockPos relativeInRangePos
	) {
		BlockPos
				blockPos =
				FuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), relativeInRangePos);
		return !NavigationConditions.isHeightInvalid(blockPos, entity)
				       && !NavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, blockPos)
				       && !NavigationConditions.isInvalidPosition(entity.getNavigation(), blockPos)
		       ? blockPos
		       : null;
	}
}
