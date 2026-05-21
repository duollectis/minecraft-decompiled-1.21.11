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
 * {@code Main}.
 */
public class Main {

	private static final Logger LOGGER = LogUtils.getLogger();

	@SuppressLinter(reason = "System.out needed before bootstrap")
	@DontObfuscate
	/**
	 * Main.
	 *
	 * @param args args
	 */
	public static void main(String[] args) {
		SharedConstants.createGameVersion();
		OptionParser optionParser = new OptionParser();
		OptionSpec<Void> optionSpec = optionParser.accepts("nogui");
		OptionSpec<Void>
				optionSpec2 =
				optionParser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
		OptionSpec<Void> optionSpec3 = optionParser.accepts("demo");
		OptionSpec<Void> optionSpec4 = optionParser.accepts("bonusChest");
		OptionSpec<Void> optionSpec5 = optionParser.accepts("forceUpgrade");
		OptionSpec<Void> optionSpec6 = optionParser.accepts("eraseCache");
		OptionSpec<Void> optionSpec7 = optionParser.accepts("recreateRegionFiles");
		OptionSpec<Void> optionSpec8 = optionParser.accepts("safeMode", "Loads level with vanilla datapack only");
		OptionSpec<Void> optionSpec9 = optionParser.accepts("help").forHelp();
		OptionSpec<String>
				optionSpec10 =
				optionParser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
		OptionSpec<String> optionSpec11 = optionParser.accepts("world").withRequiredArg();
		OptionSpec<Integer>
				optionSpec12 =
				optionParser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1, new Integer[0]);
		OptionSpec<String> optionSpec13 = optionParser.accepts("serverId").withRequiredArg();
		OptionSpec<Void> optionSpec14 = optionParser.accepts("jfrProfile");
		OptionSpec<Path>
				optionSpec15 =
				optionParser
						.accepts("pidFile")
						.withRequiredArg()
						.withValuesConvertedBy(new PathConverter(new PathProperties[0]));
		OptionSpec<String> optionSpec16 = optionParser.nonOptions();

		try {
			OptionSet optionSet = optionParser.parse(args);
			if (optionSet.has(optionSpec9)) {
				optionParser.printHelpOn(System.err);
				return;
			}

			Path path = (Path) optionSet.valueOf(optionSpec15);
			if (path != null) {
				writePidFile(path);
			}

			CrashReport.initCrashReport();
			if (optionSet.has(optionSpec14)) {
				FlightProfiler.INSTANCE.start(InstanceType.SERVER);
			}

			Bootstrap.initialize();
			Bootstrap.logMissing();
			Util.startTimerHack();
			Path path2 = Paths.get("server.properties");
			ServerPropertiesLoader serverPropertiesLoader = new ServerPropertiesLoader(path2);
			serverPropertiesLoader.store();
			ChunkCompressionFormat.setCurrentFormat(serverPropertiesLoader.getPropertiesHandler().regionFileCompression);
			Path path3 = Paths.get("eula.txt");
			EulaReader eulaReader = new EulaReader(path3);
			if (optionSet.has(optionSpec2)) {
				LOGGER.info("Initialized '{}' and '{}'", path2.toAbsolutePath(), path3.toAbsolutePath());
				return;
			}

			if (!eulaReader.isEulaAgreedTo()) {
				LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
				return;
			}

			File file = new File((String) optionSet.valueOf(optionSpec10));
			ApiServices apiServices = ApiServices.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), file);
			String
					string =
					(String) Optional
							.ofNullable((String) optionSet.valueOf(optionSpec11))
							.orElse(serverPropertiesLoader.getPropertiesHandler().levelName);
			LevelStorage levelStorage = LevelStorage.create(file.toPath());
			LevelStorage.Session session = levelStorage.createSession(string);
			Dynamic<?> dynamic;
			if (session.levelDatExists()) {
				LevelSummary levelSummary;
				try {
					dynamic = session.readLevelProperties();
					levelSummary = session.getLevelSummary(dynamic);
				}
				catch (NbtException | NbtCrashException | IOException var41) {
					LevelStorage.LevelSave levelSave = session.getDirectory();
					LOGGER.warn("Failed to load world data from {}", levelSave.getLevelDatPath(), var41);
					LOGGER.info("Attempting to use fallback");

					try {
						dynamic = session.readOldLevelProperties();
						levelSummary = session.getLevelSummary(dynamic);
					}
					catch (NbtException | NbtCrashException | IOException var40) {
						LOGGER.error("Failed to load world data from {}", levelSave.getLevelDatOldPath(), var40);
						LOGGER.error(
								"Failed to load world data from {} and {}. World files may be corrupted. Shutting down.",
								levelSave.getLevelDatPath(),
								levelSave.getLevelDatOldPath()
						);
						return;
					}

					session.tryRestoreBackup();
				}

				if (levelSummary.requiresConversion()) {
					LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
					return;
				}

				if (!levelSummary.isVersionAvailable()) {
					LOGGER.info("This world was created by an incompatible version.");
					return;
				}
			}
			else {
				dynamic = null;
			}

			Dynamic<?> dynamic2 = dynamic;
			boolean bl = optionSet.has(optionSpec8);
			if (bl) {
				LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
			}

			ResourcePackManager resourcePackManager = VanillaDataPackProvider.createManager(session);

			SaveLoader saveLoader;
			try {
				SaveLoading.ServerConfig
						serverConfig =
						createServerConfig(
								serverPropertiesLoader.getPropertiesHandler(),
								dynamic2,
								bl,
								resourcePackManager
						);
				saveLoader = Util.<SaveLoader>waitAndApply(
						                 applyExecutor -> SaveLoading.load(
								                 serverConfig,
								                 loadContextSupplierContext -> {
									                 Registry<DimensionOptions>
											                 registry =
											                 loadContextSupplierContext
													                 .dimensionsRegistryManager()
													                 .getOrThrow(RegistryKeys.DIMENSION);
									                 if (dynamic2 != null) {
										                 ParsedSaveProperties parsedSaveProperties = LevelStorage.parseSaveProperties(
												                 dynamic2,
												                 loadContextSupplierContext.dataConfiguration(),
												                 registry,
												                 loadContextSupplierContext.worldGenRegistryManager()
										                 );
										                 return new SaveLoading.LoadContext<>(
												                 parsedSaveProperties.properties(),
												                 parsedSaveProperties.dimensions().toDynamicRegistryManager()
										                 );
									                 }
									                 else {
										                 LOGGER.info("No existing world data, creating new world");
										                 return createWorld(
												                 serverPropertiesLoader,
												                 loadContextSupplierContext,
												                 registry,
												                 optionSet.has(optionSpec3),
												                 optionSet.has(optionSpec4)
										                 );
									                 }
								                 },
								                 SaveLoader::new,
								                 Util.getMainWorkerExecutor(),
								                 applyExecutor
						                 )
				                 )
				                 .get();
			}
			catch (Exception var39) {
				LOGGER.warn(
						"Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode",
						var39
				);
				return;
			}

			DynamicRegistryManager.Immutable
					immutable =
					saveLoader.combinedDynamicRegistries().getCombinedRegistryManager();
			SaveProperties saveProperties = saveLoader.saveProperties();
			boolean bl2 = optionSet.has(optionSpec7);
			if (optionSet.has(optionSpec5) || bl2) {
				forceUpgradeWorld(
						session,
						saveProperties,
						Schemas.getFixer(),
						optionSet.has(optionSpec6),
						() -> true,
						immutable,
						bl2
				);
			}

			session.backupLevelDataFile(immutable, saveProperties);
			final MinecraftDedicatedServer minecraftDedicatedServer = MinecraftServer.startServer(
					threadx -> {
						MinecraftDedicatedServer minecraftDedicatedServerx = new MinecraftDedicatedServer(
								threadx,
								session,
								resourcePackManager,
								saveLoader,
								serverPropertiesLoader,
								Schemas.getFixer(),
								apiServices
						);
						minecraftDedicatedServerx.setServerPort((Integer) optionSet.valueOf(optionSpec12));
						minecraftDedicatedServerx.setDemo(optionSet.has(optionSpec3));
						minecraftDedicatedServerx.setServerId((String) optionSet.valueOf(optionSpec13));
						boolean blx = !optionSet.has(optionSpec) && !optionSet.valuesOf(optionSpec16).contains("nogui");
						if (blx && !GraphicsEnvironment.isHeadless()) {
							minecraftDedicatedServerx.createGui();
						}

						return minecraftDedicatedServerx;
					}
			);
			Thread thread = new Thread("Server Shutdown Thread") {
				@Override
				public void run() {
					minecraftDedicatedServer.stop(true);
				}
			};
			thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
			Runtime.getRuntime().addShutdownHook(thread);
		}
		catch (Throwable var42) {
			LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", var42);
		}
	}

	private static SaveLoading.LoadContext<SaveProperties> createWorld(
			ServerPropertiesLoader serverPropertiesLoader,
			SaveLoading.LoadContextSupplierContext loadContextSupplierContext,
			Registry<DimensionOptions> registry,
			boolean bl,
			boolean bl2
	) {
		LevelInfo levelInfo;
		GeneratorOptions generatorOptions;
		DimensionOptionsRegistryHolder dimensionOptionsRegistryHolder;
		if (bl) {
			levelInfo = MinecraftServer.DEMO_LEVEL_INFO;
			generatorOptions = GeneratorOptions.DEMO_OPTIONS;
			dimensionOptionsRegistryHolder =
					WorldPresets.createDemoOptions(loadContextSupplierContext.worldGenRegistryManager());
		}
		else {
			ServerPropertiesHandler serverPropertiesHandler = serverPropertiesLoader.getPropertiesHandler();
			levelInfo = new LevelInfo(
					serverPropertiesHandler.levelName,
					serverPropertiesHandler.gameMode.get(),
					serverPropertiesHandler.hardcore,
					serverPropertiesHandler.difficulty.get(),
					false,
					new GameRules(loadContextSupplierContext.dataConfiguration().enabledFeatures()),
					loadContextSupplierContext.dataConfiguration()
			);
			generatorOptions =
					bl2 ? serverPropertiesHandler.generatorOptions.withBonusChest(true)
					    : serverPropertiesHandler.generatorOptions;
			dimensionOptionsRegistryHolder =
					serverPropertiesHandler.createDimensionsRegistryHolder(loadContextSupplierContext.worldGenRegistryManager());
		}

		DimensionOptionsRegistryHolder.DimensionsConfig
				dimensionsConfig =
				dimensionOptionsRegistryHolder.toConfig(registry);
		Lifecycle
				lifecycle =
				dimensionsConfig
						.getLifecycle()
						.add(loadContextSupplierContext.worldGenRegistryManager().getLifecycle());
		return new SaveLoading.LoadContext<>(
				new LevelProperties(levelInfo, generatorOptions, dimensionsConfig.specialWorldProperty(), lifecycle),
				dimensionsConfig.toDynamicRegistryManager()
		);
	}

	private static void writePidFile(Path path) {
		try {
			long l = ProcessHandle.current().pid();
			Files.writeString(path, Long.toString(l));
		}
		catch (IOException var3) {
			throw new UncheckedIOException(var3);
		}
	}

	private static SaveLoading.ServerConfig createServerConfig(
			ServerPropertiesHandler serverPropertiesHandler,
			@Nullable Dynamic<?> dynamic,
			boolean safeMode,
			ResourcePackManager dataPackManager
	) {
		boolean bl;
		DataConfiguration dataConfiguration2;
		if (dynamic != null) {
			DataConfiguration dataConfiguration = LevelStorage.parseDataPackSettings(dynamic);
			bl = false;
			dataConfiguration2 = dataConfiguration;
		}
		else {
			bl = true;
			dataConfiguration2 =
					new DataConfiguration(
							serverPropertiesHandler.dataPackSettings,
							FeatureFlags.DEFAULT_ENABLED_FEATURES
					);
		}

		SaveLoading.DataPacks dataPacks = new SaveLoading.DataPacks(dataPackManager, dataConfiguration2, safeMode, bl);
		return new SaveLoading.ServerConfig(
				dataPacks,
				CommandManager.RegistrationEnvironment.DEDICATED,
				serverPropertiesHandler.functionPermissionLevel
		);
	}

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

		try (WorldUpdater worldUpdater = new WorldUpdater(
				session,
				dataFixer,
				saveProperties,
				registries,
				eraseCache,
				recreateRegionFiles
		)
		) {
			Text text = null;

			while (!worldUpdater.isDone()) {
				Text text2 = worldUpdater.getStatus();
				if (text != text2) {
					text = text2;
					LOGGER.info(worldUpdater.getStatus().getString());
				}

				int i = worldUpdater.getTotalChunkCount();
				if (i > 0) {
					int j = worldUpdater.getUpgradedChunkCount() + worldUpdater.getSkippedChunkCount();
					LOGGER.info(
							"{}% completed ({} / {} chunks)...",
							new Object[]{MathHelper.floor((float) j / i * 100.0F), j, i}
					);
				}

				if (!continueCheck.getAsBoolean()) {
					worldUpdater.cancel();
				}
				else {
					try {
						Thread.sleep(1000L);
					}
					catch (InterruptedException var13) {
					}
				}
			}
		}
	}
}
