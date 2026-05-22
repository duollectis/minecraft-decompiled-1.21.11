package net.minecraft.world.updater;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.storage.RecreationStorage;
import net.minecraft.world.storage.RegionFile;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Оркестрирует полное обновление мирового хранилища до текущей версии игры.
 * <p>
 * Запускает три последовательных прохода в фоновом потоке:
 * <ol>
 *   <li>{@link EntitiesUpdate} — обновляет файлы сущностей ({@code entities/});</li>
 *   <li>{@link PoiUpdate} — обновляет файлы точек интереса ({@code poi/});</li>
 *   <li>{@link RegionUpdate} — обновляет файлы чанков ({@code region/}), включая
 *       миграцию устаревших данных структур через {@link FeatureUpdater}.</li>
 * </ol>
 * Прогресс каждого прохода доступен через {@link #getProgress(RegistryKey)} и {@link #getProgress()}.
 */
public class WorldUpdater implements AutoCloseable {

	static final Logger LOGGER = LogUtils.getLogger();
	static final Pattern REGION_FILE_PATTERN = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");

	private static final ThreadFactory UPDATE_THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
	private static final String NEW_DIRECTORY_PREFIX = "new_";

	/** Версия данных POI-чанков, начиная с которой формат стал стабильным. */
	private static final int POI_STABLE_DATA_VERSION = 1945;

	static final Text UPGRADING_POI_TEXT = Text.translatable("optimizeWorld.stage.upgrading.poi");
	static final Text FINISHED_POI_TEXT = Text.translatable("optimizeWorld.stage.finished.poi");
	static final Text UPGRADING_ENTITIES_TEXT = Text.translatable("optimizeWorld.stage.upgrading.entities");
	static final Text FINISHED_ENTITIES_TEXT = Text.translatable("optimizeWorld.stage.finished.entities");
	static final Text UPGRADING_CHUNKS_TEXT = Text.translatable("optimizeWorld.stage.upgrading.chunks");
	static final Text FINISHED_CHUNKS_TEXT = Text.translatable("optimizeWorld.stage.finished.chunks");

	final Registry<DimensionOptions> dimensionOptionsRegistry;
	final Set<RegistryKey<World>> worldKeys;
	final boolean eraseCache;
	final boolean recreateRegionFiles;
	final LevelStorage.Session session;
	final DataFixer dataFixer;
	final PersistentStateManager persistentStateManager;

	volatile boolean keepUpgradingChunks = true;
	volatile float progress;
	volatile int totalChunkCount;
	volatile int totalRegionCount;
	volatile int upgradedChunkCount;
	volatile int skippedChunkCount;
	volatile Text status = Text.translatable("optimizeWorld.stage.counting");

	final Reference2FloatMap<RegistryKey<World>> dimensionProgress =
		Reference2FloatMaps.synchronize(new Reference2FloatOpenHashMap<>());

	private final Thread updateThread;
	private volatile boolean done;

	public WorldUpdater(
		LevelStorage.Session session,
		DataFixer dataFixer,
		SaveProperties saveProperties,
		DynamicRegistryManager registries,
		boolean eraseCache,
		boolean recreateRegionFiles
	) {
		dimensionOptionsRegistry = registries.getOrThrow(RegistryKeys.DIMENSION);
		worldKeys = dimensionOptionsRegistry
			.getKeys()
			.stream()
			.map(RegistryKeys::toWorldKey)
			.collect(Collectors.toUnmodifiableSet());
		this.eraseCache = eraseCache;
		this.dataFixer = dataFixer;
		this.session = session;
		persistentStateManager = new PersistentStateManager(
			session.getWorldDirectory(World.OVERWORLD).resolve("data"),
			dataFixer,
			registries
		);
		this.recreateRegionFiles = recreateRegionFiles;
		updateThread = UPDATE_THREAD_FACTORY.newThread(this::updateWorld);
		updateThread.setUncaughtExceptionHandler((thread, throwable) -> {
			LOGGER.error("Error upgrading world", throwable);
			status = Text.translatable("optimizeWorld.stage.failed");
			done = true;
		});
		updateThread.start();
	}

	/**
	 * Прерывает обновление и ожидает завершения фонового потока.
	 */
	public void cancel() {
		keepUpgradingChunks = false;

		try {
			updateThread.join();
		} catch (InterruptedException ignored) {
		}
	}

	private void updateWorld() {
		long startTime = Util.getMeasuringTimeMs();

		LOGGER.info("Upgrading entities");
		new EntitiesUpdate().update();

		LOGGER.info("Upgrading POIs");
		new PoiUpdate().update();

		LOGGER.info("Upgrading blocks");
		new RegionUpdate().update();

		persistentStateManager.save();

		long elapsedSeconds = (Util.getMeasuringTimeMs() - startTime) / 1000L;
		LOGGER.info("World optimizaton finished after {} seconds", elapsedSeconds);
		done = true;
	}

	public boolean isDone() {
		return done;
	}

	public Set<RegistryKey<World>> getWorlds() {
		return worldKeys;
	}

	public float getProgress(RegistryKey<World> world) {
		return dimensionProgress.getFloat(world);
	}

	public float getProgress() {
		return progress;
	}

	public int getTotalChunkCount() {
		return totalChunkCount;
	}

	public int getUpgradedChunkCount() {
		return upgradedChunkCount;
	}

	public int getSkippedChunkCount() {
		return skippedChunkCount;
	}

	public Text getStatus() {
		return status;
	}

	@Override
	public void close() {
		persistentStateManager.close();
	}

	static Path getNewDirectoryPath(Path current) {
		return current.resolveSibling(NEW_DIRECTORY_PREFIX + current.getFileName().toString());
	}

	// -------------------------------------------------------------------------
	// Внутренние классы
	// -------------------------------------------------------------------------

	/**
	 * Базовый класс для обновлений, работающих с {@link VersionedChunkStorage}
	 * и адресующих чанки по {@link ChunkPos}.
	 */
	abstract class ChunkPosKeyedStorageUpdate extends Update {

		ChunkPosKeyedStorageUpdate(
			DataFixTypes dataFixTypes,
			String targetName,
			Text upgradingText,
			Text finishedText
		) {
			super(dataFixTypes, targetName, targetName, upgradingText, finishedText);
		}

		@Override
		protected VersionedChunkStorage openStorage(StorageKey storageKey, Path directory) {
			return recreateRegionFiles
				? new RecreationStorage(
					storageKey.withSuffix("source"),
					directory,
					storageKey.withSuffix("target"),
					WorldUpdater.getNewDirectoryPath(directory),
					dataFixer,
					true,
					dataFixTypes,
					ChunkUpdater.PASSTHROUGH_FACTORY
				)
				: new VersionedChunkStorage(
					storageKey,
					directory,
					dataFixer,
					true,
					dataFixTypes
				);
		}

		@Override
		protected boolean update(VersionedChunkStorage storage, ChunkPos chunkPos, RegistryKey<World> worldKey) {
			NbtCompound nbt = storage.getNbt(chunkPos).join().orElse(null);

			if (nbt == null) {
				return false;
			}

			int dataVersion = NbtHelper.getDataVersion(nbt);
			NbtCompound updatedNbt = updateNbt(storage, nbt);
			boolean isOutdated = dataVersion < SharedConstants.getGameVersion().dataVersion().id();

			if (isOutdated || recreateRegionFiles) {
				if (pendingUpdateFuture != null) {
					pendingUpdateFuture.join();
				}

				pendingUpdateFuture = storage.setNbt(chunkPos, updatedNbt);
				return true;
			}

			return false;
		}

		protected abstract NbtCompound updateNbt(VersionedChunkStorage storage, NbtCompound nbt);
	}

	class EntitiesUpdate extends ChunkPosKeyedStorageUpdate {

		EntitiesUpdate() {
			super(
				DataFixTypes.ENTITY_CHUNK,
				"entities",
				UPGRADING_ENTITIES_TEXT,
				FINISHED_ENTITIES_TEXT
			);
		}

		@Override
		protected NbtCompound updateNbt(VersionedChunkStorage storage, NbtCompound nbt) {
			return storage.updateChunkNbt(nbt, -1);
		}
	}

	class PoiUpdate extends ChunkPosKeyedStorageUpdate {

		PoiUpdate() {
			super(DataFixTypes.POI_CHUNK, "poi", UPGRADING_POI_TEXT, FINISHED_POI_TEXT);
		}

		/**
		 * POI-чанки обновляются относительно версии {@code POI_STABLE_DATA_VERSION},
		 * а не текущей версии игры, так как формат POI стабилизировался раньше.
		 */
		@Override
		protected NbtCompound updateNbt(VersionedChunkStorage storage, NbtCompound nbt) {
			return storage.updateChunkNbt(nbt, POI_STABLE_DATA_VERSION);
		}
	}

	record Region(RegionFile file, List<ChunkPos> chunksToUpgrade) {
	}

	class RegionUpdate extends Update {

		RegionUpdate() {
			super(
				DataFixTypes.CHUNK,
				"chunk",
				"region",
				UPGRADING_CHUNKS_TEXT,
				FINISHED_CHUNKS_TEXT
			);
		}

		@Override
		protected boolean update(VersionedChunkStorage storage, ChunkPos chunkPos, RegistryKey<World> worldKey) {
			NbtCompound nbt = storage.getNbt(chunkPos).join().orElse(null);

			if (nbt == null) {
				return false;
			}

			int dataVersion = NbtHelper.getDataVersion(nbt);
			ChunkGenerator chunkGenerator = dimensionOptionsRegistry
				.getValueOrThrow(RegistryKeys.toDimensionKey(worldKey))
				.chunkGenerator();

			NbtCompound updatedNbt = storage.updateChunkNbt(
				nbt,
				-1,
				ServerChunkLoadingManager.getContextNbt(worldKey, chunkGenerator.getCodecKey())
			);

			ChunkPos actualPos = new ChunkPos(updatedNbt.getInt("xPos", 0), updatedNbt.getInt("zPos", 0));

			if (!actualPos.equals(chunkPos)) {
				LOGGER.warn("Chunk {} has invalid position {}", chunkPos, actualPos);
			}

			boolean needsWrite = dataVersion < SharedConstants.getGameVersion().dataVersion().id();

			if (eraseCache) {
				needsWrite = needsWrite || updatedNbt.contains("Heightmaps");
				updatedNbt.remove("Heightmaps");
				needsWrite = needsWrite || updatedNbt.contains("isLightOn");
				updatedNbt.remove("isLightOn");

				NbtList sections = updatedNbt.getListOrEmpty("sections");

				for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
					Optional<NbtCompound> sectionOpt = sections.getCompound(sectionIndex);

					if (sectionOpt.isEmpty()) {
						continue;
					}

					NbtCompound section = sectionOpt.get();
					needsWrite = needsWrite || section.contains("BlockLight");
					section.remove("BlockLight");
					needsWrite = needsWrite || section.contains("SkyLight");
					section.remove("SkyLight");
				}
			}

			if (needsWrite || recreateRegionFiles) {
				if (pendingUpdateFuture != null) {
					pendingUpdateFuture.join();
				}

				pendingUpdateFuture = storage.setNbt(chunkPos, updatedNbt);
				return true;
			}

			return false;
		}

		@Override
		protected VersionedChunkStorage openStorage(StorageKey storageKey, Path directory) {
			Supplier<ChunkUpdater> featureUpdaterFactory = FeatureUpdater.create(
				storageKey.dimension(),
				() -> persistentStateManager,
				dataFixer
			);

			return recreateRegionFiles
				? new RecreationStorage(
					storageKey.withSuffix("source"),
					directory,
					storageKey.withSuffix("target"),
					WorldUpdater.getNewDirectoryPath(directory),
					dataFixer,
					true,
					DataFixTypes.CHUNK,
					featureUpdaterFactory
				)
				: new VersionedChunkStorage(
					storageKey,
					directory,
					dataFixer,
					true,
					DataFixTypes.CHUNK,
					featureUpdaterFactory
				);
		}
	}

	/**
	 * Базовый класс одного прохода обновления (entities / poi / region).
	 * <p>
	 * Итерирует по всем измерениям и регион-файлам, вызывая {@link #update(VersionedChunkStorage, ChunkPos, RegistryKey)}
	 * для каждого чанка. Ведёт счётчики прогресса в родительском {@link WorldUpdater}.
	 */
	abstract class Update {

		private final Text upgradingText;
		private final Text finishedText;
		private final String name;
		private final String targetName;
		protected @Nullable CompletableFuture<Void> pendingUpdateFuture;
		protected final DataFixTypes dataFixTypes;

		Update(
			DataFixTypes dataFixTypes,
			String name,
			String targetName,
			Text upgradingText,
			Text finishedText
		) {
			this.dataFixTypes = dataFixTypes;
			this.name = name;
			this.targetName = targetName;
			this.upgradingText = upgradingText;
			this.finishedText = finishedText;
		}

		public void update() {
			totalRegionCount = 0;
			totalChunkCount = 0;
			upgradedChunkCount = 0;
			skippedChunkCount = 0;

			List<WorldData> worldDataList = listWorldData();

			if (totalChunkCount == 0) {
				return;
			}

			float totalRegions = totalRegionCount;
			status = upgradingText;

			while (keepUpgradingChunks) {
				boolean hadWork = false;
				float totalProgress = 0.0F;

				for (WorldData worldData : worldDataList) {
					RegistryKey<World> worldKey = worldData.dimensionKey;
					ListIterator<Region> regionIterator = worldData.files;
					VersionedChunkStorage storage = worldData.storage;

					if (regionIterator.hasNext()) {
						Region region = regionIterator.next();
						boolean allSucceeded = true;

						for (ChunkPos chunkPos : region.chunksToUpgrade) {
							allSucceeded = allSucceeded && updateChunk(worldKey, storage, chunkPos);
							hadWork = true;
						}

						if (recreateRegionFiles) {
							if (allSucceeded) {
								recreate(region.file);
							} else {
								LOGGER.error("Failed to convert region file {}", region.file.getPath());
							}
						}
					}

					float dimensionFraction = regionIterator.nextIndex() / totalRegions;
					dimensionProgress.put(worldKey, dimensionFraction);
					totalProgress += dimensionFraction;
				}

				progress = totalProgress;

				if (!hadWork) {
					break;
				}
			}

			status = finishedText;

			for (WorldData worldData : worldDataList) {
				try {
					worldData.storage.close();
				} catch (Exception e) {
					LOGGER.error("Error upgrading chunk", e);
				}
			}
		}

		private List<WorldData> listWorldData() {
			List<WorldData> result = Lists.newArrayList();

			for (RegistryKey<World> worldKey : worldKeys) {
				StorageKey storageKey = new StorageKey(session.getDirectoryName(), worldKey, name);
				Path directory = session.getWorldDirectory(worldKey).resolve(targetName);
				VersionedChunkStorage storage = openStorage(storageKey, directory);
				ListIterator<Region> regions = enumerateRegions(storageKey, directory);
				result.add(new WorldData(worldKey, storage, regions));
			}

			return result;
		}

		protected abstract VersionedChunkStorage openStorage(StorageKey storageKey, Path directory);

		private ListIterator<Region> enumerateRegions(StorageKey key, Path regionDirectory) {
			List<Region> regions = listRegions(key, regionDirectory);
			totalRegionCount += regions.size();
			totalChunkCount += regions.stream().mapToInt(region -> region.chunksToUpgrade().size()).sum();
			return regions.listIterator();
		}

		private static List<Region> listRegions(StorageKey key, Path regionDirectory) {
			File[] files = regionDirectory.toFile().listFiles((dir, fileName) -> fileName.endsWith(".mca"));

			if (files == null) {
				return List.of();
			}

			List<Region> result = Lists.newArrayList();

			for (File file : files) {
				Matcher matcher = REGION_FILE_PATTERN.matcher(file.getName());

				if (!matcher.matches()) {
					continue;
				}

				// Координаты первого чанка в регион-файле (сдвиг на 5 бит = умножение на 32)
				int regionOriginX = Integer.parseInt(matcher.group(1)) << 5;
				int regionOriginZ = Integer.parseInt(matcher.group(2)) << 5;
				List<ChunkPos> validChunks = Lists.newArrayList();

				try (RegionFile regionFile = new RegionFile(key, file.toPath(), regionDirectory, true)) {
					for (int localX = 0; localX < 32; localX++) {
						for (int localZ = 0; localZ < 32; localZ++) {
							ChunkPos chunkPos = new ChunkPos(localX + regionOriginX, localZ + regionOriginZ);

							if (regionFile.isChunkValid(chunkPos)) {
								validChunks.add(chunkPos);
							}
						}
					}

					if (!validChunks.isEmpty()) {
						result.add(new Region(regionFile, validChunks));
					}
				} catch (Throwable e) {
					LOGGER.error("Failed to read chunks from region file {}", file.toPath(), e);
				}
			}

			return result;
		}

		private boolean updateChunk(RegistryKey<World> worldKey, VersionedChunkStorage storage, ChunkPos chunkPos) {
			boolean upgraded = false;

			try {
				upgraded = update(storage, chunkPos, worldKey);
			} catch (CompletionException | CrashException e) {
				Throwable cause = e.getCause();

				if (!(cause instanceof IOException)) {
					throw e;
				}

				LOGGER.error("Error upgrading chunk {}", chunkPos, cause);
			}

			if (upgraded) {
				upgradedChunkCount++;
			} else {
				skippedChunkCount++;
			}

			return upgraded;
		}

		protected abstract boolean update(
			VersionedChunkStorage storage,
			ChunkPos chunkPos,
			RegistryKey<World> worldKey
		);

		private void recreate(RegionFile regionFile) {
			if (!recreateRegionFiles) {
				return;
			}

			if (pendingUpdateFuture != null) {
				pendingUpdateFuture.join();
			}

			Path originalPath = regionFile.getPath();
			Path parentDir = originalPath.getParent();
			Path newPath = WorldUpdater.getNewDirectoryPath(parentDir).resolve(originalPath.getFileName().toString());

			try {
				if (newPath.toFile().exists()) {
					Files.delete(originalPath);
					Files.move(newPath, originalPath);
				} else {
					LOGGER.error("Failed to replace an old region file. New file {} does not exist.", newPath);
				}
			} catch (IOException e) {
				LOGGER.error("Failed to replace an old region file", e);
			}
		}
	}

	record WorldData(
		RegistryKey<World> dimensionKey,
		VersionedChunkStorage storage,
		ListIterator<Region> files
	) {
	}
}
