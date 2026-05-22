package net.minecraft.world.dimension;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.Heightmap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Comparator;
import java.util.Optional;

/**
 * Отвечает за поиск существующих и принудительное создание новых порталов в Нижний мир.
 * При создании портала строит рамку из обсидиана 4×5 и заполняет её блоками портала 2×3.
 */
public class PortalForcer {

	public static final int PORTAL_WIDTH = 3;

	private static final int NETHER_SEARCH_RADIUS = 16;
	private static final int OVERWORLD_SEARCH_RADIUS = 128;
	private static final int PORTAL_HEIGHT = 3;
	private static final int FRAME_SIDE_START = -1;
	private static final int FRAME_SIDE_END = 3;
	private static final int FRAME_DEPTH_START = -1;
	private static final int FRAME_DEPTH_END = 2;
	private static final int FRAME_BOTTOM_OFFSET = -1;
	private static final int FRAME_TOP_OFFSET = 4;
	/** Минимальный запас высоты над порталом для размещения рамки. */
	private static final int FRAME_CLEARANCE = 9;
	/** Минимальная Y-координата для принудительного создания портала. */
	private static final int FORCED_PORTAL_MIN_Y = 70;

	private final ServerWorld world;

	public PortalForcer(ServerWorld world) {
		this.world = world;
	}

	/**
	 * Ищет ближайший существующий портал в радиусе поиска (16 блоков для Нижнего мира,
	 * 128 для Верхнего мира). Возвращает позицию ближайшего блока портала внутри границ мира.
	 *
	 * @param pos          центр поиска
	 * @param destIsNether true, если целевое измерение — Нижний мир (меньший радиус поиска)
	 * @param worldBorder  граница мира для фильтрации позиций
	 * @return позиция ближайшего портала или пустой Optional
	 */
	public Optional<BlockPos> getPortalPos(BlockPos pos, boolean destIsNether, WorldBorder worldBorder) {
		PointOfInterestStorage poiStorage = world.getPointOfInterestStorage();
		int searchRadius = destIsNether ? NETHER_SEARCH_RADIUS : OVERWORLD_SEARCH_RADIUS;
		poiStorage.preloadChunks(world, pos, searchRadius);

		return poiStorage.getInSquare(
				poiType -> poiType.matchesKey(PointOfInterestTypes.NETHER_PORTAL),
				pos,
				searchRadius,
				PointOfInterestStorage.OccupationStatus.ANY
			)
			.map(PointOfInterest::getPos)
			.filter(worldBorder::contains)
			.filter(portalPos -> world.getBlockState(portalPos).contains(Properties.HORIZONTAL_AXIS))
			.min(
				Comparator.<BlockPos>comparingDouble(portalPos -> portalPos.getSquaredDistance(pos))
					.thenComparingInt(Vec3i::getY)
			);
	}

	/**
	 * Создаёт новый портал в Нижний мир рядом с указанной позицией.
	 * Алгоритм:
	 * 1. Ищет подходящее место с твёрдым основанием и свободным пространством над ним.
	 * 2. Если место не найдено — принудительно размещает портал на безопасной высоте.
	 * 3. Строит рамку из обсидиана и заполняет её блоками портала.
	 *
	 * @param pos  желаемая позиция для создания портала
	 * @param axis ось ориентации портала (X или Z)
	 * @return прямоугольник созданного портала (2×3) или пустой Optional, если место не найдено
	 */
	public Optional<BlockLocating.Rectangle> createPortal(BlockPos pos, Direction.Axis axis) {
		Direction portalDir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
		int maxY = Math.min(world.getTopYInclusive(), world.getBottomY() + world.getLogicalHeight() - 1);

		double bestScore = -1.0;
		BlockPos bestPos = null;
		double fallbackScore = -1.0;
		BlockPos fallbackPos = null;

		BlockPos.Mutable temp = pos.mutableCopy();

		for (BlockPos.Mutable candidate : BlockPos.iterateInSquare(pos, NETHER_SEARCH_RADIUS, Direction.EAST, Direction.SOUTH)) {
			int surfaceY = Math.min(maxY, world.getTopY(Heightmap.Type.MOTION_BLOCKING, candidate.getX(), candidate.getZ()));
			if (!world.getWorldBorder().contains(candidate)
				|| !world.getWorldBorder().contains(candidate.move(portalDir, 1))
			) {
				continue;
			}

			candidate.move(portalDir.getOpposite(), 1);

			for (int y = surfaceY; y >= world.getBottomY(); y--) {
				candidate.setY(y);
				if (!isBlockStateValid(candidate)) {
					continue;
				}

				int solidY = y;
				while (y > world.getBottomY() && isBlockStateValid(candidate.move(Direction.DOWN))) {
					y--;
				}

				if (y + FRAME_TOP_OFFSET > maxY) {
					continue;
				}

				int airColumnHeight = solidY - y;
				if (airColumnHeight > 0 && airColumnHeight < PORTAL_HEIGHT) {
					continue;
				}

				candidate.setY(y);
				if (!isValidPortalPos(candidate, temp, portalDir, 0)) {
					continue;
				}

				double score = pos.getSquaredDistance(candidate);
				if (isValidPortalPos(candidate, temp, portalDir, FRAME_BOTTOM_OFFSET)
					&& isValidPortalPos(candidate, temp, portalDir, 1)
					&& (bestScore == -1.0 || bestScore > score)
				) {
					bestScore = score;
					bestPos = candidate.toImmutable();
				}

				if (bestScore == -1.0 && (fallbackScore == -1.0 || fallbackScore > score)) {
					fallbackScore = score;
					fallbackPos = candidate.toImmutable();
				}
			}
		}

		if (bestScore == -1.0 && fallbackScore != -1.0) {
			bestPos = fallbackPos;
			bestScore = fallbackScore;
		}

		if (bestScore == -1.0) {
			bestPos = buildForcedPortalBase(pos, portalDir, maxY, temp);
			if (bestPos == null) {
				return Optional.empty();
			}
		}

		buildObsidianFrame(bestPos, portalDir, temp);

		BlockState portalState = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis);
		for (int depthOffset = 0; depthOffset < 2; depthOffset++) {
			for (int heightOffset = 0; heightOffset < PORTAL_HEIGHT; heightOffset++) {
				temp.set(
					bestPos,
					depthOffset * portalDir.getOffsetX(),
					heightOffset,
					depthOffset * portalDir.getOffsetZ()
				);
				world.setBlockState(temp, portalState, 18);
			}
		}

		return Optional.of(new BlockLocating.Rectangle(bestPos.toImmutable(), 2, PORTAL_HEIGHT));
	}

	/**
	 * Принудительно размещает основание портала на безопасной высоте, если подходящее место не найдено.
	 * Расчищает пространство 2×2×3 (воздух) и кладёт обсидиановый пол.
	 *
	 * @param pos       желаемая позиция
	 * @param portalDir направление портала
	 * @param maxY      максимальная допустимая Y-координата
	 * @param temp      изменяемый BlockPos для временных вычислений
	 * @return позиция основания или null, если высота не позволяет разместить портал
	 */
	private @org.jspecify.annotations.Nullable BlockPos buildForcedPortalBase(
		BlockPos pos,
		Direction portalDir,
		int maxY,
		BlockPos.Mutable temp
	) {
		int minSafeY = Math.max(world.getBottomY() - FRAME_BOTTOM_OFFSET, FORCED_PORTAL_MIN_Y);
		int maxSafeY = maxY - FRAME_CLEARANCE;
		if (maxSafeY < minSafeY) {
			return null;
		}

		BlockPos basePos = new BlockPos(
			pos.getX() - portalDir.getOffsetX(),
			MathHelper.clamp(pos.getY(), minSafeY, maxSafeY),
			pos.getZ() - portalDir.getOffsetZ()
		).toImmutable();
		basePos = world.getWorldBorder().clampFloored(basePos);

		Direction sideDir = portalDir.rotateYClockwise();
		for (int side = FRAME_SIDE_START; side < 2; side++) {
			for (int depth = 0; depth < 2; depth++) {
				for (int height = FRAME_BOTTOM_OFFSET; height < FRAME_SIDE_END; height++) {
					BlockState fill = height < 0 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState();
					temp.set(
						basePos,
						depth * portalDir.getOffsetX() + side * sideDir.getOffsetX(),
						height,
						depth * portalDir.getOffsetZ() + side * sideDir.getOffsetZ()
					);
					world.setBlockState(temp, fill);
				}
			}
		}

		return basePos;
	}

	/**
	 * Строит рамку из обсидиана вокруг позиции портала.
	 * Рамка охватывает периметр прямоугольника 4×5 (ширина × высота).
	 *
	 * @param portalBase позиция нижнего левого угла портала
	 * @param portalDir  направление портала
	 * @param temp       изменяемый BlockPos для временных вычислений
	 */
	private void buildObsidianFrame(BlockPos portalBase, Direction portalDir, BlockPos.Mutable temp) {
		for (int side = FRAME_SIDE_START; side < FRAME_SIDE_END; side++) {
			for (int height = FRAME_BOTTOM_OFFSET; height < FRAME_TOP_OFFSET; height++) {
				if (side == FRAME_SIDE_START || side == 2 || height == FRAME_BOTTOM_OFFSET || height == FRAME_SIDE_END) {
					temp.set(portalBase, side * portalDir.getOffsetX(), height, side * portalDir.getOffsetZ());
					world.setBlockState(temp, Blocks.OBSIDIAN.getDefaultState(), 3);
				}
			}
		}
	}

	private boolean isBlockStateValid(BlockPos.Mutable pos) {
		BlockState state = world.getBlockState(pos);
		return state.isReplaceable() && state.getFluidState().isEmpty();
	}

	/**
	 * Проверяет, что позиция пригодна для размещения портала:
	 * блоки ниже основания — твёрдые, блоки в области портала — заменяемые и без жидкости.
	 *
	 * @param pos                    базовая позиция портала
	 * @param temp                   изменяемый BlockPos для временных вычислений
	 * @param portalDirection        направление портала
	 * @param orthogonalOffset       смещение по перпендикулярной оси (для проверки соседних колонн)
	 * @return true, если позиция пригодна для портала
	 */
	private boolean isValidPortalPos(
		BlockPos pos,
		BlockPos.Mutable temp,
		Direction portalDirection,
		int orthogonalOffset
	) {
		Direction sideDir = portalDirection.rotateYClockwise();

		for (int depth = FRAME_SIDE_START; depth < FRAME_SIDE_END; depth++) {
			for (int height = FRAME_BOTTOM_OFFSET; height < FRAME_TOP_OFFSET; height++) {
				temp.set(
					pos,
					portalDirection.getOffsetX() * depth + sideDir.getOffsetX() * orthogonalOffset,
					height,
					portalDirection.getOffsetZ() * depth + sideDir.getOffsetZ() * orthogonalOffset
				);

				if (height < 0 && !world.getBlockState(temp).isSolid()) {
					return false;
				}

				if (height >= 0 && !isBlockStateValid(temp)) {
					return false;
				}
			}
		}

		return true;
	}
}
