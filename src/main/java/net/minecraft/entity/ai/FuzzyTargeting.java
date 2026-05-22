package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.function.ToDoubleFunction;

/**
 * Утилита поиска случайных целевых позиций для наземной навигации существ.
 * Генерирует кандидатов через {@link FuzzyPositions} и фильтрует невалидные позиции.
 */
public class FuzzyTargeting {

	public static @Nullable Vec3d find(PathAwareEntity entity, int horizontalRange, int verticalRange) {
		return find(entity, horizontalRange, verticalRange, entity::getPathfindingFavor);
	}

	/**
	 * Ищет случайную позицию в заданном радиусе с пользовательской функцией оценки.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус поиска
	 * @param verticalRange вертикальный диапазон поиска
	 * @param scorer функция оценки пригодности позиции
	 * @return лучшая найденная позиция или {@code null}
	 */
	public static @Nullable Vec3d find(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			ToDoubleFunction<BlockPos> scorer
	) {
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);

		return FuzzyPositions.guessBest(
				() -> {
					BlockPos fuzzPos = FuzzyPositions.localFuzz(entity.getRandom(), horizontalRange, verticalRange);
					BlockPos targetPos = towardTarget(entity, horizontalRange, posTargetInRange, fuzzPos);
					return targetPos == null ? null : validate(entity, targetPos);
				},
				scorer
		);
	}

	/**
	 * Ищет позицию в направлении заданной точки {@code end}.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус поиска
	 * @param verticalRange вертикальный диапазон поиска
	 * @param end целевая точка, в сторону которой ищется позиция
	 * @return позиция в направлении цели или {@code null}
	 */
	public static @Nullable Vec3d findTo(PathAwareEntity entity, int horizontalRange, int verticalRange, Vec3d end) {
		Vec3d direction = end.subtract(entity.getX(), entity.getY(), entity.getZ());
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);
		return findValid(entity, 0.0, horizontalRange, verticalRange, direction, posTargetInRange);
	}

	public static @Nullable Vec3d findFrom(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			Vec3d start
	) {
		return findFrom(entity, 0.0, horizontalRange, verticalRange, start);
	}

	/**
	 * Ищет позицию в направлении «от» заданной точки {@code start}.
	 * Если вектор нулевой — выбирается случайное направление.
	 *
	 * @param entity существо-навигатор
	 * @param minHorizontalRange минимальный горизонтальный радиус
	 * @param maxHorizontalRange максимальный горизонтальный радиус
	 * @param verticalRange вертикальный диапазон поиска
	 * @param start точка, от которой убегает существо
	 * @return позиция в направлении «от» цели или {@code null}
	 */
	public static @Nullable Vec3d findFrom(
			PathAwareEntity entity,
			double minHorizontalRange,
			double maxHorizontalRange,
			int verticalRange,
			Vec3d start
	) {
		Vec3d direction = entity.getEntityPos().subtract(start);

		if (direction.length() == 0.0) {
			direction = new Vec3d(
					entity.getRandom().nextDouble() - 0.5,
					0.0,
					entity.getRandom().nextDouble() - 0.5
			);
		}

		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, maxHorizontalRange);
		return findValid(entity, minHorizontalRange, maxHorizontalRange, verticalRange, direction, posTargetInRange);
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
					BlockPos fuzzPos = FuzzyPositions.localFuzz(
							entity.getRandom(),
							minHorizontalRange,
							maxHorizontalRange,
							verticalRange,
							0,
							direction.x,
							direction.z,
							(float) (Math.PI / 2)
					);

					if (fuzzPos == null) {
						return null;
					}

					BlockPos targetPos = towardTarget(entity, maxHorizontalRange, posTargetInRange, fuzzPos);
					return targetPos == null ? null : validate(entity, targetPos);
				}
		);
	}

	/**
	 * Проверяет позицию: поднимает над твёрдой поверхностью и отклоняет воду и штрафные зоны.
	 *
	 * @param entity существо для проверки штрафов пути
	 * @param pos кандидат позиции
	 * @return валидная позиция или {@code null}
	 */
	public static @Nullable BlockPos validate(PathAwareEntity entity, BlockPos pos) {
		BlockPos elevated = FuzzyPositions.upWhile(
				pos,
				entity.getEntityWorld().getTopYInclusive(),
				currentPos -> NavigationConditions.isSolidAt(entity, currentPos)
		);

		return NavigationConditions.isWaterAt(entity, elevated)
				|| NavigationConditions.hasPathfindingPenalty(entity, elevated)
				? null
				: elevated;
	}

	/**
	 * Смещает относительную позицию к цели существа и проверяет валидность высоты и навигации.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус для притяжения к цели
	 * @param posTargetInRange флаг: целевая позиция существа находится в радиусе
	 * @param relativePos относительное смещение
	 * @return абсолютная валидная позиция или {@code null}
	 */
	public static @Nullable BlockPos towardTarget(
			PathAwareEntity entity,
			double horizontalRange,
			boolean posTargetInRange,
			BlockPos relativePos
	) {
		BlockPos targetPos = FuzzyPositions.towardTarget(entity, horizontalRange, entity.getRandom(), relativePos);

		return NavigationConditions.isHeightInvalid(targetPos, entity)
				|| NavigationConditions.isPositionTargetOutOfWalkRange(posTargetInRange, entity, targetPos)
				|| NavigationConditions.isInvalidPosition(entity.getNavigation(), targetPos)
				? null
				: targetPos;
	}
}
