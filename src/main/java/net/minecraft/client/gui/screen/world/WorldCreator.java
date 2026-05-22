package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.tag.WorldPresetTags;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.text.Text;
import net.minecraft.util.path.PathUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * Хранит и управляет всеми параметрами создания нового мира:
 * режим игры, сложность, сид, тип генератора, правила игры и т.д.
 * Уведомляет подписчиков об изменениях через список listeners.
 */
@Environment(EnvType.CLIENT)
public class WorldCreator {

	private static final Text NEW_WORLD_NAME = Text.translatable("selectWorld.newWorld");
	private final List<Consumer<WorldCreator>> listeners = new ArrayList<>();
	private String worldName = NEW_WORLD_NAME.getString();
	private WorldCreator.Mode gameMode = WorldCreator.Mode.SURVIVAL;
	private Difficulty difficulty = Difficulty.NORMAL;
	private @Nullable Boolean cheatsEnabled;
	private String seed;
	private boolean generateStructures;
	private boolean bonusChestEnabled;
	private final Path savesDirectory;
	private String worldDirectoryName;
	private GeneratorOptionsHolder generatorOptionsHolder;
	private WorldCreator.WorldType worldType;
	private final List<WorldCreator.WorldType> normalWorldTypes = new ArrayList<>();
	private final List<WorldCreator.WorldType> extendedWorldTypes = new ArrayList<>();
	private GameRules gameRules;

	public WorldCreator(
			Path savesDirectory,
			GeneratorOptionsHolder generatorOptionsHolder,
			Optional<RegistryKey<WorldPreset>> defaultWorldType,
			OptionalLong seed
	) {
		this.savesDirectory = savesDirectory;
		this.generatorOptionsHolder = generatorOptionsHolder;
		this.worldType =
				new WorldCreator.WorldType(getWorldPreset(generatorOptionsHolder, defaultWorldType).orElse(null));
		this.updateWorldTypeLists();
		this.seed = seed.isPresent() ? Long.toString(seed.getAsLong()) : "";
		this.generateStructures = generatorOptionsHolder.generatorOptions().shouldGenerateStructures();
		this.bonusChestEnabled = generatorOptionsHolder.generatorOptions().hasBonusChest();
		this.worldDirectoryName = this.toDirectoryName(this.worldName);
		this.gameMode = generatorOptionsHolder.initialWorldCreationOptions().selectedGameMode();
		this.gameRules = new GameRules(generatorOptionsHolder.dataConfiguration().enabledFeatures());
		this.gameRules.copyFrom(generatorOptionsHolder.initialWorldCreationOptions().gameRuleOverwrites(), null);
		Optional.ofNullable(generatorOptionsHolder.initialWorldCreationOptions().flatLevelPreset())
		        .flatMap(
				        presetKey -> generatorOptionsHolder.getCombinedRegistryManager()
				                                           .getOptional(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET)
				                                           .flatMap(registry -> registry.getOptional(presetKey))
		        )
		        .map(preset -> preset.value().settings())
		        .ifPresent(config -> this.applyModifier(LevelScreenProvider.createModifier(config)));
	}

	public void addListener(Consumer<WorldCreator> listener) {
		listeners.add(listener);
	}

	public void update() {
		boolean bonusChest = isBonusChestEnabled();
		if (bonusChest != generatorOptionsHolder.generatorOptions().hasBonusChest()) {
			generatorOptionsHolder = generatorOptionsHolder.apply(options -> options.withBonusChest(bonusChest));
		}

		boolean generateStructures = shouldGenerateStructures();
		if (generateStructures != generatorOptionsHolder.generatorOptions().shouldGenerateStructures()) {
			generatorOptionsHolder = generatorOptionsHolder.apply(options -> options.withStructures(generateStructures));
		}

		for (Consumer<WorldCreator> listener : listeners) {
			listener.accept(this);
		}
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
		this.worldDirectoryName = this.toDirectoryName(worldName);
		this.update();
	}

	private String toDirectoryName(String worldName) {
		String string = worldName.trim();

		try {
			return PathUtil.getNextUniqueName(
					this.savesDirectory,
					!string.isEmpty() ? string : NEW_WORLD_NAME.getString(),
					""
			);
		}
		catch (Exception var5) {
			try {
				return PathUtil.getNextUniqueName(this.savesDirectory, "World", "");
			}
			catch (IOException var4) {
				throw new RuntimeException("Could not create save folder", var4);
			}
		}
	}

	public String getWorldName() {
		return this.worldName;
	}

	public String getWorldDirectoryName() {
		return this.worldDirectoryName;
	}

	public void setGameMode(WorldCreator.Mode gameMode) {
		this.gameMode = gameMode;
		this.update();
	}

	public WorldCreator.Mode getGameMode() {
		return this.isDebug() ? WorldCreator.Mode.DEBUG : this.gameMode;
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
		this.update();
	}

	public Difficulty getDifficulty() {
		return this.isHardcore() ? Difficulty.HARD : this.difficulty;
	}

	public boolean isHardcore() {
		return this.getGameMode() == WorldCreator.Mode.HARDCORE;
	}

	public void setCheatsEnabled(boolean cheatsEnabled) {
		this.cheatsEnabled = cheatsEnabled;
		this.update();
	}

	/**
	 * Определяет, включены ли читы с учётом режима игры.
	 * В debug-мире читы всегда включены, в hardcore — всегда выключены.
	 */
	public boolean areCheatsEnabled() {
		if (isDebug()) {
			return true;
		}

		if (isHardcore()) {
			return false;
		}

		return cheatsEnabled == null ? getGameMode() == WorldCreator.Mode.CREATIVE : cheatsEnabled;
	}

	public void setSeed(String seed) {
		this.seed = seed;
		this.generatorOptionsHolder =
				this.generatorOptionsHolder.apply(options -> options.withSeed(GeneratorOptions.parseSeed(this.getSeed())));
		this.update();
	}

	public String getSeed() {
		return this.seed;
	}

	public void setGenerateStructures(boolean generateStructures) {
		this.generateStructures = generateStructures;
		this.update();
	}

	public boolean shouldGenerateStructures() {
		return isDebug() ? false : generateStructures;
	}

	public void setBonusChestEnabled(boolean bonusChestEnabled) {
		this.bonusChestEnabled = bonusChestEnabled;
		this.update();
	}

	public boolean isBonusChestEnabled() {
		return !isDebug() && !isHardcore() ? bonusChestEnabled : false;
	}

	public void setGeneratorOptionsHolder(GeneratorOptionsHolder generatorOptionsHolder) {
		this.generatorOptionsHolder = generatorOptionsHolder;
		this.updateWorldTypeLists();
		this.update();
	}

	public GeneratorOptionsHolder getGeneratorOptionsHolder() {
		return this.generatorOptionsHolder;
	}

	public void applyModifier(GeneratorOptionsHolder.RegistryAwareModifier modifier) {
		generatorOptionsHolder = generatorOptionsHolder.apply(modifier);
		update();
	}

	/**
	 * Обновляет конфигурацию датапаков без перезагрузки генератора мира,
	 * если набор включённых паков и фич не изменился.
	 */
	protected boolean updateDataConfiguration(DataConfiguration dataConfiguration) {
		DataConfiguration dataConfiguration2 = this.generatorOptionsHolder.dataConfiguration();
		if (dataConfiguration2.dataPacks().getEnabled().equals(dataConfiguration.dataPacks().getEnabled())
				&& dataConfiguration2.enabledFeatures().equals(dataConfiguration.enabledFeatures())) {
			this.generatorOptionsHolder = new GeneratorOptionsHolder(
					this.generatorOptionsHolder.generatorOptions(),
					this.generatorOptionsHolder.dimensionOptionsRegistry(),
					this.generatorOptionsHolder.selectedDimensions(),
					this.generatorOptionsHolder.combinedDynamicRegistries(),
					this.generatorOptionsHolder.dataPackContents(),
					dataConfiguration,
					this.generatorOptionsHolder.initialWorldCreationOptions()
			);
			return true;
		}
		else {
			return false;
		}
	}

	public boolean isDebug() {
		return this.generatorOptionsHolder.selectedDimensions().isDebug();
	}

	public void setWorldType(WorldCreator.WorldType worldType) {
		this.worldType = worldType;
		RegistryEntry<WorldPreset> preset = worldType.preset();
		if (preset != null) {
			applyModifier((registryManager, registryHolder) -> preset
					.value()
					.createDimensionsRegistryHolder());
		}
	}

	public WorldCreator.WorldType getWorldType() {
		return this.worldType;
	}

	public @Nullable LevelScreenProvider getLevelScreenProvider() {
		RegistryEntry<WorldPreset> preset = getWorldType().preset();
		return preset != null
				? LevelScreenProvider.WORLD_PRESET_TO_SCREEN_PROVIDER.get(preset.getKey())
				: null;
	}

	public List<WorldCreator.WorldType> getNormalWorldTypes() {
		return this.normalWorldTypes;
	}

	public List<WorldCreator.WorldType> getExtendedWorldTypes() {
		return this.extendedWorldTypes;
	}

	private void updateWorldTypeLists() {
		Registry<WorldPreset> registry =
				getGeneratorOptionsHolder().getCombinedRegistryManager().getOrThrow(RegistryKeys.WORLD_PRESET);
		normalWorldTypes.clear();
		normalWorldTypes.addAll(getWorldPresetList(registry, WorldPresetTags.NORMAL).orElseGet(() -> registry
				.streamEntries()
				.map(WorldCreator.WorldType::new)
				.toList()));
		extendedWorldTypes.clear();
		extendedWorldTypes.addAll(getWorldPresetList(registry, WorldPresetTags.EXTENDED).orElse(normalWorldTypes));

		RegistryEntry<WorldPreset> currentPreset = worldType.preset();
		if (currentPreset == null) {
			return;
		}

		WorldCreator.WorldType resolvedType = getWorldPreset(getGeneratorOptionsHolder(), currentPreset.getKey())
				.map(WorldCreator.WorldType::new)
				.orElse(normalWorldTypes.getFirst());
		boolean hasCustomScreen = LevelScreenProvider.WORLD_PRESET_TO_SCREEN_PROVIDER.get(currentPreset.getKey()) != null;

		if (hasCustomScreen) {
			worldType = resolvedType;
		}
		else {
			setWorldType(resolvedType);
		}
	}

	private static Optional<RegistryEntry<WorldPreset>> getWorldPreset(
			GeneratorOptionsHolder generatorOptionsHolder,
			Optional<RegistryKey<WorldPreset>> key
	) {
		return key.flatMap(
				key2 -> generatorOptionsHolder
						.getCombinedRegistryManager()
						.getOrThrow(RegistryKeys.WORLD_PRESET)
						.getOptional((RegistryKey<WorldPreset>) key2)
		);
	}

	private static Optional<List<WorldCreator.WorldType>> getWorldPresetList(
			Registry<WorldPreset> registry,
			TagKey<WorldPreset> tag
	) {
		return registry.getOptional(tag)
		               .map(entryList -> entryList.stream().map(WorldCreator.WorldType::new).toList())
		               .filter(worldTypeList -> !worldTypeList.isEmpty());
	}

	public void setGameRules(GameRules gameRules) {
		this.gameRules = gameRules;
		this.update();
	}

	public GameRules getGameRules() {
		return this.gameRules;
	}

	@Environment(EnvType.CLIENT)
	public enum Mode {
		SURVIVAL("survival", GameMode.SURVIVAL),
		HARDCORE("hardcore", GameMode.SURVIVAL),
		CREATIVE("creative", GameMode.CREATIVE),
		DEBUG("spectator", GameMode.SPECTATOR);

		public final GameMode defaultGameMode;
		public final Text name;
		private final Text info;

		private Mode(final String name, final GameMode defaultGameMode) {
			this.defaultGameMode = defaultGameMode;
			this.name = Text.translatable("selectWorld.gameMode." + name);
			this.info = Text.translatable("selectWorld.gameMode." + name + ".info");
		}

		public Text getInfo() {
			return this.info;
		}
	}

	@Environment(EnvType.CLIENT)
	public record WorldType(@Nullable RegistryEntry<WorldPreset> preset) {

		private static final Text CUSTOM_GENERATOR_TEXT = Text.translatable("generator.custom");

		public Text getName() {
			return Optional.ofNullable(this.preset)
			               .flatMap(RegistryEntry::getKey)
			               .<Text>map(key -> Text.translatable(key.getValue().toTranslationKey("generator")))
			               .orElse(CUSTOM_GENERATOR_TEXT);
		}

		public boolean isAmplified() {
			return Optional
					.ofNullable(this.preset)
					.flatMap(RegistryEntry::getKey)
					.filter(key -> key.equals(WorldPresets.AMPLIFIED))
					.isPresent();
		}
	}
}
