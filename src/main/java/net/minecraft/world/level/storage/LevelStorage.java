package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.*;
import net.minecraft.nbt.scanner.ExclusiveNbtCollector;
import net.minecraft.nbt.scanner.NbtScanQuery;
import net.minecraft.registry.*;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.SaveLoading;
import net.minecraft.text.Text;
import net.minecraft.util.DateTimeFormatters;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashMemoryReserve;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.path.*;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.WorldGenSettings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Управляет директорией сохранений: перечисление миров, создание/удаление сессий,
 * резервное копирование и чтение/запись level.dat.
 *
 * <p>Точка входа — {@link #create(Path)} для стандартного использования или
 * конструктор для кастомных путей. Каждый открытый мир представлен объектом
 * {@link Session}, который удерживает {@link SessionLock} на директорию.
 */
public class LevelStorage {

	static final Logger LOGGER = LogUtils.getLogger();

	public static final String DATA_KEY = "Data";
	public static final String ALLOWED_SYMLINKS_FILE_NAME = "allowed_symlinks.txt";

	/** Формат Anvil (текущий). */
	private static final int FORMAT_ANVIL = 19133;
	/** Устаревший формат McRegion. */
	private static final int FORMAT_MC_REGION = 19132;
	/** Минимальный рекомендуемый свободный объём диска в байтах (64 МБ). */
	private static final long RECOMMENDED_USABLE_SPACE_BYTES = 67108864L;
	/** Максимальное количество попыток удаления директории мира. */
	private static final int MAX_DELETE_ATTEMPTS = 5;
	/** Задержка между попытками удаления в миллисекундах. */
	private static final long DELETE_RETRY_DELAY_MS = 500L;

	private static final PathMatcher DEFAULT_ALLOWED_SYMLINK_MATCHER = path -> false;

	private final Path savesDirectory;
	private final Path backupsDirectory;
	final DataFixer dataFixer;
	private final SymlinkFinder symlinkFinder;

	public LevelStorage(
		Path savesDirectory,
		Path backupsDirectory,
		SymlinkFinder symlinkFinder,
		DataFixer dataFixer
	) {
		this.dataFixer = dataFixer;

		try {
			PathUtil.createDirectories(savesDirectory);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}

		this.savesDirectory = savesDirectory;
		this.backupsDirectory = backupsDirectory;
		this.symlinkFinder = symlinkFinder;
	}

	/**
	 * Создаёт {@link SymlinkFinder} на основе файла {@code allowed_symlinks.txt}.
	 * Если файл отсутствует — все симлинки запрещены. При ошибке парсинга — тоже.
	 *
	 * @param allowedSymlinksFile путь к файлу разрешённых симлинков
	 * @return настроенный {@code SymlinkFinder}
	 */
	public static SymlinkFinder createSymlinkFinder(Path allowedSymlinksFile) {
		if (Files.exists(allowedSymlinksFile)) {
			try (BufferedReader reader = Files.newBufferedReader(allowedSymlinksFile)) {
				return new SymlinkFinder(AllowedSymlinkPathMatcher.fromReader(reader));
			} catch (Exception exception) {
				LOGGER.error("Failed to parse {}, disallowing all symbolic links", ALLOWED_SYMLINKS_FILE_NAME, exception);
			}
		}

		return new SymlinkFinder(DEFAULT_ALLOWED_SYMLINK_MATCHER);
	}

	/**
	 * Создаёт {@code LevelStorage} для стандартной директории сохранений.
	 * Резервные копии помещаются в {@code ../backups} относительно {@code path}.
	 *
	 * @param path директория сохранений
	 * @return настроенный экземпляр {@code LevelStorage}
	 */
	public static LevelStorage create(Path path) {
		SymlinkFinder symlinkFinder = createSymlinkFinder(path.resolve(ALLOWED_SYMLINKS_FILE_NAME));
		return new LevelStorage(path, path.resolve("../backups"), symlinkFinder, Schemas.getFixer());
	}

	/**
	 * Разбирает конфигурацию датапаков из NBT-совместимого {@link Dynamic}.
	 * При ошибке парсинга возвращает безопасный режим ({@link DataConfiguration#SAFE_MODE}).
	 *
	 * @param dynamic динамическое представление данных уровня
	 * @return конфигурация датапаков
	 */
	public static DataConfiguration parseDataPackSettings(Dynamic<?> dynamic) {
		return DataConfiguration.CODEC
			.parse(dynamic)
			.resultOrPartial(LOGGER::error)
			.orElse(DataConfiguration.SAFE_MODE);
	}

	public static SaveLoading.DataPacks parseDataPacks(
		Dynamic<?> dynamic,
		ResourcePackManager dataPackManager,
		boolean safeMode
	) {
		return new SaveLoading.DataPacks(dataPackManager, parseDataPackSettings(dynamic), safeMode, false);
	}

	/**
	 * Полностью разбирает свойства сохранения из NBT-данных уровня.
	 * Применяет DataFixer, разрешает конфигурацию измерений и вычисляет жизненный цикл.
	 *
	 * @param dynamic            обновлённые данные уровня
	 * @param dataConfiguration  конфигурация датапаков
	 * @param dimensionsRegistry реестр конфигураций измерений
	 * @param registries         обёртка динамических реестров
	 * @return разобранные свойства сохранения с конфигурацией измерений
	 */
	public static ParsedSaveProperties parseSaveProperties(
		Dynamic<?> dynamic,
		DataConfiguration dataConfiguration,
		Registry<DimensionOptions> dimensionsRegistry,
		RegistryWrapper.WrapperLookup registries
	) {
		Dynamic<?> registryDynamic = RegistryOps.withRegistry(dynamic, registries);
		Dynamic<?> worldGenDynamic = registryDynamic.get("WorldGenSettings").orElseEmptyMap();
		WorldGenSettings worldGenSettings = WorldGenSettings.CODEC.parse(worldGenDynamic).getOrThrow();
		LevelInfo levelInfo = LevelInfo.fromDynamic(registryDynamic, dataConfiguration);
		DimensionOptionsRegistryHolder.DimensionsConfig dimensionsConfig =
			worldGenSettings.dimensionOptionsRegistryHolder().toConfig(dimensionsRegistry);
		Lifecycle lifecycle = dimensionsConfig.getLifecycle().add(registries.getLifecycle());
		LevelProperties levelProperties = LevelProperties.readProperties(
			registryDynamic,
			levelInfo,
			dimensionsConfig.specialWorldProperty(),
			worldGenSettings.generatorOptions(),
			lifecycle
		);

		return new ParsedSaveProperties(levelProperties, dimensionsConfig);
	}

	public String getFormatName() {
		return "Anvil";
	}

	/**
	 * Возвращает список всех директорий миров в директории сохранений.
	 * Включает только директории с файлом {@code level.dat} или {@code level.dat_old}.
	 *
	 * @return список сохранений
	 * @throws LevelStorageException если директория сохранений недоступна
	 */
	public LevelList getLevelList() throws LevelStorageException {
		if (!Files.isDirectory(savesDirectory)) {
			throw new LevelStorageException(Text.translatable("selectWorld.load_folder_access"));
		}

		try (Stream<Path> stream = Files.list(savesDirectory)) {
			List<LevelSave> saves = stream
				.filter(Files::isDirectory)
				.map(LevelSave::new)
				.filter(save ->
					Files.isRegularFile(save.getLevelDatPath())
						|| Files.isRegularFile(save.getLevelDatOldPath())
				)
				.toList();

			return new LevelList(saves);
		} catch (IOException exception) {
			throw new LevelStorageException(Text.translatable("selectWorld.load_folder_access"));
		}
	}

	/**
	 * Асинхронно загружает краткие сводки для всех миров из списка.
	 * Миры с ошибками чтения возвращают {@code null} и фильтруются из результата.
	 * При нехватке памяти генерирует {@link CrashException} с подробным отчётом.
	 *
	 * @param levels список директорий миров
	 * @return будущий список сводок, отсортированных по времени последней игры
	 */
	public CompletableFuture<List<LevelSummary>> loadSummaries(LevelList levels) {
		List<CompletableFuture<LevelSummary>> futures = new ArrayList<>(levels.levels.size());

		for (LevelSave levelSave : levels.levels) {
			futures.add(CompletableFuture.supplyAsync(
				() -> {
					boolean isLocked;
					try {
						isLocked = SessionLock.isLocked(levelSave.path());
					} catch (Exception exception) {
						LOGGER.warn("Failed to read {} lock", levelSave.path(), exception);
						return null;
					}

					try {
						return readSummary(levelSave, isLocked);
					} catch (OutOfMemoryError oom) {
						CrashMemoryReserve.releaseMemory();
						String message = "Ran out of memory trying to read summary of world folder \""
							+ levelSave.getRootPath() + "\"";
						LOGGER.error(LogUtils.FATAL_MARKER, message);
						OutOfMemoryError wrappedOom = new OutOfMemoryError("Ran out of memory reading level data");
						wrappedOom.initCause(oom);
						CrashReport crashReport = CrashReport.create(wrappedOom, message);
						CrashReportSection section = crashReport.addElement("World details");
						section.add("Folder Name", levelSave.getRootPath());

						try {
							long datSize = Files.size(levelSave.getLevelDatPath());
							section.add("level.dat size", datSize);
						} catch (IOException sizeException) {
							section.add("level.dat size", (Throwable) sizeException);
						}

						throw new CrashException(crashReport);
					}
				},
				Util.getMainWorkerExecutor().named("loadLevelSummaries")
			));
		}

		return Util.combineCancellable(futures)
			.thenApply(summaries -> summaries.stream().filter(Objects::nonNull).sorted().toList());
	}

	static NbtCompound readLevelProperties(Path path) throws IOException {
		return NbtIo.readCompressed(path, NbtSizeTracker.forLevel());
	}

	/**
	 * Читает и обновляет данные level.dat через DataFixer.
	 * Применяет фиксеры для типов {@code LEVEL}, {@code PLAYER} и {@code WORLD_GEN_SETTINGS}.
	 *
	 * @param path      путь к файлу level.dat
	 * @param dataFixer фиксер данных
	 * @return обновлённое динамическое представление данных уровня
	 */
	static Dynamic<?> readLevelProperties(Path path, DataFixer dataFixer) throws IOException {
		NbtCompound root = readLevelProperties(path);
		NbtCompound data = root.getCompoundOrEmpty("Data");
		int dataVersion = NbtHelper.getDataVersion(data);
		Dynamic<?> dynamic = DataFixTypes.LEVEL.update(dataFixer, new Dynamic<>(NbtOps.INSTANCE, data), dataVersion);
		dynamic = dynamic.update("Player", player -> DataFixTypes.PLAYER.update(dataFixer, player, dataVersion));

		return dynamic.update(
			"WorldGenSettings",
			worldGen -> DataFixTypes.WORLD_GEN_SETTINGS.update(dataFixer, worldGen, dataVersion)
		);
	}

	private LevelSummary readSummary(LevelSave save, boolean locked) {
		Path levelDatPath = save.getLevelDatPath();

		if (Files.exists(levelDatPath)) {
			try {
				if (Files.isSymbolicLink(levelDatPath)) {
					List<SymlinkEntry> symlinkEntries = symlinkFinder.validate(levelDatPath);
					if (!symlinkEntries.isEmpty()) {
						LOGGER.warn("{}", SymlinkValidationException.getMessage(levelDatPath, symlinkEntries));
						return new LevelSummary.SymlinkLevelSummary(save.getRootPath(), save.getIconPath());
					}
				}

				if (loadCompactLevelData(levelDatPath) instanceof NbtCompound nbtCompound) {
					NbtCompound data = nbtCompound.getCompoundOrEmpty("Data");
					int dataVersion = NbtHelper.getDataVersion(data);
					Dynamic<?> dynamic = DataFixTypes.LEVEL_SUMMARY.update(
						dataFixer,
						new Dynamic<>(NbtOps.INSTANCE, data),
						dataVersion
					);

					return parseSummary(dynamic, save, locked);
				}

				LOGGER.warn("Invalid root tag in {}", levelDatPath);
			} catch (Exception exception) {
				LOGGER.error("Exception reading {}", levelDatPath, exception);
			}
		}

		return new LevelSummary.RecoveryWarning(save.getRootPath(), save.getIconPath(), getLastModifiedTime(save));
	}

	private static long getLastModifiedTime(LevelSave save) {
		Instant instant = getLastModifiedTime(save.getLevelDatPath());
		if (instant == null) {
			instant = getLastModifiedTime(save.getLevelDatOldPath());
		}

		return instant == null ? -1L : instant.toEpochMilli();
	}

	static @Nullable Instant getLastModifiedTime(Path path) {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException exception) {
			return null;
		}
	}

	/**
	 * Разбирает краткую сводку мира из NBT-данных.
	 * Проверяет совместимость формата уровня: допустимы только {@link #FORMAT_MC_REGION}
	 * и {@link #FORMAT_ANVIL}.
	 *
	 * @param dynamic динамическое представление данных уровня
	 * @param save    директория мира
	 * @param locked  заблокирован ли мир другим процессом
	 * @return краткая сводка мира
	 * @throws InvalidNbtException если формат уровня неизвестен
	 */
	LevelSummary parseSummary(Dynamic<?> dynamic, LevelSave save, boolean locked) {
		SaveVersionInfo saveVersionInfo = SaveVersionInfo.fromDynamic(dynamic);
		int levelFormatVersion = saveVersionInfo.getLevelFormatVersion();

		if (levelFormatVersion != FORMAT_MC_REGION && levelFormatVersion != FORMAT_ANVIL) {
			throw new InvalidNbtException("Unknown data version: " + Integer.toHexString(levelFormatVersion));
		}

		boolean requiresConversion = levelFormatVersion != FORMAT_ANVIL;
		DataConfiguration dataConfiguration = parseDataPackSettings(dynamic);
		LevelInfo levelInfo = LevelInfo.fromDynamic(dynamic, dataConfiguration);
		FeatureSet featureSet = parseEnabledFeatures(dynamic);
		boolean isExperimental = FeatureFlags.isNotVanilla(featureSet);

		return new LevelSummary(levelInfo, saveVersionInfo, save.getRootPath(), requiresConversion, locked, isExperimental, save.getIconPath());
	}

	private static FeatureSet parseEnabledFeatures(Dynamic<?> levelData) {
		Set<Identifier> featureIds = levelData.get("enabled_features")
			.asStream()
			.flatMap(flag -> flag.asString().result().map(Identifier::tryParse).stream())
			.collect(Collectors.toSet());

		return FeatureFlags.FEATURE_MANAGER.featureSetOf(featureIds, id -> {});
	}

	private static @Nullable NbtElement loadCompactLevelData(Path path) throws IOException {
		ExclusiveNbtCollector collector = new ExclusiveNbtCollector(
			new NbtScanQuery("Data", NbtCompound.TYPE, "Player"),
			new NbtScanQuery("Data", NbtCompound.TYPE, "WorldGenSettings")
		);
		NbtIo.scanCompressed(path, collector, NbtSizeTracker.forLevel());

		return collector.getRoot();
	}

	public boolean isLevelNameValid(String name) {
		try {
			Path path = resolve(name);
			Files.createDirectory(path);
			Files.deleteIfExists(path);
			return true;
		} catch (IOException exception) {
			return false;
		}
	}

	public boolean levelExists(String name) {
		try {
			return Files.isDirectory(resolve(name));
		} catch (InvalidPathException exception) {
			return false;
		}
	}

	public Path resolve(String name) {
		return savesDirectory.resolve(name);
	}

	public Path getSavesDirectory() {
		return savesDirectory;
	}

	public Path getBackupsDirectory() {
		return backupsDirectory;
	}

	/**
	 * Создаёт сессию для работы с миром, предварительно проверяя симлинки.
	 *
	 * @param directoryName имя директории мира
	 * @return открытая сессия с захваченной блокировкой
	 * @throws SymlinkValidationException если директория содержит небезопасные симлинки
	 */
	public Session createSession(String directoryName) throws IOException, SymlinkValidationException {
		Path path = resolve(directoryName);
		List<SymlinkEntry> symlinkEntries = symlinkFinder.collect(path, true);

		if (!symlinkEntries.isEmpty()) {
			throw new SymlinkValidationException(path, symlinkEntries);
		}

		return new Session(directoryName, path);
	}

	public Session createSessionWithoutSymlinkCheck(String directoryName) throws IOException {
		Path path = resolve(directoryName);
		return new Session(directoryName, path);
	}

	public SymlinkFinder getSymlinkFinder() {
		return symlinkFinder;
	}

	/**
	 * Список директорий миров в директории сохранений.
	 */
	public record LevelList(List<LevelSave> levels) implements Iterable<LevelSave> {

		public boolean isEmpty() {
			return levels.isEmpty();
		}

		@Override
		public Iterator<LevelSave> iterator() {
			return levels.iterator();
		}
	}

	/**
	 * Представляет директорию одного сохранённого мира.
	 * Предоставляет удобные методы для получения путей к стандартным файлам.
	 */
	public record LevelSave(Path path) {

		public String getRootPath() {
			return path.getFileName().toString();
		}

		public Path getLevelDatPath() {
			return getPath(WorldSavePath.LEVEL_DAT);
		}

		public Path getLevelDatOldPath() {
			return getPath(WorldSavePath.LEVEL_DAT_OLD);
		}

		public Path getCorruptedLevelDatPath(ZonedDateTime dateTime) {
			return path.resolve(
				WorldSavePath.LEVEL_DAT.getRelativePath() + "_corrupted_" + dateTime.format(DateTimeFormatters.MINUTES)
			);
		}

		public Path getRawLevelDatPath(ZonedDateTime dateTime) {
			return path.resolve(
				WorldSavePath.LEVEL_DAT.getRelativePath() + "_raw_" + dateTime.format(DateTimeFormatters.MINUTES)
			);
		}

		public Path getIconPath() {
			return getPath(WorldSavePath.ICON_PNG);
		}

		public Path getSessionLockPath() {
			return getPath(WorldSavePath.SESSION_LOCK);
		}

		public Path getPath(WorldSavePath savePath) {
			return path.resolve(savePath.getRelativePath());
		}
	}

	/**
	 * Активная сессия работы с миром. Удерживает {@link SessionLock} на директорию,
	 * предоставляет методы чтения/записи level.dat, создания резервных копий и удаления.
	 *
	 * <p>Реализует {@link AutoCloseable}: закрытие освобождает блокировку файла.
	 */
	public class Session implements AutoCloseable {

		final SessionLock lock;
		final LevelSave directory;
		private final String directoryName;
		private final Map<WorldSavePath, Path> paths = Maps.newHashMap();

		Session(String directoryName, Path path) throws IOException {
			this.directoryName = directoryName;
			this.directory = new LevelSave(path);
			this.lock = SessionLock.create(path);
		}

		public long getUsableSpace() {
			try {
				return Files.getFileStore(directory.path()).getUsableSpace();
			} catch (Exception exception) {
				return Long.MAX_VALUE;
			}
		}

		/** Возвращает {@code true} если свободного места меньше рекомендуемого минимума (64 МБ). */
		public boolean shouldShowLowDiskSpaceWarning() {
			return getUsableSpace() < RECOMMENDED_USABLE_SPACE_BYTES;
		}

		public void tryClose() {
			try {
				close();
			} catch (IOException exception) {
				LevelStorage.LOGGER.warn("Failed to unlock access to level {}", getDirectoryName(), exception);
			}
		}

		public LevelStorage getLevelStorage() {
			return LevelStorage.this;
		}

		public LevelSave getDirectory() {
			return directory;
		}

		public String getDirectoryName() {
			return directoryName;
		}

		public Path getDirectory(WorldSavePath savePath) {
			return paths.computeIfAbsent(savePath, directory::getPath);
		}

		public Path getWorldDirectory(RegistryKey<World> key) {
			return DimensionType.getSaveDirectory(key, directory.path());
		}

		private void checkValid() {
			if (!lock.isValid()) {
				throw new IllegalStateException("Lock is no longer valid");
			}
		}

		public PlayerSaveHandler createSaveHandler() {
			checkValid();
			return new PlayerSaveHandler(this, LevelStorage.this.dataFixer);
		}

		public LevelSummary getLevelSummary(Dynamic<?> dynamic) {
			checkValid();
			return LevelStorage.this.parseSummary(dynamic, directory, false);
		}

		public Dynamic<?> readLevelProperties() throws IOException {
			return readLevelProperties(false);
		}

		public Dynamic<?> readOldLevelProperties() throws IOException {
			return readLevelProperties(true);
		}

		private Dynamic<?> readLevelProperties(boolean old) throws IOException {
			checkValid();
			Path datPath = old ? directory.getLevelDatOldPath() : directory.getLevelDatPath();
			return LevelStorage.readLevelProperties(datPath, LevelStorage.this.dataFixer);
		}

		public void backupLevelDataFile(DynamicRegistryManager registryManager, SaveProperties saveProperties) {
			backupLevelDataFile(registryManager, saveProperties, null);
		}

		/**
		 * Сериализует и сохраняет свойства мира в level.dat.
		 * Если передан {@code nbt} — используется как данные игрока вместо сохранённых.
		 *
		 * @param registryManager менеджер реестров для кодирования
		 * @param saveProperties  свойства мира для сохранения
		 * @param nbt             данные игрока или {@code null}
		 */
		public void backupLevelDataFile(
			DynamicRegistryManager registryManager,
			SaveProperties saveProperties,
			@Nullable NbtCompound nbt
		) {
			NbtCompound worldNbt = saveProperties.cloneWorldNbt(registryManager, nbt);
			NbtCompound root = new NbtCompound();
			root.put("Data", worldNbt);
			save(root);
		}

		private void save(NbtCompound nbt) {
			Path worldPath = directory.path();

			try {
				Path tempFile = Files.createTempFile(worldPath, "level", ".dat");
				NbtIo.writeCompressed(nbt, tempFile);
				Path oldDat = directory.getLevelDatOldPath();
				Path currentDat = directory.getLevelDatPath();
				Util.backupAndReplace(currentDat, tempFile, oldDat);
			} catch (Exception exception) {
				LevelStorage.LOGGER.error("Failed to save level {}", worldPath, exception);
			}
		}

		public Optional<Path> getIconFile() {
			return !lock.isValid() ? Optional.empty() : Optional.of(directory.getIconPath());
		}

		/**
		 * Удаляет всю директорию мира, пропуская файл блокировки сессии до последнего.
		 * Повторяет попытку до {@link LevelStorage#MAX_DELETE_ATTEMPTS} раз при ошибках.
		 *
		 * @throws IOException если удаление не удалось после всех попыток
		 */
		public void deleteSessionLock() throws IOException {
			checkValid();
			final Path sessionLockPath = directory.getSessionLockPath();
			LOGGER.info("Deleting level {}", directoryName);

			for (int attempt = 1; attempt <= MAX_DELETE_ATTEMPTS; attempt++) {
				LOGGER.info("Attempt {}...", attempt);

				try {
					Files.walkFileTree(directory.path(), new SimpleFileVisitor<>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							// Файл блокировки удаляется последним в postVisitDirectory
							if (!file.equals(sessionLockPath)) {
								LOGGER.debug("Deleting {}", file);
								Files.delete(file);
							}

							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exception)
						throws IOException {
							if (exception != null) {
								throw exception;
							}

							if (dir.equals(Session.this.directory.path())) {
								Session.this.lock.close();
								Files.deleteIfExists(sessionLockPath);
							}

							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						}
					});

					break;
				} catch (IOException exception) {
					if (attempt >= MAX_DELETE_ATTEMPTS) {
						throw exception;
					}

					LOGGER.warn("Failed to delete {}", directory.path(), exception);

					try {
						Thread.sleep(DELETE_RETRY_DELAY_MS);
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}

		/**
		 * Переименовывает мир, обновляя поле {@code LevelName} в level.dat.
		 *
		 * @param name новое отображаемое имя мира
		 */
		public void save(String name) throws IOException {
			save(nbt -> nbt.putString("LevelName", name.trim()));
		}

		/**
		 * Переименовывает мир и удаляет данные игрока из level.dat.
		 * Используется при пересоздании мира, чтобы игрок начал с нуля.
		 *
		 * @param name новое отображаемое имя мира
		 */
		public void removePlayerAndSave(String name) throws IOException {
			save(nbt -> {
				nbt.putString("LevelName", name.trim());
				nbt.remove("Player");
			});
		}

		private void save(Consumer<NbtCompound> nbtProcessor) throws IOException {
			checkValid();
			NbtCompound root = LevelStorage.readLevelProperties(directory.getLevelDatPath());
			nbtProcessor.accept(root.getCompoundOrEmpty("Data"));
			save(root);
		}

		/**
		 * Создаёт ZIP-архив резервной копии мира в директории резервных копий.
		 * Имя архива включает дату/время и имя директории мира.
		 * Файл {@code session.lock} исключается из архива.
		 *
		 * @return размер созданного архива в байтах
		 * @throws IOException при ошибке создания архива
		 */
		public long createBackup() throws IOException {
			checkValid();
			String archiveName = DateTimeFormatters.MINUTES.format(ZonedDateTime.now()) + "_" + directoryName;
			Path backupsPath = LevelStorage.this.getBackupsDirectory();

			try {
				PathUtil.createDirectories(backupsPath);
			} catch (IOException exception) {
				throw new RuntimeException(exception);
			}

			Path archivePath = backupsPath.resolve(PathUtil.getNextUniqueName(backupsPath, archiveName, ".zip"));

			try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(archivePath)))) {
				final Path relativeRoot = Paths.get(directoryName);

				Files.walkFileTree(directory.path(), new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.endsWith("session.lock")) {
							return FileVisitResult.CONTINUE;
						}

						String entryName = relativeRoot
							.resolve(Session.this.directory.path().relativize(file))
							.toString()
							.replace('\\', '/');
						ZipEntry entry = new ZipEntry(entryName);
						zipOut.putNextEntry(entry);
						com.google.common.io.Files.asByteSource(file.toFile()).copyTo(zipOut);
						zipOut.closeEntry();

						return FileVisitResult.CONTINUE;
					}
				});
			}

			return Files.size(archivePath);
		}

		public boolean levelDatExists() {
			return Files.exists(directory.getLevelDatPath()) || Files.exists(directory.getLevelDatOldPath());
		}

		@Override
		public void close() throws IOException {
			lock.close();
		}

		/**
		 * Пытается восстановить level.dat из резервной копии ({@code level.dat_old}).
		 * Повреждённый файл переименовывается с суффиксом {@code _corrupted_<дата>}.
		 *
		 * @return {@code true} если восстановление прошло успешно
		 */
		public boolean tryRestoreBackup() {
			return Util.backupAndReplace(
				directory.getLevelDatPath(),
				directory.getLevelDatOldPath(),
				directory.getCorruptedLevelDatPath(ZonedDateTime.now()),
				true
			);
		}

		public @Nullable Instant getLastModifiedTime(boolean old) {
			return LevelStorage.getLastModifiedTime(
				old ? directory.getLevelDatOldPath() : directory.getLevelDatPath()
			);
		}
	}
}
