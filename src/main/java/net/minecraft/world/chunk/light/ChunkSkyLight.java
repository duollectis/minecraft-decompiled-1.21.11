package net.minecraft.world.chunk.light;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

/**
 * Хранит высоту поверхности для каждой колонки блоков (16×16) в чанке.
 * <p>
 * «Поверхность» — это Y-координата первого блока сверху, который блокирует небесный свет.
 * Значение {@link #NO_LIGHT_LEVEL} означает, что колонка полностью прозрачна до самого дна.
 * <p>
 * Данные хранятся в упакованном виде через {@link PaletteStorage} для экономии памяти.
 */
public class ChunkSkyLight {

	private static final int SECTION_SIZE = 16;
	public static final int NO_LIGHT_LEVEL = Integer.MIN_VALUE;

	private final int minY;
	private final PaletteStorage palette;
	private final BlockPos.Mutable reusableBlockPos1 = new BlockPos.Mutable();
	private final BlockPos.Mutable reusableBlockPos2 = new BlockPos.Mutable();

	public ChunkSkyLight(HeightLimitView heightLimitView) {
		minY = heightLimitView.getBottomY() - 1;
		int topY = heightLimitView.getTopYInclusive() + 1;
		int bitsNeeded = MathHelper.ceilLog2(topY - minY + 1);
		palette = new PackedIntegerArray(bitsNeeded, 256);
	}

	/**
	 * Пересчитывает высоту поверхности для всех 256 колонок чанка.
	 * Вызывается при загрузке чанка или изменении блоков, влияющих на небесный свет.
	 */
	public void refreshSurfaceY(Chunk chunk) {
		int topSectionIndex = chunk.getHighestNonEmptySection();

		if (topSectionIndex == -1) {
			fill(minY);
			return;
		}

		for (int localX = 0; localX < SECTION_SIZE; localX++) {
			for (int localZ = 0; localZ < SECTION_SIZE; localZ++) {
				int surfaceY = Math.max(calculateSurfaceY(chunk, topSectionIndex, localX, localZ), minY);
				set(getPackedIndex(localX, localZ), surfaceY);
			}
		}
	}

	/**
	 * Вычисляет Y-координату поверхности для одной колонки, сканируя секции сверху вниз.
	 * Останавливается на первом блоке, который блокирует свет снизу.
	 */
	private int calculateSurfaceY(Chunk chunk, int topSectionIndex, int localX, int localZ) {
		int startY = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(topSectionIndex) + 1);
		BlockPos.Mutable upper = reusableBlockPos1.set(localX, startY, localZ);
		BlockPos.Mutable lower = reusableBlockPos2.set(upper, Direction.DOWN);
		BlockState upperState = Blocks.AIR.getDefaultState();

		for (int sectionIndex = topSectionIndex; sectionIndex >= 0; sectionIndex--) {
			ChunkSection section = chunk.getSection(sectionIndex);

			if (section.isEmpty()) {
				upperState = Blocks.AIR.getDefaultState();
				int sectionY = chunk.sectionIndexToCoord(sectionIndex);
				upper.setY(ChunkSectionPos.getBlockCoord(sectionY));
				lower.setY(upper.getY() - 1);
			} else {
				for (int localY = 15; localY >= 0; localY--) {
					BlockState lowerState = section.getBlockState(localX, localY, localZ);

					if (faceBlocksLight(upperState, lowerState)) {
						return upper.getY();
					}

					upperState = lowerState;
					upper.set(lower);
					lower.move(Direction.DOWN);
				}
			}
		}

		return minY;
	}

	/**
	 * Проверяет, доступен ли небесный свет для блока на заданной Y-координате.
	 * Также обновляет высоту поверхности, если блок изменился.
	 */
	public boolean isSkyLightAccessible(BlockView blockView, int localX, int y, int localZ) {
		int aboveY = y + 1;
		int packedIndex = getPackedIndex(localX, localZ);
		int surfaceY = get(packedIndex);

		if (aboveY < surfaceY) {
			return false;
		}

		BlockPos upperPos = reusableBlockPos1.set(localX, y + 1, localZ);
		BlockState upperState = blockView.getBlockState(upperPos);
		BlockPos lowerPos = reusableBlockPos2.set(localX, y, localZ);
		BlockState lowerState = blockView.getBlockState(lowerPos);

		if (isSkyLightAccessible(blockView, packedIndex, surfaceY, upperPos, upperState, lowerPos, lowerState)) {
			return true;
		}

		BlockPos belowPos = reusableBlockPos1.set(localX, y - 1, localZ);
		BlockState belowState = blockView.getBlockState(belowPos);

		return isSkyLightAccessible(blockView, packedIndex, surfaceY, lowerPos, lowerState, belowPos, belowState);
	}

	private boolean isSkyLightAccessible(
		BlockView blockView,
		int packedIndex,
		int surfaceY,
		BlockPos upperPos,
		BlockState upperState,
		BlockPos lowerPos,
		BlockState lowerState
	) {
		int upperY = upperPos.getY();

		if (faceBlocksLight(upperState, lowerState)) {
			if (upperY > surfaceY) {
				set(packedIndex, upperY);
				return true;
			}
		} else if (upperY == surfaceY) {
			set(packedIndex, locateLightBlockingBlockBelow(blockView, lowerPos, lowerState));
			return true;
		}

		return false;
	}

	private int locateLightBlockingBlockBelow(BlockView blockView, BlockPos pos, BlockState blockState) {
		BlockPos.Mutable current = reusableBlockPos1.set(pos);
		BlockPos.Mutable below = reusableBlockPos2.set(pos, Direction.DOWN);
		BlockState currentState = blockState;

		while (below.getY() >= minY) {
			BlockState belowState = blockView.getBlockState(below);

			if (faceBlocksLight(currentState, belowState)) {
				return current.getY();
			}

			currentState = belowState;
			current.set(below);
			below.move(Direction.DOWN);
		}

		return minY;
	}

	private static boolean faceBlocksLight(BlockState upper, BlockState lower) {
		if (lower.getOpacity() != 0) {
			return true;
		}

		VoxelShape upperShape = ChunkLightProvider.getOpaqueShape(upper, Direction.DOWN);
		VoxelShape lowerShape = ChunkLightProvider.getOpaqueShape(lower, Direction.UP);

		return VoxelShapes.unionCoversFullCube(upperShape, lowerShape);
	}

	public int get(int localX, int localZ) {
		return convertMinY(get(getPackedIndex(localX, localZ)));
	}

	public int getMaxSurfaceY() {
		int maxRaw = Integer.MIN_VALUE;

		for (int index = 0; index < palette.getSize(); index++) {
			int raw = palette.get(index);

			if (raw > maxRaw) {
				maxRaw = raw;
			}
		}

		return convertMinY(maxRaw + minY);
	}

	private void fill(int y) {
		int relativeY = y - minY;

		for (int index = 0; index < palette.getSize(); index++) {
			palette.set(index, relativeY);
		}
	}

	private void set(int index, int y) {
		palette.set(index, y - minY);
	}

	private int get(int index) {
		return palette.get(index) + minY;
	}

	private int convertMinY(int y) {
		return y == minY ? Integer.MIN_VALUE : y;
	}

	private static int getPackedIndex(int localX, int localZ) {
		return localX + localZ * SECTION_SIZE;
	}
}
