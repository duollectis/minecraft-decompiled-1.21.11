package net.minecraft.world.gen.surfacebuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.chunk.BlockColumn;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseParametersKeys;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Строит поверхность чанка, применяя правила материалов {@link MaterialRules.MaterialRule}
 * к каждому блоку. Отвечает за размещение почвы, камня, терракоты в бэдлендах,
 * айсбергов в замёрзших океанах и других поверхностных особенностей.
 */
public class SurfaceBuilder {

	private static final int TERRACOTTA_BAND_COUNT = 192;
	private static final BlockState WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getDefaultState();
	private static final BlockState ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.getDefaultState();
	private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.getDefaultState();
	private static final BlockState YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.getDefaultState();
	private static final BlockState BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.getDefaultState();
	private static final BlockState RED_TERRACOTTA = Blocks.RED_TERRACOTTA.getDefaultState();
	private static final BlockState LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.getDefaultState();
	private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.getDefaultState();
	private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.getDefaultState();
	private final BlockState defaultState;
	private final int seaLevel;
	private final BlockState[] terracottaBands;
	private final DoublePerlinNoiseSampler terracottaBandsOffsetNoise;
	private final DoublePerlinNoiseSampler badlandsPillarNoise;
	private final DoublePerlinNoiseSampler badlandsPillarRoofNoise;
	private final DoublePerlinNoiseSampler badlandsSurfaceNoise;
	private final DoublePerlinNoiseSampler icebergPillarNoise;
	private final DoublePerlinNoiseSampler icebergPillarRoofNoise;
	private final DoublePerlinNoiseSampler icebergSurfaceNoise;
	private final RandomSplitter randomDeriver;
	private final DoublePerlinNoiseSampler surfaceNoise;
	private final DoublePerlinNoiseSampler surfaceSecondaryNoise;

	public SurfaceBuilder(
			NoiseConfig noiseConfig,
			BlockState defaultState,
			int seaLevel,
			RandomSplitter randomDeriver
	) {
		this.defaultState = defaultState;
		this.seaLevel = seaLevel;
		this.randomDeriver = randomDeriver;
		this.terracottaBandsOffsetNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.CLAY_BANDS_OFFSET);
		this.terracottaBands = createTerracottaBands(randomDeriver.split(Identifier.ofVanilla("clay_bands")));
		this.surfaceNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.SURFACE);
		this.surfaceSecondaryNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.SURFACE_SECONDARY);
		this.badlandsPillarNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.BADLANDS_PILLAR);
		this.badlandsPillarRoofNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.BADLANDS_PILLAR_ROOF);
		this.badlandsSurfaceNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.BADLANDS_SURFACE);
		this.icebergPillarNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.ICEBERG_PILLAR);
		this.icebergPillarRoofNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.ICEBERG_PILLAR_ROOF);
		this.icebergSurfaceNoise = noiseConfig.getOrCreateSampler(NoiseParametersKeys.ICEBERG_SURFACE);
	}

	public void buildSurface(
			NoiseConfig noiseConfig,
			BiomeAccess biomeAccess,
			Registry<Biome> biomeRegistry,
			boolean useLegacyRandom,
			HeightContext heightContext,
			Chunk chunk,
			ChunkNoiseSampler chunkNoiseSampler,
			MaterialRules.MaterialRule materialRule
	) {
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		final ChunkPos chunkPos = chunk.getPos();
		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();
		BlockColumn blockColumn = new BlockColumn() {
			@Override
			public BlockState getState(int y) {
				return chunk.getBlockState(mutable.setY(y));
			}

			@Override
			public void setState(int y, BlockState state) {
				HeightLimitView heightLimitView = chunk.getHeightLimitView();
				if (heightLimitView.isInHeightLimit(y)) {
					chunk.setBlockState(mutable.setY(y), state);
					if (!state.getFluidState().isEmpty()) {
						chunk.markBlockForPostProcessing(mutable);
					}
				}
			}

			@Override
			public String toString() {
				return "ChunkBlockColumn " + chunkPos;
			}
		};
		MaterialRules.MaterialRuleContext materialRuleContext = new MaterialRules.MaterialRuleContext(
				this, noiseConfig, chunk, chunkNoiseSampler, biomeAccess::getBiome, biomeRegistry, heightContext
		);
		MaterialRules.BlockStateRule blockStateRule = materialRule.apply(materialRuleContext);
		BlockPos.Mutable mutable2 = new BlockPos.Mutable();

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int worldX = startX + localX;
				int worldZ = startZ + localZ;
				int surfaceHeight = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ) + 1;
				mutable.setX(worldX).setZ(worldZ);
				RegistryEntry<Biome> biomeEntry = biomeAccess.getBiome(
						mutable2.set(worldX, useLegacyRandom ? 0 : surfaceHeight, worldZ)
				);

				if (biomeEntry.matchesKey(BiomeKeys.ERODED_BADLANDS)) {
					placeBadlandsPillar(blockColumn, worldX, worldZ, surfaceHeight, chunk);
				}

				int topY = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ) + 1;
				materialRuleContext.initHorizontalContext(worldX, worldZ);
				int stoneDepthAbove = 0;
				int fluidSurfaceY = Integer.MIN_VALUE;
				int stoneFloorY = Integer.MAX_VALUE;
				int bottomY = chunk.getBottomY();

				for (int y = topY; y >= bottomY; y--) {
					BlockState blockState = blockColumn.getState(y);

					if (blockState.isAir()) {
						stoneDepthAbove = 0;
						fluidSurfaceY = Integer.MIN_VALUE;
					} else if (!blockState.getFluidState().isEmpty()) {
						if (fluidSurfaceY == Integer.MIN_VALUE) {
							fluidSurfaceY = y + 1;
						}
					} else {
						if (stoneFloorY >= y) {
							stoneFloorY = DimensionType.MIN_HEIGHT_IN_BLOCKS;

							for (int scanY = y - 1; scanY >= bottomY - 1; scanY--) {
								BlockState scanState = blockColumn.getState(scanY);

								if (!isDefaultBlock(scanState)) {
									stoneFloorY = scanY + 1;
									break;
								}
							}
						}

						stoneDepthAbove++;
						int depthFromFloor = y - stoneFloorY + 1;
						materialRuleContext.initVerticalContext(stoneDepthAbove, depthFromFloor, fluidSurfaceY, worldX, y, worldZ);

						if (blockState == defaultState) {
							BlockState replacement = blockStateRule.tryApply(worldX, y, worldZ);

							if (replacement != null) {
								blockColumn.setState(y, replacement);
							}
						}
					}
				}

				if (biomeEntry.matchesKey(BiomeKeys.FROZEN_OCEAN)
						|| biomeEntry.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN)
				) {
					placeIceberg(
							materialRuleContext.estimateSurfaceHeight(),
							biomeEntry.value(),
							blockColumn,
							mutable2,
							worldX,
							worldZ,
							surfaceHeight
					);
				}
			}
		}
	}

	/**
	 * Вычисляет глубину поверхностного слоя (runDepth) для заданной горизонтальной позиции.
	 * Значение определяет, насколько глубоко от поверхности применяются правила материалов.
	 */
	protected int sampleRunDepth(int blockX, int blockZ) {
		double noiseValue = surfaceNoise.sample(blockX, 0.0, blockZ);
		return (int) (noiseValue * 2.75 + 3.0 + randomDeriver.split(blockX, 0, blockZ).nextDouble() * 0.25);
	}

	protected double sampleSecondaryDepth(int blockX, int blockZ) {
		return surfaceSecondaryNoise.sample(blockX, 0.0, blockZ);
	}

	private boolean isDefaultBlock(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty();
	}

	public int getSeaLevel() {
		return seaLevel;
	}

	@Deprecated
	public Optional<BlockState> applyMaterialRule(
			MaterialRules.MaterialRule rule,
			CarverContext context,
			Function<BlockPos, RegistryEntry<Biome>> posToBiome,
			Chunk chunk,
			ChunkNoiseSampler chunkNoiseSampler,
			BlockPos pos,
			boolean hasFluid
	) {
		MaterialRules.MaterialRuleContext materialRuleContext = new MaterialRules.MaterialRuleContext(
				this,
				context.getNoiseConfig(),
				chunk,
				chunkNoiseSampler,
				posToBiome,
				context.getRegistryManager().getOrThrow(RegistryKeys.BIOME),
				context
		);
		MaterialRules.BlockStateRule blockStateRule = rule.apply(materialRuleContext);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		materialRuleContext.initHorizontalContext(x, z);
		materialRuleContext.initVerticalContext(1, 1, hasFluid ? y + 1 : Integer.MIN_VALUE, x, y, z);
		BlockState blockState = blockStateRule.tryApply(x, y, z);
		return Optional.ofNullable(blockState);
	}

	private void placeBadlandsPillar(BlockColumn column, int x, int z, int surfaceY, HeightLimitView chunk) {
		double pillarStrength = Math.min(
				Math.abs(badlandsSurfaceNoise.sample(x, 0.0, z) * 8.25),
				badlandsPillarNoise.sample(x * 0.2, 0.0, z * 0.2) * 15.0
		);

		if (pillarStrength <= 0.0) {
			return;
		}

		double roofNoise = Math.abs(badlandsPillarRoofNoise.sample(x * 0.75, 0.0, z * 0.75) * 1.5);
		double pillarTopY = 64.0 + Math.min(pillarStrength * pillarStrength * 2.5, Math.ceil(roofNoise * 50.0) + 24.0);
		int pillarTopBlock = MathHelper.floor(pillarTopY);

		if (surfaceY > pillarTopBlock) {
			return;
		}

		for (int y = pillarTopBlock; y >= chunk.getBottomY(); y--) {
			BlockState blockState = column.getState(y);

			if (blockState.isOf(defaultState.getBlock())) {
				break;
			}

			if (blockState.isOf(Blocks.WATER)) {
				return;
			}
		}

		for (int y = pillarTopBlock; y >= chunk.getBottomY() && column.getState(y).isAir(); y--) {
			column.setState(y, defaultState);
		}
	}

	private void placeIceberg(
			int minY,
			Biome biome,
			BlockColumn column,
			BlockPos.Mutable mutablePos,
			int x,
			int z,
			int surfaceY
	) {
		double icebergStrength = Math.min(
				Math.abs(icebergSurfaceNoise.sample(x, 0.0, z) * 8.25),
				icebergPillarNoise.sample(x * 1.28, 0.0, z * 1.28) * 15.0
		);

		if (icebergStrength <= 1.8) {
			return;
		}

		double roofNoise = Math.abs(icebergPillarRoofNoise.sample(x * 1.17, 0.0, z * 1.17) * 1.5);
		double icebergHeight = Math.min(icebergStrength * icebergStrength * 1.2, Math.ceil(roofNoise * 40.0) + 14.0);

		if (biome.shouldGenerateLowerFrozenOceanSurface(mutablePos.set(x, seaLevel, z), seaLevel)) {
			icebergHeight -= 2.0;
		}

		double icebergTopY;
		double icebergBottomY;

		if (icebergHeight > 2.0) {
			icebergBottomY = seaLevel - icebergHeight - 7.0;
			icebergTopY = icebergHeight + seaLevel;
		} else {
			icebergTopY = 0.0;
			icebergBottomY = 0.0;
		}

		Random random = randomDeriver.split(x, 0, z);
		int snowCapThickness = 2 + random.nextInt(4);
		int snowCapMinY = seaLevel + 18 + random.nextInt(10);
		int snowLayers = 0;

		for (int y = Math.max(surfaceY, (int) icebergTopY + 1); y >= minY; y--) {
			boolean isAirInIceberg = column.getState(y).isAir()
					&& y < (int) icebergTopY
					&& random.nextDouble() > 0.01;
			boolean isWaterInIceberg = column.getState(y).isOf(Blocks.WATER)
					&& y > (int) icebergBottomY
					&& y < seaLevel
					&& icebergBottomY != 0.0
					&& random.nextDouble() > 0.15;

			if (isAirInIceberg || isWaterInIceberg) {
				if (snowLayers <= snowCapThickness && y > snowCapMinY) {
					column.setState(y, SNOW_BLOCK);
					snowLayers++;
				} else {
					column.setState(y, PACKED_ICE);
				}
			}
		}
	}

	private static BlockState[] createTerracottaBands(Random random) {
		BlockState[] bands = new BlockState[TERRACOTTA_BAND_COUNT];
		Arrays.fill(bands, TERRACOTTA);

		for (int i = 0; i < bands.length; i++) {
			i += random.nextInt(5) + 1;

			if (i < bands.length) {
				bands[i] = ORANGE_TERRACOTTA;
			}
		}

		addTerracottaBands(random, bands, 1, YELLOW_TERRACOTTA);
		addTerracottaBands(random, bands, 2, BROWN_TERRACOTTA);
		addTerracottaBands(random, bands, 1, RED_TERRACOTTA);

		int whiteCount = random.nextBetween(9, 15);
		int placed = 0;

		for (int k = 0; placed < whiteCount && k < bands.length; k += random.nextInt(16) + 4) {
			bands[k] = WHITE_TERRACOTTA;

			if (k - 1 > 0 && random.nextBoolean()) {
				bands[k - 1] = LIGHT_GRAY_TERRACOTTA;
			}

			if (k + 1 < bands.length && random.nextBoolean()) {
				bands[k + 1] = LIGHT_GRAY_TERRACOTTA;
			}

			placed++;
		}

		return bands;
	}

	private static void addTerracottaBands(
			Random random,
			BlockState[] bands,
			int minBandSize,
			BlockState state
	) {
		int bandCount = random.nextBetween(6, 15);

		for (int band = 0; band < bandCount; band++) {
			int bandSize = minBandSize + random.nextInt(3);
			int startIndex = random.nextInt(bands.length);

			for (int offset = 0; startIndex + offset < bands.length && offset < bandSize; offset++) {
				bands[startIndex + offset] = state;
			}
		}
	}

	protected BlockState getTerracottaBlock(int x, int y, int z) {
		int offset = (int) Math.round(terracottaBandsOffsetNoise.sample(x, 0.0, z) * 4.0);
		return terracottaBands[(y + offset + terracottaBands.length) % terracottaBands.length];
	}
}
