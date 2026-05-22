package net.minecraft.world.gen.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChainedBlockSource;
import net.minecraft.world.gen.OreVeinSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Сэмплер шума для генерации чанка. Управляет трёхмерной интерполяцией плотности,
 * кэшированием результатов и координирует работу аквифера, блендера и рудных жил.
 * <p>
 * Внутренние классы реализуют различные стратегии кэширования функций плотности:
 * {@link DensityInterpolator} — трилинейная интерполяция по ячейкам,
 * {@link FlatCache} — плоский 2D-кэш по биомным координатам,
 * {@link Cache2D} — кэш по XZ-колонке,
 * {@link CacheOnce} — кэш на один проход,
 * {@link CellCache} — кэш на всю ячейку.
 */
public class ChunkNoiseSampler implements DensityFunction.EachApplier, DensityFunction.NoisePos {

	final int horizontalCellCount;
	final int verticalCellCount;
	final int minimumCellY;
	private final int startCellX;
	private final int startCellZ;
	final int startBiomeX;
	final int startBiomeZ;
	final List<ChunkNoiseSampler.DensityInterpolator> interpolators;
	final List<ChunkNoiseSampler.CellCache> caches;
	private final Map<DensityFunction, DensityFunction> actualDensityFunctionCache = new HashMap<>();
	private final Long2IntMap surfaceHeightEstimateCache = new Long2IntOpenHashMap();
	private final AquiferSampler aquiferSampler;
	private final DensityFunction preliminarySurfaceLevel;
	private final ChunkNoiseSampler.BlockStateSampler blockStateSampler;
	private final Blender blender;
	private final ChunkNoiseSampler.FlatCache cachedBlendAlphaDensityFunction;
	private final ChunkNoiseSampler.FlatCache cachedBlendOffsetDensityFunction;
	private final DensityFunctionTypes.Beardifying beardifying;
	private long lastBlendingColumnPos = ChunkPos.MARKER;
	private Blender.BlendResult lastBlendingResult = new Blender.BlendResult(1.0, 0.0);
	final int horizontalBiomeEnd;
	final int horizontalCellBlockCount;
	final int verticalCellBlockCount;
	boolean isInInterpolationLoop;
	boolean isSamplingForCaches;
	private int startBlockX;
	int startBlockY;
	private int startBlockZ;
	int cellBlockX;
	int cellBlockY;
	int cellBlockZ;
	long sampleUniqueIndex;
	long cacheOnceUniqueIndex;
	int index;
	private final DensityFunction.EachApplier interpolationEachApplier = new DensityFunction.EachApplier() {
		@Override
		public DensityFunction.NoisePos at(int index) {
			ChunkNoiseSampler.this.startBlockY =
					(index + ChunkNoiseSampler.this.minimumCellY) * ChunkNoiseSampler.this.verticalCellBlockCount;
			ChunkNoiseSampler.this.sampleUniqueIndex++;
			ChunkNoiseSampler.this.cellBlockY = 0;
			ChunkNoiseSampler.this.index = index;
			return ChunkNoiseSampler.this;
		}

		@Override
		public void fill(double[] densities, DensityFunction densityFunction) {
			for (int i = 0; i < ChunkNoiseSampler.this.verticalCellCount + 1; i++) {
				ChunkNoiseSampler.this.startBlockY =
						(i + ChunkNoiseSampler.this.minimumCellY) * ChunkNoiseSampler.this.verticalCellBlockCount;
				ChunkNoiseSampler.this.sampleUniqueIndex++;
				ChunkNoiseSampler.this.cellBlockY = 0;
				ChunkNoiseSampler.this.index = i;
				densities[i] = densityFunction.sample(ChunkNoiseSampler.this);
			}
		}
	};

	public static ChunkNoiseSampler create(
			Chunk chunk,
			NoiseConfig noiseConfig,
			DensityFunctionTypes.Beardifying beardifying,
			ChunkGeneratorSettings chunkGeneratorSettings,
			AquiferSampler.FluidLevelSampler fluidLevelSampler,
			Blender blender
	) {
		GenerationShapeConfig shapeConfig = chunkGeneratorSettings.generationShapeConfig().trimHeight(chunk);
		ChunkPos chunkPos = chunk.getPos();
		int horizontalCellCount = 16 / shapeConfig.horizontalCellBlockCount();
		return new ChunkNoiseSampler(
				horizontalCellCount,
				noiseConfig,
				chunkPos.getStartX(),
				chunkPos.getStartZ(),
				shapeConfig,
				beardifying,
				chunkGeneratorSettings,
				fluidLevelSampler,
				blender
		);
	}

	public ChunkNoiseSampler(
			int horizontalCellCount,
			NoiseConfig noiseConfig,
			int startBlockX,
			int startBlockZ,
			GenerationShapeConfig generationShapeConfig,
			DensityFunctionTypes.Beardifying beardifying,
			ChunkGeneratorSettings chunkGeneratorSettings,
			AquiferSampler.FluidLevelSampler fluidLevelSampler,
			Blender blender
	) {
		this.horizontalCellBlockCount = generationShapeConfig.horizontalCellBlockCount();
		this.verticalCellBlockCount = generationShapeConfig.verticalCellBlockCount();
		this.horizontalCellCount = horizontalCellCount;
		this.verticalCellCount = MathHelper.floorDiv(generationShapeConfig.height(), this.verticalCellBlockCount);
		this.minimumCellY = MathHelper.floorDiv(generationShapeConfig.minimumY(), this.verticalCellBlockCount);
		this.startCellX = Math.floorDiv(startBlockX, this.horizontalCellBlockCount);
		this.startCellZ = Math.floorDiv(startBlockZ, this.horizontalCellBlockCount);
		this.interpolators = Lists.newArrayList();
		this.caches = Lists.newArrayList();
		this.startBiomeX = BiomeCoords.fromBlock(startBlockX);
		this.startBiomeZ = BiomeCoords.fromBlock(startBlockZ);
		this.horizontalBiomeEnd = BiomeCoords.fromBlock(horizontalCellCount * this.horizontalCellBlockCount);
		this.blender = blender;
		this.beardifying = beardifying;
		this.cachedBlendAlphaDensityFunction =
				new ChunkNoiseSampler.FlatCache(new ChunkNoiseSampler.BlendAlphaDensityFunction(), false);
		this.cachedBlendOffsetDensityFunction =
				new ChunkNoiseSampler.FlatCache(new ChunkNoiseSampler.BlendOffsetDensityFunction(), false);
		if (!blender.isEmpty()) {
			for (int biomeOffX = 0; biomeOffX <= this.horizontalBiomeEnd; biomeOffX++) {
				int biomeX = this.startBiomeX + biomeOffX;
				int blockX = BiomeCoords.toBlock(biomeX);

				for (int biomeOffZ = 0; biomeOffZ <= this.horizontalBiomeEnd; biomeOffZ++) {
					int biomeZ = this.startBiomeZ + biomeOffZ;
					int blockZ = BiomeCoords.toBlock(biomeZ);
					Blender.BlendResult blendResult = blender.calculate(blockX, blockZ);
					this.cachedBlendAlphaDensityFunction.cache[biomeOffX
							+ biomeOffZ * this.cachedBlendAlphaDensityFunction.horizontalCacheSize] = blendResult.alpha();
					this.cachedBlendOffsetDensityFunction.cache[biomeOffX
							+ biomeOffZ * this.cachedBlendOffsetDensityFunction.horizontalCacheSize] =
							blendResult.blendingOffset();
				}
			}
		}
		else {
			Arrays.fill(this.cachedBlendAlphaDensityFunction.cache, 1.0);
			Arrays.fill(this.cachedBlendOffsetDensityFunction.cache, 0.0);
		}

		NoiseRouter noiseRouter = noiseConfig.getNoiseRouter();
		NoiseRouter mappedRouter = noiseRouter.apply(this::getActualDensityFunction);
		this.preliminarySurfaceLevel = mappedRouter.preliminarySurfaceLevel();

		if (!chunkGeneratorSettings.hasAquifers()) {
			this.aquiferSampler = AquiferSampler.seaLevel(fluidLevelSampler);
		}
		else {
			int chunkX = ChunkSectionPos.getSectionCoord(startBlockX);
			int chunkZ = ChunkSectionPos.getSectionCoord(startBlockZ);
			this.aquiferSampler = AquiferSampler.aquifer(
					this,
					new ChunkPos(chunkX, chunkZ),
					mappedRouter,
					noiseConfig.getAquiferRandomDeriver(),
					generationShapeConfig.minimumY(),
					generationShapeConfig.height(),
					fluidLevelSampler
			);
		}

		List<ChunkNoiseSampler.BlockStateSampler> samplers = new ArrayList<>();
		DensityFunction finalDensity = DensityFunctionTypes
				.cacheAllInCell(DensityFunctionTypes.add(mappedRouter.finalDensity(), DensityFunctionTypes.Beardifier.INSTANCE))
				.apply(this::getActualDensityFunction);
		samplers.add(pos -> this.aquiferSampler.apply(pos, finalDensity.sample(pos)));

		if (chunkGeneratorSettings.oreVeins()) {
			samplers.add(OreVeinSampler.create(
					mappedRouter.veinToggle(),
					mappedRouter.veinRidged(),
					mappedRouter.veinGap(),
					noiseConfig.getOreRandomDeriver()
			));
		}

		this.blockStateSampler = new ChainedBlockSource(samplers.toArray(new ChunkNoiseSampler.BlockStateSampler[0]));
	}

	protected MultiNoiseUtil.MultiNoiseSampler createMultiNoiseSampler(
			NoiseRouter noiseRouter,
			List<MultiNoiseUtil.NoiseHypercube> spawnTarget
	) {
		return new MultiNoiseUtil.MultiNoiseSampler(
				noiseRouter.temperature().apply(this::getActualDensityFunction),
				noiseRouter.vegetation().apply(this::getActualDensityFunction),
				noiseRouter.continents().apply(this::getActualDensityFunction),
				noiseRouter.erosion().apply(this::getActualDensityFunction),
				noiseRouter.depth().apply(this::getActualDensityFunction),
				noiseRouter.ridges().apply(this::getActualDensityFunction),
				spawnTarget
		);
	}

	protected @Nullable BlockState sampleBlockState() {
		return this.blockStateSampler.sample(this);
	}

	@Override
	public int blockX() {
		return this.startBlockX + this.cellBlockX;
	}

	@Override
	public int blockY() {
		return this.startBlockY + this.cellBlockY;
	}

	@Override
	public int blockZ() {
		return this.startBlockZ + this.cellBlockZ;
	}

	public int estimateHighestSurfaceLevel(int minX, int minZ, int maxX, int maxZ) {
		int highest = Integer.MIN_VALUE;

		for (int z = minZ; z <= maxZ; z += 4) {
			for (int x = minX; x <= maxX; x += 4) {
				int height = estimateSurfaceHeight(x, z);
				if (height > highest) {
					highest = height;
				}
			}
		}

		return highest;
	}

	public int estimateSurfaceHeight(int blockX, int blockZ) {
		int snappedX = BiomeCoords.toBlock(BiomeCoords.fromBlock(blockX));
		int snappedZ = BiomeCoords.toBlock(BiomeCoords.fromBlock(blockZ));
		return surfaceHeightEstimateCache.computeIfAbsent(
				ColumnPos.pack(snappedX, snappedZ),
				this::calculateSurfaceHeightEstimate
		);
	}

	private int calculateSurfaceHeightEstimate(long columnPos) {
		int x = ColumnPos.getX(columnPos);
		int z = ColumnPos.getZ(columnPos);
		return MathHelper.floor(preliminarySurfaceLevel.sample(new DensityFunction.UnblendedNoisePos(x, 0, z)));
	}

	@Override
	public Blender getBlender() {
		return this.blender;
	}

	private void sampleDensity(boolean start, int cellX) {
		this.startBlockX = cellX * this.horizontalCellBlockCount;
		this.cellBlockX = 0;

		for (int cellOffZ = 0; cellOffZ < this.horizontalCellCount + 1; cellOffZ++) {
			int cellZ = this.startCellZ + cellOffZ;
			this.startBlockZ = cellZ * this.horizontalCellBlockCount;
			this.cellBlockZ = 0;
			this.cacheOnceUniqueIndex++;

			for (ChunkNoiseSampler.DensityInterpolator interpolator : this.interpolators) {
				double[] buffer = (start ? interpolator.startDensityBuffer : interpolator.endDensityBuffer)[cellOffZ];
				interpolator.fill(buffer, this.interpolationEachApplier);
			}
		}

		this.cacheOnceUniqueIndex++;
	}

	public void sampleStartDensity() {
		if (this.isInInterpolationLoop) {
			throw new IllegalStateException("Staring interpolation twice");
		}

		this.isInInterpolationLoop = true;
		this.sampleUniqueIndex = 0L;
		this.sampleDensity(true, this.startCellX);
	}

	public void sampleEndDensity(int cellX) {
		this.sampleDensity(false, this.startCellX + cellX + 1);
		this.startBlockX = (this.startCellX + cellX) * this.horizontalCellBlockCount;
	}

	public ChunkNoiseSampler at(int flatIndex) {
		int localZ = Math.floorMod(flatIndex, this.horizontalCellBlockCount);
		int xAndY = Math.floorDiv(flatIndex, this.horizontalCellBlockCount);
		int localX = Math.floorMod(xAndY, this.horizontalCellBlockCount);
		int localY = this.verticalCellBlockCount - 1 - Math.floorDiv(xAndY, this.horizontalCellBlockCount);
		this.cellBlockX = localX;
		this.cellBlockY = localY;
		this.cellBlockZ = localZ;
		this.index = flatIndex;
		return this;
	}

	@Override
	public void fill(double[] densities, DensityFunction densityFunction) {
		this.index = 0;

		for (int i = this.verticalCellBlockCount - 1; i >= 0; i--) {
			this.cellBlockY = i;

			for (int j = 0; j < this.horizontalCellBlockCount; j++) {
				this.cellBlockX = j;

				for (int k = 0; k < this.horizontalCellBlockCount; k++) {
					this.cellBlockZ = k;
					densities[this.index++] = densityFunction.sample(this);
				}
			}
		}
	}

	public void onSampledCellCorners(int cellY, int cellZ) {
		for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.interpolators) {
			densityInterpolator.onSampledCellCorners(cellY, cellZ);
		}

		this.isSamplingForCaches = true;
		this.startBlockY = (cellY + this.minimumCellY) * this.verticalCellBlockCount;
		this.startBlockZ = (this.startCellZ + cellZ) * this.horizontalCellBlockCount;
		this.cacheOnceUniqueIndex++;

		for (ChunkNoiseSampler.CellCache cellCache : this.caches) {
			cellCache.delegate.fill(cellCache.cache, this);
		}

		this.cacheOnceUniqueIndex++;
		this.isSamplingForCaches = false;
	}

	public void interpolateY(int blockY, double deltaY) {
		this.cellBlockY = blockY - this.startBlockY;

		for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.interpolators) {
			densityInterpolator.interpolateY(deltaY);
		}
	}

	public void interpolateX(int blockX, double deltaX) {
		this.cellBlockX = blockX - this.startBlockX;

		for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.interpolators) {
			densityInterpolator.interpolateX(deltaX);
		}
	}

	public void interpolateZ(int blockZ, double deltaZ) {
		this.cellBlockZ = blockZ - this.startBlockZ;
		this.sampleUniqueIndex++;

		for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.interpolators) {
			densityInterpolator.interpolateZ(deltaZ);
		}
	}

	public void stopInterpolation() {
		if (!this.isInInterpolationLoop) {
			throw new IllegalStateException("Staring interpolation twice");
		}

		this.isInInterpolationLoop = false;
	}

	public void swapBuffers() {
		this.interpolators.forEach(ChunkNoiseSampler.DensityInterpolator::swapBuffers);
	}

	public AquiferSampler getAquiferSampler() {
		return this.aquiferSampler;
	}

	protected int getHorizontalCellBlockCount() {
		return this.horizontalCellBlockCount;
	}

	protected int getVerticalCellBlockCount() {
		return this.verticalCellBlockCount;
	}

	Blender.BlendResult calculateBlendResult(int blockX, int blockZ) {
		long columnKey = ChunkPos.toLong(blockX, blockZ);

		if (this.lastBlendingColumnPos == columnKey) {
			return this.lastBlendingResult;
		}

		this.lastBlendingColumnPos = columnKey;
		Blender.BlendResult result = this.blender.calculate(blockX, blockZ);
		this.lastBlendingResult = result;
		return result;
	}

	protected DensityFunction getActualDensityFunction(DensityFunction function) {
		return this.actualDensityFunctionCache.computeIfAbsent(function, this::getActualDensityFunctionImpl);
	}

	private DensityFunction getActualDensityFunctionImpl(DensityFunction function) {
		if (function instanceof DensityFunctionTypes.Wrapping wrapping) {
			return switch (wrapping.type()) {
				case INTERPOLATED -> new ChunkNoiseSampler.DensityInterpolator(wrapping.wrapped());
				case FLAT_CACHE -> new ChunkNoiseSampler.FlatCache(wrapping.wrapped(), true);
				case CACHE2D -> new ChunkNoiseSampler.Cache2D(wrapping.wrapped());
				case CACHE_ONCE -> new ChunkNoiseSampler.CacheOnce(wrapping.wrapped());
				case CACHE_ALL_IN_CELL -> new ChunkNoiseSampler.CellCache(wrapping.wrapped());
			};
		}

		if (this.blender != Blender.getNoBlending()) {
			if (function == DensityFunctionTypes.BlendAlpha.INSTANCE) {
				return this.cachedBlendAlphaDensityFunction;
			}

			if (function == DensityFunctionTypes.BlendOffset.INSTANCE) {
				return this.cachedBlendOffsetDensityFunction;
			}
		}

		if (function == DensityFunctionTypes.Beardifier.INSTANCE) {
			return this.beardifying;
		}

		return function instanceof DensityFunctionTypes.RegistryEntryHolder registryEntryHolder
				? registryEntryHolder.function().value()
				: function;
	}

	class BlendAlphaDensityFunction implements ChunkNoiseSampler.ParentedNoiseType {

		@Override
		public DensityFunction wrapped() {
			return DensityFunctionTypes.BlendAlpha.INSTANCE;
		}

		@Override
		public DensityFunction apply(DensityFunction.DensityFunctionVisitor visitor) {
			return this.wrapped().apply(visitor);
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			return ChunkNoiseSampler.this.calculateBlendResult(pos.blockX(), pos.blockZ()).alpha();
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public double minValue() {
			return 0.0;
		}

		@Override
		public double maxValue() {
			return 1.0;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return DensityFunctionTypes.BlendAlpha.CODEC;
		}
	}

	class BlendOffsetDensityFunction implements ChunkNoiseSampler.ParentedNoiseType {

		@Override
		public DensityFunction wrapped() {
			return DensityFunctionTypes.BlendOffset.INSTANCE;
		}

		@Override
		public DensityFunction apply(DensityFunction.DensityFunctionVisitor visitor) {
			return this.wrapped().apply(visitor);
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			return ChunkNoiseSampler.this.calculateBlendResult(pos.blockX(), pos.blockZ()).blendingOffset();
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public double minValue() {
			return Double.NEGATIVE_INFINITY;
		}

		@Override
		public double maxValue() {
			return Double.POSITIVE_INFINITY;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return DensityFunctionTypes.BlendOffset.CODEC;
		}
	}

	/** Сэмплер блочного состояния для данной позиции в пространстве шума. */
	@FunctionalInterface
	public interface BlockStateSampler {

		@Nullable BlockState sample(DensityFunction.NoisePos pos);
	}

	/** Кэширует результат функции плотности по XZ-колонке (игнорирует Y). */
	static class Cache2D implements DensityFunctionTypes.Wrapper, ChunkNoiseSampler.ParentedNoiseType {

		private final DensityFunction delegate;
		private long lastSamplingColumnPos = ChunkPos.MARKER;
		private double lastSamplingResult;

		Cache2D(DensityFunction delegate) {
			this.delegate = delegate;
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			long columnKey = ChunkPos.toLong(pos.blockX(), pos.blockZ());

			if (this.lastSamplingColumnPos == columnKey) {
				return this.lastSamplingResult;
			}

			this.lastSamplingColumnPos = columnKey;
			double result = this.delegate.sample(pos);
			this.lastSamplingResult = result;
			return result;
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			this.delegate.fill(densities, applier);
		}

		@Override
		public DensityFunction wrapped() {
			return this.delegate;
		}

		@Override
		public DensityFunctionTypes.Wrapping.Type type() {
			return DensityFunctionTypes.Wrapping.Type.CACHE2D;
		}
	}

	/** Кэширует результат функции плотности на один уникальный проход сэмплирования. */
	class CacheOnce implements DensityFunctionTypes.Wrapper, ChunkNoiseSampler.ParentedNoiseType {

		private final DensityFunction delegate;
		private long sampleUniqueIndex;
		private long cacheOnceUniqueIndex;
		private double lastSamplingResult;
		private double @Nullable [] cache;

		CacheOnce(final DensityFunction delegate) {
			this.delegate = delegate;
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			if (pos != ChunkNoiseSampler.this) {
				return this.delegate.sample(pos);
			}
			else if (this.cache != null && this.cacheOnceUniqueIndex == ChunkNoiseSampler.this.cacheOnceUniqueIndex) {
				return this.cache[ChunkNoiseSampler.this.index];
			}
			else if (this.sampleUniqueIndex == ChunkNoiseSampler.this.sampleUniqueIndex) {
				return this.lastSamplingResult;
			}
			else {
				this.sampleUniqueIndex = ChunkNoiseSampler.this.sampleUniqueIndex;
				double d = this.delegate.sample(pos);
				this.lastSamplingResult = d;
				return d;
			}
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			if (this.cache != null && this.cacheOnceUniqueIndex == ChunkNoiseSampler.this.cacheOnceUniqueIndex) {
				System.arraycopy(this.cache, 0, densities, 0, densities.length);
			}
			else {
				this.wrapped().fill(densities, applier);
				if (this.cache != null && this.cache.length == densities.length) {
					System.arraycopy(densities, 0, this.cache, 0, densities.length);
				}
				else {
					this.cache = (double[]) densities.clone();
				}

				this.cacheOnceUniqueIndex = ChunkNoiseSampler.this.cacheOnceUniqueIndex;
			}
		}

		@Override
		public DensityFunction wrapped() {
			return this.delegate;
		}

		@Override
		public DensityFunctionTypes.Wrapping.Type type() {
			return DensityFunctionTypes.Wrapping.Type.CACHE_ONCE;
		}
	}

	/** Кэширует значения функции плотности для всех блоков внутри одной ячейки генерации. */
	class CellCache implements DensityFunctionTypes.Wrapper, ChunkNoiseSampler.ParentedNoiseType {

		final DensityFunction delegate;
		final double[] cache;

		CellCache(final DensityFunction delegate) {
			this.delegate = delegate;
			this.cache = new double[ChunkNoiseSampler.this.horizontalCellBlockCount
					* ChunkNoiseSampler.this.horizontalCellBlockCount
					* ChunkNoiseSampler.this.verticalCellBlockCount];
			ChunkNoiseSampler.this.caches.add(this);
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			if (pos != ChunkNoiseSampler.this) {
				return this.delegate.sample(pos);
			}

			if (!ChunkNoiseSampler.this.isInInterpolationLoop) {
				throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
			}

			int localX = ChunkNoiseSampler.this.cellBlockX;
			int localY = ChunkNoiseSampler.this.cellBlockY;
			int localZ = ChunkNoiseSampler.this.cellBlockZ;
			boolean inBounds = localX >= 0
					&& localY >= 0
					&& localZ >= 0
					&& localX < ChunkNoiseSampler.this.horizontalCellBlockCount
					&& localY < ChunkNoiseSampler.this.verticalCellBlockCount
					&& localZ < ChunkNoiseSampler.this.horizontalCellBlockCount;

			return inBounds
					? this.cache[((ChunkNoiseSampler.this.verticalCellBlockCount - 1 - localY)
					* ChunkNoiseSampler.this.horizontalCellBlockCount + localX)
					* ChunkNoiseSampler.this.horizontalCellBlockCount + localZ]
					: this.delegate.sample(pos);
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public DensityFunction wrapped() {
			return this.delegate;
		}

		@Override
		public DensityFunctionTypes.Wrapping.Type type() {
			return DensityFunctionTypes.Wrapping.Type.CACHE_ALL_IN_CELL;
		}
	}

	/**
	 * Выполняет трилинейную интерполяцию функции плотности по восьми угловым точкам ячейки.
	 * Буферы {@code startDensityBuffer} и {@code endDensityBuffer} хранят значения
	 * на границах ячейки по X, которые затем интерполируются по Y и Z.
	 */
	public class DensityInterpolator implements DensityFunctionTypes.Wrapper, ChunkNoiseSampler.ParentedNoiseType {

		double[][] startDensityBuffer;
		double[][] endDensityBuffer;
		private final DensityFunction delegate;
		private double x0y0z0;
		private double x0y0z1;
		private double x1y0z0;
		private double x1y0z1;
		private double x0y1z0;
		private double x0y1z1;
		private double x1y1z0;
		private double x1y1z1;
		private double x0z0;
		private double x1z0;
		private double x0z1;
		private double x1z1;
		private double z0;
		private double z1;
		private double result;

		DensityInterpolator(final DensityFunction delegate) {
			this.delegate = delegate;
			this.startDensityBuffer =
					this.createBuffer(
							ChunkNoiseSampler.this.verticalCellCount,
							ChunkNoiseSampler.this.horizontalCellCount
					);
			this.endDensityBuffer =
					this.createBuffer(
							ChunkNoiseSampler.this.verticalCellCount,
							ChunkNoiseSampler.this.horizontalCellCount
					);
			ChunkNoiseSampler.this.interpolators.add(this);
		}

		private double[][] createBuffer(int sizeZ, int sizeX) {
			int cols = sizeX + 1;
			int rows = sizeZ + 1;
			double[][] buffer = new double[cols][rows];

			for (int col = 0; col < cols; col++) {
				buffer[col] = new double[rows];
			}

			return buffer;
		}

		void onSampledCellCorners(int cellY, int cellZ) {
			this.x0y0z0 = this.startDensityBuffer[cellZ][cellY];
			this.x0y0z1 = this.startDensityBuffer[cellZ + 1][cellY];
			this.x1y0z0 = this.endDensityBuffer[cellZ][cellY];
			this.x1y0z1 = this.endDensityBuffer[cellZ + 1][cellY];
			this.x0y1z0 = this.startDensityBuffer[cellZ][cellY + 1];
			this.x0y1z1 = this.startDensityBuffer[cellZ + 1][cellY + 1];
			this.x1y1z0 = this.endDensityBuffer[cellZ][cellY + 1];
			this.x1y1z1 = this.endDensityBuffer[cellZ + 1][cellY + 1];
		}

		void interpolateY(double deltaY) {
			this.x0z0 = MathHelper.lerp(deltaY, this.x0y0z0, this.x0y1z0);
			this.x1z0 = MathHelper.lerp(deltaY, this.x1y0z0, this.x1y1z0);
			this.x0z1 = MathHelper.lerp(deltaY, this.x0y0z1, this.x0y1z1);
			this.x1z1 = MathHelper.lerp(deltaY, this.x1y0z1, this.x1y1z1);
		}

		void interpolateX(double deltaX) {
			this.z0 = MathHelper.lerp(deltaX, this.x0z0, this.x1z0);
			this.z1 = MathHelper.lerp(deltaX, this.x0z1, this.x1z1);
		}

		void interpolateZ(double deltaZ) {
			this.result = MathHelper.lerp(deltaZ, this.z0, this.z1);
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			if (pos != ChunkNoiseSampler.this) {
				return this.delegate.sample(pos);
			}

			if (!ChunkNoiseSampler.this.isInInterpolationLoop) {
				throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
			}

			return ChunkNoiseSampler.this.isSamplingForCaches
					? MathHelper.lerp3(
					(double) ChunkNoiseSampler.this.cellBlockX / ChunkNoiseSampler.this.horizontalCellBlockCount,
					(double) ChunkNoiseSampler.this.cellBlockY / ChunkNoiseSampler.this.verticalCellBlockCount,
					(double) ChunkNoiseSampler.this.cellBlockZ / ChunkNoiseSampler.this.horizontalCellBlockCount,
					this.x0y0z0,
					this.x1y0z0,
					this.x0y1z0,
					this.x1y1z0,
					this.x0y0z1,
					this.x1y0z1,
					this.x0y1z1,
					this.x1y1z1
			)
					: this.result;
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			if (ChunkNoiseSampler.this.isSamplingForCaches) {
				applier.fill(densities, this);
			}
			else {
				this.wrapped().fill(densities, applier);
			}
		}

		@Override
		public DensityFunction wrapped() {
			return this.delegate;
		}

		private void swapBuffers() {
			double[][] temp = this.startDensityBuffer;
			this.startDensityBuffer = this.endDensityBuffer;
			this.endDensityBuffer = temp;
		}

		@Override
		public DensityFunctionTypes.Wrapping.Type type() {
			return DensityFunctionTypes.Wrapping.Type.INTERPOLATED;
		}
	}

	/** Кэширует значения функции плотности по плоской 2D-сетке биомных координат (Y игнорируется). */
	class FlatCache implements DensityFunctionTypes.Wrapper, ChunkNoiseSampler.ParentedNoiseType {

		private final DensityFunction delegate;
		final double[] cache;
		final int horizontalCacheSize;

		FlatCache(final DensityFunction delegate, final boolean sample) {
			this.delegate = delegate;
			this.horizontalCacheSize = ChunkNoiseSampler.this.horizontalBiomeEnd + 1;
			this.cache = new double[this.horizontalCacheSize * this.horizontalCacheSize];

			if (sample) {
				for (int biomeOffX = 0; biomeOffX <= ChunkNoiseSampler.this.horizontalBiomeEnd; biomeOffX++) {
					int biomeX = ChunkNoiseSampler.this.startBiomeX + biomeOffX;
					int blockX = BiomeCoords.toBlock(biomeX);

					for (int biomeOffZ = 0; biomeOffZ <= ChunkNoiseSampler.this.horizontalBiomeEnd; biomeOffZ++) {
						int biomeZ = ChunkNoiseSampler.this.startBiomeZ + biomeOffZ;
						int blockZ = BiomeCoords.toBlock(biomeZ);
						this.cache[biomeOffX + biomeOffZ * this.horizontalCacheSize] =
								delegate.sample(new DensityFunction.UnblendedNoisePos(blockX, 0, blockZ));
					}
				}
			}
		}

		@Override
		public double sample(DensityFunction.NoisePos pos) {
			int biomeX = BiomeCoords.fromBlock(pos.blockX());
			int biomeZ = BiomeCoords.fromBlock(pos.blockZ());
			int offX = biomeX - ChunkNoiseSampler.this.startBiomeX;
			int offZ = biomeZ - ChunkNoiseSampler.this.startBiomeZ;
			return offX >= 0 && offZ >= 0 && offX < this.horizontalCacheSize && offZ < this.horizontalCacheSize
					? this.cache[offX + offZ * this.horizontalCacheSize]
					: this.delegate.sample(pos);
		}

		@Override
		public void fill(double[] densities, DensityFunction.EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public DensityFunction wrapped() {
			return this.delegate;
		}

		@Override
		public DensityFunctionTypes.Wrapping.Type type() {
			return DensityFunctionTypes.Wrapping.Type.FLAT_CACHE;
		}
	}

	/** Маркерный интерфейс для внутренних типов функций плотности, привязанных к родительскому сэмплеру. */
	interface ParentedNoiseType extends DensityFunction {

		DensityFunction wrapped();

		@Override
		default double minValue() {
			return this.wrapped().minValue();
		}

		@Override
		default double maxValue() {
			return this.wrapped().maxValue();
		}
	}
}
