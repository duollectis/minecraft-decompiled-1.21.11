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
 * {@code FuzzyPositions}.
 */
public class FuzzyPositions {

	private static final int GAUSS_RANGE = 10;

	/**
	 * Local fuzz.
	 *
	 * @param random random
	 * @param horizontalRange horizontal range
	 * @param verticalRange vertical range
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos localFuzz(Random random, int horizontalRange, int verticalRange) {
		int i = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
		int j = random.nextInt(2 * verticalRange + 1) - verticalRange;
		int k = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
		return new BlockPos(i, j, k);
	}

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
		double d = MathHelper.atan2(directionZ, directionX) - (float) (Math.PI / 2);
		double e = d + (2.0F * random.nextFloat() - 1.0F) * angleRange;
		double
				f =
				MathHelper.lerp(Math.sqrt(random.nextDouble()), minHorizontalRange, maxHorizontalRange)
						* MathHelper.SQUARE_ROOT_OF_TWO;
		double g = -f * Math.sin(e);
		double h = f * Math.cos(e);
		if (!(Math.abs(g) > maxHorizontalRange) && !(Math.abs(h) > maxHorizontalRange)) {
			int i = random.nextInt(2 * verticalRange + 1) - verticalRange + startHeight;
			return BlockPos.ofFloored(g, i, h);
		}
		else {
			return null;
		}
	}

	@VisibleForTesting
	/**
	 * Up while.
	 *
	 * @param pos pos
	 * @param maxY max y
	 * @param condition condition
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos upWhile(BlockPos pos, int maxY, Predicate<BlockPos> condition) {
		if (!condition.test(pos)) {
			return pos;
		}
		else {
			BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

			while (mutable.getY() <= maxY && condition.test(mutable)) {
				mutable.move(Direction.UP);
			}

			return mutable.toImmutable();
		}
	}

	@VisibleForTesting
	/**
	 * Up while.
	 *
	 * @param pos pos
	 * @param extraAbove extra above
	 * @param max max
	 * @param condition condition
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos upWhile(BlockPos pos, int extraAbove, int max, Predicate<BlockPos> condition) {
		if (extraAbove < 0) {
			throw new IllegalArgumentException("aboveSolidAmount was " + extraAbove + ", expected >= 0");
		}
		else if (!condition.test(pos)) {
			return pos;
		}
		else {
			BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

			while (mutable.getY() <= max && condition.test(mutable)) {
				mutable.move(Direction.UP);
			}

			int i = mutable.getY();

			while (mutable.getY() <= max && mutable.getY() - i < extraAbove) {
				mutable.move(Direction.UP);
				if (condition.test(mutable)) {
					mutable.move(Direction.DOWN);
					break;
				}
			}

			return mutable.toImmutable();
		}
	}

	/**
	 * Guess best path target.
	 *
	 * @param entity entity
	 * @param factory factory
	 *
	 * @return @Nullable Vec3d — результат операции
	 */
	public static @Nullable Vec3d guessBestPathTarget(PathAwareEntity entity, Supplier<@Nullable BlockPos> factory) {
		return guessBest(factory, entity::getPathfindingFavor);
	}

	/**
	 * Guess best.
	 *
	 * @param factory factory
	 * @param scorer scorer
	 *
	 * @return @Nullable Vec3d — результат операции
	 */
	public static @Nullable Vec3d guessBest(Supplier<@Nullable BlockPos> factory, ToDoubleFunction<BlockPos> scorer) {
		double d = Double.NEGATIVE_INFINITY;
		BlockPos blockPos = null;

		for (int i = 0; i < 10; i++) {
			BlockPos blockPos2 = factory.get();
			if (blockPos2 != null) {
				double e = scorer.applyAsDouble(blockPos2);
				if (e > d) {
					d = e;
					blockPos = blockPos2;
				}
			}
		}

		return blockPos != null ? Vec3d.ofBottomCenter(blockPos) : null;
	}

	/**
	 * Toward target.
	 *
	 * @param entity entity
	 * @param horizontalRange horizontal range
	 * @param random random
	 * @param fuzz fuzz
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos towardTarget(PathAwareEntity entity, double horizontalRange, Random random, BlockPos fuzz) {
		double d = fuzz.getX();
		double e = fuzz.getZ();
		if (entity.hasPositionTarget() && horizontalRange > 1.0) {
			BlockPos blockPos = entity.getPositionTarget();
			if (entity.getX() > blockPos.getX()) {
				d -= random.nextDouble() * horizontalRange / 2.0;
			}
			else {
				d += random.nextDouble() * horizontalRange / 2.0;
			}

			if (entity.getZ() > blockPos.getZ()) {
				e -= random.nextDouble() * horizontalRange / 2.0;
			}
			else {
				e += random.nextDouble() * horizontalRange / 2.0;
			}
		}

		return BlockPos.ofFloored(d + entity.getX(), fuzz.getY() + entity.getY(), e + entity.getZ());
	}
}
