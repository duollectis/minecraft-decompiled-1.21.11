package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Экран настройки суперплоского мира. Отображает список слоёв генерации
 * ({@link SuperflatLayersListWidget}) и позволяет удалять слои, переходить
 * к экрану пресетов ({@link PresetsScreen}) и подтверждать конфигурацию.
 */
@Environment(EnvType.CLIENT)
public class CustomizeFlatLevelScreen extends Screen {

	private static final Text TITLE = Text.translatable("createWorld.customize.flat.title");
	static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
	private static final int ICON_SIZE = 18;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ICON_BACKGROUND_OFFSET_X = 1;
	private static final int ICON_BACKGROUND_OFFSET_Y = 1;
	private static final int ICON_OFFSET_X = 2;
	private static final int ICON_OFFSET_Y = 2;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 33, 64);
	protected final CreateWorldScreen parent;
	private final Consumer<FlatChunkGeneratorConfig> configConsumer;
	FlatChunkGeneratorConfig config;
	private CustomizeFlatLevelScreen.@Nullable SuperflatLayersListWidget layers;
	private @Nullable ButtonWidget widgetButtonRemoveLayer;

	public CustomizeFlatLevelScreen(
			CreateWorldScreen parent,
			Consumer<FlatChunkGeneratorConfig> configConsumer,
			FlatChunkGeneratorConfig config
	) {
		super(TITLE);
		this.parent = parent;
		this.configConsumer = configConsumer;
		this.config = config;
	}

	public FlatChunkGeneratorConfig getConfig() {
		return config;
	}

	public void setConfig(FlatChunkGeneratorConfig config) {
		this.config = config;

		if (layers == null) {
			return;
		}

		layers.updateLayers();
		updateRemoveLayerButton();
	}

	@Override
	protected void init() {
		layout.addHeader(title, textRenderer);
		layers = layout.addBody(new CustomizeFlatLevelScreen.SuperflatLayersListWidget());
		DirectionalLayoutWidget footer = layout.addFooter(DirectionalLayoutWidget.vertical().spacing(4));
		footer.getMainPositioner().alignVerticalCenter();
		DirectionalLayoutWidget topRow = footer.add(DirectionalLayoutWidget.horizontal().spacing(8));
		DirectionalLayoutWidget bottomRow = footer.add(DirectionalLayoutWidget.horizontal().spacing(8));

		widgetButtonRemoveLayer = topRow.add(
				ButtonWidget.builder(
						Text.translatable("createWorld.customize.flat.removeLayer"),
						button -> {
							if (layers != null
									&& layers.getSelectedOrNull() instanceof SuperflatLayersListWidget.SuperflatLayerEntry entry) {
								layers.removeLayer(entry);
							}
						}
				).build()
		);

		topRow.add(ButtonWidget.builder(
				Text.translatable("createWorld.customize.presets"),
				button -> {
					client.setScreen(new PresetsScreen(this));
					config.updateLayerBlocks();
					updateRemoveLayerButton();
				}
		).build());

		bottomRow.add(ButtonWidget.builder(
				ScreenTexts.DONE,
				button -> {
					configConsumer.accept(config);
					close();
					config.updateLayerBlocks();
				}
		).build());

		bottomRow.add(ButtonWidget.builder(
				ScreenTexts.CANCEL,
				button -> {
					close();
					config.updateLayerBlocks();
				}
		).build());

		config.updateLayerBlocks();
		updateRemoveLayerButton();
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (layers != null) {
			layers.position(width, layout);
		}

		layout.refreshPositions();
	}

	void updateRemoveLayerButton() {
		if (widgetButtonRemoveLayer == null) {
			return;
		}

		widgetButtonRemoveLayer.active = hasLayerSelected();
	}

	private boolean hasLayerSelected() {
		return layers != null
				&& layers.getSelectedOrNull() instanceof SuperflatLayersListWidget.SuperflatLayerEntry;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	/**
	 * Список слоёв суперплоского мира. Первая строка — заголовок с названиями колонок,
	 * остальные — записи {@link SuperflatLayerEntry} в обратном порядке (верхний слой первым).
	 */
	@Environment(EnvType.CLIENT)
	class SuperflatLayersListWidget extends AlwaysSelectedEntryListWidget<CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry> {

		static final Text
				LAYER_MATERIAL_TEXT =
				Text.translatable("createWorld.customize.flat.tile").formatted(Formatting.UNDERLINE);
		static final Text
				HEIGHT_TEXT =
				Text.translatable("createWorld.customize.flat.height").formatted(Formatting.UNDERLINE);

		private static final int HEADER_HEIGHT = 13;

		public SuperflatLayersListWidget() {
			super(
					CustomizeFlatLevelScreen.this.client,
					CustomizeFlatLevelScreen.this.width,
					CustomizeFlatLevelScreen.this.height - 103,
					43,
					24
			);
			refreshLayers();
		}

		private void refreshLayers() {
			addEntry(new HeaderEntry(CustomizeFlatLevelScreen.this.textRenderer), HEADER_HEIGHT);
			List<FlatChunkGeneratorLayer> reversedLayers = CustomizeFlatLevelScreen.this.config.getLayers().reversed();

			for (int i = 0; i < reversedLayers.size(); i++) {
				addEntry(new SuperflatLayerEntry(reversedLayers.get(i), i));
			}
		}

		public void setSelected(CustomizeFlatLevelScreen.SuperflatLayersListWidget.@Nullable Entry entry) {
			super.setSelected(entry);
			CustomizeFlatLevelScreen.this.updateRemoveLayerButton();
		}

		public void updateLayers() {
			int selectedIndex = children().indexOf(getSelectedOrNull());
			clearEntries();
			refreshLayers();
			List<CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry> entries = children();

			if (selectedIndex >= 0 && selectedIndex < entries.size()) {
				setSelected(entries.get(selectedIndex));
			}
		}

		void removeLayer(CustomizeFlatLevelScreen.SuperflatLayersListWidget.SuperflatLayerEntry layer) {
			List<FlatChunkGeneratorLayer> flatLayers = CustomizeFlatLevelScreen.this.config.getLayers();
			int layerIndex = children().indexOf(layer);
			removeEntry(layer);
			flatLayers.remove(layer.layer);
			setSelected(flatLayers.isEmpty() ? null : children().get(Math.min(layerIndex, flatLayers.size())));
			CustomizeFlatLevelScreen.this.config.updateLayerBlocks();
			updateLayers();
			CustomizeFlatLevelScreen.this.updateRemoveLayerButton();
		}

		@Environment(EnvType.CLIENT)
		abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry> {
		}

		@Environment(EnvType.CLIENT)
		static class HeaderEntry extends CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry {

			private final TextRenderer textRenderer;

			public HeaderEntry(TextRenderer textRenderer) {
				this.textRenderer = textRenderer;
			}

			@Override
				public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
					context.drawTextWithShadow(
							textRenderer,
							SuperflatLayersListWidget.LAYER_MATERIAL_TEXT,
							getContentX(),
							getContentY(),
							-1
					);
					context.drawTextWithShadow(
							textRenderer,
							SuperflatLayersListWidget.HEIGHT_TEXT,
							getContentRightEnd() - textRenderer.getWidth(SuperflatLayersListWidget.HEIGHT_TEXT),
							getContentY(),
							-1
					);
				}
	
				@Override
				public Text getNarration() {
					return ScreenTexts.joinSentences(
							SuperflatLayersListWidget.LAYER_MATERIAL_TEXT,
							SuperflatLayersListWidget.HEIGHT_TEXT
					);
				}
			}
	
			@Environment(EnvType.CLIENT)
			class SuperflatLayerEntry extends CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry {

			final FlatChunkGeneratorLayer layer;
			private final int index;

			public SuperflatLayerEntry(final FlatChunkGeneratorLayer layer, final int index) {
				this.layer = layer;
				this.index = index;
			}

			@Override
				public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
					BlockState blockState = layer.getBlockState();
					ItemStack iconStack = createItemStackFor(blockState);
					renderIcon(context, getContentX(), getContentY(), iconStack);
					int textY = getContentMiddleY() - 9 / 2;
	
					context.drawTextWithShadow(
							CustomizeFlatLevelScreen.this.textRenderer,
							iconStack.getName(),
							getContentX() + ICON_SIZE + 5,
							textY,
							-1
					);
	
					int lastLayerIndex = CustomizeFlatLevelScreen.this.config.getLayers().size() - 1;
					Text layerText;
	
					if (index == 0) {
						layerText = Text.translatable("createWorld.customize.flat.layer.top", layer.getThickness());
					} else if (index == lastLayerIndex) {
						layerText = Text.translatable("createWorld.customize.flat.layer.bottom", layer.getThickness());
					} else {
						layerText = Text.translatable("createWorld.customize.flat.layer", layer.getThickness());
					}
	
					context.drawTextWithShadow(
							CustomizeFlatLevelScreen.this.textRenderer,
							layerText,
							getContentRightEnd() - CustomizeFlatLevelScreen.this.textRenderer.getWidth(layerText),
							textY,
							-1
					);
				}
	
				private ItemStack createItemStackFor(BlockState state) {
					Item item = state.getBlock().asItem();
	
					if (item == Items.AIR) {
						if (state.isOf(Blocks.WATER)) {
							item = Items.WATER_BUCKET;
						} else if (state.isOf(Blocks.LAVA)) {
							item = Items.LAVA_BUCKET;
						}
					}
	
					return new ItemStack(item);
				}
	
				@Override
				public Text getNarration() {
					ItemStack iconStack = createItemStackFor(layer.getBlockState());
					return iconStack.isEmpty()
							? ScreenTexts.EMPTY
							: ScreenTexts.joinSentences(
									Text.translatable("narrator.select", iconStack.getName()),
									SuperflatLayersListWidget.HEIGHT_TEXT,
									Text.literal(String.valueOf(layer.getThickness()))
							);
				}
	
				@Override
				public boolean mouseClicked(Click click, boolean doubled) {
					SuperflatLayersListWidget.this.setSelected(this);
					return super.mouseClicked(click, doubled);
				}
	
				private void renderIcon(DrawContext context, int x, int y, ItemStack iconItem) {
					renderIconBackgroundTexture(context, x + ICON_BACKGROUND_OFFSET_X, y + ICON_BACKGROUND_OFFSET_Y);
	
					if (!iconItem.isEmpty()) {
						context.drawItemWithoutEntity(iconItem, x + ICON_OFFSET_X, y + ICON_OFFSET_Y);
					}
				}
	
				private void renderIconBackgroundTexture(DrawContext context, int x, int y) {
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							CustomizeFlatLevelScreen.SLOT_TEXTURE,
							x,
							y,
							ICON_SIZE,
							ICON_SIZE
					);
				}
		}
	}
}
