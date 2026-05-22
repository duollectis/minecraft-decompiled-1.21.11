package net.minecraft.world;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.scanner.NbtScanQuery;
import net.minecraft.nbt.scanner.SelectiveNbtCollector;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.storage.NbtScannable;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Локатор структур мира.
 * Кэширует результаты сканирования чанков для быстрого определения наличия структур
 * без полной загрузки чанка. Использует частичное чтение NBT через {@link SelectiveNbtCollector}.
 */
public class StructureLocator {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Значение, означающее отсутствие структуры в чанке. */
	private static final int START_NOT_PRESENT_REFERENCE = -1;

	/** Минимальная версия данных чанка, при которой возможно частичное сканирование. */
	private static final int MIN_SCANNABLE_DATA_VERSION = 1493;

	/** Идентификатор «невалидного» старта структуры в NBT. */
	private static final String INVALID_STRUCTURE_ID = "INVALID";

	private final NbtScannable chunkIoWorker;
	private final DynamicRegistryManager registryManager;
	private final StructureTemplateManager structureTemplateManager;
	private final RegistryKey<World> worldKey;
	private final ChunkGenerator chunkGenerator;
	private final NoiseConfig noiseConfig;
	private final HeightLimitView world;
	private final BiomeSource biomeSource;
	private final long seed;
	private final DataFixer dataFixer;

	private final Long2ObjectMap<Object2IntMap<Structure>> cachedStructuresByChunkPos = new Long2ObjectOpenHashMap<>();
	private final Map<Structure, Long2BooleanMap> generationPossibilityByStructure = new HashMap<>();

	public StructureLocator(
		NbtScannable chunkIoWorker,
		DynamicRegistryManager registryManager,
		StructureTemplateManager structureTemplateManager,
		RegistryKey<World> worldKey,
		ChunkGenerator chunkGenerator,
		NoiseConfig noiseConfig,
		HeightLimitView world,
		BiomeSource biomeSource,
		long seed,
		DataFixer dataFixer
	) {
		this.chunkIoWorker = chunkIoWorker;
		this.registryManager = registryManager;
		this.structureTemplateManager = structureTemplateManager;
		this.worldKey = worldKey;
		this.chunkGenerator = chunkGenerator;
		this.noiseConfig = noiseConfig;
		this.world = world;
		this.biomeSource = biomeSource;
		this.seed = seed;
		this.dataFixer = dataFixer;
	}

	/**
	 * Определяет наличие структуры в чанке.
	 * Сначала проверяет кэш, затем сканирует NBT чанка, и наконец
	 * проверяет возможность генерации структуры в данном биоме.
	 *
	 * @param pos                      позиция чанка
	 * @param type                     тип структуры
	 * @param placement                правила размещения структуры
	 * @param skipReferencedStructures пропускать ли уже использованные структуры
	 * @return статус присутствия структуры
	 */
	public StructurePresence getStructurePresence(
		ChunkPos pos,
		Structure type,
		StructurePlacement placement,
		boolean skipReferencedStructures
	) {
		long packedPos = pos.toLong();
		Object2IntMap<Structure> cached = cachedStructuresByChunkPos.get(packedPos);

		if (cached != null) {
			return getStructurePresence(cached, type, skipReferencedStructures);
		}

		StructurePresence scanned = getStructurePresenceFromDisk(pos, type, skipReferencedStructures, packedPos);

		if (scanned != null) {
			return scanned;
		}

		if (!placement.applyFrequencyReduction(pos.x, pos.z, seed)) {
			return StructurePresence.START_NOT_PRESENT;
		}

		boolean canGenerate = generationPossibilityByStructure
			.computeIfAbsent(type, s -> new Long2BooleanOpenHashMap())
			.computeIfAbsent(packedPos, chunkPos -> isGenerationPossible(pos, type));

		return canGenerate ? StructurePresence.CHUNK_LOAD_NEEDED : StructurePresence.START_NOT_PRESENT;
	}

	private boolean isGenerationPossible(ChunkPos pos, Structure structure) {
		return structure.getValidStructurePosition(
			new Structure.Context(
				registryManager,
				chunkGenerator,
				biomeSource,
				noiseConfig,
				structureTemplateManager,
				seed,
				pos,
				world,
				structure.getValidBiomes()::contains
			)
		).isPresent();
	}

	private @Nullable StructurePresence getStructurePresenceFromDisk(
		ChunkPos pos,
		Structure structure,
		boolean skipReferencedStructures,
		long packedPos
	) {
		SelectiveNbtCollector collector = new SelectiveNbtCollector(
			new NbtScanQuery(NbtInt.TYPE, "DataVersion"),
			new NbtScanQuery("Level", "Structures", NbtCompound.TYPE, "Starts"),
			new NbtScanQuery("structures", NbtCompound.TYPE, "starts")
		);

		try {
			chunkIoWorker.scanChunk(pos, collector).join();
		} catch (Exception e) {
			LOGGER.warn("Failed to read chunk {}", pos, e);
			return StructurePresence.CHUNK_LOAD_NEEDED;
		}

		if (!(collector.getRoot() instanceof NbtCompound nbt)) {
			return null;
		}

		int dataVersion = NbtHelper.getDataVersion(nbt);

		if (dataVersion <= MIN_SCANNABLE_DATA_VERSION) {
			return StructurePresence.CHUNK_LOAD_NEEDED;
		}

		VersionedChunkStorage.saveContextToNbt(
			nbt,
			ServerChunkLoadingManager.getContextNbt(worldKey, chunkGenerator.getCodecKey())
		);

		NbtCompound updatedNbt;

		try {
			updatedNbt = DataFixTypes.CHUNK.update(dataFixer, nbt, dataVersion);
		} catch (Exception e) {
			LOGGER.warn("Failed to partially datafix chunk {}", pos, e);
			return StructurePresence.CHUNK_LOAD_NEEDED;
		}

		Object2IntMap<Structure> structureMap = collectStructuresAndReferences(updatedNbt);

		if (structureMap == null) {
			return null;
		}

		cache(packedPos, structureMap);
		return getStructurePresence(structureMap, structure, skipReferencedStructures);
	}

	private @Nullable Object2IntMap<Structure> collectStructuresAndReferences(NbtCompound nbt) {
		Optional<NbtCompound> startsNbt = nbt.getCompound("structures")
			.flatMap(structures -> structures.getCompound("starts"));

		if (startsNbt.isEmpty()) {
			return null;
		}

		NbtCompound starts = startsNbt.get();

		if (starts.isEmpty()) {
			return Object2IntMaps.emptyMap();
		}

		Object2IntMap<Structure> result = new Object2IntOpenHashMap<>();
		Registry<Structure> registry = registryManager.getOrThrow(RegistryKeys.STRUCTURE);

		starts.forEach((key, element) -> {
			Identifier id = Identifier.tryParse(key);

			if (id == null) {
				return;
			}

			Structure structure = registry.get(id);

			if (structure == null) {
				return;
			}

			element.asCompound().ifPresent(startNbt -> {
				if (INVALID_STRUCTURE_ID.equals(startNbt.getString("id", ""))) {
					return;
				}

				int references = startNbt.getInt("references", 0);
				result.put(structure, references);
			});
		});

		return result;
	}

	private static Object2IntMap<Structure> normalizeMap(Object2IntMap<Structure> map) {
		return map.isEmpty() ? Object2IntMaps.emptyMap() : map;
	}

	private StructurePresence getStructurePresence(
		Object2IntMap<Structure> referencesByStructure,
		Structure structure,
		boolean skipReferencedStructures
	) {
		int references = referencesByStructure.getOrDefault(structure, START_NOT_PRESENT_REFERENCE);
		return references == START_NOT_PRESENT_REFERENCE || skipReferencedStructures && references != 0
			? StructurePresence.START_NOT_PRESENT
			: StructurePresence.START_PRESENT;
	}

	/**
	 * Кэширует информацию о структурах в чанке после его полной загрузки.
	 *
	 * @param pos             позиция чанка
	 * @param structureStarts карта стартов структур в чанке
	 */
	public void cache(ChunkPos pos, Map<Structure, StructureStart> structureStarts) {
		long packedPos = pos.toLong();
		Object2IntMap<Structure> references = new Object2IntOpenHashMap<>();

		structureStarts.forEach((structure, start) -> {
			if (start.hasChildren()) {
				references.put(structure, start.getReferences());
			}
		});

		cache(packedPos, references);
	}

	private void cache(long pos, Object2IntMap<Structure> referencesByStructure) {
		cachedStructuresByChunkPos.put(pos, normalizeMap(referencesByStructure));
		generationPossibilityByStructure.values().forEach(map -> map.remove(pos));
	}

	/**
	 * Увеличивает счётчик ссылок на структуру в кэше для заданного чанка.
	 * Вызывается при генерации новых структур, ссылающихся на данный чанк.
	 *
	 * @param pos       позиция чанка
	 * @param structure тип структуры
	 */
	public void incrementReferences(ChunkPos pos, Structure structure) {
		cachedStructuresByChunkPos.compute(pos.toLong(), (key, existing) -> {
			if (existing == null || existing.isEmpty()) {
				existing = new Object2IntOpenHashMap<>();
			}

			existing.computeInt(structure, (feat, refs) -> refs == null ? 1 : refs + 1);
			return existing;
		});
	}
}
