package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.LoadingTab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Экран статистики игрока с тремя вкладками: общая, предметы и мобы.
 */
@Environment(EnvType.CLIENT)
public class StatsScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("gui.stats");
	static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
	static final Identifier HEADER_TEXTURE = Identifier.ofVanilla("statistics/header");
	static final Identifier SORT_UP_TEXTURE = Identifier.ofVanilla("statistics/sort_up");
	static final Identifier SORT_DOWN_TEXTURE = Identifier.ofVanilla("statistics/sort_down");
	private static final Text DOWNLOADING_STATS_TEXT = Text.translatable("multiplayer.downloadingStats");
	static final Text NONE_TEXT = Text.translatable("stats.none");
	private static final Text GENERAL_BUTTON_TEXT = Text.translatable("stat.generalButton");
	private static final Text ITEM_BUTTON_TEXT = Text.translatable("stat.itemsButton");
	private static final Text MOBS_BUTTON_TEXT = Text.translatable("stat.mobsButton");
	protected final Screen parent;
	private static final int SCREEN_WIDTH = 280;
	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final TabManager tabManager = new TabManager(
		child -> addDrawableChild(child),
		child -> remove(child)
	);
	private @Nullable TabNavigationWidget tabNavigationWidget;
	final StatHandler statHandler;
	private boolean downloadingStats = true;

	public StatsScreen(Screen parent, StatHandler statHandler) {
		super(TITLE_TEXT);
		this.parent = parent;
		this.statHandler = statHandler;
	}

	@Override
	protected void init() {
		tabNavigationWidget = TabNavigationWidget.builder(tabManager, width)
			.tabs(
				new LoadingTab(getTextRenderer(), GENERAL_BUTTON_TEXT, DOWNLOADING_STATS_TEXT),
				new LoadingTab(getTextRenderer(), ITEM_BUTTON_TEXT, DOWNLOADING_STATS_TEXT),
				new LoadingTab(getTextRenderer(), MOBS_BUTTON_TEXT, DOWNLOADING_STATS_TEXT)
			)
			.build();
		addDrawableChild(tabNavigationWidget);
		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
		tabNavigationWidget.setTabActive(0, true);
		tabNavigationWidget.setTabActive(1, false);
		tabNavigationWidget.setTabActive(2, false);
		layout.forEachChild(child -> {
			child.setNavigationOrder(1);
			addDrawableChild(child);
		});
		tabNavigationWidget.selectTab(0, false);
		refreshWidgetPositions();
		client.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
	}

	public void onStatsReady() {
		if (!downloadingStats) {
			return;
		}

		if (tabNavigationWidget != null) {
			remove(tabNavigationWidget);
		}

		tabNavigationWidget = TabNavigationWidget.builder(tabManager, width)
			.tabs(
				new StatsTab(GENERAL_BUTTON_TEXT, new GeneralStatsListWidget(client)),
				new StatsTab(ITEM_BUTTON_TEXT, new ItemStatsListWidget(client)),
				new StatsTab(MOBS_BUTTON_TEXT, new EntityStatsListWidget(client))
			)
			.build();
		setFocused(tabNavigationWidget);
		addDrawableChild(tabNavigationWidget);
		refreshTab(1);
		refreshTab(2);
		tabNavigationWidget.selectTab(0, false);
		refreshWidgetPositions();
		downloadingStats = false;
	}

	private void refreshTab(int tab) {
		if (tabNavigationWidget == null) {
			return;
		}

		boolean hasEntries = tabNavigationWidget.getTabs().get(tab) instanceof StatsTab statsTab
			&& !statsTab.widget.children().isEmpty();
		tabNavigationWidget.setTabActive(tab, hasEntries);
		tabNavigationWidget.setTabTooltip(
			tab,
			hasEntries ? null : Tooltip.of(Text.translatable("gui.stats.none_found"))
		);
	}

	@Override
	protected void refreshWidgetPositions() {
		if (tabNavigationWidget == null) {
			return;
		}

		tabNavigationWidget.setWidth(width);
		tabNavigationWidget.init();
		int headerBottom = tabNavigationWidget.getNavigationFocus().getBottom();
		ScreenRect contentArea = new ScreenRect(0, headerBottom, width, height - layout.getFooterHeight() - headerBottom);
		tabNavigationWidget.getTabs().forEach(tab -> tab.forEachChild(child -> child.setHeight(contentArea.height())));
		tabManager.setTabArea(contentArea);
		layout.setHeaderHeight(headerBottom);
		layout.refreshPositions();
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return tabNavigationWidget != null && tabNavigationWidget.keyPressed(input)
			|| super.keyPressed(input);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			Screen.FOOTER_SEPARATOR_TEXTURE,
			0,
			height - layout.getFooterHeight(),
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
			CreateWorldScreen.TAB_HEADER_BACKGROUND_TEXTURE,
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

	@Override
	public void close() {
		client.setScreen(parent);
	}

	static String getStatTranslationKey(Stat<Identifier> stat) {
		return "stat." + stat.getValue().toString().replace(':', '.');
	}

	@Environment(EnvType.CLIENT)
	class EntityStatsListWidget extends AlwaysSelectedEntryListWidget<StatsScreen.EntityStatsListWidget.Entry> {

		public EntityStatsListWidget(final MinecraftClient client) {
			super(client, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 9 * 4);

			for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
				if (StatsScreen.this.statHandler.getStat(Stats.KILLED.getOrCreateStat(entityType)) > 0
						|| StatsScreen.this.statHandler.getStat(Stats.KILLED_BY.getOrCreateStat(entityType)) > 0) {
					this.addEntry(new StatsScreen.EntityStatsListWidget.Entry(entityType));
				}
			}
		}

		@Override
		public int getRowWidth() {
			return SCREEN_WIDTH;
		}

		@Override
		protected void drawMenuListBackground(DrawContext context) {
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Environment(EnvType.CLIENT)
		class Entry extends AlwaysSelectedEntryListWidget.Entry<StatsScreen.EntityStatsListWidget.Entry> {

			private final Text entityTypeName;
			private final Text killedText;
			private final Text killedByText;
			private final boolean killedAny;
			private final boolean killedByAny;

			public Entry(final EntityType<?> entityType) {
				this.entityTypeName = entityType.getName();
				int i = StatsScreen.this.statHandler.getStat(Stats.KILLED.getOrCreateStat(entityType));
				if (i == 0) {
					this.killedText = Text.translatable("stat_type.minecraft.killed.none", this.entityTypeName);
					this.killedAny = false;
				}
				else {
					this.killedText = Text.translatable("stat_type.minecraft.killed", i, this.entityTypeName);
					this.killedAny = true;
				}

				int j = StatsScreen.this.statHandler.getStat(Stats.KILLED_BY.getOrCreateStat(entityType));
				if (j == 0) {
					this.killedByText = Text.translatable("stat_type.minecraft.killed_by.none", this.entityTypeName);
					this.killedByAny = false;
				}
				else {
					this.killedByText = Text.translatable("stat_type.minecraft.killed_by", this.entityTypeName, j);
					this.killedByAny = true;
				}
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				context.drawTextWithShadow(
						StatsScreen.this.textRenderer,
						this.entityTypeName,
						this.getContentX() + 2,
						this.getContentY() + 1,
						-1
				);
				context.drawTextWithShadow(
						StatsScreen.this.textRenderer,
						this.killedText,
						this.getContentX() + 2 + 10,
						this.getContentY() + 1 + 9,
						this.killedAny ? -4539718 : -8355712
				);
				context.drawTextWithShadow(
						StatsScreen.this.textRenderer,
						this.killedByText,
						this.getContentX() + 2 + 10,
						this.getContentY() + 1 + 9 * 2,
						this.killedByAny ? -4539718 : -8355712
				);
			}

			@Override
			public Text getNarration() {
				return Text.translatable(
						"narrator.select",
						ScreenTexts.joinSentences(this.killedText, this.killedByText)
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	class GeneralStatsListWidget extends AlwaysSelectedEntryListWidget<StatsScreen.GeneralStatsListWidget.Entry> {

		private static final int ROW_HEIGHT = 14;
		private static final int HEADER_HEIGHT = 33;
		private static final int COLOR_WHITE = -1;
		private static final int COLOR_GRAY = -4539718;

		public GeneralStatsListWidget(final MinecraftClient client) {
			super(client, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), HEADER_HEIGHT, ROW_HEIGHT);
			ObjectArrayList<Stat<Identifier>> stats = new ObjectArrayList<>(Stats.CUSTOM.iterator());
			stats.sort(Comparator.comparing(stat -> I18n.translate(StatsScreen.getStatTranslationKey(stat))));

			for (Stat<Identifier> stat : stats) {
				addEntry(new Entry(stat));
			}
		}

		@Override
		public int getRowWidth() {
			return SCREEN_WIDTH;
		}

		@Override
		protected void drawMenuListBackground(DrawContext context) {
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Environment(EnvType.CLIENT)
		class Entry extends AlwaysSelectedEntryListWidget.Entry<StatsScreen.GeneralStatsListWidget.Entry> {

			private final Stat<Identifier> stat;
			private final Text displayName;

			Entry(final Stat<Identifier> stat) {
				this.stat = stat;
				displayName = Text.translatable(StatsScreen.getStatTranslationKey(stat));
			}

			private String getFormatted() {
				return stat.format(StatsScreen.this.statHandler.getStat(stat));
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				int textY = getContentMiddleY() - 9 / 2;
				int rowIndex = GeneralStatsListWidget.this.children().indexOf(this);
				int color = rowIndex % 2 == 0 ? COLOR_WHITE : COLOR_GRAY;
				context.drawTextWithShadow(StatsScreen.this.textRenderer, displayName, getContentX() + 2, textY, color);
				String formatted = getFormatted();
				context.drawTextWithShadow(
					StatsScreen.this.textRenderer,
					formatted,
					getContentRightEnd() - StatsScreen.this.textRenderer.getWidth(formatted) - 4,
					textY,
					color
				);
			}

			@Override
			public Text getNarration() {
				return Text.translatable(
					"narrator.select",
					Text.empty().append(displayName).append(ScreenTexts.SPACE).append(getFormatted())
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	class ItemStatsListWidget extends ElementListWidget<StatsScreen.ItemStatsListWidget.Entry> {

		private static final int ROW_HEIGHT = 18;
		private static final int HEADER_HEIGHT = 22;
		private static final int SORT_ASCENDING = 1;
		private static final int SORT_NONE = 0;
		private static final int SORT_DESCENDING = -1;
		private static final int INITIAL_LIST_ORDER = 1;
		protected final List<StatType<Block>> blockStatTypes;
		protected final List<StatType<Item>> itemStatTypes;
		protected final Comparator<StatsScreen.ItemStatsListWidget.StatEntry>
				comparator =
				new StatsScreen.ItemStatsListWidget.ItemComparator();
		protected @Nullable StatType<?> selectedStatType;
		protected int listOrder;

		public ItemStatsListWidget(final MinecraftClient client) {
			super(client, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, HEADER_HEIGHT);
			blockStatTypes = Lists.newArrayList();
			blockStatTypes.add(Stats.MINED);
			itemStatTypes = Lists.newArrayList(Stats.BROKEN, Stats.CRAFTED, Stats.USED, Stats.PICKED_UP, Stats.DROPPED);

			Set<Item> itemsWithStats = Sets.newIdentityHashSet();

			for (Item item : Registries.ITEM) {
				boolean hasStats = itemStatTypes.stream()
					.anyMatch(statType -> statType.hasStat(item)
						&& StatsScreen.this.statHandler.getStat(statType.getOrCreateStat(item)) > 0);
				if (hasStats) {
					itemsWithStats.add(item);
				}
			}

			for (Block block : Registries.BLOCK) {
				boolean hasStats = blockStatTypes.stream()
					.anyMatch(statType -> statType.hasStat(block)
						&& StatsScreen.this.statHandler.getStat(statType.getOrCreateStat(block)) > 0);
				if (hasStats) {
					itemsWithStats.add(block.asItem());
				}
			}

			itemsWithStats.remove(Items.AIR);
			if (!itemsWithStats.isEmpty()) {
				addEntry(new Header());
				for (Item item : itemsWithStats) {
					addEntry(new StatEntry(item));
				}
			}
		}

		@Override
		protected void drawMenuListBackground(DrawContext context) {
		}

		int getIconX(int index) {
			return 75 + 40 * index;
		}

		@Override
		public int getRowWidth() {
			return SCREEN_WIDTH;
		}

		StatType<?> getStatType(int headerColumn) {
			return headerColumn < blockStatTypes.size()
				? blockStatTypes.get(headerColumn)
				: itemStatTypes.get(headerColumn - blockStatTypes.size());
		}

		int getHeaderIndex(StatType<?> statType) {
			int blockIndex = blockStatTypes.indexOf(statType);
			if (blockIndex >= 0) {
				return blockIndex;
			}

			int itemIndex = itemStatTypes.indexOf(statType);
			return itemIndex >= 0 ? itemIndex + blockStatTypes.size() : -1;
		}

		protected void selectStatType(StatType<?> statType) {
			if (statType != selectedStatType) {
				selectedStatType = statType;
				listOrder = SORT_DESCENDING;
			} else if (listOrder == SORT_DESCENDING) {
				listOrder = SORT_ASCENDING;
			} else {
				selectedStatType = null;
				listOrder = SORT_NONE;
			}

			sortStats(comparator);
		}

		protected void sortStats(Comparator<StatsScreen.ItemStatsListWidget.StatEntry> comparator) {
			List<StatEntry> entries = getStatEntries();
			entries.sort(comparator);
			clearEntriesExcept(children().getFirst());
			for (StatEntry entry : entries) {
				addEntry(entry);
			}
		}

		private List<StatEntry> getStatEntries() {
			List<StatEntry> entries = new ArrayList<>();
			children().forEach(child -> {
				if (child instanceof StatEntry statEntry) {
					entries.add(statEntry);
				}
			});
			return entries;
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Environment(EnvType.CLIENT)
		abstract static class Entry extends ElementListWidget.Entry<StatsScreen.ItemStatsListWidget.Entry> {
		}

		@Environment(EnvType.CLIENT)
		class Header extends StatsScreen.ItemStatsListWidget.Entry {

			private static final Identifier BLOCK_MINED_TEXTURE = Identifier.ofVanilla("statistics/block_mined");
			private static final Identifier ITEM_BROKEN_TEXTURE = Identifier.ofVanilla("statistics/item_broken");
			private static final Identifier ITEM_CRAFTED_TEXTURE = Identifier.ofVanilla("statistics/item_crafted");
			private static final Identifier ITEM_USED_TEXTURE = Identifier.ofVanilla("statistics/item_used");
			private static final Identifier ITEM_PICKED_UP_TEXTURE = Identifier.ofVanilla("statistics/item_picked_up");
			private static final Identifier ITEM_DROPPED_TEXTURE = Identifier.ofVanilla("statistics/item_dropped");
			private final StatsScreen.ItemStatsListWidget.Header.HeaderButton blockMinedButton;
			private final StatsScreen.ItemStatsListWidget.Header.HeaderButton itemBrokenButton;
			private final StatsScreen.ItemStatsListWidget.Header.HeaderButton itemCraftedButton;
			private final StatsScreen.ItemStatsListWidget.Header.HeaderButton itemUsedButton;
			private final StatsScreen.ItemStatsListWidget.Header.HeaderButton itemPickedUpButton;
			private final StatsScreen.ItemStatsListWidget.Header.HeaderButton itemDroppedButton;
			private final List<ClickableWidget> buttons = new ArrayList<>();

			Header() {
				this.blockMinedButton = new StatsScreen.ItemStatsListWidget.Header.HeaderButton(0, BLOCK_MINED_TEXTURE);
				this.itemBrokenButton = new StatsScreen.ItemStatsListWidget.Header.HeaderButton(1, ITEM_BROKEN_TEXTURE);
				this.itemCraftedButton =
						new StatsScreen.ItemStatsListWidget.Header.HeaderButton(2, ITEM_CRAFTED_TEXTURE);
				this.itemUsedButton = new StatsScreen.ItemStatsListWidget.Header.HeaderButton(3, ITEM_USED_TEXTURE);
				this.itemPickedUpButton =
						new StatsScreen.ItemStatsListWidget.Header.HeaderButton(4, ITEM_PICKED_UP_TEXTURE);
				this.itemDroppedButton =
						new StatsScreen.ItemStatsListWidget.Header.HeaderButton(5, ITEM_DROPPED_TEXTURE);
				this.buttons
						.addAll(
								List.of(
										this.blockMinedButton,
										this.itemBrokenButton,
										this.itemCraftedButton,
										this.itemUsedButton,
										this.itemPickedUpButton,
										this.itemDroppedButton
								)
						);
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				this.blockMinedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(0) - ROW_HEIGHT,
						this.getContentY() + 1
				);
				this.blockMinedButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemBrokenButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(1) - ROW_HEIGHT,
						this.getContentY() + 1
				);
				this.itemBrokenButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemCraftedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(2) - ROW_HEIGHT,
						this.getContentY() + 1
				);
				this.itemCraftedButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemUsedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(3) - ROW_HEIGHT,
						this.getContentY() + 1
				);
				this.itemUsedButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemPickedUpButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(4) - ROW_HEIGHT,
						this.getContentY() + 1
				);
				this.itemPickedUpButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemDroppedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(5) - ROW_HEIGHT,
						this.getContentY() + 1
				);
				this.itemDroppedButton.render(context, mouseX, mouseY, deltaTicks);
				if (ItemStatsListWidget.this.selectedStatType != null) {
					int
							i =
							ItemStatsListWidget.this.getIconX(ItemStatsListWidget.this.getHeaderIndex(
									ItemStatsListWidget.this.selectedStatType)) - 36;
					Identifier
							identifier =
							ItemStatsListWidget.this.listOrder == 1 ? StatsScreen.SORT_UP_TEXTURE
							                                        : StatsScreen.SORT_DOWN_TEXTURE;
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							identifier,
							this.getContentX() + i,
							this.getContentY() + 1,
							ROW_HEIGHT,
							ROW_HEIGHT
					);
				}
			}

			@Override
			public List<? extends Element> children() {
				return this.buttons;
			}

			@Override
			public List<? extends Selectable> selectableChildren() {
				return this.buttons;
			}

			@Environment(EnvType.CLIENT)
			class HeaderButton extends TexturedButtonWidget {

				private final Identifier texture;

				HeaderButton(final int index, final Identifier texture) {
					super(
							ROW_HEIGHT,
							ROW_HEIGHT,
							new ButtonTextures(StatsScreen.HEADER_TEXTURE, StatsScreen.SLOT_TEXTURE),
							button -> ItemStatsListWidget.this.selectStatType(ItemStatsListWidget.this.getStatType(index)),
							ItemStatsListWidget.this.getStatType(index).getName()
					);
					this.texture = texture;
					this.setTooltip(Tooltip.of(this.getMessage()));
				}

				@Override
				public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
					Identifier identifier = this.textures.get(this.isInteractable(), this.isSelected());
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							identifier,
							this.getX(),
							this.getY(),
							this.width,
							this.height
					);
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							this.texture,
							this.getX(),
							this.getY(),
							this.width,
							this.height
					);
				}
			}
		}

		@Environment(EnvType.CLIENT)
		class ItemComparator implements Comparator<StatsScreen.ItemStatsListWidget.StatEntry> {

			@Override
			public int compare(StatEntry first, StatEntry second) {
				Item item1 = first.getItem();
				Item item2 = second.getItem();
				int value1;
				int value2;

				if (selectedStatType == null) {
					value1 = 0;
					value2 = 0;
				} else if (blockStatTypes.contains(selectedStatType)) {
					StatType<Block> statType = (StatType<Block>) selectedStatType;
					value1 = item1 instanceof BlockItem blockItem1
						? StatsScreen.this.statHandler.getStat(statType, blockItem1.getBlock())
						: -1;
					value2 = item2 instanceof BlockItem blockItem2
						? StatsScreen.this.statHandler.getStat(statType, blockItem2.getBlock())
						: -1;
				} else {
					StatType<Item> statType = (StatType<Item>) selectedStatType;
					value1 = StatsScreen.this.statHandler.getStat(statType, item1);
					value2 = StatsScreen.this.statHandler.getStat(statType, item2);
				}

				return value1 == value2
					? listOrder * Integer.compare(Item.getRawId(item1), Item.getRawId(item2))
					: listOrder * Integer.compare(value1, value2);
			}
		}

		@Environment(EnvType.CLIENT)
		class StatEntry extends StatsScreen.ItemStatsListWidget.Entry {

			private final Item item;
			private final StatsScreen.ItemStatsListWidget.StatEntry.ItemStackInSlotWidget button;

			private static final int COLOR_WHITE = -1;
			private static final int COLOR_GRAY = -4539718;

			StatEntry(final Item item) {
				this.item = item;
				button = new ItemStackInSlotWidget(item.getDefaultStack());
			}

			protected Item getItem() {
				return item;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				button.setPosition(getContentX(), getContentY());
				button.render(context, mouseX, mouseY, deltaTicks);
				int rowIndex = ItemStatsListWidget.this.children().indexOf(this);
				boolean isEvenRow = rowIndex % 2 == 0;
				int textY = getContentMiddleY() - 9 / 2;

				for (int col = 0; col < ItemStatsListWidget.this.blockStatTypes.size(); col++) {
					Stat<Block> stat = item instanceof BlockItem blockItem
						? ItemStatsListWidget.this.blockStatTypes.get(col).getOrCreateStat(blockItem.getBlock())
						: null;
					render(context, stat, getContentX() + ItemStatsListWidget.this.getIconX(col), textY, isEvenRow);
				}

				for (int col = 0; col < ItemStatsListWidget.this.itemStatTypes.size(); col++) {
					render(
						context,
						ItemStatsListWidget.this.itemStatTypes.get(col).getOrCreateStat(item),
						getContentX() + ItemStatsListWidget.this.getIconX(col + ItemStatsListWidget.this.blockStatTypes.size()),
						textY,
						isEvenRow
					);
				}
			}

			protected void render(DrawContext context, @Nullable Stat<?> stat, int x, int y, boolean white) {
				Text text = stat == null
					? StatsScreen.NONE_TEXT
					: Text.literal(stat.format(StatsScreen.this.statHandler.getStat(stat)));
				context.drawTextWithShadow(
					StatsScreen.this.textRenderer,
					text,
					x - StatsScreen.this.textRenderer.getWidth(text),
					y,
					white ? COLOR_WHITE : COLOR_GRAY
				);
			}

			@Override
			public List<? extends Selectable> selectableChildren() {
				return List.of(button);
			}

			@Override
			public List<? extends Element> children() {
				return List.of(button);
			}

			@Environment(EnvType.CLIENT)
			class ItemStackInSlotWidget extends ItemStackWidget {

				ItemStackInSlotWidget(final ItemStack stack) {
					super(ItemStatsListWidget.this.client, 1, 1, ROW_HEIGHT, ROW_HEIGHT, stack.getName(), stack, false, true);
				}

				@Override
				protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							StatsScreen.SLOT_TEXTURE,
							StatEntry.this.getContentX(),
							StatEntry.this.getContentY(),
							ROW_HEIGHT,
							ROW_HEIGHT
					);
					super.renderWidget(context, mouseX, mouseY, deltaTicks);
				}

				@Override
				protected void renderTooltip(DrawContext context, int mouseX, int mouseY) {
					super.renderTooltip(context, StatEntry.this.getContentX() + ROW_HEIGHT, StatEntry.this.getContentY() + ROW_HEIGHT);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	class StatsTab extends GridScreenTab {

		protected final EntryListWidget<?> widget;

		public StatsTab(final Text title, final EntryListWidget<?> widget) {
			super(title);
			grid.add(widget, 1, 1);
			this.widget = widget;
		}

		@Override
		public void refreshGrid(ScreenRect tabArea) {
			widget.position(
				StatsScreen.this.width,
				StatsScreen.this.layout.getContentHeight(),
				StatsScreen.this.layout.getHeaderHeight()
			);
			super.refreshGrid(tabArea);
		}
	}
}
