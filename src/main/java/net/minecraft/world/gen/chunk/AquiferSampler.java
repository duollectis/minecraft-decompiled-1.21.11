package net.minecraft.world.gen.chunk;

import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Сэмплер водоносных горизонтов (аквиферов). Определяет, какой блок жидкости
 * (вода или лава) должен быть размещён в данной позиции при генерации пещер.
 * <p>
 * Реализация {@link Impl} использует трёхмерную сетку случайных точек для
 * создания органично выглядящих водоносных горизонтов разных уровней.
 */
public interface AquiferSampler {

	static AquiferSampler aquifer(
			ChunkNoiseSampler chunkNoiseSampler,
			ChunkPos chunkPos,
			NoiseRouter noiseRouter,
			RandomSplitter randomSplitter,
			int minimumY,
			int height,
			AquiferSampler.FluidLevelSampler fluidLevelSampler
	) {
		return new AquiferSampler.Impl(
				chunkNoiseSampler,
				chunkPos,
				noiseRouter,
				randomSplitter,
				minimumY,
				height,
				fluidLevelSampler
		);
	}

	static AquiferSampler seaLevel(AquiferSampler.FluidLevelSampler fluidLevelSampler) {
		return new AquiferSampler() {
			@Override
			public @Nullable BlockState apply(DensityFunction.NoisePos pos, double density) {
				return density > 0.0 ? null : fluidLevelSampler
				                              .getFluidLevel(pos.blockX(), pos.blockY(), pos.blockZ())
				                              .getBlockState(pos.blockY());
			}

			@Override
			public boolean needsFluidTick() {
				return false;
			}
		};
	}

	@Nullable BlockState apply(DensityFunction.NoisePos pos, double density);

	boolean needsFluidTick();

	/** Уровень жидкости: Y-координата поверхности и тип блока жидкости. */
	public record FluidLevel(int y, BlockState state) {

		public BlockState getBlockState(int blockY) {
			return blockY < y ? state : Blocks.AIR.getDefaultState();
		}
	}

	/** Поставщик уровней жидкости для заданной позиции в мире. */
	public interface FluidLevelSampler {

		AquiferSampler.FluidLevel getFluidLevel(int x, int y, int z);
	}

	/**
	 * Полная реализация аквифера на основе трёхмерной сетки случайных точек.
	 * Для каждого блока находит 4 ближайшие точки сетки и интерполирует между
	 * их уровнями жидкости, создавая плавные границы водоносных горизонтов.
	 */
	public static class Impl implements AquiferSampler {

		private static final int GRID_SIZE_X = 10;
		private static final int GRID_SIZE_Y = 9;
		private static final int GRID_SIZE_Z = 10;
		private static final int GRID_STEP_X = 6;
		private static final int GRID_STEP_Y = 3;
		private static final int GRID_STEP_Z = 6;
		private static final int BLOCK_STEP_X = 16;
		private static final int BLOCK_STEP_Y = 12;
		private static final int BLOCK_STEP_Z = 16;
		private static final int GRID_HALF_X = 4;
		private static final int GRID_HALF_Z = 4;
		private static final int Y_SCALE = 11;
		private static final double
				NEEDS_FLUID_TICK_DISTANCE_THRESHOLD =
				maxDistance(MathHelper.square(10), MathHelper.square(BLOCK_STEP_Y));
		private static final int START_X_OFFSET = -5;
		private static final int START_X_STEP = 1;
		private static final int START_Z_OFFSET = -5;
		private static final int START_Z_STEP = 0;
		private static final int START_Y_OFFSET = -1;
		private static final int START_Y_STEP = 0;
		private static final int END_X_STEP = 1;
		private static final int END_Y_STEP = 1;
		private static final int END_Z_STEP = 1;
		private final ChunkNoiseSampler chunkNoiseSampler;
		private final DensityFunction barrierNoise;
		private final DensityFunction fluidLevelFloodednessNoise;
		private final DensityFunction fluidLevelSpreadNoise;
		private final DensityFunction fluidTypeNoise;
		private final RandomSplitter randomDeriver;
		private final AquiferSampler.@Nullable FluidLevel[] waterLevels;
		private final long[] blockPositions;
		private final AquiferSampler.FluidLevelSampler fluidLevelSampler;
		private final DensityFunction erosionDensityFunction;
		private final DensityFunction depthDensityFunction;
		private boolean needsFluidTick;
		private final int maxLocalY;
		private final int startX;
		private final int startY;
		private final int startZ;
		private final int sizeX;
		private final int sizeZ;
		private static final int[][] CHUNK_POS_OFFSETS = new int[][]{
				{0, 0},
				{-2, -1},
				{-1, -1},
				{0, -1},
				{1, -1},
				{-3, 0},
				{-2, 0},
				{-1, 0},
				{1, 0},
				{-2, 1},
				{-1, 1},
				{0, 1},
				{1, 1}
		};

		Impl(
				ChunkNoiseSampler chunkNoiseSampler,
				ChunkPos chunkPos,
				NoiseRouter noiseRouter,
				RandomSplitter randomSplitter,
				int minimumY,
				int height,
				AquiferSampler.FluidLevelSampler fluidLevelSampler
		) {
			this.chunkNoiseSampler = chunkNoiseSampler;
			this.barrierNoise = noiseRouter.barrierNoise();
			this.fluidLevelFloodednessNoise = noiseRouter.fluidLevelFloodednessNoise();
			this.fluidLevelSpreadNoise = noiseRouter.fluidLevelSpreadNoise();
			this.fluidTypeNoise = noiseRouter.lavaNoise();
			this.erosionDensityFunction = noiseRouter.erosion();
			this.depthDensityFunction = noiseRouter.depth();
			this.randomDeriver = randomSplitter;
			this.startX = getLocalX(chunkPos.getStartX() + START_X_OFFSET) + START_X_STEP;
			this.fluidLevelSampler = fluidLevelSampler;
			int endLocalX = getLocalX(chunkPos.getEndX() + START_X_OFFSET) + END_X_STEP;
			this.sizeX = endLocalX - this.startX + END_X_STEP;
			this.startY = getLocalY(minimumY + 1) + START_Y_OFFSET;
			int endLocalY = getLocalY(minimumY + height + 1) + END_Y_STEP;
			int sizeY = endLocalY - this.startY + END_Y_STEP;
			this.startZ = getLocalZ(chunkPos.getStartZ() + START_Z_OFFSET) + START_Z_STEP;
			int endLocalZ = getLocalZ(chunkPos.getEndZ() + START_Z_OFFSET) + END_Z_STEP;
			this.sizeZ = endLocalZ - this.startZ + END_Z_STEP;
			int totalCells = this.sizeX * sizeY * this.sizeZ;
			this.waterLevels = new AquiferSampler.FluidLevel[totalCells];
			this.blockPositions = new long[totalCells];
			Arrays.fill(this.blockPositions, Long.MAX_VALUE);
			int highestSurface = this.addSurfaceOffset(
					chunkNoiseSampler.estimateHighestSurfaceLevel(
							toWorldX(this.startX, 0),
							toWorldZ(this.startZ, 0),
							toWorldX(endLocalX, 9),
							toWorldZ(endLocalZ, 9)
					)
			);
			int maxLocalYGrid = getLocalY(highestSurface + BLOCK_STEP_Y) - START_Y_OFFSET;
			this.maxLocalY = toWorldY(maxLocalYGrid, Y_SCALE) - 1;
		}

		private int index(int x, int y, int z) {
			int localX = x - startX;
			int localY = y - startY;
			int localZ = z - startZ;
			return (localY * sizeZ + localZ) * sizeX + localX;
		}

		@Override
		public @Nullable BlockState apply(DensityFunction.NoisePos pos, double density) {
			if (density > 0.0) {
				needsFluidTick = false;
				return null;
			}

			int blockX = pos.blockX();
			int blockY = pos.blockY();
			int blockZ = pos.blockZ();
			AquiferSampler.FluidLevel globalFluidLevel = fluidLevelSampler.getFluidLevel(blockX, blockY, blockZ);

			if (blockY > maxLocalY) {
				needsFluidTick = false;
				return globalFluidLevel.getBlockState(blockY);
			}

			if (globalFluidLevel.getBlockState(blockY).isOf(Blocks.LAVA)) {
				needsFluidTick = false;
				return SharedConstants.DISABLE_FLUID_GENERATION
						? Blocks.AIR.getDefaultState()
						: Blocks.LAVA.getDefaultState();
			}

			int gridX = getLocalX(blockX + START_X_OFFSET);
			int gridY = getLocalY(blockY + 1);
			int gridZ = getLocalZ(blockZ + START_Z_OFFSET);

			// Индексы четырёх ближайших точек сетки и их квадраты расстояний
			int dist1 = Integer.MAX_VALUE;
			int dist2 = Integer.MAX_VALUE;
			int dist3 = Integer.MAX_VALUE;
			int dist4 = Integer.MAX_VALUE;
			int cell1 = 0;
			int cell2 = 0;
			int cell3 = 0;
			int cell4 = 0;

			for (int dx = START_Z_STEP; dx <= END_X_STEP; dx++) {
				for (int dy = -1; dy <= END_Y_STEP; dy++) {
					for (int dz = START_Z_STEP; dz <= END_Z_STEP; dz++) {
						int cellX = gridX + dx;
						int cellY = gridY + dy;
						int cellZ = gridZ + dz;
						int cellIndex = index(cellX, cellY, cellZ);
						long cachedPos = blockPositions[cellIndex];
						long packedPos;

						if (cachedPos != Long.MAX_VALUE) {
							packedPos = cachedPos;
						}
						else {
							Random random = randomDeriver.split(cellX, cellY, cellZ);
							packedPos = BlockPos.asLong(
									toWorldX(cellX, random.nextInt(GRID_SIZE_X)),
									toWorldY(cellY, random.nextInt(GRID_SIZE_Y)),
									toWorldZ(cellZ, random.nextInt(GRID_SIZE_Z))
							);
							blockPositions[cellIndex] = packedPos;
						}

						int relX = BlockPos.unpackLongX(packedPos) - blockX;
						int relY = BlockPos.unpackLongY(packedPos) - blockY;
						int relZ = BlockPos.unpackLongZ(packedPos) - blockZ;
						int squaredDist = relX * relX + relY * relY + relZ * relZ;

						if (dist1 >= squaredDist) {
							cell4 = cell3;
							cell3 = cell2;
							cell2 = cell1;
							cell1 = cellIndex;
							dist4 = dist3;
							dist3 = dist2;
							dist2 = dist1;
							dist1 = squaredDist;
						}
						else if (dist2 >= squaredDist) {
							cell4 = cell3;
							cell3 = cell2;
							cell2 = cellIndex;
							dist4 = dist3;
							dist3 = dist2;
							dist2 = squaredDist;
						}
						else if (dist3 >= squaredDist) {
							cell4 = cell3;
							cell3 = cellIndex;
							dist4 = dist3;
							dist3 = squaredDist;
						}
						else if (dist4 >= squaredDist) {
							cell4 = cellIndex;
							dist4 = squaredDist;
						}
					}
				}
			}

			AquiferSampler.FluidLevel nearest = getWaterLevel(cell1);
			double borderFactor12 = maxDistance(dist1, dist2);
			BlockState nearestState = nearest.getBlockState(blockY);
			BlockState resultState = SharedConstants.DISABLE_FLUID_GENERATION
					? Blocks.AIR.getDefaultState()
					: nearestState;

			if (borderFactor12 <= 0.0) {
				if (borderFactor12 >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD) {
					AquiferSampler.FluidLevel secondNearest = getWaterLevel(cell2);
					needsFluidTick = !nearest.equals(secondNearest);
				}
				else {
					needsFluidTick = false;
				}

				return resultState;
			}

			if (nearestState.isOf(Blocks.WATER)
					&& fluidLevelSampler.getFluidLevel(blockX, blockY - 1, blockZ)
					.getBlockState(blockY - 1)
					.isOf(Blocks.LAVA)
			) {
				needsFluidTick = true;
				return resultState;
			}

			MutableDouble barrierCache = new MutableDouble(Double.NaN);
			AquiferSampler.FluidLevel second = getWaterLevel(cell2);
			double density12 = borderFactor12 * calculateDensity(pos, barrierCache, nearest, second);

			if (density + density12 > 0.0) {
				needsFluidTick = false;
				return null;
			}

			AquiferSampler.FluidLevel third = getWaterLevel(cell3);
			double borderFactor13 = maxDistance(dist1, dist3);

			if (borderFactor13 > 0.0) {
				double density13 = borderFactor12 * borderFactor13 * calculateDensity(pos, barrierCache, nearest, third);
				if (density + density13 > 0.0) {
					needsFluidTick = false;
					return null;
				}
			}

			double borderFactor23 = maxDistance(dist2, dist3);

			if (borderFactor23 > 0.0) {
				double density23 = borderFactor12 * borderFactor23 * calculateDensity(pos, barrierCache, second, third);
				if (density + density23 > 0.0) {
					needsFluidTick = false;
					return null;
				}
			}

			boolean differentFluid12 = !nearest.equals(second);
			boolean differentFluid23 = borderFactor23 >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD && !second.equals(third);
			boolean differentFluid13 = borderFactor13 >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD && !nearest.equals(third);

			if (differentFluid12 || differentFluid23 || differentFluid13) {
				needsFluidTick = true;
			}
			else {
				needsFluidTick = borderFactor13 >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD
						&& maxDistance(dist1, dist4) >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD
						&& !nearest.equals(getWaterLevel(cell4));
			}

			return resultState;
		}

		@Override
		public boolean needsFluidTick() {
			return this.needsFluidTick;
		}

		private static double maxDistance(int i, int a) {
			double d = 25.0;
			return 1.0 - (a - i) / 25.0;
		}

		private double calculateDensity(
				DensityFunction.NoisePos pos,
				MutableDouble barrierCache,
				AquiferSampler.FluidLevel levelA,
				AquiferSampler.FluidLevel levelB
		) {
			int blockY = pos.blockY();
			BlockState stateA = levelA.getBlockState(blockY);
			BlockState stateB = levelB.getBlockState(blockY);

			// Граница вода/лава — всегда максимальная плотность барьера
			boolean isLavaWaterBorder = (stateA.isOf(Blocks.LAVA) && stateB.isOf(Blocks.WATER))
					|| (stateA.isOf(Blocks.WATER) && stateB.isOf(Blocks.LAVA));

			if (isLavaWaterBorder) {
				return 2.0;
			}

			int levelDiff = Math.abs(levelA.y - levelB.y);

			if (levelDiff == 0) {
				return 0.0;
			}

			double midY = 0.5 * (levelA.y + levelB.y);
			double relativeY = blockY + 0.5 - midY;
			double halfDiff = levelDiff / 2.0;
			double distanceToEdge = halfDiff - Math.abs(relativeY);

			double normalizedQ;

			if (relativeY > 0.0) {
				double shifted = distanceToEdge;
				normalizedQ = shifted > 0.0 ? shifted / 1.5 : shifted / 2.5;
			}
			else {
				double shifted = 3.0 + distanceToEdge;
				normalizedQ = shifted > 0.0 ? shifted / 3.0 : shifted / 10.0;
			}

			// Если нормализованное значение вне диапазона [-2, 2], барьерный шум не нужен
			double barrierNoiseSample;

			if (normalizedQ >= -2.0 && normalizedQ <= 2.0) {
				double cached = barrierCache.doubleValue();
				if (Double.isNaN(cached)) {
					double sampled = barrierNoise.sample(pos);
					barrierCache.setValue(sampled);
					barrierNoiseSample = sampled;
				}
				else {
					barrierNoiseSample = cached;
				}
			}
			else {
				barrierNoiseSample = 0.0;
			}

			return 2.0 * (barrierNoiseSample + normalizedQ);
		}

		private static int getLocalX(int i) {
			return i >> 4;
		}

		private static int toWorldX(int i, int j) {
			return (i << 4) + j;
		}

		private static int getLocalY(int i) {
			return Math.floorDiv(i, BLOCK_STEP_Y);
		}

		private static int toWorldY(int i, int j) {
			return i * BLOCK_STEP_Y + j;
		}

		private static int getLocalZ(int i) {
			return i >> 4;
		}

		private static int toWorldZ(int i, int j) {
			return (i << 4) + j;
		}

		private AquiferSampler.FluidLevel getWaterLevel(int cellIndex) {
			AquiferSampler.FluidLevel cached = waterLevels[cellIndex];

			if (cached != null) {
				return cached;
			}

			long packedPos = blockPositions[cellIndex];
			AquiferSampler.FluidLevel computed = getFluidLevel(
					BlockPos.unpackLongX(packedPos),
					BlockPos.unpackLongY(packedPos),
					BlockPos.unpackLongZ(packedPos)
			);
			waterLevels[cellIndex] = computed;
			return computed;
		}

		private AquiferSampler.FluidLevel getFluidLevel(int blockX, int blockY, int blockZ) {
			AquiferSampler.FluidLevel defaultLevel = fluidLevelSampler.getFluidLevel(blockX, blockY, blockZ);
			int minSurfaceHeight = Integer.MAX_VALUE;
			int aboveY = blockY + BLOCK_STEP_Y;
			int belowY = blockY - BLOCK_STEP_Y;
			boolean isCurrentChunkNearSurface = false;

			for (int[] offset : CHUNK_POS_OFFSETS) {
				int neighborX = blockX + ChunkSectionPos.getBlockCoord(offset[0]);
				int neighborZ = blockZ + ChunkSectionPos.getBlockCoord(offset[1]);
				int surfaceHeight = chunkNoiseSampler.estimateSurfaceHeight(neighborX, neighborZ);
				int surfaceWithOffset = addSurfaceOffset(surfaceHeight);
				boolean isCurrentChunk = offset[0] == 0 && offset[1] == 0;

				if (isCurrentChunk && belowY > surfaceWithOffset) {
					return defaultLevel;
				}

				boolean isAboveSurface = aboveY > surfaceWithOffset;

				if (isAboveSurface || isCurrentChunk) {
					AquiferSampler.FluidLevel neighborLevel = fluidLevelSampler.getFluidLevel(neighborX, surfaceWithOffset, neighborZ);
					if (!neighborLevel.getBlockState(surfaceWithOffset).isAir()) {
						if (isCurrentChunk) {
							isCurrentChunkNearSurface = true;
						}

						if (isAboveSurface) {
							return neighborLevel;
						}
					}
				}

				minSurfaceHeight = Math.min(minSurfaceHeight, surfaceHeight);
			}

			int fluidY = getFluidBlockY(blockX, blockY, blockZ, defaultLevel, minSurfaceHeight, isCurrentChunkNearSurface);
			return new AquiferSampler.FluidLevel(fluidY, getFluidBlockState(blockX, blockY, blockZ, defaultLevel, fluidY));
		}

		private int addSurfaceOffset(int i) {
			return i + 8;
		}

		private int getFluidBlockY(
				int blockX,
				int blockY,
				int blockZ,
				AquiferSampler.FluidLevel defaultFluidLevel,
				int surfaceHeightEstimate,
				boolean nearSurface
		) {
			DensityFunction.UnblendedNoisePos noisePos = new DensityFunction.UnblendedNoisePos(blockX, blockY, blockZ);

			double floodednessLow;
			double floodednessHigh;

			if (VanillaBiomeParameters.inDeepDarkParameters(erosionDensityFunction, depthDensityFunction, noisePos)) {
				floodednessLow = -1.0;
				floodednessHigh = -1.0;
			}
			else {
				int depthBelowSurface = surfaceHeightEstimate + 8 - blockY;
				double surfaceProximity = nearSurface
						? MathHelper.clampedMap(depthBelowSurface, 0.0, 64.0, 1.0, 0.0)
						: 0.0;
				double floodedness = MathHelper.clamp(fluidLevelFloodednessNoise.sample(noisePos), -1.0, 1.0);
				double thresholdHigh = MathHelper.map(surfaceProximity, 1.0, 0.0, -0.3, 0.8);
				double thresholdLow = MathHelper.map(surfaceProximity, 1.0, 0.0, -0.8, 0.4);
				floodednessLow = floodedness - thresholdLow;
				floodednessHigh = floodedness - thresholdHigh;
			}

			if (floodednessHigh > 0.0) {
				return defaultFluidLevel.y;
			}

			if (floodednessLow > 0.0) {
				return getNoiseBasedFluidLevel(blockX, blockY, blockZ, surfaceHeightEstimate);
			}

			return DimensionType.MIN_HEIGHT_IN_BLOCKS;
		}

		private int getNoiseBasedFluidLevel(int blockX, int blockY, int blockZ, int surfaceHeightEstimate) {
			int cellX = Math.floorDiv(blockX, 16);
			int cellY = Math.floorDiv(blockY, 40);
			int cellZ = Math.floorDiv(blockZ, 16);
			int baseLevelY = cellY * 40 + 20;
			double spreadNoise = fluidLevelSpreadNoise.sample(new DensityFunction.UnblendedNoisePos(cellX, cellY, cellZ)) * 10.0;
			int spreadOffset = MathHelper.roundDownToMultiple(spreadNoise, 3);
			int noiseLevelY = baseLevelY + spreadOffset;
			return Math.min(surfaceHeightEstimate, noiseLevelY);
		}

		private BlockState getFluidBlockState(
				int blockX,
				int blockY,
				int blockZ,
				AquiferSampler.FluidLevel defaultFluidLevel,
				int fluidLevel
		) {
			BlockState blockState = defaultFluidLevel.state;
			if (fluidLevel <= -10 && fluidLevel != DimensionType.MIN_HEIGHT_IN_BLOCKS
					&& defaultFluidLevel.state != Blocks.LAVA.getDefaultState()) {
				int k = Math.floorDiv(blockX, BLOCK_STEP_X * GRID_HALF_X);
				int l = Math.floorDiv(blockY, 40);
				int m = Math.floorDiv(blockZ, BLOCK_STEP_Z * GRID_HALF_Z);
				double d = this.fluidTypeNoise.sample(new DensityFunction.UnblendedNoisePos(k, l, m));
				if (Math.abs(d) > 0.3) {
					blockState = Blocks.LAVA.getDefaultState();
				}
			}

			return blockState;
		}
	}
}
