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

@Environment(EnvType.CLIENT)
/**
 * {@code StatsScreen}.
 */
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
			child -> {
				ClickableWidget var10000 = this.addDrawableChild(child);
			}, child -> this.remove(child)
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
		Text text = DOWNLOADING_STATS_TEXT;
		this.tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width)
		                                              .tabs(
				                                              new LoadingTab(
						                                              this.getTextRenderer(),
						                                              GENERAL_BUTTON_TEXT,
						                                              text
				                                              ),
				                                              new LoadingTab(
						                                              this.getTextRenderer(),
						                                              ITEM_BUTTON_TEXT,
						                                              text
				                                              ),
				                                              new LoadingTab(
						                                              this.getTextRenderer(),
						                                              MOBS_BUTTON_TEXT,
						                                              text
				                                              )
		                                              )
		                                              .build();
		this.addDrawableChild(this.tabNavigationWidget);
		this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).width(200).build());
		this.tabNavigationWidget.setTabActive(0, true);
		this.tabNavigationWidget.setTabActive(1, false);
		this.tabNavigationWidget.setTabActive(2, false);
		this.layout.forEachChild(child -> {
			child.setNavigationOrder(1);
			this.addDrawableChild(child);
		});
		this.tabNavigationWidget.selectTab(0, false);
		this.refreshWidgetPositions();
		this.client.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
	}

	public void onStatsReady() {
		if (this.downloadingStats) {
			if (this.tabNavigationWidget != null) {
				this.remove(this.tabNavigationWidget);
			}

			this.tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width)
			                                              .tabs(
					                                              new StatsScreen.StatsTab(
							                                              GENERAL_BUTTON_TEXT,
							                                              new StatsScreen.GeneralStatsListWidget(this.client)
					                                              ),
					                                              new StatsScreen.StatsTab(
							                                              ITEM_BUTTON_TEXT,
							                                              new StatsScreen.ItemStatsListWidget(this.client)
					                                              ),
					                                              new StatsScreen.StatsTab(
							                                              MOBS_BUTTON_TEXT,
							                                              new StatsScreen.EntityStatsListWidget(this.client)
					                                              )
			                                              )
			                                              .build();
			this.setFocused(this.tabNavigationWidget);
			this.addDrawableChild(this.tabNavigationWidget);
			this.refreshTab(1);
			this.refreshTab(2);
			this.tabNavigationWidget.selectTab(0, false);
			this.refreshWidgetPositions();
			this.downloadingStats = false;
		}
	}

	private void refreshTab(int tab) {
		if (this.tabNavigationWidget != null) {
			boolean
					bl =
					this.tabNavigationWidget.getTabs().get(tab) instanceof StatsScreen.StatsTab statsTab
							&& !statsTab.widget.children().isEmpty();
			this.tabNavigationWidget.setTabActive(tab, bl);
			if (bl) {
				this.tabNavigationWidget.setTabTooltip(tab, null);
			}
			else {
				this.tabNavigationWidget.setTabTooltip(tab, Tooltip.of(Text.translatable("gui.stats.none_found")));
			}
		}
	}

	@Override
	protected void refreshWidgetPositions() {
		if (this.tabNavigationWidget != null) {
			this.tabNavigationWidget.setWidth(this.width);
			this.tabNavigationWidget.init();
			int i = this.tabNavigationWidget.getNavigationFocus().getBottom();
			ScreenRect screenRect = new ScreenRect(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
			this.tabNavigationWidget
					.getTabs()
					.forEach(tab -> tab.forEachChild(child -> child.setHeight(screenRect.height())));
			this.tabManager.setTabArea(screenRect);
			this.layout.setHeaderHeight(i);
			this.layout.refreshPositions();
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return this.tabNavigationWidget != null && this.tabNavigationWidget.keyPressed(input) ? true
		                                                                                      : super.keyPressed(input);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				Screen.FOOTER_SEPARATOR_TEXTURE,
				0,
				this.height - this.layout.getFooterHeight(),
				0.0F,
				0.0F,
				this.width,
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
				this.width,
				this.layout.getHeaderHeight(),
				16,
				16
		);
		this.renderDarkening(context, 0, this.layout.getHeaderHeight(), this.width, this.height);
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	static String getStatTranslationKey(Stat<Identifier> stat) {
		return "stat." + stat.getValue().toString().replace(':', '.');
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code EntityStatsListWidget}.
	 */
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
			return 280;
		}

		@Override
		protected void drawMenuListBackground(DrawContext context) {
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Entry}.
		 */
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
	/**
	 * {@code GeneralStatsListWidget}.
	 */
	class GeneralStatsListWidget extends AlwaysSelectedEntryListWidget<StatsScreen.GeneralStatsListWidget.Entry> {

		public GeneralStatsListWidget(final MinecraftClient client) {
			super(client, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 14);
			ObjectArrayList<Stat<Identifier>> objectArrayList = new ObjectArrayList(Stats.CUSTOM.iterator());
			objectArrayList.sort(Comparator.comparing(statx -> I18n.translate(StatsScreen.getStatTranslationKey(statx))));
			ObjectListIterator var4 = objectArrayList.iterator();

			while (var4.hasNext()) {
				Stat<Identifier> stat = (Stat<Identifier>) var4.next();
				this.addEntry(new StatsScreen.GeneralStatsListWidget.Entry(stat));
			}
		}

		@Override
		public int getRowWidth() {
			return 280;
		}

		@Override
		protected void drawMenuListBackground(DrawContext context) {
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Entry}.
		 */
		class Entry extends AlwaysSelectedEntryListWidget.Entry<StatsScreen.GeneralStatsListWidget.Entry> {

			private final Stat<Identifier> stat;
			private final Text displayName;

			Entry(final Stat<Identifier> stat) {
				this.stat = stat;
				this.displayName = Text.translatable(StatsScreen.getStatTranslationKey(stat));
			}

			private String getFormatted() {
				return this.stat.format(StatsScreen.this.statHandler.getStat(this.stat));
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				int i = this.getContentMiddleY() - 9 / 2;
				int j = GeneralStatsListWidget.this.children().indexOf(this);
				int k = j % 2 == 0 ? -1 : -4539718;
				context.drawTextWithShadow(
						StatsScreen.this.textRenderer,
						this.displayName,
						this.getContentX() + 2,
						i,
						k
				);
				String string = this.getFormatted();
				context.drawTextWithShadow(
						StatsScreen.this.textRenderer,
						string,
						this.getContentRightEnd() - StatsScreen.this.textRenderer.getWidth(string) - 4,
						i,
						k
				);
			}

			@Override
			public Text getNarration() {
				return Text.translatable(
						"narrator.select",
						Text.empty().append(this.displayName).append(ScreenTexts.SPACE).append(this.getFormatted())
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ItemStatsListWidget}.
	 */
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
			super(client, StatsScreen.this.width, StatsScreen.this.layout.getContentHeight(), 33, 22);
			this.blockStatTypes = Lists.newArrayList();
			this.blockStatTypes.add(Stats.MINED);
			this.itemStatTypes =
					Lists.newArrayList(new StatType[]{
							Stats.BROKEN,
							Stats.CRAFTED,
							Stats.USED,
							Stats.PICKED_UP,
							Stats.DROPPED
					});
			Set<Item> set = Sets.newIdentityHashSet();

			for (Item item : Registries.ITEM) {
				boolean bl = false;

				for (StatType<Item> statType : this.itemStatTypes) {
					if (statType.hasStat(item)
							&& StatsScreen.this.statHandler.getStat(statType.getOrCreateStat(item)) > 0) {
						bl = true;
					}
				}

				if (bl) {
					set.add(item);
				}
			}

			for (Block block : Registries.BLOCK) {
				boolean bl = false;

				for (StatType<Block> statTypex : this.blockStatTypes) {
					if (statTypex.hasStat(block)
							&& StatsScreen.this.statHandler.getStat(statTypex.getOrCreateStat(block)) > 0) {
						bl = true;
					}
				}

				if (bl) {
					set.add(block.asItem());
				}
			}

			set.remove(Items.AIR);
			if (!set.isEmpty()) {
				this.addEntry(new StatsScreen.ItemStatsListWidget.Header());

				for (Item item : set) {
					this.addEntry(new StatsScreen.ItemStatsListWidget.StatEntry(item));
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
			return 280;
		}

		StatType<?> getStatType(int headerColumn) {
			return headerColumn < this.blockStatTypes.size()
			       ? this.blockStatTypes.get(headerColumn)
			       : this.itemStatTypes.get(headerColumn - this.blockStatTypes.size());
		}

		int getHeaderIndex(StatType<?> statType) {
			int i = this.blockStatTypes.indexOf(statType);
			if (i >= 0) {
				return i;
			}
			else {
				int j = this.itemStatTypes.indexOf(statType);
				return j >= 0 ? j + this.blockStatTypes.size() : -1;
			}
		}

		protected void selectStatType(StatType<?> statType) {
			if (statType != this.selectedStatType) {
				this.selectedStatType = statType;
				this.listOrder = -1;
			}
			else if (this.listOrder == -1) {
				this.listOrder = 1;
			}
			else {
				this.selectedStatType = null;
				this.listOrder = 0;
			}

			this.sortStats(this.comparator);
		}

		protected void sortStats(Comparator<StatsScreen.ItemStatsListWidget.StatEntry> comparator) {
			List<StatsScreen.ItemStatsListWidget.StatEntry> list = this.getStatEntries();
			list.sort(comparator);
			this.clearEntriesExcept(this.children().getFirst());

			for (StatsScreen.ItemStatsListWidget.StatEntry statEntry : list) {
				this.addEntry(statEntry);
			}
		}

		private List<StatsScreen.ItemStatsListWidget.StatEntry> getStatEntries() {
			List<StatsScreen.ItemStatsListWidget.StatEntry> list = new ArrayList<>();
			this.children().forEach(child -> {
				if (child instanceof StatsScreen.ItemStatsListWidget.StatEntry statEntry) {
					list.add(statEntry);
				}
			});
			return list;
		}

		@Override
		protected void drawHeaderAndFooterSeparators(DrawContext context) {
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Entry}.
		 */
		abstract static class Entry extends ElementListWidget.Entry<StatsScreen.ItemStatsListWidget.Entry> {
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Header}.
		 */
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
						this.getContentX() + ItemStatsListWidget.this.getIconX(0) - 18,
						this.getContentY() + 1
				);
				this.blockMinedButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemBrokenButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(1) - 18,
						this.getContentY() + 1
				);
				this.itemBrokenButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemCraftedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(2) - 18,
						this.getContentY() + 1
				);
				this.itemCraftedButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemUsedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(3) - 18,
						this.getContentY() + 1
				);
				this.itemUsedButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemPickedUpButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(4) - 18,
						this.getContentY() + 1
				);
				this.itemPickedUpButton.render(context, mouseX, mouseY, deltaTicks);
				this.itemDroppedButton.setPosition(
						this.getContentX() + ItemStatsListWidget.this.getIconX(5) - 18,
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
							18,
							18
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
			/**
			 * {@code HeaderButton}.
			 */
			class HeaderButton extends TexturedButtonWidget {

				private final Identifier texture;

				HeaderButton(final int index, final Identifier texture) {
					super(
							18,
							18,
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
		/**
		 * {@code ItemComparator}.
		 */
		class ItemComparator implements Comparator<StatsScreen.ItemStatsListWidget.StatEntry> {

			public int compare(
					StatsScreen.ItemStatsListWidget.StatEntry statEntry,
					StatsScreen.ItemStatsListWidget.StatEntry statEntry2
			) {
				Item item = statEntry.getItem();
				Item item2 = statEntry2.getItem();
				int i;
				int j;
				if (ItemStatsListWidget.this.selectedStatType == null) {
					i = 0;
					j = 0;
				}
				else if (ItemStatsListWidget.this.blockStatTypes.contains(ItemStatsListWidget.this.selectedStatType)) {
					StatType<Block> statType = (StatType<Block>) ItemStatsListWidget.this.selectedStatType;
					i =
							item instanceof BlockItem ? StatsScreen.this.statHandler.getStat(
									statType,
									((BlockItem) item).getBlock()
							) : -1;
					j =
							item2 instanceof BlockItem ? StatsScreen.this.statHandler.getStat(
									statType,
									((BlockItem) item2).getBlock()
							) : -1;
				}
				else {
					StatType<Item> statType = (StatType<Item>) ItemStatsListWidget.this.selectedStatType;
					i = StatsScreen.this.statHandler.getStat(statType, item);
					j = StatsScreen.this.statHandler.getStat(statType, item2);
				}

				return i == j
				       ? ItemStatsListWidget.this.listOrder * Integer.compare(Item.getRawId(item), Item.getRawId(item2))
				       : ItemStatsListWidget.this.listOrder * Integer.compare(i, j);
			}
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code StatEntry}.
		 */
		class StatEntry extends StatsScreen.ItemStatsListWidget.Entry {

			private final Item item;
			private final StatsScreen.ItemStatsListWidget.StatEntry.ItemStackInSlotWidget button;

			StatEntry(final Item item) {
				this.item = item;
				this.button =
						new StatsScreen.ItemStatsListWidget.StatEntry.ItemStackInSlotWidget(item.getDefaultStack());
			}

			protected Item getItem() {
				return this.item;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				this.button.setPosition(this.getContentX(), this.getContentY());
				this.button.render(context, mouseX, mouseY, deltaTicks);
				StatsScreen.ItemStatsListWidget itemStatsListWidget = ItemStatsListWidget.this;
				int i = itemStatsListWidget.children().indexOf(this);

				for (int j = 0; j < itemStatsListWidget.blockStatTypes.size(); j++) {
					Stat<Block> stat;
					if (this.item instanceof BlockItem blockItem) {
						stat = itemStatsListWidget.blockStatTypes.get(j).getOrCreateStat(blockItem.getBlock());
					}
					else {
						stat = null;
					}

					this.render(
							context,
							stat,
							this.getContentX() + ItemStatsListWidget.this.getIconX(j),
							this.getContentMiddleY() - 9 / 2,
							i % 2 == 0
					);
				}

				for (int j = 0; j < itemStatsListWidget.itemStatTypes.size(); j++) {
					this.render(
							context,
							itemStatsListWidget.itemStatTypes.get(j).getOrCreateStat(this.item),
							this.getContentX() + ItemStatsListWidget.this.getIconX(
									j + itemStatsListWidget.blockStatTypes.size()),
							this.getContentMiddleY() - 9 / 2,
							i % 2 == 0
					);
				}
			}

			protected void render(DrawContext context, @Nullable Stat<?> stat, int x, int y, boolean white) {
				Text
						text =
						(Text) (stat == null ? StatsScreen.NONE_TEXT
						                     : Text.literal(stat.format(StatsScreen.this.statHandler.getStat(stat)))
						);
				context.drawTextWithShadow(
						StatsScreen.this.textRenderer,
						text,
						x - StatsScreen.this.textRenderer.getWidth(text),
						y,
						white ? -1 : -4539718
				);
			}

			@Override
			public List<? extends Selectable> selectableChildren() {
				return List.of(this.button);
			}

			@Override
			public List<? extends Element> children() {
				return List.of(this.button);
			}

			@Environment(EnvType.CLIENT)
			/**
			 * {@code ItemStackInSlotWidget}.
			 */
			class ItemStackInSlotWidget extends ItemStackWidget {

				ItemStackInSlotWidget(final ItemStack stack) {
					super(ItemStatsListWidget.this.client, 1, 1, 18, 18, stack.getName(), stack, false, true);
				}

				@Override
				protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
					context.drawGuiTexture(
							RenderPipelines.GUI_TEXTURED,
							StatsScreen.SLOT_TEXTURE,
							StatEntry.this.getContentX(),
							StatEntry.this.getContentY(),
							18,
							18
					);
					super.renderWidget(context, mouseX, mouseY, deltaTicks);
				}

				@Override
				protected void renderTooltip(DrawContext context, int mouseX, int mouseY) {
					super.renderTooltip(context, StatEntry.this.getContentX() + 18, StatEntry.this.getContentY() + 18);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code StatsTab}.
	 */
	class StatsTab extends GridScreenTab {

		protected final EntryListWidget<?> widget;

		public StatsTab(final Text title, final EntryListWidget<?> widget) {
			super(title);
			this.grid.add(widget, 1, 1);
			this.widget = widget;
		}

		@Override
		public void refreshGrid(ScreenRect tabArea) {
			this.widget.position(
					StatsScreen.this.width,
					StatsScreen.this.layout.getContentHeight(),
					StatsScreen.this.layout.getHeaderHeight()
			);
			super.refreshGrid(tabArea);
		}
	}
}
