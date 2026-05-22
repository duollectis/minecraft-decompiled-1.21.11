package net.minecraft.world.gen.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Основная реализация генератора чанков на основе шума (noise-based).
 * Использует {@link ChunkNoiseSampler} для трёхмерной интерполяции плотности,
 * {@link AquiferSampler} для размещения жидкостей и {@link Blender} для сглаживания
 * границ между старыми и новыми чанками.
 */
public final class NoiseChunkGenerator extends ChunkGenerator {

	public static final MapCodec<NoiseChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
					ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
			)
			.apply(instance, instance.stable(NoiseChunkGenerator::new))
	);
	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final int CARVER_SEARCH_RADIUS = 8;
	private final RegistryEntry<ChunkGeneratorSettings> settings;
	private final Supplier<AquiferSampler.FluidLevelSampler> fluidLevelSampler;

	public NoiseChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
		super(biomeSource);
		this.settings = settings;
		this.fluidLevelSampler = Suppliers.memoize(() -> createFluidLevelSampler(settings.value()));
	}

	private static AquiferSampler.FluidLevelSampler createFluidLevelSampler(ChunkGeneratorSettings settings) {
		// -54 — нижняя граница лавового слоя в Overworld (hardcoded в Mojang)
		AquiferSampler.FluidLevel lavaLevel = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
		int seaLevelY = settings.seaLevel();
		AquiferSampler.FluidLevel seaLevelFluid = new AquiferSampler.FluidLevel(seaLevelY, settings.defaultFluid());
		AquiferSampler.FluidLevel emptyFluid = new AquiferSampler.FluidLevel(DimensionType.MIN_HEIGHT * 2, Blocks.AIR.getDefaultState());

		return (x, y, z) -> {
			if (SharedConstants.DISABLE_FLUID_GENERATION) {
				return emptyFluid;
			}

			return y < Math.min(-54, seaLevelY) ? lavaLevel : seaLevelFluid;
		};
	}

	@Override
	public CompletableFuture<Chunk> populateBiomes(
			NoiseConfig noiseConfig,
			Blender blender,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		return CompletableFuture.supplyAsync(
				() -> {
					this.populateBiomes(blender, noiseConfig, structureAccessor, chunk);
					return chunk;
				}, Util.getMainWorkerExecutor().named("init_biomes")
		);
	}

	private void populateBiomes(
			Blender blender,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		ChunkNoiseSampler noiseSampler = chunk.getOrCreateChunkNoiseSampler(
				c -> this.createChunkNoiseSampler(c, structureAccessor, blender, noiseConfig)
		);
		BiomeSupplier biomeSupplier = BelowZeroRetrogen.getBiomeSupplier(blender.getBiomeSupplier(biomeSource), chunk);

		chunk.populateBiomes(
				biomeSupplier,
				noiseSampler.createMultiNoiseSampler(
						noiseConfig.getNoiseRouter(),
						settings.value().spawnTarget()
				)
		);
	}

	private ChunkNoiseSampler createChunkNoiseSampler(
			Chunk chunk,
			StructureAccessor world,
			Blender blender,
			NoiseConfig noiseConfig
	) {
		return ChunkNoiseSampler.create(
				chunk,
				noiseConfig,
				StructureWeightSampler.createStructureWeightSampler(world, chunk.getPos()),
				settings.value(),
				fluidLevelSampler.get(),
				blender
		);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	public RegistryEntry<ChunkGeneratorSettings> getSettings() {
		return settings;
	}

	public boolean matchesSettings(RegistryKey<ChunkGeneratorSettings> settingsKey) {
		return settings.matchesKey(settingsKey);
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		return this
				.sampleHeightmap(world, noiseConfig, x, z, null, heightmap.getBlockPredicate())
				.orElse(world.getBottomY());
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		MutableObject<VerticalBlockSample> columnHolder = new MutableObject<>();
		sampleHeightmap(world, noiseConfig, x, z, columnHolder, null);
		return columnHolder.getValue();
	}

	@Override
	public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		DecimalFormat decimalFormat = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ROOT));
		NoiseRouter noiseRouter = noiseConfig.getNoiseRouter();
		DensityFunction.UnblendedNoisePos
				unblendedNoisePos =
				new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ());
		double d = noiseRouter.ridges().sample(unblendedNoisePos);
		text.add(
				"NoiseRouter T: "
						+ decimalFormat.format(noiseRouter.temperature().sample(unblendedNoisePos))
						+ " V: "
						+ decimalFormat.format(noiseRouter.vegetation().sample(unblendedNoisePos))
						+ " C: "
						+ decimalFormat.format(noiseRouter.continents().sample(unblendedNoisePos))
						+ " E: "
						+ decimalFormat.format(noiseRouter.erosion().sample(unblendedNoisePos))
						+ " D: "
						+ decimalFormat.format(noiseRouter.depth().sample(unblendedNoisePos))
						+ " W: "
						+ decimalFormat.format(d)
						+ " PV: "
						+ decimalFormat.format(DensityFunctions.getPeaksValleysNoise((float) d))
						+ " PS: "
						+ decimalFormat.format(noiseRouter.preliminarySurfaceLevel().sample(unblendedNoisePos))
						+ " N: "
						+ decimalFormat.format(noiseRouter.finalDensity().sample(unblendedNoisePos))
		);
	}

	/**
	 * Сэмплирует вертикальный столбец блоков по шумовой карте для получения высоты поверхности.
	 * Используется как для {@link #getHeight} (поиск первого блока по предикату),
	 * так и для {@link #getColumnSample} (заполнение всего массива состояний блоков).
	 *
	 * @param columnSample  если не null — заполняется массив состояний блоков по всей высоте
	 * @param stopPredicate если не null — возвращает Y+1 первого блока, удовлетворяющего предикату
	 */
	private OptionalInt sampleHeightmap(
			HeightLimitView world,
			NoiseConfig noiseConfig,
			int x,
			int z,
			@Nullable MutableObject<VerticalBlockSample> columnSample,
			@Nullable Predicate<BlockState> stopPredicate
	) {
		GenerationShapeConfig shapeConfig = settings.value().generationShapeConfig().trimHeight(world);
		int cellHeight = shapeConfig.verticalCellBlockCount();
		int minimumY = shapeConfig.minimumY();
		int minimumCellY = MathHelper.floorDiv(minimumY, cellHeight);
		int cellCount = MathHelper.floorDiv(shapeConfig.height(), cellHeight);

		if (cellCount <= 0) {
			return OptionalInt.empty();
		}

		BlockState[] blockStates = columnSample == null
				? null
				: new BlockState[shapeConfig.height()];

		if (columnSample != null) {
			columnSample.setValue(new VerticalBlockSample(minimumY, blockStates));
		}

		int cellWidth = shapeConfig.horizontalCellBlockCount();
		int cellX = Math.floorDiv(x, cellWidth);
		int cellZ = Math.floorDiv(z, cellWidth);
		int localX = Math.floorMod(x, cellWidth);
		int localZ = Math.floorMod(z, cellWidth);
		int startX = cellX * cellWidth;
		int startZ = cellZ * cellWidth;
		double deltaX = (double) localX / cellWidth;
		double deltaZ = (double) localZ / cellWidth;

		ChunkNoiseSampler noiseSampler = new ChunkNoiseSampler(
				1,
				noiseConfig,
				startX,
				startZ,
				shapeConfig,
				DensityFunctionTypes.Beardifier.INSTANCE,
				settings.value(),
				fluidLevelSampler.get(),
				Blender.getNoBlending()
		);
		noiseSampler.sampleStartDensity();
		noiseSampler.sampleEndDensity(0);

		for (int cellY = cellCount - 1; cellY >= 0; cellY--) {
			noiseSampler.onSampledCellCorners(cellY, 0);

			for (int blockInCell = cellHeight - 1; blockInCell >= 0; blockInCell--) {
				int blockY = (minimumCellY + cellY) * cellHeight + blockInCell;
				double deltaY = (double) blockInCell / cellHeight;
				noiseSampler.interpolateY(blockY, deltaY);
				noiseSampler.interpolateX(x, deltaX);
				noiseSampler.interpolateZ(z, deltaZ);

				BlockState sampled = noiseSampler.sampleBlockState();
				BlockState blockState = sampled == null ? settings.value().defaultBlock() : sampled;

				if (blockStates != null) {
					blockStates[cellY * cellHeight + blockInCell] = blockState;
				}

				if (stopPredicate != null && stopPredicate.test(blockState)) {
					noiseSampler.stopInterpolation();
					return OptionalInt.of(blockY + 1);
				}
			}
		}

		noiseSampler.stopInterpolation();
		return OptionalInt.empty();
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
		if (!SharedConstants.isOutsideGenerationArea(chunk.getPos()) && !SharedConstants.DISABLE_SURFACE) {
			HeightContext heightContext = new HeightContext(this, region);
			this.buildSurface(
					chunk,
					heightContext,
					noiseConfig,
					structures,
					region.getBiomeAccess(),
					region.getRegistryManager().getOrThrow(RegistryKeys.BIOME),
					Blender.getBlender(region)
			);
		}
	}

	@VisibleForTesting
	public void buildSurface(
			Chunk chunk,
			HeightContext heightContext,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			BiomeAccess biomeAccess,
			Registry<Biome> biomeRegistry,
			Blender blender
	) {
		ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler(
				chunkx -> this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig)
		);
		ChunkGeneratorSettings chunkGeneratorSettings = this.settings.value();
		noiseConfig.getSurfaceBuilder()
		           .buildSurface(
				           noiseConfig,
				           biomeAccess,
				           biomeRegistry,
				           chunkGeneratorSettings.usesLegacyRandom(),
				           heightContext,
				           chunk,
				           chunkNoiseSampler,
				           chunkGeneratorSettings.surfaceRule()
		           );
	}

	@Override
	public void carve(
			ChunkRegion chunkRegion,
			long seed,
			NoiseConfig noiseConfig,
			BiomeAccess biomeAccess,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		if (SharedConstants.DISABLE_CARVERS) {
			return;
		}

		BiomeAccess noiseBiomeAccess = biomeAccess.withSource(
				(biomeX, biomeY, biomeZ) -> biomeSource.getBiome(
						biomeX,
						biomeY,
						biomeZ,
						noiseConfig.getMultiNoiseSampler()
				)
		);
		ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
		ChunkPos chunkPos = chunk.getPos();
		ChunkNoiseSampler noiseSampler = chunk.getOrCreateChunkNoiseSampler(
				c -> createChunkNoiseSampler(c, structureAccessor, Blender.getBlender(chunkRegion), noiseConfig)
		);
		AquiferSampler aquiferSampler = noiseSampler.getAquiferSampler();
		CarverContext carverContext = new CarverContext(
				this,
				chunkRegion.getRegistryManager(),
				chunk.getHeightLimitView(),
				noiseSampler,
				noiseConfig,
				settings.value().surfaceRule()
		);
		CarvingMask carvingMask = ((ProtoChunk) chunk).getOrCreateCarvingMask();

		for (int dx = -CARVER_SEARCH_RADIUS; dx <= CARVER_SEARCH_RADIUS; dx++) {
			for (int dz = -CARVER_SEARCH_RADIUS; dz <= CARVER_SEARCH_RADIUS; dz++) {
				ChunkPos neighborPos = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
				Chunk neighborChunk = chunkRegion.getChunk(neighborPos.x, neighborPos.z);
				GenerationSettings generationSettings = neighborChunk.getOrCreateGenerationSettings(
						() -> getGenerationSettings(
								biomeSource.getBiome(
										BiomeCoords.fromBlock(neighborPos.getStartX()),
										0,
										BiomeCoords.fromBlock(neighborPos.getStartZ()),
										noiseConfig.getMultiNoiseSampler()
								)
						)
				);
				int carverIndex = 0;

				for (RegistryEntry<ConfiguredCarver<?>> carverEntry : generationSettings.getCarversForStep()) {
					ConfiguredCarver<?> carver = carverEntry.value();
					chunkRandom.setCarverSeed(seed + carverIndex, neighborPos.x, neighborPos.z);

					if (carver.shouldCarve(chunkRandom)) {
						carver.carve(
								carverContext,
								chunk,
								noiseBiomeAccess::getBiome,
								chunkRandom,
								aquiferSampler,
								neighborPos,
								carvingMask
						);
					}

					carverIndex++;
				}
			}
		}
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(
			Blender blender,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		GenerationShapeConfig shapeConfig = settings.value().generationShapeConfig().trimHeight(chunk.getHeightLimitView());
		int minimumY = shapeConfig.minimumY();
		int minimumCellY = MathHelper.floorDiv(minimumY, shapeConfig.verticalCellBlockCount());
		int cellCount = MathHelper.floorDiv(shapeConfig.height(), shapeConfig.verticalCellBlockCount());

		if (cellCount <= 0) {
			return CompletableFuture.completedFuture(chunk);
		}

		return CompletableFuture.supplyAsync(
				() -> {
					int topSectionIndex = chunk.getSectionIndex(cellCount * shapeConfig.verticalCellBlockCount() - 1 + minimumY);
					int bottomSectionIndex = chunk.getSectionIndex(minimumY);
					Set<ChunkSection> lockedSections = Sets.newHashSet();

					for (int sectionIdx = topSectionIndex; sectionIdx >= bottomSectionIndex; sectionIdx--) {
						ChunkSection section = chunk.getSection(sectionIdx);
						section.lock();
						lockedSections.add(section);
					}

					Chunk result;
					try {
						result = this.populateNoise(blender, structureAccessor, noiseConfig, chunk, minimumCellY, cellCount);
					} finally {
						for (ChunkSection section : lockedSections) {
							section.unlock();
						}
					}

					return result;
				}, Util.getMainWorkerExecutor().named("wgen_fill_noise")
		);
	}

	private Chunk populateNoise(
			Blender blender,
			StructureAccessor structureAccessor,
			NoiseConfig noiseConfig,
			Chunk chunk,
			int minimumCellY,
			int cellCount
	) {
		ChunkNoiseSampler noiseSampler = chunk.getOrCreateChunkNoiseSampler(
				c -> createChunkNoiseSampler(c, structureAccessor, blender, noiseConfig)
		);
		Heightmap oceanFloorHeightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
		Heightmap worldSurfaceHeightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
		ChunkPos chunkPos = chunk.getPos();
		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();
		AquiferSampler aquiferSampler = noiseSampler.getAquiferSampler();
		noiseSampler.sampleStartDensity();
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		int cellWidth = noiseSampler.getHorizontalCellBlockCount();
		int cellHeight = noiseSampler.getVerticalCellBlockCount();
		int cellsPerChunkX = 16 / cellWidth;
		int cellsPerChunkZ = 16 / cellWidth;

		for (int cellX = 0; cellX < cellsPerChunkX; cellX++) {
			noiseSampler.sampleEndDensity(cellX);

			for (int cellZ = 0; cellZ < cellsPerChunkZ; cellZ++) {
				int currentSectionIdx = chunk.countVerticalSections() - 1;
				ChunkSection currentSection = chunk.getSection(currentSectionIdx);

				for (int cellY = cellCount - 1; cellY >= 0; cellY--) {
					noiseSampler.onSampledCellCorners(cellY, cellZ);

					for (int blockInCellY = cellHeight - 1; blockInCellY >= 0; blockInCellY--) {
						int blockY = (minimumCellY + cellY) * cellHeight + blockInCellY;
						int localY = blockY & 15;
						int sectionIdx = chunk.getSectionIndex(blockY);

						if (currentSectionIdx != sectionIdx) {
							currentSectionIdx = sectionIdx;
							currentSection = chunk.getSection(sectionIdx);
						}

						double deltaY = (double) blockInCellY / cellHeight;
						noiseSampler.interpolateY(blockY, deltaY);

						for (int blockInCellX = 0; blockInCellX < cellWidth; blockInCellX++) {
							int blockX = startX + cellX * cellWidth + blockInCellX;
							int localX = blockX & 15;
							double deltaX = (double) blockInCellX / cellWidth;
							noiseSampler.interpolateX(blockX, deltaX);

							for (int blockInCellZ = 0; blockInCellZ < cellWidth; blockInCellZ++) {
								int blockZ = startZ + cellZ * cellWidth + blockInCellZ;
								int localZ = blockZ & 15;
								double deltaZ = (double) blockInCellZ / cellWidth;
								noiseSampler.interpolateZ(blockZ, deltaZ);

								BlockState blockState = noiseSampler.sampleBlockState();

								if (blockState == null) {
									blockState = settings.value().defaultBlock();
								}

								blockState = getBlockState(noiseSampler, blockX, blockY, blockZ, blockState);

								if (blockState != AIR && !SharedConstants.isOutsideGenerationArea(chunk.getPos())) {
									currentSection.setBlockState(localX, localY, localZ, blockState, false);
									oceanFloorHeightmap.trackUpdate(localX, blockY, localZ, blockState);
									worldSurfaceHeightmap.trackUpdate(localX, blockY, localZ, blockState);

									if (aquiferSampler.needsFluidTick() && !blockState.getFluidState().isEmpty()) {
										mutablePos.set(blockX, blockY, blockZ);
										chunk.markBlockForPostProcessing(mutablePos);
									}
								}
							}
						}
					}
				}
			}

			noiseSampler.swapBuffers();
		}

		noiseSampler.stopInterpolation();
		return chunk;
	}

	private BlockState getBlockState(ChunkNoiseSampler noiseSampler, int x, int y, int z, BlockState state) {
		if (SharedConstants.AQUIFERS && z >= 0 && z % 4 == 0) {
			int surfaceY = noiseSampler.estimateSurfaceHeight(x, z);
			int debugY = surfaceY + 8;

			if (y == debugY) {
				return debugY < getSeaLevel()
						? Blocks.SLIME_BLOCK.getDefaultState()
						: Blocks.HONEY_BLOCK.getDefaultState();
			}
		}

		return state;
	}

	@Override
	public int getWorldHeight() {
		return settings.value().generationShapeConfig().height();
	}

	@Override
	public int getSeaLevel() {
		return settings.value().seaLevel();
	}

	@Override
	public int getMinimumY() {
		return settings.value().generationShapeConfig().minimumY();
	}

	@Override
	public void populateEntities(ChunkRegion region) {
		if (settings.value().mobGenerationDisabled()) {
			return;
		}

		ChunkPos chunkPos = region.getCenterPos();
		RegistryEntry<Biome> biome = region.getBiome(chunkPos.getStartPos().withY(region.getTopYInclusive()));
		ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
		chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
		SpawnHelper.populateEntities(region, biome, chunkPos, chunkRandom);
	}
}
