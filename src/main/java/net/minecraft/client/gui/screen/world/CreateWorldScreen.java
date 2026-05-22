package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.ExperimentalWarningScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.GeneratorOptionsFactory;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.SaveLoading;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.path.SymlinkFinder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.FlatLevelGeneratorPresets;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.rule.ServerGameRules;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Экран создания нового мира. Управляет вкладками настроек игры, мира и дополнительных параметров,
 * а также загрузкой датапаков и валидацией конфигурации генератора мира.
 */
@Environment(EnvType.CLIENT)
public class CreateWorldScreen extends Screen {

	private static final int MIN_VERSION = 1;
	private static final int BUTTON_WIDTH = 210;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String TEMP_DIR_PREFIX = "mcworld-";
	static final Text GAME_MODE_TEXT = Text.translatable("selectWorld.gameMode");
	static final Text ENTER_NAME_TEXT = Text.translatable("selectWorld.enterName");
	static final Text EXPERIMENTS_TEXT = Text.translatable("selectWorld.experiments");
	static final Text ALLOW_COMMANDS_INFO_TEXT = Text.translatable("selectWorld.allowCommands.info");
	private static final Text PREPARING_TEXT = Text.translatable("createWorld.preparing");
	private static final int CONTENT_PADDING = 10;
	private static final int BUTTON_PADDING = 8;
	public static final Identifier TAB_HEADER_BACKGROUND_TEXTURE =
			Identifier.ofVanilla("textures/gui/tab_header_background.png");
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	final WorldCreator worldCreator;
	private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
	private boolean recreated;
	private final SymlinkFinder symlinkFinder;
	private final CreateWorldCallback callback;
	private final Runnable closeCallback;
	private @Nullable Path dataPackTempDir;
	private @Nullable ResourcePackManager packManager;
	private @Nullable TabNavigationWidget tabNavigation;

	public static void show(MinecraftClient client, Runnable runnable) {
		show(
				client,
				runnable,
				(screen, combinedDynamicRegistries, levelProperties, dataPackTempDir) -> screen.startServer(
						combinedDynamicRegistries,
						levelProperties
				)
		);
	}

	public static void show(MinecraftClient client, Runnable runnable, CreateWorldCallback callback) {
		GeneratorOptionsFactory
				generatorOptionsFactory =
				(dataPackContents, dynamicRegistries, settings) -> new GeneratorOptionsHolder(
						settings.worldGenSettings(), dynamicRegistries, dataPackContents, settings.dataConfiguration()
				);
		Function<SaveLoading.LoadContextSupplierContext, WorldGenSettings> function = context -> new WorldGenSettings(
				GeneratorOptions.createRandom(), WorldPresets.createDemoOptions(context.worldGenRegistryManager())
		);
		show(client, runnable, function, generatorOptionsFactory, WorldPresets.DEFAULT, callback);
	}

	public static void showTestWorld(MinecraftClient client, Runnable runnable) {
		GeneratorOptionsFactory
				generatorOptionsFactory =
				(dataPackContents, dynamicRegistries, settings) -> new GeneratorOptionsHolder(
						settings.worldGenSettings().generatorOptions(),
						settings.worldGenSettings().dimensionOptionsRegistryHolder(),
						dynamicRegistries,
						dataPackContents,
						settings.dataConfiguration(),
						new InitialWorldOptions(
								WorldCreator.Mode.CREATIVE,
								new ServerGameRules.Builder()
										.put(GameRules.ADVANCE_TIME, false)
										.put(GameRules.ADVANCE_WEATHER, false)
										.put(GameRules.DO_MOB_SPAWNING, false)
										.build(),
								FlatLevelGeneratorPresets.REDSTONE_READY
						)
				);
		Function<SaveLoading.LoadContextSupplierContext, WorldGenSettings> function = context -> new WorldGenSettings(
				GeneratorOptions.createTestWorld(), WorldPresets.createTestOptions(context.worldGenRegistryManager())
		);
		show(
				client,
				runnable,
				function,
				generatorOptionsFactory,
				WorldPresets.FLAT,
				(screen, combinedDynamicRegistries, levelProperties, dataPackTempDir) -> screen.startServer(
						combinedDynamicRegistries,
						levelProperties
				)
		);
	}

	private static void show(
			MinecraftClient client,
			Runnable runnable,
			Function<SaveLoading.LoadContextSupplierContext, WorldGenSettings> settingsSupplier,
			GeneratorOptionsFactory generatorOptionsFactory,
			RegistryKey<WorldPreset> presetKey,
			CreateWorldCallback callback
	) {
		showMessage(client, PREPARING_TEXT);
		ResourcePackManager
				resourcePackManager =
				new ResourcePackManager(new VanillaDataPackProvider(client.getSymlinkFinder()));
		DataConfiguration dataConfiguration = SharedConstants.isDevelopment
		                                      ? new DataConfiguration(
				new DataPackSettings(
						List.of("vanilla", "tests"),
						List.of()
				), FeatureFlags.DEFAULT_ENABLED_FEATURES
		)
		                                      : DataConfiguration.SAFE_MODE;
		SaveLoading.ServerConfig serverConfig = createServerConfig(resourcePackManager, dataConfiguration);
		CompletableFuture<GeneratorOptionsHolder> completableFuture = SaveLoading.load(
				serverConfig,
				context -> new SaveLoading.LoadContext<>(
						new WorldCreationSettings(settingsSupplier.apply(context), context.dataConfiguration()),
						context.dimensionsRegistryManager()
				),
				(resourceManager, dataPackContents, dynamicRegistries, settings) -> {
					resourceManager.close();
					return generatorOptionsFactory.apply(dataPackContents, dynamicRegistries, settings);
				},
				Util.getMainWorkerExecutor(),
				client
		);
		client.runTasks(completableFuture::isDone);
		client.setScreen(new CreateWorldScreen(
				client,
				runnable,
				completableFuture.join(),
				Optional.of(presetKey),
				OptionalLong.empty(),
				callback
		));
	}

	public static CreateWorldScreen create(
			MinecraftClient client,
			Runnable runnable,
			LevelInfo levelInfo,
			GeneratorOptionsHolder generatorOptionsHolder,
			@Nullable Path dataPackTempDir
	) {
		CreateWorldScreen createWorldScreen = new CreateWorldScreen(
				client,
				runnable,
				generatorOptionsHolder,
				WorldPresets.getWorldPreset(generatorOptionsHolder.selectedDimensions()),
				OptionalLong.of(generatorOptionsHolder.generatorOptions().getSeed()),
				(screen, combinedDynamicRegistries, levelProperties, dataPackTempDirx) -> screen.startServer(
						combinedDynamicRegistries,
						levelProperties
				)
		);
		createWorldScreen.recreated = true;
		createWorldScreen.worldCreator.setWorldName(levelInfo.getLevelName());
		createWorldScreen.worldCreator.setCheatsEnabled(levelInfo.areCommandsAllowed());
		createWorldScreen.worldCreator.setDifficulty(levelInfo.getDifficulty());
		createWorldScreen.worldCreator.getGameRules().copyFrom(levelInfo.getGameRules(), null);
		if (levelInfo.isHardcore()) {
			createWorldScreen.worldCreator.setGameMode(WorldCreator.Mode.HARDCORE);
		}
		else if (levelInfo.getGameMode().isSurvivalLike()) {
			createWorldScreen.worldCreator.setGameMode(WorldCreator.Mode.SURVIVAL);
		}
		else if (levelInfo.getGameMode().isCreative()) {
			createWorldScreen.worldCreator.setGameMode(WorldCreator.Mode.CREATIVE);
		}

		createWorldScreen.dataPackTempDir = dataPackTempDir;
		return createWorldScreen;
	}

	private CreateWorldScreen(
			MinecraftClient client,
			Runnable runnable,
			GeneratorOptionsHolder generatorOptionsHolder,
			Optional<RegistryKey<WorldPreset>> defaultWorldType,
			OptionalLong seed,
			CreateWorldCallback callback
	) {
		super(Text.translatable("selectWorld.create"));
		this.closeCallback = runnable;
		this.symlinkFinder = client.getSymlinkFinder();
		this.callback = callback;
		this.worldCreator =
				new WorldCreator(
						client.getLevelStorage().getSavesDirectory(),
						generatorOptionsHolder,
						defaultWorldType,
						seed
				);
	}

	public WorldCreator getWorldCreator() {
		return worldCreator;
	}

	@Override
	protected void init() {
		tabNavigation = TabNavigationWidget
				.builder(tabManager, width)
				.tabs(
						new GameTab(),
						new WorldTab(),
						new MoreTab()
				)
				.build();
		addDrawableChild(tabNavigation);
		DirectionalLayoutWidget footer = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		footer.add(ButtonWidget
				.builder(Text.translatable("selectWorld.create"), button -> createLevel())
				.build());
		footer.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> onCloseScreen()).build());
		layout.forEachChild(child -> {
			child.setNavigationOrder(1);
			addDrawableChild(child);
		});
		tabNavigation.selectTab(0, false);
		worldCreator.update();
		refreshWidgetPositions();
	}

	@Override
	protected void setInitialFocus() {
	}

	@Override
	public void refreshWidgetPositions() {
		if (tabNavigation == null) {
			return;
		}

		tabNavigation.setWidth(width);
		tabNavigation.init();
		int navBottom = tabNavigation.getNavigationFocus().getBottom();
		ScreenRect tabArea = new ScreenRect(0, navBottom, width, height - layout.getFooterHeight() - navBottom);
		tabManager.setTabArea(tabArea);
		layout.setHeaderHeight(navBottom);
		layout.refreshPositions();
	}

	private static void showMessage(MinecraftClient client, Text text) {
		client.setScreenAndRender(new MessageScreen(text));
	}

	private void createLevel() {
		GeneratorOptionsHolder generatorOptionsHolder = worldCreator.getGeneratorOptionsHolder();
		DimensionOptionsRegistryHolder.DimensionsConfig dimensionsConfig = generatorOptionsHolder
				.selectedDimensions()
				.toConfig(generatorOptionsHolder.dimensionOptionsRegistry());
		CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries = generatorOptionsHolder
				.combinedDynamicRegistries()
				.with(
						ServerDynamicRegistryType.DIMENSIONS,
						dimensionsConfig.toDynamicRegistryManager()
				);
		Lifecycle dataLifecycle = FeatureFlags.isNotVanilla(generatorOptionsHolder.dataConfiguration().enabledFeatures())
				? Lifecycle.experimental()
				: Lifecycle.stable();
		Lifecycle registryLifecycle = combinedDynamicRegistries.getCombinedRegistryManager().getLifecycle();
		Lifecycle finalLifecycle = registryLifecycle.add(dataLifecycle);
		boolean isNewWorld = !recreated && registryLifecycle == Lifecycle.stable();
		LevelInfo levelInfo = createLevelInfo(
				dimensionsConfig.specialWorldProperty() == LevelProperties.SpecialProperty.DEBUG
		);
		LevelProperties levelProperties = new LevelProperties(
				levelInfo,
				worldCreator.getGeneratorOptionsHolder().generatorOptions(),
				dimensionsConfig.specialWorldProperty(),
				finalLifecycle
		);
		IntegratedServerLoader.tryLoad(
				client,
				this,
				finalLifecycle,
				() -> createAndClearTempDir(combinedDynamicRegistries, levelProperties),
				isNewWorld
		);
	}

	private void createAndClearTempDir(
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
			LevelProperties levelProperties
	) {
		boolean created = callback.create(this, combinedDynamicRegistries, levelProperties, dataPackTempDir);
		clearDataPackTempDir();
		if (!created) {
			onCloseScreen();
		}
	}

	private boolean startServer(
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
			SaveProperties saveProperties
	) {
		String dirName = worldCreator.getWorldDirectoryName();
		GeneratorOptionsHolder generatorOptionsHolder = worldCreator.getGeneratorOptionsHolder();
		showMessage(client, PREPARING_TEXT);
		Optional<LevelStorage.Session> sessionOpt = createSession(client, dirName, dataPackTempDir);
		if (sessionOpt.isEmpty()) {
			SystemToast.addPackCopyFailure(client, dirName);
			return false;
		}

		client
				.createIntegratedServerLoader()
				.startNewWorld(
						sessionOpt.get(),
						generatorOptionsHolder.dataPackContents(),
						combinedDynamicRegistries,
						saveProperties
				);
		return true;
	}

	private LevelInfo createLevelInfo(boolean debugWorld) {
		String worldName = worldCreator.getWorldName().trim();
		if (debugWorld) {
			GameRules debugRules = new GameRules(DataConfiguration.SAFE_MODE.enabledFeatures());
			debugRules.setValue(GameRules.ADVANCE_TIME, false, null);
			return new LevelInfo(
					worldName,
					GameMode.SPECTATOR,
					false,
					Difficulty.PEACEFUL,
					true,
					debugRules,
					DataConfiguration.SAFE_MODE
			);
		}

		return new LevelInfo(
				worldName,
				worldCreator.getGameMode().defaultGameMode,
				worldCreator.isHardcore(),
				worldCreator.getDifficulty(),
				worldCreator.areCheatsEnabled(),
				worldCreator.getGameRules(),
				worldCreator.getGeneratorOptionsHolder().dataConfiguration()
		);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (tabNavigation.keyPressed(input)) {
			return true;
		}

		if (super.keyPressed(input)) {
			return true;
		}

		if (input.isEnter()) {
			createLevel();
			return true;
		}

		return false;
	}

	@Override
	public void close() {
		onCloseScreen();
	}

	public void onCloseScreen() {
		closeCallback.run();
		clearDataPackTempDir();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				Screen.FOOTER_SEPARATOR_TEXTURE,
				0,
				height - layout.getFooterHeight() - 2,
				0.0F,
				0.0F,
				width,
				2,
				32,
				2
		);
	}

	@Override
	protected void renderDarkening(DrawContext context) {
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				TAB_HEADER_BACKGROUND_TEXTURE,
				0,
				0,
				0.0F,
				0.0F,
				width,
				layout.getHeaderHeight(),
				16,
				16
		);
		renderDarkening(context, 0, layout.getHeaderHeight(), width, height);
	}

	private @Nullable Path getOrCreateDataPackTempDir() {
		if (dataPackTempDir == null) {
			try {
				dataPackTempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
			}
			catch (IOException exception) {
				LOGGER.warn("Failed to create temporary dir", exception);
				SystemToast.addPackCopyFailure(client, worldCreator.getWorldDirectoryName());
				onCloseScreen();
			}
		}

		return dataPackTempDir;
	}

	void openExperimentsScreen(DataConfiguration dataConfiguration) {
		Pair<Path, ResourcePackManager> pack = getScannedPack(dataConfiguration);
		if (pack == null) {
			return;
		}

		client.setScreen(new ExperimentsScreen(
				this,
				pack.getSecond(),
				manager -> applyDataPacks(manager, false, this::openExperimentsScreen)
		));
	}

	void openPackScreen(DataConfiguration dataConfiguration) {
		Pair<Path, ResourcePackManager> pack = getScannedPack(dataConfiguration);
		if (pack == null) {
			return;
		}

		client.setScreen(new PackScreen(
				pack.getSecond(),
				manager -> applyDataPacks(manager, true, this::openPackScreen),
				pack.getFirst(),
				Text.translatable("dataPack.title")
		));
	}

	private void applyDataPacks(
			ResourcePackManager dataPackManager,
			boolean fromPackScreen,
			Consumer<DataConfiguration> configurationSetter
	) {
		List<String> enabledPacks = ImmutableList.copyOf(dataPackManager.getEnabledIds());
		List<String> disabledPacks = dataPackManager
				.getIds()
				.stream()
				.filter(name -> !enabledPacks.contains(name))
				.collect(ImmutableList.toImmutableList());
		DataConfiguration dataConfiguration = new DataConfiguration(
				new DataPackSettings(enabledPacks, disabledPacks),
				worldCreator.getGeneratorOptionsHolder().dataConfiguration().enabledFeatures()
		);
		if (worldCreator.updateDataConfiguration(dataConfiguration)) {
			client.setScreen(this);
			return;
		}

		FeatureSet featureSet = dataPackManager.getRequestedFeatures();
		if (FeatureFlags.isNotVanilla(featureSet) && fromPackScreen) {
			client.setScreen(new ExperimentalWarningScreen(
					dataPackManager.getEnabledProfiles(),
					confirmed -> {
						if (confirmed) {
							validateDataPacks(dataPackManager, dataConfiguration, configurationSetter);
						}
						else {
							configurationSetter.accept(worldCreator.getGeneratorOptionsHolder().dataConfiguration());
						}
					}
			));
		}
		else {
			validateDataPacks(dataPackManager, dataConfiguration, configurationSetter);
		}
	}

	private void validateDataPacks(
			ResourcePackManager dataPackManager,
			DataConfiguration dataConfiguration,
			Consumer<DataConfiguration> configurationSetter
	) {
		client.setScreenAndRender(new MessageScreen(Text.translatable("dataPack.validation.working")));
		SaveLoading.ServerConfig serverConfig = createServerConfig(dataPackManager, dataConfiguration);
		SaveLoading.<WorldCreationSettings, GeneratorOptionsHolder>load(
				serverConfig,
				context -> {
					if (context
							.worldGenRegistryManager()
							.getOrThrow(RegistryKeys.WORLD_PRESET)
							.streamEntries()
							.findAny()
							.isEmpty()
					) {
						throw new IllegalStateException("Needs at least one world preset to continue");
					}

					if (context
							.worldGenRegistryManager()
							.getOrThrow(RegistryKeys.BIOME)
							.streamEntries()
							.findAny()
							.isEmpty()
					) {
						throw new IllegalStateException("Needs at least one biome continue");
					}

					GeneratorOptionsHolder generatorOptionsHolder = worldCreator.getGeneratorOptionsHolder();
					DynamicOps<JsonElement> currentOps = generatorOptionsHolder
							.getCombinedRegistryManager()
							.getOps(JsonOps.INSTANCE);
					DataResult<JsonElement> dataResult = WorldGenSettings
							.encode(
									currentOps,
									generatorOptionsHolder.generatorOptions(),
									generatorOptionsHolder.selectedDimensions()
							)
							.setLifecycle(Lifecycle.stable());
					DynamicOps<JsonElement> newOps = context.worldGenRegistryManager().getOps(JsonOps.INSTANCE);
					WorldGenSettings worldGenSettings = dataResult
							.flatMap(json -> WorldGenSettings.CODEC.parse(newOps, json))
							.getOrThrow(error -> new IllegalStateException(
									"Error parsing worldgen settings after loading data packs: " + error
							));
					return new SaveLoading.LoadContext<>(
							new WorldCreationSettings(worldGenSettings, context.dataConfiguration()),
							context.dimensionsRegistryManager()
					);
				},
				(resourceManager, dataPackContents, combinedDynamicRegistries, context) -> {
					resourceManager.close();
					return new GeneratorOptionsHolder(
							context.worldGenSettings(),
							combinedDynamicRegistries,
							dataPackContents,
							context.dataConfiguration()
					);
				},
				Util.getMainWorkerExecutor(),
				client
		)
		.thenApply(holder -> {
			holder.initializeIndexedFeaturesLists();
			return holder;
		})
		.thenAcceptAsync(worldCreator::setGeneratorOptionsHolder, client)
		.handleAsync(
				(ignored, throwable) -> {
					if (throwable != null) {
						LOGGER.warn("Failed to validate datapack", throwable);
						client.setScreen(new ConfirmScreen(
								confirmed -> {
									if (confirmed) {
										configurationSetter.accept(
												worldCreator.getGeneratorOptionsHolder().dataConfiguration()
										);
									}
									else {
										configurationSetter.accept(DataConfiguration.SAFE_MODE);
									}
								},
								Text.translatable("dataPack.validation.failed"),
								ScreenTexts.EMPTY,
								Text.translatable("dataPack.validation.back"),
								Text.translatable("dataPack.validation.reset")
						));
					}
					else {
						client.setScreen(this);
					}

					return null;
				},
				client
		);
	}

	private static SaveLoading.ServerConfig createServerConfig(
			ResourcePackManager dataPackManager,
			DataConfiguration dataConfiguration
	) {
		SaveLoading.DataPacks dataPacks = new SaveLoading.DataPacks(dataPackManager, dataConfiguration, false, true);
		return new SaveLoading.ServerConfig(
				dataPacks,
				CommandManager.RegistrationEnvironment.INTEGRATED,
				LeveledPermissionPredicate.GAMEMASTERS
		);
	}

	private void clearDataPackTempDir() {
		if (dataPackTempDir != null && Files.exists(dataPackTempDir)) {
			try (Stream<Path> stream = Files.walk(dataPackTempDir)) {
				stream.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					}
					catch (IOException exception) {
						LOGGER.warn("Failed to remove temporary file {}", path, exception);
					}
				});
			}
			catch (IOException exception) {
				LOGGER.warn("Failed to list temporary dir {}", dataPackTempDir);
			}
		}

		dataPackTempDir = null;
	}

	private static void copyDataPack(Path srcFolder, Path destFolder, Path dataPackFile) {
		try {
			Util.relativeCopy(srcFolder, destFolder, dataPackFile);
		}
		catch (IOException var4) {
			LOGGER.warn("Failed to copy datapack file from {} to {}", dataPackFile, destFolder);
			throw new UncheckedIOException(var4);
		}
	}

	/**
	 * Создаёт сессию хранилища для нового мира и копирует датапаки из временной директории.
	 * Если копирование завершается ошибкой, сессия закрывается и возвращается пустой Optional.
	 */
	private static Optional<LevelStorage.Session> createSession(
			MinecraftClient client,
			String worldDirectoryName,
			@Nullable Path dataPackTempDir
	) {
		try {
			LevelStorage.Session session = client
					.getLevelStorage()
					.createSessionWithoutSymlinkCheck(worldDirectoryName);
			if (dataPackTempDir == null) {
				return Optional.of(session);
			}

			try (Stream<Path> stream = Files.walk(dataPackTempDir)) {
				Path dataPacksDir = session.getDirectory(WorldSavePath.DATAPACKS);
				PathUtil.createDirectories(dataPacksDir);
				stream
						.filter(file -> !file.equals(dataPackTempDir))
						.forEach(file -> copyDataPack(dataPackTempDir, dataPacksDir, file));
				return Optional.of(session);
			}
			catch (UncheckedIOException | IOException exception) {
				LOGGER.warn("Failed to copy datapacks to world {}", worldDirectoryName, exception);
				session.close();
			}
		}
		catch (UncheckedIOException | IOException exception) {
			LOGGER.warn("Failed to create access for {}", worldDirectoryName, exception);
		}

		return Optional.empty();
	}

	/**
	 * Создаёт копию data pack.
	 *
	 * @param srcFolder src folder
	 * @param client client
	 *
	 * @return Path — результат операции
	 */
	public static Path copyDataPack(Path srcFolder, MinecraftClient client) {
		MutableObject<Path> mutableObject = new MutableObject();

		try (Stream<Path> stream = Files.walk(srcFolder)) {
			stream.filter(dataPackFile -> !dataPackFile.equals(srcFolder)).forEach(dataPackFile -> {
				Path path2 = (Path) mutableObject.get();
				if (path2 == null) {
					try {
						path2 = Files.createTempDirectory("mcworld-");
					}
					catch (IOException var5) {
						LOGGER.warn("Failed to create temporary dir");
						throw new UncheckedIOException(var5);
					}

					mutableObject.setValue(path2);
				}

				copyDataPack(srcFolder, path2, dataPackFile);
			});
		}
		catch (UncheckedIOException | IOException var8) {
			LOGGER.warn("Failed to copy datapacks from world {}", srcFolder, var8);
			SystemToast.addPackCopyFailure(client, srcFolder.toString());
			return null;
		}

		return (Path) mutableObject.get();
	}

	private @Nullable Pair<Path, ResourcePackManager> getScannedPack(DataConfiguration dataConfiguration) {
		Path tempDir = getOrCreateDataPackTempDir();
		if (tempDir == null) {
			return null;
		}

		if (packManager == null) {
			packManager = VanillaDataPackProvider.createManager(tempDir, symlinkFinder);
			packManager.scanPacks();
		}

		packManager.setEnabledProfiles(dataConfiguration.dataPacks().getEnabled());
		return Pair.of(tempDir, packManager);
	}

	/**
	 * Вкладка «Игра» — настройки режима игры, сложности и читов.
	 */
	@Environment(EnvType.CLIENT)
	class GameTab extends GridScreenTab {

		private static final Text GAME_TAB_TITLE_TEXT = Text.translatable("createWorld.tab.game.title");
		private static final Text ALLOW_COMMANDS_TEXT = Text.translatable("selectWorld.allowCommands");
		private final TextFieldWidget worldNameField;

		GameTab() {
			super(GAME_TAB_TITLE_TEXT);
			GridWidget.Adder adder = this.grid.setRowSpacing(8).createAdder(1);
			Positioner positioner = adder.copyPositioner();
			this.worldNameField =
					new TextFieldWidget(
							CreateWorldScreen.this.textRenderer,
							208,
							20,
							Text.translatable("selectWorld.enterName")
					);
			this.worldNameField.setText(CreateWorldScreen.this.worldCreator.getWorldName());
			this.worldNameField.setChangedListener(CreateWorldScreen.this.worldCreator::setWorldName);
			CreateWorldScreen.this.worldCreator
					.addListener(
							creator -> this.worldNameField
									.setTooltip(
											Tooltip.of(Text.translatable(
													"selectWorld.targetFolder",
													Text
															.literal(creator.getWorldDirectoryName())
															.formatted(Formatting.ITALIC)
											))
									)
					);
			CreateWorldScreen.this.setInitialFocus(this.worldNameField);
			adder.add(
					LayoutWidgets.createLabeledWidget(
							CreateWorldScreen.this.textRenderer,
							this.worldNameField,
							CreateWorldScreen.ENTER_NAME_TEXT
					),
					adder.copyPositioner().alignHorizontalCenter()
			);
			CyclingButtonWidget<WorldCreator.Mode> cyclingButtonWidget = adder.add(
					CyclingButtonWidget
							.<WorldCreator.Mode>builder(
									value -> value.name,
									CreateWorldScreen.this.worldCreator.getGameMode()
							)
							.values(WorldCreator.Mode.SURVIVAL, WorldCreator.Mode.HARDCORE, WorldCreator.Mode.CREATIVE)
							.build(
									0,
									0,
									BUTTON_WIDTH,
									20,
									CreateWorldScreen.GAME_MODE_TEXT,
									(button, value) -> CreateWorldScreen.this.worldCreator.setGameMode(value)
							),
					positioner
			);
			CreateWorldScreen.this.worldCreator.addListener(creator -> {
				cyclingButtonWidget.setValue(creator.getGameMode());
				cyclingButtonWidget.active = !creator.isDebug();
				cyclingButtonWidget.setTooltip(Tooltip.of(creator.getGameMode().getInfo()));
			});
			CyclingButtonWidget<Difficulty> cyclingButtonWidget2 = adder.add(
					CyclingButtonWidget
							.builder(
									Difficulty::getTranslatableName,
									CreateWorldScreen.this.worldCreator.getDifficulty()
							)
							.values(Difficulty.values())
							.build(
									0,
									0,
									BUTTON_WIDTH,
									20,
									Text.translatable("options.difficulty"),
									(button, value) -> CreateWorldScreen.this.worldCreator.setDifficulty(value)
							),
					positioner
			);
			CreateWorldScreen.this.worldCreator.addListener(creator -> {
				cyclingButtonWidget2.setValue(CreateWorldScreen.this.worldCreator.getDifficulty());
				cyclingButtonWidget2.active = !CreateWorldScreen.this.worldCreator.isHardcore();
				cyclingButtonWidget2.setTooltip(Tooltip.of(CreateWorldScreen.this.worldCreator
						.getDifficulty()
						.getInfo()));
			});
			CyclingButtonWidget<Boolean> cyclingButtonWidget3 = adder.add(
					CyclingButtonWidget.onOffBuilder(CreateWorldScreen.this.worldCreator.areCheatsEnabled())
					                   .tooltip(value -> Tooltip.of(CreateWorldScreen.ALLOW_COMMANDS_INFO_TEXT))
					                   .build(
							                   0,
							                   0,
							                   BUTTON_WIDTH,
							                   20,
							                   ALLOW_COMMANDS_TEXT,
							                   (button, value) -> CreateWorldScreen.this.worldCreator.setCheatsEnabled(
									                   value)
					                   )
			);
			CreateWorldScreen.this.worldCreator.addListener(creator -> {
				cyclingButtonWidget3.setValue(CreateWorldScreen.this.worldCreator.areCheatsEnabled());
				cyclingButtonWidget3.active =
						!CreateWorldScreen.this.worldCreator.isDebug()
								&& !CreateWorldScreen.this.worldCreator.isHardcore();
			});
			if (!SharedConstants.getGameVersion().stable()) {
				adder.add(
						ButtonWidget.builder(
								            CreateWorldScreen.EXPERIMENTS_TEXT,
								            button -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.worldCreator
										            .getGeneratorOptionsHolder()
										            .dataConfiguration())
						            )
						            .width(BUTTON_WIDTH)
						            .build()
				);
			}
		}
	}

	/**
	 * Вкладка «Дополнительно» — правила игры, эксперименты и датапаки.
	 */
	@Environment(EnvType.CLIENT)
	class MoreTab extends GridScreenTab {

		private static final Text MORE_TAB_TITLE_TEXT = Text.translatable("createWorld.tab.more.title");
		private static final Text GAME_RULES_TEXT = Text.translatable("selectWorld.gameRules");
		private static final Text DATA_PACKS_TEXT = Text.translatable("selectWorld.dataPacks");

		MoreTab() {
			super(MORE_TAB_TITLE_TEXT);
			GridWidget.Adder adder = this.grid.setRowSpacing(8).createAdder(1);
			adder.add(ButtonWidget.builder(GAME_RULES_TEXT, button -> this.openGameRulesScreen()).width(BUTTON_WIDTH).build());
			adder.add(
					ButtonWidget.builder(
							            CreateWorldScreen.EXPERIMENTS_TEXT,
							            button -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.worldCreator
									            .getGeneratorOptionsHolder()
									            .dataConfiguration())
					            )
					            .width(BUTTON_WIDTH)
					            .build()
			);
			adder.add(
					ButtonWidget.builder(
							            DATA_PACKS_TEXT,
							            button -> CreateWorldScreen.this.openPackScreen(CreateWorldScreen.this.worldCreator
									            .getGeneratorOptionsHolder()
									            .dataConfiguration())
					            )
					            .width(BUTTON_WIDTH)
					            .build()
			);
		}

		private void openGameRulesScreen() {
			CreateWorldScreen.this.client
					.setScreen(
							new EditGameRulesScreen(
									CreateWorldScreen.this.worldCreator
											.getGameRules()
											.withEnabledFeatures(CreateWorldScreen.this.worldCreator
													.getGeneratorOptionsHolder()
													.dataConfiguration()
													.enabledFeatures()),
									gameRules -> {
										CreateWorldScreen.this.client.setScreen(CreateWorldScreen.this);
										gameRules.ifPresent(CreateWorldScreen.this.worldCreator::setGameRules);
									}
							)
					);
		}
	}

	/**
	 * Вкладка «Мир» — тип генератора, сид, структуры и бонусный сундук.
	 */
	@Environment(EnvType.CLIENT)
	class WorldTab extends GridScreenTab {

		private static final Text WORLD_TAB_TITLE_TEXT = Text.translatable("createWorld.tab.world.title");
		private static final Text
				AMPLIFIED_GENERATOR_INFO_TEXT =
				Text.translatable("generator.minecraft.amplified.info");
		private static final Text MAP_FEATURES_TEXT = Text.translatable("selectWorld.mapFeatures");
		private static final Text MAP_FEATURES_INFO_TEXT = Text.translatable("selectWorld.mapFeatures.info");
		private static final Text BONUS_ITEMS_TEXT = Text.translatable("selectWorld.bonusItems");
		private static final Text ENTER_SEED_TEXT = Text.translatable("selectWorld.enterSeed");
		static final Text SEED_INFO_TEXT = Text.translatable("selectWorld.seedInfo");
		private static final int TAB_WIDTH = 310;
		private final TextFieldWidget seedField;
		private final ButtonWidget customizeButton;

		WorldTab() {
			super(WORLD_TAB_TITLE_TEXT);
			GridWidget.Adder adder = this.grid.setColumnSpacing(CONTENT_PADDING).setRowSpacing(8).createAdder(2);
			CyclingButtonWidget<WorldCreator.WorldType> cyclingButtonWidget = adder.add(
					CyclingButtonWidget
							.builder(
									WorldCreator.WorldType::getName,
									CreateWorldScreen.this.worldCreator.getWorldType()
							)
							.values(this.getWorldTypes())
							.narration(CreateWorldScreen.WorldTab::getWorldTypeNarrationMessage)
							.build(
									0,
									0,
									150,
									20,
									Text.translatable("selectWorld.mapType"),
									(button, worldType) -> CreateWorldScreen.this.worldCreator.setWorldType(worldType)
							)
			);
			cyclingButtonWidget.setValue(CreateWorldScreen.this.worldCreator.getWorldType());
			CreateWorldScreen.this.worldCreator.addListener(creator -> {
				WorldCreator.WorldType worldType = creator.getWorldType();
				cyclingButtonWidget.setValue(worldType);
				if (worldType.isAmplified()) {
					cyclingButtonWidget.setTooltip(Tooltip.of(AMPLIFIED_GENERATOR_INFO_TEXT));
				}
				else {
					cyclingButtonWidget.setTooltip(null);
				}

				cyclingButtonWidget.active = CreateWorldScreen.this.worldCreator.getWorldType().preset() != null;
			});
			this.customizeButton =
					adder.add(ButtonWidget
							.builder(
									Text.translatable("selectWorld.customizeType"),
									button -> this.openCustomizeScreen()
							)
							.build());
			CreateWorldScreen.this.worldCreator
					.addListener(creator ->
							this.customizeButton.active =
									!creator.isDebug() && creator.getLevelScreenProvider() != null);
			this.seedField =
					new TextFieldWidget(
							CreateWorldScreen.this.textRenderer,
							308,
							20,
							Text.translatable("selectWorld.enterSeed")
					) {
						@Override
						protected MutableText getNarrationMessage() {
							return super
									.getNarrationMessage()
									.append(ScreenTexts.SENTENCE_SEPARATOR)
									.append(CreateWorldScreen.WorldTab.SEED_INFO_TEXT);
						}
					};
			this.seedField.setPlaceholder(SEED_INFO_TEXT);
			this.seedField.setText(CreateWorldScreen.this.worldCreator.getSeed());
			this.seedField.setChangedListener(seed -> CreateWorldScreen.this.worldCreator.setSeed(this.seedField.getText()));
			adder.add(
					LayoutWidgets.createLabeledWidget(
							CreateWorldScreen.this.textRenderer,
							this.seedField,
							ENTER_SEED_TEXT
					), 2
			);
			WorldScreenOptionGrid.Builder builder = WorldScreenOptionGrid.builder(TAB_WIDTH);
			builder.add(
					       MAP_FEATURES_TEXT,
					       CreateWorldScreen.this.worldCreator::shouldGenerateStructures,
					       CreateWorldScreen.this.worldCreator::setGenerateStructures
			       )
			       .toggleable(() -> !CreateWorldScreen.this.worldCreator.isDebug())
			       .tooltip(MAP_FEATURES_INFO_TEXT);
			builder
					.add(
							BONUS_ITEMS_TEXT,
							CreateWorldScreen.this.worldCreator::isBonusChestEnabled,
							CreateWorldScreen.this.worldCreator::setBonusChestEnabled
					)
					.toggleable(() -> !CreateWorldScreen.this.worldCreator.isHardcore()
							&& !CreateWorldScreen.this.worldCreator.isDebug());
			WorldScreenOptionGrid worldScreenOptionGrid = builder.build();
			adder.add(worldScreenOptionGrid.getLayout(), 2);
			CreateWorldScreen.this.worldCreator.addListener(creator -> worldScreenOptionGrid.refresh());
		}

		private void openCustomizeScreen() {
			LevelScreenProvider levelScreenProvider = CreateWorldScreen.this.worldCreator.getLevelScreenProvider();
			if (levelScreenProvider != null) {
				CreateWorldScreen.this.client
						.setScreen(levelScreenProvider.createEditScreen(
								CreateWorldScreen.this,
								CreateWorldScreen.this.worldCreator.getGeneratorOptionsHolder()
						));
			}
		}

		private CyclingButtonWidget.Values<WorldCreator.WorldType> getWorldTypes() {
			return new CyclingButtonWidget.Values<WorldCreator.WorldType>() {
				@Override
				public List<WorldCreator.WorldType> getCurrent() {
					return CyclingButtonWidget.HAS_ALT_DOWN.getAsBoolean()
					       ? CreateWorldScreen.this.worldCreator.getExtendedWorldTypes()
					       : CreateWorldScreen.this.worldCreator.getNormalWorldTypes();
				}

				@Override
				public List<WorldCreator.WorldType> getDefaults() {
					return CreateWorldScreen.this.worldCreator.getNormalWorldTypes();
				}
			};
		}

		private static MutableText getWorldTypeNarrationMessage(CyclingButtonWidget<WorldCreator.WorldType> worldTypeButton) {
			return worldTypeButton.getValue().isAmplified()
			       ? ScreenTexts.joinSentences(
					worldTypeButton.getGenericNarrationMessage(),
					AMPLIFIED_GENERATOR_INFO_TEXT
			)
			       : worldTypeButton.getGenericNarrationMessage();
		}
	}
}
