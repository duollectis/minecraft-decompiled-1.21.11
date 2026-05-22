package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Утилита поиска позиции над твёрдой поверхностью без штрафов пути.
 * В отличие от {@link NoPenaltyTargeting}, не проверяет валидность навигации,
 * но поднимает позицию над твёрдыми блоками.
 */
public class NoPenaltySolidTargeting {

	/**
	 * Ищет позицию над твёрдой поверхностью в заданном направлении без штрафов пути.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус поиска
	 * @param verticalRange вертикальный диапазон поиска
	 * @param startHeight начальное смещение по Y
	 * @param directionX X-компонента вектора направления
	 * @param directionZ Z-компонента вектора направления
	 * @param rangeAngle угол разброса в радианах
	 * @return лучшая найденная позиция или {@code null}
	 */
	public static @Nullable Vec3d find(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			int startHeight,
			double directionX,
			double directionZ,
			double rangeAngle
	) {
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);

		return FuzzyPositions.guessBestPathTarget(
				entity,
				() -> tryMake(
						entity,
						horizontalRange,
						verticalRange,
						startHeight,
						directionX,
						directionZ,
						rangeAngle,
						posTargetInRange
				)
		);
	}

	/**
	 * Генерирует одного кандидата позиции над твёрдой поверхностью.
	 * Возвращает {@code null}, если позиция невалидна по высоте, вне зоны цели или имеет штраф пути.
	 */
	public static @Nullable BlockPos tryMake(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			int startHeight,
			double directionX,
			double directionZ,
			double rangeAngle,
			boolean posTargetInRange
	) {
		BlockPos fuzzPos = FuzzyPositions.localFuzz(
				entity.getRandom(),
				0.0,
				horizontalRange,
				verticalRange,
				startHeight,
				directionX,
				directionZ,
				rangeAngle
		);

		if (fuzzPos == null) {
			return null;
		}

		BlockPos targetPos = FuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), fuzzPos);

		if (NavigationConditions.isHeightInvalid(targetPos, entity)
				|| NavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, targetPos)) {
			return null;
		}

		BlockPos elevated = FuzzyPositions.upWhile(
				targetPos,
				entity.getEntityWorld().getTopYInclusive(),
				pos -> NavigationConditions.isSolidAt(entity, pos)
		);

		return NavigationConditions.hasPathfindingPenalty(entity, elevated) ? null : elevated;
	}
}
