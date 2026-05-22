package net.minecraft.world;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Утилитарный класс для поиска прямоугольных областей блоков и колонн.
 * Используется, например, для нахождения порталов и других структур.
 */
public class BlockLocating {

	/**
	 * Находит наибольший прямоугольник из блоков, удовлетворяющих предикату,
	 * с центром в заданной позиции. Алгоритм работает по двум осям:
	 * первичной (primaryAxis) и вторичной (secondaryAxis).
	 *
	 * @param center           центральная позиция поиска
	 * @param primaryAxis      ось, вдоль которой ищется ширина прямоугольника
	 * @param primaryMaxBlocks максимальное число блоков в каждую сторону по первичной оси
	 * @param secondaryAxis    ось, вдоль которой ищется высота прямоугольника
	 * @param secondaryMaxBlocks максимальное число блоков в каждую сторону по вторичной оси
	 * @param predicate        условие, которому должен удовлетворять каждый блок
	 * @return наибольший найденный прямоугольник
	 */
	public static BlockLocating.Rectangle getLargestRectangle(
		BlockPos center,
		Direction.Axis primaryAxis,
		int primaryMaxBlocks,
		Direction.Axis secondaryAxis,
		int secondaryMaxBlocks,
		Predicate<BlockPos> predicate
	) {
		BlockPos.Mutable mutable = center.mutableCopy();
		Direction negPrimary = Direction.get(Direction.AxisDirection.NEGATIVE, primaryAxis);
		Direction posPrimary = negPrimary.getOpposite();
		Direction negSecondary = Direction.get(Direction.AxisDirection.NEGATIVE, secondaryAxis);
		Direction posSecondary = negSecondary.getOpposite();

		int negPrimaryCount = moveWhile(predicate, mutable.set(center), negPrimary, primaryMaxBlocks);
		int posPrimaryCount = moveWhile(predicate, mutable.set(center), posPrimary, primaryMaxBlocks);
		int centerIndex = negPrimaryCount;

		BlockLocating.IntBounds[] boundsPerColumn = new BlockLocating.IntBounds[negPrimaryCount + 1 + posPrimaryCount];
		boundsPerColumn[centerIndex] = new BlockLocating.IntBounds(
			moveWhile(predicate, mutable.set(center), negSecondary, secondaryMaxBlocks),
			moveWhile(predicate, mutable.set(center), posSecondary, secondaryMaxBlocks)
		);

		int secondaryOrigin = boundsPerColumn[centerIndex].min;

		for (int step = 1; step <= negPrimaryCount; step++) {
			BlockLocating.IntBounds prevBounds = boundsPerColumn[centerIndex - (step - 1)];
			boundsPerColumn[centerIndex - step] = new BlockLocating.IntBounds(
				moveWhile(predicate, mutable.set(center).move(negPrimary, step), negSecondary, prevBounds.min),
				moveWhile(predicate, mutable.set(center).move(negPrimary, step), posSecondary, prevBounds.max)
			);
		}

		for (int step = 1; step <= posPrimaryCount; step++) {
			BlockLocating.IntBounds prevBounds = boundsPerColumn[centerIndex + step - 1];
			boundsPerColumn[centerIndex + step] = new BlockLocating.IntBounds(
				moveWhile(predicate, mutable.set(center).move(posPrimary, step), negSecondary, prevBounds.min),
				moveWhile(predicate, mutable.set(center).move(posPrimary, step), posSecondary, prevBounds.max)
			);
		}

		int bestPrimaryStart = 0;
		int bestSecondaryStart = 0;
		int bestWidth = 0;
		int bestHeight = 0;
		int[] columnHeights = new int[boundsPerColumn.length];

		for (int secondaryOffset = secondaryOrigin; secondaryOffset >= 0; secondaryOffset--) {
			for (int col = 0; col < boundsPerColumn.length; col++) {
				BlockLocating.IntBounds bounds = boundsPerColumn[col];
				int top = secondaryOrigin - bounds.min;
				int bottom = secondaryOrigin + bounds.max;
				columnHeights[col] = secondaryOffset >= top && secondaryOffset <= bottom ? bottom + 1 - secondaryOffset : 0;
			}

			Pair<BlockLocating.IntBounds, Integer> largest = findLargestRectangle(columnHeights);
			BlockLocating.IntBounds widthBounds = largest.getFirst();
			int width = 1 + widthBounds.max - widthBounds.min;
			int height = largest.getSecond();

			if (width * height > bestWidth * bestHeight) {
				bestPrimaryStart = widthBounds.min;
				bestSecondaryStart = secondaryOffset;
				bestWidth = width;
				bestHeight = height;
			}
		}

		return new BlockLocating.Rectangle(
			center.offset(primaryAxis, bestPrimaryStart - centerIndex).offset(secondaryAxis, bestSecondaryStart - secondaryOrigin),
			bestWidth,
			bestHeight
		);
	}

	private static int moveWhile(Predicate<BlockPos> predicate, BlockPos.Mutable pos, Direction direction, int max) {
		int count = 0;

		while (count < max && predicate.test(pos.move(direction))) {
			count++;
		}

		return count;
	}

	/**
	 * Находит наибольший прямоугольник в гистограмме высот столбцов.
	 * Использует алгоритм на основе стека за O(n).
	 *
	 * @param heights массив высот столбцов гистограммы
	 * @return пара: диапазон столбцов (IntBounds) и высота прямоугольника
	 */
	@VisibleForTesting
	static Pair<BlockLocating.IntBounds, Integer> findLargestRectangle(int[] heights) {
		int bestLeft = 0;
		int bestRight = 0;
		int bestHeight = 0;
		IntStack stack = new IntArrayList();
		stack.push(0);

		for (int index = 1; index <= heights.length; index++) {
			int currentHeight = index == heights.length ? 0 : heights[index];

			while (!stack.isEmpty()) {
				int topHeight = heights[stack.topInt()];
				if (currentHeight >= topHeight) {
					stack.push(index);
					break;
				}

				stack.popInt();
				int left = stack.isEmpty() ? 0 : stack.topInt() + 1;

				if (topHeight * (index - left) > bestHeight * (bestRight - bestLeft)) {
					bestRight = index;
					bestLeft = left;
					bestHeight = topHeight;
				}
			}

			if (stack.isEmpty()) {
				stack.push(index);
			}
		}

		return new Pair<>(new BlockLocating.IntBounds(bestLeft, bestRight - 1), bestHeight);
	}

	/**
	 * Ищет конец колонны блоков заданного типа в указанном направлении.
	 * Двигается от {@code pos} в сторону {@code direction}, пока встречает {@code intermediateBlock},
	 * и возвращает позицию, если там находится {@code endBlock}.
	 *
	 * @param world             мир для чтения блоков
	 * @param pos               начальная позиция
	 * @param intermediateBlock блок, через который проходим
	 * @param direction         направление движения
	 * @param endBlock          блок, которым должна заканчиваться колонна
	 * @return позиция конечного блока, если найден
	 */
	public static Optional<BlockPos> findColumnEnd(
		BlockView world,
		BlockPos pos,
		Block intermediateBlock,
		Direction direction,
		Block endBlock
	) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		BlockState blockState;
		do {
			mutable.move(direction);
			blockState = world.getBlockState(mutable);
		} while (blockState.isOf(intermediateBlock));

		return blockState.isOf(endBlock) ? Optional.of(mutable) : Optional.empty();
	}

	/** Целочисленный диапазон [min, max] включительно. */
	public static class IntBounds {

		public final int min;
		public final int max;

		public IntBounds(int min, int max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public String toString() {
			return "IntBounds{min=" + min + ", max=" + max + "}";
		}
	}

	/** Прямоугольная область блоков с нижним левым углом и размерами. */
	public static class Rectangle {

		public final BlockPos lowerLeft;
		public final int width;
		public final int height;

		public Rectangle(BlockPos lowerLeft, int width, int height) {
			this.lowerLeft = lowerLeft;
			this.width = width;
			this.height = height;
		}
	}
}
