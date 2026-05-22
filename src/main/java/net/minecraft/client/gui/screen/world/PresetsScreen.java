package net.minecraft.client.gui.screen.world;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FlatLevelGeneratorPresetTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.structure.StructureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Экран выбора пресета плоского мира (Superflat).
 * Позволяет выбрать готовый пресет или ввести строку конфигурации вручную.
 */
@Environment(EnvType.CLIENT)
public class PresetsScreen extends Screen {

	static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
	static final Logger LOGGER = LogUtils.getLogger();
	private static final int ICON_SIZE = 18;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ICON_BACKGROUND_OFFSET_X = 1;
	private static final int ICON_BACKGROUND_OFFSET_Y = 1;
	private static final int ICON_OFFSET_X = 2;
	private static final int ICON_OFFSET_Y = 2;
	private static final RegistryKey<Biome> BIOME_KEY = BiomeKeys.PLAINS;
	public static final Text UNKNOWN_PRESET_TEXT = Text.translatable("flat_world_preset.unknown");
	private final CustomizeFlatLevelScreen parent;
	private Text shareText;
	private Text listText;
	private PresetsScreen.SuperflatPresetsListWidget listWidget;
	private ButtonWidget selectPresetButton;
	TextFieldWidget customPresetField;
	FlatChunkGeneratorConfig config;

	public PresetsScreen(CustomizeFlatLevelScreen parent) {
		super(Text.translatable("createWorld.customize.presets.title"));
		this.parent = parent;
	}

	private static @Nullable FlatChunkGeneratorLayer parseLayerString(
			RegistryEntryLookup<Block> blockLookup,
			String layer,
			int layerStartHeight
	) {
		List<String> parts = Splitter.on('*').limit(2).splitToList(layer);
		String blockId;
		int layerCount;

		if (parts.size() == 2) {
			blockId = parts.get(1);
			try {
				layerCount = Math.max(Integer.parseInt(parts.get(0)), 0);
			}
			catch (NumberFormatException exception) {
				LOGGER.error("Error while parsing flat world string", exception);
				return null;
			}
		}
		else {
			blockId = parts.get(0);
			layerCount = 1;
		}

		int maxHeight = Math.min(layerStartHeight + layerCount, DimensionType.MAX_HEIGHT);
		int actualThickness = maxHeight - layerStartHeight;

		Optional<RegistryEntry.Reference<Block>> blockEntry;
		try {
			blockEntry = blockLookup.getOptional(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(blockId)));
		}
		catch (Exception exception) {
			LOGGER.error("Error while parsing flat world string", exception);
			return null;
		}

		if (blockEntry.isEmpty()) {
			LOGGER.error("Error while parsing flat world string => Unknown block, {}", blockId);
			return null;
		}

		return new FlatChunkGeneratorLayer(actualThickness, blockEntry.get().value());
	}

	private static List<FlatChunkGeneratorLayer> parsePresetLayersString(
			RegistryEntryLookup<Block> blockLookup,
			String layers
	) {
		List<FlatChunkGeneratorLayer> result = Lists.newArrayList();
		int currentHeight = 0;

		for (String layerStr : layers.split(",")) {
			FlatChunkGeneratorLayer layer = parseLayerString(blockLookup, layerStr, currentHeight);
			if (layer == null) {
				return Collections.emptyList();
			}

			int remainingHeight = DimensionType.MAX_HEIGHT - currentHeight;
			if (remainingHeight > 0) {
				result.add(layer.withMaxThickness(remainingHeight));
				currentHeight += layer.getThickness();
			}
		}

		return result;
	}

	/**
	 * Парсит строку пресета плоского мира формата "слои;биом" в конфигурацию генератора.
	 * Если строка некорректна или пуста — возвращает конфигурацию по умолчанию.
	 */
	public static FlatChunkGeneratorConfig parsePresetString(
			RegistryEntryLookup<Block> blockLookup,
			RegistryEntryLookup<Biome> biomeLookup,
			RegistryEntryLookup<StructureSet> structureSetLookup,
			RegistryEntryLookup<PlacedFeature> placedFeatureLookup,
			String preset,
			FlatChunkGeneratorConfig config
	) {
		Iterator<String> parts = Splitter.on(';').split(preset).iterator();
		if (!parts.hasNext()) {
			return FlatChunkGeneratorConfig.getDefaultConfig(biomeLookup, structureSetLookup, placedFeatureLookup);
		}

		List<FlatChunkGeneratorLayer> layers = parsePresetLayersString(blockLookup, parts.next());
		if (layers.isEmpty()) {
			return FlatChunkGeneratorConfig.getDefaultConfig(biomeLookup, structureSetLookup, placedFeatureLookup);
		}

		RegistryEntry.Reference<Biome> defaultBiome = biomeLookup.getOrThrow(BIOME_KEY);
		RegistryEntry<Biome> biome = defaultBiome;

		if (parts.hasNext()) {
			String biomeStr = parts.next();
			biome = Optional.ofNullable(Identifier.tryParse(biomeStr))
					.map(biomeId -> RegistryKey.of(RegistryKeys.BIOME, biomeId))
					.flatMap(biomeLookup::getOptional)
					.orElseGet(() -> {
						LOGGER.warn("Invalid biome: {}", biomeStr);
						return defaultBiome;
					});
		}

		return config.with(layers, config.getStructureOverrides(), biome);
	}

	static String getGeneratorConfigString(FlatChunkGeneratorConfig config) {
		StringBuilder builder = new StringBuilder();
		List<FlatChunkGeneratorLayer> configLayers = config.getLayers();

		for (int i = 0; i < configLayers.size(); i++) {
			if (i > 0) {
				builder.append(",");
			}

			builder.append(configLayers.get(i));
		}

		builder.append(";");
		builder.append(config
				.getBiome()
				.getKey()
				.map(RegistryKey::getValue)
				.orElseThrow(() -> new IllegalStateException("Biome not registered")));
		return builder.toString();
	}

	@Override
	protected void init() {
		shareText = Text.translatable("createWorld.customize.presets.share");
		listText = Text.translatable("createWorld.customize.presets.list");
		customPresetField = new TextFieldWidget(textRenderer, 50, 40, width - 100, BUTTON_HEIGHT, shareText);
		customPresetField.setMaxLength(1230);

		GeneratorOptionsHolder generatorOptionsHolder = parent.parent.getWorldCreator().getGeneratorOptionsHolder();
		DynamicRegistryManager registryManager = generatorOptionsHolder.getCombinedRegistryManager();
		FeatureSet featureSet = generatorOptionsHolder.dataConfiguration().enabledFeatures();
		RegistryEntryLookup<Biome> biomeLookup = registryManager.getOrThrow(RegistryKeys.BIOME);
		RegistryEntryLookup<StructureSet> structureLookup = registryManager.getOrThrow(RegistryKeys.STRUCTURE_SET);
		RegistryEntryLookup<PlacedFeature> featureLookup = registryManager.getOrThrow(RegistryKeys.PLACED_FEATURE);
		RegistryEntryLookup<Block> blockLookup = registryManager.getOrThrow(RegistryKeys.BLOCK).withFeatureFilter(featureSet);

		customPresetField.setText(getGeneratorConfigString(parent.getConfig()));
		config = parent.getConfig();
		addSelectableChild(customPresetField);
		listWidget = addDrawableChild(new PresetsScreen.SuperflatPresetsListWidget(registryManager, featureSet));
		selectPresetButton = addDrawableChild(
				ButtonWidget.builder(
						Text.translatable("createWorld.customize.presets.select"),
						button -> {
							FlatChunkGeneratorConfig parsed = parsePresetString(
									blockLookup,
									biomeLookup,
									structureLookup,
									featureLookup,
									customPresetField.getText(),
									config
							);
							parent.setConfig(parsed);
							client.setScreen(parent);
						}
				)
				.dimensions(width / 2 - 155, height - 28, 150, BUTTON_HEIGHT)
				.build()
		);
		addDrawableChild(
				ButtonWidget.builder(ScreenTexts.CANCEL, button -> client.setScreen(parent))
						.dimensions(width / 2 + 5, height - 28, 150, BUTTON_HEIGHT)
						.build()
		);
		updateSelectButton(listWidget.getSelectedOrNull() != null);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return listWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void resize(int width, int height) {
		String savedText = customPresetField.getText();
		init(width, height);
		customPresetField.setText(savedText);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, -1);
		context.drawTextWithShadow(textRenderer, shareText, 51, 30, -6250336);
		context.drawTextWithShadow(textRenderer, listText, 51, 68, -6250336);
		customPresetField.render(context, mouseX, mouseY, deltaTicks);
	}

	public void updateSelectButton(boolean hasSelected) {
		selectPresetButton.active = hasSelected || customPresetField.getText().length() > 1;
	}

	@Environment(EnvType.CLIENT)
	class SuperflatPresetsListWidget extends AlwaysSelectedEntryListWidget<PresetsScreen.SuperflatPresetsListWidget.SuperflatPresetEntry> {

		public SuperflatPresetsListWidget(
				final DynamicRegistryManager dynamicRegistryManager,
				final FeatureSet featureSet
		) {
			super(PresetsScreen.this.client, PresetsScreen.this.width, PresetsScreen.this.height - 117, 80, 24);

			for (RegistryEntry<FlatLevelGeneratorPreset> registryEntry : dynamicRegistryManager
					.getOrThrow(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET)
					.iterateEntries(FlatLevelGeneratorPresetTags.VISIBLE)) {
				Set<Block> set = registryEntry.value()
				                              .settings()
				                              .getLayers()
				                              .stream()
				                              .map(layer -> layer.getBlockState().getBlock())
				                              .filter(block -> !block.isEnabled(featureSet))
				                              .collect(Collectors.toSet());
				if (!set.isEmpty()) {
					PresetsScreen.LOGGER
							.info(
									"Discarding flat world preset {} since it contains experimental blocks {}",
									registryEntry.getKey().map(key -> key.getValue().toString()).orElse("<unknown>"),
									set
							);
				}
				else {
					this.addEntry(new PresetsScreen.SuperflatPresetsListWidget.SuperflatPresetEntry(registryEntry));
				}
			}
		}

		public void setSelected(PresetsScreen.SuperflatPresetsListWidget.@Nullable SuperflatPresetEntry superflatPresetEntry) {
			super.setSelected(superflatPresetEntry);
			PresetsScreen.this.updateSelectButton(superflatPresetEntry != null);
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (super.keyPressed(input)) {
				return true;
			}
			else {
				if (input.isEnterOrSpace() && this.getSelectedOrNull() != null) {
					this.getSelectedOrNull().setPreset();
				}

				return false;
			}
		}

		@Environment(EnvType.CLIENT)
		public class SuperflatPresetEntry extends AlwaysSelectedEntryListWidget.Entry<PresetsScreen.SuperflatPresetsListWidget.SuperflatPresetEntry> {

			private static final Identifier
					STATS_ICONS_TEXTURE =
					Identifier.ofVanilla("textures/gui/container/stats_icons.png");
			private final FlatLevelGeneratorPreset preset;
			private final Text text;

			public SuperflatPresetEntry(final RegistryEntry<FlatLevelGeneratorPreset> preset) {
				this.preset = preset.value();
				this.text = preset.getKey()
				                  .<Text>map(key -> Text.translatable(key
						                  .getValue()
						                  .toTranslationKey("flat_world_preset")))
				                  .orElse(PresetsScreen.UNKNOWN_PRESET_TEXT);
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				this.renderIcon(context, this.getContentX(), this.getContentY(), this.preset.displayItem().value());
				context.drawTextWithShadow(
						PresetsScreen.this.textRenderer,
						this.text,
						this.getContentX() + ICON_SIZE + 5,
						this.getContentY() + 6,
						-1
				);
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				this.setPreset();
				return super.mouseClicked(click, doubled);
			}

			void setPreset() {
				SuperflatPresetsListWidget.this.setSelected(this);
				PresetsScreen.this.config = this.preset.settings();
				PresetsScreen.this.customPresetField.setText(PresetsScreen.getGeneratorConfigString(PresetsScreen.this.config));
				PresetsScreen.this.customPresetField.setCursorToStart(false);
			}

			private void renderIcon(DrawContext context, int x, int y, Item iconItem) {
				this.drawIconBackground(context, x + 1, y + 1);
				context.drawItemWithoutEntity(new ItemStack(iconItem), x + 2, y + 2);
			}

			private void drawIconBackground(DrawContext context, int x, int y) {
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PresetsScreen.SLOT_TEXTURE, x, y, ICON_SIZE, ICON_SIZE);
			}

			@Override
			public Text getNarration() {
				return Text.translatable("narrator.select", this.text);
			}
		}
	}
}
