package net.minecraft.world.gen.chunk.placement;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Вычисляет и кэширует позиции размещения структур для данного мира.
 * <p>
 * Для {@link RandomSpreadStructurePlacement} позиции вычисляются детерминированно
 * на лету. Для {@link ConcentricRingsStructurePlacement} (Крепости) позиции
 * вычисляются асинхронно через {@link CompletableFuture} при первом обращении.
 */
public class StructurePlacementCalculator {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final NoiseConfig noiseConfig;
	private final BiomeSource biomeSource;
	private final long structureSeed;
	private final long concentricRingSeed;
	private final List<RegistryEntry<StructureSet>> structureSets;
	private final Map<Structure, List<StructurePlacement>> structuresToPlacements = new Object2ObjectOpenHashMap<>();
	private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>>
			concentricPlacementsToPositions = new Object2ObjectArrayMap<>();
	private boolean calculated;

	/**
	 * Создаёт калькулятор для произвольного потока наборов структур.
	 * Используется при загрузке мира из кастомного источника данных.
	 * Seed концентрических колец фиксирован в 0 — кольца не привязаны к seed мира.
	 */
	public static StructurePlacementCalculator create(
			NoiseConfig noiseConfig,
			long seed,
			BiomeSource biomeSource,
			Stream<RegistryEntry<StructureSet>> structureSets
	) {
		List<RegistryEntry<StructureSet>> filtered = structureSets
				.filter(entry -> hasValidBiome(entry.value(), biomeSource))
				.toList();
		return new StructurePlacementCalculator(noiseConfig, biomeSource, seed, 0L, filtered);
	}

	/**
	 * Создаёт калькулятор из реестра наборов структур.
	 * Seed концентрических колец совпадает с seed мира — стандартное поведение.
	 */
	public static StructurePlacementCalculator create(
			NoiseConfig noiseConfig,
			long seed,
			BiomeSource biomeSource,
			RegistryWrapper<StructureSet> structureSetRegistry
	) {
		List<RegistryEntry<StructureSet>> filtered = structureSetRegistry.streamEntries()
				.filter(entry -> hasValidBiome(entry.value(), biomeSource))
				.collect(Collectors.toUnmodifiableList());
		return new StructurePlacementCalculator(noiseConfig, biomeSource, seed, seed, filtered);
	}

	private static boolean hasValidBiome(StructureSet structureSet, BiomeSource biomeSource) {
		Set<RegistryEntry<Biome>> worldBiomes = biomeSource.getBiomes();
		return structureSet.structures()
		                   .stream()
		                   .flatMap(entry -> entry.structure().value().getValidBiomes().stream())
		                   .anyMatch(worldBiomes::contains);
	}

	private StructurePlacementCalculator(
			NoiseConfig noiseConfig,
			BiomeSource biomeSource,
			long structureSeed,
			long concentricRingSeed,
			List<RegistryEntry<StructureSet>> structureSets
	) {
		this.noiseConfig = noiseConfig;
		this.structureSeed = structureSeed;
		this.biomeSource = biomeSource;
		this.concentricRingSeed = concentricRingSeed;
		this.structureSets = structureSets;
	}

	public List<RegistryEntry<StructureSet>> getStructureSets() {
		return structureSets;
	}

	/**
	 * Заполняет карты {@code structuresToPlacements} и {@code concentricPlacementsToPositions}.
	 * Для каждого набора структур проверяет совместимость биомов и регистрирует размещения.
	 * Для концентрических колец запускает асинхронное вычисление позиций.
	 */
	private void calculate() {
		Set<RegistryEntry<Biome>> worldBiomes = biomeSource.getBiomes();

		getStructureSets().forEach(structureSetEntry -> {
			StructureSet set = structureSetEntry.value();
			boolean hasValidStructure = false;

			for (StructureSet.WeightedEntry weightedEntry : set.structures()) {
				Structure structure = weightedEntry.structure().value();

				if (structure.getValidBiomes().stream().anyMatch(worldBiomes::contains)) {
					structuresToPlacements
							.computeIfAbsent(structure, key -> new ArrayList<>())
							.add(set.placement());
					hasValidStructure = true;
				}
			}

			if (hasValidStructure
					&& set.placement() instanceof ConcentricRingsStructurePlacement concentricPlacement) {
				concentricPlacementsToPositions.put(
						concentricPlacement,
						calculateConcentricRingPositions(structureSetEntry, concentricPlacement)
				);
			}
		});
	}

	/**
	 * Асинхронно вычисляет позиции чанков для размещения структур по концентрическим кольцам.
	 * <p>
	 * Алгоритм: структуры равномерно распределяются по кольцам, каждое кольцо расположено
	 * дальше от центра. Для каждой позиции ищется ближайший подходящий биом из
	 * {@code preferredBiomes}. Если биом не найден — используется исходная позиция кольца.
	 */
	@SuppressWarnings("unchecked")
	private CompletableFuture<List<ChunkPos>> calculateConcentricRingPositions(
			RegistryEntry<StructureSet> structureSetEntry,
			ConcentricRingsStructurePlacement placement
	) {
		if (placement.getCount() == 0) {
			return CompletableFuture.completedFuture(List.of());
		}

		Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
		int distance = placement.getDistance();
		int totalCount = placement.getCount();
		int ringSpread = placement.getSpread();
		RegistryEntryList<Biome> preferredBiomes = placement.getPreferredBiomes();

		Random random = Random.create();
		random.setSeed(concentricRingSeed);

		double angle = random.nextDouble() * Math.PI * 2.0;
		int positionsInCurrentRing = 0;
		int currentRing = 0;

		List<CompletableFuture<ChunkPos>> futures = new ArrayList<>(totalCount);

		for (int placed = 0; placed < totalCount; placed++) {
			double radius = 4 * distance + distance * currentRing * 6 + (random.nextDouble() - 0.5) * (distance * 2.5);
			int targetChunkX = (int) Math.round(Math.cos(angle) * radius);
			int targetChunkZ = (int) Math.round(Math.sin(angle) * radius);
			Random splitRandom = random.split();

			futures.add(CompletableFuture.supplyAsync(
					() -> {
						Pair<BlockPos, RegistryEntry<Biome>> biomeResult = biomeSource.locateBiome(
								ChunkSectionPos.getOffsetPos(targetChunkX, 8),
								0,
								ChunkSectionPos.getOffsetPos(targetChunkZ, 8),
								112,
								preferredBiomes::contains,
								splitRandom,
								noiseConfig.getMultiNoiseSampler()
						);

						if (biomeResult != null) {
							BlockPos biomePos = (BlockPos) biomeResult.getFirst();
							return new ChunkPos(
									ChunkSectionPos.getSectionCoord(biomePos.getX()),
									ChunkSectionPos.getSectionCoord(biomePos.getZ())
							);
						}

						return new ChunkPos(targetChunkX, targetChunkZ);
					},
					Util.getMainWorkerExecutor().named("structureRings")
			));

			angle += (Math.PI * 2) / ringSpread;

			if (++positionsInCurrentRing == ringSpread) {
				currentRing++;
				positionsInCurrentRing = 0;
				ringSpread += 2 * ringSpread / (currentRing + 1);
				ringSpread = Math.min(ringSpread, totalCount - placed);
				angle += random.nextDouble() * Math.PI * 2.0;
			}
		}

		return Util.combineSafe(futures).thenApply(positions -> {
			double elapsedSeconds = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0;
			LOGGER.debug("Calculation for {} took {}s", structureSetEntry, elapsedSeconds);
			return positions;
		});
	}

	/**
	 * Инициализирует вычисление позиций при первом обращении (ленивая инициализация).
	 */
	public void tryCalculate() {
		if (!calculated) {
			calculate();
			calculated = true;
		}
	}

	public @Nullable List<ChunkPos> getPlacementPositions(ConcentricRingsStructurePlacement placement) {
		tryCalculate();
		CompletableFuture<List<ChunkPos>> future = concentricPlacementsToPositions.get(placement);
		return future != null ? future.join() : null;
	}

	public List<StructurePlacement> getPlacements(RegistryEntry<Structure> structureEntry) {
		tryCalculate();
		return structuresToPlacements.getOrDefault(structureEntry.value(), List.of());
	}

	public NoiseConfig getNoiseConfig() {
		return noiseConfig;
	}

	/**
	 * Проверяет, может ли структура из {@code structureSetEntry} генерироваться
	 * в квадрате чанков с центром {@code (centerChunkX, centerChunkZ)} и радиусом {@code chunkCount}.
	 */
	public boolean canGenerate(
			RegistryEntry<StructureSet> structureSetEntry,
			int centerChunkX,
			int centerChunkZ,
			int chunkCount
	) {
		StructurePlacement placement = structureSetEntry.value().placement();

		for (int x = centerChunkX - chunkCount; x <= centerChunkX + chunkCount; x++) {
			for (int z = centerChunkZ - chunkCount; z <= centerChunkZ + chunkCount; z++) {
				if (placement.shouldGenerate(this, x, z)) {
					return true;
				}
			}
		}

		return false;
	}

	public long getStructureSeed() {
		return structureSeed;
	}
}
