package net.minecraft.server;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.fabricmc.fabric.impl.resource.DataResourceStoreImpl;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FuelRegistry;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketApplyBatcher;
import net.minecraft.network.QueryableServer;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.*;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.ScoreboardState;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.debug.SubscriberTracker;
import net.minecraft.server.dedicated.management.ActivityNotifier;
import net.minecraft.server.dedicated.management.listener.CompositeManagementListener;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.network.*;
import net.minecraft.server.world.*;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.test.TestManager;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.crash.*;
import net.minecraft.util.function.Finishable;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.profiler.*;
import net.minecraft.util.profiler.log.DebugSampleLog;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.util.profiling.jfr.InstanceType;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.village.ZombieSiegeManager;
import net.minecraft.world.*;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkLoadMap;
import net.minecraft.world.chunk.ChunkLoadProgress;
import net.minecraft.world.chunk.ChunkLoadingCounter;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.feature.MiscConfiguredFeatures;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleVisitor;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.spawner.CatSpawner;
import net.minecraft.world.spawner.PatrolSpawner;
import net.minecraft.world.spawner.PhantomSpawner;
import net.minecraft.world.spawner.SpecialSpawner;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.timer.stopwatch.StopwatchPersistentState;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Proxy;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Абстрактная реализация сервера Minecraft.
 * Управляет игровым циклом, мирами, игроками, датапаками и сетевым вводом-выводом.
 * Конкретные реализации: {@link net.minecraft.server.dedicated.MinecraftDedicatedServer}
 * и {@link net.minecraft.server.integrated.IntegratedServer}.
 */
public abstract class MinecraftServer
	extends ReentrantThreadExecutor<ServerTask>
	implements QueryableServer, CommandOutput, ChunkErrorHandler, DataResourceStore {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final String VANILLA = "vanilla";
	private static final float TICK_TIME_EMA_DECAY = 0.8F;
	private static final int TICK_TIMES_ARRAY_SIZE = 100;
	private static final long OVERLOAD_THRESHOLD_NANOS = 20L * TimeHelper.SECOND_IN_NANOS / 20L;
	private static final int TICKS_PER_SECOND = 20;
	private static final long OVERLOAD_WARNING_INTERVAL_NANOS = 10L * TimeHelper.SECOND_IN_NANOS;
	private static final long PLAYER_SAMPLE_UPDATE_INTERVAL_NANOS = 5L * TimeHelper.SECOND_IN_NANOS;
	private static final long PREPARE_START_REGION_TICK_DELAY_NANOS = 10L * TimeHelper.MILLI_IN_NANOS;
	private static final int DEFAULT_SPAWN_PROTECTION_RADIUS = 12;
	private static final int MAX_PLAYER_SAMPLE_COUNT = 30;
	private static final int RECENT_TICK_TIMES_WINDOW = 100;
	private static final int NETWORK_COMPRESSION_THRESHOLD = 256;
	private static final int MAX_CHAINED_NEIGHBOR_UPDATES = 1000000;
	private static final int FAVICON_SIZE = 64;
	private static final long MIN_CHUNK_REPORT_DISK_SPACE = 8192L;

	public static final int MAX_OP_PERMISSION_LEVEL = 5;
	public static final int MAX_WORLD_BORDER_RADIUS = 29999984;
	public static final LevelInfo DEMO_LEVEL_INFO = new LevelInfo(
		"Demo World",
		GameMode.SURVIVAL,
		false,
		Difficulty.NORMAL,
		false,
		new GameRules(FeatureFlags.DEFAULT_ENABLED_FEATURES),
		DataConfiguration.SAFE_MODE
	);
	public static final PlayerConfigEntry ANONYMOUS_PLAYER_PROFILE =
		new PlayerConfigEntry(Util.NIL_UUID, "Anonymous Player");

	private static final AtomicReference<@Nullable RuntimeException> WORLD_GEN_EXCEPTION = new AtomicReference<>();

	protected final LevelStorage.Session session;
	protected final PlayerSaveHandler saveHandler;
	protected final SaveProperties saveProperties;
	protected final Proxy proxy;
	protected final ApiServices apiServices;

	private final List<Runnable> serverGuiTickables = Lists.newArrayList();
	private Recorder recorder = DummyRecorder.INSTANCE;
	private Consumer<ProfileResult> recorderResultConsumer = profileResult -> resetRecorder();
	private Consumer<Path> recorderDumpConsumer = path -> {};
	private boolean needsRecorderSetup;
	private MinecraftServer.@Nullable DebugStart debugStart;
	private boolean needsDebugSetup;
	private final ServerNetworkIo networkIo;
	private final ChunkLoadProgress chunkLoadProgress;
	private @Nullable ServerMetadata metadata;
	private ServerMetadata.@Nullable Favicon favicon;
	private final Random random = Random.create();
	private final DataFixer dataFixer;
	private String serverIp;
	private int serverPort = -1;
	private final CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries;
	private final Map<RegistryKey<World>, ServerWorld> worlds = Maps.newLinkedHashMap();
	private PlayerManager playerManager;
	private volatile boolean running = true;
	private boolean stopped;
	private int ticks;
	private int ticksUntilAutosave;
	private boolean onlineMode;
	private boolean preventProxyConnections;
	private @Nullable String motd;
	private int playerIdleTimeout;
	private final long[] tickTimes = new long[TICK_TIMES_ARRAY_SIZE];
	private long recentTickTimesNanos = 0L;
	private @Nullable KeyPair keyPair;
	private @Nullable GameProfile hostProfile;
	private boolean demo;
	private volatile boolean loading;
	private long lastOverloadWarningNanos;
	private final CompositeManagementListener managementListener;
	private final ActivityNotifier activityNotifier;
	private long lastPlayerSampleUpdate;
	private final Thread serverThread;
	private long lastFullTickLogTime = Util.getMeasuringTimeNano();
	private long tasksStartTime = Util.getMeasuringTimeNano();
	private long waitTime;
	private long tickStartTimeNanos = Util.getMeasuringTimeNano();
	private boolean waitingForNextTick = false;
	private long tickEndTimeNanos;
	private boolean hasJustExecutedTask;
	private final ResourcePackManager dataPackManager;
	private final ServerScoreboard scoreboard = new ServerScoreboard(this);
	private @Nullable StopwatchPersistentState stopwatchPersistentState;
	private @Nullable DataCommandStorage dataCommandStorage;
	private final BossBarManager bossBarManager = new BossBarManager();
	private final CommandFunctionManager commandFunctionManager;
	private boolean enforceWhitelist;
	private boolean useAllowlist;
	private float averageTickTime;
	private final Executor workerExecutor;
	private @Nullable String serverId;
	private MinecraftServer.ResourceManagerHolder resourceManagerHolder;
	private final StructureTemplateManager structureTemplateManager;
	private final ServerTickManager tickManager;
	private final SubscriberTracker subscriberTracker = new SubscriberTracker(this);
	private WorldProperties.SpawnPoint spawnPoint = WorldProperties.SpawnPoint.DEFAULT;
	private final BrewingRecipeRegistry brewingRecipeRegistry;
	private FuelRegistry fuelRegistry;
	private int idleTickCount;
	private volatile boolean saving;
	private final SuppressedExceptionsTracker suppressedExceptionsTracker = new SuppressedExceptionsTracker();
	private final DiscontinuousFrame discontinuousFrame;
	private final PacketApplyBatcher packetApplyBatcher;
	private final DataResourceStoreImpl dataResourceStore = new DataResourceStoreImpl();

	/**
	 * Создаёт и запускает сервер в отдельном потоке.
	 * Устанавливает повышенный приоритет потока на многоядерных системах (более 4 ядер).
	 */
	public static <S extends MinecraftServer> S startServer(Function<Thread, S> serverFactory) {
		AtomicReference<S> serverRef = new AtomicReference<>();
		Thread thread = new Thread(() -> serverRef.get().runServer(), "Server thread");
		thread.setUncaughtExceptionHandler(
			(t, throwable) -> LOGGER.error("Uncaught exception in server thread", throwable)
		);

		if (Runtime.getRuntime().availableProcessors() > 4) {
			thread.setPriority(8);
		}

		S server = serverFactory.apply(thread);
		serverRef.set(server);
		thread.start();
		return server;
	}

	public MinecraftServer(
		Thread serverThread,
		LevelStorage.Session session,
		ResourcePackManager dataPackManager,
		SaveLoader saveLoader,
		Proxy proxy,
		DataFixer dataFixer,
		ApiServices apiServices,
		ChunkLoadProgress chunkLoadProgress
	) {
		super("Server");
		this.combinedDynamicRegistries = saveLoader.combinedDynamicRegistries();
		this.saveProperties = saveLoader.saveProperties();

		if (!combinedDynamicRegistries
			.getCombinedRegistryManager()
			.getOrThrow(RegistryKeys.DIMENSION)
			.contains(DimensionOptions.OVERWORLD)
		) {
			throw new IllegalStateException("Missing Overworld dimension data");
		}

		this.proxy = proxy;
		this.dataPackManager = dataPackManager;
		this.resourceManagerHolder = new MinecraftServer.ResourceManagerHolder(
			saveLoader.resourceManager(),
			saveLoader.dataPackContents()
		);
		this.apiServices = apiServices;
		this.networkIo = new ServerNetworkIo(this);
		this.tickManager = new ServerTickManager(this);
		this.chunkLoadProgress = chunkLoadProgress;
		this.session = session;
		this.saveHandler = session.createSaveHandler();
		this.dataFixer = dataFixer;
		this.commandFunctionManager = new CommandFunctionManager(
			this,
			resourceManagerHolder.dataPackContents.getFunctionLoader()
		);

		RegistryEntryLookup<Block> blockLookup = combinedDynamicRegistries
			.getCombinedRegistryManager()
			.getOrThrow(RegistryKeys.BLOCK)
			.withFeatureFilter(saveProperties.getEnabledFeatures());

		this.structureTemplateManager = new StructureTemplateManager(
			saveLoader.resourceManager(),
			session,
			dataFixer,
			blockLookup
		);
		this.serverThread = serverThread;
		this.workerExecutor = Util.getMainWorkerExecutor();
		this.brewingRecipeRegistry = BrewingRecipeRegistry.create(saveProperties.getEnabledFeatures());
		resourceManagerHolder.dataPackContents
			.getRecipeManager()
			.initialize(saveProperties.getEnabledFeatures());
		this.fuelRegistry = FuelRegistry.createDefault(
			combinedDynamicRegistries.getCombinedRegistryManager(),
			saveProperties.getEnabledFeatures()
		);
		this.discontinuousFrame = TracyClient.createDiscontinuousFrame("Server Tick");
		this.managementListener = new CompositeManagementListener();
		this.activityNotifier = new ActivityNotifier(managementListener, MAX_PLAYER_SAMPLE_COUNT);
		this.packetApplyBatcher = new PacketApplyBatcher(serverThread);
		this.ticksUntilAutosave = getAutosaveInterval();
	}

	protected abstract boolean setupServer() throws IOException;

	public ChunkLoadMap createChunkLoadMap(int radius) {
		return new ChunkLoadMap() {
			private @Nullable ServerChunkLoadingManager chunkLoadingManager;
			private int spawnChunkX;
			private int spawnChunkZ;

			@Override
			public void initSpawnPos(RegistryKey<World> world, ChunkPos spawnPos) {
				ServerWorld serverWorld = MinecraftServer.this.getWorld(world);
				chunkLoadingManager = serverWorld != null
					? serverWorld.getChunkManager().chunkLoadingManager
					: null;
				spawnChunkX = spawnPos.x;
				spawnChunkZ = spawnPos.z;
			}

			@Override
			public @Nullable ChunkStatus getStatus(int x, int z) {
				return chunkLoadingManager == null
					? null
					: chunkLoadingManager.getStatus(ChunkPos.toLong(
						x + spawnChunkX - radius,
						z + spawnChunkZ - radius
					));
			}

			@Override
			public int getRadius() {
				return radius;
			}
		};
	}

	protected void loadWorld() {
		boolean startedJfr = !FlightProfiler.INSTANCE.isProfiling()
			&& SharedConstants.JFR_PROFILING_ENABLE_LEVEL_LOADING
			&& FlightProfiler.INSTANCE.start(InstanceType.get(this));
		Finishable finishable = FlightProfiler.INSTANCE.startWorldLoadProfiling();

		saveProperties.addServerBrand(getServerModName(), getModStatus().isModded());
		createWorlds();
		updateDifficulty();
		prepareStartRegion();

		if (finishable != null) {
			finishable.finish(true);
		}

		if (startedJfr) {
			try {
				FlightProfiler.INSTANCE.stop();
			} catch (Throwable e) {
				LOGGER.warn("Failed to stop JFR profiling", e);
			}
		}
	}

	protected void updateDifficulty() {
	}

	protected void createWorlds() {
		ServerWorldProperties mainWorldProperties = saveProperties.getMainWorldProperties();
		boolean isDebugWorld = saveProperties.isDebugWorld();
		Registry<DimensionOptions> dimensionRegistry = combinedDynamicRegistries
			.getCombinedRegistryManager()
			.getOrThrow(RegistryKeys.DIMENSION);
		GeneratorOptions generatorOptions = saveProperties.getGeneratorOptions();
		long seed = generatorOptions.getSeed();
		long biomeSeed = BiomeAccess.hashSeed(seed);

		List<SpecialSpawner> specialSpawners = ImmutableList.of(
			new PhantomSpawner(),
			new PatrolSpawner(),
			new CatSpawner(),
			new ZombieSiegeManager(),
			new WanderingTraderManager(mainWorldProperties)
		);

		DimensionOptions overworldOptions = dimensionRegistry.get(DimensionOptions.OVERWORLD);
		ServerWorld overworld = new ServerWorld(
			this,
			workerExecutor,
			session,
			mainWorldProperties,
			World.OVERWORLD,
			overworldOptions,
			isDebugWorld,
			biomeSeed,
			specialSpawners,
			true,
			null
		);
		worlds.put(World.OVERWORLD, overworld);

		PersistentStateManager persistentStateManager = overworld.getPersistentStateManager();
		scoreboard.read(persistentStateManager.getOrCreate(ScoreboardState.TYPE).getPackedState());
		dataCommandStorage = new DataCommandStorage(persistentStateManager);
		stopwatchPersistentState = persistentStateManager.getOrCreate(StopwatchPersistentState.STATE_TYPE);

		if (!mainWorldProperties.isInitialized()) {
			try {
				setupSpawn(
					overworld,
					mainWorldProperties,
					generatorOptions.hasBonusChest(),
					isDebugWorld,
					chunkLoadProgress
				);
				mainWorldProperties.setInitialized(true);

				if (isDebugWorld) {
					setToDebugWorldProperties(saveProperties);
				}
			} catch (Throwable e) {
				CrashReport crashReport = CrashReport.create(e, "Exception initializing level");

				try {
					overworld.addDetailsToCrashReport(crashReport);
				} catch (Throwable ignored) {
				}

				throw new CrashException(crashReport);
			}

			mainWorldProperties.setInitialized(true);
		}

		GlobalPos spawnGlobalPos = getSpawnPos();
		chunkLoadProgress.initSpawnPos(spawnGlobalPos.dimension(), new ChunkPos(spawnGlobalPos.pos()));

		if (saveProperties.getCustomBossEvents() != null) {
			getBossBarManager().readNbt(saveProperties.getCustomBossEvents(), getRegistryManager());
		}

		RandomSequencesState randomSequences = overworld.getRandomSequences();
		boolean hadWorldBorder = false;

		for (Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : dimensionRegistry.getEntrySet()) {
			RegistryKey<DimensionOptions> dimensionKey = entry.getKey();
			ServerWorld world;

			if (dimensionKey != DimensionOptions.OVERWORLD) {
				RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionKey.getValue());
				UnmodifiableLevelProperties levelProperties =
					new UnmodifiableLevelProperties(saveProperties, mainWorldProperties);
				world = new ServerWorld(
					this,
					workerExecutor,
					session,
					levelProperties,
					worldKey,
					entry.getValue(),
					isDebugWorld,
					biomeSeed,
					ImmutableList.of(),
					false,
					randomSequences
				);
				worlds.put(worldKey, world);
			} else {
				world = overworld;
			}

			Optional<WorldBorder.Properties> borderProps = mainWorldProperties.getWorldBorder();
			if (borderProps.isPresent()) {
				WorldBorder.Properties props = borderProps.get();
				PersistentStateManager worldPersistentState = world.getPersistentStateManager();

				if (worldPersistentState.get(WorldBorder.TYPE) == null) {
					double coordScale = world.getDimension().coordinateScale();
					WorldBorder.Properties scaledProps = new WorldBorder.Properties(
						props.centerX() / coordScale,
						props.centerZ() / coordScale,
						props.damagePerBlock(),
						props.safeZone(),
						props.warningBlocks(),
						props.warningTime(),
						props.size(),
						props.lerpTime(),
						props.lerpTarget()
					);
					WorldBorder worldBorder = new WorldBorder(scaledProps);
					worldBorder.ensureInitialized(world.getTime());
					worldPersistentState.set(WorldBorder.TYPE, worldBorder);
				}

				hadWorldBorder = true;
			}

			world.getWorldBorder().setMaxRadius(getMaxWorldBorderRadius());
			getPlayerManager().setMainWorld(world);
		}

		if (hadWorldBorder) {
			mainWorldProperties.setWorldBorder(Optional.empty());
		}
	}

	private static void setupSpawn(
		ServerWorld world,
		ServerWorldProperties worldProperties,
		boolean bonusChest,
		boolean debugWorld,
		ChunkLoadProgress loadProgress
	) {
		if (SharedConstants.ONLY_GENERATE_HALF_THE_WORLD && SharedConstants.WORLD_RECREATE) {
			worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(
				world.getRegistryKey(),
				new BlockPos(0, 64, -100),
				0.0F,
				0.0F
			));
		} else if (debugWorld) {
			worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(
				world.getRegistryKey(),
				BlockPos.ORIGIN.up(80),
				0.0F,
				0.0F
			));
		} else {
			ServerChunkManager chunkManager = world.getChunkManager();
			ChunkPos spawnChunk = new ChunkPos(
				chunkManager.getNoiseConfig().getMultiNoiseSampler().findBestSpawnPosition()
			);
			loadProgress.init(ChunkLoadProgress.Stage.PREPARE_GLOBAL_SPAWN, 0);
			loadProgress.initSpawnPos(world.getRegistryKey(), spawnChunk);

			int spawnHeight = chunkManager.getChunkGenerator().getSpawnHeight(world);
			if (spawnHeight < world.getBottomY()) {
				BlockPos startPos = spawnChunk.getStartPos();
				spawnHeight = world.getTopY(Heightmap.Type.WORLD_SURFACE, startPos.getX() + 8, startPos.getZ() + 8);
			}

			worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(
				world.getRegistryKey(),
				spawnChunk.getStartPos().add(8, spawnHeight, 8),
				0.0F,
				0.0F
			));

			// Поиск подходящей точки спавна по спирали 11x11 чанков
			int dx = 0;
			int dz = 0;
			int stepX = 0;
			int stepZ = -1;

			for (int step = 0; step < MathHelper.square(11); step++) {
				if (dx >= -5 && dx <= 5 && dz >= -5 && dz <= 5) {
					BlockPos spawnPos = SpawnLocating.findServerSpawnPoint(
						world,
						new ChunkPos(spawnChunk.x + dx, spawnChunk.z + dz)
					);
					if (spawnPos != null) {
						worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(
							world.getRegistryKey(),
							spawnPos,
							0.0F,
							0.0F
						));
						break;
					}
				}

				if (dx == dz || dx < 0 && dx == -dz || dx > 0 && dx == 1 - dz) {
					int tmp = stepX;
					stepX = -stepZ;
					stepZ = tmp;
				}

				dx += stepX;
				dz += stepZ;
			}

			if (bonusChest) {
				world.getRegistryManager()
					.getOptional(RegistryKeys.CONFIGURED_FEATURE)
					.flatMap(reg -> reg.getOptional(MiscConfiguredFeatures.BONUS_CHEST))
					.ifPresent(feature -> feature.value().generate(
						world,
						chunkManager.getChunkGenerator(),
						world.random,
						worldProperties.getSpawnPoint().getPos()
					));
			}

			loadProgress.finish(ChunkLoadProgress.Stage.PREPARE_GLOBAL_SPAWN);
		}
	}

	private void setToDebugWorldProperties(SaveProperties properties) {
		properties.setDifficulty(Difficulty.PEACEFUL);
		properties.setDifficultyLocked(true);
		ServerWorldProperties worldProperties = properties.getMainWorldProperties();
		worldProperties.setRaining(false);
		worldProperties.setThundering(false);
		worldProperties.setClearWeatherTime(1000000000);
		worldProperties.setTimeOfDay(6000L);
		worldProperties.setGameMode(GameMode.SPECTATOR);
	}

	private void prepareStartRegion() {
		ChunkLoadingCounter counter = new ChunkLoadingCounter();

		for (ServerWorld world : worlds.values()) {
			counter.load(world, () -> {
				ChunkTicketManager ticketManager =
					world.getPersistentStateManager().get(ChunkTicketManager.STATE_TYPE);
				if (ticketManager != null) {
					ticketManager.promoteToRealTickets();
				}
			});
		}

		chunkLoadProgress.init(ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS, counter.getTotalChunks());

		do {
			chunkLoadProgress.progress(
				ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS,
				counter.getFullChunks(),
				counter.getTotalChunks()
			);
			tickStartTimeNanos = Util.getMeasuringTimeNano() + PREPARE_START_REGION_TICK_DELAY_NANOS;
			runTasksTillTickEnd();
		} while (counter.getNonFullChunks() > 0);

		chunkLoadProgress.finish(ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS);
		updateMobSpawnOptions();
		refreshSpawnPoint();
	}

	protected GlobalPos getSpawnPos() {
		return saveProperties.getMainWorldProperties().getSpawnPoint().globalPos();
	}

	public GameMode getDefaultGameMode() {
		return saveProperties.getGameMode();
	}

	public boolean isHardcore() {
		return saveProperties.isHardcore();
	}

	public abstract LeveledPermissionPredicate getOpPermissionLevel();

	public abstract PermissionPredicate getFunctionPermissions();

	public abstract boolean shouldBroadcastRconToOps();

	/**
	 * Сохраняет все миры и данные игроков.
	 *
	 * @param suppressLogs не выводить лог о сохранении чанков
	 * @param flush        ждать полного сброса данных на диск
	 * @param force        сохранять даже если сохранение отключено
	 */
	public boolean save(boolean suppressLogs, boolean flush, boolean force) {
		scoreboard.writeTo(getOverworld().getPersistentStateManager().getOrCreate(ScoreboardState.TYPE));
		boolean saved = false;

		for (ServerWorld world : getWorlds()) {
			if (!suppressLogs) {
				LOGGER.info("Saving chunks for level '{}'/{}", world, world.getRegistryKey().getValue());
			}

			world.save(null, flush, SharedConstants.DONT_SAVE_WORLD || world.savingDisabled && !force);
			saved = true;
		}

		saveProperties.setCustomBossEvents(getBossBarManager().toNbt(getRegistryManager()));
		session.backupLevelDataFile(
			getRegistryManager(),
			saveProperties,
			getPlayerManager().getUserData()
		);

		if (flush) {
			for (ServerWorld world : getWorlds()) {
				LOGGER.info(
					"ThreadedAnvilChunkStorage ({}): All chunks are saved",
					world.getChunkManager().chunkLoadingManager.getSaveDir()
				);
			}

			LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
		}

		return saved;
	}


	public boolean saveAll(boolean suppressLogs, boolean flush, boolean force) {
		try {
			saving = true;
			getPlayerManager().saveAllPlayerData();
			return save(suppressLogs, flush, force);
		} finally {
			saving = false;
		}
	}

	@Override
	public void close() {
		shutdown();
	}

	/**
	 * Останавливает сервер: сохраняет данные игроков, миры и освобождает ресурсы.
	 * Вызывается как при штатном завершении, так и при аварийном.
	 */
	public void shutdown() {
		packetApplyBatcher.close();
		if (recorder.isActive()) {
			forceStopRecorder();
		}

		LOGGER.info("Stopping server");
		getNetworkIo().stop();
		saving = true;

		if (playerManager != null) {
			LOGGER.info("Saving players");
			playerManager.saveAllPlayerData();
			playerManager.disconnectAllPlayers();
		}

		LOGGER.info("Saving worlds");

		for (ServerWorld world : getWorlds()) {
			if (world != null) {
				world.savingDisabled = false;
			}
		}

		while (worlds.values().stream().anyMatch(world -> world.getChunkManager().chunkLoadingManager.shouldDelayShutdown())) {
			tickStartTimeNanos = Util.getMeasuringTimeNano() + TimeHelper.MILLI_IN_NANOS;

			for (ServerWorld world : getWorlds()) {
				world.getChunkManager().shutdown();
				world.getChunkManager().tick(() -> true, false);
			}

			runTasksTillTickEnd();
		}

		save(false, true, false);

		for (ServerWorld world : getWorlds()) {
			if (world != null) {
				try {
					world.close();
				} catch (IOException e) {
					LOGGER.error("Exception closing the level", e);
				}
			}
		}

		saving = false;
		resourceManagerHolder.close();

		try {
			session.close();
		} catch (IOException e) {
			LOGGER.error("Failed to unlock level {}", session.getDirectoryName(), e);
		}
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public boolean isRunning() {
		return running;
	}

	public void stop(boolean waitForShutdown) {
		running = false;
		if (waitForShutdown) {
			try {
				serverThread.join();
			} catch (InterruptedException e) {
				LOGGER.error("Error while shutting down", e);
			}
		}
	}

	/**
	 * Основной цикл выполнения сервера. Инициализирует сервер, затем крутит
	 * игровой цикл до тех пор, пока {@code running == true}.
	 * При любом необработанном исключении формирует crash-report и завершает работу.
	 */
	protected void runServer() {
		try {
			if (!setupServer()) {
				throw new IllegalStateException("Failed to initialize server");
			}

			tickStartTimeNanos = Util.getMeasuringTimeNano();
			favicon = loadFavicon().orElse(null);
			metadata = createMetadata();

			while (running) {
				long nanosPerTick;
				if (!isPaused() && tickManager.isSprinting() && tickManager.sprint()) {
					nanosPerTick = 0L;
					tickStartTimeNanos = Util.getMeasuringTimeNano();
					lastOverloadWarningNanos = tickStartTimeNanos;
				} else {
					nanosPerTick = tickManager.getNanosPerTick();
					long elapsedNanos = Util.getMeasuringTimeNano() - tickStartTimeNanos;
					if (elapsedNanos > OVERLOAD_THRESHOLD_NANOS + 20L * nanosPerTick
							&& tickStartTimeNanos - lastOverloadWarningNanos
							>= OVERLOAD_WARNING_INTERVAL_NANOS + 100L * nanosPerTick) {
						long ticksBehind = elapsedNanos / nanosPerTick;
						LOGGER.warn(
								"Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind",
								elapsedNanos / TimeHelper.MILLI_IN_NANOS,
								ticksBehind
						);
						tickStartTimeNanos += ticksBehind * nanosPerTick;
						lastOverloadWarningNanos = tickStartTimeNanos;
					}
				}

				boolean isSprinting = nanosPerTick == 0L;
				if (needsDebugSetup) {
					needsDebugSetup = false;
					debugStart = new MinecraftServer.DebugStart(Util.getMeasuringTimeNano(), ticks);
				}

				tickStartTimeNanos += nanosPerTick;

				try (Profilers.Scoped scoped = Profilers.using(startTickMetrics())) {
					processPacketsAndTick(isSprinting);
					Profiler profiler = Profilers.get();
					profiler.push("nextTickWait");
					hasJustExecutedTask = true;
					tickEndTimeNanos = Math.max(Util.getMeasuringTimeNano() + nanosPerTick, tickStartTimeNanos);
					startTaskPerformanceLog();
					runTasksTillTickEnd();
					pushPerformanceLogs();
					if (isSprinting) {
						tickManager.updateSprintTime();
					}

					profiler.pop();
					pushFullTickLog();
				} finally {
					endTickMetrics();
				}

				loading = true;
				FlightProfiler.INSTANCE.onTick(averageTickTime);
			}
		} catch (Throwable throwable) {
			LOGGER.error("Encountered an unexpected exception", throwable);
			CrashReport crashReport = createCrashReport(throwable);
			addSystemDetails(crashReport.getSystemDetailsSection());
			Path crashPath = getRunDirectory()
					.resolve("crash-reports")
					.resolve("crash-" + Util.getFormattedCurrentTime() + "-server.txt");
			if (crashReport.writeToFile(crashPath, ReportType.MINECRAFT_CRASH_REPORT)) {
				LOGGER.error("This crash report has been saved to: {}", crashPath.toAbsolutePath());
			} else {
				LOGGER.error("We were unable to save this crash report to disk.");
			}

			setCrashReport(crashReport);
		} finally {
			try {
				stopped = true;
				shutdown();
			} catch (Throwable throwable) {
				LOGGER.error("Exception stopping the server", throwable);
			} finally {
				exit();
			}
		}
	}

	private void pushFullTickLog() {
		long now = Util.getMeasuringTimeNano();
		if (shouldPushTickTimeLog()) {
			getDebugSampleLog().push(now - lastFullTickLogTime);
		}

		lastFullTickLogTime = now;
	}

	private void startTaskPerformanceLog() {
		if (shouldPushTickTimeLog()) {
			tasksStartTime = Util.getMeasuringTimeNano();
			waitTime = 0L;
		}
	}

	private void pushPerformanceLogs() {
		if (shouldPushTickTimeLog()) {
			DebugSampleLog debugSampleLog = getDebugSampleLog();
			debugSampleLog.push(
					Util.getMeasuringTimeNano() - tasksStartTime - waitTime,
					ServerTickType.SCHEDULED_TASKS.ordinal()
			);
			debugSampleLog.push(waitTime, ServerTickType.IDLE.ordinal());
		}
	}

	private static CrashReport createCrashReport(Throwable throwable) {
		CrashException innerCrash = null;

		for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof CrashException crashException) {
				innerCrash = crashException;
			}
		}

		if (innerCrash == null) {
			return new CrashReport("Exception in server tick loop", throwable);
		}

		CrashReport crashReport = innerCrash.getReport();
		if (innerCrash != throwable) {
			crashReport.addElement("Wrapped in").add("Wrapping exception", throwable);
		}

		return crashReport;
	}

	private boolean shouldKeepTicking() {
		return hasRunningTasks()
				|| Util.getMeasuringTimeNano() < (hasJustExecutedTask ? tickEndTimeNanos : tickStartTimeNanos);
	}

	/**
	 * Проверяет наличие отложенного исключения из потока генерации мира.
	 * Если исключение было установлено через {@link #setWorldGenException}, пробрасывает его.
	 *
	 * @return {@code true} если исключений нет
	 */
	public static boolean checkWorldGenException() {
		RuntimeException exception = WORLD_GEN_EXCEPTION.get();
		if (exception != null) {
			throw exception;
		}

		return true;
	}

	public static void setWorldGenException(RuntimeException exception) {
		WORLD_GEN_EXCEPTION.compareAndSet(null, exception);
	}

	@Override
	public void runTasks(BooleanSupplier stopCondition) {
		super.runTasks(() -> checkWorldGenException() && stopCondition.getAsBoolean());
	}

	public CompositeManagementListener getManagementListener() {
		return managementListener;
	}

	protected void runTasksTillTickEnd() {
		runTasks();
		waitingForNextTick = true;

		try {
			runTasks(() -> !shouldKeepTicking());
		} finally {
			waitingForNextTick = false;
		}
	}

	@Override
	public void waitForTasks() {
		boolean shouldLog = shouldPushTickTimeLog();
		long startNanos = shouldLog ? Util.getMeasuringTimeNano() : 0L;
		long parkNanos = waitingForNextTick ? tickStartTimeNanos - Util.getMeasuringTimeNano() : 100000L;
		LockSupport.parkNanos("waiting for tasks", parkNanos);
		if (shouldLog) {
			waitTime += Util.getMeasuringTimeNano() - startNanos;
		}
	}

	public ServerTask createTask(Runnable runnable) {
		return new ServerTask(ticks, runnable);
	}

	protected boolean canExecute(ServerTask serverTask) {
		return serverTask.getCreationTicks() + 3 < ticks || shouldKeepTicking();
	}

	@Override
	public boolean runTask() {
		boolean executed = runOneTask();
		hasJustExecutedTask = executed;
		return executed;
	}

	private boolean runOneTask() {
		if (super.runTask()) {
			return true;
		}

		if (tickManager.isSprinting() || isExecutionInProgress() || shouldKeepTicking()) {
			for (ServerWorld world : getWorlds()) {
				if (world.getChunkManager().executeQueuedTasks()) {
					return true;
				}
			}
		}

		return false;
	}

	public void executeTask(ServerTask serverTask) {
		Profilers.get().visit("runTask");
		super.executeTask(serverTask);
	}

	private Optional<ServerMetadata.Favicon> loadFavicon() {
		Optional<Path> iconPath = Optional.of(getPath("server-icon.png"))
				.filter(Files::isRegularFile)
				.or(() -> session.getIconFile().filter(Files::isRegularFile));

		return iconPath.flatMap(path -> {
			try {
				byte[] bytes = Files.readAllBytes(path);
				PngMetadata pngMetadata = PngMetadata.fromBytes(bytes);
				if (pngMetadata.width() == FAVICON_SIZE && pngMetadata.height() == FAVICON_SIZE) {
					return Optional.of(new ServerMetadata.Favicon(bytes));
				}

				throw new IllegalArgumentException(
						"Invalid world icon size [" + pngMetadata.width() + ", " + pngMetadata.height()
								+ "], but expected [" + FAVICON_SIZE + ", " + FAVICON_SIZE + "]"
				);
			} catch (Exception e) {
				LOGGER.error("Couldn't load server icon", e);
				return Optional.empty();
			}
		});
	}

	public Optional<Path> getIconFile() {
		return session.getIconFile();
	}

	public Path getRunDirectory() {
		return Path.of("");
	}

	public ActivityNotifier getActivityNotifier() {
		return activityNotifier;
	}

	public void setCrashReport(CrashReport report) {
	}

	public void exit() {
	}

	public boolean isPaused() {
		return false;
	}

	/**
	 * Выполняет один игровой тик. Управляет паузой при пустом сервере,
	 * обновляет метаданные, запускает автосохранение и собирает статистику времени тика.
	 *
	 * @param shouldKeepTicking условие продолжения обработки задач в этом тике
	 */
	public void tick(BooleanSupplier shouldKeepTicking) {
		long tickStartTime = Util.getMeasuringTimeNano();
		int pauseThreshold = getPauseWhenEmptySeconds() * TICKS_PER_SECOND;

		if (pauseThreshold > 0) {
			if (playerManager.getCurrentPlayerCount() == 0 && !tickManager.isSprinting()) {
				idleTickCount++;
			} else {
				idleTickCount = 0;
			}

			if (idleTickCount >= pauseThreshold) {
				if (idleTickCount == pauseThreshold) {
					LOGGER.info("Server empty for {} seconds, pausing", getPauseWhenEmptySeconds());
					runAutosave();
				}

				tickNetworkIo();
				return;
			}
		}

		ticks++;
		tickManager.step();
		tickWorlds(shouldKeepTicking);

		if (tickStartTime - lastPlayerSampleUpdate >= PLAYER_SAMPLE_UPDATE_INTERVAL_NANOS) {
			lastPlayerSampleUpdate = tickStartTime;
			metadata = createMetadata();
		}

		ticksUntilAutosave--;
		if (ticksUntilAutosave <= 0) {
			runAutosave();
		}

		Profiler profiler = Profilers.get();
		profiler.push("tallying");
		long tickDuration = Util.getMeasuringTimeNano() - tickStartTime;
		int tickIndex = ticks % RECENT_TICK_TIMES_WINDOW;
		recentTickTimesNanos -= tickTimes[tickIndex];
		recentTickTimesNanos += tickDuration;
		tickTimes[tickIndex] = tickDuration;
		averageTickTime = averageTickTime * TICK_TIME_EMA_DECAY
				+ (float) tickDuration / (float) TimeHelper.MILLI_IN_NANOS * (1.0F - TICK_TIME_EMA_DECAY);
		pushTickLog(tickStartTime);
		profiler.pop();
	}

	protected void processPacketsAndTick(boolean sprint) {
		Profiler profiler = Profilers.get();
		profiler.push("tick");
		discontinuousFrame.start();
		profiler.push("scheduledPacketProcessing");
		packetApplyBatcher.apply();
		profiler.pop();
		tick(sprint ? () -> false : this::shouldKeepTicking);
		discontinuousFrame.end();
		profiler.pop();
	}

	private void runAutosave() {
		ticksUntilAutosave = getAutosaveInterval();
		LOGGER.debug("Autosave started");
		Profiler profiler = Profilers.get();
		profiler.push("save");
		saveAll(true, false, false);
		profiler.pop();
		LOGGER.debug("Autosave finished");
	}

	private void pushTickLog(long tickStartTime) {
		if (shouldPushTickTimeLog()) {
			getDebugSampleLog().push(
					Util.getMeasuringTimeNano() - tickStartTime,
					ServerTickType.TICK_SERVER_METHOD.ordinal()
			);
		}
	}

	private int getAutosaveInterval() {
		float tickRate;
		if (tickManager.isSprinting()) {
			long avgNanos = getAverageNanosPerTick() + 1L;
			tickRate = (float) TimeHelper.SECOND_IN_NANOS / (float) avgNanos;
		} else {
			tickRate = tickManager.getTickRate();
		}

		return Math.max(100, (int) (tickRate * 300.0F));
	}

	public void updateAutosaveTicks() {
		int interval = getAutosaveInterval();
		if (interval < ticksUntilAutosave) {
			ticksUntilAutosave = interval;
		}
	}

	protected abstract DebugSampleLog getDebugSampleLog();

	public abstract boolean shouldPushTickTimeLog();

	private ServerMetadata createMetadata() {
		return new ServerMetadata(
				Text.of(getServerMotd()),
				Optional.of(createMetadataPlayers()),
				Optional.of(ServerMetadata.Version.create()),
				Optional.ofNullable(favicon),
				shouldEnforceSecureProfile()
		);
	}

	private ServerMetadata.Players createMetadataPlayers() {
		List<ServerPlayerEntity> players = playerManager.getPlayerList();
		int maxPlayers = getMaxPlayerCount();

		if (hideOnlinePlayers()) {
			return new ServerMetadata.Players(maxPlayers, players.size(), List.of());
		}

		int sampleCount = Math.min(players.size(), MAX_PLAYER_SAMPLE_COUNT);
		ObjectArrayList<PlayerConfigEntry> sample = new ObjectArrayList<>(sampleCount);
		int startIndex = MathHelper.nextInt(random, 0, players.size() - sampleCount);

		for (int i = 0; i < sampleCount; i++) {
			ServerPlayerEntity player = players.get(startIndex + i);
			sample.add(player.allowsServerListing() ? player.getPlayerConfigEntry() : ANONYMOUS_PLAYER_PROFILE);
		}

		Util.shuffle(sample, random);
		return new ServerMetadata.Players(maxPlayers, players.size(), sample);
	}

	/**
	 * Выполняет тик всех игровых миров, обновляет сетевое I/O, позиции игроков
	 * и отправляет пакеты чанков. Является центральным методом игрового цикла.
	 *
	 * @param shouldKeepTicking условие продолжения обработки задач
	 */
	protected void tickWorlds(BooleanSupplier shouldKeepTicking) {
		Profiler profiler = Profilers.get();
		getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.disableFlush());
		profiler.push("commandFunctions");
		getCommandFunctionManager().tick();
		profiler.swap("levels");
		refreshSpawnPoint();

		for (ServerWorld world : getWorlds()) {
			profiler.push(() -> world + " " + world.getRegistryKey().getValue());
			if (ticks % TICKS_PER_SECOND == 0) {
				profiler.push("timeSync");
				sendTimeUpdatePackets(world);
				profiler.pop();
			}

			profiler.push("tick");

			try {
				world.tick(shouldKeepTicking);
			} catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "Exception ticking world");
				world.addDetailsToCrashReport(crashReport);
				throw new CrashException(crashReport);
			}

			profiler.pop();
			profiler.pop();
		}

		profiler.swap("connection");
		tickNetworkIo();
		profiler.swap("players");
		playerManager.updatePlayerLatency();
		profiler.swap("debugSubscribers");
		subscriberTracker.tick();

		if (tickManager.shouldTick()) {
			profiler.swap("gameTests");
			TestManager.INSTANCE.tick();
		}

		profiler.swap("server gui refresh");

		for (Runnable tickable : serverGuiTickables) {
			tickable.run();
		}

		profiler.swap("send chunks");

		for (ServerPlayerEntity player : playerManager.getPlayerList()) {
			player.networkHandler.chunkDataSender.sendChunkBatches(player);
			player.networkHandler.enableFlush();
		}

		profiler.pop();
		activityNotifier.notifyListeners();
	}

	private void refreshSpawnPoint() {
		WorldProperties.SpawnPoint currentSpawnPoint = saveProperties.getMainWorldProperties().getSpawnPoint();
		ServerWorld spawnWorld = getSpawnWorld();
		spawnPoint = spawnWorld.ensureWithinBorder(currentSpawnPoint);
	}

	public void tickNetworkIo() {
		getNetworkIo().tick();
	}

	private void sendTimeUpdatePackets(ServerWorld world) {
		playerManager.sendToDimension(
				new WorldTimeUpdateS2CPacket(
						world.getTime(),
						world.getTimeOfDay(),
						world.getGameRules().getValue(GameRules.ADVANCE_TIME)
				),
				world.getRegistryKey()
		);
	}

	public void sendTimeUpdatePackets() {
		Profiler profiler = Profilers.get();
		profiler.push("timeSync");

		for (ServerWorld world : getWorlds()) {
			sendTimeUpdatePackets(world);
		}

		profiler.pop();
	}

	public void addServerGuiTickable(Runnable tickable) {
		serverGuiTickables.add(tickable);
	}

	protected void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public boolean isStopping() {
		return !serverThread.isAlive();
	}

	public Path getPath(String path) {
		return getRunDirectory().resolve(path);
	}

	public final ServerWorld getOverworld() {
		return worlds.get(World.OVERWORLD);
	}

	public @Nullable ServerWorld getWorld(RegistryKey<World> key) {
		return worlds.get(key);
	}

	public Set<RegistryKey<World>> getWorldRegistryKeys() {
		return worlds.keySet();
	}

	public Iterable<ServerWorld> getWorlds() {
		return worlds.values();
	}

	@Override
	public String getVersion() {
		return SharedConstants.getGameVersion().name();
	}

	@Override
	public int getCurrentPlayerCount() {
		return playerManager.getCurrentPlayerCount();
	}

	public String[] getPlayerNames() {
		return playerManager.getPlayerNames();
	}

	@DontObfuscate
	public String getServerModName() {
		return VANILLA;
	}

	/**
	 * Добавляет системную информацию в секцию crash-report: состояние сервера,
	 * количество игроков, активные датапаки, включённые фичи и seed мира.
	 *
	 * @param details секция системных деталей crash-report
	 * @return заполненная секция деталей
	 */
	public SystemDetails addSystemDetails(SystemDetails details) {
		details.addSection("Server Running", () -> Boolean.toString(running));

		if (playerManager != null) {
			details.addSection(
					"Player Count",
					() -> playerManager.getCurrentPlayerCount() + " / " + playerManager.getMaxPlayerCount()
							+ "; " + playerManager.getPlayerList()
			);
		}

		details.addSection(
				"Active Data Packs",
				() -> ResourcePackManager.listPacks(dataPackManager.getEnabledProfiles())
		);
		details.addSection(
				"Available Data Packs",
				() -> ResourcePackManager.listPacks(dataPackManager.getProfiles())
		);
		details.addSection(
				"Enabled Feature Flags",
				() -> FeatureFlags.FEATURE_MANAGER
						.toId(saveProperties.getEnabledFeatures())
						.stream()
						.map(Identifier::toString)
						.collect(Collectors.joining(", "))
		);
		details.addSection("World Generation", () -> saveProperties.getLifecycle().toString());
		details.addSection("World Seed", () -> String.valueOf(saveProperties.getGeneratorOptions().getSeed()));
		details.addSection("Suppressed Exceptions", suppressedExceptionsTracker::collect);

		if (serverId != null) {
			details.addSection("Server Id", () -> serverId);
		}

		return addExtraSystemDetails(details);
	}

	public abstract SystemDetails addExtraSystemDetails(SystemDetails details);

	public ModStatus getModStatus() {
		return ModStatus.check("vanilla", this::getServerModName, "Server", MinecraftServer.class);
	}

	@Override
	public void sendMessage(Text message) {
		LOGGER.info(message.getString());
	}

	public KeyPair getKeyPair() {
		return Objects.requireNonNull(keyPair);
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public @Nullable GameProfile getHostProfile() {
		return hostProfile;
	}

	public void setHostProfile(@Nullable GameProfile hostProfile) {
		this.hostProfile = hostProfile;
	}

	public boolean isSingleplayer() {
		return hostProfile != null;
	}

	protected void generateKeyPair() {
		LOGGER.info("Generating keypair");

		try {
			keyPair = NetworkEncryptionUtils.generateServerKeyPair();
		} catch (NetworkEncryptionException e) {
			throw new IllegalStateException("Failed to generate key pair", e);
		}
	}

	public void setDifficulty(Difficulty difficulty, boolean forceUpdate) {
		if (forceUpdate || !saveProperties.isDifficultyLocked()) {
			saveProperties.setDifficulty(saveProperties.isHardcore() ? Difficulty.HARD : difficulty);
			updateMobSpawnOptions();
			getPlayerManager().getPlayerList().forEach(this::sendDifficulty);
		}
	}

	public int adjustTrackingDistance(int initialDistance) {
		return initialDistance;
	}

	public void updateMobSpawnOptions() {
		for (ServerWorld world : getWorlds()) {
			world.setMobSpawnOptions(world.shouldSpawnMonsters());
		}
	}

	public void setDifficultyLocked(boolean locked) {
		saveProperties.setDifficultyLocked(locked);
		getPlayerManager().getPlayerList().forEach(this::sendDifficulty);
	}

	private void sendDifficulty(ServerPlayerEntity player) {
		WorldProperties worldProperties = player.getEntityWorld().getLevelProperties();
		player.networkHandler.sendPacket(new DifficultyS2CPacket(
				worldProperties.getDifficulty(),
				worldProperties.isDifficultyLocked()
		));
	}

	public boolean isDemo() {
		return demo;
	}

	public void setDemo(boolean demo) {
		this.demo = demo;
	}

	public Map<String, String> getCodeOfConductLanguages() {
		return Map.of();
	}

	public Optional<MinecraftServer.ServerResourcePackProperties> getResourcePackProperties() {
		return Optional.empty();
	}

	public boolean requireResourcePack() {
		return getResourcePackProperties()
				.filter(MinecraftServer.ServerResourcePackProperties::isRequired)
				.isPresent();
	}

	public abstract boolean isDedicated();

	public abstract int getRateLimit();

	public boolean isOnlineMode() {
		return onlineMode;
	}

	public void setOnlineMode(boolean onlineMode) {
		this.onlineMode = onlineMode;
	}

	public boolean shouldPreventProxyConnections() {
		return preventProxyConnections;
	}

	public void setPreventProxyConnections(boolean preventProxyConnections) {
		this.preventProxyConnections = preventProxyConnections;
	}

	public abstract boolean isUsingNativeTransport();

	public boolean isFlightEnabled() {
		return true;
	}

	@Override
	public String getServerMotd() {
		return motd;
	}

	public void setMotd(String motd) {
		this.motd = motd;
	}

	public boolean isStopped() {
		return stopped;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public void setPlayerManager(PlayerManager playerManager) {
		this.playerManager = playerManager;
	}

	public abstract boolean isRemote();

	public void setDefaultGameMode(GameMode gameMode) {
		saveProperties.setGameMode(gameMode);
	}

	public int changeGameModeGlobally(@Nullable GameMode gameMode) {
		if (gameMode == null) {
			return 0;
		}

		int changed = 0;

		for (ServerPlayerEntity player : getPlayerManager().getPlayerList()) {
			if (player.changeGameMode(gameMode)) {
				changed++;
			}
		}

		return changed;
	}

	public ServerNetworkIo getNetworkIo() {
		return networkIo;
	}

	public boolean isLoading() {
		return loading;
	}

	public boolean openToLan(@Nullable GameMode gameMode, boolean cheatsAllowed, int port) {
		return false;
	}

	public int getTicks() {
		return ticks;
	}

	public boolean isSpawnProtected(ServerWorld world, BlockPos pos, PlayerEntity player) {
		return false;
	}

	public boolean acceptsStatusQuery() {
		return true;
	}

	public boolean hideOnlinePlayers() {
		return false;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public int getPlayerIdleTimeout() {
		return playerIdleTimeout;
	}

	public void setPlayerIdleTimeout(int playerIdleTimeout) {
		this.playerIdleTimeout = playerIdleTimeout;
	}

	public ApiServices getApiServices() {
		return apiServices;
	}

	public @Nullable ServerMetadata getServerMetadata() {
		return metadata;
	}

	public void forcePlayerSampleUpdate() {
		lastPlayerSampleUpdate = 0L;
	}

	public int getMaxWorldBorderRadius() {
		return MAX_WORLD_BORDER_RADIUS;
	}

	@Override
	public boolean shouldExecuteAsync() {
		return super.shouldExecuteAsync() && !isStopped();
	}

	@Override
	public void executeSync(Runnable runnable) {
		if (isStopped()) {
			throw new RejectedExecutionException("Server already shutting down");
		}

		super.executeSync(runnable);
	}

	@Override
	public Thread getThread() {
		return serverThread;
	}

	public int getNetworkCompressionThreshold() {
		return NETWORK_COMPRESSION_THRESHOLD;
	}

	public boolean shouldEnforceSecureProfile() {
		return false;
	}

	public long getTimeReference() {
		return tickStartTimeNanos;
	}

	public DataFixer getDataFixer() {
		return dataFixer;
	}

	public ServerAdvancementLoader getAdvancementLoader() {
		return resourceManagerHolder.dataPackContents.getServerAdvancementLoader();
	}

	public CommandFunctionManager getCommandFunctionManager() {
		return commandFunctionManager;
	}

	/**
	 * Перезагружает ресурсы сервера с новым набором датапаков.
	 * Создаёт новый {@link LifecycledResourceManager}, перезагружает теги, рецепты,
	 * команды и функции, затем уведомляет игроков об изменениях.
	 *
	 * @param dataPacks набор идентификаторов датапаков для активации
	 * @return future, завершающийся после применения всех изменений на серверном потоке
	 */
	public CompletableFuture<Void> reloadResources(Collection<String> dataPacks) {
		CompletableFuture<Void> completableFuture = CompletableFuture
				.<ImmutableList>supplyAsync(
						() -> dataPacks.stream()
								.map(dataPackManager::getProfile)
								.filter(Objects::nonNull)
								.map(ResourcePackProfile::createResourcePack)
								.collect(ImmutableList.toImmutableList()),
						this
				)
				.thenCompose(resourcePacks -> {
					LifecycledResourceManager lifecycledResourceManager = new LifecycledResourceManagerImpl(
							ResourceType.SERVER_DATA,
							resourcePacks
					);
					List<Registry.PendingTagLoad<?>> pendingTagLoads = TagGroupLoader.startReload(
							lifecycledResourceManager,
							combinedDynamicRegistries.getCombinedRegistryManager()
					);
					return DataPackContents.reload(
									lifecycledResourceManager,
									combinedDynamicRegistries,
									pendingTagLoads,
									saveProperties.getEnabledFeatures(),
									isDedicated()
											? CommandManager.RegistrationEnvironment.DEDICATED
											: CommandManager.RegistrationEnvironment.INTEGRATED,
									getFunctionPermissions(),
									workerExecutor,
									this
							)
							.whenComplete((dataPackContents, throwable) -> {
								if (throwable != null) {
									lifecycledResourceManager.close();
								}
							})
							.thenApply(dataPackContents -> new MinecraftServer.ResourceManagerHolder(
									lifecycledResourceManager,
									dataPackContents
							));
				})
				.thenAcceptAsync(newHolder -> {
					resourceManagerHolder.close();
					resourceManagerHolder = newHolder;
					dataPackManager.setEnabledProfiles(dataPacks);
					DataConfiguration dataConfiguration = new DataConfiguration(
							createDataPackSettings(dataPackManager, true),
							saveProperties.getEnabledFeatures()
					);
					saveProperties.updateLevelInfo(dataConfiguration);
					resourceManagerHolder.dataPackContents.applyPendingTagLoads();
					resourceManagerHolder.dataPackContents
							.getRecipeManager()
							.initialize(saveProperties.getEnabledFeatures());
					getPlayerManager().saveAllPlayerData();
					getPlayerManager().onDataPacksReloaded();
					commandFunctionManager.setFunctions(resourceManagerHolder.dataPackContents.getFunctionLoader());
					structureTemplateManager.setResourceManager(resourceManagerHolder.resourceManager);
					fuelRegistry = FuelRegistry.createDefault(
							combinedDynamicRegistries.getCombinedRegistryManager(),
							saveProperties.getEnabledFeatures()
					);
				}, this);

		if (isOnThread()) {
			runTasks(completableFuture::isDone);
		}

		return completableFuture;
	}

	public static DataConfiguration loadDataPacks(
			ResourcePackManager resourcePackManager,
			DataConfiguration dataConfiguration,
			boolean initMode,
			boolean safeMode
	) {
		DataPackSettings dataPackSettings = dataConfiguration.dataPacks();
		FeatureSet minFeatures = initMode ? FeatureSet.empty() : dataConfiguration.enabledFeatures();
		FeatureSet maxFeatures = initMode
				? FeatureFlags.FEATURE_MANAGER.getFeatureSet()
				: dataConfiguration.enabledFeatures();
		resourcePackManager.scanPacks();

		if (safeMode) {
			return loadDataPacks(resourcePackManager, List.of("vanilla"), minFeatures, false);
		}

		Set<String> enabledPacks = Sets.newLinkedHashSet();

		for (String packId : dataPackSettings.getEnabled()) {
			if (resourcePackManager.hasProfile(packId)) {
				enabledPacks.add(packId);
			} else {
				LOGGER.warn("Missing data pack {}", packId);
			}
		}

		for (ResourcePackProfile profile : resourcePackManager.getProfiles()) {
			String packId = profile.getId();
			if (dataPackSettings.getDisabled().contains(packId)) {
				continue;
			}

			FeatureSet requiredFeatures = profile.getRequestedFeatures();
			boolean isEnabled = enabledPacks.contains(packId);

			if (!isEnabled && profile.getSource().canBeEnabledLater()) {
				if (requiredFeatures.isSubsetOf(maxFeatures)) {
					LOGGER.info("Found new data pack {}, loading it automatically", packId);
					enabledPacks.add(packId);
				} else {
					LOGGER.info(
							"Found new data pack {}, but can't load it due to missing features {}",
							packId,
							FeatureFlags.printMissingFlags(maxFeatures, requiredFeatures)
					);
				}
			}

			if (isEnabled && !requiredFeatures.isSubsetOf(maxFeatures)) {
				LOGGER.warn(
						"Pack {} requires features {} that are not enabled for this world, disabling pack.",
						packId,
						FeatureFlags.printMissingFlags(maxFeatures, requiredFeatures)
				);
				enabledPacks.remove(packId);
			}
		}

		if (enabledPacks.isEmpty()) {
			LOGGER.info("No datapacks selected, forcing vanilla");
			enabledPacks.add("vanilla");
		}

		return loadDataPacks(resourcePackManager, enabledPacks, minFeatures, true);
	}

	private static DataConfiguration loadDataPacks(
			ResourcePackManager resourcePackManager,
			Collection<String> enabledProfiles,
			FeatureSet enabledFeatures,
			boolean allowEnabling
	) {
		resourcePackManager.setEnabledProfiles(enabledProfiles);
		forceEnableRequestedFeatures(resourcePackManager, enabledFeatures);
		DataPackSettings dataPackSettings = createDataPackSettings(resourcePackManager, allowEnabling);
		FeatureSet combinedFeatures = resourcePackManager.getRequestedFeatures().combine(enabledFeatures);
		return new DataConfiguration(dataPackSettings, combinedFeatures);
	}

	private static void forceEnableRequestedFeatures(
			ResourcePackManager resourcePackManager,
			FeatureSet enabledFeatures
	) {
		FeatureSet requestedFeatures = resourcePackManager.getRequestedFeatures();
		FeatureSet missingFeatures = enabledFeatures.subtract(requestedFeatures);

		if (missingFeatures.isEmpty()) {
			return;
		}

		Set<String> enabledIds = new ObjectArraySet<>(resourcePackManager.getEnabledIds());

		for (ResourcePackProfile profile : resourcePackManager.getProfiles()) {
			if (missingFeatures.isEmpty()) {
				break;
			}

			if (profile.getSource() != ResourcePackSource.FEATURE) {
				continue;
			}

			String packId = profile.getId();
			FeatureSet packFeatures = profile.getRequestedFeatures();

			if (packFeatures.isEmpty() || !packFeatures.intersects(missingFeatures) || !packFeatures.isSubsetOf(enabledFeatures)) {
				continue;
			}

			if (!enabledIds.add(packId)) {
				throw new IllegalStateException("Tried to force '" + packId + "', but it was already enabled");
			}

			LOGGER.info("Found feature pack ('{}') for requested feature, forcing to enabled", packId);
			missingFeatures = missingFeatures.subtract(packFeatures);
		}

		resourcePackManager.setEnabledProfiles(enabledIds);
	}

	private static DataPackSettings createDataPackSettings(ResourcePackManager dataPackManager, boolean allowEnabling) {
		Collection<String> enabledIds = dataPackManager.getEnabledIds();
		List<String> enabled = ImmutableList.copyOf(enabledIds);
		List<String> disabled = allowEnabling
				? dataPackManager.getIds().stream().filter(id -> !enabledIds.contains(id)).toList()
				: List.of();
		return new DataPackSettings(enabled, disabled);
	}

	public void kickNonWhitelistedPlayers() {
		if (!isEnforceWhitelist() || !getUseAllowlist()) {
			return;
		}

		PlayerManager manager = getPlayerManager();
		Whitelist whitelist = manager.getWhitelist();

		for (ServerPlayerEntity player : Lists.newArrayList(manager.getPlayerList())) {
			if (!whitelist.isAllowed(player.getPlayerConfigEntry())) {
				player.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.not_whitelisted"));
			}
		}
	}

	public ResourcePackManager getDataPackManager() {
		return dataPackManager;
	}

	public CommandManager getCommandManager() {
		return resourceManagerHolder.dataPackContents.getCommandManager();
	}

	public ServerCommandSource getCommandSource() {
		ServerWorld spawnWorld = getSpawnWorld();
		return new ServerCommandSource(
				this,
				Vec3d.of(getSpawnPoint().getPos()),
				Vec2f.ZERO,
				spawnWorld,
				LeveledPermissionPredicate.OWNERS,
				"Server",
				Text.literal("Server"),
				this,
				null
		);
	}

	public ServerWorld getSpawnWorld() {
		WorldProperties.SpawnPoint currentSpawnPoint = getSaveProperties().getMainWorldProperties().getSpawnPoint();
		RegistryKey<World> dimension = currentSpawnPoint.getDimension();
		ServerWorld world = getWorld(dimension);
		return world != null ? world : getOverworld();
	}

	public void setSpawnPoint(WorldProperties.SpawnPoint newSpawnPoint) {
		ServerWorldProperties serverWorldProperties = saveProperties.getMainWorldProperties();
		WorldProperties.SpawnPoint currentSpawnPoint = serverWorldProperties.getSpawnPoint();

		if (currentSpawnPoint.equals(newSpawnPoint)) {
			return;
		}

		serverWorldProperties.setSpawnPoint(newSpawnPoint);
		getPlayerManager().sendToAll(new PlayerSpawnPositionS2CPacket(newSpawnPoint));
		refreshSpawnPoint();
	}

	public WorldProperties.SpawnPoint getSpawnPoint() {
		return spawnPoint;
	}

	@Override
	public boolean shouldReceiveFeedback() {
		return true;
	}

	@Override
	public boolean shouldTrackOutput() {
		return true;
	}

	@Override
	public abstract boolean shouldBroadcastConsoleToOps();

	public ServerRecipeManager getRecipeManager() {
		return resourceManagerHolder.dataPackContents.getRecipeManager();
	}

	public ServerScoreboard getScoreboard() {
		return scoreboard;
	}

	public DataCommandStorage getDataCommandStorage() {
		if (dataCommandStorage == null) {
			throw new NullPointerException("Called before server init");
		}

		return dataCommandStorage;
	}

	public StopwatchPersistentState getStopwatchPersistentState() {
		if (stopwatchPersistentState == null) {
			throw new NullPointerException("Called before server init");
		}

		return stopwatchPersistentState;
	}

	public BossBarManager getBossBarManager() {
		return bossBarManager;
	}

	public boolean isEnforceWhitelist() {
		return enforceWhitelist;
	}

	public void setEnforceWhitelist(boolean enforceWhitelist) {
		this.enforceWhitelist = enforceWhitelist;
	}

	public boolean getUseAllowlist() {
		return useAllowlist;
	}

	public void setUseAllowlist(boolean useAllowlist) {
		this.useAllowlist = useAllowlist;
	}

	public float getAverageTickTime() {
		return averageTickTime;
	}

	public ServerTickManager getTickManager() {
		return tickManager;
	}

	public long getAverageNanosPerTick() {
		return recentTickTimesNanos / Math.min(RECENT_TICK_TIMES_WINDOW, Math.max(ticks, 1));
	}

	public long[] getTickTimes() {
		return tickTimes;
	}

	public LeveledPermissionPredicate getPermissionLevel(PlayerConfigEntry player) {
		if (!getPlayerManager().isOperator(player)) {
			return LeveledPermissionPredicate.ALL;
		}

		OperatorEntry operatorEntry = getPlayerManager().getOpList().get(player);
		if (operatorEntry != null) {
			return operatorEntry.getLevel();
		}

		if (isHost(player)) {
			return LeveledPermissionPredicate.OWNERS;
		}

		if (isSingleplayer()) {
			return getPlayerManager().areCheatsAllowed()
					? LeveledPermissionPredicate.OWNERS
					: LeveledPermissionPredicate.ALL;
		}

		return getOpPermissionLevel();
	}

	public abstract boolean isHost(PlayerConfigEntry player);

	public void dumpProperties(Path file) throws IOException {
	}

	private void dump(Path path) {
		Path levelsPath = path.resolve("levels");

		try {
			for (Entry<RegistryKey<World>, ServerWorld> entry : worlds.entrySet()) {
				Identifier identifier = entry.getKey().getValue();
				Path worldPath = levelsPath.resolve(identifier.getNamespace()).resolve(identifier.getPath());
				Files.createDirectories(worldPath);
				entry.getValue().dump(worldPath);
			}

			dumpGamerules(path.resolve("gamerules.txt"));
			dumpClasspath(path.resolve("classpath.txt"));
			dumpStats(path.resolve("stats.txt"));
			dumpThreads(path.resolve("threads.txt"));
			dumpProperties(path.resolve("server.properties.txt"));
			dumpNativeModules(path.resolve("modules.txt"));
		} catch (IOException e) {
			LOGGER.warn("Failed to save debug report", e);
		}
	}

	private void dumpStats(Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path)) {
			writer.write(String.format(Locale.ROOT, "pending_tasks: %d\n", getTaskCount()));
			writer.write(String.format(Locale.ROOT, "average_tick_time: %f\n", getAverageTickTime()));
			writer.write(String.format(Locale.ROOT, "tick_times: %s\n", Arrays.toString(tickTimes)));
			writer.write(String.format(Locale.ROOT, "queue: %s\n", Util.getMainWorkerExecutor()));
		}
	}

	private void dumpGamerules(Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path)) {
			final List<String> lines = Lists.newArrayList();
			final GameRules gameRules = saveProperties.getGameRules();
			gameRules.accept(new GameRuleVisitor() {
				@Override
				public <T> void visit(GameRule<T> rule) {
					lines.add(String.format(Locale.ROOT, "%s=%s\n", rule.getId(), gameRules.getRuleValueName(rule)));
				}
			});

			for (String line : lines) {
				writer.write(line);
			}
		}
	}

	private void dumpClasspath(Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path)) {
			String classpath = System.getProperty("java.class.path");
			String separator = File.pathSeparator;

			for (String entry : Splitter.on(separator).split(classpath)) {
				writer.write(entry);
				writer.write("\n");
			}
		}
	}

	private void dumpThreads(Path path) throws IOException {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
		Arrays.sort(threadInfos, Comparator.comparing(ThreadInfo::getThreadName));

		try (Writer writer = Files.newBufferedWriter(path)) {
			for (ThreadInfo threadInfo : threadInfos) {
				writer.write(threadInfo.toString());
				writer.write('\n');
			}
		}
	}

	private void dumpNativeModules(Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path)) {
			List<WinNativeModuleUtil.NativeModule> modules;
			try {
				modules = Lists.newArrayList(WinNativeModuleUtil.collectNativeModules());
			} catch (Throwable e) {
				LOGGER.warn("Failed to list native modules", e);
				return;
			}

			modules.sort(Comparator.comparing(module -> module.path));

			for (WinNativeModuleUtil.NativeModule module : modules) {
				writer.write(module.toString());
				writer.write('\n');
			}
		}
	}

	private Profiler startTickMetrics() {
		if (needsRecorderSetup) {
			recorder = DebugRecorder.of(
					new ServerSamplerSource(Util.nanoTimeSupplier, isDedicated()),
					Util.nanoTimeSupplier,
					Util.getIoWorkerExecutor(),
					new RecordDumper("server"),
					recorderResultConsumer,
					path -> {
						submitAndJoin(() -> dump(path.resolve("server")));
						recorderDumpConsumer.accept(path);
					}
			);
			needsRecorderSetup = false;
		}

		recorder.startTick();
		return TickDurationMonitor.tickProfiler(recorder.getProfiler(), TickDurationMonitor.create("Server"));
	}

	public void endTickMetrics() {
		recorder.endTick();
	}

	public boolean isRecorderActive() {
		return recorder.isActive();
	}

	public void setupRecorder(Consumer<ProfileResult> resultConsumer, Consumer<Path> dumpConsumer) {
		recorderResultConsumer = result -> {
			resetRecorder();
			resultConsumer.accept(result);
		};
		recorderDumpConsumer = dumpConsumer;
		needsRecorderSetup = true;
	}

	public void resetRecorder() {
		recorder = DummyRecorder.INSTANCE;
	}

	public void stopRecorder() {
		recorder.stop();
	}

	public void forceStopRecorder() {
		recorder.forceStop();
	}

	public Path getSavePath(WorldSavePath worldSavePath) {
		return session.getDirectory(worldSavePath);
	}

	public boolean syncChunkWrites() {
		return true;
	}

	public StructureTemplateManager getStructureTemplateManager() {
		return structureTemplateManager;
	}

	public SaveProperties getSaveProperties() {
		return saveProperties;
	}

	public DynamicRegistryManager.Immutable getRegistryManager() {
		return combinedDynamicRegistries.getCombinedRegistryManager();
	}

	public CombinedDynamicRegistries<ServerDynamicRegistryType> getCombinedDynamicRegistries() {
		return combinedDynamicRegistries;
	}

	public ReloadableRegistries.Lookup getReloadableRegistries() {
		return resourceManagerHolder.dataPackContents.getReloadableRegistries();
	}

	public TextStream createFilterer(ServerPlayerEntity player) {
		return TextStream.UNFILTERED;
	}

	public ServerPlayerInteractionManager getPlayerInteractionManager(ServerPlayerEntity player) {
		return isDemo()
				? new DemoServerPlayerInteractionManager(player)
				: new ServerPlayerInteractionManager(player);
	}

	public @Nullable GameMode getForcedGameMode() {
		return null;
	}

	public ResourceManager getResourceManager() {
		return resourceManagerHolder.resourceManager;
	}

	public boolean isSaving() {
		return saving;
	}

	public boolean isDebugRunning() {
		return needsDebugSetup || debugStart != null;
	}

	public void startDebug() {
		needsDebugSetup = true;
	}

	public ProfileResult stopDebug() {
		if (debugStart == null) {
			return EmptyProfileResult.INSTANCE;
		}

		ProfileResult profileResult = debugStart.end(Util.getMeasuringTimeNano(), ticks);
		debugStart = null;
		return profileResult;
	}

	public int getMaxChainedNeighborUpdates() {
		return MAX_CHAINED_NEIGHBOR_UPDATES;
	}

	public void logChatMessage(Text message, MessageType.Parameters params, @Nullable String prefix) {
		String formatted = params.applyChatDecoration(message).getString();
		if (prefix != null) {
			LOGGER.info("[{}] {}", prefix, formatted);
		} else {
			LOGGER.info("{}", formatted);
		}
	}

	public MessageDecorator getMessageDecorator() {
		return MessageDecorator.NOOP;
	}

	public boolean shouldLogIps() {
		return true;
	}

	public void handleCustomClickAction(Identifier id, Optional<NbtElement> payload) {
		LOGGER.debug("Received custom click action {} with payload {}", id, payload.orElse(null));
	}

	public ChunkLoadProgress getChunkLoadProgress() {
		return chunkLoadProgress;
	}

	public boolean setAutosave(boolean autosave) {
		boolean changed = false;

		for (ServerWorld world : getWorlds()) {
			if (world != null && world.savingDisabled == autosave) {
				world.savingDisabled = !autosave;
				changed = true;
			}
		}

		return changed;
	}

	public boolean getAutosave() {
		for (ServerWorld world : getWorlds()) {
			if (world != null && !world.savingDisabled) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Обрабатывает обновление игрового правила: рассылает соответствующие пакеты игрокам.
	 * Поддерживает правила: REDUCED_DEBUG_INFO, LIMITED_CRAFTING, DO_IMMEDIATE_RESPAWN,
	 * LOCATOR_BAR, SPAWN_MONSTERS.
	 *
	 * @param gameRule обновлённое правило
	 * @param value    новое значение правила
	 */
	public <T> void onGameRuleUpdated(GameRule<T> gameRule, T value) {
		getManagementListener().onGameRuleUpdated(gameRule, value);

		if (gameRule == GameRules.REDUCED_DEBUG_INFO) {
			byte statusId = (byte) ((Boolean) value ? 22 : 23);

			for (ServerPlayerEntity player : getPlayerManager().getPlayerList()) {
				player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, statusId));
			}
		} else if (gameRule == GameRules.LIMITED_CRAFTING || gameRule == GameRules.DO_IMMEDIATE_RESPAWN) {
			GameStateChangeS2CPacket.Reason reason = gameRule == GameRules.LIMITED_CRAFTING
					? GameStateChangeS2CPacket.LIMITED_CRAFTING_TOGGLED
					: GameStateChangeS2CPacket.IMMEDIATE_RESPAWN;
			GameStateChangeS2CPacket packet = new GameStateChangeS2CPacket(reason, (Boolean) value ? 1.0F : 0.0F);
			getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.sendPacket(packet));
		} else if (gameRule == GameRules.LOCATOR_BAR) {
			getWorlds().forEach(world -> {
				ServerWaypointHandler waypointHandler = world.getWaypointHandler();
				if ((Boolean) value) {
					world.getPlayers().forEach(waypointHandler::updatePlayerPos);
				} else {
					waypointHandler.clear();
				}
			});
		} else if (gameRule == GameRules.SPAWN_MONSTERS) {
			updateMobSpawnOptions();
		}
	}

	public boolean acceptsTransfers() {
		return false;
	}

	private void writeChunkIoReport(CrashReport report, ChunkPos pos, StorageKey key) {
		Util.getIoWorkerExecutor().execute(() -> {
			try {
				Path debugPath = getPath("debug");
				PathUtil.createDirectories(debugPath);
				String levelName = PathUtil.replaceInvalidChars(key.level());
				Path reportPath = debugPath.resolve("chunk-" + levelName + "-" + Util.getFormattedCurrentTime() + "-server.txt");
				FileStore fileStore = Files.getFileStore(debugPath);
				long usableSpace = fileStore.getUsableSpace();

				if (usableSpace < MIN_CHUNK_REPORT_DISK_SPACE) {
					LOGGER.warn("Not storing chunk IO report due to low space on drive {}", fileStore.name());
					return;
				}

				CrashReportSection section = report.addElement("Chunk Info");
				section.add("Level", key::level);
				section.add("Dimension", () -> key.dimension().getValue().toString());
				section.add("Storage", key::type);
				section.add("Position", pos::toString);
				report.writeToFile(reportPath, ReportType.MINECRAFT_CHUNK_IO_ERROR_REPORT);
				LOGGER.info("Saved details to {}", report.getFile());
			} catch (Exception e) {
				LOGGER.warn("Failed to store chunk IO exception", e);
			}
		});
	}

	@Override
	public void onChunkLoadFailure(Throwable exception, StorageKey key, ChunkPos chunkPos) {
		LOGGER.error("Failed to load chunk {},{}", new Object[]{chunkPos.x, chunkPos.z, exception});
		suppressedExceptionsTracker.onSuppressedException("chunk/load", exception);
		writeChunkIoReport(CrashReport.create(exception, "Chunk load failure"), chunkPos, key);
	}

	@Override
	public void onChunkSaveFailure(Throwable exception, StorageKey key, ChunkPos chunkPos) {
		LOGGER.error("Failed to save chunk {},{}", new Object[]{chunkPos.x, chunkPos.z, exception});
		suppressedExceptionsTracker.onSuppressedException("chunk/save", exception);
		writeChunkIoReport(CrashReport.create(exception, "Chunk save failure"), chunkPos, key);
	}

	public void onPacketException(Throwable exception, PacketType<?> type) {
		suppressedExceptionsTracker.onSuppressedException("packet/" + type, exception);
	}

	public BrewingRecipeRegistry getBrewingRecipeRegistry() {
		return brewingRecipeRegistry;
	}

	public FuelRegistry getFuelRegistry() {
		return fuelRegistry;
	}

	public ServerLinks getServerLinks() {
		return ServerLinks.EMPTY;
	}

	protected int getPauseWhenEmptySeconds() {
		return 0;
	}

	public PacketApplyBatcher getPacketApplyBatcher() {
		return packetApplyBatcher;
	}

	public SubscriberTracker getSubscriberTracker() {
		return subscriberTracker;
	}

	@Override
	public <T> T getOrThrow(DataResourceStore.Key<T> key) {
		return dataResourceStore.getOrThrow(key);
	}

	static class DebugStart {

		final long time;
		final int tick;

		DebugStart(long time, int tick) {
			this.time = time;
			this.tick = tick;
		}

		ProfileResult end(long endTime, int endTick) {
			return new ProfileResult() {
				@Override
				public List<ProfilerTiming> getTimings(String parentPath) {
					return Collections.emptyList();
				}

				@Override
				public boolean save(Path path) {
					return false;
				}

				@Override
				public long getStartTime() {
					return DebugStart.this.time;
				}

				@Override
				public int getStartTick() {
					return DebugStart.this.tick;
				}

				@Override
				public long getEndTime() {
					return endTime;
				}

				@Override
				public int getEndTick() {
					return endTick;
				}

				@Override
				public String getRootTimings() {
					return "";
				}
			};
		}
	}

	record ResourceManagerHolder(
			LifecycledResourceManager resourceManager,
			DataPackContents dataPackContents
	) implements AutoCloseable {

		@Override
		public void close() {
			resourceManager.close();
		}
	}

	public record ServerResourcePackProperties(
			UUID id,
			String url,
			String hash,
			boolean isRequired,
			@Nullable Text prompt
	) {
	}
}
