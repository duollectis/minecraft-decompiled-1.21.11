package net.minecraft.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Утилита генерации «размытых» (fuzzy) позиций для навигации существ.
 * Использует гауссово распределение для выбора лучшей из нескольких случайных точек.
 */
public class FuzzyPositions {

	/** Количество попыток генерации кандидатов при выборе лучшей позиции. */
	private static final int GAUSS_RANGE = 10;

	/**
	 * Генерирует случайное смещение в заданном горизонтальном и вертикальном диапазоне.
	 */
	public static BlockPos localFuzz(Random random, int horizontalRange, int verticalRange) {
		int dx = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
		int dy = random.nextInt(2 * verticalRange + 1) - verticalRange;
		int dz = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
		return new BlockPos(dx, dy, dz);
	}

	/**
	 * Генерирует случайное смещение с учётом направления и угла разброса.
	 * Возвращает {@code null}, если сгенерированная точка выходит за пределы горизонтального диапазона.
	 *
	 * @param random источник случайности
	 * @param minHorizontalRange минимальный горизонтальный радиус
	 * @param maxHorizontalRange максимальный горизонтальный радиус
	 * @param verticalRange вертикальный диапазон
	 * @param startHeight начальное смещение по Y
	 * @param directionX X-компонента вектора направления
	 * @param directionZ Z-компонента вектора направления
	 * @param angleRange угол разброса в радианах
	 * @return смещение в виде {@link BlockPos} или {@code null}
	 */
	public static @Nullable BlockPos localFuzz(
			Random random,
			double minHorizontalRange,
			double maxHorizontalRange,
			int verticalRange,
			int startHeight,
			double directionX,
			double directionZ,
			double angleRange
	) {
		double baseAngle = MathHelper.atan2(directionZ, directionX) - (float) (Math.PI / 2);
		double scatteredAngle = baseAngle + (2.0F * random.nextFloat() - 1.0F) * angleRange;
		double radius = MathHelper.lerp(Math.sqrt(random.nextDouble()), minHorizontalRange, maxHorizontalRange)
				* MathHelper.SQUARE_ROOT_OF_TWO;
		double offsetX = -radius * Math.sin(scatteredAngle);
		double offsetZ = radius * Math.cos(scatteredAngle);

		if (Math.abs(offsetX) > maxHorizontalRange || Math.abs(offsetZ) > maxHorizontalRange) {
			return null;
		}

		int offsetY = random.nextInt(2 * verticalRange + 1) - verticalRange + startHeight;
		return BlockPos.ofFloored(offsetX, offsetY, offsetZ);
	}

	/**
	 * Поднимает позицию вверх, пока выполняется условие или не достигнут максимум Y.
	 */
	@VisibleForTesting
	public static BlockPos upWhile(BlockPos pos, int maxY, Predicate<BlockPos> condition) {
		if (!condition.test(pos)) {
			return pos;
		}

		BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

		while (mutable.getY() <= maxY && condition.test(mutable)) {
			mutable.move(Direction.UP);
		}

		return mutable.toImmutable();
	}

	/**
	 * Поднимает позицию вверх над твёрдой поверхностью на заданное количество блоков.
	 * Сначала поднимается до первого нетвёрдого блока, затем ещё на {@code extraAbove} блоков.
	 *
	 * @param pos начальная позиция
	 * @param extraAbove количество блоков над поверхностью (должно быть >= 0)
	 * @param max максимальная Y-координата
	 * @param condition предикат «блок твёрдый»
	 * @return итоговая позиция над поверхностью
	 */
	@VisibleForTesting
	public static BlockPos upWhile(BlockPos pos, int extraAbove, int max, Predicate<BlockPos> condition) {
		if (extraAbove < 0) {
			throw new IllegalArgumentException("aboveSolidAmount was " + extraAbove + ", expected >= 0");
		}

		if (!condition.test(pos)) {
			return pos;
		}

		BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

		while (mutable.getY() <= max && condition.test(mutable)) {
			mutable.move(Direction.UP);
		}

		int surfaceY = mutable.getY();

		while (mutable.getY() <= max && mutable.getY() - surfaceY < extraAbove) {
			mutable.move(Direction.UP);
			if (condition.test(mutable)) {
				mutable.move(Direction.DOWN);
				break;
			}
		}

		return mutable.toImmutable();
	}

	/**
	 * Выбирает лучшую из {@value #GAUSS_RANGE} случайных позиций по оценке пригодности пути существа.
	 */
	public static @Nullable Vec3d guessBestPathTarget(PathAwareEntity entity, Supplier<@Nullable BlockPos> factory) {
		return guessBest(factory, entity::getPathfindingFavor);
	}

	/**
	 * Выбирает лучшую из {@value #GAUSS_RANGE} случайных позиций по произвольной функции оценки.
	 *
	 * @param factory поставщик случайных кандидатов
	 * @param scorer функция оценки позиции (чем выше — тем лучше)
	 * @return центр нижней грани лучшей позиции или {@code null}, если ни одна не подошла
	 */
	public static @Nullable Vec3d guessBest(Supplier<@Nullable BlockPos> factory, ToDoubleFunction<BlockPos> scorer) {
		double bestScore = Double.NEGATIVE_INFINITY;
		BlockPos bestPos = null;

		for (int attempt = 0; attempt < GAUSS_RANGE; attempt++) {
			BlockPos candidate = factory.get();
			if (candidate == null) {
				continue;
			}

			double score = scorer.applyAsDouble(candidate);
			if (score > bestScore) {
				bestScore = score;
				bestPos = candidate;
			}
		}

		return bestPos != null ? Vec3d.ofBottomCenter(bestPos) : null;
	}

	/**
	 * Смещает относительную позицию {@code fuzz} к цели существа (если она задана),
	 * добавляя случайное притяжение в сторону {@link PathAwareEntity#getPositionTarget()}.
	 *
	 * @param entity существо с возможной целевой позицией
	 * @param horizontalRange горизонтальный радиус для масштабирования притяжения
	 * @param random источник случайности
	 * @param fuzz относительное смещение
	 * @return абсолютная позиция в мире
	 */
	public static BlockPos towardTarget(PathAwareEntity entity, double horizontalRange, Random random, BlockPos fuzz) {
		double targetX = fuzz.getX();
		double targetZ = fuzz.getZ();

		if (entity.hasPositionTarget() && horizontalRange > 1.0) {
			BlockPos posTarget = entity.getPositionTarget();

			if (entity.getX() > posTarget.getX()) {
				targetX -= random.nextDouble() * horizontalRange / 2.0;
			} else {
				targetX += random.nextDouble() * horizontalRange / 2.0;
			}

			if (entity.getZ() > posTarget.getZ()) {
				targetZ -= random.nextDouble() * horizontalRange / 2.0;
			} else {
				targetZ += random.nextDouble() * horizontalRange / 2.0;
			}
		}

		return BlockPos.ofFloored(targetX + entity.getX(), fuzz.getY() + entity.getY(), targetZ + entity.getZ());
	}
}
