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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.network.packet.s2c.play.DifficultyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
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
import net.minecraft.server.network.DemoServerPlayerInteractionManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.network.ServerWaypointHandler;
import net.minecraft.server.network.SpawnLocating;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.test.TestManager;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Identifier;
import net.minecraft.util.ModStatus;
import net.minecraft.util.PngMetadata;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.TickDurationMonitor;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.Util;
import net.minecraft.util.WinNativeModuleUtil;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.ReportType;
import net.minecraft.util.crash.SuppressedExceptionsTracker;
import net.minecraft.util.function.Finishable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.profiler.DebugRecorder;
import net.minecraft.util.profiler.DummyRecorder;
import net.minecraft.util.profiler.EmptyProfileResult;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerTiming;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.RecordDumper;
import net.minecraft.util.profiler.Recorder;
import net.minecraft.util.profiler.ServerSamplerSource;
import net.minecraft.util.profiler.ServerTickType;
import net.minecraft.util.profiler.log.DebugSampleLog;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.util.profiling.jfr.InstanceType;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.village.ZombieSiegeManager;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.WanderingTraderManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
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

public abstract class MinecraftServer
   extends ReentrantThreadExecutor<ServerTask>
   implements QueryableServer,
   CommandOutput,
   ChunkErrorHandler,
   DataResourceStore {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String VANILLA = "vanilla";
   private static final float field_33212 = 0.8F;
   private static final int field_33213 = 100;
   private static final long OVERLOAD_THRESHOLD_NANOS = 20L * TimeHelper.SECOND_IN_NANOS / 20L;
   private static final int field_47144 = 20;
   private static final long OVERLOAD_WARNING_INTERVAL_NANOS = 10L * TimeHelper.SECOND_IN_NANOS;
   private static final int field_47146 = 100;
   private static final long PLAYER_SAMPLE_UPDATE_INTERVAL_NANOS = 5L * TimeHelper.SECOND_IN_NANOS;
   private static final long PREPARE_START_REGION_TICK_DELAY_NANOS = 10L * TimeHelper.MILLI_IN_NANOS;
   private static final int field_33218 = 12;
   public static final int field_48466 = 5;
   private static final int field_63650 = 30;
   private static final int field_33220 = 6000;
   private static final int field_47149 = 100;
   private static final int field_33221 = 3;
   public static final int MAX_WORLD_BORDER_RADIUS = 29999984;
   public static final LevelInfo DEMO_LEVEL_INFO = new LevelInfo(
      "Demo World", GameMode.SURVIVAL, false, Difficulty.NORMAL, false, new GameRules(FeatureFlags.DEFAULT_ENABLED_FEATURES), DataConfiguration.SAFE_MODE
   );
   public static final PlayerConfigEntry ANONYMOUS_PLAYER_PROFILE = new PlayerConfigEntry(Util.NIL_UUID, "Anonymous Player");
   protected final LevelStorage.Session session;
   protected final PlayerSaveHandler saveHandler;
   private final List<Runnable> serverGuiTickables = Lists.newArrayList();
   private Recorder recorder = DummyRecorder.INSTANCE;
   private Consumer<ProfileResult> recorderResultConsumer = profileResult -> this.resetRecorder();
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
   private int ticksUntilAutosave = 6000;
   protected final Proxy proxy;
   private boolean onlineMode;
   private boolean preventProxyConnections;
   private @Nullable String motd;
   private int playerIdleTimeout;
   private final long[] tickTimes = new long[100];
   private long recentTickTimesNanos = 0L;
   private @Nullable KeyPair keyPair;
   private @Nullable GameProfile hostProfile;
   private boolean demo;
   private volatile boolean loading;
   private long lastOverloadWarningNanos;
   protected final ApiServices apiServices;
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
   protected final SaveProperties saveProperties;
   private WorldProperties.SpawnPoint spawnPoint = WorldProperties.SpawnPoint.DEFAULT;
   private final BrewingRecipeRegistry brewingRecipeRegistry;
   private FuelRegistry fuelRegistry;
   private int idleTickCount;
   private volatile boolean saving;
   private static final AtomicReference<@Nullable RuntimeException> WORLD_GEN_EXCEPTION = new AtomicReference<>();
   private final SuppressedExceptionsTracker suppressedExceptionsTracker = new SuppressedExceptionsTracker();
   private final DiscontinuousFrame discontinuousFrame;
   private final PacketApplyBatcher packetApplyBatcher;
   private final DataResourceStoreImpl dataResourceStore = new DataResourceStoreImpl();

   public static <S extends MinecraftServer> S startServer(Function<Thread, S> serverFactory) {
      AtomicReference<S> atomicReference = new AtomicReference<>();
      Thread thread = new Thread(() -> atomicReference.get().runServer(), "Server thread");
      thread.setUncaughtExceptionHandler((threadx, throwable) -> LOGGER.error("Uncaught exception in server thread", throwable));
      if (Runtime.getRuntime().availableProcessors() > 4) {
         thread.setPriority(8);
      }

      S minecraftServer = (S)serverFactory.apply(thread);
      atomicReference.set(minecraftServer);
      thread.start();
      return minecraftServer;
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
      if (!this.combinedDynamicRegistries.getCombinedRegistryManager().getOrThrow(RegistryKeys.DIMENSION).contains(DimensionOptions.OVERWORLD)) {
         throw new IllegalStateException("Missing Overworld dimension data");
      } else {
         this.proxy = proxy;
         this.dataPackManager = dataPackManager;
         this.resourceManagerHolder = new MinecraftServer.ResourceManagerHolder(saveLoader.resourceManager(), saveLoader.dataPackContents());
         this.apiServices = apiServices;
         this.networkIo = new ServerNetworkIo(this);
         this.tickManager = new ServerTickManager(this);
         this.chunkLoadProgress = chunkLoadProgress;
         this.session = session;
         this.saveHandler = session.createSaveHandler();
         this.dataFixer = dataFixer;
         this.commandFunctionManager = new CommandFunctionManager(this, this.resourceManagerHolder.dataPackContents.getFunctionLoader());
         RegistryEntryLookup<Block> registryEntryLookup = this.combinedDynamicRegistries
            .getCombinedRegistryManager()
            .getOrThrow(RegistryKeys.BLOCK)
            .withFeatureFilter(this.saveProperties.getEnabledFeatures());
         this.structureTemplateManager = new StructureTemplateManager(saveLoader.resourceManager(), session, dataFixer, registryEntryLookup);
         this.serverThread = serverThread;
         this.workerExecutor = Util.getMainWorkerExecutor();
         this.brewingRecipeRegistry = BrewingRecipeRegistry.create(this.saveProperties.getEnabledFeatures());
         this.resourceManagerHolder.dataPackContents.getRecipeManager().initialize(this.saveProperties.getEnabledFeatures());
         this.fuelRegistry = FuelRegistry.createDefault(this.combinedDynamicRegistries.getCombinedRegistryManager(), this.saveProperties.getEnabledFeatures());
         this.discontinuousFrame = TracyClient.createDiscontinuousFrame("Server Tick");
         this.managementListener = new CompositeManagementListener();
         this.activityNotifier = new ActivityNotifier(this.managementListener, 30);
         this.packetApplyBatcher = new PacketApplyBatcher(serverThread);
      }
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
            this.chunkLoadingManager = serverWorld != null ? serverWorld.getChunkManager().chunkLoadingManager : null;
            this.spawnChunkX = spawnPos.x;
            this.spawnChunkZ = spawnPos.z;
         }

         @Override
         public @Nullable ChunkStatus getStatus(int x, int z) {
            return this.chunkLoadingManager == null
               ? null
               : this.chunkLoadingManager.getStatus(ChunkPos.toLong(x + this.spawnChunkX - radius, z + this.spawnChunkZ - radius));
         }

         @Override
         public int getRadius() {
            return radius;
         }
      };
   }

   protected void loadWorld() {
      boolean bl = !FlightProfiler.INSTANCE.isProfiling()
         && SharedConstants.JFR_PROFILING_ENABLE_LEVEL_LOADING
         && FlightProfiler.INSTANCE.start(InstanceType.get(this));
      Finishable finishable = FlightProfiler.INSTANCE.startWorldLoadProfiling();
      this.saveProperties.addServerBrand(this.getServerModName(), this.getModStatus().isModded());
      this.createWorlds();
      this.updateDifficulty();
      this.prepareStartRegion();
      if (finishable != null) {
         finishable.finish(true);
      }

      if (bl) {
         try {
            FlightProfiler.INSTANCE.stop();
         } catch (Throwable var4) {
            LOGGER.warn("Failed to stop JFR profiling", var4);
         }
      }
   }

   protected void updateDifficulty() {
   }

   protected void createWorlds() {
      ServerWorldProperties serverWorldProperties = this.saveProperties.getMainWorldProperties();
      boolean bl = this.saveProperties.isDebugWorld();
      Registry<DimensionOptions> registry = this.combinedDynamicRegistries.getCombinedRegistryManager().getOrThrow(RegistryKeys.DIMENSION);
      GeneratorOptions generatorOptions = this.saveProperties.getGeneratorOptions();
      long l = generatorOptions.getSeed();
      long m = BiomeAccess.hashSeed(l);
      List<SpecialSpawner> list = ImmutableList.of(
         new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new ZombieSiegeManager(), new WanderingTraderManager(serverWorldProperties)
      );
      DimensionOptions dimensionOptions = registry.get(DimensionOptions.OVERWORLD);
      ServerWorld serverWorld = new ServerWorld(
         this, this.workerExecutor, this.session, serverWorldProperties, World.OVERWORLD, dimensionOptions, bl, m, list, true, null
      );
      this.worlds.put(World.OVERWORLD, serverWorld);
      PersistentStateManager persistentStateManager = serverWorld.getPersistentStateManager();
      this.scoreboard.read(persistentStateManager.getOrCreate(ScoreboardState.TYPE).getPackedState());
      this.dataCommandStorage = new DataCommandStorage(persistentStateManager);
      this.stopwatchPersistentState = persistentStateManager.getOrCreate(StopwatchPersistentState.STATE_TYPE);
      if (!serverWorldProperties.isInitialized()) {
         try {
            setupSpawn(serverWorld, serverWorldProperties, generatorOptions.hasBonusChest(), bl, this.chunkLoadProgress);
            serverWorldProperties.setInitialized(true);
            if (bl) {
               this.setToDebugWorldProperties(this.saveProperties);
            }
         } catch (Throwable var28) {
            CrashReport crashReport = CrashReport.create(var28, "Exception initializing level");

            try {
               serverWorld.addDetailsToCrashReport(crashReport);
            } catch (Throwable var27) {
            }

            throw new CrashException(crashReport);
         }

         serverWorldProperties.setInitialized(true);
      }

      GlobalPos globalPos = this.getSpawnPos();
      this.chunkLoadProgress.initSpawnPos(globalPos.dimension(), new ChunkPos(globalPos.pos()));
      if (this.saveProperties.getCustomBossEvents() != null) {
         this.getBossBarManager().readNbt(this.saveProperties.getCustomBossEvents(), this.getRegistryManager());
      }

      RandomSequencesState randomSequencesState = serverWorld.getRandomSequences();
      boolean bl2 = false;

      for (Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : registry.getEntrySet()) {
         RegistryKey<DimensionOptions> registryKey = entry.getKey();
         ServerWorld serverWorld2;
         if (registryKey != DimensionOptions.OVERWORLD) {
            RegistryKey<World> registryKey2 = RegistryKey.of(RegistryKeys.WORLD, registryKey.getValue());
            UnmodifiableLevelProperties unmodifiableLevelProperties = new UnmodifiableLevelProperties(this.saveProperties, serverWorldProperties);
            serverWorld2 = new ServerWorld(
               this,
               this.workerExecutor,
               this.session,
               unmodifiableLevelProperties,
               registryKey2,
               entry.getValue(),
               bl,
               m,
               ImmutableList.of(),
               false,
               randomSequencesState
            );
            this.worlds.put(registryKey2, serverWorld2);
         } else {
            serverWorld2 = serverWorld;
         }

         Optional<WorldBorder.Properties> optional = serverWorldProperties.getWorldBorder();
         if (optional.isPresent()) {
            WorldBorder.Properties properties = optional.get();
            PersistentStateManager persistentStateManager2 = serverWorld2.getPersistentStateManager();
            if (persistentStateManager2.get(WorldBorder.TYPE) == null) {
               double d = serverWorld2.getDimension().coordinateScale();
               WorldBorder.Properties properties2 = new WorldBorder.Properties(
                  properties.centerX() / d,
                  properties.centerZ() / d,
                  properties.damagePerBlock(),
                  properties.safeZone(),
                  properties.warningBlocks(),
                  properties.warningTime(),
                  properties.size(),
                  properties.lerpTime(),
                  properties.lerpTarget()
               );
               WorldBorder worldBorder = new WorldBorder(properties2);
               worldBorder.ensureInitialized(serverWorld2.getTime());
               persistentStateManager2.set(WorldBorder.TYPE, worldBorder);
            }

            bl2 = true;
         }

         serverWorld2.getWorldBorder().setMaxRadius(this.getMaxWorldBorderRadius());
         this.getPlayerManager().setMainWorld(serverWorld2);
      }

      if (bl2) {
         serverWorldProperties.setWorldBorder(Optional.empty());
      }
   }

   private static void setupSpawn(
      ServerWorld world, ServerWorldProperties worldProperties, boolean bonusChest, boolean debugWorld, ChunkLoadProgress loadProgress
   ) {
      if (SharedConstants.ONLY_GENERATE_HALF_THE_WORLD && SharedConstants.WORLD_RECREATE) {
         worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(world.getRegistryKey(), new BlockPos(0, 64, -100), 0.0F, 0.0F));
      } else if (debugWorld) {
         worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(world.getRegistryKey(), BlockPos.ORIGIN.up(80), 0.0F, 0.0F));
      } else {
         ServerChunkManager serverChunkManager = world.getChunkManager();
         ChunkPos chunkPos = new ChunkPos(serverChunkManager.getNoiseConfig().getMultiNoiseSampler().findBestSpawnPosition());
         loadProgress.init(ChunkLoadProgress.Stage.PREPARE_GLOBAL_SPAWN, 0);
         loadProgress.initSpawnPos(world.getRegistryKey(), chunkPos);
         int i = serverChunkManager.getChunkGenerator().getSpawnHeight(world);
         if (i < world.getBottomY()) {
            BlockPos blockPos = chunkPos.getStartPos();
            i = world.getTopY(Heightmap.Type.WORLD_SURFACE, blockPos.getX() + 8, blockPos.getZ() + 8);
         }

         worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(world.getRegistryKey(), chunkPos.getStartPos().add(8, i, 8), 0.0F, 0.0F));
         int j = 0;
         int k = 0;
         int l = 0;
         int m = -1;

         for (int n = 0; n < MathHelper.square(11); n++) {
            if (j >= -5 && j <= 5 && k >= -5 && k <= 5) {
               BlockPos blockPos2 = SpawnLocating.findServerSpawnPoint(world, new ChunkPos(chunkPos.x + j, chunkPos.z + k));
               if (blockPos2 != null) {
                  worldProperties.setSpawnPoint(WorldProperties.SpawnPoint.create(world.getRegistryKey(), blockPos2, 0.0F, 0.0F));
                  break;
               }
            }

            if (j == k || j < 0 && j == -k || j > 0 && j == 1 - k) {
               int o = l;
               l = -m;
               m = o;
            }

            j += l;
            k += m;
         }

         if (bonusChest) {
            world.getRegistryManager()
               .getOptional(RegistryKeys.CONFIGURED_FEATURE)
               .flatMap(featureRegistry -> featureRegistry.getOptional(MiscConfiguredFeatures.BONUS_CHEST))
               .ifPresent(
                  feature -> feature.value().generate(world, serverChunkManager.getChunkGenerator(), world.random, worldProperties.getSpawnPoint().getPos())
               );
         }

         loadProgress.finish(ChunkLoadProgress.Stage.PREPARE_GLOBAL_SPAWN);
      }
   }

   private void setToDebugWorldProperties(SaveProperties properties) {
      properties.setDifficulty(Difficulty.PEACEFUL);
      properties.setDifficultyLocked(true);
      ServerWorldProperties serverWorldProperties = properties.getMainWorldProperties();
      serverWorldProperties.setRaining(false);
      serverWorldProperties.setThundering(false);
      serverWorldProperties.setClearWeatherTime(1000000000);
      serverWorldProperties.setTimeOfDay(6000L);
      serverWorldProperties.setGameMode(GameMode.SPECTATOR);
   }

   private void prepareStartRegion() {
      ChunkLoadingCounter chunkLoadingCounter = new ChunkLoadingCounter();

      for (ServerWorld serverWorld : this.worlds.values()) {
         chunkLoadingCounter.load(serverWorld, () -> {
            ChunkTicketManager chunkTicketManager = serverWorld.getPersistentStateManager().get(ChunkTicketManager.STATE_TYPE);
            if (chunkTicketManager != null) {
               chunkTicketManager.promoteToRealTickets();
            }
         });
      }

      this.chunkLoadProgress.init(ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS, chunkLoadingCounter.getTotalChunks());

      do {
         this.chunkLoadProgress
            .progress(ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS, chunkLoadingCounter.getFullChunks(), chunkLoadingCounter.getTotalChunks());
         this.tickStartTimeNanos = Util.getMeasuringTimeNano() + PREPARE_START_REGION_TICK_DELAY_NANOS;
         this.runTasksTillTickEnd();
      } while (chunkLoadingCounter.getNonFullChunks() > 0);

      this.chunkLoadProgress.finish(ChunkLoadProgress.Stage.LOAD_INITIAL_CHUNKS);
      this.updateMobSpawnOptions();
      this.refreshSpawnPoint();
   }

   protected GlobalPos getSpawnPos() {
      return this.saveProperties.getMainWorldProperties().getSpawnPoint().globalPos();
   }

   public GameMode getDefaultGameMode() {
      return this.saveProperties.getGameMode();
   }

   public boolean isHardcore() {
      return this.saveProperties.isHardcore();
   }

   public abstract LeveledPermissionPredicate getOpPermissionLevel();

   public abstract PermissionPredicate getFunctionPermissions();

   public abstract boolean shouldBroadcastRconToOps();

   public boolean save(boolean suppressLogs, boolean flush, boolean force) {
      this.scoreboard.writeTo(this.getOverworld().getPersistentStateManager().getOrCreate(ScoreboardState.TYPE));
      boolean bl = false;

      for (ServerWorld serverWorld : this.getWorlds()) {
         if (!suppressLogs) {
            LOGGER.info("Saving chunks for level '{}'/{}", serverWorld, serverWorld.getRegistryKey().getValue());
         }

         serverWorld.save(null, flush, SharedConstants.DONT_SAVE_WORLD || serverWorld.savingDisabled && !force);
         bl = true;
      }

      this.saveProperties.setCustomBossEvents(this.getBossBarManager().toNbt(this.getRegistryManager()));
      this.session.backupLevelDataFile(this.getRegistryManager(), this.saveProperties, this.getPlayerManager().getUserData());
      if (flush) {
         for (ServerWorld serverWorld : this.getWorlds()) {
            LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", serverWorld.getChunkManager().chunkLoadingManager.getSaveDir());
         }

         LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
      }

      return bl;
   }

   public boolean saveAll(boolean suppressLogs, boolean flush, boolean force) {
      boolean var4;
      try {
         this.saving = true;
         this.getPlayerManager().saveAllPlayerData();
         var4 = this.save(suppressLogs, flush, force);
      } finally {
         this.saving = false;
      }

      return var4;
   }

   @Override
   public void close() {
      this.shutdown();
   }

   public void shutdown() {
      this.packetApplyBatcher.close();
      if (this.recorder.isActive()) {
         this.forceStopRecorder();
      }

      LOGGER.info("Stopping server");
      this.getNetworkIo().stop();
      this.saving = true;
      if (this.playerManager != null) {
         LOGGER.info("Saving players");
         this.playerManager.saveAllPlayerData();
         this.playerManager.disconnectAllPlayers();
      }

      LOGGER.info("Saving worlds");

      for (ServerWorld serverWorld : this.getWorlds()) {
         if (serverWorld != null) {
            serverWorld.savingDisabled = false;
         }
      }

      while (this.worlds.values().stream().anyMatch(world -> world.getChunkManager().chunkLoadingManager.shouldDelayShutdown())) {
         this.tickStartTimeNanos = Util.getMeasuringTimeNano() + TimeHelper.MILLI_IN_NANOS;

         for (ServerWorld serverWorldx : this.getWorlds()) {
            serverWorldx.getChunkManager().shutdown();
            serverWorldx.getChunkManager().tick(() -> true, false);
         }

         this.runTasksTillTickEnd();
      }

      this.save(false, true, false);

      for (ServerWorld serverWorldx : this.getWorlds()) {
         if (serverWorldx != null) {
            try {
               serverWorldx.close();
            } catch (IOException var5) {
               LOGGER.error("Exception closing the level", var5);
            }
         }
      }

      this.saving = false;
      this.resourceManagerHolder.close();

      try {
         this.session.close();
      } catch (IOException var4) {
         LOGGER.error("Failed to unlock level {}", this.session.getDirectoryName(), var4);
      }
   }

   public String getServerIp() {
      return this.serverIp;
   }

   public void setServerIp(String serverIp) {
      this.serverIp = serverIp;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void stop(boolean waitForShutdown) {
      this.running = false;
      if (waitForShutdown) {
         try {
            this.serverThread.join();
         } catch (InterruptedException var3) {
            LOGGER.error("Error while shutting down", var3);
         }
      }
   }

   protected void runServer() {
      try {
         if (!this.setupServer()) {
            throw new IllegalStateException("Failed to initialize server");
         }

         this.tickStartTimeNanos = Util.getMeasuringTimeNano();
         this.favicon = this.loadFavicon().orElse(null);
         this.metadata = this.createMetadata();

         while (this.running) {
            long l;
            if (!this.isPaused() && this.tickManager.isSprinting() && this.tickManager.sprint()) {
               l = 0L;
               this.tickStartTimeNanos = Util.getMeasuringTimeNano();
               this.lastOverloadWarningNanos = this.tickStartTimeNanos;
            } else {
               l = this.tickManager.getNanosPerTick();
               long m = Util.getMeasuringTimeNano() - this.tickStartTimeNanos;
               if (m > OVERLOAD_THRESHOLD_NANOS + 20L * l
                  && this.tickStartTimeNanos - this.lastOverloadWarningNanos >= OVERLOAD_WARNING_INTERVAL_NANOS + 100L * l) {
                  long n = m / l;
                  LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", m / TimeHelper.MILLI_IN_NANOS, n);
                  this.tickStartTimeNanos += n * l;
                  this.lastOverloadWarningNanos = this.tickStartTimeNanos;
               }
            }

            boolean bl = l == 0L;
            if (this.needsDebugSetup) {
               this.needsDebugSetup = false;
               this.debugStart = new MinecraftServer.DebugStart(Util.getMeasuringTimeNano(), this.ticks);
            }

            this.tickStartTimeNanos += l;

            try (Profilers.Scoped scoped = Profilers.using(this.startTickMetrics())) {
               this.processPacketsAndTick(bl);
               Profiler profiler = Profilers.get();
               profiler.push("nextTickWait");
               this.hasJustExecutedTask = true;
               this.tickEndTimeNanos = Math.max(Util.getMeasuringTimeNano() + l, this.tickStartTimeNanos);
               this.startTaskPerformanceLog();
               this.runTasksTillTickEnd();
               this.pushPerformanceLogs();
               if (bl) {
                  this.tickManager.updateSprintTime();
               }

               profiler.pop();
               this.pushFullTickLog();
            } finally {
               this.endTickMetrics();
            }

            this.loading = true;
            FlightProfiler.INSTANCE.onTick(this.averageTickTime);
         }
      } catch (Throwable var69) {
         LOGGER.error("Encountered an unexpected exception", var69);
         CrashReport crashReport = createCrashReport(var69);
         this.addSystemDetails(crashReport.getSystemDetailsSection());
         Path path = this.getRunDirectory().resolve("crash-reports").resolve("crash-" + Util.getFormattedCurrentTime() + "-server.txt");
         if (crashReport.writeToFile(path, ReportType.MINECRAFT_CRASH_REPORT)) {
            LOGGER.error("This crash report has been saved to: {}", path.toAbsolutePath());
         } else {
            LOGGER.error("We were unable to save this crash report to disk.");
         }

         this.setCrashReport(crashReport);
      } finally {
         try {
            this.stopped = true;
            this.shutdown();
         } catch (Throwable var64) {
            LOGGER.error("Exception stopping the server", var64);
         } finally {
            this.exit();
         }
      }
   }

   private void pushFullTickLog() {
      long l = Util.getMeasuringTimeNano();
      if (this.shouldPushTickTimeLog()) {
         this.getDebugSampleLog().push(l - this.lastFullTickLogTime);
      }

      this.lastFullTickLogTime = l;
   }

   private void startTaskPerformanceLog() {
      if (this.shouldPushTickTimeLog()) {
         this.tasksStartTime = Util.getMeasuringTimeNano();
         this.waitTime = 0L;
      }
   }

   private void pushPerformanceLogs() {
      if (this.shouldPushTickTimeLog()) {
         DebugSampleLog debugSampleLog = this.getDebugSampleLog();
         debugSampleLog.push(Util.getMeasuringTimeNano() - this.tasksStartTime - this.waitTime, ServerTickType.SCHEDULED_TASKS.ordinal());
         debugSampleLog.push(this.waitTime, ServerTickType.IDLE.ordinal());
      }
   }

   private static CrashReport createCrashReport(Throwable throwable) {
      CrashException crashException = null;

      for (Throwable throwable2 = throwable; throwable2 != null; throwable2 = throwable2.getCause()) {
         if (throwable2 instanceof CrashException crashException2) {
            crashException = crashException2;
         }
      }

      CrashReport crashReport;
      if (crashException != null) {
         crashReport = crashException.getReport();
         if (crashException != throwable) {
            crashReport.addElement("Wrapped in").add("Wrapping exception", throwable);
         }
      } else {
         crashReport = new CrashReport("Exception in server tick loop", throwable);
      }

      return crashReport;
   }

   private boolean shouldKeepTicking() {
      return this.hasRunningTasks() || Util.getMeasuringTimeNano() < (this.hasJustExecutedTask ? this.tickEndTimeNanos : this.tickStartTimeNanos);
   }

   public static boolean checkWorldGenException() {
      RuntimeException runtimeException = WORLD_GEN_EXCEPTION.get();
      if (runtimeException != null) {
         throw runtimeException;
      } else {
         return true;
      }
   }

   public static void setWorldGenException(RuntimeException exception) {
      WORLD_GEN_EXCEPTION.compareAndSet(null, exception);
   }

   @Override
   public void runTasks(BooleanSupplier stopCondition) {
      super.runTasks(() -> checkWorldGenException() && stopCondition.getAsBoolean());
   }

   public CompositeManagementListener getManagementListener() {
      return this.managementListener;
   }

   protected void runTasksTillTickEnd() {
      this.runTasks();
      this.waitingForNextTick = true;

      try {
         this.runTasks(() -> !this.shouldKeepTicking());
      } finally {
         this.waitingForNextTick = false;
      }
   }

   @Override
   public void waitForTasks() {
      boolean bl = this.shouldPushTickTimeLog();
      long l = bl ? Util.getMeasuringTimeNano() : 0L;
      long m = this.waitingForNextTick ? this.tickStartTimeNanos - Util.getMeasuringTimeNano() : 100000L;
      LockSupport.parkNanos("waiting for tasks", m);
      if (bl) {
         this.waitTime = this.waitTime + (Util.getMeasuringTimeNano() - l);
      }
   }

   public ServerTask createTask(Runnable runnable) {
      return new ServerTask(this.ticks, runnable);
   }

   protected boolean canExecute(ServerTask serverTask) {
      return serverTask.getCreationTicks() + 3 < this.ticks || this.shouldKeepTicking();
   }

   @Override
   public boolean runTask() {
      boolean bl = this.runOneTask();
      this.hasJustExecutedTask = bl;
      return bl;
   }

   private boolean runOneTask() {
      if (super.runTask()) {
         return true;
      } else {
         if (this.tickManager.isSprinting() || this.isExecutionInProgress() || this.shouldKeepTicking()) {
            for (ServerWorld serverWorld : this.getWorlds()) {
               if (serverWorld.getChunkManager().executeQueuedTasks()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public void executeTask(ServerTask serverTask) {
      Profilers.get().visit("runTask");
      super.executeTask(serverTask);
   }

   private Optional<ServerMetadata.Favicon> loadFavicon() {
      Optional<Path> optional = Optional.of(this.getPath("server-icon.png"))
         .filter(path -> Files.isRegularFile(path))
         .or(() -> this.session.getIconFile().filter(path -> Files.isRegularFile(path)));
      return optional.flatMap(path -> {
         try {
            byte[] bs = Files.readAllBytes(path);
            PngMetadata pngMetadata = PngMetadata.fromBytes(bs);
            if (pngMetadata.width() == 64 && pngMetadata.height() == 64) {
               return Optional.of(new ServerMetadata.Favicon(bs));
            } else {
               throw new IllegalArgumentException("Invalid world icon size [" + pngMetadata.width() + ", " + pngMetadata.height() + "], but expected [64, 64]");
            }
         } catch (Exception var3) {
            LOGGER.error("Couldn't load server icon", var3);
            return Optional.empty();
         }
      });
   }

   public Optional<Path> getIconFile() {
      return this.session.getIconFile();
   }

   public Path getRunDirectory() {
      return Path.of("");
   }

   public ActivityNotifier getActivityNotifier() {
      return this.activityNotifier;
   }

   public void setCrashReport(CrashReport report) {
   }

   public void exit() {
   }

   public boolean isPaused() {
      return false;
   }

   public void tick(BooleanSupplier shouldKeepTicking) {
      long l = Util.getMeasuringTimeNano();
      int i = this.getPauseWhenEmptySeconds() * 20;
      if (i > 0) {
         if (this.playerManager.getCurrentPlayerCount() == 0 && !this.tickManager.isSprinting()) {
            this.idleTickCount++;
         } else {
            this.idleTickCount = 0;
         }

         if (this.idleTickCount >= i) {
            if (this.idleTickCount == i) {
               LOGGER.info("Server empty for {} seconds, pausing", this.getPauseWhenEmptySeconds());
               this.runAutosave();
            }

            this.tickNetworkIo();
            return;
         }
      }

      this.ticks++;
      this.tickManager.step();
      this.tickWorlds(shouldKeepTicking);
      if (l - this.lastPlayerSampleUpdate >= PLAYER_SAMPLE_UPDATE_INTERVAL_NANOS) {
         this.lastPlayerSampleUpdate = l;
         this.metadata = this.createMetadata();
      }

      this.ticksUntilAutosave--;
      if (this.ticksUntilAutosave <= 0) {
         this.runAutosave();
      }

      Profiler profiler = Profilers.get();
      profiler.push("tallying");
      long m = Util.getMeasuringTimeNano() - l;
      int j = this.ticks % 100;
      this.recentTickTimesNanos = this.recentTickTimesNanos - this.tickTimes[j];
      this.recentTickTimesNanos += m;
      this.tickTimes[j] = m;
      this.averageTickTime = this.averageTickTime * 0.8F + (float)m / (float)TimeHelper.MILLI_IN_NANOS * 0.19999999F;
      this.pushTickLog(l);
      profiler.pop();
   }

   protected void processPacketsAndTick(boolean sprint) {
      Profiler profiler = Profilers.get();
      profiler.push("tick");
      this.discontinuousFrame.start();
      profiler.push("scheduledPacketProcessing");
      this.packetApplyBatcher.apply();
      profiler.pop();
      this.tick(sprint ? () -> false : this::shouldKeepTicking);
      this.discontinuousFrame.end();
      profiler.pop();
   }

   private void runAutosave() {
      this.ticksUntilAutosave = this.getAutosaveInterval();
      LOGGER.debug("Autosave started");
      Profiler profiler = Profilers.get();
      profiler.push("save");
      this.saveAll(true, false, false);
      profiler.pop();
      LOGGER.debug("Autosave finished");
   }

   private void pushTickLog(long tickStartTime) {
      if (this.shouldPushTickTimeLog()) {
         this.getDebugSampleLog().push(Util.getMeasuringTimeNano() - tickStartTime, ServerTickType.TICK_SERVER_METHOD.ordinal());
      }
   }

   private int getAutosaveInterval() {
      float f;
      if (this.tickManager.isSprinting()) {
         long l = this.getAverageNanosPerTick() + 1L;
         f = (float)TimeHelper.SECOND_IN_NANOS / (float)l;
      } else {
         f = this.tickManager.getTickRate();
      }

      int i = 300;
      return Math.max(100, (int)(f * 300.0F));
   }

   public void updateAutosaveTicks() {
      int i = this.getAutosaveInterval();
      if (i < this.ticksUntilAutosave) {
         this.ticksUntilAutosave = i;
      }
   }

   protected abstract DebugSampleLog getDebugSampleLog();

   public abstract boolean shouldPushTickTimeLog();

   private ServerMetadata createMetadata() {
      ServerMetadata.Players players = this.createMetadataPlayers();
      return new ServerMetadata(
         Text.of(this.getServerMotd()),
         Optional.of(players),
         Optional.of(ServerMetadata.Version.create()),
         Optional.ofNullable(this.favicon),
         this.shouldEnforceSecureProfile()
      );
   }

   private ServerMetadata.Players createMetadataPlayers() {
      List<ServerPlayerEntity> list = this.playerManager.getPlayerList();
      int i = this.getMaxPlayerCount();
      if (this.hideOnlinePlayers()) {
         return new ServerMetadata.Players(i, list.size(), List.of());
      } else {
         int j = Math.min(list.size(), 12);
         ObjectArrayList<PlayerConfigEntry> objectArrayList = new ObjectArrayList(j);
         int k = MathHelper.nextInt(this.random, 0, list.size() - j);

         for (int l = 0; l < j; l++) {
            ServerPlayerEntity serverPlayerEntity = list.get(k + l);
            objectArrayList.add(serverPlayerEntity.allowsServerListing() ? serverPlayerEntity.getPlayerConfigEntry() : ANONYMOUS_PLAYER_PROFILE);
         }

         Util.shuffle(objectArrayList, this.random);
         return new ServerMetadata.Players(i, list.size(), objectArrayList);
      }
   }

   protected void tickWorlds(BooleanSupplier shouldKeepTicking) {
      Profiler profiler = Profilers.get();
      this.getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.disableFlush());
      profiler.push("commandFunctions");
      this.getCommandFunctionManager().tick();
      profiler.swap("levels");
      this.refreshSpawnPoint();

      for (ServerWorld serverWorld : this.getWorlds()) {
         profiler.push(() -> serverWorld + " " + serverWorld.getRegistryKey().getValue());
         if (this.ticks % 20 == 0) {
            profiler.push("timeSync");
            this.sendTimeUpdatePackets(serverWorld);
            profiler.pop();
         }

         profiler.push("tick");

         try {
            serverWorld.tick(shouldKeepTicking);
         } catch (Throwable var7) {
            CrashReport crashReport = CrashReport.create(var7, "Exception ticking world");
            serverWorld.addDetailsToCrashReport(crashReport);
            throw new CrashException(crashReport);
         }

         profiler.pop();
         profiler.pop();
      }

      profiler.swap("connection");
      this.tickNetworkIo();
      profiler.swap("players");
      this.playerManager.updatePlayerLatency();
      profiler.swap("debugSubscribers");
      this.subscriberTracker.tick();
      if (this.tickManager.shouldTick()) {
         profiler.swap("gameTests");
         TestManager.INSTANCE.tick();
      }

      profiler.swap("server gui refresh");

      for (Runnable runnable : this.serverGuiTickables) {
         runnable.run();
      }

      profiler.swap("send chunks");

      for (ServerPlayerEntity serverPlayerEntity : this.playerManager.getPlayerList()) {
         serverPlayerEntity.networkHandler.chunkDataSender.sendChunkBatches(serverPlayerEntity);
         serverPlayerEntity.networkHandler.enableFlush();
      }

      profiler.pop();
      this.activityNotifier.notifyListeners();
   }

   private void refreshSpawnPoint() {
      WorldProperties.SpawnPoint spawnPoint = this.saveProperties.getMainWorldProperties().getSpawnPoint();
      ServerWorld serverWorld = this.getSpawnWorld();
      this.spawnPoint = serverWorld.ensureWithinBorder(spawnPoint);
   }

   public void tickNetworkIo() {
      this.getNetworkIo().tick();
   }

   private void sendTimeUpdatePackets(ServerWorld world) {
      this.playerManager
         .sendToDimension(
            new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(), world.getGameRules().getValue(GameRules.ADVANCE_TIME)), world.getRegistryKey()
         );
   }

   public void sendTimeUpdatePackets() {
      Profiler profiler = Profilers.get();
      profiler.push("timeSync");

      for (ServerWorld serverWorld : this.getWorlds()) {
         this.sendTimeUpdatePackets(serverWorld);
      }

      profiler.pop();
   }

   public void addServerGuiTickable(Runnable tickable) {
      this.serverGuiTickables.add(tickable);
   }

   protected void setServerId(String serverId) {
      this.serverId = serverId;
   }

   public boolean isStopping() {
      return !this.serverThread.isAlive();
   }

   public Path getPath(String path) {
      return this.getRunDirectory().resolve(path);
   }

   public final ServerWorld getOverworld() {
      return this.worlds.get(World.OVERWORLD);
   }

   public @Nullable ServerWorld getWorld(RegistryKey<World> key) {
      return this.worlds.get(key);
   }

   public Set<RegistryKey<World>> getWorldRegistryKeys() {
      return this.worlds.keySet();
   }

   public Iterable<ServerWorld> getWorlds() {
      return this.worlds.values();
   }

   @Override
   public String getVersion() {
      return SharedConstants.getGameVersion().name();
   }

   @Override
   public int getCurrentPlayerCount() {
      return this.playerManager.getCurrentPlayerCount();
   }

   public String[] getPlayerNames() {
      return this.playerManager.getPlayerNames();
   }

   @DontObfuscate
   public String getServerModName() {
      return "vanilla";
   }

   public SystemDetails addSystemDetails(SystemDetails details) {
      details.addSection("Server Running", () -> Boolean.toString(this.running));
      if (this.playerManager != null) {
         details.addSection(
            "Player Count",
            () -> this.playerManager.getCurrentPlayerCount() + " / " + this.playerManager.getMaxPlayerCount() + "; " + this.playerManager.getPlayerList()
         );
      }

      details.addSection("Active Data Packs", () -> ResourcePackManager.listPacks(this.dataPackManager.getEnabledProfiles()));
      details.addSection("Available Data Packs", () -> ResourcePackManager.listPacks(this.dataPackManager.getProfiles()));
      details.addSection(
         "Enabled Feature Flags",
         () -> FeatureFlags.FEATURE_MANAGER.toId(this.saveProperties.getEnabledFeatures()).stream().map(Identifier::toString).collect(Collectors.joining(", "))
      );
      details.addSection("World Generation", () -> this.saveProperties.getLifecycle().toString());
      details.addSection("World Seed", () -> String.valueOf(this.saveProperties.getGeneratorOptions().getSeed()));
      details.addSection("Suppressed Exceptions", this.suppressedExceptionsTracker::collect);
      if (this.serverId != null) {
         details.addSection("Server Id", () -> this.serverId);
      }

      return this.addExtraSystemDetails(details);
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
      return Objects.requireNonNull(this.keyPair);
   }

   public int getServerPort() {
      return this.serverPort;
   }

   public void setServerPort(int serverPort) {
      this.serverPort = serverPort;
   }

   public @Nullable GameProfile getHostProfile() {
      return this.hostProfile;
   }

   public void setHostProfile(@Nullable GameProfile hostProfile) {
      this.hostProfile = hostProfile;
   }

   public boolean isSingleplayer() {
      return this.hostProfile != null;
   }

   protected void generateKeyPair() {
      LOGGER.info("Generating keypair");

      try {
         this.keyPair = NetworkEncryptionUtils.generateServerKeyPair();
      } catch (NetworkEncryptionException var2) {
         throw new IllegalStateException("Failed to generate key pair", var2);
      }
   }

   public void setDifficulty(Difficulty difficulty, boolean forceUpdate) {
      if (forceUpdate || !this.saveProperties.isDifficultyLocked()) {
         this.saveProperties.setDifficulty(this.saveProperties.isHardcore() ? Difficulty.HARD : difficulty);
         this.updateMobSpawnOptions();
         this.getPlayerManager().getPlayerList().forEach(this::sendDifficulty);
      }
   }

   public int adjustTrackingDistance(int initialDistance) {
      return initialDistance;
   }

   public void updateMobSpawnOptions() {
      for (ServerWorld serverWorld : this.getWorlds()) {
         serverWorld.setMobSpawnOptions(serverWorld.shouldSpawnMonsters());
      }
   }

   public void setDifficultyLocked(boolean locked) {
      this.saveProperties.setDifficultyLocked(locked);
      this.getPlayerManager().getPlayerList().forEach(this::sendDifficulty);
   }

   private void sendDifficulty(ServerPlayerEntity player) {
      WorldProperties worldProperties = player.getEntityWorld().getLevelProperties();
      player.networkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
   }

   public boolean isDemo() {
      return this.demo;
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
      return this.getResourcePackProperties().filter(MinecraftServer.ServerResourcePackProperties::isRequired).isPresent();
   }

   public abstract boolean isDedicated();

   public abstract int getRateLimit();

   public boolean isOnlineMode() {
      return this.onlineMode;
   }

   public void setOnlineMode(boolean onlineMode) {
      this.onlineMode = onlineMode;
   }

   public boolean shouldPreventProxyConnections() {
      return this.preventProxyConnections;
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
      return this.motd;
   }

   public void setMotd(String motd) {
      this.motd = motd;
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public PlayerManager getPlayerManager() {
      return this.playerManager;
   }

   public void setPlayerManager(PlayerManager playerManager) {
      this.playerManager = playerManager;
   }

   public abstract boolean isRemote();

   public void setDefaultGameMode(GameMode gameMode) {
      this.saveProperties.setGameMode(gameMode);
   }

   public int changeGameModeGlobally(@Nullable GameMode gameMode) {
      if (gameMode == null) {
         return 0;
      } else {
         int i = 0;

         for (ServerPlayerEntity serverPlayerEntity : this.getPlayerManager().getPlayerList()) {
            if (serverPlayerEntity.changeGameMode(gameMode)) {
               i++;
            }
         }

         return i;
      }
   }

   public ServerNetworkIo getNetworkIo() {
      return this.networkIo;
   }

   public boolean isLoading() {
      return this.loading;
   }

   public boolean openToLan(@Nullable GameMode gameMode, boolean cheatsAllowed, int port) {
      return false;
   }

   public int getTicks() {
      return this.ticks;
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
      return this.proxy;
   }

   public int getPlayerIdleTimeout() {
      return this.playerIdleTimeout;
   }

   public void setPlayerIdleTimeout(int playerIdleTimeout) {
      this.playerIdleTimeout = playerIdleTimeout;
   }

   public ApiServices getApiServices() {
      return this.apiServices;
   }

   public @Nullable ServerMetadata getServerMetadata() {
      return this.metadata;
   }

   public void forcePlayerSampleUpdate() {
      this.lastPlayerSampleUpdate = 0L;
   }

   public int getMaxWorldBorderRadius() {
      return 29999984;
   }

   @Override
   public boolean shouldExecuteAsync() {
      return super.shouldExecuteAsync() && !this.isStopped();
   }

   @Override
   public void executeSync(Runnable runnable) {
      if (this.isStopped()) {
         throw new RejectedExecutionException("Server already shutting down");
      } else {
         super.executeSync(runnable);
      }
   }

   @Override
   public Thread getThread() {
      return this.serverThread;
   }

   public int getNetworkCompressionThreshold() {
      return 256;
   }

   public boolean shouldEnforceSecureProfile() {
      return false;
   }

   public long getTimeReference() {
      return this.tickStartTimeNanos;
   }

   public DataFixer getDataFixer() {
      return this.dataFixer;
   }

   public ServerAdvancementLoader getAdvancementLoader() {
      return this.resourceManagerHolder.dataPackContents.getServerAdvancementLoader();
   }

   public CommandFunctionManager getCommandFunctionManager() {
      return this.commandFunctionManager;
   }

   public CompletableFuture<Void> reloadResources(Collection<String> dataPacks) {
      CompletableFuture<Void> completableFuture = CompletableFuture.<ImmutableList>supplyAsync(
            () -> dataPacks.stream()
               .map(this.dataPackManager::getProfile)
               .filter(Objects::nonNull)
               .map(ResourcePackProfile::createResourcePack)
               .collect(ImmutableList.toImmutableList()),
            this
         )
         .thenCompose(
            resourcePacks -> {
               LifecycledResourceManager lifecycledResourceManager = new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, resourcePacks);
               List<Registry.PendingTagLoad<?>> list = TagGroupLoader.startReload(
                  lifecycledResourceManager, this.combinedDynamicRegistries.getCombinedRegistryManager()
               );
               return DataPackContents.reload(
                     lifecycledResourceManager,
                     this.combinedDynamicRegistries,
                     list,
                     this.saveProperties.getEnabledFeatures(),
                     this.isDedicated() ? CommandManager.RegistrationEnvironment.DEDICATED : CommandManager.RegistrationEnvironment.INTEGRATED,
                     this.getFunctionPermissions(),
                     this.workerExecutor,
                     this
                  )
                  .whenComplete((dataPackContents, throwable) -> {
                     if (throwable != null) {
                        lifecycledResourceManager.close();
                     }
                  })
                  .thenApply(dataPackContents -> new MinecraftServer.ResourceManagerHolder(lifecycledResourceManager, dataPackContents));
            }
         )
         .thenAcceptAsync(
            resourceManagerHolder -> {
               this.resourceManagerHolder.close();
               this.resourceManagerHolder = resourceManagerHolder;
               this.dataPackManager.setEnabledProfiles(dataPacks);
               DataConfiguration dataConfiguration = new DataConfiguration(
                  createDataPackSettings(this.dataPackManager, true), this.saveProperties.getEnabledFeatures()
               );
               this.saveProperties.updateLevelInfo(dataConfiguration);
               this.resourceManagerHolder.dataPackContents.applyPendingTagLoads();
               this.resourceManagerHolder.dataPackContents.getRecipeManager().initialize(this.saveProperties.getEnabledFeatures());
               this.getPlayerManager().saveAllPlayerData();
               this.getPlayerManager().onDataPacksReloaded();
               this.commandFunctionManager.setFunctions(this.resourceManagerHolder.dataPackContents.getFunctionLoader());
               this.structureTemplateManager.setResourceManager(this.resourceManagerHolder.resourceManager);
               this.fuelRegistry = FuelRegistry.createDefault(
                  this.combinedDynamicRegistries.getCombinedRegistryManager(), this.saveProperties.getEnabledFeatures()
               );
            },
            this
         );
      if (this.isOnThread()) {
         this.runTasks(completableFuture::isDone);
      }

      return completableFuture;
   }

   public static DataConfiguration loadDataPacks(
      ResourcePackManager resourcePackManager, DataConfiguration dataConfiguration, boolean initMode, boolean safeMode
   ) {
      DataPackSettings dataPackSettings = dataConfiguration.dataPacks();
      FeatureSet featureSet = initMode ? FeatureSet.empty() : dataConfiguration.enabledFeatures();
      FeatureSet featureSet2 = initMode ? FeatureFlags.FEATURE_MANAGER.getFeatureSet() : dataConfiguration.enabledFeatures();
      resourcePackManager.scanPacks();
      if (safeMode) {
         return loadDataPacks(resourcePackManager, List.of("vanilla"), featureSet, false);
      } else {
         Set<String> set = Sets.newLinkedHashSet();

         for (String string : dataPackSettings.getEnabled()) {
            if (resourcePackManager.hasProfile(string)) {
               set.add(string);
            } else {
               LOGGER.warn("Missing data pack {}", string);
            }
         }

         for (ResourcePackProfile resourcePackProfile : resourcePackManager.getProfiles()) {
            String string2 = resourcePackProfile.getId();
            if (!dataPackSettings.getDisabled().contains(string2)) {
               FeatureSet featureSet3 = resourcePackProfile.getRequestedFeatures();
               boolean bl = set.contains(string2);
               if (!bl && resourcePackProfile.getSource().canBeEnabledLater()) {
                  if (featureSet3.isSubsetOf(featureSet2)) {
                     LOGGER.info("Found new data pack {}, loading it automatically", string2);
                     set.add(string2);
                  } else {
                     LOGGER.info(
                        "Found new data pack {}, but can't load it due to missing features {}",
                        string2,
                        FeatureFlags.printMissingFlags(featureSet2, featureSet3)
                     );
                  }
               }

               if (bl && !featureSet3.isSubsetOf(featureSet2)) {
                  LOGGER.warn(
                     "Pack {} requires features {} that are not enabled for this world, disabling pack.",
                     string2,
                     FeatureFlags.printMissingFlags(featureSet2, featureSet3)
                  );
                  set.remove(string2);
               }
            }
         }

         if (set.isEmpty()) {
            LOGGER.info("No datapacks selected, forcing vanilla");
            set.add("vanilla");
         }

         return loadDataPacks(resourcePackManager, set, featureSet, true);
      }
   }

   private static DataConfiguration loadDataPacks(
      ResourcePackManager resourcePackManager, Collection<String> enabledProfiles, FeatureSet enabledFeatures, boolean allowEnabling
   ) {
      resourcePackManager.setEnabledProfiles(enabledProfiles);
      forceEnableRequestedFeatures(resourcePackManager, enabledFeatures);
      DataPackSettings dataPackSettings = createDataPackSettings(resourcePackManager, allowEnabling);
      FeatureSet featureSet = resourcePackManager.getRequestedFeatures().combine(enabledFeatures);
      return new DataConfiguration(dataPackSettings, featureSet);
   }

   private static void forceEnableRequestedFeatures(ResourcePackManager resourcePackManager, FeatureSet enabledFeatures) {
      FeatureSet featureSet = resourcePackManager.getRequestedFeatures();
      FeatureSet featureSet2 = enabledFeatures.subtract(featureSet);
      if (!featureSet2.isEmpty()) {
         Set<String> set = new ObjectArraySet(resourcePackManager.getEnabledIds());

         for (ResourcePackProfile resourcePackProfile : resourcePackManager.getProfiles()) {
            if (featureSet2.isEmpty()) {
               break;
            }

            if (resourcePackProfile.getSource() == ResourcePackSource.FEATURE) {
               String string = resourcePackProfile.getId();
               FeatureSet featureSet3 = resourcePackProfile.getRequestedFeatures();
               if (!featureSet3.isEmpty() && featureSet3.intersects(featureSet2) && featureSet3.isSubsetOf(enabledFeatures)) {
                  if (!set.add(string)) {
                     throw new IllegalStateException("Tried to force '" + string + "', but it was already enabled");
                  }

                  LOGGER.info("Found feature pack ('{}') for requested feature, forcing to enabled", string);
                  featureSet2 = featureSet2.subtract(featureSet3);
               }
            }
         }

         resourcePackManager.setEnabledProfiles(set);
      }
   }

   private static DataPackSettings createDataPackSettings(ResourcePackManager dataPackManager, boolean allowEnabling) {
      Collection<String> collection = dataPackManager.getEnabledIds();
      List<String> list = ImmutableList.copyOf(collection);
      List<String> list2 = allowEnabling ? dataPackManager.getIds().stream().filter(name -> !collection.contains(name)).toList() : List.of();
      return new DataPackSettings(list, list2);
   }

   public void kickNonWhitelistedPlayers() {
      if (this.isEnforceWhitelist() && this.getUseAllowlist()) {
         PlayerManager playerManager = this.getPlayerManager();
         Whitelist whitelist = playerManager.getWhitelist();

         for (ServerPlayerEntity serverPlayerEntity : Lists.newArrayList(playerManager.getPlayerList())) {
            if (!whitelist.isAllowed(serverPlayerEntity.getPlayerConfigEntry())) {
               serverPlayerEntity.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.not_whitelisted"));
            }
         }
      }
   }

   public ResourcePackManager getDataPackManager() {
      return this.dataPackManager;
   }

   public CommandManager getCommandManager() {
      return this.resourceManagerHolder.dataPackContents.getCommandManager();
   }

   public ServerCommandSource getCommandSource() {
      ServerWorld serverWorld = this.getSpawnWorld();
      return new ServerCommandSource(
         this,
         Vec3d.of(this.getSpawnPoint().getPos()),
         Vec2f.ZERO,
         serverWorld,
         LeveledPermissionPredicate.OWNERS,
         "Server",
         Text.literal("Server"),
         this,
         null
      );
   }

   public ServerWorld getSpawnWorld() {
      WorldProperties.SpawnPoint spawnPoint = this.getSaveProperties().getMainWorldProperties().getSpawnPoint();
      RegistryKey<World> registryKey = spawnPoint.getDimension();
      ServerWorld serverWorld = this.getWorld(registryKey);
      return serverWorld != null ? serverWorld : this.getOverworld();
   }

   public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
      ServerWorldProperties serverWorldProperties = this.saveProperties.getMainWorldProperties();
      WorldProperties.SpawnPoint spawnPoint2 = serverWorldProperties.getSpawnPoint();
      if (!spawnPoint2.equals(spawnPoint)) {
         serverWorldProperties.setSpawnPoint(spawnPoint);
         this.getPlayerManager().sendToAll(new PlayerSpawnPositionS2CPacket(spawnPoint));
         this.refreshSpawnPoint();
      }
   }

   public WorldProperties.SpawnPoint getSpawnPoint() {
      return this.spawnPoint;
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
      return this.resourceManagerHolder.dataPackContents.getRecipeManager();
   }

   public ServerScoreboard getScoreboard() {
      return this.scoreboard;
   }

   public DataCommandStorage getDataCommandStorage() {
      if (this.dataCommandStorage == null) {
         throw new NullPointerException("Called before server init");
      } else {
         return this.dataCommandStorage;
      }
   }

   public StopwatchPersistentState getStopwatchPersistentState() {
      if (this.stopwatchPersistentState == null) {
         throw new NullPointerException("Called before server init");
      } else {
         return this.stopwatchPersistentState;
      }
   }

   public BossBarManager getBossBarManager() {
      return this.bossBarManager;
   }

   public boolean isEnforceWhitelist() {
      return this.enforceWhitelist;
   }

   public void setEnforceWhitelist(boolean enforceWhitelist) {
      this.enforceWhitelist = enforceWhitelist;
   }

   public boolean getUseAllowlist() {
      return this.useAllowlist;
   }

   public void setUseAllowlist(boolean useAllowlist) {
      this.useAllowlist = useAllowlist;
   }

   public float getAverageTickTime() {
      return this.averageTickTime;
   }

   public ServerTickManager getTickManager() {
      return this.tickManager;
   }

   public long getAverageNanosPerTick() {
      return this.recentTickTimesNanos / Math.min(100, Math.max(this.ticks, 1));
   }

   public long[] getTickTimes() {
      return this.tickTimes;
   }

   public LeveledPermissionPredicate getPermissionLevel(PlayerConfigEntry player) {
      if (this.getPlayerManager().isOperator(player)) {
         OperatorEntry operatorEntry = this.getPlayerManager().getOpList().get(player);
         if (operatorEntry != null) {
            return operatorEntry.getLevel();
         } else if (this.isHost(player)) {
            return LeveledPermissionPredicate.OWNERS;
         } else if (this.isSingleplayer()) {
            return this.getPlayerManager().areCheatsAllowed() ? LeveledPermissionPredicate.OWNERS : LeveledPermissionPredicate.ALL;
         } else {
            return this.getOpPermissionLevel();
         }
      } else {
         return LeveledPermissionPredicate.ALL;
      }
   }

   public abstract boolean isHost(PlayerConfigEntry player);

   public void dumpProperties(Path file) throws IOException {
   }

   private void dump(Path path) {
      Path path2 = path.resolve("levels");

      try {
         for (Entry<RegistryKey<World>, ServerWorld> entry : this.worlds.entrySet()) {
            Identifier identifier = entry.getKey().getValue();
            Path path3 = path2.resolve(identifier.getNamespace()).resolve(identifier.getPath());
            Files.createDirectories(path3);
            entry.getValue().dump(path3);
         }

         this.dumpGamerules(path.resolve("gamerules.txt"));
         this.dumpClasspath(path.resolve("classpath.txt"));
         this.dumpStats(path.resolve("stats.txt"));
         this.dumpThreads(path.resolve("threads.txt"));
         this.dumpProperties(path.resolve("server.properties.txt"));
         this.dumpNativeModules(path.resolve("modules.txt"));
      } catch (IOException var7) {
         LOGGER.warn("Failed to save debug report", var7);
      }
   }

   private void dumpStats(Path path) throws IOException {
      try (Writer writer = Files.newBufferedWriter(path)) {
         writer.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getTaskCount()));
         writer.write(String.format(Locale.ROOT, "average_tick_time: %f\n", this.getAverageTickTime()));
         writer.write(String.format(Locale.ROOT, "tick_times: %s\n", Arrays.toString(this.tickTimes)));
         writer.write(String.format(Locale.ROOT, "queue: %s\n", Util.getMainWorkerExecutor()));
      }
   }

   private void dumpGamerules(Path path) throws IOException {
      try (Writer writer = Files.newBufferedWriter(path)) {
         final List<String> list = Lists.newArrayList();
         final GameRules gameRules = this.saveProperties.getGameRules();
         gameRules.accept(new GameRuleVisitor() {
            @Override
            public <T> void visit(GameRule<T> rule) {
               list.add(String.format(Locale.ROOT, "%s=%s\n", rule.getId(), gameRules.getRuleValueName(rule)));
            }
         });

         for (String string : list) {
            writer.write(string);
         }
      }
   }

   private void dumpClasspath(Path path) throws IOException {
      try (Writer writer = Files.newBufferedWriter(path)) {
         String string = System.getProperty("java.class.path");
         String string2 = File.pathSeparator;

         for (String string3 : Splitter.on(string2).split(string)) {
            writer.write(string3);
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
            writer.write(10);
         }
      }
   }

   private void dumpNativeModules(Path path) throws IOException {
      try (Writer writer = Files.newBufferedWriter(path)) {
         List<WinNativeModuleUtil.NativeModule> list;
         try {
            list = Lists.newArrayList(WinNativeModuleUtil.collectNativeModules());
         } catch (Throwable var7) {
            LOGGER.warn("Failed to list native modules", var7);
            return;
         }

         list.sort(Comparator.comparing(module -> module.path));

         for (WinNativeModuleUtil.NativeModule nativeModule : list) {
            writer.write(nativeModule.toString());
            writer.write(10);
         }
      }
   }

   private Profiler startTickMetrics() {
      if (this.needsRecorderSetup) {
         this.recorder = DebugRecorder.of(
            new ServerSamplerSource(Util.nanoTimeSupplier, this.isDedicated()),
            Util.nanoTimeSupplier,
            Util.getIoWorkerExecutor(),
            new RecordDumper("server"),
            this.recorderResultConsumer,
            path -> {
               this.submitAndJoin(() -> this.dump(path.resolve("server")));
               this.recorderDumpConsumer.accept(path);
            }
         );
         this.needsRecorderSetup = false;
      }

      this.recorder.startTick();
      return TickDurationMonitor.tickProfiler(this.recorder.getProfiler(), TickDurationMonitor.create("Server"));
   }

   public void endTickMetrics() {
      this.recorder.endTick();
   }

   public boolean isRecorderActive() {
      return this.recorder.isActive();
   }

   public void setupRecorder(Consumer<ProfileResult> resultConsumer, Consumer<Path> dumpConsumer) {
      this.recorderResultConsumer = result -> {
         this.resetRecorder();
         resultConsumer.accept(result);
      };
      this.recorderDumpConsumer = dumpConsumer;
      this.needsRecorderSetup = true;
   }

   public void resetRecorder() {
      this.recorder = DummyRecorder.INSTANCE;
   }

   public void stopRecorder() {
      this.recorder.stop();
   }

   public void forceStopRecorder() {
      this.recorder.forceStop();
   }

   public Path getSavePath(WorldSavePath worldSavePath) {
      return this.session.getDirectory(worldSavePath);
   }

   public boolean syncChunkWrites() {
      return true;
   }

   public StructureTemplateManager getStructureTemplateManager() {
      return this.structureTemplateManager;
   }

   public SaveProperties getSaveProperties() {
      return this.saveProperties;
   }

   public DynamicRegistryManager.Immutable getRegistryManager() {
      return this.combinedDynamicRegistries.getCombinedRegistryManager();
   }

   public CombinedDynamicRegistries<ServerDynamicRegistryType> getCombinedDynamicRegistries() {
      return this.combinedDynamicRegistries;
   }

   public ReloadableRegistries.Lookup getReloadableRegistries() {
      return this.resourceManagerHolder.dataPackContents.getReloadableRegistries();
   }

   public TextStream createFilterer(ServerPlayerEntity player) {
      return TextStream.UNFILTERED;
   }

   public ServerPlayerInteractionManager getPlayerInteractionManager(ServerPlayerEntity player) {
      return (ServerPlayerInteractionManager)(this.isDemo() ? new DemoServerPlayerInteractionManager(player) : new ServerPlayerInteractionManager(player));
   }

   public @Nullable GameMode getForcedGameMode() {
      return null;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManagerHolder.resourceManager;
   }

   public boolean isSaving() {
      return this.saving;
   }

   public boolean isDebugRunning() {
      return this.needsDebugSetup || this.debugStart != null;
   }

   public void startDebug() {
      this.needsDebugSetup = true;
   }

   public ProfileResult stopDebug() {
      if (this.debugStart == null) {
         return EmptyProfileResult.INSTANCE;
      } else {
         ProfileResult profileResult = this.debugStart.end(Util.getMeasuringTimeNano(), this.ticks);
         this.debugStart = null;
         return profileResult;
      }
   }

   public int getMaxChainedNeighborUpdates() {
      return 1000000;
   }

   public void logChatMessage(Text message, MessageType.Parameters params, @Nullable String prefix) {
      String string = params.applyChatDecoration(message).getString();
      if (prefix != null) {
         LOGGER.info("[{}] {}", prefix, string);
      } else {
         LOGGER.info("{}", string);
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
      return this.chunkLoadProgress;
   }

   public boolean setAutosave(boolean autosave) {
      boolean bl = false;

      for (ServerWorld serverWorld : this.getWorlds()) {
         if (serverWorld != null && serverWorld.savingDisabled == autosave) {
            serverWorld.savingDisabled = !autosave;
            bl = true;
         }
      }

      return bl;
   }

   public boolean getAutosave() {
      for (ServerWorld serverWorld : this.getWorlds()) {
         if (serverWorld != null && !serverWorld.savingDisabled) {
            return true;
         }
      }

      return false;
   }

   public <T> void onGameRuleUpdated(GameRule<T> gameRule, T object) {
      this.getManagementListener().onGameRuleUpdated(gameRule, object);
      if (gameRule == GameRules.REDUCED_DEBUG_INFO) {
         byte b = (byte)((Boolean)object ? 22 : 23);

         for (ServerPlayerEntity serverPlayerEntity : this.getPlayerManager().getPlayerList()) {
            serverPlayerEntity.networkHandler.sendPacket(new EntityStatusS2CPacket(serverPlayerEntity, b));
         }
      } else if (gameRule == GameRules.LIMITED_CRAFTING || gameRule == GameRules.DO_IMMEDIATE_RESPAWN) {
         GameStateChangeS2CPacket.Reason reason = gameRule == GameRules.LIMITED_CRAFTING
            ? GameStateChangeS2CPacket.LIMITED_CRAFTING_TOGGLED
            : GameStateChangeS2CPacket.IMMEDIATE_RESPAWN;
         GameStateChangeS2CPacket gameStateChangeS2CPacket = new GameStateChangeS2CPacket(reason, (Boolean)object ? 1.0F : 0.0F);
         this.getPlayerManager().getPlayerList().forEach(serverPlayerEntityx -> serverPlayerEntityx.networkHandler.sendPacket(gameStateChangeS2CPacket));
      } else if (gameRule == GameRules.LOCATOR_BAR) {
         this.getWorlds().forEach(serverWorld -> {
            ServerWaypointHandler serverWaypointHandler = serverWorld.getWaypointHandler();
            if ((Boolean)object) {
               serverWorld.getPlayers().forEach(serverWaypointHandler::updatePlayerPos);
            } else {
               serverWaypointHandler.clear();
            }
         });
      } else if (gameRule == GameRules.SPAWN_MONSTERS) {
         this.updateMobSpawnOptions();
      }
   }

   public boolean acceptsTransfers() {
      return false;
   }

   private void writeChunkIoReport(CrashReport report, ChunkPos pos, StorageKey key) {
      Util.getIoWorkerExecutor().execute(() -> {
         try {
            Path path = this.getPath("debug");
            PathUtil.createDirectories(path);
            String string = PathUtil.replaceInvalidChars(key.level());
            Path path2 = path.resolve("chunk-" + string + "-" + Util.getFormattedCurrentTime() + "-server.txt");
            FileStore fileStore = Files.getFileStore(path);
            long l = fileStore.getUsableSpace();
            if (l < 8192L) {
               LOGGER.warn("Not storing chunk IO report due to low space on drive {}", fileStore.name());
               return;
            }

            CrashReportSection crashReportSection = report.addElement("Chunk Info");
            crashReportSection.add("Level", key::level);
            crashReportSection.add("Dimension", () -> key.dimension().getValue().toString());
            crashReportSection.add("Storage", key::type);
            crashReportSection.add("Position", pos::toString);
            report.writeToFile(path2, ReportType.MINECRAFT_CHUNK_IO_ERROR_REPORT);
            LOGGER.info("Saved details to {}", report.getFile());
         } catch (Exception var11) {
            LOGGER.warn("Failed to store chunk IO exception", var11);
         }
      });
   }

   @Override
   public void onChunkLoadFailure(Throwable exception, StorageKey key, ChunkPos chunkPos) {
      LOGGER.error("Failed to load chunk {},{}", new Object[]{chunkPos.x, chunkPos.z, exception});
      this.suppressedExceptionsTracker.onSuppressedException("chunk/load", exception);
      this.writeChunkIoReport(CrashReport.create(exception, "Chunk load failure"), chunkPos, key);
   }

   @Override
   public void onChunkSaveFailure(Throwable exception, StorageKey key, ChunkPos chunkPos) {
      LOGGER.error("Failed to save chunk {},{}", new Object[]{chunkPos.x, chunkPos.z, exception});
      this.suppressedExceptionsTracker.onSuppressedException("chunk/save", exception);
      this.writeChunkIoReport(CrashReport.create(exception, "Chunk save failure"), chunkPos, key);
   }

   public void onPacketException(Throwable exception, PacketType<?> type) {
      this.suppressedExceptionsTracker.onSuppressedException("packet/" + type, exception);
   }

   public BrewingRecipeRegistry getBrewingRecipeRegistry() {
      return this.brewingRecipeRegistry;
   }

   public FuelRegistry getFuelRegistry() {
      return this.fuelRegistry;
   }

   public ServerLinks getServerLinks() {
      return ServerLinks.EMPTY;
   }

   protected int getPauseWhenEmptySeconds() {
      return 0;
   }

   public PacketApplyBatcher getPacketApplyBatcher() {
      return this.packetApplyBatcher;
   }

   public SubscriberTracker getSubscriberTracker() {
      return this.subscriberTracker;
   }

   @Override
   public <T> T getOrThrow(DataResourceStore.Key<T> key) {
      return this.dataResourceStore.getOrThrow(key);
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

   record ResourceManagerHolder(LifecycledResourceManager resourceManager, DataPackContents dataPackContents) implements AutoCloseable {

      @Override
      public void close() {
         this.resourceManager.close();
      }
   }

   public record ServerResourcePackProperties(UUID id, String url, String hash, boolean isRequired, @Nullable Text prompt) {
   }
}
