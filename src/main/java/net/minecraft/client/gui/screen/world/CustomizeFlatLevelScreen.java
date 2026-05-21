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

@Environment(EnvType.CLIENT)
/**
 * {@code CustomizeFlatLevelScreen}.
 */
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
		return this.config;
	}

	public void setConfig(FlatChunkGeneratorConfig config) {
		this.config = config;
		if (this.layers != null) {
			this.layers.updateLayers();
			this.updateRemoveLayerButton();
		}
	}

	@Override
	protected void init() {
		this.layout.addHeader(this.title, this.textRenderer);
		this.layers = this.layout.addBody(new CustomizeFlatLevelScreen.SuperflatLayersListWidget());
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addFooter(DirectionalLayoutWidget.vertical().spacing(4));
		directionalLayoutWidget.getMainPositioner().alignVerticalCenter();
		DirectionalLayoutWidget
				directionalLayoutWidget2 =
				directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
		DirectionalLayoutWidget
				directionalLayoutWidget3 =
				directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
		this.widgetButtonRemoveLayer = directionalLayoutWidget2.add(
				ButtonWidget.builder(
						            Text.translatable("createWorld.customize.flat.removeLayer"),
						            button -> {
							            if (this.layers != null
									            && this.layers.getSelectedOrNull() instanceof CustomizeFlatLevelScreen.SuperflatLayersListWidget.SuperflatLayerEntry superflatLayerEntry) {
								            this.layers.removeLayer(superflatLayerEntry);
							            }
						            }
				            )
				            .build()
		);
		directionalLayoutWidget2.add(ButtonWidget.builder(
				Text.translatable("createWorld.customize.presets"), button -> {
					this.client.setScreen(new PresetsScreen(this));
					this.config.updateLayerBlocks();
					this.updateRemoveLayerButton();
				}
		).build());
		directionalLayoutWidget3.add(ButtonWidget.builder(
				ScreenTexts.DONE, button -> {
					this.configConsumer.accept(this.config);
					this.close();
					this.config.updateLayerBlocks();
				}
		).build());
		directionalLayoutWidget3.add(ButtonWidget.builder(
				ScreenTexts.CANCEL, button -> {
					this.close();
					this.config.updateLayerBlocks();
				}
		).build());
		this.config.updateLayerBlocks();
		this.updateRemoveLayerButton();
		this.layout.forEachChild(this::addDrawableChild);
		this.refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (this.layers != null) {
			this.layers.position(this.width, this.layout);
		}

		this.layout.refreshPositions();
	}

	void updateRemoveLayerButton() {
		if (this.widgetButtonRemoveLayer != null) {
			this.widgetButtonRemoveLayer.active = this.hasLayerSelected();
		}
	}

	private boolean hasLayerSelected() {
		return this.layers != null
				&& this.layers.getSelectedOrNull() instanceof CustomizeFlatLevelScreen.SuperflatLayersListWidget.SuperflatLayerEntry;
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SuperflatLayersListWidget}.
	 */
	class SuperflatLayersListWidget extends AlwaysSelectedEntryListWidget<CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry> {

		static final Text
				LAYER_MATERIAL_TEXT =
				Text.translatable("createWorld.customize.flat.tile").formatted(Formatting.UNDERLINE);
		static final Text
				HEIGHT_TEXT =
				Text.translatable("createWorld.customize.flat.height").formatted(Formatting.UNDERLINE);

		public SuperflatLayersListWidget() {
			super(
					CustomizeFlatLevelScreen.this.client,
					CustomizeFlatLevelScreen.this.width,
					CustomizeFlatLevelScreen.this.height - 103,
					43,
					24
			);
			this.refreshLayers();
		}

		private void refreshLayers() {
			this.addEntry(
					new CustomizeFlatLevelScreen.SuperflatLayersListWidget.HeaderEntry(CustomizeFlatLevelScreen.this.textRenderer),
					(int) (9.0 * 1.5)
			);
			List<FlatChunkGeneratorLayer> list = CustomizeFlatLevelScreen.this.config.getLayers().reversed();

			for (int i = 0; i < list.size(); i++) {
				this.addEntry(new CustomizeFlatLevelScreen.SuperflatLayersListWidget.SuperflatLayerEntry(
						list.get(i),
						i
				));
			}
		}

		public void setSelected(CustomizeFlatLevelScreen.SuperflatLayersListWidget.@Nullable Entry entry) {
			super.setSelected(entry);
			CustomizeFlatLevelScreen.this.updateRemoveLayerButton();
		}

		public void updateLayers() {
			int i = this.children().indexOf(this.getSelectedOrNull());
			this.clearEntries();
			this.refreshLayers();
			List<CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry> list = this.children();
			if (i >= 0 && i < list.size()) {
				this.setSelected(list.get(i));
			}
		}

		void removeLayer(CustomizeFlatLevelScreen.SuperflatLayersListWidget.SuperflatLayerEntry layer) {
			List<FlatChunkGeneratorLayer> list = CustomizeFlatLevelScreen.this.config.getLayers();
			int i = this.children().indexOf(layer);
			this.removeEntry(layer);
			list.remove(layer.layer);
			this.setSelected(list.isEmpty() ? null : this.children().get(Math.min(i, list.size())));
			CustomizeFlatLevelScreen.this.config.updateLayerBlocks();
			this.updateLayers();
			CustomizeFlatLevelScreen.this.updateRemoveLayerButton();
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Entry}.
		 */
		abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry> {
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code HeaderEntry}.
		 */
		static class HeaderEntry extends CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry {

			private final TextRenderer textRenderer;

			public HeaderEntry(TextRenderer textRenderer) {
				this.textRenderer = textRenderer;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				context.drawTextWithShadow(
						this.textRenderer,
						CustomizeFlatLevelScreen.SuperflatLayersListWidget.LAYER_MATERIAL_TEXT,
						this.getContentX(),
						this.getContentY(),
						-1
				);
				context.drawTextWithShadow(
						this.textRenderer,
						CustomizeFlatLevelScreen.SuperflatLayersListWidget.HEIGHT_TEXT,
						this.getContentRightEnd()
								- this.textRenderer.getWidth(CustomizeFlatLevelScreen.SuperflatLayersListWidget.HEIGHT_TEXT),
						this.getContentY(),
						-1
				);
			}

			@Override
			public Text getNarration() {
				return ScreenTexts.joinSentences(
						CustomizeFlatLevelScreen.SuperflatLayersListWidget.LAYER_MATERIAL_TEXT,
						CustomizeFlatLevelScreen.SuperflatLayersListWidget.HEIGHT_TEXT
				);
			}
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code SuperflatLayerEntry}.
		 */
		class SuperflatLayerEntry extends CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry {

			final FlatChunkGeneratorLayer layer;
			private final int index;

			public SuperflatLayerEntry(final FlatChunkGeneratorLayer layer, final int index) {
				this.layer = layer;
				this.index = index;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				BlockState blockState = this.layer.getBlockState();
				ItemStack itemStack = this.createItemStackFor(blockState);
				this.renderIcon(context, this.getContentX(), this.getContentY(), itemStack);
				int i = this.getContentMiddleY() - 9 / 2;
				context.drawTextWithShadow(
						CustomizeFlatLevelScreen.this.textRenderer,
						itemStack.getName(),
						this.getContentX() + 18 + 5,
						i,
						-1
				);
				Text text;
				if (this.index == 0) {
					text = Text.translatable("createWorld.customize.flat.layer.top", this.layer.getThickness());
				}
				else if (this.index == CustomizeFlatLevelScreen.this.config.getLayers().size() - 1) {
					text = Text.translatable("createWorld.customize.flat.layer.bottom", this.layer.getThickness());
				}
				else {
					text = Text.translatable("createWorld.customize.flat.layer", this.layer.getThickness());
				}

				context.drawTextWithShadow(
						CustomizeFlatLevelScreen.this.textRenderer,
						text,
						this.getContentRightEnd() - CustomizeFlatLevelScreen.this.textRenderer.getWidth(text),
						i,
						-1
				);
			}

			private ItemStack createItemStackFor(BlockState state) {
				Item item = state.getBlock().asItem();
				if (item == Items.AIR) {
					if (state.isOf(Blocks.WATER)) {
						item = Items.WATER_BUCKET;
					}
					else if (state.isOf(Blocks.LAVA)) {
						item = Items.LAVA_BUCKET;
					}
				}

				return new ItemStack(item);
			}

			@Override
			public Text getNarration() {
				ItemStack itemStack = this.createItemStackFor(this.layer.getBlockState());
				return (Text) (!itemStack.isEmpty()
				               ? ScreenTexts.joinSentences(
						Text.translatable("narrator.select", itemStack.getName()),
						CustomizeFlatLevelScreen.SuperflatLayersListWidget.HEIGHT_TEXT,
						Text.literal(String.valueOf(this.layer.getThickness()))
				)
				               : ScreenTexts.EMPTY
				);
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				SuperflatLayersListWidget.this.setSelected((CustomizeFlatLevelScreen.SuperflatLayersListWidget.Entry) this);
				return super.mouseClicked(click, doubled);
			}

			private void renderIcon(DrawContext context, int x, int y, ItemStack iconItem) {
				this.renderIconBackgroundTexture(context, x + 1, y + 1);
				if (!iconItem.isEmpty()) {
					context.drawItemWithoutEntity(iconItem, x + 2, y + 2);
				}
			}

			private void renderIconBackgroundTexture(DrawContext context, int x, int y) {
				context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						CustomizeFlatLevelScreen.SLOT_TEXTURE,
						x,
						y,
						18,
						18
				);
			}
		}
	}
}
