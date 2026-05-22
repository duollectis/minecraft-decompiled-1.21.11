package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Утилита поиска позиции без штрафов пути для наземной навигации.
 * Проверяет высоту, зону цели, валидность навигации и отсутствие штрафов пути.
 */
public class NoPenaltyTargeting {

	public static @Nullable Vec3d find(PathAwareEntity entity, int horizontalRange, int verticalRange) {
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);

		return FuzzyPositions.guessBestPathTarget(
				entity,
				() -> {
					BlockPos fuzzPos = FuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange);
					return tryMake(entity, horizontalRange, posTargetInRange, fuzzPos);
				}
		);
	}

	/**
	 * Ищет позицию в направлении заданной точки {@code end} без штрафов пути.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус поиска
	 * @param verticalRange вертикальный диапазон поиска
	 * @param end целевая точка направления
	 * @param angleRange угол разброса в радианах
	 * @return лучшая найденная позиция или {@code null}
	 */
	public static @Nullable Vec3d findTo(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			Vec3d end,
			double angleRange
	) {
		Vec3d direction = end.subtract(entity.getX(), entity.getY(), entity.getZ());
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);

		return FuzzyPositions.guessBestPathTarget(
				entity,
				() -> {
					BlockPos fuzzPos = FuzzyPositions.localFuzz(
							entity.getRandom(),
							0.0,
							horizontalRange,
							verticalRange,
							0,
							direction.x,
							direction.z,
							angleRange
					);
					return fuzzPos == null ? null : tryMake(entity, horizontalRange, posTargetInRange, fuzzPos);
				}
		);
	}

	/**
	 * Ищет позицию в направлении «от» заданной точки {@code start} без штрафов пути.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус поиска
	 * @param verticalRange вертикальный диапазон поиска
	 * @param start точка, от которой убегает существо
	 * @return лучшая найденная позиция или {@code null}
	 */
	public static @Nullable Vec3d findFrom(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			Vec3d start
	) {
		Vec3d direction = entity.getEntityPos().subtract(start);
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);

		return FuzzyPositions.guessBestPathTarget(
				entity,
				() -> {
					BlockPos fuzzPos = FuzzyPositions.localFuzz(
							entity.getRandom(),
							0.0,
							horizontalRange,
							verticalRange,
							0,
							direction.x,
							direction.z,
							(float) (Math.PI / 2)
					);
					return fuzzPos == null ? null : tryMake(entity, horizontalRange, posTargetInRange, fuzzPos);
				}
		);
	}

	private static @Nullable BlockPos tryMake(
			PathAwareEntity entity,
			int horizontalRange,
			boolean posTargetInRange,
			BlockPos fuzz
	) {
		BlockPos targetPos = FuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), fuzz);

		return NavigationConditions.isHeightInvalid(targetPos, entity)
				|| NavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, targetPos)
				|| NavigationConditions.isInvalidPosition(entity.getNavigation(), targetPos)
				|| NavigationConditions.hasPathfindingPenalty(entity, targetPos)
				? null
				: targetPos;
	}
}
