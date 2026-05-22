package net.minecraft.test;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import net.minecraft.command.argument.RegistrySelectorArgumentType;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.datafixer.Schemas;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.dedicated.management.listener.BlankManagementListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.*;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.ReportType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import net.minecraft.util.profiler.log.DebugSampleLog;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.chunk.LoggingChunkLoadProgress;
import net.minecraft.world.debug.gizmo.GizmoCollector;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

/**
 * Специализированный сервер для запуска игровых тестов в headless-режиме.
 * Создаёт плоский мир, размещает тесты в случайной позиции и завершает процесс
 * с кодом, равным количеству упавших обязательных тестов.
 */
public class TestServer extends MinecraftServer {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int RESULT_STRING_LOG_INTERVAL = 20;
	private static final int TEST_POS_XZ_RANGE = 14999992;
	/** Y-координата спавна тестов: достаточно низко, чтобы не мешать небу. */
	private static final int TEST_SPAWN_Y = -59;
	private static final int VERIFICATION_RUNS_PER_ROTATION = 100;

	private static final ApiServices NONE_API_SERVICES = new ApiServices(
		null,
		ServicesKeySet.EMPTY,
		null,
		new DummyNameToIdCache(),
		new DummyGameProfileResolver()
	);
	private static final FeatureSet ENABLED_FEATURES = FeatureFlags.FEATURE_MANAGER
		.getFeatureSet()
		.subtract(FeatureSet.of(FeatureFlags.REDSTONE_EXPERIMENTS, FeatureFlags.MINECART_IMPROVEMENTS));
	private static final GeneratorOptions TEST_LEVEL = new GeneratorOptions(0L, false, false);

	private final MultiValueDebugSampleLogImpl debugSampleLog = new MultiValueDebugSampleLogImpl(4);
	private final Optional<String> tests;
	private final boolean verify;
	private List<GameTestBatch> batches = new ArrayList<>();
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private @Nullable TestSet testSet;

	/**
	 * Создаёт и инициализирует тестовый сервер: загружает датапаки, создаёт плоский мир.
	 *
	 * @param tests   селектор тестов (пустой = все тесты)
	 * @param verify  если true — каждый тест запускается {@value VERIFICATION_RUNS_PER_ROTATION} раз
	 *                для каждого из 4 поворотов структуры
	 */
	public static TestServer create(
		Thread thread,
		LevelStorage.Session session,
		ResourcePackManager resourcePackManager,
		Optional<String> tests,
		boolean verify
	) {
		resourcePackManager.scanPacks();
		ArrayList<String> packIds = new ArrayList<>(resourcePackManager.getIds());
		packIds.remove("vanilla");
		packIds.addFirst("vanilla");

		DataConfiguration dataConfiguration = new DataConfiguration(
			new DataPackSettings(packIds, List.of()),
			ENABLED_FEATURES
		);
		LevelInfo levelInfo = new LevelInfo(
			"Test Level",
			GameMode.CREATIVE,
			false,
			Difficulty.NORMAL,
			true,
			new GameRules(ENABLED_FEATURES),
			dataConfiguration
		);
		SaveLoading.DataPacks dataPacks = new SaveLoading.DataPacks(
			resourcePackManager,
			dataConfiguration,
			false,
			true
		);
		SaveLoading.ServerConfig serverConfig = new SaveLoading.ServerConfig(
			dataPacks,
			CommandManager.RegistrationEnvironment.DEDICATED,
			LeveledPermissionPredicate.OWNERS
		);

		try {
			LOGGER.debug("Starting resource loading");
			Stopwatch loadStopwatch = Stopwatch.createStarted();
			SaveLoader saveLoader = Util.<SaveLoader>waitAndApply(
				executor -> SaveLoading.load(
					serverConfig,
					context -> {
						Registry<DimensionOptions> dimensionRegistry =
							new SimpleRegistry<>(RegistryKeys.DIMENSION, Lifecycle.stable()).freeze();
						DimensionOptionsRegistryHolder.DimensionsConfig dimensionsConfig =
							context.worldGenRegistryManager()
								.getOrThrow(RegistryKeys.WORLD_PRESET)
								.getOrThrow(WorldPresets.FLAT)
								.value()
								.createDimensionsRegistryHolder()
								.toConfig(dimensionRegistry);
						return new SaveLoading.LoadContext<>(
							new LevelProperties(
								levelInfo,
								TEST_LEVEL,
								dimensionsConfig.specialWorldProperty(),
								dimensionsConfig.getLifecycle()
							),
							dimensionsConfig.toDynamicRegistryManager()
						);
					},
					SaveLoader::new,
					Util.getMainWorkerExecutor(),
					executor
				)
			).get();
			loadStopwatch.stop();
			LOGGER.debug("Finished resource loading after {} ms", loadStopwatch.elapsed(TimeUnit.MILLISECONDS));
			return new TestServer(thread, session, resourcePackManager, saveLoader, tests, verify);
		} catch (Exception ex) {
			LOGGER.warn("Failed to load vanilla datapack, bit oops", ex);
			System.exit(-1);
			throw new IllegalStateException();
		}
	}

	private TestServer(
		Thread serverThread,
		LevelStorage.Session session,
		ResourcePackManager dataPackManager,
		SaveLoader saveLoader,
		Optional<String> tests,
		boolean verify
	) {
		super(
			serverThread,
			session,
			dataPackManager,
			saveLoader,
			Proxy.NO_PROXY,
			Schemas.getFixer(),
			NONE_API_SERVICES,
			LoggingChunkLoadProgress.withoutPlayer()
		);
		this.tests = tests;
		this.verify = verify;
	}

	@Override
	public boolean setupServer() {
		setPlayerManager(new PlayerManager(
			this,
			getCombinedDynamicRegistries(),
			saveHandler,
			new BlankManagementListener()
		) {});
		GizmoDrawing.using(GizmoCollector.EMPTY);
		loadWorld();
		batches = batch(getOverworld());
		LOGGER.info("Started game test server");
		return true;
	}

	private List<GameTestBatch> batch(ServerWorld world) {
		Registry<TestInstance> registry = world.getRegistryManager().getOrThrow(RegistryKeys.TEST_INSTANCE);
		Collection<RegistryEntry.Reference<TestInstance>> instances;
		Batches.Decorator decorator;

		if (tests.isPresent()) {
			instances = selectInstances(world.getRegistryManager(), tests.get())
				.filter(instance -> !instance.value().isManualOnly())
				.toList();

			if (verify) {
				decorator = TestServer::makeVerificationBatches;
				LOGGER.info(
					"Verify requested. Will run each test that matches {} {} times",
					tests.get(),
					VERIFICATION_RUNS_PER_ROTATION * BlockRotation.values().length
				);
			} else {
				decorator = Batches.DEFAULT_DECORATOR;
				LOGGER.info("Will run tests matching {} ({} tests)", tests.get(), instances.size());
			}
		} else {
			instances = registry.streamEntries()
				.filter(instance -> !instance.value().isManualOnly())
				.toList();
			decorator = Batches.DEFAULT_DECORATOR;
		}

		return Batches.batch(instances, decorator, world);
	}

	private static Stream<GameTestState> makeVerificationBatches(
		RegistryEntry.Reference<TestInstance> instance,
		ServerWorld world
	) {
		Builder<GameTestState> builder = Stream.builder();

		for (BlockRotation rotation : BlockRotation.values()) {
			for (int run = 0; run < VERIFICATION_RUNS_PER_ROTATION; run++) {
				builder.add(new GameTestState(instance, rotation, world, TestAttemptConfig.once()));
			}
		}

		return builder.build();
	}

	public static Stream<RegistryEntry.Reference<TestInstance>> selectInstances(
		DynamicRegistryManager registryManager,
		String selector
	) {
		return RegistrySelectorArgumentType
			.select(new StringReader(selector), registryManager.getOrThrow(RegistryKeys.TEST_INSTANCE))
			.stream();
	}

	@Override
	public void tick(BooleanSupplier shouldKeepTicking) {
		super.tick(shouldKeepTicking);
		ServerWorld overworld = getOverworld();

		if (!isTesting()) {
			runTestBatches(overworld);
		}

		if (overworld.getTime() % RESULT_STRING_LOG_INTERVAL == 0L) {
			LOGGER.info(testSet.getResultString());
		}

		if (testSet.isDone()) {
			stop(false);
			LOGGER.info(testSet.getResultString());
			TestFailureLogger.stop();
			LOGGER.info(
				"========= {} GAME TESTS COMPLETE IN {} ======================",
				testSet.getTestCount(),
				stopwatch.stop()
			);

			if (testSet.failed()) {
				LOGGER.info("{} required tests failed :(", testSet.getFailedRequiredTestCount());
				testSet.getRequiredTests().forEach(TestServer::logFailure);
			} else {
				LOGGER.info("All {} required tests passed :)", testSet.getTestCount());
			}

			if (testSet.hasFailedOptionalTests()) {
				LOGGER.info("{} optional tests failed", testSet.getFailedOptionalTestCount());
				testSet.getOptionalTests().forEach(TestServer::logFailure);
			}

			LOGGER.info("====================================================");
		}
	}

	private static void logFailure(GameTestState state) {
		if (state.getRotation() != BlockRotation.NONE) {
			LOGGER.info(
				"   - {} with rotation {}: {}",
				state.getId(),
				state.getRotation().asString(),
				state.getThrowable().getText().getString()
			);
		} else {
			LOGGER.info("   - {}: {}", state.getId(), state.getThrowable().getText().getString());
		}
	}

	@Override
	public DebugSampleLog getDebugSampleLog() {
		return debugSampleLog;
	}

	@Override
	public boolean shouldPushTickTimeLog() {
		return false;
	}

	@Override
	public void runTasksTillTickEnd() {
		runTasks();
	}

	@Override
	public SystemDetails addExtraSystemDetails(SystemDetails details) {
		details.addSection("Type", "Game test server");
		return details;
	}

	@Override
	public void exit() {
		super.exit();
		LOGGER.info("Game test server shutting down");
		System.exit(testSet != null ? testSet.getFailedRequiredTestCount() : -1);
	}

	@Override
	public void setCrashReport(CrashReport report) {
		super.setCrashReport(report);
		LOGGER.error("Game test server crashed\n{}", report.asString(ReportType.MINECRAFT_CRASH_REPORT));
		System.exit(1);
	}

	private void runTestBatches(ServerWorld world) {
		BlockPos spawnPos = new BlockPos(
			world.random.nextBetween(-TEST_POS_XZ_RANGE, TEST_POS_XZ_RANGE),
			TEST_SPAWN_Y,
			world.random.nextBetween(-TEST_POS_XZ_RANGE, TEST_POS_XZ_RANGE)
		);
		world.setSpawnPoint(WorldProperties.SpawnPoint.create(world.getRegistryKey(), spawnPos, 0.0F, 0.0F));

		TestRunContext runContext = TestRunContext.Builder
			.of(batches, world)
			.initialSpawner(new TestStructurePlacer(spawnPos, 8, false))
			.build();

		Collection<GameTestState> allStates = runContext.getStates();
		testSet = new TestSet(allStates);
		LOGGER.info("{} tests are now running at position {}!", testSet.getTestCount(), spawnPos.toShortString());
		stopwatch.reset();
		stopwatch.start();
		runContext.start();
	}

	private boolean isTesting() {
		return testSet != null;
	}

	@Override
	public boolean isHardcore() {
		return false;
	}

	@Override
	public LeveledPermissionPredicate getOpPermissionLevel() {
		return LeveledPermissionPredicate.ALL;
	}

	@Override
	public PermissionPredicate getFunctionPermissions() {
		return LeveledPermissionPredicate.OWNERS;
	}

	@Override
	public boolean shouldBroadcastRconToOps() {
		return false;
	}

	@Override
	public boolean isDedicated() {
		return false;
	}

	@Override
	public int getRateLimit() {
		return 0;
	}

	@Override
	public boolean isUsingNativeTransport() {
		return false;
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public boolean shouldBroadcastConsoleToOps() {
		return false;
	}

	@Override
	public boolean isHost(PlayerConfigEntry player) {
		return false;
	}

	@Override
	public int getMaxPlayerCount() {
		return 1;
	}

	static class DummyGameProfileResolver implements GameProfileResolver {

		@Override
		public Optional<GameProfile> getProfileByName(String name) {
			return Optional.empty();
		}

		@Override
		public Optional<GameProfile> getProfileById(UUID id) {
			return Optional.empty();
		}
	}

	static class DummyNameToIdCache implements NameToIdCache {

		private final Set<PlayerConfigEntry> players = new HashSet<>();

		@Override
		public void add(PlayerConfigEntry player) {
			players.add(player);
		}

		@Override
		public Optional<PlayerConfigEntry> findByName(String name) {
			return players.stream()
				.filter(player -> player.name().equals(name))
				.findFirst()
				.or(() -> Optional.of(PlayerConfigEntry.fromNickname(name)));
		}

		@Override
		public Optional<PlayerConfigEntry> getByUuid(UUID uuid) {
			return players.stream().filter(player -> player.id().equals(uuid)).findFirst();
		}

		@Override
		public void setOfflineMode(boolean offlineMode) {
		}

		@Override
		public void save() {
		}
	}
}
