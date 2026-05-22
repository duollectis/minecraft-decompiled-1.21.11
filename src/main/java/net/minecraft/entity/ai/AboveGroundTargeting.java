package net.minecraft.entity.ai;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Утилита поиска позиции над землёй для навигации летающих существ.
 * Генерирует случайную точку в заданном радиусе, поднимает её над твёрдой поверхностью
 * и проверяет отсутствие штрафов пути.
 */
public class AboveGroundTargeting {

	/**
	 * Ищет подходящую позицию над землёй в заданном радиусе от существа.
	 *
	 * @param entity существо, для которого ищется позиция
	 * @param horizontalRange горизонтальный радиус поиска в блоках
	 * @param verticalRange вертикальный диапазон поиска в блоках
	 * @param x целевая X-координата направления движения
	 * @param z целевая Z-координата направления движения
	 * @param angle угол разброса направления в радианах
	 * @param maxAboveSolid максимальная высота над твёрдым блоком
	 * @param minAboveSolid минимальная высота над твёрдым блоком
	 * @return позиция над землёй или {@code null}, если подходящая не найдена
	 */
	public static @Nullable Vec3d find(
			PathAwareEntity entity,
			int horizontalRange,
			int verticalRange,
			double x,
			double z,
			float angle,
			int maxAboveSolid,
			int minAboveSolid
	) {
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
							x,
							z,
							angle
					);

					if (fuzzPos == null) {
						return null;
					}

					BlockPos targetPos = FuzzyTargeting.towardTarget(entity, horizontalRange, posTargetInRange, fuzzPos);

					if (targetPos == null) {
						return null;
					}

					int aboveOffset = entity.getRandom().nextInt(maxAboveSolid - minAboveSolid + 1) + minAboveSolid;
					targetPos = FuzzyPositions.upWhile(
							targetPos,
							aboveOffset,
							entity.getEntityWorld().getTopYInclusive(),
							pos -> NavigationConditions.isSolidAt(entity, pos)
					);

					return NavigationConditions.isWaterAt(entity, targetPos)
							|| NavigationConditions.hasPathfindingPenalty(entity, targetPos)
							? null
							: targetPos;
				}
		);
	}
}
