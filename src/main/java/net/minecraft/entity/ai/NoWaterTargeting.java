package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Утилита поиска позиции над твёрдой поверхностью без воды и штрафов пути.
 * Расширяет логику {@link NoPenaltySolidTargeting}, дополнительно исключая водные блоки.
 */
public class NoWaterTargeting {

	/**
	 * Ищет позицию над твёрдой поверхностью в заданном направлении, исключая воду.
	 *
	 * @param entity существо-навигатор
	 * @param horizontalRange горизонтальный радиус поиска
	 * @param verticalRange вертикальный диапазон поиска
	 * @param startHeight начальное смещение по Y
	 * @param direction вектор направления (абсолютные координаты)
	 * @param angleRange угол разброса в радианах
	 * @return лучшая найденная позиция или {@code null}
	 */
	public static @Nullable Vec3d find(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			int startHeight,
			Vec3d direction,
			double angleRange
	) {
		Vec3d relativeDirection = direction.subtract(entity.getX(), entity.getY(), entity.getZ());
		boolean posTargetInRange = NavigationConditions.isPositionTargetInRange(entity, horizontalRange);

		return FuzzyPositions.guessBestPathTarget(
				entity,
				() -> {
					BlockPos candidate = NoPenaltySolidTargeting.tryMake(
							entity,
							horizontalRange,
							verticalRange,
							startHeight,
							relativeDirection.x,
							relativeDirection.z,
							angleRange,
							posTargetInRange
					);
					return candidate != null && !NavigationConditions.isWaterAt(entity, candidate)
							? candidate
							: null;
				}
		);
	}
}
