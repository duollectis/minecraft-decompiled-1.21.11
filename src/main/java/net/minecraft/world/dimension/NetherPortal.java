package net.minecraft.world.dimension;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Логика обнаружения, валидации и создания портала в Нижний мир.
 * Портал может быть ориентирован по оси X или Z, иметь ширину от 2 до 21 блока
 * и высоту от 3 до 21 блока. Рамка должна быть из обсидиана.
 */
public class NetherPortal {

	public static final int MAX_WIDTH = 21;
	public static final int MAX_HEIGHT = 21;

	private static final int MIN_WIDTH = 2;
	private static final int MIN_HEIGHT = 3;
	private static final float FALLBACK_THRESHOLD = 4.0F;
	private static final double HEIGHT_STRETCH = 1.0;

	private static final AbstractBlock.ContextPredicate IS_VALID_FRAME_BLOCK =
		(state, world, pos) -> state.isOf(Blocks.OBSIDIAN);

	private final Direction.Axis axis;
	private final Direction negativeDir;
	private final int foundPortalBlocks;
	private final BlockPos lowerCorner;
	private final int height;
	private final int width;

	private NetherPortal(
		Direction.Axis axis,
		int foundPortalBlocks,
		Direction negativeDir,
		BlockPos lowerCorner,
		int width,
		int height
	) {
		this.axis = axis;
		this.foundPortalBlocks = foundPortalBlocks;
		this.negativeDir = negativeDir;
		this.lowerCorner = lowerCorner;
		this.width = width;
		this.height = height;
	}

	/**
	 * Ищет позицию для создания нового портала — пустое место без существующих блоков портала.
	 *
	 * @param world            мир для поиска
	 * @param pos              начальная позиция поиска
	 * @param firstCheckedAxis ось, которая проверяется первой
	 * @return найденный портал, если подходящее место существует
	 */
	public static Optional<NetherPortal> getNewPortal(
		WorldAccess world,
		BlockPos pos,
		Direction.Axis firstCheckedAxis
	) {
		return getOrEmpty(
			world,
			pos,
			portal -> portal.isValid() && portal.foundPortalBlocks == 0,
			firstCheckedAxis
		);
	}

	/**
	 * Ищет портал по обеим осям (X и Z), возвращая первый, прошедший валидацию.
	 *
	 * @param world            мир для поиска
	 * @param pos              начальная позиция
	 * @param validator        предикат валидации найденного портала
	 * @param firstCheckedAxis ось, проверяемая первой
	 * @return первый валидный портал или пустой Optional
	 */
	public static Optional<NetherPortal> getOrEmpty(
		WorldAccess world,
		BlockPos pos,
		Predicate<NetherPortal> validator,
		Direction.Axis firstCheckedAxis
	) {
		Optional<NetherPortal> result = Optional.of(getOnAxis(world, pos, firstCheckedAxis)).filter(validator);
		if (result.isPresent()) {
			return result;
		}

		Direction.Axis otherAxis = firstCheckedAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		return Optional.of(getOnAxis(world, pos, otherAxis)).filter(validator);
	}

	/**
	 * Строит описание портала на указанной оси, начиная с заданной позиции.
	 * Если рамка не найдена или некорректна — возвращает портал с нулевыми размерами.
	 *
	 * @param world мир
	 * @param pos   начальная позиция
	 * @param axis  ось ориентации портала
	 * @return описание найденного (или отсутствующего) портала
	 */
	public static NetherPortal getOnAxis(BlockView world, BlockPos pos, Direction.Axis axis) {
		Direction direction = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
		BlockPos lowerCorner = getLowerCorner(world, direction, pos);
		if (lowerCorner == null) {
			return new NetherPortal(axis, 0, direction, pos, 0, 0);
		}

		int width = getValidatedWidth(world, lowerCorner, direction);
		if (width == 0) {
			return new NetherPortal(axis, 0, direction, lowerCorner, 0, 0);
		}

		MutableInt portalBlockCount = new MutableInt();
		int height = getHeight(world, lowerCorner, direction, width, portalBlockCount);
		return new NetherPortal(axis, portalBlockCount.intValue(), direction, lowerCorner, width, height);
	}

	/**
	 * Находит нижний левый угол рамки портала, спускаясь вниз и смещаясь в сторону рамки.
	 *
	 * @param world     мир
	 * @param direction направление к отрицательной стороне портала
	 * @param pos       начальная позиция
	 * @return нижний левый угол рамки или null, если рамка не найдена
	 */
	private static @Nullable BlockPos getLowerCorner(BlockView world, Direction direction, BlockPos pos) {
		int minY = Math.max(world.getBottomY(), pos.getY() - MAX_HEIGHT);

		while (pos.getY() > minY && validStateInsidePortal(world.getBlockState(pos.down()))) {
			pos = pos.down();
		}

		Direction opposite = direction.getOpposite();
		int offset = getWidth(world, pos, opposite) - 1;
		return offset < 0 ? null : pos.offset(opposite, offset);
	}

	private static int getValidatedWidth(BlockView world, BlockPos lowerCorner, Direction negativeDir) {
		int width = getWidth(world, lowerCorner, negativeDir);
		return width >= MIN_WIDTH && width <= MAX_WIDTH ? width : 0;
	}

	private static int getWidth(BlockView world, BlockPos lowerCorner, Direction negativeDir) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int offset = 0; offset <= MAX_WIDTH; offset++) {
			mutable.set(lowerCorner).move(negativeDir, offset);
			BlockState state = world.getBlockState(mutable);
			if (!validStateInsidePortal(state)) {
				return IS_VALID_FRAME_BLOCK.test(state, world, mutable) ? offset : 0;
			}

			BlockState below = world.getBlockState(mutable.move(Direction.DOWN));
			if (!IS_VALID_FRAME_BLOCK.test(below, world, mutable)) {
				return 0;
			}
		}

		return 0;
	}

	private static int getHeight(
		BlockView world,
		BlockPos lowerCorner,
		Direction negativeDir,
		int width,
		MutableInt foundPortalBlocks
	) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int potentialHeight = getPotentialHeight(world, lowerCorner, negativeDir, mutable, width, foundPortalBlocks);
		boolean topFrameValid = potentialHeight >= MIN_HEIGHT
			&& potentialHeight <= MAX_WIDTH
			&& isHorizontalFrameValid(world, lowerCorner, negativeDir, mutable, width, potentialHeight);

		return topFrameValid ? potentialHeight : 0;
	}

	private static boolean isHorizontalFrameValid(
		BlockView world,
		BlockPos lowerCorner,
		Direction direction,
		BlockPos.Mutable pos,
		int width,
		int height
	) {
		for (int offset = 0; offset < width; offset++) {
			BlockPos.Mutable framePos = pos.set(lowerCorner).move(Direction.UP, height).move(direction, offset);
			if (!IS_VALID_FRAME_BLOCK.test(world.getBlockState(framePos), world, framePos)) {
				return false;
			}
		}

		return true;
	}

	private static int getPotentialHeight(
		BlockView world,
		BlockPos lowerCorner,
		Direction negativeDir,
		BlockPos.Mutable pos,
		int width,
		MutableInt foundPortalBlocks
	) {
		for (int row = 0; row < MAX_WIDTH; row++) {
			pos.set(lowerCorner).move(Direction.UP, row).move(negativeDir, -1);
			if (!IS_VALID_FRAME_BLOCK.test(world.getBlockState(pos), world, pos)) {
				return row;
			}

			pos.set(lowerCorner).move(Direction.UP, row).move(negativeDir, width);
			if (!IS_VALID_FRAME_BLOCK.test(world.getBlockState(pos), world, pos)) {
				return row;
			}

			for (int col = 0; col < width; col++) {
				pos.set(lowerCorner).move(Direction.UP, row).move(negativeDir, col);
				BlockState state = world.getBlockState(pos);
				if (!validStateInsidePortal(state)) {
					return row;
				}

				if (state.isOf(Blocks.NETHER_PORTAL)) {
					foundPortalBlocks.increment();
				}
			}
		}

		return MAX_WIDTH;
	}

	private static boolean validStateInsidePortal(BlockState state) {
		return state.isAir() || state.isIn(BlockTags.FIRE) || state.isOf(Blocks.NETHER_PORTAL);
	}

	/** Проверяет, что портал имеет допустимые размеры (ширина 2–21, высота 3–21). */
	public boolean isValid() {
		return width >= MIN_WIDTH && width <= MAX_WIDTH && height >= MIN_HEIGHT && height <= MAX_HEIGHT;
	}

	/**
	 * Заполняет область портала блоками {@code NETHER_PORTAL} с правильной ориентацией по оси.
	 *
	 * @param world мир, в котором создаётся портал
	 */
	public void createPortal(WorldAccess world) {
		BlockState portalState = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis);
		BlockPos.iterate(
			lowerCorner,
			lowerCorner.offset(Direction.UP, height - 1).offset(negativeDir, width - 1)
		).forEach(pos -> world.setBlockState(pos, portalState, 18));
	}

	/** Возвращает true, если портал уже был полностью заполнен блоками портала. */
	public boolean wasAlreadyValid() {
		return isValid() && foundPortalBlocks == width * height;
	}

	/**
	 * Вычисляет нормализованную позицию сущности внутри портала (0..1 по ширине и высоте)
	 * и смещение по перпендикулярной оси. Используется для корректного позиционирования
	 * при телепортации через портал.
	 *
	 * @param portalRect      прямоугольник портала
	 * @param portalAxis      ось ориентации портала
	 * @param entityPos       позиция сущности
	 * @param entityDimensions размеры сущности
	 * @return вектор (нормализованная позиция по ширине, по высоте, смещение по перп. оси)
	 */
	public static Vec3d entityPosInPortal(
		BlockLocating.Rectangle portalRect,
		Direction.Axis portalAxis,
		Vec3d entityPos,
		EntityDimensions entityDimensions
	) {
		double availableWidth = portalRect.width - entityDimensions.width();
		double availableHeight = portalRect.height - entityDimensions.height();
		BlockPos lowerLeft = portalRect.lowerLeft;

		double normalizedWidth;
		if (availableWidth > 0.0) {
			double baseOffset = lowerLeft.getComponentAlongAxis(portalAxis) + entityDimensions.width() / 2.0;
			normalizedWidth = MathHelper.clamp(
				MathHelper.getLerpProgress(entityPos.getComponentAlongAxis(portalAxis) - baseOffset, 0.0, availableWidth),
				0.0, 1.0
			);
		} else {
			normalizedWidth = 0.5;
		}

		double normalizedHeight;
		if (availableHeight > 0.0) {
			normalizedHeight = MathHelper.clamp(
				MathHelper.getLerpProgress(
					entityPos.getComponentAlongAxis(Direction.Axis.Y) - lowerLeft.getComponentAlongAxis(Direction.Axis.Y),
					0.0, availableHeight
				),
				0.0, 1.0
			);
		} else {
			normalizedHeight = 0.0;
		}

		Direction.Axis perpAxis = portalAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		double perpOffset = entityPos.getComponentAlongAxis(perpAxis)
			- (lowerLeft.getComponentAlongAxis(perpAxis) + 0.5);

		return new Vec3d(normalizedWidth, normalizedHeight, perpOffset);
	}

	/**
	 * Ищет открытую позицию для размещения сущности рядом с порталом.
	 * Для крупных сущностей (ширина или высота > 4) возвращает исходную позицию без изменений.
	 *
	 * @param fallback   позиция по умолчанию, если свободное место не найдено
	 * @param world      серверный мир
	 * @param entity     сущность, для которой ищется позиция
	 * @param dimensions размеры сущности
	 * @return скорректированная позиция или fallback
	 */
	public static Vec3d findOpenPosition(
		Vec3d fallback,
		ServerWorld world,
		Entity entity,
		EntityDimensions dimensions
	) {
		if (dimensions.width() > FALLBACK_THRESHOLD || dimensions.height() > FALLBACK_THRESHOLD) {
			return fallback;
		}

		double halfHeight = dimensions.height() / 2.0;
		Vec3d center = fallback.add(0.0, halfHeight, 0.0);
		VoxelShape searchShape = VoxelShapes.cuboid(
			Box.of(center, dimensions.width(), 0.0, dimensions.width())
				.stretch(0.0, HEIGHT_STRETCH, 0.0)
				.expand(1.0E-6)
		);

		return world.findClosestCollision(
				entity,
				searchShape,
				center,
				dimensions.width(),
				dimensions.height(),
				dimensions.width()
			)
			.map(pos -> pos.subtract(0.0, halfHeight, 0.0))
			.orElse(fallback);
	}
}
