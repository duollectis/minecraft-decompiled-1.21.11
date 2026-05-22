package net.minecraft.world.gen.chunk;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureDebugLogger;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Абстрактный генератор чанков. Определяет форму рельефа, биомы, структуры и фичи мира.
 * Конкретные реализации: {@link NoiseChunkGenerator}, {@link FlatChunkGenerator}, {@link DebugChunkGenerator}.
 */
public abstract class ChunkGenerator {

	public static final Codec<ChunkGenerator>
			CODEC =
			Registries.CHUNK_GENERATOR.getCodec().dispatchStable(ChunkGenerator::getCodec, Function.identity());
	protected final BiomeSource biomeSource;
	private final Supplier<List<PlacedFeatureIndexer.IndexedFeatures>> indexedFeaturesListSupplier;
	private final Function<RegistryEntry<Biome>, GenerationSettings> generationSettingsGetter;

	public ChunkGenerator(BiomeSource biomeSource) {
		this(biomeSource, biomeEntry -> biomeEntry.value().getGenerationSettings());
	}

	public ChunkGenerator(
			BiomeSource biomeSource,
			Function<RegistryEntry<Biome>, GenerationSettings> generationSettingsGetter
	) {
		this.biomeSource = biomeSource;
		this.generationSettingsGetter = generationSettingsGetter;
		this.indexedFeaturesListSupplier = Suppliers.memoize(
				() -> PlacedFeatureIndexer.collectIndexedFeatures(
						List.copyOf(biomeSource.getBiomes()),
						biomeEntry -> generationSettingsGetter.apply(biomeEntry).getFeatures(),
						true
				)
		);
	}

	public void initializeIndexedFeaturesList() {
		indexedFeaturesListSupplier.get();
	}

	protected abstract MapCodec<? extends ChunkGenerator> getCodec();

	public StructurePlacementCalculator createStructurePlacementCalculator(
			RegistryWrapper<StructureSet> structureSetRegistry, NoiseConfig noiseConfig, long seed
	) {
		return StructurePlacementCalculator.create(noiseConfig, seed, this.biomeSource, structureSetRegistry);
	}

	public Optional<RegistryKey<MapCodec<? extends ChunkGenerator>>> getCodecKey() {
		return Registries.CHUNK_GENERATOR.getKey(getCodec());
	}

	public CompletableFuture<Chunk> populateBiomes(
			NoiseConfig noiseConfig,
			Blender blender,
			StructureAccessor structureAccessor,
			Chunk chunk
	) {
		return CompletableFuture.supplyAsync(
				() -> {
					chunk.populateBiomes(this.biomeSource, noiseConfig.getMultiNoiseSampler());
					return chunk;
				}, Util.getMainWorkerExecutor().named("init_biomes")
		);
	}

	public abstract void carve(
			ChunkRegion chunkRegion,
			long seed,
			NoiseConfig noiseConfig,
			BiomeAccess biomeAccess,
			StructureAccessor structureAccessor,
			Chunk chunk
	);

	public @Nullable Pair<BlockPos, RegistryEntry<Structure>> locateStructure(
			ServerWorld world,
			RegistryEntryList<Structure> structures,
			BlockPos center,
			int radius,
			boolean skipReferencedStructures
	) {
		if (SharedConstants.DISABLE_FEATURES) {
			return null;
		}

		StructurePlacementCalculator placementCalculator = world.getChunkManager().getStructurePlacementCalculator();
		Map<StructurePlacement, Set<RegistryEntry<Structure>>> placementMap = new Object2ObjectArrayMap();

		for (RegistryEntry<Structure> registryEntry : structures) {
			for (StructurePlacement placement : placementCalculator.getPlacements(registryEntry)) {
				placementMap.computeIfAbsent(placement, p -> new ObjectArraySet()).add(registryEntry);
			}
		}

		if (placementMap.isEmpty()) {
			return null;
		}

		Pair<BlockPos, RegistryEntry<Structure>> closest = null;
		double closestDistSq = Double.MAX_VALUE;
		StructureAccessor structureAccessor = world.getStructureAccessor();
		List<Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> randomSpreadEntries = new ArrayList<>(placementMap.size());

		for (Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : placementMap.entrySet()) {
			StructurePlacement placement = entry.getKey();

			if (placement instanceof ConcentricRingsStructurePlacement concentricPlacement) {
				Pair<BlockPos, RegistryEntry<Structure>> found = locateConcentricRingsStructure(
						entry.getValue(),
						world,
						structureAccessor,
						center,
						skipReferencedStructures,
						concentricPlacement
				);

				if (found != null) {
					double distSq = center.getSquaredDistance(found.getFirst());
					if (distSq < closestDistSq) {
						closestDistSq = distSq;
						closest = found;
					}
				}
			}
			else if (placement instanceof RandomSpreadStructurePlacement) {
				randomSpreadEntries.add(entry);
			}
		}

		if (randomSpreadEntries.isEmpty()) {
			return closest;
		}

		int centerChunkX = ChunkSectionPos.getSectionCoord(center.getX());
		int centerChunkZ = ChunkSectionPos.getSectionCoord(center.getZ());

		for (int searchRadius = 0; searchRadius <= radius; searchRadius++) {
			boolean foundAtRadius = false;

			for (Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry : randomSpreadEntries) {
				RandomSpreadStructurePlacement spreadPlacement = (RandomSpreadStructurePlacement) entry.getKey();
				Pair<BlockPos, RegistryEntry<Structure>> found = locateRandomSpreadStructure(
						entry.getValue(),
						world,
						structureAccessor,
						centerChunkX,
						centerChunkZ,
						searchRadius,
						skipReferencedStructures,
						placementCalculator.getStructureSeed(),
						spreadPlacement
				);

				if (found != null) {
					foundAtRadius = true;
					double distSq = center.getSquaredDistance((Vec3i) found.getFirst());
					if (distSq < closestDistSq) {
						closestDistSq = distSq;
						closest = found;
					}
				}
			}

			if (foundAtRadius) {
				return closest;
			}
		}

		return closest;
	}

	private @Nullable Pair<BlockPos, RegistryEntry<Structure>> locateConcentricRingsStructure(
			Set<RegistryEntry<Structure>> structures,
			ServerWorld world,
			StructureAccessor structureAccessor,
			BlockPos center,
			boolean skipReferencedStructures,
			ConcentricRingsStructurePlacement placement
	) {
		List<ChunkPos> positions = world.getChunkManager().getStructurePlacementCalculator().getPlacementPositions(placement);

		if (positions == null) {
			throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
		}

		Pair<BlockPos, RegistryEntry<Structure>> closest = null;
		double closestDistSq = Double.MAX_VALUE;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (ChunkPos chunkPos : positions) {
			mutable.set(ChunkSectionPos.getOffsetPos(chunkPos.x, 8), 32, ChunkSectionPos.getOffsetPos(chunkPos.z, 8));
			double distSq = mutable.getSquaredDistance(center);

			if (closest == null || distSq < closestDistSq) {
				Pair<BlockPos, RegistryEntry<Structure>> found = locateStructure(
						structures, world, structureAccessor, skipReferencedStructures, placement, chunkPos
				);

				if (found != null) {
					closest = found;
					closestDistSq = distSq;
				}
			}
		}

		return closest;
	}

	private static @Nullable Pair<BlockPos, RegistryEntry<Structure>> locateRandomSpreadStructure(
			Set<RegistryEntry<Structure>> structures,
			WorldView world,
			StructureAccessor structureAccessor,
			int centerChunkX,
			int centerChunkZ,
			int radius,
			boolean skipReferencedStructures,
			long seed,
			RandomSpreadStructurePlacement placement
	) {
		int spacing = placement.getSpacing();

		for (int dx = -radius; dx <= radius; dx++) {
			boolean isEdgeX = dx == -radius || dx == radius;

			for (int dz = -radius; dz <= radius; dz++) {
				boolean isEdgeZ = dz == -radius || dz == radius;

				if (isEdgeX || isEdgeZ) {
					int regionX = centerChunkX + spacing * dx;
					int regionZ = centerChunkZ + spacing * dz;
					ChunkPos chunkPos = placement.getStartChunk(seed, regionX, regionZ);
					Pair<BlockPos, RegistryEntry<Structure>> found = locateStructure(
							structures, world, structureAccessor, skipReferencedStructures, placement, chunkPos
					);

					if (found != null) {
						return found;
					}
				}
			}
		}

		return null;
	}

	private static @Nullable Pair<BlockPos, RegistryEntry<Structure>> locateStructure(
			Set<RegistryEntry<Structure>> structures,
			WorldView world,
			StructureAccessor structureAccessor,
			boolean skipReferencedStructures,
			StructurePlacement placement,
			ChunkPos pos
	) {
		for (RegistryEntry<Structure> registryEntry : structures) {
			StructurePresence
					structurePresence =
					structureAccessor.getStructurePresence(
							pos,
							registryEntry.value(),
							placement,
							skipReferencedStructures
					);
			if (structurePresence != StructurePresence.START_NOT_PRESENT) {
				if (!skipReferencedStructures && structurePresence == StructurePresence.START_PRESENT) {
					return Pair.of(placement.getLocatePos(pos), registryEntry);
				}

				Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
				StructureStart
						structureStart =
						structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), registryEntry.value(), chunk);
				if (structureStart != null && structureStart.hasChildren() && (!skipReferencedStructures
						|| checkNotReferenced(structureAccessor, structureStart)
				)) {
					return Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry);
				}
			}
		}

		return null;
	}

	private static boolean checkNotReferenced(StructureAccessor structureAccessor, StructureStart start) {
		if (start.isNeverReferenced()) {
			structureAccessor.incrementReferences(start);
			return true;
		}

		return false;
	}

	/**
	 * Генерирует фичи (деревья, руды, структуры и т.д.) для данного чанка.
	 * Обходит все шаги генерации {@link GenerationStep.Feature} и для каждого шага
	 * сначала размещает структуры, затем биомные фичи в порядке их индексов.
	 */
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
		ChunkPos chunkPos = chunk.getPos();

		if (SharedConstants.isOutsideGenerationArea(chunkPos)) {
			return;
		}

		ChunkSectionPos sectionPos = ChunkSectionPos.from(chunkPos, world.getBottomSectionCoord());
		BlockPos minPos = sectionPos.getMinPos();
		Registry<Structure> structureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
		Map<Integer, List<Structure>> structuresByStep = structureRegistry.stream()
				.collect(Collectors.groupingBy(s -> s.getFeatureGenerationStep().ordinal()));
		List<PlacedFeatureIndexer.IndexedFeatures> indexedFeatures = indexedFeaturesListSupplier.get();
		ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
		long populationSeed = chunkRandom.setPopulationSeed(world.getSeed(), minPos.getX(), minPos.getZ());
		Set<RegistryEntry<Biome>> localBiomes = new ObjectArraySet();

		ChunkPos.stream(sectionPos.toChunkPos(), 1).forEach(pos -> {
			Chunk nearbyChunk = world.getChunk(pos.x, pos.z);

			for (ChunkSection section : nearbyChunk.getSectionArray()) {
				section.getBiomeContainer().forEachValue(localBiomes::add);
			}
		});

		localBiomes.retainAll(biomeSource.getBiomes());
		int featureStepCount = indexedFeatures.size();

		try {
			Registry<PlacedFeature> featureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.PLACED_FEATURE);
			int totalSteps = Math.max(GenerationStep.Feature.values().length, featureStepCount);

			for (int step = 0; step < totalSteps; step++) {
				int structureIndex = 0;

				if (structureAccessor.shouldGenerateStructures()) {
					for (Structure structure : structuresByStep.getOrDefault(step, Collections.emptyList())) {
						chunkRandom.setDecoratorSeed(populationSeed, structureIndex, step);
						Supplier<String> structureName = () -> structureRegistry
								.getKey(structure)
								.map(Object::toString)
								.orElseGet(structure::toString);

						try {
							world.setCurrentlyGeneratingStructureName(structureName);
							structureAccessor.getStructureStarts(sectionPos, structure)
									.forEach(start -> start.place(
											world,
											structureAccessor,
											this,
											chunkRandom,
											getBlockBoxForChunk(chunk),
											chunkPos
									));
						}
						catch (Exception ex) {
							CrashReport report = CrashReport.create(ex, "Feature placement");
							report.addElement("Feature").add("Description", structureName::get);
							throw new CrashException(report);
						}

						structureIndex++;
					}
				}

				if (step >= featureStepCount) {
					continue;
				}

				IntSet featureIndices = new IntArraySet();

				for (RegistryEntry<Biome> biomeEntry : localBiomes) {
					List<RegistryEntryList<PlacedFeature>> biomeFeatures = generationSettingsGetter
							.apply(biomeEntry)
							.getFeatures();

					if (step < biomeFeatures.size()) {
						RegistryEntryList<PlacedFeature> stepFeatures = biomeFeatures.get(step);
						PlacedFeatureIndexer.IndexedFeatures stepIndexed = indexedFeatures.get(step);
						stepFeatures.stream()
								.map(RegistryEntry::value)
								.forEach(f -> featureIndices.add(stepIndexed.indexMapping().applyAsInt(f)));
					}
				}

				int featureCount = featureIndices.size();
				int[] sortedIndices = featureIndices.toIntArray();
				Arrays.sort(sortedIndices);
				PlacedFeatureIndexer.IndexedFeatures stepIndexed = indexedFeatures.get(step);

				for (int fi = 0; fi < featureCount; fi++) {
					int featureIdx = sortedIndices[fi];
					PlacedFeature feature = stepIndexed.features().get(featureIdx);
					Supplier<String> featureName = () -> featureRegistry
							.getKey(feature)
							.map(Object::toString)
							.orElseGet(feature::toString);
					chunkRandom.setDecoratorSeed(populationSeed, featureIdx, step);

					try {
						world.setCurrentlyGeneratingStructureName(featureName);
						feature.generate(world, this, chunkRandom, minPos);
					}
					catch (Exception ex) {
						CrashReport report = CrashReport.create(ex, "Feature placement");
						report.addElement("Feature").add("Description", featureName::get);
						throw new CrashException(report);
					}
				}
			}

			world.setCurrentlyGeneratingStructureName(null);

			if (SharedConstants.FEATURE_COUNT) {
				FeatureDebugLogger.incrementTotalChunksCount(world.toServerWorld());
			}
		}
		catch (Exception ex) {
			CrashReport report = CrashReport.create(ex, "Biome decoration");
			report.addElement("Generation")
					.add("CenterX", chunkPos.x)
					.add("CenterZ", chunkPos.z)
					.add("Decoration Seed", populationSeed);
			throw new CrashException(report);
		}
	}

	private static BlockBox getBlockBoxForChunk(Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();
		HeightLimitView heightLimit = chunk.getHeightLimitView();
		int bottomY = heightLimit.getBottomY() + 1;
		int topY = heightLimit.getTopYInclusive();
		return new BlockBox(startX, bottomY, startZ, startX + 15, topY, startZ + 15);
	}

	public abstract void buildSurface(
			ChunkRegion region,
			StructureAccessor structures,
			NoiseConfig noiseConfig,
			Chunk chunk
	);

	public abstract void populateEntities(ChunkRegion region);

	public int getSpawnHeight(HeightLimitView world) {
		return 64;
	}

	public BiomeSource getBiomeSource() {
		return biomeSource;
	}

	public abstract int getWorldHeight();

	public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(
			RegistryEntry<Biome> biome,
			StructureAccessor accessor,
			SpawnGroup group,
			BlockPos pos
	) {
		Map<Structure, LongSet> map = accessor.getStructureReferences(pos);

		for (Entry<Structure, LongSet> entry : map.entrySet()) {
			Structure structure = entry.getKey();
			StructureSpawns structureSpawns = structure.getStructureSpawns().get(group);
			if (structureSpawns != null) {
				MutableBoolean mutableBoolean = new MutableBoolean(false);
				Predicate<StructureStart> predicate = structureSpawns.boundingBox() == StructureSpawns.BoundingBox.PIECE
				                                      ? start -> accessor.structureContains(pos, start)
				                                      : start -> start.getBoundingBox().contains(pos);
				accessor.acceptStructureStarts(
						structure, entry.getValue(), start -> {
							if (mutableBoolean.isFalse() && predicate.test(start)) {
								mutableBoolean.setTrue();
							}
						}
				);
				if (mutableBoolean.isTrue()) {
					return structureSpawns.spawns();
				}
			}
		}

		return biome.value().getSpawnSettings().getSpawnEntries(group);
	}

	public void setStructureStarts(
			DynamicRegistryManager registryManager,
			StructurePlacementCalculator placementCalculator,
			StructureAccessor structureAccessor,
			Chunk chunk,
			StructureTemplateManager structureTemplateManager,
			RegistryKey<World> dimension
	) {
		if (SharedConstants.DISABLE_STRUCTURES) {
			return;
		}

		ChunkPos chunkPos = chunk.getPos();
		ChunkSectionPos sectionPos = ChunkSectionPos.from(chunk);
		NoiseConfig noiseConfig = placementCalculator.getNoiseConfig();

		placementCalculator.getStructureSets().forEach(structureSetEntry -> {
			StructurePlacement placement = structureSetEntry.value().placement();
			List<StructureSet.WeightedEntry> entries = structureSetEntry.value().structures();

			for (StructureSet.WeightedEntry entry : entries) {
				StructureStart existing = structureAccessor.getStructureStart(sectionPos, entry.structure().value(), chunk);
				if (existing != null && existing.hasChildren()) {
					return;
				}
			}

			if (!placement.shouldGenerate(placementCalculator, chunkPos.x, chunkPos.z)) {
				return;
			}

			if (entries.size() == 1) {
				trySetStructureStart(
						entries.get(0),
						structureAccessor,
						registryManager,
						noiseConfig,
						structureTemplateManager,
						placementCalculator.getStructureSeed(),
						chunk,
						chunkPos,
						sectionPos,
						dimension
				);
				return;
			}

			ArrayList<StructureSet.WeightedEntry> candidates = new ArrayList<>(entries);
			ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(0L));
			chunkRandom.setCarverSeed(placementCalculator.getStructureSeed(), chunkPos.x, chunkPos.z);
			int totalWeight = 0;

			for (StructureSet.WeightedEntry entry : candidates) {
				totalWeight += entry.weight();
			}

			while (!candidates.isEmpty()) {
				int roll = chunkRandom.nextInt(totalWeight);
				int selectedIndex = 0;

				for (StructureSet.WeightedEntry entry : candidates) {
					roll -= entry.weight();
					if (roll < 0) {
						break;
					}

					selectedIndex++;
				}

				StructureSet.WeightedEntry selected = candidates.get(selectedIndex);

				if (trySetStructureStart(
						selected,
						structureAccessor,
						registryManager,
						noiseConfig,
						structureTemplateManager,
						placementCalculator.getStructureSeed(),
						chunk,
						chunkPos,
						sectionPos,
						dimension
				)) {
					return;
				}

				candidates.remove(selectedIndex);
				totalWeight -= selected.weight();
			}
		});
	}

	private boolean trySetStructureStart(
			StructureSet.WeightedEntry weightedEntry,
			StructureAccessor structureAccessor,
			DynamicRegistryManager dynamicRegistryManager,
			NoiseConfig noiseConfig,
			StructureTemplateManager structureManager,
			long seed,
			Chunk chunk,
			ChunkPos pos,
			ChunkSectionPos sectionPos,
			RegistryKey<World> dimension
	) {
		Structure structure = weightedEntry.structure().value();
		int references = getStructureReferences(structureAccessor, chunk, sectionPos, structure);
		Predicate<RegistryEntry<Biome>> validBiome = structure.getValidBiomes()::contains;
		StructureStart structureStart = structure.createStructureStart(
				weightedEntry.structure(),
				dimension,
				dynamicRegistryManager,
				this,
				biomeSource,
				noiseConfig,
				structureManager,
				seed,
				pos,
				references,
				chunk,
				validBiome
		);

		if (structureStart.hasChildren()) {
			structureAccessor.setStructureStart(sectionPos, structure, structureStart, chunk);
			return true;
		}

		return false;
	}

	private static int getStructureReferences(
			StructureAccessor structureAccessor,
			Chunk chunk,
			ChunkSectionPos sectionPos,
			Structure structure
	) {
		StructureStart structureStart = structureAccessor.getStructureStart(sectionPos, structure, chunk);
		return structureStart != null ? structureStart.getReferences() : 0;
	}

	/**
	 * Регистрирует ссылки на структуры из соседних чанков (в радиусе 8 чанков),
	 * чьи ограничивающие боксы пересекают текущий чанк.
	 */
	public void addStructureReferences(StructureWorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {
		final int searchRadius = 8;
		ChunkPos chunkPos = chunk.getPos();
		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;
		int startX = chunkPos.getStartX();
		int startZ = chunkPos.getStartZ();
		ChunkSectionPos sectionPos = ChunkSectionPos.from(chunk);

		for (int nx = chunkX - searchRadius; nx <= chunkX + searchRadius; nx++) {
			for (int nz = chunkZ - searchRadius; nz <= chunkZ + searchRadius; nz++) {
				long neighborChunkKey = ChunkPos.toLong(nx, nz);

				for (StructureStart structureStart : world.getChunk(nx, nz).getStructureStarts().values()) {
					try {
						if (structureStart.hasChildren()
								&& structureStart.getBoundingBox().intersectsXZ(startX, startZ, startX + 15, startZ + 15)
						) {
							structureAccessor.addStructureReference(
									sectionPos,
									structureStart.getStructure(),
									neighborChunkKey,
									chunk
							);
						}
					}
					catch (Exception ex) {
						CrashReport report = CrashReport.create(ex, "Generating structure reference");
						CrashReportSection section = report.addElement("Structure");
						Optional<? extends Registry<Structure>> structureRegistry = world.getRegistryManager()
								.getOptional(RegistryKeys.STRUCTURE);
						section.add(
								"Id",
								() -> structureRegistry
										.<String>map(reg -> reg.getId(structureStart.getStructure()).toString())
										.orElse("UNKNOWN")
						);
						section.add(
								"Name",
								() -> Registries.STRUCTURE_TYPE.getId(structureStart.getStructure().getType()).toString()
						);
						section.add("Class", () -> structureStart.getStructure().getClass().getCanonicalName());
						throw new CrashException(report);
					}
				}
			}
		}
	}

	public abstract CompletableFuture<Chunk> populateNoise(
			Blender blender,
			NoiseConfig noiseConfig,
			StructureAccessor structureAccessor,
			Chunk chunk
	);

	public abstract int getSeaLevel();

	public abstract int getMinimumY();

	public abstract int getHeight(
			int x,
			int z,
			Heightmap.Type heightmap,
			HeightLimitView world,
			NoiseConfig noiseConfig
	);

	public abstract VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig);

	public int getHeightOnGround(
			int x,
			int z,
			Heightmap.Type heightmap,
			HeightLimitView world,
			NoiseConfig noiseConfig
	) {
		return getHeight(x, z, heightmap, world, noiseConfig);
	}

	public int getHeightInGround(
			int x,
			int z,
			Heightmap.Type heightmap,
			HeightLimitView world,
			NoiseConfig noiseConfig
	) {
		return getHeight(x, z, heightmap, world, noiseConfig) - 1;
	}

	public abstract void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos);

	@Deprecated
	public GenerationSettings getGenerationSettings(RegistryEntry<Biome> biomeEntry) {
		return generationSettingsGetter.apply(biomeEntry);
	}
}
