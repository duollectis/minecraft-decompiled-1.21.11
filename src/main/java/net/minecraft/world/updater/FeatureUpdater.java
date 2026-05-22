package net.minecraft.world.updater;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkUpdateState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Мигрирует устаревшие данные структур (до версии 1493) в новый формат.
 * <p>
 * До версии 1493 данные о стартовых позициях структур хранились в отдельных
 * файлах {@code .dat} (например, {@code Village.dat}). Этот класс читает
 * эти файлы при первом обращении и встраивает данные непосредственно в NBT
 * каждого чанка в секции {@code Level.Structures}.
 */
public class FeatureUpdater implements ChunkUpdater {

	public static final int TARGET_DATA_VERSION = 1493;

	/** Радиус поиска соседних чанков при восстановлении ссылок на структуры. */
	private static final int STRUCTURE_SEARCH_RADIUS = 8;

	private static final Map<String, String> OLD_TO_NEW = Util.make(
		Maps.newHashMap(), map -> {
			map.put("Village", "Village");
			map.put("Mineshaft", "Mineshaft");
			map.put("Mansion", "Mansion");
			map.put("Igloo", "Temple");
			map.put("Desert_Pyramid", "Temple");
			map.put("Jungle_Pyramid", "Temple");
			map.put("Swamp_Hut", "Temple");
			map.put("Stronghold", "Stronghold");
			map.put("Monument", "Monument");
			map.put("Fortress", "Fortress");
			map.put("EndCity", "EndCity");
		}
	);

	private static final Map<String, String> ANCIENT_TO_OLD = Util.make(
		Maps.newHashMap(), map -> {
			map.put("Iglu", "Igloo");
			map.put("TeDP", "Desert_Pyramid");
			map.put("TeJP", "Jungle_Pyramid");
			map.put("TeSH", "Swamp_Hut");
		}
	);

	private static final java.util.Set<String> NEW_STRUCTURE_NAMES = java.util.Set.of(
		"pillager_outpost",
		"mineshaft",
		"mansion",
		"jungle_pyramid",
		"desert_pyramid",
		"igloo",
		"ruined_portal",
		"shipwreck",
		"swamp_hut",
		"stronghold",
		"monument",
		"ocean_ruin",
		"fortress",
		"endcity",
		"buried_treasure",
		"village",
		"nether_fossil",
		"bastion_remnant"
	);

	private final boolean needsUpdate;
	private final Map<String, Long2ObjectMap<NbtCompound>> featureIdToChunkNbt = Maps.newHashMap();
	private final Map<String, ChunkUpdateState> updateStates = Maps.newHashMap();
	private final @Nullable PersistentStateManager persistentStateManager;
	private final List<String> oldNames;
	private final List<String> newNames;
	private final DataFixer dataFixer;
	private boolean initialized;

	public FeatureUpdater(
		@Nullable PersistentStateManager persistentStateManager,
		List<String> oldNames,
		List<String> newNames,
		DataFixer dataFixer
	) {
		this.persistentStateManager = persistentStateManager;
		this.oldNames = oldNames;
		this.newNames = newNames;
		this.dataFixer = dataFixer;

		boolean hasData = false;

		for (String name : newNames) {
			hasData |= featureIdToChunkNbt.get(name) != null;
		}

		needsUpdate = hasData;
	}

	@Override
	public void markChunkDone(ChunkPos chunkPos) {
		long chunkKey = chunkPos.toLong();

		for (String name : oldNames) {
			ChunkUpdateState state = updateStates.get(name);

			if (state != null && state.isRemaining(chunkKey)) {
				state.markResolved(chunkKey);
			}
		}
	}

	@Override
	public int targetDataVersion() {
		return TARGET_DATA_VERSION;
	}

	@Override
	public NbtCompound applyFix(NbtCompound nbt) {
		if (!initialized && persistentStateManager != null) {
			init(persistentStateManager);
		}

		int dataVersion = NbtHelper.getDataVersion(nbt);

		if (dataVersion >= TARGET_DATA_VERSION) {
			return nbt;
		}

		nbt = DataFixTypes.CHUNK.update(dataFixer, nbt, dataVersion, TARGET_DATA_VERSION);

		boolean hasLegacyData = nbt
			.getCompound("Level")
			.flatMap(levelTag -> levelTag.getBoolean("hasLegacyStructureData"))
			.orElse(false);

		return hasLegacyData ? getUpdatedReferences(nbt) : nbt;
	}

	/**
	 * Восстанавливает ссылки на структуры в секции {@code Level.Structures.References}.
	 * <p>
	 * Для каждой новой структуры, у которой ещё нет ссылок, сканирует квадрат
	 * {@code STRUCTURE_SEARCH_RADIUS × STRUCTURE_SEARCH_RADIUS} соседних чанков
	 * и добавляет ключи тех, в которых структура действительно начинается.
	 */
	private NbtCompound getUpdatedReferences(NbtCompound nbt) {
		NbtCompound levelTag = nbt.getCompoundOrEmpty("Level");
		ChunkPos chunkPos = new ChunkPos(levelTag.getInt("xPos", 0), levelTag.getInt("zPos", 0));

		if (needsUpdate(chunkPos.x, chunkPos.z)) {
			nbt = getUpdatedStarts(nbt, chunkPos);
		}

		NbtCompound structuresTag = levelTag.getCompoundOrEmpty("Structures");
		NbtCompound referencesTag = structuresTag.getCompoundOrEmpty("References");

		for (String name : newNames) {
			boolean isKnownStructure = NEW_STRUCTURE_NAMES.contains(name.toLowerCase(Locale.ROOT));

			if (referencesTag.getLongArray(name).isPresent() || !isKnownStructure) {
				continue;
			}

			LongList references = new LongArrayList();

			for (int chunkX = chunkPos.x - STRUCTURE_SEARCH_RADIUS; chunkX <= chunkPos.x + STRUCTURE_SEARCH_RADIUS; chunkX++) {
				for (int chunkZ = chunkPos.z - STRUCTURE_SEARCH_RADIUS; chunkZ <= chunkPos.z + STRUCTURE_SEARCH_RADIUS; chunkZ++) {
					if (needsUpdate(chunkX, chunkZ, name)) {
						references.add(ChunkPos.toLong(chunkX, chunkZ));
					}
				}
			}

			referencesTag.putLongArray(name, references.toLongArray());
		}

		structuresTag.put("References", referencesTag);
		levelTag.put("Structures", structuresTag);
		nbt.put("Level", levelTag);

		return nbt;
	}

	private boolean needsUpdate(int chunkX, int chunkZ, String id) {
		return needsUpdate
			&& featureIdToChunkNbt.get(id) != null
			&& updateStates.get(OLD_TO_NEW.get(id)).contains(ChunkPos.toLong(chunkX, chunkZ));
	}

	private boolean needsUpdate(int chunkX, int chunkZ) {
		if (!needsUpdate) {
			return false;
		}

		for (String name : newNames) {
			if (featureIdToChunkNbt.get(name) != null
				&& updateStates.get(OLD_TO_NEW.get(name)).isRemaining(ChunkPos.toLong(chunkX, chunkZ))
			) {
				return true;
			}
		}

		return false;
	}

	private NbtCompound getUpdatedStarts(NbtCompound nbt, ChunkPos pos) {
		NbtCompound levelTag = nbt.getCompoundOrEmpty("Level");
		NbtCompound structuresTag = levelTag.getCompoundOrEmpty("Structures");
		NbtCompound startsTag = structuresTag.getCompoundOrEmpty("Starts");

		for (String name : newNames) {
			Long2ObjectMap<NbtCompound> chunkNbtMap = featureIdToChunkNbt.get(name);

			if (chunkNbtMap == null) {
				continue;
			}

			long chunkKey = pos.toLong();

			if (!updateStates.get(OLD_TO_NEW.get(name)).isRemaining(chunkKey)) {
				continue;
			}

			NbtCompound startNbt = chunkNbtMap.get(chunkKey);

			if (startNbt != null) {
				startsTag.put(name, startNbt);
			}
		}

		structuresTag.put("Starts", startsTag);
		levelTag.put("Structures", structuresTag);
		nbt.put("Level", levelTag);

		return nbt;
	}

	/**
	 * Лениво инициализирует данные из файлов {@code .dat} при первом вызове {@link #applyFix}.
	 * <p>
	 * Синхронизирован, так как {@link #applyFix} может вызываться из нескольких потоков
	 * во время параллельного обновления чанков.
	 */
	private synchronized void init(PersistentStateManager stateManager) {
		if (initialized) {
			return;
		}

		for (String name : oldNames) {
			NbtCompound featuresNbt = new NbtCompound();

			try {
				featuresNbt = stateManager
					.readNbt(name, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES, TARGET_DATA_VERSION)
					.getCompoundOrEmpty("data")
					.getCompoundOrEmpty("Features");

				if (featuresNbt.isEmpty()) {
					continue;
				}
			} catch (IOException ignored) {
			}

			featuresNbt.forEach((key, nbt) -> {
				if (!(nbt instanceof NbtCompound featureNbt)) {
					return;
				}

				long chunkKey = ChunkPos.toLong(
					featureNbt.getInt("ChunkX", 0),
					featureNbt.getInt("ChunkZ", 0)
				);

				NbtList children = featureNbt.getListOrEmpty("Children");

				if (!children.isEmpty()) {
					Optional<String> childId = children.getCompound(0).flatMap(child -> child.getString("id"));
					childId.map(ANCIENT_TO_OLD::get).ifPresent(id -> featureNbt.putString("id", id));
				}

				featureNbt.getString("id").ifPresent(id ->
					featureIdToChunkNbt
						.computeIfAbsent(id, featureId -> new Long2ObjectOpenHashMap<>())
						.put(chunkKey, featureNbt)
				);
			});

			String indexName = name + "_index";
			ChunkUpdateState existingState = stateManager.getOrCreate(ChunkUpdateState.createStateType(indexName));

			if (existingState.getAll().isEmpty()) {
				ChunkUpdateState freshState = new ChunkUpdateState();
				updateStates.put(name, freshState);

				featuresNbt.forEach((key, nbt) -> {
					if (nbt instanceof NbtCompound featureNbt) {
						freshState.add(ChunkPos.toLong(
							featureNbt.getInt("ChunkX", 0),
							featureNbt.getInt("ChunkZ", 0)
						));
					}
				});
			} else {
				updateStates.put(name, existingState);
			}
		}

		initialized = true;
	}

	/**
	 * Создаёт фабрику {@link FeatureUpdater} для указанного измерения.
	 * <p>
	 * Каждое измерение имеет свой набор старых и новых имён структур.
	 * Для измерений, не являющихся Overworld/Nether/End, возвращает
	 * {@link ChunkUpdater#PASSTHROUGH_FACTORY} — обновление не требуется.
	 */
	public static Supplier<ChunkUpdater> create(
		RegistryKey<World> world,
		Supplier<@Nullable PersistentStateManager> persistentStateManagerSupplier,
		DataFixer dataFixer
	) {
		if (world == World.OVERWORLD) {
			return () -> new FeatureUpdater(
				persistentStateManagerSupplier.get(),
				ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
				ImmutableList.of(
					"Village",
					"Mineshaft",
					"Mansion",
					"Igloo",
					"Desert_Pyramid",
					"Jungle_Pyramid",
					"Swamp_Hut",
					"Stronghold",
					"Monument"
				),
				dataFixer
			);
		}

		if (world == World.NETHER) {
			List<String> fortressList = ImmutableList.of("Fortress");
			return () -> new FeatureUpdater(persistentStateManagerSupplier.get(), fortressList, fortressList, dataFixer);
		}

		if (world == World.END) {
			List<String> endCityList = ImmutableList.of("EndCity");
			return () -> new FeatureUpdater(persistentStateManagerSupplier.get(), endCityList, endCityList, dataFixer);
		}

		return ChunkUpdater.PASSTHROUGH_FACTORY;
	}
}
