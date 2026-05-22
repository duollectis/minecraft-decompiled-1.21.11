package net.minecraft.server;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtCrashException;
import net.minecraft.nbt.NbtException;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.dedicated.EulaReader;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.server.dedicated.ServerPropertiesLoader;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.SuppressLinter;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.util.profiling.jfr.InstanceType;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.ParsedSaveProperties;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.storage.ChunkCompressionFormat;
import net.minecraft.world.updater.WorldUpdater;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Точка входа выделенного сервера Minecraft.
 * Парсит аргументы командной строки, инициализирует хранилище мира,
 * загружает датапаки и запускает {@link MinecraftDedicatedServer}.
 */
public class Main {

	private static final Logger LOGGER = LogUtils.getLogger();

	@SuppressLinter(reason = "System.out needed before bootstrap")
	@DontObfuscate
	public static void main(String[] args) {
		SharedConstants.createGameVersion();

		OptionParser parser = new OptionParser();
		OptionSpec<Void> noGuiOption = parser.accepts("nogui");
		OptionSpec<Void> initSettingsOption = parser.accepts(
			"initSettings",
			"Initializes 'server.properties' and 'eula.txt', then quits"
		);
		OptionSpec<Void> demoOption = parser.accepts("demo");
		OptionSpec<Void> bonusChestOption = parser.accepts("bonusChest");
		OptionSpec<Void> forceUpgradeOption = parser.accepts("forceUpgrade");
		OptionSpec<Void> eraseCacheOption = parser.accepts("eraseCache");
		OptionSpec<Void> recreateRegionFilesOption = parser.accepts("recreateRegionFiles");
		OptionSpec<Void> safeModeOption = parser.accepts("safeMode", "Loads level with vanilla datapack only");
		OptionSpec<Void> helpOption = parser.accepts("help").forHelp();
		OptionSpec<String> universeOption = parser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
		OptionSpec<String> worldOption = parser.accepts("world").withRequiredArg();
		OptionSpec<Integer> portOption = parser.accepts("port")
			.withRequiredArg()
			.ofType(Integer.class)
			.defaultsTo(-1, new Integer[0]);
		OptionSpec<String> serverIdOption = parser.accepts("serverId").withRequiredArg();
		OptionSpec<Void> jfrProfileOption = parser.accepts("jfrProfile");
		OptionSpec<Path> pidFileOption = parser.accepts("pidFile")
			.withRequiredArg()
			.withValuesConvertedBy(new PathConverter(new PathProperties[0]));
		OptionSpec<String> nonOptions = parser.nonOptions();

		try {
			OptionSet options = parser.parse(args);

			if (options.has(helpOption)) {
				parser.printHelpOn(System.err);
				return;
			}

			Path pidFile = options.valueOf(pidFileOption);
			if (pidFile != null) {
				writePidFile(pidFile);
			}

			CrashReport.initCrashReport();

			if (options.has(jfrProfileOption)) {
				FlightProfiler.INSTANCE.start(InstanceType.SERVER);
			}

			Bootstrap.initialize();
			Bootstrap.logMissing();
			Util.startTimerHack();

			Path serverPropertiesPath = Paths.get("server.properties");
			ServerPropertiesLoader propertiesLoader = new ServerPropertiesLoader(serverPropertiesPath);
			propertiesLoader.store();
			ChunkCompressionFormat.setCurrentFormat(propertiesLoader.getPropertiesHandler().regionFileCompression);

			Path eulaPath = Paths.get("eula.txt");
			EulaReader eulaReader = new EulaReader(eulaPath);

			if (options.has(initSettingsOption)) {
				LOGGER.info("Initialized '{}' and '{}'", serverPropertiesPath.toAbsolutePath(), eulaPath.toAbsolutePath());
				return;
			}

			if (!eulaReader.isEulaAgreedTo()) {
				LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
				return;
			}

			File universeDir = new File(options.valueOf(universeOption));
			ApiServices apiServices = ApiServices.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), universeDir);

			String levelName = Optional
				.ofNullable(options.valueOf(worldOption))
				.orElse(propertiesLoader.getPropertiesHandler().levelName);

			LevelStorage levelStorage = LevelStorage.create(universeDir.toPath());
			LevelStorage.Session session = levelStorage.createSession(levelName);

			Dynamic<?> levelData = readLevelData(session);
			boolean safeMode = options.has(safeModeOption);

			if (safeMode) {
				LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
			}

			ResourcePackManager resourcePackManager = VanillaDataPackProvider.createManager(session);

			SaveLoader saveLoader;
			try {
				SaveLoading.ServerConfig serverConfig = createServerConfig(
					propertiesLoader.getPropertiesHandler(),
					levelData,
					safeMode,
					resourcePackManager
				);
				saveLoader = Util.<SaveLoader>waitAndApply(
					applyExecutor -> SaveLoading.load(
						serverConfig,
						context -> {
							Registry<DimensionOptions> dimensionRegistry =
								context.dimensionsRegistryManager().getOrThrow(RegistryKeys.DIMENSION);
							if (levelData != null) {
								ParsedSaveProperties parsed = LevelStorage.parseSaveProperties(
									levelData,
									context.dataConfiguration(),
									dimensionRegistry,
									context.worldGenRegistryManager()
								);
								return new SaveLoading.LoadContext<>(
									parsed.properties(),
									parsed.dimensions().toDynamicRegistryManager()
								);
							} else {
								LOGGER.info("No existing world data, creating new world");
								return createWorld(
									propertiesLoader,
									context,
									dimensionRegistry,
									options.has(demoOption),
									options.has(bonusChestOption)
								);
							}
						},
						SaveLoader::new,
						Util.getMainWorkerExecutor(),
						applyExecutor
					)
				).get();
			} catch (Exception e) {
				LOGGER.warn(
					"Failed to load datapacks, can't proceed with server load. "
						+ "You can either fix your datapacks or reset to vanilla with --safeMode",
					e
				);
				return;
			}

			DynamicRegistryManager.Immutable registries =
				saveLoader.combinedDynamicRegistries().getCombinedRegistryManager();
			SaveProperties saveProperties = saveLoader.saveProperties();
			boolean recreateRegionFiles = options.has(recreateRegionFilesOption);

			if (options.has(forceUpgradeOption) || recreateRegionFiles) {
				forceUpgradeWorld(
					session,
					saveProperties,
					Schemas.getFixer(),
					options.has(eraseCacheOption),
					() -> true,
					registries,
					recreateRegionFiles
				);
			}

			session.backupLevelDataFile(registries, saveProperties);

			final MinecraftDedicatedServer dedicatedServer = MinecraftServer.startServer(
				thread -> {
					MinecraftDedicatedServer server = new MinecraftDedicatedServer(
						thread,
						session,
						resourcePackManager,
						saveLoader,
						propertiesLoader,
						Schemas.getFixer(),
						apiServices
					);
					server.setServerPort(options.valueOf(portOption));
					server.setDemo(options.has(demoOption));
					server.setServerId(options.valueOf(serverIdOption));

					boolean showGui = !options.has(noGuiOption)
						&& !options.valuesOf(nonOptions).contains("nogui");
					if (showGui && !GraphicsEnvironment.isHeadless()) {
						server.createGui();
					}

					return server;
				}
			);

			Thread shutdownThread = new Thread("Server Shutdown Thread") {
				@Override
				public void run() {
					dedicatedServer.stop(true);
				}
			};
			shutdownThread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
			Runtime.getRuntime().addShutdownHook(shutdownThread);
		} catch (Throwable e) {
			LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", e);
		}
	}

	/**
	 * Читает данные уровня из сессии, при ошибке пытается восстановить из резервной копии.
	 *
	 * @return данные уровня или {@code null}, если файл level.dat отсутствует
	 */
	@Nullable
	private static Dynamic<?> readLevelData(LevelStorage.Session session) throws Exception {
		if (!session.levelDatExists()) {
			return null;
		}

		try {
			Dynamic<?> data = session.readLevelProperties();
			LevelSummary summary = session.getLevelSummary(data);

			if (summary.requiresConversion()) {
				LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
				return null;
			}

			if (!summary.isVersionAvailable()) {
				LOGGER.info("This world was created by an incompatible version.");
				return null;
			}

			return data;
		} catch (NbtException | NbtCrashException | IOException e) {
			LevelStorage.LevelSave levelSave = session.getDirectory();
			LOGGER.warn("Failed to load world data from {}", levelSave.getLevelDatPath(), e);
			LOGGER.info("Attempting to use fallback");

			try {
				Dynamic<?> fallback = session.readOldLevelProperties();
				session.getLevelSummary(fallback);
				session.tryRestoreBackup();
				return fallback;
			} catch (NbtException | NbtCrashException | IOException fallbackEx) {
				LevelStorage.LevelSave levelSave2 = session.getDirectory();
				LOGGER.error("Failed to load world data from {}", levelSave2.getLevelDatOldPath(), fallbackEx);
				LOGGER.error(
					"Failed to load world data from {} and {}. World files may be corrupted. Shutting down.",
					levelSave2.getLevelDatPath(),
					levelSave2.getLevelDatOldPath()
				);
				throw fallbackEx;
			}
		}
	}

	/**
	 * Создаёт контекст загрузки для нового мира на основе настроек сервера.
	 *
	 * @param isDemo      режим демо-версии
	 * @param bonusChest  добавить бонусный сундук при создании мира
	 */
	private static SaveLoading.LoadContext<SaveProperties> createWorld(
		ServerPropertiesLoader propertiesLoader,
		SaveLoading.LoadContextSupplierContext context,
		Registry<DimensionOptions> dimensionRegistry,
		boolean isDemo,
		boolean bonusChest
	) {
		LevelInfo levelInfo;
		GeneratorOptions generatorOptions;
		DimensionOptionsRegistryHolder dimensionHolder;

		if (isDemo) {
			levelInfo = MinecraftServer.DEMO_LEVEL_INFO;
			generatorOptions = GeneratorOptions.DEMO_OPTIONS;
			dimensionHolder = WorldPresets.createDemoOptions(context.worldGenRegistryManager());
		} else {
			ServerPropertiesHandler props = propertiesLoader.getPropertiesHandler();
			levelInfo = new LevelInfo(
				props.levelName,
				props.gameMode.get(),
				props.hardcore,
				props.difficulty.get(),
				false,
				new GameRules(context.dataConfiguration().enabledFeatures()),
				context.dataConfiguration()
			);
			generatorOptions = bonusChest
				? props.generatorOptions.withBonusChest(true)
				: props.generatorOptions;
			dimensionHolder = props.createDimensionsRegistryHolder(context.worldGenRegistryManager());
		}

		DimensionOptionsRegistryHolder.DimensionsConfig dimensionsConfig =
			dimensionHolder.toConfig(dimensionRegistry);
		Lifecycle lifecycle = dimensionsConfig
			.getLifecycle()
			.add(context.worldGenRegistryManager().getLifecycle());

		return new SaveLoading.LoadContext<>(
			new LevelProperties(levelInfo, generatorOptions, dimensionsConfig.specialWorldProperty(), lifecycle),
			dimensionsConfig.toDynamicRegistryManager()
		);
	}

	private static void writePidFile(Path path) {
		try {
			long pid = ProcessHandle.current().pid();
			Files.writeString(path, Long.toString(pid));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Создаёт конфигурацию сервера для загрузки датапаков.
	 * Если данные уровня существуют — читает конфигурацию из них, иначе использует настройки сервера.
	 */
	private static SaveLoading.ServerConfig createServerConfig(
		ServerPropertiesHandler props,
		@Nullable Dynamic<?> levelData,
		boolean safeMode,
		ResourcePackManager dataPackManager
	) {
		DataConfiguration dataConfiguration;
		boolean initMode;

		if (levelData != null) {
			dataConfiguration = LevelStorage.parseDataPackSettings(levelData);
			initMode = false;
		} else {
			dataConfiguration = new DataConfiguration(props.dataPackSettings, FeatureFlags.DEFAULT_ENABLED_FEATURES);
			initMode = true;
		}

		SaveLoading.DataPacks dataPacks = new SaveLoading.DataPacks(dataPackManager, dataConfiguration, safeMode, initMode);
		return new SaveLoading.ServerConfig(
			dataPacks,
			CommandManager.RegistrationEnvironment.DEDICATED,
			props.functionPermissionLevel
		);
	}

	/**
	 * Принудительно обновляет все чанки мира через {@link WorldUpdater}.
	 * Логирует прогресс каждую секунду до завершения или отмены.
	 */
	private static void forceUpgradeWorld(
		LevelStorage.Session session,
		SaveProperties saveProperties,
		DataFixer dataFixer,
		boolean eraseCache,
		BooleanSupplier continueCheck,
		DynamicRegistryManager registries,
		boolean recreateRegionFiles
	) {
		LOGGER.info("Forcing world upgrade!");

		try (WorldUpdater updater = new WorldUpdater(
			session,
			dataFixer,
			saveProperties,
			registries,
			eraseCache,
			recreateRegionFiles
		)) {
			Text lastStatus = null;

			while (!updater.isDone()) {
				Text currentStatus = updater.getStatus();
				if (currentStatus != lastStatus) {
					lastStatus = currentStatus;
					LOGGER.info(updater.getStatus().getString());
				}

				int totalChunks = updater.getTotalChunkCount();
				if (totalChunks > 0) {
					int processedChunks = updater.getUpgradedChunkCount() + updater.getSkippedChunkCount();
					LOGGER.info(
						"{}% completed ({} / {} chunks)...",
						MathHelper.floor((float) processedChunks / totalChunks * 100.0F),
						processedChunks,
						totalChunks
					);
				}

				if (!continueCheck.getAsBoolean()) {
					updater.cancel();
				} else {
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException ignored) {
					}
				}
			}
		}
	}
}
