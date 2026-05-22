package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FreeTypeUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlTimer;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import net.minecraft.client.gui.hud.debug.chart.PieChart;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.network.*;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.client.option.*;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleSpriteManager;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsPeriodicCheckers;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.resource.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.client.resource.waypoint.WaypointStyleAssetManager;
import net.minecraft.client.session.Bans;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.session.telemetry.GameLoadTimeEvent;
import net.minecraft.client.session.telemetry.TelemetryEventProperty;
import net.minecraft.client.session.telemetry.TelemetryManager;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.*;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.client.tutorial.TutorialManager;
import net.minecraft.client.util.*;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.datafixer.Schemas;
import net.minecraft.dialog.Dialogs;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketApplyBatcher;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DialogTags;
import net.minecraft.resource.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.GameProfileResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.MusicType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.KeybindTranslations;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.crash.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.path.SymlinkFinder;
import net.minecraft.util.profiler.*;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.World;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.chunk.ChunkLoadProgress;
import net.minecraft.world.chunk.LoggingChunkLoadProgress;
import net.minecraft.world.debug.gizmo.GizmoCollectorImpl;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.tick.TickManager;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Главный класс клиента Minecraft.
 * <p>Является точкой входа клиентской части игры: управляет игровым циклом,
 * рендерингом, экранами, звуком, сетевым подключением и всеми подсистемами клиента.</p>
 * <p>Доступен как синглтон через {@link MinecraftClient#getInstance()}.</p>
 */
@Environment(EnvType.CLIENT)
public class MinecraftClient extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {

	static MinecraftClient instance;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_TICKS_PER_FRAME = 10;
	private static final int PANORAMA_SIZE = 4096;
	private static final int PANORAMA_FACE_COUNT = 6;
	private static final int PANORAMA_DOWNSCALE_FACTOR = 4;
	private static final int MAX_FPS_LIMIT = 260;
	private static final int ITEM_USE_COOLDOWN_TICKS = 4;
	private static final int SOCIAL_INTERACTIONS_TOAST_DURATION_MS = 8000;
	public static final Identifier DEFAULT_FONT_ID = Identifier.ofVanilla("default");
	public static final Identifier UNICODE_FONT_ID = Identifier.ofVanilla("uniform");
	public static final Identifier ALT_TEXT_RENDERER_ID = Identifier.ofVanilla("alt");
	private static final Identifier REGIONAL_COMPLIANCIES_ID = Identifier.ofVanilla("regional_compliancies.json");
	private static final CompletableFuture<Unit> COMPLETED_UNIT_FUTURE = CompletableFuture.completedFuture(Unit.INSTANCE);
	private static final Text SOCIAL_INTERACTIONS_NOT_AVAILABLE = Text.translatable("multiplayer.socialInteractions.not_available");
	private static final Text SAVING_LEVEL_TEXT = Text.translatable("menu.savingLevel");
	public static final String GL_ERROR_DIALOGUE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
	/** Значение кулдауна атаки при открытом экране — блокирует любые атаки. */
	private static final int SCREEN_ATTACK_COOLDOWN = 10000;
	private static final long UNIVERSE = Double.doubleToLongBits(Math.PI);
	private final Path resourcePackDir;
	private final CompletableFuture<@Nullable ProfileResult> gameProfileFuture;
	private final TextureManager textureManager;
	private final ShaderLoader shaderLoader;
	private final DataFixer dataFixer;
	private final WindowProvider windowProvider;
	private final Window window;
	private final RenderTickCounter.Dynamic
			renderTickCounter =
			new RenderTickCounter.Dynamic(20.0F, 0L, this::getTargetMillisPerTick);
	private final BufferBuilderStorage bufferBuilders;
	public final WorldRenderer worldRenderer;
	private final EntityRenderManager entityRenderManager;
	private final ItemModelManager itemModelManager;
	private final ItemRenderer itemRenderer;
	private final MapRenderer mapRenderer;
	public final ParticleManager particleManager;
	private final ParticleSpriteManager particleSpriteManager;
	private final Session session;
	public final TextRenderer textRenderer;
	public final TextRenderer advanceValidatingTextRenderer;
	public final GameRenderer gameRenderer;
	public final InGameHud inGameHud;
	public final GameOptions options;
	public final DebugHudProfile debugHudEntryList;
	private final HotbarStorage creativeHotbarStorage;
	public final Mouse mouse;
	public final Keyboard keyboard;
	private GuiNavigationType navigationType = GuiNavigationType.NONE;
	public final File runDirectory;
	private final String gameVersion;
	private final String versionType;
	private final Proxy networkProxy;
	private final boolean offlineDeveloperMode;
	private final LevelStorage levelStorage;
	private final boolean isDemo;
	private final boolean multiplayerEnabled;
	private final boolean onlineChatEnabled;
	private final ReloadableResourceManagerImpl resourceManager;
	private final DefaultResourcePack defaultResourcePack;
	private final ServerResourcePackLoader serverResourcePackLoader;
	private final ResourcePackManager resourcePackManager;
	private final LanguageManager languageManager;
	private final BlockColors blockColors;
	private final Framebuffer framebuffer;
	private final @Nullable TracyFrameCapturer tracyFrameCapturer;
	private final SoundManager soundManager;
	private final MusicTracker musicTracker;
	private final FontManager fontManager;
	private final SplashTextResourceSupplier splashTextLoader;
	private final VideoWarningManager videoWarningManager;
	private final PeriodicNotificationManager regionalComplianciesManager = new PeriodicNotificationManager(
			REGIONAL_COMPLIANCIES_ID, MinecraftClient::isCountrySetTo
	);
	private final UserApiService userApiService;
	private final CompletableFuture<UserProperties> userPropertiesFuture;
	private final PlayerSkinProvider skinProvider;
	private final AtlasManager atlasManager;
	private final BakedModelManager bakedModelManager;
	private final BlockRenderManager blockRenderManager;
	private final MapTextureManager mapTextureManager;
	private final WaypointStyleAssetManager waypointStyleAssetManager;
	private final ToastManager toastManager;
	private final TutorialManager tutorialManager;
	private final SocialInteractionsManager socialInteractionsManager;
	private final BlockEntityRenderManager blockEntityRenderManager;
	private final TelemetryManager telemetryManager;
	private final ProfileKeys profileKeys;
	private final RealmsPeriodicCheckers realmsPeriodicCheckers;
	private final QuickPlayLogger quickPlayLogger;
	private final ApiServices apiServices;
	private final PlayerSkinCache playerSkinCache;
	public @Nullable ClientPlayerInteractionManager interactionManager;
	public @Nullable ClientWorld world;
	public @Nullable ClientPlayerEntity player;
	private @Nullable IntegratedServer server;
	private @Nullable ClientConnection integratedServerConnection;
	private boolean integratedServerRunning;
	private @Nullable Entity cameraEntity;
	public @Nullable Entity targetedEntity;
	public @Nullable HitResult crosshairTarget;
	private int itemUseCooldown;
	public int attackCooldown;
	private volatile boolean paused;
	private long lastMetricsSampleTime = Util.getMeasuringTimeNano();
	private long nextDebugInfoUpdateTime;
	private int fpsCounter;
	public boolean skipGameRender;
	public @Nullable Screen currentScreen;
	private @Nullable Overlay overlay;
	private boolean disconnecting;
	Thread thread;
	private volatile boolean running;
	private @Nullable Supplier<CrashReport> crashReportSupplier;
	private static int currentFps;
	private long renderTime;
	private final InactivityFpsLimiter inactivityFpsLimiter;
	public boolean wireFrame;
	public boolean chunkCullingEnabled = true;
	private boolean windowFocused;
	private @Nullable CompletableFuture<Void> resourceReloadFuture;
	private @Nullable TutorialToast socialInteractionsToast;
	private int trackingTick;
	private final TickTimeTracker tickTimeTracker;
	private Recorder recorder = DummyRecorder.INSTANCE;
	private final ResourceReloadLogger resourceReloadLogger = new ResourceReloadLogger();
	private long metricsSampleDuration;
	private double gpuUtilizationPercentage;
	private GlTimer.@Nullable Query currentGlTimerQuery;
	private final NarratorManager narratorManager;
	private final MessageHandler messageHandler;
	private AbuseReportContext abuseReportContext;
	private final CommandHistoryManager commandHistoryManager;
	private final SymlinkFinder symlinkFinder;
	private boolean finishedLoading;
	private final long startTime;
	private long uptimeInTicks;
	private final PacketApplyBatcher packetApplyBatcher;
	private final GizmoCollectorImpl gizmoCollector = new GizmoCollectorImpl();
	private List<GizmoCollectorImpl.Entry> gizmos = new ArrayList<>();

	public MinecraftClient(RunArgs args) {
		super("Client");
		instance = this;
		startTime = System.currentTimeMillis();
		runDirectory = args.directories.runDir;
		File assetDir = args.directories.assetDir;
		resourcePackDir = args.directories.resourcePackDir.toPath();
		gameVersion = args.game.version;
		versionType = args.game.versionType;
		Path runPath = runDirectory.toPath();
		symlinkFinder = LevelStorage.createSymlinkFinder(runPath.resolve("allowed_symlinks.txt"));
		DefaultClientResourcePackProvider defaultClientResourcePackProvider = new DefaultClientResourcePackProvider(
			args.directories.getAssetDir(), symlinkFinder
		);
		serverResourcePackLoader = new ServerResourcePackLoader(this, runPath.resolve("downloads"), args.network);
		ResourcePackProvider resourcePackProvider = new FileResourcePackProvider(
			resourcePackDir, ResourceType.CLIENT_RESOURCES, ResourcePackSource.NONE, symlinkFinder
		);
		resourcePackManager = new ResourcePackManager(
			defaultClientResourcePackProvider,
			serverResourcePackLoader.getPassthroughPackProvider(),
			resourcePackProvider
		);
		defaultResourcePack = defaultClientResourcePackProvider.getResourcePack();
		networkProxy = args.network.netProxy;
		offlineDeveloperMode = args.game.offlineDeveloperMode;
		YggdrasilAuthenticationService yggdrasilAuthService = offlineDeveloperMode
			? YggdrasilAuthenticationService.createOffline(networkProxy)
			: new YggdrasilAuthenticationService(networkProxy);
		apiServices = ApiServices.create(yggdrasilAuthService, runDirectory);
		session = args.network.session;
		gameProfileFuture = offlineDeveloperMode
			? CompletableFuture.<ProfileResult>completedFuture(null)
			: CompletableFuture.supplyAsync(
				() -> apiServices.sessionService().fetchProfile(session.getUuidOrNull(), true),
				Util.getDownloadWorkerExecutor()
			);
		userApiService = createUserApiService(yggdrasilAuthService, args);
		userPropertiesFuture = CompletableFuture.supplyAsync(
			() -> {
				try {
					return userApiService.fetchProperties();
				} catch (AuthenticationException exception) {
					LOGGER.error("Failed to fetch user properties", exception);
					return UserApiService.OFFLINE_PROPERTIES;
				}
			},
			Util.getDownloadWorkerExecutor()
		);
		LOGGER.info("Setting user: {}", session.getUsername());
		LOGGER.debug("(Session ID is {})", session.getSessionId());
		isDemo = args.game.demo;
		multiplayerEnabled = !args.game.multiplayerDisabled;
		onlineChatEnabled = !args.game.onlineChatDisabled;
		server = null;
		KeybindTranslations.setFactory(KeyBinding::getLocalizedName);
		dataFixer = Schemas.getFixer();
		thread = Thread.currentThread();
		options = new GameOptions(this, runDirectory);
		debugHudEntryList = new DebugHudProfile(runDirectory);
		toastManager = new ToastManager(this, options);
		boolean startedCleanly = options.startedCleanly;
		options.startedCleanly = false;
		options.write();
		running = true;
		tutorialManager = new TutorialManager(this, options);
		creativeHotbarStorage = new HotbarStorage(runPath, dataFixer);
		LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
		WindowSettings windowSettings = args.windowSettings;
		if (options.overrideHeight > 0 && options.overrideWidth > 0) {
			windowSettings = args.windowSettings.withDimensions(options.overrideWidth, options.overrideHeight);
		}

		if (!startedCleanly) {
			windowSettings = windowSettings.withFullscreen(false);
			options.fullscreenResolution = null;
			LOGGER.warn("Detected unexpected shutdown during last game startup: resetting fullscreen mode");
		}

		Util.nanoTimeSupplier = RenderSystem.initBackendSystem();
		windowProvider = new WindowProvider(this);
		window = windowProvider.createWindow(windowSettings, options.fullscreenResolution, getWindowTitle());
		onWindowFocusChanged(true);
		window.setCloseCallback(new Runnable() {
			private boolean closed;

			@Override
			public void run() {
				if (!closed) {
					closed = true;
					ClientWatchdog.shutdownClient(args.directories.runDir, thread.threadId());
				}
			}
		});
		GameLoadTimeEvent.INSTANCE.stopTimer(TelemetryEventProperty.LOAD_TIME_PRE_WINDOW_MS);

		try {
			window.setIcon(
				defaultResourcePack,
				SharedConstants.getGameVersion().stable() ? Icons.RELEASE : Icons.SNAPSHOT
			);
		} catch (IOException exception) {
			LOGGER.error("Couldn't set icon", exception);
		}

		mouse = new Mouse(this);
		mouse.setup(window);
		keyboard = new Keyboard(this);
		keyboard.setup(window);
		RenderSystem.initRenderer(
			window.getHandle(),
			options.glDebugVerbosity,
			SharedConstants.SYNCHRONOUS_GL_LOGS,
			(id, type) -> getShaderLoader().getSource(id, type),
			args.game.renderDebugLabels
		);
		options.applyGraphicsMode(options.getPreset().getValue());
		LOGGER.info("Using optional rendering extensions: {}", String.join(", ", RenderSystem.getDevice().getEnabledExtensions()));
		framebuffer = new WindowFramebuffer(window.getFramebufferWidth(), window.getFramebufferHeight());
		resourceManager = new ReloadableResourceManagerImpl(ResourceType.CLIENT_RESOURCES);
		resourcePackManager.scanPacks();
		options.addResourcePackProfilesToManager(resourcePackManager);
		languageManager = new LanguageManager(
			options.language,
			translationStorage -> {
				if (player != null) {
					player.networkHandler.refreshSearchManager();
				}
			}
		);
		resourceManager.registerReloader(languageManager);
		textureManager = new TextureManager(resourceManager);
		resourceManager.registerReloader(textureManager);
		shaderLoader = new ShaderLoader(textureManager, this::onShaderResourceReloadFailure);
		resourceManager.registerReloader(shaderLoader);
		PlayerSkinTextureDownloader playerSkinTextureDownloader = new PlayerSkinTextureDownloader(networkProxy, textureManager, this);
		skinProvider = new PlayerSkinProvider(
			assetDir.toPath().resolve("skins"),
			apiServices,
			playerSkinTextureDownloader,
			this
		);
		levelStorage = new LevelStorage(runPath.resolve("saves"), runPath.resolve("backups"), symlinkFinder, dataFixer);
		commandHistoryManager = new CommandHistoryManager(runPath);
		musicTracker = new MusicTracker(this);
		soundManager = new SoundManager(options);
		resourceManager.registerReloader(soundManager);
		splashTextLoader = new SplashTextResourceSupplier(session);
		resourceManager.registerReloader(splashTextLoader);
		atlasManager = new AtlasManager(textureManager, options.getMipmapLevels().getValue());
		resourceManager.registerReloader(atlasManager);
		GameProfileResolver gameProfileResolver = new ClientPlayerProfileResolver(this, apiServices.profileResolver());
		playerSkinCache = new PlayerSkinCache(textureManager, skinProvider, gameProfileResolver);
		ClientMannequinEntity.setFactory(playerSkinCache);
		fontManager = new FontManager(textureManager, atlasManager, playerSkinCache);
		textRenderer = fontManager.createTextRenderer();
		advanceValidatingTextRenderer = fontManager.createAdvanceValidatingTextRenderer();
		resourceManager.registerReloader(fontManager);
		onFontOptionsChanged();
		resourceManager.registerReloader(new GrassColormapResourceSupplier());
		resourceManager.registerReloader(new FoliageColormapResourceSupplier());
		resourceManager.registerReloader(new DryFoliageColormapResourceSupplier());
		window.setPhase("Startup");
		RenderSystem.setupDefaultState();
		window.setPhase("Post startup");
		blockColors = BlockColors.create();
		bakedModelManager = new BakedModelManager(blockColors, atlasManager, playerSkinCache);
		resourceManager.registerReloader(bakedModelManager);
		EquipmentModelLoader equipmentModelLoader = new EquipmentModelLoader();
		resourceManager.registerReloader(equipmentModelLoader);
		itemModelManager = new ItemModelManager(bakedModelManager);
		itemRenderer = new ItemRenderer();
		mapTextureManager = new MapTextureManager(textureManager);
		mapRenderer = new MapRenderer(atlasManager, mapTextureManager);

		try {
			int processorCount = Runtime.getRuntime().availableProcessors();
			Tessellator.initialize();
			bufferBuilders = new BufferBuilderStorage(processorCount);
		} catch (OutOfMemoryError error) {
			TinyFileDialogs.tinyfd_messageBox(
				"Minecraft",
				"Oh no! The game was unable to allocate memory off-heap while trying to start. You may try to free some memory by closing other applications on your computer, check that your system meets the minimum requirements, and try again. If the problem persists, please visit: "
					+ Urls.MINECRAFT_SUPPORT,
				"ok",
				"error",
				true
			);
			throw new GlException("Unable to allocate render buffers", error);
		}

		socialInteractionsManager = new SocialInteractionsManager(this, userApiService);
		blockRenderManager = new BlockRenderManager(bakedModelManager.getBlockModels(), atlasManager, blockColors);
		resourceManager.registerReloader(blockRenderManager);
		entityRenderManager = new EntityRenderManager(
			this,
			textureManager,
			itemModelManager,
			mapRenderer,
			blockRenderManager,
			atlasManager,
			textRenderer,
			options,
			bakedModelManager.getEntityModelsSupplier(),
			equipmentModelLoader,
			playerSkinCache
		);
		resourceManager.registerReloader(entityRenderManager);
		blockEntityRenderManager = new BlockEntityRenderManager(
			textRenderer,
			bakedModelManager.getEntityModelsSupplier(),
			blockRenderManager,
			itemModelManager,
			itemRenderer,
			entityRenderManager,
			atlasManager,
			playerSkinCache
		);
		resourceManager.registerReloader(blockEntityRenderManager);
		particleSpriteManager = new ParticleSpriteManager();
		resourceManager.registerReloader(particleSpriteManager);
		particleManager = new ParticleManager(world, particleSpriteManager);
		particleSpriteManager.setOnPreparedTask(particleManager::clearParticles);
		waypointStyleAssetManager = new WaypointStyleAssetManager();
		resourceManager.registerReloader(waypointStyleAssetManager);
		gameRenderer = new GameRenderer(this, entityRenderManager.getHeldItemRenderer(), bufferBuilders, blockRenderManager);
		worldRenderer = new WorldRenderer(
			this,
			entityRenderManager,
			blockEntityRenderManager,
			bufferBuilders,
			gameRenderer.getEntityRenderStates(),
			gameRenderer.getEntityRenderDispatcher()
		);
		resourceManager.registerReloader(worldRenderer);
		resourceManager.registerReloader(worldRenderer.getCloudRenderer());
		videoWarningManager = new VideoWarningManager();
		resourceManager.registerReloader(videoWarningManager);
		resourceManager.registerReloader(regionalComplianciesManager);
		inGameHud = new InGameHud(this);
		RealmsClient realmsClient = RealmsClient.createRealmsClient(this);
		realmsPeriodicCheckers = new RealmsPeriodicCheckers(realmsClient);
		RenderSystem.setErrorCallback(this::handleGlErrorByDisableVsync);
		if (framebuffer.textureWidth != window.getFramebufferWidth()
			|| framebuffer.textureHeight != window.getFramebufferHeight()
		) {
			String resolutionMsg = "Recovering from unsupported resolution ("
				+ window.getFramebufferWidth() + "x" + window.getFramebufferHeight()
				+ ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
			StringBuilder messageBuilder = new StringBuilder(resolutionMsg);

			try {
				GpuDevice gpuDevice = RenderSystem.getDevice();
				List<String> debugMessages = gpuDevice.getLastDebugMessages();
				if (!debugMessages.isEmpty()) {
					messageBuilder.append("\n\nReported GL debug messages:\n").append(String.join("\n", debugMessages));
				}
			} catch (Throwable ignored) {
			}

			window.setWindowedSize(framebuffer.textureWidth, framebuffer.textureHeight);
			TinyFileDialogs.tinyfd_messageBox("Minecraft", messageBuilder.toString(), "ok", "error", false);
		} else if (options.getFullscreen().getValue() && !window.isFullscreen()) {
			if (startedCleanly) {
				window.toggleFullscreen();
				options.getFullscreen().setValue(window.isFullscreen());
			} else {
				options.getFullscreen().setValue(false);
			}
		}

		window.setVsync(options.getEnableVsync().getValue());
		window.setRawMouseMotion(options.getRawMouseInput().getValue());
		window.setAllowCursorChanges(options.getAllowCursorChanges().getValue());
		window.logOnGlError();
		onResolutionChanged();
		gameRenderer.preloadPrograms(defaultResourcePack.getFactory());
		telemetryManager = new TelemetryManager(this, userApiService, session);
		profileKeys = offlineDeveloperMode
			? ProfileKeys.MISSING
			: ProfileKeys.create(userApiService, session, runPath);
		narratorManager = new NarratorManager(this);
		narratorManager.checkNarratorLibrary(options.getNarrator().getValue() != NarratorMode.OFF);
		messageHandler = new MessageHandler(this);
		messageHandler.setChatDelay(options.getChatDelay().getValue());
		abuseReportContext = AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), userApiService);
		TitleScreen.registerTextures(textureManager);
		SplashOverlay.init(textureManager);
		gameRenderer.getRotatingPanoramaRenderer().registerTextures(textureManager);
		setScreen(new MessageScreen(Text.translatable("gui.loadingMinecraft")));
		List<ResourcePack> initialPacks = resourcePackManager.createResourcePacks();
		resourceReloadLogger.reload(ResourceReloadLogger.ReloadReason.INITIAL, initialPacks);
		ResourceReload resourceReload = resourceManager.reload(
			Util.getMainWorkerExecutor().named("resourceLoad"),
			this,
			COMPLETED_UNIT_FUTURE,
			initialPacks
		);
		GameLoadTimeEvent.INSTANCE.startTimer(TelemetryEventProperty.LOAD_TIME_LOADING_OVERLAY_MS);
		LoadingContext loadingContext = new LoadingContext(realmsClient, args.quickPlay);
		setOverlay(
			new SplashOverlay(
				this,
				resourceReload,
				error -> Util.ifPresentOrElse(
					error,
					throwable -> handleResourceReloadException(throwable, loadingContext),
					() -> {
						if (SharedConstants.isDevelopment) {
							checkGameData();
						}

						resourceReloadLogger.finish();
						onFinishedLoading(loadingContext);
					}
				),
				false
			)
		);
		quickPlayLogger = QuickPlayLogger.create(args.quickPlay.logPath());
		inactivityFpsLimiter = new InactivityFpsLimiter(options, this);
		tickTimeTracker = new TickTimeTracker(
			Util.nanoTimeSupplier,
			() -> trackingTick,
			inactivityFpsLimiter::shouldDisableProfilerTimeout
		);
		tracyFrameCapturer = TracyClient.isAvailable() && args.game.tracyEnabled
			? new TracyFrameCapturer()
			: null;
		packetApplyBatcher = new PacketApplyBatcher(thread);
	}

	public boolean isShiftPressed() {
		Window win = getWindow();
		return InputUtil.isKeyPressed(win, InputUtil.GLFW_KEY_LEFT_SHIFT)
			|| InputUtil.isKeyPressed(win, InputUtil.GLFW_KEY_RIGHT_SHIFT);
	}

	public boolean isCtrlPressed() {
		Window win = getWindow();
		return InputUtil.isKeyPressed(win, InputUtil.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(win, InputUtil.GLFW_KEY_RIGHT_CONTROL);
	}

	public boolean isAltPressed() {
		Window win = getWindow();
		return InputUtil.isKeyPressed(win, InputUtil.GLFW_KEY_LEFT_ALT)
			|| InputUtil.isKeyPressed(win, InputUtil.GLFW_KEY_RIGHT_ALT);
	}

	private void onFinishedLoading(MinecraftClient.@Nullable LoadingContext loadingContext) {
		if (!this.finishedLoading) {
			this.finishedLoading = true;
			this.collectLoadTimes(loadingContext);
		}
	}

	private void collectLoadTimes(MinecraftClient.@Nullable LoadingContext loadingContext) {
		Runnable runnable = this.onInitFinished(loadingContext);
		GameLoadTimeEvent.INSTANCE.stopTimer(TelemetryEventProperty.LOAD_TIME_LOADING_OVERLAY_MS);
		GameLoadTimeEvent.INSTANCE.stopTimer(TelemetryEventProperty.LOAD_TIME_TOTAL_TIME_MS);
		GameLoadTimeEvent.INSTANCE.send(this.telemetryManager.getSender());
		runnable.run();
		this.options.startedCleanly = true;
		this.options.write();
	}

	public boolean isFinishedLoading() {
		return this.finishedLoading;
	}

	private Runnable onInitFinished(MinecraftClient.@Nullable LoadingContext loadingContext) {
		List<Function<Runnable, Screen>> list = new ArrayList<>();
		boolean bl = this.createInitScreens(list);
		Runnable runnable = () -> {
			if (loadingContext != null && loadingContext.quickPlayData.isEnabled()) {
				QuickPlay.startQuickPlay(this, loadingContext.quickPlayData.variant(), loadingContext.realmsClient());
			}
			else {
				this.setScreen(new TitleScreen(true, new LogoDrawer(bl)));
			}
		};

		for (Function<Runnable, Screen> function : Lists.reverse(list)) {
			Screen screen = function.apply(runnable);
			runnable = () -> this.setScreen(screen);
		}

		return runnable;
	}

	private boolean createInitScreens(List<Function<Runnable, Screen>> list) {
		boolean bl = false;
		if (this.options.onboardAccessibility || SharedConstants.FORCE_ONBOARDING_SCREEN) {
			list.add(onClose -> new AccessibilityOnboardingScreen(this.options, onClose));
			bl = true;
		}

		BanDetails banDetails = this.getMultiplayerBanDetails();
		if (banDetails != null) {
			list.add(onClose -> Bans.createBanScreen(
					confirmed -> {
						if (confirmed) {
							Util.getOperatingSystem().open(Urls.JAVA_MODERATION);
						}

						onClose.run();
					}, banDetails
			));
		}

		ProfileResult profileResult = this.gameProfileFuture.join();
		if (profileResult != null) {
			GameProfile gameProfile = profileResult.profile();
			Set<ProfileActionType> set = profileResult.actions();
			if (set.contains(ProfileActionType.FORCED_NAME_CHANGE)) {
				list.add(onClose -> Bans.createUsernameBanScreen(gameProfile.name(), onClose));
			}

			if (set.contains(ProfileActionType.USING_BANNED_SKIN)) {
				list.add(Bans::createSkinBanScreen);
			}
		}

		return bl;
	}

	private static boolean isCountrySetTo(Object country) {
		try {
			return Locale.getDefault().getISO3Country().equals(country);
		}
		catch (MissingResourceException var2) {
			return false;
		}
	}

	public void updateWindowTitle() {
		window.setTitle(getWindowTitle());
	}

	private String getWindowTitle() {
		StringBuilder title = new StringBuilder("Minecraft");
		if (getModStatus().isModded()) {
			title.append("*");
		}

		title.append(" ").append(SharedConstants.getGameVersion().name());
		ClientPlayNetworkHandler networkHandler = getNetworkHandler();
		if (networkHandler == null || !networkHandler.getConnection().isOpen()) {
			return title.toString();
		}

		title.append(" - ");
		ServerInfo serverInfo = getCurrentServerEntry();
		if (server != null && !server.isRemote()) {
			title.append(I18n.translate("title.singleplayer"));
		} else if (serverInfo != null && serverInfo.isRealm()) {
			title.append(I18n.translate("title.multiplayer.realms"));
		} else if (server == null && (serverInfo == null || !serverInfo.isLocal())) {
			title.append(I18n.translate("title.multiplayer.other"));
		} else {
			title.append(I18n.translate("title.multiplayer.lan"));
		}

		return title.toString();
	}

	private UserApiService createUserApiService(YggdrasilAuthenticationService authService, RunArgs runArgs) {
		return runArgs.game.offlineDeveloperMode
			? UserApiService.OFFLINE
			: authService.createUserApiService(runArgs.network.session.getAccessToken());
	}

	public boolean isOfflineDeveloperMode() {
		return offlineDeveloperMode;
	}

	public static ModStatus getModStatus() {
		return ModStatus.check("vanilla", ClientBrandRetriever::getClientModName, "Client", MinecraftClient.class);
	}

	private void handleResourceReloadException(Throwable throwable, @Nullable LoadingContext loadingContext) {
		if (resourcePackManager.getEnabledIds().size() > 1) {
			onResourceReloadFailure(throwable, null, loadingContext);
		} else {
			Util.throwUnchecked(throwable);
		}
	}

	public void onResourceReloadFailure(Throwable exception, @Nullable Text resourceName, @Nullable LoadingContext loadingContext) {
		LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", exception);
		resourceReloadLogger.recover(exception);
		serverResourcePackLoader.onReloadFailure();
		resourcePackManager.setEnabledProfiles(Collections.emptyList());
		options.resourcePacks.clear();
		options.incompatibleResourcePacks.clear();
		options.write();
		reloadResources(true, loadingContext)
			.thenRunAsync(() -> showResourceReloadFailureToast(resourceName), this);
	}

	private void onForcedResourceReloadFailure() {
		setOverlay(null);
		if (world != null) {
			world.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
			disconnectWithProgressScreen();
		}

		setScreen(new TitleScreen());
		showResourceReloadFailureToast(null);
	}

	private void showResourceReloadFailureToast(@Nullable Text description) {
		SystemToast.show(
			getToastManager(),
			SystemToast.Type.PACK_LOAD_FAILURE,
			Text.translatable("resourcePack.load_fail"),
			description
		);
	}

	/**
	 * Обрабатывает сбой перезагрузки шейдеров.
	 * Если включены опциональные ресурс-паки — пытается откатиться к ним.
	 * Если активен только дефолтный пак — крашит игру, так как восстановление невозможно.
	 */
	public void onShaderResourceReloadFailure(Exception exception) {
		if (!resourcePackManager.hasOptionalProfilesEnabled()) {
			if (resourcePackManager.getEnabledIds().size() <= 1) {
				LOGGER.error(LogUtils.FATAL_MARKER, exception.getMessage(), exception);
				printCrashReport(new CrashReport(exception.getMessage(), exception));
			} else {
				send(this::onForcedResourceReloadFailure);
			}
		} else {
			onResourceReloadFailure(exception, Text.translatable("resourcePack.runtime_failure"), null);
		}
	}

	/**
	 * Запускает главный игровой цикл.
	 * Обрабатывает рендер-тики, OOM-ошибки и крэши.
	 */
	public void run() {
		thread = Thread.currentThread();
		if (Runtime.getRuntime().availableProcessors() > 4) {
			thread.setPriority(MAX_TICKS_PER_FRAME);
		}

		DiscontinuousFrame discontinuousFrame = TracyClient.createDiscontinuousFrame("Client Tick");

		try {
			boolean recoveredFromOom = false;

			while (running) {
				printCrashReport();

				try {
					TickDurationMonitor tickDurationMonitor = TickDurationMonitor.create("Renderer");
					boolean showRenderChart = getDebugHud().shouldShowRenderingChart();

					try (Profilers.Scoped scoped = Profilers.using(startMonitor(showRenderChart, tickDurationMonitor))) {
						recorder.startTick();
						discontinuousFrame.start();
						render(!recoveredFromOom);
						discontinuousFrame.end();
						recorder.endTick();
					}

					endMonitor(showRenderChart, tickDurationMonitor);
				} catch (OutOfMemoryError oomError) {
					if (recoveredFromOom) {
						throw oomError;
					}

					cleanUpAfterCrash();
					setScreen(new OutOfMemoryScreen());
					System.gc();
					LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", oomError);
					recoveredFromOom = true;
				}
			}
		} catch (CrashException crashException) {
			LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", crashException);
			printCrashReport(crashException.getReport());
		} catch (Throwable throwable) {
			LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", throwable);
			printCrashReport(new CrashReport("Unexpected error", throwable));
		}
	}

	/**
	 * Обрабатывает событие font options changed.
	 */
	public void onFontOptionsChanged() {
		this.fontManager.setActiveFilters(this.options);
	}

	private void handleGlErrorByDisableVsync(int error, long description) {
		this.options.getEnableVsync().setValue(false);
		this.options.write();
	}

	public Framebuffer getFramebuffer() {
		return this.framebuffer;
	}

	public String getGameVersion() {
		return this.gameVersion;
	}

	public String getVersionType() {
		return this.versionType;
	}

	public void setCrashReportSupplierAndAddDetails(CrashReport crashReport) {
		this.crashReportSupplier = () -> this.addDetailsToCrashReport(crashReport);
	}

	public void setCrashReportSupplier(CrashReport crashReport) {
		this.crashReportSupplier = () -> crashReport;
	}

	private void printCrashReport() {
		if (this.crashReportSupplier != null) {
			printCrashReport(this, this.runDirectory, this.crashReportSupplier.get());
		}
	}

	public void printCrashReport(CrashReport crashReport) {
		CrashMemoryReserve.releaseMemory();
		CrashReport detailedReport = addDetailsToCrashReport(crashReport);
		cleanUpAfterCrash();
		printCrashReport(this, runDirectory, detailedReport);
	}

	/**
	 * Сохраняет crash-репорт в директорию {@code crash-reports/}.
	 * Возвращает {@code -1} при успехе, {@code -2} если файл не удалось записать.
	 *
	 * @param runDir      рабочая директория игры
	 * @param crashReport отчёт о крэше
	 * @return код завершения процесса
	 */
	public static int saveCrashReport(File runDir, CrashReport crashReport) {
		Path crashDir = runDir.toPath().resolve("crash-reports");
		Path crashFile = crashDir.resolve("crash-" + Util.getFormattedCurrentTime() + "-client.txt");
		Bootstrap.println(crashReport.asString(ReportType.MINECRAFT_CRASH_REPORT));
		if (crashReport.getFile() != null) {
			Bootstrap.println("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReport.getFile().toAbsolutePath());
			return -1;
		} else if (crashReport.writeToFile(crashFile, ReportType.MINECRAFT_CRASH_REPORT)) {
			Bootstrap.println("#@!@# Game crashed! Crash report saved to: #@!@# " + crashFile.toAbsolutePath());
			return -1;
		} else {
			Bootstrap.println("#@?@# Game crashed! Crash report could not be saved. #@?@#");
			return -2;
		}
	}

	public static void printCrashReport(@Nullable MinecraftClient client, File runDirectory, CrashReport crashReport) {
		int exitCode = saveCrashReport(runDirectory, crashReport);
		if (client != null) {
			client.soundManager.stopAbruptly();
		}

		System.exit(exitCode);
	}

	public boolean forcesUnicodeFont() {
		return options.getForceUnicodeFont().getValue();
	}

	public CompletableFuture<Void> reloadResources() {
		return reloadResources(false, null);
	}

	/**
	 * Перезагружает ресурсы игры, создавая {@link SplashOverlay} с прогрессом загрузки.
	 * Если перезагрузка уже идёт и не форсирована — возвращает существующий future.
	 *
	 * @param force           форсировать перезагрузку, даже если уже показан SplashOverlay
	 * @param loadingContext  контекст начальной загрузки (может быть null при ручной перезагрузке)
	 * @return future, завершающийся после успешной перезагрузки всех ресурсов
	 */
	private CompletableFuture<Void> reloadResources(
			boolean force,
			MinecraftClient.@Nullable LoadingContext loadingContext
	) {
		if (resourceReloadFuture != null) {
			return resourceReloadFuture;
		}

		CompletableFuture<Void> reloadFuture = new CompletableFuture<>();

		if (!force && overlay instanceof SplashOverlay) {
			resourceReloadFuture = reloadFuture;
			return reloadFuture;
		}

		resourcePackManager.scanPacks();
		List<ResourcePack> packs = resourcePackManager.createResourcePacks();

		if (!force) {
			resourceReloadLogger.reload(ResourceReloadLogger.ReloadReason.MANUAL, packs);
		}

		setOverlay(
				new SplashOverlay(
						this,
						resourceManager.reload(
								Util.getMainWorkerExecutor().named("resourceLoad"),
								this,
								COMPLETED_UNIT_FUTURE,
								packs
						),
						error -> Util.ifPresentOrElse(
								error, throwable -> {
									if (force) {
										serverResourcePackLoader.onForcedReloadFailure();
										onForcedResourceReloadFailure();
									}
									else {
										handleResourceReloadException(throwable, loadingContext);
									}
								}, () -> {
									worldRenderer.reload();
									resourceReloadLogger.finish();
									serverResourcePackLoader.onReloadSuccess();
									reloadFuture.complete(null);
									onFinishedLoading(loadingContext);
								}
						),
						!force
				)
		);

		return reloadFuture;
	}

	private void checkGameData() {
		boolean hasMissingData = false;
		BlockModels blockModels = getBlockRenderManager().getModels();
		BlockStateModel missingModel = blockModels.getModelManager().getMissingModel();

		for (Block block : Registries.BLOCK) {
			for (BlockState blockState : block.getStateManager().getStates()) {
				if (blockState.getRenderType() == BlockRenderType.MODEL) {
					BlockStateModel model = blockModels.getModel(blockState);
					if (model == missingModel) {
						LOGGER.debug("Missing model for: {}", blockState);
						hasMissingData = true;
					}
				}
			}
		}

		Sprite missingParticleSprite = missingModel.particleSprite();

		for (Block block : Registries.BLOCK) {
			for (BlockState blockState : block.getStateManager().getStates()) {
				Sprite particleSprite = blockModels.getModelParticleSprite(blockState);
				if (!blockState.isAir() && particleSprite == missingParticleSprite) {
					LOGGER.debug("Missing particle icon for: {}", blockState);
				}
			}
		}

		Registries.ITEM.streamEntries().forEach(itemEntry -> {
			Item item = itemEntry.value();
			String translationKey = item.getTranslationKey();
			String translated = Text.translatable(translationKey).getString();
			if (translated.toLowerCase(Locale.ROOT).equals(item.getTranslationKey())) {
				LOGGER.debug("Missing translation for: {} {} {}", itemEntry.registryKey().getValue(), translationKey, item);
			}
		});
		hasMissingData |= HandledScreens.isMissingScreens();
		hasMissingData |= EntityRendererFactories.isMissingRendererFactories();
		if (hasMissingData) {
			throw new IllegalStateException("Your game data is foobar, fix the errors above!");
		}
	}

	public LevelStorage getLevelStorage() {
		return this.levelStorage;
	}

	/**
	 * Открывает chat screen.
	 *
	 * @param method method
	 */
	public void openChatScreen(ChatHud.ChatMethod method) {
		MinecraftClient.ChatRestriction chatRestriction = this.getChatRestriction();
		if (!chatRestriction.allowsChat(this.isInSingleplayer())) {
			if (this.inGameHud.shouldShowChatDisabledScreen()) {
				this.inGameHud.setCanShowChatDisabledScreen(false);
				this.setScreen(new ConfirmLinkScreen(
						confirmed -> {
							if (confirmed) {
								Util.getOperatingSystem().open(Urls.JAVA_ACCOUNT_SETTINGS);
							}

							this.setScreen(null);
						}, MinecraftClient.ChatRestriction.MORE_INFO_TEXT, Urls.JAVA_ACCOUNT_SETTINGS, true
				));
			}
			else {
				Text text = chatRestriction.getDescription();
				this.inGameHud.setOverlayMessage(text, false);
				this.narratorManager.narrateSystemImmediately(text);
				this.inGameHud.setCanShowChatDisabledScreen(
						chatRestriction == MinecraftClient.ChatRestriction.DISABLED_BY_PROFILE);
			}
		}
		else {
			this.inGameHud.getChatHud().setClientScreen(method, ChatScreen::new);
		}
	}

	public void setScreen(@Nullable Screen screen) {
		if (SharedConstants.isDevelopment && Thread.currentThread() != this.thread) {
			LOGGER.error("setScreen called from non-game thread");
		}

		if (this.currentScreen != null) {
			this.currentScreen.removed();
		}
		else {
			this.setNavigationType(GuiNavigationType.NONE);
		}

		if (screen == null) {
			if (this.disconnecting) {
				throw new IllegalStateException("Trying to return to in-game GUI during disconnection");
			}

			if (this.world == null) {
				screen = new TitleScreen();
			}
			else if (this.player.isDead()) {
				if (this.player.showsDeathScreen()) {
					screen = new DeathScreen(null, this.world.getLevelProperties().isHardcore(), this.player);
				}
				else {
					this.player.requestRespawn();
				}
			}
			else {
				screen = this.inGameHud.getChatHud().removeScreen();
			}
		}

		this.currentScreen = screen;
		if (this.currentScreen != null) {
			this.currentScreen.onDisplayed();
		}

		if (screen != null) {
			this.mouse.unlockCursor();
			KeyBinding.unpressAll();
			screen.init(this.window.getScaledWidth(), this.window.getScaledHeight());
			this.skipGameRender = false;
		}
		else {
			if (this.world != null) {
				KeyBinding.restoreToggleStates();
			}

			this.soundManager.resumeAll();
			this.mouse.lockCursor();
		}

		this.updateWindowTitle();
	}

	public void setOverlay(@Nullable Overlay overlay) {
		this.overlay = overlay;
	}

	/**
	 * Останавливает клиент: отключает мир, закрывает экраны и освобождает ресурсы.
	 * При отсутствии crash report завершает JVM с кодом 0.
	 */
	public void stop() {
		try {
			LOGGER.info("Stopping!");

			try {
				narratorManager.destroy();
			}
			catch (Throwable ignored) {
			}

			try {
				if (world != null) {
					world.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
				}

				disconnectWithProgressScreen();
			}
			catch (Throwable ignored) {
			}

			if (currentScreen != null) {
				currentScreen.removed();
			}

			close();
		}
		finally {
			Util.nanoTimeSupplier = System::nanoTime;
			if (crashReportSupplier == null) {
				System.exit(0);
			}
		}
	}

	@Override
	public void close() {
		if (this.currentGlTimerQuery != null) {
			this.currentGlTimerQuery.close();
		}

		try {
			this.telemetryManager.close();
			this.regionalComplianciesManager.close();
			this.atlasManager.close();
			this.fontManager.close();
			this.gameRenderer.close();
			this.shaderLoader.close();
			this.worldRenderer.close();
			this.soundManager.close();
			this.mapTextureManager.close();
			this.textureManager.close();
			this.resourceManager.close();
			if (this.tracyFrameCapturer != null) {
				this.tracyFrameCapturer.close();
			}

			FreeTypeUtil.release();
			Util.shutdownExecutors();
			RenderSystem.getSamplerCache().close();
			RenderSystem.getDevice().close();
		}
		catch (Throwable var5) {
			LOGGER.error("Shutdown failure!", var5);
			throw var5;
		}
		finally {
			this.windowProvider.close();
			this.window.close();
		}
	}

	/**
	 * Выполняет один кадр рендеринга: обрабатывает тики, рисует мир и обновляет метрики FPS/GPU.
	 *
	 * @param tick {@code true} — разрешить выполнение игровых тиков в этом кадре
	 */
	private void render(boolean tick) {
		window.setPhase("Pre render");
		if (window.shouldClose()) {
			scheduleStop();
		}

		if (resourceReloadFuture != null && !(overlay instanceof SplashOverlay)) {
			CompletableFuture<Void> pendingReload = resourceReloadFuture;
			resourceReloadFuture = null;
			reloadResources().thenRun(() -> pendingReload.complete(null));
		}

		int tickCount = renderTickCounter.beginRenderTick(Util.getMeasuringTimeMs(), tick);
		Profiler profiler = Profilers.get();

		if (tick) {
			try (GizmoDrawing.CollectorScope collectorScope = newGizmoScope()) {
				profiler.push("scheduledPacketProcessing");
				packetApplyBatcher.apply();
				profiler.swap("scheduledExecutables");
				runTasks();
				profiler.pop();
			}

			profiler.push("tick");
			if (tickCount > 0 && shouldTick()) {
				profiler.push("textures");
				textureManager.tick();
				profiler.pop();
			}

			for (int tickIndex = 0; tickIndex < Math.min(MAX_TICKS_PER_FRAME, tickCount); tickIndex++) {
				profiler.visit("clientTick");

				try (GizmoDrawing.CollectorScope collectorScope = newGizmoScope()) {
					tick();
				}
			}

			if (tickCount > 0 && (world == null || world.getTickManager().shouldTick())) {
				gizmos = gizmoCollector.extractGizmos();
			}

			profiler.pop();
		}

		window.setPhase("Render");

		boolean gpuTimerActive;
		try (GizmoDrawing.CollectorScope collectorScope = worldRenderer.startDrawingGizmos()) {
			profiler.push("gpuAsync");
			RenderSystem.executePendingTasks();
			profiler.swap("sound");
			soundManager.updateListenerPosition(gameRenderer.getCamera());
			profiler.swap("toasts");
			toastManager.update();
			profiler.swap("mouse");
			mouse.tick();
			profiler.swap("render");
			long renderStart = Util.getMeasuringTimeNano();

			if (!debugHudEntryList.isEntryVisible(DebugHudEntries.GPU_UTILIZATION) && !recorder.isActive()) {
				gpuTimerActive = false;
				gpuUtilizationPercentage = 0.0;
			}
			else {
				gpuTimerActive = (currentGlTimerQuery == null || currentGlTimerQuery.isResultAvailable())
						&& !GlTimer.getInstance().isRunning();
				if (gpuTimerActive) {
					GlTimer.getInstance().beginProfile();
				}
			}

			Framebuffer mainFramebuffer = getFramebuffer();
			RenderSystem
					.getDevice()
					.createCommandEncoder()
					.clearColorAndDepthTextures(
							mainFramebuffer.getColorAttachment(),
							0,
							mainFramebuffer.getDepthAttachment(),
							1.0
					);
			profiler.push("gameRenderer");
			if (!skipGameRender) {
				gameRenderer.render(renderTickCounter, tick);
			}

			profiler.swap("blit");
			if (!window.hasZeroWidthOrHeight()) {
				framebuffer.blitToScreen();
			}

			renderTime = Util.getMeasuringTimeNano() - renderStart;
			if (gpuTimerActive) {
				currentGlTimerQuery = GlTimer.getInstance().endProfile();
			}

			profiler.swap("updateDisplay");
			if (tracyFrameCapturer != null) {
				tracyFrameCapturer.upload();
				tracyFrameCapturer.capture(framebuffer);
			}

			window.swapBuffers(tracyFrameCapturer);
			int fpsLimit = inactivityFpsLimiter.update();
			if (fpsLimit < MAX_FPS_LIMIT) {
				RenderSystem.limitDisplayFPS(fpsLimit);
			}

			profiler.pop();
			profiler.swap("yield");
			Thread.yield();
			profiler.pop();
		}

		window.setPhase("Post render");
		fpsCounter++;
		boolean wasPaused = paused;
		paused = isIntegratedServerRunning()
				&& (currentScreen != null && currentScreen.shouldPause()
				|| overlay != null && overlay.pausesGame()
		)
				&& !server.isRemote();

		if (!wasPaused && paused) {
			soundManager.pauseAllExcept(SoundCategory.MUSIC, SoundCategory.UI);
		}

		renderTickCounter.tick(paused);
		renderTickCounter.setTickFrozen(!shouldTick());
		long sampleTime = Util.getMeasuringTimeNano();
		long sampleDelta = sampleTime - lastMetricsSampleTime;

		if (gpuTimerActive) {
			metricsSampleDuration = sampleDelta;
		}

		getDebugHud().pushToFrameLog(sampleDelta);
		lastMetricsSampleTime = sampleTime;
		profiler.push("fpsUpdate");
		if (currentGlTimerQuery != null && currentGlTimerQuery.isResultAvailable()) {
			gpuUtilizationPercentage = currentGlTimerQuery.queryResult() * 100.0 / metricsSampleDuration;
		}

		while (Util.getMeasuringTimeMs() >= nextDebugInfoUpdateTime + 1000L) {
			currentFps = fpsCounter;
			nextDebugInfoUpdateTime += 1000L;
			fpsCounter = 0;
		}

		profiler.pop();
	}

	private Profiler startMonitor(boolean active, @Nullable TickDurationMonitor monitor) {
		if (!active) {
			this.tickTimeTracker.disable();
			if (!this.recorder.isActive() && monitor == null) {
				return DummyProfiler.INSTANCE;
			}
		}

		Profiler profiler;
		if (active) {
			if (!this.tickTimeTracker.isActive()) {
				this.trackingTick = 0;
				this.tickTimeTracker.enable();
			}

			this.trackingTick++;
			profiler = this.tickTimeTracker.getProfiler();
		}
		else {
			profiler = DummyProfiler.INSTANCE;
		}

		if (this.recorder.isActive()) {
			profiler = Profiler.union(profiler, this.recorder.getProfiler());
		}

		return TickDurationMonitor.tickProfiler(profiler, monitor);
	}

	private void endMonitor(boolean active, @Nullable TickDurationMonitor monitor) {
		if (monitor != null) {
			monitor.endTick();
		}

		PieChart pieChart = this.getDebugHud().getPieChart();
		if (active) {
			pieChart.setProfileResult(this.tickTimeTracker.getResult());
		}
		else {
			pieChart.setProfileResult(null);
		}
	}

	@Override
	public void onResolutionChanged() {
		int scaleFactor = window.calculateScaleFactor(options.getGuiScale().getValue(), forcesUnicodeFont());
		window.setScaleFactor(scaleFactor);

		if (currentScreen != null) {
			currentScreen.resize(window.getScaledWidth(), window.getScaledHeight());
		}

		Framebuffer resizedFramebuffer = getFramebuffer();
		resizedFramebuffer.resize(window.getFramebufferWidth(), window.getFramebufferHeight());
		gameRenderer.onResized(window.getFramebufferWidth(), window.getFramebufferHeight());
		mouse.onResolutionChanged();
	}

	@Override
	public void onCursorEnterChanged() {
		this.mouse.setResolutionChanged();
	}

	public int getCurrentFps() {
		return currentFps;
	}

	public long getRenderTime() {
		return this.renderTime;
	}

	private void cleanUpAfterCrash() {
		CrashMemoryReserve.releaseMemory();

		try {
			if (this.integratedServerRunning && this.server != null) {
				this.server.stop(true);
			}

			this.disconnectWithSavingScreen();
		}
		catch (Throwable var2) {
		}

		System.gc();
	}

	/**
	 * Переключает режим отладочного профилировщика.
	 * При старте создаёт {@link DebugRecorder} для клиента и (если есть) сервера,
	 * при остановке — сохраняет результаты в zip-архив и отправляет ссылку в чат.
	 *
	 * @param chatMessageSender получатель текстовых сообщений о результатах профилирования
	 * @return {@code true} — профилировщик запущен, {@code false} — остановлен
	 */
	public boolean toggleDebugProfiler(Consumer<Text> chatMessageSender) {
		if (recorder.isActive()) {
			stopRecorder();
			return false;
		}

		Consumer<net.minecraft.util.profiler.ProfileResult> onProfileResult = profilerResult -> {
			if (profilerResult == EmptyProfileResult.INSTANCE) {
				return;
			}

			int tickSpan = profilerResult.getTickSpan();
			double seconds = (double) profilerResult.getTimeSpan() / TimeHelper.SECOND_IN_NANOS;
			execute(
					() -> chatMessageSender.accept(
							Text.translatable(
									"commands.debug.stopped",
									String.format(Locale.ROOT, "%.2f", seconds),
									tickSpan,
									String.format(Locale.ROOT, "%.2f", tickSpan / seconds)
							)
					)
			);
		};

		Consumer<Path> onFileSaved = resultPath -> {
			Text pathLink = Text.literal(resultPath.toString())
			                    .formatted(Formatting.UNDERLINE)
			                    .styled(style -> style.withClickEvent(new ClickEvent.OpenFile(resultPath.getParent())));
			execute(() -> chatMessageSender.accept(Text.translatable("debug.profiling.stop", pathLink)));
		};

		SystemDetails systemDetails = addSystemDetailsToCrashReport(
				new SystemDetails(),
				this,
				languageManager,
				gameVersion,
				options
		);

		Consumer<List<Path>> onAllFilesSaved = files -> onFileSaved.accept(saveProfilingResult(systemDetails, files));

		Consumer<Path> clientResultConsumer;
		if (server == null) {
			clientResultConsumer = path -> onAllFilesSaved.accept(ImmutableList.of(path));
		}
		else {
			server.addSystemDetails(systemDetails);
			CompletableFuture<Path> clientFuture = new CompletableFuture<>();
			CompletableFuture<Path> serverFuture = new CompletableFuture<>();
			CompletableFuture
					.allOf(clientFuture, serverFuture)
					.thenRunAsync(
							() -> onAllFilesSaved.accept(ImmutableList.of(clientFuture.join(), serverFuture.join())),
							Util.getIoWorkerExecutor()
					);
			server.setupRecorder(result -> {}, serverFuture::complete);
			clientResultConsumer = clientFuture::complete;
		}

		recorder = DebugRecorder.of(
				new ClientSamplerSource(Util.nanoTimeSupplier, worldRenderer),
				Util.nanoTimeSupplier,
				Util.getIoWorkerExecutor(),
				new RecordDumper("client"),
				result -> {
					recorder = DummyRecorder.INSTANCE;
					onProfileResult.accept(result);
				},
				clientResultConsumer
		);

		return true;
	}

	private void stopRecorder() {
		this.recorder.stop();
		if (this.server != null) {
			this.server.stopRecorder();
		}
	}

	private void forceStopRecorder() {
		this.recorder.forceStop();
		if (this.server != null) {
			this.server.forceStopRecorder();
		}
	}

	/**
	 * Сохраняет результаты профилирования в zip-архив с системными деталями и настройками клиента.
	 * После записи удаляет временные файлы профилировщика.
	 *
	 * @param details системные детали для включения в архив
	 * @param files   список временных файлов профилировщика (клиент + сервер)
	 * @return путь к созданному zip-архиву
	 */
	private Path saveProfilingResult(SystemDetails details, List<Path> files) {
		String levelName = isInSingleplayer()
				? getServer().getSaveProperties().getLevelName()
				: Optional.ofNullable(getCurrentServerEntry()).map(info -> info.name).orElse("unknown");

		Path archivePath;
		try {
			String archiveName = String.format(
					Locale.ROOT,
					"%s-%s-%s",
					Util.getFormattedCurrentTime(),
					levelName,
					SharedConstants.getGameVersion().id()
			);
			String uniqueName = PathUtil.getNextUniqueName(RecordDumper.DEBUG_PROFILING_DIRECTORY, archiveName, ".zip");
			archivePath = RecordDumper.DEBUG_PROFILING_DIRECTORY.resolve(uniqueName);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}

		try (ZipCompressor zip = new ZipCompressor(archivePath)) {
			zip.write(Paths.get("system.txt"), details.collect());
			zip.write(
					Paths.get("client").resolve(options.getOptionsFile().getName()),
					options.collectProfiledOptions()
			);
			files.forEach(zip::copyAll);
		}
		finally {
			for (Path tempFile : files) {
				try {
					FileUtils.forceDelete(tempFile.toFile());
				}
				catch (IOException exception) {
					LOGGER.warn("Failed to delete temporary profiling result {}", tempFile, exception);
				}
			}
		}

		return archivePath;
	}

	/**
	 * Schedule stop.
	 */
	public void scheduleStop() {
		this.running = false;
	}

	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Открывает игровое меню паузы.
	 * В одиночной игре показывает полное меню (с кнопкой выхода), в мультиплеере — только паузу.
	 *
	 * @param pauseOnly {@code true} — показать только экран паузы без кнопки выхода из мира
	 */
	public void openGameMenu(boolean pauseOnly) {
		if (currentScreen != null) {
			return;
		}

		boolean isLocalServer = isIntegratedServerRunning() && !server.isRemote();
		setScreen(new GameMenuScreen(isLocalServer ? !pauseOnly : true));
	}

	private void handleBlockBreaking(boolean breaking) {
		if (!breaking) {
			this.attackCooldown = 0;
		}

		if (this.attackCooldown <= 0 && !this.player.isUsingItem()) {
			ItemStack itemStack = this.player.getStackInHand(Hand.MAIN_HAND);
			if (!itemStack.contains(DataComponentTypes.PIERCING_WEAPON)) {
				if (breaking && this.crosshairTarget != null
						&& this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
					BlockHitResult blockHitResult = (BlockHitResult) this.crosshairTarget;
					BlockPos blockPos = blockHitResult.getBlockPos();
					if (!this.world.getBlockState(blockPos).isAir()) {
						Direction direction = blockHitResult.getSide();
						if (this.interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
							this.world.spawnBlockBreakingParticle(blockPos, direction);
							this.player.swingHand(Hand.MAIN_HAND);
						}
					}
				}
				else {
					this.interactionManager.cancelBlockBreaking();
				}
			}
		}
	}

	private boolean doAttack() {
		if (attackCooldown > 0) {
			return false;
		}

		if (crosshairTarget == null) {
			LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
			if (interactionManager.hasLimitedAttackSpeed()) {
				attackCooldown = MAX_TICKS_PER_FRAME;
			}

			return false;
		}

		if (player.isRiding()) {
			return false;
		}

		ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
		if (!mainHandStack.isItemEnabled(world.getEnabledFeatures())) {
			return false;
		}

		if (player.isBelowMinimumAttackCharge(mainHandStack, 0)) {
			return false;
		}

		PiercingWeaponComponent piercingWeapon = mainHandStack.get(DataComponentTypes.PIERCING_WEAPON);
		if (piercingWeapon != null && !interactionManager.isFlyingLocked()) {
			interactionManager.attackWithPiercingWeapon(piercingWeapon);
			player.swingHand(Hand.MAIN_HAND);
			return true;
		}

		boolean brokeBlock = false;

		switch (crosshairTarget.getType()) {
			case ENTITY:
				AttackRangeComponent attackRange = mainHandStack.get(DataComponentTypes.ATTACK_RANGE);
				if (attackRange == null || attackRange.isWithinRange(player, crosshairTarget.getPos())) {
					interactionManager.attackEntity(player, ((EntityHitResult) crosshairTarget).getEntity());
				}
				break;
			case BLOCK:
				BlockHitResult blockHitResult = (BlockHitResult) crosshairTarget;
				BlockPos blockPos = blockHitResult.getBlockPos();
				if (!world.getBlockState(blockPos).isAir()) {
					interactionManager.attackBlock(blockPos, blockHitResult.getSide());
					if (world.getBlockState(blockPos).isAir()) {
						brokeBlock = true;
					}

					break;
				}
			case MISS:
				if (interactionManager.hasLimitedAttackSpeed()) {
					attackCooldown = MAX_TICKS_PER_FRAME;
				}

				player.resetTicksSince();
		}

		if (!player.isSpectator()) {
			player.swingHand(Hand.MAIN_HAND);
		}

		return brokeBlock;
	}

	private void doItemUse() {
		if (interactionManager.isBreakingBlock()) {
			return;
		}

		itemUseCooldown = ITEM_USE_COOLDOWN_TICKS;

		if (player.isRiding()) {
			return;
		}

		if (crosshairTarget == null) {
			LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
		}

		for (Hand hand : Hand.values()) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (!itemStack.isItemEnabled(world.getEnabledFeatures())) {
				return;
			}

			if (crosshairTarget != null) {
				switch (crosshairTarget.getType()) {
					case ENTITY:
						EntityHitResult entityHitResult = (EntityHitResult) crosshairTarget;
						Entity entity = entityHitResult.getEntity();
						if (!world.getWorldBorder().contains(entity.getBlockPos())) {
							return;
						}

						if (player.canInteractWithEntity(entity, 0.0)) {
							ActionResult actionResult = interactionManager.interactEntityAtLocation(
									player,
									entity,
									entityHitResult,
									hand
							);
							if (!actionResult.isAccepted()) {
								actionResult = interactionManager.interactEntity(player, entity, hand);
							}

							if (actionResult instanceof ActionResult.Success success) {
								if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
									player.swingHand(hand);
								}

								return;
							}
						}
						break;
					case BLOCK:
						BlockHitResult blockHitResult = (BlockHitResult) crosshairTarget;
						int prevCount = itemStack.getCount();
						ActionResult blockResult = interactionManager.interactBlock(player, hand, blockHitResult);
						if (blockResult instanceof ActionResult.Success success) {
							if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
								player.swingHand(hand);
								if (!itemStack.isEmpty() && (itemStack.getCount() != prevCount
										|| player.isInCreativeMode()
								)) {
									gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
								}
							}

							return;
						}

						if (blockResult instanceof ActionResult.Fail) {
							return;
						}
				}
			}

			if (!itemStack.isEmpty()
					&& interactionManager.interactItem(player, hand) instanceof ActionResult.Success success
			) {
				if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
					player.swingHand(hand);
				}

				gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
				return;
			}
		}
	}

	public MusicTracker getMusicTracker() {
		return this.musicTracker;
	}

	/**
	 * Выполняет один игровой тик: обновляет GUI, мир, сущности, звук и обрабатывает ввод.
	 */
	public void tick() {
		uptimeInTicks++;
		if (world != null && !paused) {
			world.getTickManager().step();
		}

		if (itemUseCooldown > 0) {
			itemUseCooldown--;
		}

		Profiler profiler = Profilers.get();
		profiler.push("gui");
		messageHandler.processDelayedMessages();
		inGameHud.tick(paused);
		profiler.pop();
		gameRenderer.updateCrosshairTarget(1.0F);
		tutorialManager.tick(world, crosshairTarget);
		profiler.push("gameMode");
		if (!paused && world != null) {
			interactionManager.tick();
		}

		profiler.swap("screen");
		if (currentScreen != null || player == null) {
			if (currentScreen instanceof SleepingChatScreen sleepingChatScreen && !player.isSleeping()) {
				sleepingChatScreen.closeChatIfEmpty();
			}
		}
		else if (player.isDead() && !(currentScreen instanceof DeathScreen)) {
			setScreen(null);
		}
		else if (player.isSleeping() && world != null) {
			inGameHud.getChatHud().setClientScreen(ChatHud.ChatMethod.MESSAGE, SleepingChatScreen::new);
		}

		if (currentScreen != null) {
			attackCooldown = SCREEN_ATTACK_COOLDOWN;
		}

		if (currentScreen != null) {
			try {
				currentScreen.tick();
			}
			catch (Throwable throwable) {
				CrashReport crashReport = CrashReport.create(throwable, "Ticking screen");
				currentScreen.addCrashReportSection(crashReport);
				throw new CrashException(crashReport);
			}
		}

		if (overlay != null) {
			overlay.tick();
		}

		if (!getDebugHud().shouldShowDebugHud()) {
			inGameHud.resetDebugHudChunk();
		}

		if (overlay == null && currentScreen == null) {
			profiler.swap("Keybindings");
			handleInputEvents();
			if (attackCooldown > 0) {
				attackCooldown--;
			}
		}

		if (world != null) {
			if (!paused) {
				profiler.swap("gameRenderer");
				gameRenderer.tick();
				profiler.swap("entities");
				world.tickEntities();
				profiler.swap("blockEntities");
				world.tickBlockEntities();
			}
		}
		else if (gameRenderer.getPostProcessorId() != null) {
			gameRenderer.clearPostProcessor();
		}

		musicTracker.tick();
		soundManager.tick(paused);

		if (world != null) {
			if (!paused) {
				profiler.swap("level");
				if (!options.joinedFirstServer && isConnectedToServer()) {
					Text socialTitle = Text.translatable("tutorial.socialInteractions.title");
					Text socialDesc = Text.translatable(
							"tutorial.socialInteractions.description",
							TutorialManager.keyToText("socialInteractions")
					);
					socialInteractionsToast = new TutorialToast(
							textRenderer,
							TutorialToast.Type.SOCIAL_INTERACTIONS,
							socialTitle,
							socialDesc,
							true,
							SOCIAL_INTERACTIONS_TOAST_DURATION_MS
					);
					toastManager.add(socialInteractionsToast);
					options.joinedFirstServer = true;
					options.write();
				}

				tutorialManager.tick();

				try {
					world.tick(() -> true);
				}
				catch (Throwable throwable) {
					CrashReport crashReport = CrashReport.create(throwable, "Exception in world tick");
					if (world == null) {
						CrashReportSection section = crashReport.addElement("Affected level");
						section.add("Problem", "Level is null!");
					}
					else {
						world.addDetailsToCrashReport(crashReport);
					}

					throw new CrashException(crashReport);
				}
			}

			profiler.swap("animateTick");
			if (!paused && shouldTick()) {
				world.doRandomBlockDisplayTicks(player.getBlockX(), player.getBlockY(), player.getBlockZ());
			}

			profiler.swap("particles");
			if (!paused && shouldTick()) {
				particleManager.tick();
			}

			ClientPlayNetworkHandler networkHandler = getNetworkHandler();
			if (networkHandler != null && !paused) {
				networkHandler.sendPacket(ClientTickEndC2SPacket.INSTANCE);
			}
		}
		else if (integratedServerConnection != null) {
			profiler.swap("pendingConnection");
			integratedServerConnection.tick();
		}

		profiler.swap("keyboard");
		keyboard.pollDebugCrash();
		profiler.pop();
	}

	private boolean shouldTick() {
		return this.world == null || this.world.getTickManager().shouldTick();
	}

	private boolean isConnectedToServer() {
		return !this.integratedServerRunning || this.server != null && this.server.isRemote();
	}

	private void handleInputEvents() {
		while (options.togglePerspectiveKey.wasPressed()) {
			Perspective prevPerspective = options.getPerspective();
			options.setPerspective(options.getPerspective().next());
			if (prevPerspective.isFirstPerson() != options.getPerspective().isFirstPerson()) {
				gameRenderer.onCameraEntitySet(
						options.getPerspective().isFirstPerson() ? getCameraEntity() : null
				);
			}

			worldRenderer.scheduleTerrainUpdate();
		}

		while (options.smoothCameraKey.wasPressed()) {
			options.smoothCameraEnabled = !options.smoothCameraEnabled;
		}

		for (int slot = 0; slot < 9; slot++) {
			boolean saveToolbar = options.saveToolbarActivatorKey.isPressed();
			boolean loadToolbar = options.loadToolbarActivatorKey.isPressed();
			if (options.hotbarKeys[slot].wasPressed()) {
				if (player.isSpectator()) {
					inGameHud.getSpectatorHud().selectSlot(slot);
				}
				else if (!player.isInCreativeMode() || currentScreen != null || !loadToolbar && !saveToolbar) {
					player.getInventory().setSelectedSlot(slot);
				}
				else {
					CreativeInventoryScreen.onHotbarKeyPress(this, slot, loadToolbar, saveToolbar);
				}
			}
		}

		while (options.socialInteractionsKey.wasPressed()) {
			if (!isConnectedToServer() && !SharedConstants.SOCIAL_INTERACTIONS) {
				player.sendMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
				narratorManager.narrateSystemImmediately(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
			}
			else {
				if (socialInteractionsToast != null) {
					socialInteractionsToast.hide();
					socialInteractionsToast = null;
				}

				setScreen(new SocialInteractionsScreen());
			}
		}

		while (options.inventoryKey.wasPressed()) {
			if (interactionManager.hasRidingInventory()) {
				player.openRidingInventory();
			}
			else {
				tutorialManager.onInventoryOpened();
				setScreen(new InventoryScreen(player));
			}
		}

		while (options.advancementsKey.wasPressed()) {
			setScreen(new AdvancementsScreen(player.networkHandler.getAdvancementHandler()));
		}

		while (options.quickActionsKey.wasPressed()) {
			getQuickActionsDialog()
					.ifPresent(dialog -> player.networkHandler.showDialog(
							(RegistryEntry<Dialog>) dialog,
							currentScreen
					));
		}

		while (options.swapHandsKey.wasPressed()) {
			if (!player.isSpectator()) {
				getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
						PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
						BlockPos.ORIGIN,
						Direction.DOWN
				));
			}
		}

		while (options.dropKey.wasPressed()) {
			if (!player.isSpectator() && player.dropSelectedItem(isCtrlPressed())) {
				player.swingHand(Hand.MAIN_HAND);
			}
		}

		while (options.chatKey.wasPressed()) {
			openChatScreen(ChatHud.ChatMethod.MESSAGE);
		}

		if (currentScreen == null && overlay == null && options.commandKey.wasPressed()) {
			openChatScreen(ChatHud.ChatMethod.COMMAND);
		}

		boolean attackedThisTick = false;
		if (player.isUsingItem()) {
			if (!options.useKey.isPressed()) {
				interactionManager.stopUsingItem(player);
			}

			while (options.attackKey.wasPressed()) {
			}

			while (options.useKey.wasPressed()) {
			}

			while (options.pickItemKey.wasPressed()) {
			}
		}
		else {
			while (options.attackKey.wasPressed()) {
				attackedThisTick |= doAttack();
			}

			while (options.useKey.wasPressed()) {
				doItemUse();
			}

			while (options.pickItemKey.wasPressed()) {
				doItemPick();
			}

			if (player.isSpectator()) {
				while (options.spectatorHotbarKey.wasPressed()) {
					inGameHud.getSpectatorHud().useSelectedCommand();
				}
			}
		}

		if (options.useKey.isPressed() && itemUseCooldown == 0 && !player.isUsingItem()) {
			doItemUse();
		}

		handleBlockBreaking(currentScreen == null && !attackedThisTick && options.attackKey.isPressed()
				&& mouse.isCursorLocked());
	}

	private Optional<RegistryEntry<Dialog>> getQuickActionsDialog() {
		Registry<Dialog> registry = this.player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.DIALOG);
		return registry.getOptional(DialogTags.QUICK_ACTIONS).flatMap(quickActionsDialogs -> {
			if (quickActionsDialogs.size() == 0) {
				return Optional.empty();
			}
			else {
				return quickActionsDialogs.size() == 1 ? Optional.of(quickActionsDialogs.get(0))
				                                       : registry.getOptional(Dialogs.QUICK_ACTIONS);
			}
		});
	}

	public TelemetryManager getTelemetryManager() {
		return this.telemetryManager;
	}

	public double getGpuUtilizationPercentage() {
		return this.gpuUtilizationPercentage;
	}

	public ProfileKeys getProfileKeys() {
		return this.profileKeys;
	}

	/**
	 * Создаёт integrated server loader.
	 *
	 * @return IntegratedServerLoader — результат операции
	 */
	public IntegratedServerLoader createIntegratedServerLoader() {
		return new IntegratedServerLoader(this, this.levelStorage);
	}

	/**
	 * Запускает интегрированный (одиночный) сервер и подключается к нему локально.
	 * Блокирует рендер-поток до завершения загрузки мира.
	 *
	 * @param session         сессия хранилища уровня
	 * @param dataPackManager менеджер пакетов данных
	 * @param saveLoader      загрузчик сохранения
	 * @param newWorld        {@code true} — создаётся новый мир (увеличивает задержку прогресса)
	 */
	public void startIntegratedServer(
			LevelStorage.Session session,
			ResourcePackManager dataPackManager,
			SaveLoader saveLoader,
			boolean newWorld
	) {
		disconnectWithProgressScreen();
		Instant startTime = Instant.now();
		ClientChunkLoadProgress clientChunkLoadProgress = new ClientChunkLoadProgress(newWorld ? 500L : 0L);
		LevelLoadingScreen levelLoadingScreen = new LevelLoadingScreen(
				clientChunkLoadProgress,
				LevelLoadingScreen.WorldEntryReason.OTHER
		);
		setScreen(levelLoadingScreen);
		// Math.max(5, 3) — намеренно оставлено как есть: значение 5 является минимальным радиусом чанков
		int chunkMapRadius = Math.max(5, 3) + ChunkLevels.FULL_GENERATION_REQUIRED_LEVEL + 1;

		try {
			session.backupLevelDataFile(
					saveLoader.combinedDynamicRegistries().getCombinedRegistryManager(),
					saveLoader.saveProperties()
			);
			ChunkLoadProgress chunkLoadProgress = ChunkLoadProgress.compose(
					clientChunkLoadProgress,
					LoggingChunkLoadProgress.withPlayer()
			);
			server = MinecraftServer.startServer(
					thread -> new IntegratedServer(
							thread,
							this,
							session,
							dataPackManager,
							saveLoader,
							apiServices,
							chunkLoadProgress
					)
			);
			clientChunkLoadProgress.setChunkLoadMap(server.createChunkLoadMap(chunkMapRadius));
			integratedServerRunning = true;
			ensureAbuseReportContext(ReporterEnvironment.ofIntegratedServer());
			quickPlayLogger.setWorld(
					QuickPlayLogger.WorldType.SINGLEPLAYER,
					session.getDirectoryName(),
					saveLoader.saveProperties().getLevelName()
			);
		}
		catch (Throwable throwable) {
			CrashReport crashReport = CrashReport.create(throwable, "Starting integrated server");
			CrashReportSection section = crashReport.addElement("Starting integrated server");
			section.add("Level ID", session.getDirectoryName());
			section.add("Level Name", () -> saveLoader.saveProperties().getLevelName());
			throw new CrashException(crashReport);
		}

		Profiler profiler = Profilers.get();
		profiler.push("waitForServer");
		long frameNanos = TimeUnit.SECONDS.toNanos(1L) / 60L;

		while (!server.isLoading() || overlay != null) {
			long frameDeadline = Util.getMeasuringTimeNano() + frameNanos;
			levelLoadingScreen.tick();
			if (overlay != null) {
				overlay.tick();
			}

			render(false);
			runTasks();
			runTasks(() -> Util.getMeasuringTimeNano() > frameDeadline);
			printCrashReport();
		}

		profiler.pop();
		Duration loadDuration = Duration.between(startTime, Instant.now());
		SocketAddress socketAddress = server.getNetworkIo().bindLocal();
		ClientConnection clientConnection = ClientConnection.connectLocal(socketAddress);
		clientConnection.connect(
				socketAddress.toString(),
				0,
				new ClientLoginNetworkHandler(
						clientConnection,
						this,
						null,
						null,
						newWorld,
						loadDuration,
						status -> {},
						clientChunkLoadProgress,
						null
				)
		);
		clientConnection.send(new LoginHelloC2SPacket(
				getSession().getUsername(),
				getSession().getUuidOrNull()
		));
		integratedServerConnection = clientConnection;
	}

	/**
	 * Join world.
	 *
	 * @param world world
	 */
	public void joinWorld(ClientWorld world) {
		this.world = world;
		this.resetWorld(world);
	}

	/**
	 * Отключается от текущего мира и возвращает игрока на соответствующий экран:
	 * в одиночной игре — на главный экран, в реалмах — на экран реалмов, иначе — на мультиплеер.
	 *
	 * @param reasonText причина отключения (передаётся в мир для логирования)
	 */
	public void disconnect(Text reasonText) {
		boolean singleplayer = isInSingleplayer();
		ServerInfo serverInfo = getCurrentServerEntry();

		if (world != null) {
			world.disconnect(reasonText);
		}

		if (singleplayer) {
			disconnectWithSavingScreen();
		}
		else {
			disconnectWithProgressScreen();
		}

		TitleScreen titleScreen = new TitleScreen();
		if (singleplayer) {
			setScreen(titleScreen);
		}
		else if (serverInfo != null && serverInfo.isRealm()) {
			setScreen(new RealmsMainScreen(titleScreen));
		}
		else {
			setScreen(new MultiplayerScreen(titleScreen));
		}
	}

	/**
	 * Disconnect with saving screen.
	 */
	public void disconnectWithSavingScreen() {
		this.disconnectWithScreen(new MessageScreen(SAVING_LEVEL_TEXT), false);
	}

	/**
	 * Disconnect with progress screen.
	 */
	public void disconnectWithProgressScreen() {
		this.disconnectWithProgress(true);
	}

	public void disconnectWithProgress(boolean stopSounds) {
		disconnect(new ProgressScreen(true), false, stopSounds);
	}

	public void disconnectWithScreen(Screen screen, boolean transferring) {
		disconnect(screen, transferring, true);
	}

	/**
	 * Выполняет полное отключение от мира: останавливает сетевой обработчик, интегрированный сервер
	 * и переключает на указанный экран. Параметр {@code stopSounds} управляет остановкой звуков.
	 *
	 * @param disconnectionScreen экран, который будет показан после отключения
	 * @param transferring        {@code true} — это трансфер (не вызывать {@link #onDisconnected()})
	 * @param stopSounds          {@code true} — остановить все звуки при смене мира
	 */
	public void disconnect(Screen disconnectionScreen, boolean transferring, boolean stopSounds) {
		ClientPlayNetworkHandler networkHandler = getNetworkHandler();
		if (networkHandler != null) {
			cancelTasks();
			networkHandler.unloadWorld();
			if (!transferring) {
				onDisconnected();
			}
		}

		socialInteractionsManager.unloadBlockList();
		if (recorder.isActive()) {
			forceStopRecorder();
		}

		IntegratedServer integratedServer = server;
		server = null;
		gameRenderer.reset();
		interactionManager = null;
		narratorManager.clear();
		disconnecting = true;

		try {
			if (world != null) {
				inGameHud.clear();
			}

			if (integratedServer != null) {
				setScreen(new MessageScreen(SAVING_LEVEL_TEXT));
				Profiler profiler = Profilers.get();
				profiler.push("waitForServer");

				while (!integratedServer.isStopping()) {
					render(false);
				}

				profiler.pop();
			}

			setScreenAndRender(disconnectionScreen);
			integratedServerRunning = false;
			world = null;
			setWorld(null, stopSounds);
			player = null;
		}
		finally {
			disconnecting = false;
		}
	}

	/**
	 * Обрабатывает событие disconnected.
	 */
	public void onDisconnected() {
		this.serverResourcePackLoader.clear();
		this.runTasks();
	}

	/**
	 * Enter reconfiguration.
	 *
	 * @param reconfigurationScreen reconfiguration screen
	 */
	public void enterReconfiguration(Screen reconfigurationScreen) {
		ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
		if (clientPlayNetworkHandler != null) {
			clientPlayNetworkHandler.clearWorld();
		}

		if (this.recorder.isActive()) {
			this.forceStopRecorder();
		}

		this.gameRenderer.reset();
		this.interactionManager = null;
		this.narratorManager.clear();
		this.disconnecting = true;

		try {
			this.setScreenAndRender(reconfigurationScreen);
			this.inGameHud.clear();
			this.world = null;
			this.resetWorld(null);
			this.player = null;
		}
		finally {
			this.disconnecting = false;
		}
	}

	public void setScreenAndRender(Screen screen) {
		try (ScopedProfiler scopedProfiler = Profilers.get().scoped("forcedTick")) {
			this.setScreen(screen);
			this.render(false);
		}
	}

	private void resetWorld(@Nullable ClientWorld clientWorld) {
		this.setWorld(clientWorld, true);
	}

	private void setWorld(@Nullable ClientWorld world, boolean bl) {
		if (bl) {
			this.soundManager.stopAll();
		}

		this.setCameraEntity(null);
		this.integratedServerConnection = null;
		this.worldRenderer.setWorld(world);
		this.particleManager.setWorld(world);
		this.gameRenderer.setWorld(world);
		this.updateWindowTitle();
	}

	private UserProperties getUserProperties() {
		return this.userPropertiesFuture.join();
	}

	public boolean isOptionalTelemetryEnabled() {
		return this.isOptionalTelemetryEnabledByApi() && this.options.getTelemetryOptInExtra().getValue();
	}

	public boolean isOptionalTelemetryEnabledByApi() {
		return this.isTelemetryEnabledByApi() && this.getUserProperties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
	}

	public boolean isTelemetryEnabledByApi() {
		return (SharedConstants.isDevelopment && !SharedConstants.FORCE_TELEMETRY)
				? false
				: getUserProperties().flag(UserFlag.TELEMETRY_ENABLED);
	}


	public boolean isMultiplayerEnabled() {
		return this.multiplayerEnabled
				&& this.getUserProperties().flag(UserFlag.SERVERS_ALLOWED)
				&& this.getMultiplayerBanDetails() == null
				&& !this.isUsernameBanned();
	}

	public boolean isRealmsEnabled() {
		return this.getUserProperties().flag(UserFlag.REALMS_ALLOWED) && this.getMultiplayerBanDetails() == null;
	}

	public @Nullable BanDetails getMultiplayerBanDetails() {
		return (BanDetails) this.getUserProperties().bannedScopes().get("MULTIPLAYER");
	}

	public boolean isUsernameBanned() {
		ProfileResult profileResult = this.gameProfileFuture.getNow(null);
		return profileResult != null && profileResult.actions().contains(ProfileActionType.FORCED_NAME_CHANGE);
	}

	/**
	 * Определяет, следует ли block messages.
	 *
	 * @param sender sender
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldBlockMessages(UUID sender) {
		return this.getChatRestriction().allowsChat(false)
		       ? this.socialInteractionsManager.isPlayerMuted(sender)
		       : (this.player == null || !sender.equals(this.player.getUuid())) && !sender.equals(Util.NIL_UUID);
	}

	public MinecraftClient.ChatRestriction getChatRestriction() {
		if (this.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) {
			return MinecraftClient.ChatRestriction.DISABLED_BY_OPTIONS;
		}
		else if (!this.onlineChatEnabled) {
			return MinecraftClient.ChatRestriction.DISABLED_BY_LAUNCHER;
		}
		else {
			return !this.getUserProperties().flag(UserFlag.CHAT_ALLOWED)
			       ? MinecraftClient.ChatRestriction.DISABLED_BY_PROFILE
			       : MinecraftClient.ChatRestriction.ENABLED;
		}
	}

	public final boolean isDemo() {
		return this.isDemo;
	}

	/**
	 * Проверяет возможность switch game mode.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public final boolean canSwitchGameMode() {
		return this.player != null && this.interactionManager != null;
	}

	public @Nullable ClientPlayNetworkHandler getNetworkHandler() {
		return this.player == null ? null : this.player.networkHandler;
	}

	public static boolean isHudEnabled() {
		return !instance.options.hudHidden;
	}

	/**
	 * Использует s improved transparency.
	 *
	 * @return boolean — результат операции
	 */
	public static boolean usesImprovedTransparency() {
		return !instance.gameRenderer.isRenderingPanorama() && instance.options.getImprovedTransparency().getValue();
	}

	public static boolean isAmbientOcclusionEnabled() {
		return instance.options.getAo().getValue();
	}

	private void doItemPick() {
		if (this.crosshairTarget != null && this.crosshairTarget.getType() != HitResult.Type.MISS) {
			boolean bl = this.isCtrlPressed();
			switch (this.crosshairTarget) {
				case BlockHitResult blockHitResult:
					this.interactionManager.pickItemFromBlock(blockHitResult.getBlockPos(), bl);
					break;
				case EntityHitResult entityHitResult:
					this.interactionManager.pickItemFromEntity(entityHitResult.getEntity(), bl);
					break;
				default:
			}
		}
	}

	/**
	 * Добавляет details to crash report.
	 *
	 * @param report report
	 *
	 * @return CrashReport — результат операции
	 */
	public CrashReport addDetailsToCrashReport(CrashReport report) {
		SystemDetails systemDetails = report.getSystemDetailsSection();

		try {
			addSystemDetailsToCrashReport(systemDetails, this, this.languageManager, this.gameVersion, this.options);
			this.addUptimesToCrashReport(report.addElement("Uptime"));
			if (this.world != null) {
				this.world.addDetailsToCrashReport(report);
			}

			if (this.server != null) {
				this.server.addSystemDetails(systemDetails);
			}

			this.resourceReloadLogger.addReloadSection(report);
		}
		catch (Throwable var4) {
			LOGGER.error("Failed to collect details", var4);
		}

		return report;
	}

	public static void addSystemDetailsToCrashReport(
			@Nullable MinecraftClient client,
			@Nullable LanguageManager languageManager,
			String version,
			@Nullable GameOptions options,
			CrashReport report
	) {
		SystemDetails systemDetails = report.getSystemDetailsSection();
		addSystemDetailsToCrashReport(systemDetails, client, languageManager, version, options);
	}

	private static String formatSeconds(double seconds) {
		return String.format(Locale.ROOT, "%.3fs", seconds);
	}

	private void addUptimesToCrashReport(CrashReportSection section) {
		section.add("JVM uptime", () -> formatSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0));
		section.add("Wall uptime", () -> formatSeconds((System.currentTimeMillis() - this.startTime) / 1000.0));
		section.add("High-res time", () -> formatSeconds(Util.getMeasuringTimeMs() / 1000.0));
		section.add(
				"Client ticks",
				() -> String.format(Locale.ROOT, "%d ticks / %.3fs", this.uptimeInTicks, this.uptimeInTicks / 20.0)
		);
	}

	private static SystemDetails addSystemDetailsToCrashReport(
			SystemDetails systemDetails,
			@Nullable MinecraftClient client,
			@Nullable LanguageManager languageManager,
			String version,
			@Nullable GameOptions options
	) {
		systemDetails.addSection("Launched Version", () -> version);
		String string = getLauncherBrand();
		if (string != null) {
			systemDetails.addSection("Launcher name", string);
		}

		systemDetails.addSection("Backend library", RenderSystem::getBackendDescription);
		systemDetails.addSection("Backend API", RenderSystem::getApiDescription);
		systemDetails.addSection(
				"Window size",
				() -> client != null ? client.window.getFramebufferWidth() + "x" + client.window.getFramebufferHeight()
				                     : "<not initialized>"
		);
		systemDetails.addSection("GFLW Platform", Window::getGlfwPlatform);
		systemDetails.addSection(
				"Render Extensions",
				() -> String.join(", ", RenderSystem.getDevice().getEnabledExtensions())
		);
		systemDetails.addSection(
				"GL debug messages", () -> {
					GpuDevice gpuDevice = RenderSystem.tryGetDevice();
					if (gpuDevice == null) {
						return "<no renderer available>";
					}
					else {
						return gpuDevice.isDebuggingEnabled() ? String.join("\n", gpuDevice.getLastDebugMessages())
						                                      : "<debugging unavailable>";
					}
				}
		);
		systemDetails.addSection("Is Modded", () -> getModStatus().getMessage());
		systemDetails.addSection("Universe", () -> client != null ? Long.toHexString(client.UNIVERSE) : "404");
		systemDetails.addSection("Type", "Client (map_client.txt)");
		if (options != null) {
			if (client != null) {
				String string2 = client.getVideoWarningManager().getWarningsAsString();
				if (string2 != null) {
					systemDetails.addSection("GPU Warnings", string2);
				}
			}

			systemDetails.addSection(
					"Transparency",
					options.getImprovedTransparency().getValue() ? "shader" : "regular"
			);
			systemDetails.addSection(
					"Render Distance",
					options.getClampedViewDistance() + "/" + options.getViewDistance().getValue() + " chunks"
			);
		}

		if (client != null) {
			systemDetails.addSection(
					"Resource Packs",
					() -> ResourcePackManager.listPacks(client.getResourcePackManager().getEnabledProfiles())
			);
		}

		if (languageManager != null) {
			systemDetails.addSection("Current Language", () -> languageManager.getLanguage());
		}

		systemDetails.addSection("Locale", String.valueOf(Locale.getDefault()));
		systemDetails.addSection("System encoding", () -> System.getProperty("sun.jnu.encoding", "<not set>"));
		systemDetails.addSection("File encoding", () -> System.getProperty("file.encoding", "<not set>"));
		systemDetails.addSection("CPU", GLX::_getCpuInfo);
		return systemDetails;
	}

	public static MinecraftClient getInstance() {
		return instance;
	}

	/**
	 * Reload resources concurrently.
	 *
	 * @return CompletableFuture — результат операции
	 */
	public CompletableFuture<Void> reloadResourcesConcurrently() {
		return this
				.<CompletableFuture<Void>>submit((Supplier<CompletableFuture<Void>>) this::reloadResources)
				.thenCompose(future -> future);
	}

	/**
	 * Ensure abuse report context.
	 *
	 * @param environment environment
	 */
	public void ensureAbuseReportContext(ReporterEnvironment environment) {
		if (!this.abuseReportContext.environmentEquals(environment)) {
			this.abuseReportContext = AbuseReportContext.create(environment, this.userApiService);
		}
	}

	public @Nullable ServerInfo getCurrentServerEntry() {
		return Nullables.map(this.getNetworkHandler(), ClientPlayNetworkHandler::getServerInfo);
	}

	public boolean isInSingleplayer() {
		return this.integratedServerRunning;
	}

	public boolean isIntegratedServerRunning() {
		return this.integratedServerRunning && this.server != null;
	}

	public @Nullable IntegratedServer getServer() {
		return this.server;
	}

	public boolean isConnectedToLocalServer() {
		IntegratedServer integratedServer = this.getServer();
		return integratedServer != null && !integratedServer.isRemote();
	}

	/**
	 * Uuid equals.
	 *
	 * @param uuid uuid
	 *
	 * @return boolean — результат операции
	 */
	public boolean uuidEquals(UUID uuid) {
		return uuid.equals(this.getSession().getUuidOrNull());
	}

	public Session getSession() {
		return this.session;
	}

	public GameProfile getGameProfile() {
		ProfileResult profileResult = this.gameProfileFuture.join();
		return profileResult != null ? profileResult.profile()
		                             : new GameProfile(this.session.getUuidOrNull(), this.session.getUsername());
	}

	public Proxy getNetworkProxy() {
		return this.networkProxy;
	}

	public TextureManager getTextureManager() {
		return this.textureManager;
	}

	public ShaderLoader getShaderLoader() {
		return this.shaderLoader;
	}

	public ResourceManager getResourceManager() {
		return this.resourceManager;
	}

	public ResourcePackManager getResourcePackManager() {
		return this.resourcePackManager;
	}

	public DefaultResourcePack getDefaultResourcePack() {
		return this.defaultResourcePack;
	}

	public ServerResourcePackLoader getServerResourcePackProvider() {
		return this.serverResourcePackLoader;
	}

	public Path getResourcePackDir() {
		return this.resourcePackDir;
	}

	public LanguageManager getLanguageManager() {
		return this.languageManager;
	}

	public boolean isPaused() {
		return this.paused;
	}

	public VideoWarningManager getVideoWarningManager() {
		return this.videoWarningManager;
	}

	public SoundManager getSoundManager() {
		return this.soundManager;
	}

	public @Nullable MusicSound getMusicInstance() {
		MusicSound screenMusic = Nullables.map(currentScreen, Screen::getMusic);
		if (screenMusic != null) {
			return screenMusic;
		}

		Camera camera = gameRenderer.getCamera();
		if (player == null || camera == null) {
			return MusicType.MENU;
		}

		World playerWorld = player.getEntityWorld();
		if (playerWorld.getRegistryKey() == World.END && inGameHud.getBossBarHud().shouldPlayDragonMusic()) {
			return MusicType.DRAGON;
		}

		BackgroundMusic backgroundMusic = camera
				.getEnvironmentAttributeInterpolator()
				.get(EnvironmentAttributes.BACKGROUND_MUSIC_AUDIO, 1.0F);
		boolean isCreativeFlying = player.getAbilities().creativeMode && player.getAbilities().allowFlying;
		boolean isSubmerged = player.isSubmergedInWater();

		return backgroundMusic.getCurrent(isCreativeFlying, isSubmerged).orElse(null);
	}

	public float getMusicVolume() {
		if (this.currentScreen != null && this.currentScreen.getMusic() != null) {
			return 1.0F;
		}
		else {
			Camera camera = this.gameRenderer.getCamera();
			return camera != null ? camera
			                        .getEnvironmentAttributeInterpolator()
			                        .get(EnvironmentAttributes.MUSIC_VOLUME_AUDIO, 1.0F) : 1.0F;
		}
	}

	public ApiServices getApiServices() {
		return this.apiServices;
	}

	public PlayerSkinProvider getSkinProvider() {
		return this.skinProvider;
	}

	public @Nullable Entity getCameraEntity() {
		return this.cameraEntity;
	}

	public void setCameraEntity(@Nullable Entity entity) {
		this.cameraEntity = entity;
		this.gameRenderer.onCameraEntitySet(entity);
	}

	public boolean hasOutline(Entity entity) {
		return entity.isGlowing()
				|| this.player != null && this.player.isSpectator() && this.options.spectatorOutlinesKey.isPressed()
				&& entity.getType() == EntityType.PLAYER;
	}

	@Override
	protected Thread getThread() {
		return this.thread;
	}

	@Override
	public Runnable createTask(Runnable runnable) {
		return runnable;
	}

	@Override
	protected boolean canExecute(Runnable task) {
		return true;
	}

	public BlockRenderManager getBlockRenderManager() {
		return this.blockRenderManager;
	}

	public EntityRenderManager getEntityRenderDispatcher() {
		return this.entityRenderManager;
	}

	public BlockEntityRenderManager getBlockEntityRenderDispatcher() {
		return this.blockEntityRenderManager;
	}

	public ItemRenderer getItemRenderer() {
		return this.itemRenderer;
	}

	public MapRenderer getMapRenderer() {
		return this.mapRenderer;
	}

	public DataFixer getDataFixer() {
		return this.dataFixer;
	}

	public RenderTickCounter getRenderTickCounter() {
		return this.renderTickCounter;
	}

	public BlockColors getBlockColors() {
		return this.blockColors;
	}

	public boolean hasReducedDebugInfo() {
		return this.player != null && this.player.hasReducedDebugInfo() || this.options
				.getReducedDebugInfo()
				.getValue();
	}

	public ToastManager getToastManager() {
		return this.toastManager;
	}

	public TutorialManager getTutorialManager() {
		return this.tutorialManager;
	}

	public boolean isWindowFocused() {
		return this.windowFocused;
	}

	public HotbarStorage getCreativeHotbarStorage() {
		return this.creativeHotbarStorage;
	}

	public BakedModelManager getBakedModelManager() {
		return this.bakedModelManager;
	}

	public AtlasManager getAtlasManager() {
		return this.atlasManager;
	}

	public MapTextureManager getMapTextureManager() {
		return this.mapTextureManager;
	}

	public WaypointStyleAssetManager getWaypointStyleAssetManager() {
		return this.waypointStyleAssetManager;
	}

	@Override
	public void onWindowFocusChanged(boolean focused) {
		this.windowFocused = focused;
	}

	/**
	 * Take panorama.
	 *
	 * @param directory directory
	 *
	 * @return Text — результат операции
	 */
	public Text takePanorama(File directory) {
		int originalWidth = window.getFramebufferWidth();
		int originalHeight = window.getFramebufferHeight();
		float originalPitch = player.getPitch();
		float originalYaw = player.getYaw();
		float originalLastPitch = player.lastPitch;
		float originalLastYaw = player.lastYaw;
		gameRenderer.setBlockOutlineEnabled(false);

		MutableText failureMessage = null;

		try {
			gameRenderer.setCameraOverride(
					new CameraOverride(new Vector3f(gameRenderer.getCamera().getHorizontalPlane()))
			);
			window.setFramebufferWidth(PANORAMA_SIZE);
			window.setFramebufferHeight(PANORAMA_SIZE);
			framebuffer.resize(PANORAMA_SIZE, PANORAMA_SIZE);

			for (int face = 0; face < PANORAMA_FACE_COUNT; face++) {
				switch (face) {
					case 0 -> {
						player.setYaw(originalYaw);
						player.setPitch(0.0F);
					}
					case 1 -> {
						player.setYaw((originalYaw + 90.0F) % 360.0F);
						player.setPitch(0.0F);
					}
					case 2 -> {
						player.setYaw((originalYaw + 180.0F) % 360.0F);
						player.setPitch(0.0F);
					}
					case 3 -> {
						player.setYaw((originalYaw - 90.0F) % 360.0F);
						player.setPitch(0.0F);
					}
					case 4 -> {
						player.setYaw(originalYaw);
						player.setPitch(-90.0F);
					}
					default -> {
						player.setYaw(originalYaw);
						player.setPitch(90.0F);
					}
				}

				player.lastYaw = player.getYaw();
				player.lastPitch = player.getPitch();
				gameRenderer.updateCamera(RenderTickCounter.ONE);
				gameRenderer.renderWorld(RenderTickCounter.ONE);

				try {
					Thread.sleep(10L);
				}
				catch (InterruptedException ignored) {
				}

				ScreenshotRecorder.saveScreenshot(
						directory,
						"panorama_" + face + ".png",
						framebuffer,
						PANORAMA_DOWNSCALE_FACTOR,
						message -> {}
				);
			}

			Text dirLink = Text.literal(directory.getName())
			                   .formatted(Formatting.UNDERLINE)
			                   .styled(style -> style.withClickEvent(
					                   new ClickEvent.OpenFile(directory.getAbsoluteFile())
			                   ));
			return Text.translatable("screenshot.success", dirLink);
		}
		catch (Exception exception) {
			LOGGER.error("Couldn't save image", exception);
			failureMessage = Text.translatable("screenshot.failure", exception.getMessage());
		}
		finally {
			player.setPitch(originalPitch);
			player.setYaw(originalYaw);
			player.lastPitch = originalLastPitch;
			player.lastYaw = originalLastYaw;
			gameRenderer.setBlockOutlineEnabled(true);
			window.setFramebufferWidth(originalWidth);
			window.setFramebufferHeight(originalHeight);
			framebuffer.resize(originalWidth, originalHeight);
			gameRenderer.setCameraOverride(null);
		}

		return failureMessage;
	}

	public SplashTextResourceSupplier getSplashTextLoader() {
		return this.splashTextLoader;
	}

	public @Nullable Overlay getOverlay() {
		return this.overlay;
	}

	public SocialInteractionsManager getSocialInteractionsManager() {
		return this.socialInteractionsManager;
	}

	public Window getWindow() {
		return this.window;
	}

	public InactivityFpsLimiter getInactivityFpsLimiter() {
		return this.inactivityFpsLimiter;
	}

	public DebugHud getDebugHud() {
		return this.inGameHud.getDebugHud();
	}

	public BufferBuilderStorage getBufferBuilders() {
		return this.bufferBuilders;
	}

	public void setMipmapLevels(int mipmapLevels) {
		this.atlasManager.setMipmapLevels(mipmapLevels);
	}

	public LoadedEntityModels getLoadedEntityModels() {
		return this.bakedModelManager.getEntityModelsSupplier().get();
	}

	/**
	 * Определяет, следует ли filter text.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldFilterText() {
		return this.getUserProperties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
	}

	/**
	 * Загружает block list.
	 */
	public void loadBlockList() {
		this.socialInteractionsManager.loadBlockList();
		this.getProfileKeys().fetchKeyPair();
	}

	public GuiNavigationType getNavigationType() {
		return this.navigationType;
	}

	public void setNavigationType(GuiNavigationType navigationType) {
		this.navigationType = navigationType;
	}

	public NarratorManager getNarratorManager() {
		return this.narratorManager;
	}

	public MessageHandler getMessageHandler() {
		return this.messageHandler;
	}

	public AbuseReportContext getAbuseReportContext() {
		return this.abuseReportContext;
	}

	public RealmsPeriodicCheckers getRealmsPeriodicCheckers() {
		return this.realmsPeriodicCheckers;
	}

	public QuickPlayLogger getQuickPlayLogger() {
		return this.quickPlayLogger;
	}

	public CommandHistoryManager getCommandHistoryManager() {
		return this.commandHistoryManager;
	}

	public SymlinkFinder getSymlinkFinder() {
		return this.symlinkFinder;
	}

	public PlayerSkinCache getPlayerSkinCache() {
		return this.playerSkinCache;
	}

	private float getTargetMillisPerTick(float millis) {
		if (this.world != null) {
			TickManager tickManager = this.world.getTickManager();
			if (tickManager.shouldTick()) {
				return Math.max(millis, tickManager.getMillisPerTick());
			}
		}

		return millis;
	}

	public ItemModelManager getItemModelManager() {
		return this.itemModelManager;
	}

	/**
	 * Проверяет возможность current screen interrupt other screen.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canCurrentScreenInterruptOtherScreen() {
		return (this.currentScreen == null || this.currentScreen.canInterruptOtherScreen()) && !this.disconnecting;
	}

	public static @Nullable String getLauncherBrand() {
		return System.getProperty("minecraft.launcher.brand");
	}

	public PacketApplyBatcher getPacketApplyBatcher() {
		return this.packetApplyBatcher;
	}

	public GizmoDrawing.CollectorScope newGizmoScope() {
		return GizmoDrawing.using(this.gizmoCollector);
	}

	public Collection<GizmoCollectorImpl.Entry> getGizmos() {
		return this.gizmos;
	}

	/**
	 * Ограничение чата, определяющее, разрешён ли чат и по какой причине он может быть отключён.
	 */
	@Environment(EnvType.CLIENT)
	public enum ChatRestriction {
		ENABLED(ScreenTexts.EMPTY) {
			@Override
			public boolean allowsChat(boolean singlePlayer) {
				return true;
			}
		},
		DISABLED_BY_OPTIONS(Text.translatable("chat.disabled.options").formatted(Formatting.RED)) {
			@Override
			public boolean allowsChat(boolean singlePlayer) {
				return false;
			}
		},
		DISABLED_BY_LAUNCHER(Text.translatable("chat.disabled.launcher").formatted(Formatting.RED)) {
			@Override
			public boolean allowsChat(boolean singlePlayer) {
				return singlePlayer;
			}
		},
		DISABLED_BY_PROFILE(Text
				.translatable("chat.disabled.profile", Text.keybind(MinecraftClient.instance.options.chatKey.getId()))
				.formatted(Formatting.RED)) {
			@Override
			public boolean allowsChat(boolean singlePlayer) {
				return singlePlayer;
			}
		};

		static final Text MORE_INFO_TEXT = Text.translatable("chat.disabled.profile.moreInfo");
		private final Text description;

		ChatRestriction(final Text description) {
			this.description = description;
		}

		public Text getDescription() {
			return this.description;
		}

		/**
		 * Allows chat.
		 *
		 * @param singlePlayer single player
		 *
		 * @return boolean — результат операции
		 */
		public abstract boolean allowsChat(boolean singlePlayer);
	}

	/**
	 * Контекст загрузки, передаваемый между этапами инициализации клиента.
	 */
	@Environment(EnvType.CLIENT)
	record LoadingContext(RealmsClient realmsClient, RunArgs.QuickPlay quickPlayData) {
	}
}
