package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Экран творческого инвентаря с поддержкой вкладок предметных групп,
 * поиска по тегам и прокрутки списка предметов.
 */
@Environment(EnvType.CLIENT)
public class CreativeInventoryScreen extends HandledScreen<CreativeInventoryScreen.CreativeScreenHandler> implements FabricCreativeInventoryScreen {

	private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller");
	private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller_disabled");
	private static final Identifier[] TAB_TOP_UNSELECTED_TEXTURES = new Identifier[]{
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_1"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_2"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_3"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_4"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_5"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_6"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_7")
	};
	private static final Identifier[] TAB_TOP_SELECTED_TEXTURES = new Identifier[]{
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_1"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_2"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_3"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_4"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_5"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_6"),
		Identifier.ofVanilla("container/creative_inventory/tab_top_selected_7")
	};
	private static final Identifier[] TAB_BOTTOM_UNSELECTED_TEXTURES = new Identifier[]{
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_1"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_2"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_3"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_4"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_5"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_6"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_7")
	};
	private static final Identifier[] TAB_BOTTOM_SELECTED_TEXTURES = new Identifier[]{
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_1"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_2"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_3"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_4"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_5"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_6"),
		Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_7")
	};

	private static final int ROWS_COUNT = 5;
	private static final int COLUMNS_COUNT = 9;
	private static final int TAB_WIDTH = 26;
	private static final int TAB_HEIGHT = 32;
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int SCROLLBAR_HEIGHT = 15;
	private static final int SCROLLBAR_AREA_X_OFFSET = 175;
	private static final int SCROLLBAR_AREA_Y_OFFSET = 18;
	private static final int SCROLLBAR_AREA_HEIGHT = 112;
	private static final int SCROLLBAR_AREA_WIDTH = 14;
	private static final int SCROLL_DRAG_HALF_HANDLE = 7;
	private static final int SCROLL_DRAG_FULL_HANDLE = 15;
	private static final int SLOT_SIZE = 18;
	private static final int INVENTORY_SLOT_GRID_X = 9;
	private static final int INVENTORY_SLOT_GRID_Y = 54;
	private static final int INVENTORY_HOTBAR_Y = 112;
	private static final int INVENTORY_ARMOR_COLUMN_X = 54;
	private static final int INVENTORY_ARMOR_COLUMN_STEP = 54;
	private static final int INVENTORY_ARMOR_ROW_STEP = 27;
	private static final int INVENTORY_ARMOR_ROW_Y = 6;
	private static final int INVENTORY_OFFHAND_SLOT = 45;
	private static final int INVENTORY_OFFHAND_X = 35;
	private static final int INVENTORY_OFFHAND_Y = 20;
	private static final int DELETE_SLOT_X = 173;
	private static final int DELETE_SLOT_Y = 112;
	private static final int ENTITY_LEFT = 73;
	private static final int ENTITY_TOP = 6;
	private static final int ENTITY_RIGHT = 105;
	private static final int ENTITY_BOTTOM = 49;
	private static final int ENTITY_SCALE = 20;
	private static final float ENTITY_Y_OFFSET = 0.0625F;
	private static final int TAB_ICON_X_OFFSET = 13;
	private static final int TAB_ICON_Y_OFFSET = 16;
	private static final int TAB_TOOLTIP_INNER_X = 3;
	private static final int TAB_TOOLTIP_INNER_Y = 3;
	private static final int TAB_TOOLTIP_WIDTH = 21;
	private static final int TAB_TOOLTIP_HEIGHT = 27;
	private static final int TAB_COLUMN_WIDTH = 27;
	private static final int SEARCH_BOX_X_OFFSET = 82;
	private static final int SEARCH_BOX_Y_OFFSET = 6;
	private static final int SEARCH_BOX_WIDTH = 80;
	private static final int SEARCH_BOX_HEIGHT = 9;
	private static final int SEARCH_BOX_MAX_LENGTH = 50;
	private static final int ITEMS_PER_PAGE = 45;
	private static final int TEXT_COLOR_TITLE = -12566464;

	static final SimpleInventory INVENTORY = new SimpleInventory(ITEMS_PER_PAGE);
	private static final Text DELETE_ITEM_SLOT_TEXT = Text.translatable("inventory.binSlot");

	public static ItemGroup selectedTab = ItemGroups.getDefaultTab();

	private float scrollPosition;
	private boolean scrolling;
	private TextFieldWidget searchBox;
	private @Nullable List<Slot> slots;
	private @Nullable Slot deleteItemSlot;
	private CreativeInventoryListener listener;
	private boolean ignoreTypedCharacter;
	private boolean lastClickOutsideBounds;
	private final Set<TagKey<Item>> searchResultTags = new HashSet<>();
	private final boolean operatorTabEnabled;
	private final StatusEffectsDisplay statusEffectsDisplay;

	public CreativeInventoryScreen(ClientPlayerEntity player, FeatureSet enabledFeatures, boolean operatorTabEnabled) {
		super(new CreativeInventoryScreen.CreativeScreenHandler(player), player.getInventory(), ScreenTexts.EMPTY);
		player.currentScreenHandler = handler;
		backgroundHeight = 136;
		backgroundWidth = 195;
		this.operatorTabEnabled = operatorTabEnabled;
		populateDisplay(
			player.networkHandler.getSearchManager(),
			enabledFeatures,
			shouldShowOperatorTab(player),
			player.getEntityWorld().getRegistryManager()
		);
		statusEffectsDisplay = new StatusEffectsDisplay(this);
	}

	private boolean shouldShowOperatorTab(PlayerEntity player) {
		return player.isCreativeLevelTwoOp() && operatorTabEnabled;
	}

	private void updateDisplayParameters(
		FeatureSet enabledFeatures,
		boolean showOperatorTab,
		RegistryWrapper.WrapperLookup registries
	) {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (!populateDisplay(
			networkHandler != null ? networkHandler.getSearchManager() : null,
			enabledFeatures,
			showOperatorTab,
			registries
		)) {
			return;
		}

		for (ItemGroup itemGroup : ItemGroups.getGroups()) {
			Collection<ItemStack> displayStacks = itemGroup.getDisplayStacks();
			if (itemGroup != selectedTab) {
				continue;
			}

			if (itemGroup.getType() == ItemGroup.Type.CATEGORY && displayStacks.isEmpty()) {
				setSelectedTab(ItemGroups.getDefaultTab());
			}
			else {
				refreshSelectedTab(displayStacks);
			}
		}
	}

	private boolean populateDisplay(
		@Nullable SearchManager searchManager,
		FeatureSet enabledFeatures,
		boolean showOperatorTab,
		RegistryWrapper.WrapperLookup registries
	) {
		if (!ItemGroups.updateDisplayContext(enabledFeatures, showOperatorTab, registries)) {
			return false;
		}

		if (searchManager != null) {
			List<ItemStack> searchItems = List.copyOf(ItemGroups.getSearchGroup().getDisplayStacks());
			searchManager.addItemTooltipReloader(registries, searchItems);
			searchManager.addItemTagReloader(searchItems);
		}

		return true;
	}

	private void refreshSelectedTab(Collection<ItemStack> displayStacks) {
		int currentRow = handler.getRow(scrollPosition);
		handler.itemList.clear();

		if (selectedTab.getType() == ItemGroup.Type.SEARCH) {
			search();
		}
		else {
			handler.itemList.addAll(displayStacks);
		}

		scrollPosition = handler.getScrollPosition(currentRow);
		handler.scrollItems(scrollPosition);
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}

		updateDisplayParameters(
			player.networkHandler.getEnabledFeatures(),
			shouldShowOperatorTab(player),
			player.getEntityWorld().getRegistryManager()
		);

		if (!player.isInCreativeMode()) {
			client.setScreen(new InventoryScreen(player));
		}
	}

	@Override
	protected void onMouseClick(@Nullable Slot slot, int slotId, int button, SlotActionType actionType) {
		if (isCreativeInventorySlot(slot)) {
			searchBox.setCursorToEnd(false);
			searchBox.setSelectionEnd(0);
		}

		boolean isQuickMove = actionType == SlotActionType.QUICK_MOVE;
		actionType = slotId == -999 && actionType == SlotActionType.PICKUP ? SlotActionType.THROW : actionType;

		if (actionType == SlotActionType.THROW && !client.player.canDropItems()) {
			return;
		}

		onMouseClick(slot, actionType);

		if (slot == null && selectedTab.getType() != ItemGroup.Type.INVENTORY
			&& actionType != SlotActionType.QUICK_CRAFT) {
			if (!handler.getCursorStack().isEmpty() && lastClickOutsideBounds) {
				if (!client.player.canDropItems()) {
					return;
				}

				if (button == 0) {
					client.player.dropItem(handler.getCursorStack(), true);
					client.interactionManager.dropCreativeStack(handler.getCursorStack());
					handler.setCursorStack(ItemStack.EMPTY);
				}

				if (button == 1) {
					ItemStack splitStack = handler.getCursorStack().split(1);
					client.player.dropItem(splitStack, true);
					client.interactionManager.dropCreativeStack(splitStack);
				}
			}

			return;
		}

		if (slot != null && !slot.canTakeItems(client.player)) {
			return;
		}

		if (slot == deleteItemSlot && isQuickMove) {
			for (int slotIndex = 0; slotIndex < client.player.playerScreenHandler.getStacks().size(); slotIndex++) {
				client.player.playerScreenHandler.getSlot(slotIndex).setStackNoCallbacks(ItemStack.EMPTY);
				client.interactionManager.clickCreativeStack(ItemStack.EMPTY, slotIndex);
			}
		}
		else if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
			if (slot == deleteItemSlot) {
				handler.setCursorStack(ItemStack.EMPTY);
			}
			else if (actionType == SlotActionType.THROW && slot != null && slot.hasStack()) {
				ItemStack droppedStack = slot.takeStack(button == 0 ? 1 : slot.getStack().getMaxCount());
				ItemStack remainingStack = slot.getStack();
				client.player.dropItem(droppedStack, true);
				client.interactionManager.dropCreativeStack(droppedStack);
				client.interactionManager.clickCreativeStack(
					remainingStack,
					((CreativeInventoryScreen.CreativeSlot) slot).slot.id
				);
			}
			else if (actionType == SlotActionType.THROW && slotId == -999 && !handler.getCursorStack().isEmpty()) {
				client.player.dropItem(handler.getCursorStack(), true);
				client.interactionManager.dropCreativeStack(handler.getCursorStack());
				handler.setCursorStack(ItemStack.EMPTY);
			}
			else {
				client.player.playerScreenHandler.onSlotClick(
					slot == null ? slotId : ((CreativeInventoryScreen.CreativeSlot) slot).slot.id,
					button,
					actionType,
					client.player
				);
				client.player.playerScreenHandler.sendContentUpdates();
			}
		}
		else if (actionType != SlotActionType.QUICK_CRAFT && slot.inventory == INVENTORY) {
			ItemStack cursorStack = handler.getCursorStack();
			ItemStack slotStack = slot.getStack();

			if (actionType == SlotActionType.SWAP) {
				if (!slotStack.isEmpty()) {
					client.player.getInventory().setStack(button, slotStack.copyWithCount(slotStack.getMaxCount()));
					client.player.playerScreenHandler.sendContentUpdates();
				}

				return;
			}

			if (actionType == SlotActionType.CLONE) {
				if (handler.getCursorStack().isEmpty() && slot.hasStack()) {
					ItemStack cloneSource = slot.getStack();
					handler.setCursorStack(cloneSource.copyWithCount(cloneSource.getMaxCount()));
				}

				return;
			}

			if (actionType == SlotActionType.THROW) {
				if (!slotStack.isEmpty()) {
					ItemStack throwStack = slotStack.copyWithCount(button == 0 ? 1 : slotStack.getMaxCount());
					client.player.dropItem(throwStack, true);
					client.interactionManager.dropCreativeStack(throwStack);
				}

				return;
			}

			if (!cursorStack.isEmpty() && !slotStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(cursorStack, slotStack)) {
				if (button == 0) {
					if (isQuickMove) {
						cursorStack.setCount(cursorStack.getMaxCount());
					}
					else if (cursorStack.getCount() < cursorStack.getMaxCount()) {
						cursorStack.increment(1);
					}
				}
				else {
					cursorStack.decrement(1);
				}
			}
			else if (!slotStack.isEmpty() && cursorStack.isEmpty()) {
				int pickCount = isQuickMove ? slotStack.getMaxCount() : slotStack.getCount();
				handler.setCursorStack(slotStack.copyWithCount(pickCount));
			}
			else if (button == 0) {
				handler.setCursorStack(ItemStack.EMPTY);
			}
			else if (!handler.getCursorStack().isEmpty()) {
				handler.getCursorStack().decrement(1);
			}
		}
		else if (handler != null) {
			ItemStack preClickStack = slot == null ? ItemStack.EMPTY : handler.getSlot(slot.id).getStack();
			handler.onSlotClick(slot == null ? slotId : slot.id, button, actionType, client.player);

			if (ScreenHandler.unpackQuickCraftStage(button) == 2) {
				for (int hotbarIndex = 0; hotbarIndex < COLUMNS_COUNT; hotbarIndex++) {
					client.interactionManager.clickCreativeStack(
						handler.getSlot(ITEMS_PER_PAGE + hotbarIndex).getStack(),
						36 + hotbarIndex
					);
				}
			}
			else if (slot != null && PlayerInventory.isValidHotbarIndex(slot.getIndex())
				&& selectedTab.getType() != ItemGroup.Type.INVENTORY) {
				if (actionType == SlotActionType.THROW && !preClickStack.isEmpty() && !handler.getCursorStack().isEmpty()) {
					int throwCount = button == 0 ? 1 : preClickStack.getCount();
					ItemStack throwStack = preClickStack.copyWithCount(throwCount);
					preClickStack.decrement(throwCount);
					client.player.dropItem(throwStack, true);
					client.interactionManager.dropCreativeStack(throwStack);
				}

				client.player.playerScreenHandler.sendContentUpdates();
			}
		}
	}

	private boolean isCreativeInventorySlot(@Nullable Slot slot) {
		return slot != null && slot.inventory == INVENTORY;
	}

	@Override
	protected void init() {
		if (!client.player.isInCreativeMode()) {
			client.setScreen(new InventoryScreen(client.player));
			return;
		}

		super.init();
		searchBox = new TextFieldWidget(
			textRenderer,
			x + SEARCH_BOX_X_OFFSET,
			y + SEARCH_BOX_Y_OFFSET,
			SEARCH_BOX_WIDTH,
			SEARCH_BOX_HEIGHT,
			Text.translatable("itemGroup.search")
		);
		searchBox.setMaxLength(SEARCH_BOX_MAX_LENGTH);
		searchBox.setDrawsBackground(false);
		searchBox.setVisible(false);
		searchBox.setEditableColor(-1);
		searchBox.setInvertSelectionBackground(false);
		addSelectableChild(searchBox);

		ItemGroup previousTab = selectedTab;
		selectedTab = ItemGroups.getDefaultTab();
		setSelectedTab(previousTab);
		client.player.playerScreenHandler.removeListener(listener);
		listener = new CreativeInventoryListener(client);
		client.player.playerScreenHandler.addListener(listener);

		if (!selectedTab.shouldDisplay()) {
			setSelectedTab(ItemGroups.getDefaultTab());
		}
	}

	@Override
	public void resize(int width, int height) {
		int savedRow = handler.getRow(scrollPosition);
		String savedSearch = searchBox.getText();
		init(width, height);
		searchBox.setText(savedSearch);

		if (!searchBox.getText().isEmpty()) {
			search();
		}

		scrollPosition = handler.getScrollPosition(savedRow);
		handler.scrollItems(scrollPosition);
	}

	@Override
	public void removed() {
		super.removed();
		if (client.player != null && client.player.getInventory() != null) {
			client.player.playerScreenHandler.removeListener(listener);
		}
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (ignoreTypedCharacter || selectedTab.getType() != ItemGroup.Type.SEARCH) {
			return false;
		}

		String previousText = searchBox.getText();
		if (!searchBox.charTyped(input)) {
			return false;
		}

		if (!Objects.equals(previousText, searchBox.getText())) {
			search();
		}

		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		ignoreTypedCharacter = false;

		if (selectedTab.getType() != ItemGroup.Type.SEARCH) {
			if (client.options.chatKey.matchesKey(input)) {
				ignoreTypedCharacter = true;
				setSelectedTab(ItemGroups.getSearchGroup());
				return true;
			}

			return super.keyPressed(input);
		}

		boolean canUseHotbar = !isCreativeInventorySlot(focusedSlot) || focusedSlot.hasStack();
		boolean isHotbarKey = InputUtil.fromKeyCode(input).toInt().isPresent();

		if (canUseHotbar && isHotbarKey && handleHotbarKeyPressed(input)) {
			ignoreTypedCharacter = true;
			return true;
		}

		String previousText = searchBox.getText();
		if (searchBox.keyPressed(input)) {
			if (!Objects.equals(previousText, searchBox.getText())) {
				search();
			}

			return true;
		}

		return searchBox.isFocused() && searchBox.isVisible() && !input.isEscape()
			? true
			: super.keyPressed(input);
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		ignoreTypedCharacter = false;
		return super.keyReleased(input);
	}

	private void search() {
		handler.itemList.clear();
		searchResultTags.clear();
		String query = searchBox.getText();

		if (query.isEmpty()) {
			handler.itemList.addAll(selectedTab.getDisplayStacks());
			scrollPosition = 0.0F;
			handler.scrollItems(0.0F);
			return;
		}

		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
		if (networkHandler == null) {
			return;
		}

		SearchManager searchManager = networkHandler.getSearchManager();
		SearchProvider<ItemStack> searchProvider;

		if (query.startsWith("#")) {
			query = query.substring(1);
			searchProvider = searchManager.getItemTagReloadFuture();
			searchForTags(query);
		}
		else {
			searchProvider = searchManager.getItemTooltipReloadFuture();
		}

		handler.itemList.addAll(searchProvider.findAll(query.toLowerCase(Locale.ROOT)));
		scrollPosition = 0.0F;
		handler.scrollItems(0.0F);
	}

	private void searchForTags(String id) {
		int colonIndex = id.indexOf(':');
		Predicate<Identifier> predicate;

		if (colonIndex == -1) {
			predicate = tagId -> tagId.getPath().contains(id);
		}
		else {
			String namespace = id.substring(0, colonIndex).trim();
			String path = id.substring(colonIndex + 1).trim();
			predicate = tagId -> tagId.getNamespace().contains(namespace) && tagId.getPath().contains(path);
		}

		Registries.ITEM
			.streamTags()
			.map(RegistryEntryList.Named::getTag)
			.filter(tag -> predicate.test(tag.id()))
			.forEach(searchResultTags::add);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		if (selectedTab.shouldRenderName()) {
			context.drawText(textRenderer, selectedTab.getDisplayName(), 8, 6, TEXT_COLOR_TITLE, false);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			double relX = click.x() - x;
			double relY = click.y() - y;

			for (ItemGroup itemGroup : ItemGroups.getGroupsToDisplay()) {
				if (isClickInTab(itemGroup, relX, relY)) {
					return true;
				}
			}

			if (selectedTab.getType() != ItemGroup.Type.INVENTORY && isClickInScrollbar(click.x(), click.y())) {
				scrolling = hasScrollbar();
				return true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (click.button() == 0) {
			double relX = click.x() - x;
			double relY = click.y() - y;
			scrolling = false;

			for (ItemGroup itemGroup : ItemGroups.getGroupsToDisplay()) {
				if (isClickInTab(itemGroup, relX, relY)) {
					setSelectedTab(itemGroup);
					return true;
				}
			}
		}

		return super.mouseReleased(click);
	}

	private boolean hasScrollbar() {
		return selectedTab.hasScrollbar() && handler.shouldShowScrollbar();
	}

	private void setSelectedTab(ItemGroup group) {
		ItemGroup previousTab = selectedTab;
		selectedTab = group;
		cursorDragSlots.clear();
		handler.itemList.clear();
		endTouchDrag();

		if (selectedTab.getType() == ItemGroup.Type.HOTBAR) {
			HotbarStorage hotbarStorage = client.getCreativeHotbarStorage();

			for (int hotbarSlot = 0; hotbarSlot < COLUMNS_COUNT; hotbarSlot++) {
				HotbarStorageEntry entry = hotbarStorage.getSavedHotbar(hotbarSlot);
				if (entry.isEmpty()) {
					for (int col = 0; col < COLUMNS_COUNT; col++) {
						if (col == hotbarSlot) {
							ItemStack placeholder = new ItemStack(Items.PAPER);
							placeholder.set(DataComponentTypes.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
							Text hotbarKey = client.options.hotbarKeys[hotbarSlot].getBoundKeyLocalizedText();
							Text saveKey = client.options.saveToolbarActivatorKey.getBoundKeyLocalizedText();
							placeholder.set(
								DataComponentTypes.ITEM_NAME,
								Text.translatable("inventory.hotbarInfo", saveKey, hotbarKey)
							);
							handler.itemList.add(placeholder);
						}
						else {
							handler.itemList.add(ItemStack.EMPTY);
						}
					}
				}
				else {
					handler.itemList.addAll(entry.deserialize(client.world.getRegistryManager()));
				}
			}
		}
		else if (selectedTab.getType() == ItemGroup.Type.CATEGORY) {
			handler.itemList.addAll(selectedTab.getDisplayStacks());
		}

		if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
			ScreenHandler playerHandler = client.player.playerScreenHandler;
			if (slots == null) {
				slots = ImmutableList.copyOf(handler.slots);
			}

			handler.slots.clear();

			for (int slotIndex = 0; slotIndex < playerHandler.slots.size(); slotIndex++) {
				int slotX;
				int slotY;

				if (slotIndex >= 5 && slotIndex < 9) {
					int armorIndex = slotIndex - 5;
					int armorCol = armorIndex / 2;
					int armorRow = armorIndex % 2;
					slotX = INVENTORY_ARMOR_COLUMN_X + armorCol * INVENTORY_ARMOR_COLUMN_STEP;
					slotY = INVENTORY_ARMOR_ROW_Y + armorRow * INVENTORY_ARMOR_ROW_STEP;
				}
				else if (slotIndex >= 0 && slotIndex < 5) {
					slotX = -2000;
					slotY = -2000;
				}
				else if (slotIndex == INVENTORY_OFFHAND_SLOT) {
					slotX = INVENTORY_OFFHAND_X;
					slotY = INVENTORY_OFFHAND_Y;
				}
				else {
					int gridIndex = slotIndex - 9;
					int gridCol = gridIndex % COLUMNS_COUNT;
					int gridRow = gridIndex / COLUMNS_COUNT;
					slotX = INVENTORY_SLOT_GRID_X + gridCol * SLOT_SIZE;
					slotY = slotIndex >= 36
						? INVENTORY_HOTBAR_Y
						: INVENTORY_SLOT_GRID_Y + gridRow * SLOT_SIZE;
				}
	
				Slot creativeSlot = new CreativeInventoryScreen.CreativeSlot(playerHandler.slots.get(slotIndex), slotIndex, slotX, slotY);
				handler.slots.add(creativeSlot);
			}
	
			deleteItemSlot = new Slot(INVENTORY, 0, DELETE_SLOT_X, DELETE_SLOT_Y);
			handler.slots.add(deleteItemSlot);
		}
		else if (previousTab.getType() == ItemGroup.Type.INVENTORY) {
			handler.slots.clear();
			handler.slots.addAll(slots);
			slots = null;
		}
	
		if (selectedTab.getType() == ItemGroup.Type.SEARCH) {
			searchBox.setVisible(true);
			searchBox.setFocusUnlocked(false);
			searchBox.setFocused(true);
			if (previousTab != group) {
				searchBox.setText("");
			}
	
			search();
		}
		else {
			searchBox.setVisible(false);
			searchBox.setFocusUnlocked(true);
			searchBox.setFocused(false);
			searchBox.setText("");
		}
	
		scrollPosition = 0.0F;
		handler.scrollItems(0.0F);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
			return true;
		}
	
		if (!hasScrollbar()) {
			return false;
		}
	
		scrollPosition = handler.getScrollPosition(scrollPosition, verticalAmount);
		handler.scrollItems(scrollPosition);
		return true;
	}
	
	@Override
	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top) {
		boolean outsideBg = mouseX < left || mouseY < top
			|| mouseX >= left + backgroundWidth
			|| mouseY >= top + backgroundHeight;
		lastClickOutsideBounds = outsideBg && !isClickInTab(selectedTab, mouseX, mouseY);
		return lastClickOutsideBounds;
	}
	
	protected boolean isClickInScrollbar(double mouseX, double mouseY) {
		int scrollX = x + SCROLLBAR_AREA_X_OFFSET;
		int scrollY = y + SCROLLBAR_AREA_Y_OFFSET;
		int scrollRight = scrollX + SCROLLBAR_AREA_WIDTH;
		int scrollBottom = scrollY + SCROLLBAR_AREA_HEIGHT;
		return mouseX >= scrollX && mouseY >= scrollY && mouseX < scrollRight && mouseY < scrollBottom;
	}
	
	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (!scrolling) {
			return super.mouseDragged(click, offsetX, offsetY);
		}
	
		int scrollTop = y + SCROLLBAR_AREA_Y_OFFSET;
		int scrollBottom = scrollTop + SCROLLBAR_AREA_HEIGHT;
		scrollPosition = ((float) click.y() - scrollTop - SCROLL_DRAG_HALF_HANDLE)
			/ (scrollBottom - scrollTop - SCROLL_DRAG_FULL_HANDLE);
		scrollPosition = MathHelper.clamp(scrollPosition, 0.0F, 1.0F);
		handler.scrollItems(scrollPosition);
		return true;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		statusEffectsDisplay.render(context, mouseX, mouseY);
		super.render(context, mouseX, mouseY, deltaTicks);
	
		for (ItemGroup itemGroup : ItemGroups.getGroupsToDisplay()) {
			if (renderTabTooltipIfHovered(context, itemGroup, mouseX, mouseY)) {
				break;
			}
		}
	
		if (deleteItemSlot != null
			&& selectedTab.getType() == ItemGroup.Type.INVENTORY
			&& isPointWithinBounds(deleteItemSlot.x, deleteItemSlot.y, 16, 16, mouseX, mouseY)) {
			context.drawTooltip(textRenderer, DELETE_ITEM_SLOT_TEXT, mouseX, mouseY);
		}
	
		drawMouseoverTooltip(context, mouseX, mouseY);
	}
	
	@Override
	public boolean showsStatusEffects() {
		return statusEffectsDisplay.shouldHideStatusEffectHud();
	}
	
	@Override
	public List<Text> getTooltipFromItem(ItemStack stack) {
		boolean isLockableSlot = focusedSlot instanceof CreativeInventoryScreen.LockableSlot;
		boolean isCategoryTab = selectedTab.getType() == ItemGroup.Type.CATEGORY;
		boolean isSearchTab = selectedTab.getType() == ItemGroup.Type.SEARCH;
		TooltipType.Default defaultType = client.options.advancedItemTooltips
			? TooltipType.Default.ADVANCED
			: TooltipType.Default.BASIC;
		TooltipType tooltipType = isLockableSlot ? defaultType.withCreative() : defaultType;
		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.create(client.world), client.player, tooltipType);
	
		if (tooltip.isEmpty()) {
			return tooltip;
		}
	
		if (isCategoryTab && isLockableSlot) {
			return tooltip;
		}
	
		List<Text> enriched = Lists.newArrayList(tooltip);
	
		if (isSearchTab && isLockableSlot) {
			searchResultTags.forEach(tagKey -> {
				if (stack.isIn((TagKey<Item>) tagKey)) {
					enriched.add(1, Text.literal("#" + tagKey.id()).formatted(Formatting.DARK_PURPLE));
				}
			});
		}
	
		int insertIndex = 1;
		for (ItemGroup itemGroup : ItemGroups.getGroupsToDisplay()) {
			if (itemGroup.getType() != ItemGroup.Type.SEARCH && itemGroup.contains(stack)) {
				enriched.add(insertIndex++, itemGroup.getDisplayName().copy().formatted(Formatting.BLUE));
			}
		}
	
		return enriched;
	}
	
	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		for (ItemGroup itemGroup : ItemGroups.getGroupsToDisplay()) {
			if (itemGroup != selectedTab) {
				renderTabIcon(context, mouseX, mouseY, itemGroup);
			}
		}
	
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			selectedTab.getTexture(),
			x,
			y,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
	
		if (isClickInScrollbar(mouseX, mouseY) && hasScrollbar()) {
			context.setCursor(scrolling ? StandardCursors.RESIZE_NS : StandardCursors.POINTING_HAND);
		}
	
		searchBox.render(context, mouseX, mouseY, deltaTicks);
	
		int scrollbarX = x + SCROLLBAR_AREA_X_OFFSET;
		int scrollbarTop = y + SCROLLBAR_AREA_Y_OFFSET;
		int scrollbarBottom = scrollbarTop + SCROLLBAR_AREA_HEIGHT;
	
		if (selectedTab.hasScrollbar()) {
			Identifier scrollerTexture = hasScrollbar() ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				scrollerTexture,
				scrollbarX,
				scrollbarTop + (int) ((scrollbarBottom - scrollbarTop - SCROLLBAR_HEIGHT) * scrollPosition),
				SCROLLBAR_WIDTH,
				SCROLLBAR_HEIGHT
			);
		}
	
		renderTabIcon(context, mouseX, mouseY, selectedTab);
	
		if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
			InventoryScreen.drawEntity(
				context,
				x + ENTITY_LEFT,
				y + ENTITY_TOP,
				x + ENTITY_RIGHT,
				y + ENTITY_BOTTOM,
				ENTITY_SCALE,
				ENTITY_Y_OFFSET,
				mouseX,
				mouseY,
				client.player
			);
		}
	}
	
	private int getTabX(ItemGroup group) {
		int column = group.getColumn();
		int tabX = TAB_COLUMN_WIDTH * column;
		if (group.isSpecial()) {
			tabX = backgroundWidth - TAB_COLUMN_WIDTH * (7 - column) + 1;
		}
	
		return tabX;
	}
	
	private int getTabY(ItemGroup group) {
		return group.getRow() == ItemGroup.Row.TOP
			? -TAB_HEIGHT
			: backgroundHeight;
	}
	
	protected boolean isClickInTab(ItemGroup group, double mouseX, double mouseY) {
		int tabX = getTabX(group);
		int tabY = getTabY(group);
		return mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT;
	}
	
	/**
		* Отрисовывает подсказку вкладки при наведении курсора.
		*
		* @param context контекст отрисовки
		* @param group вкладка предметной группы
		* @param mouseX позиция курсора по X
		* @param mouseY позиция курсора по Y
		* @return {@code true}, если подсказка была отрисована
		*/
	protected boolean renderTabTooltipIfHovered(DrawContext context, ItemGroup group, int mouseX, int mouseY) {
		int tabX = getTabX(group);
		int tabY = getTabY(group);
		if (!isPointWithinBounds(tabX + TAB_TOOLTIP_INNER_X, tabY + TAB_TOOLTIP_INNER_Y, TAB_TOOLTIP_WIDTH, TAB_TOOLTIP_HEIGHT, mouseX, mouseY)) {
			return false;
		}
	
		context.drawTooltip(textRenderer, group.getDisplayName(), mouseX, mouseY);
		return true;
	}
	
	/**
		* Отрисовывает иконку вкладки предметной группы.
		*
		* @param context контекст отрисовки
		* @param mouseX позиция курсора по X
		* @param mouseY позиция курсора по Y
		* @param tab вкладка предметной группы
		*/
	protected void renderTabIcon(DrawContext context, int mouseX, int mouseY, ItemGroup tab) {
		boolean isSelected = tab == selectedTab;
		boolean isTopRow = tab.getRow() == ItemGroup.Row.TOP;
		int column = tab.getColumn();
		int tabX = x + getTabX(tab);
		int tabY = y - (isTopRow ? 28 : -(backgroundHeight - 4));
		Identifier[] textures;
	
		if (isTopRow) {
			textures = isSelected ? TAB_TOP_SELECTED_TEXTURES : TAB_TOP_UNSELECTED_TEXTURES;
		}
		else {
			textures = isSelected ? TAB_BOTTOM_SELECTED_TEXTURES : TAB_BOTTOM_UNSELECTED_TEXTURES;
		}
	
		if (!isSelected && mouseX > tabX && mouseY > tabY && mouseX < tabX + TAB_WIDTH && mouseY < tabY + TAB_HEIGHT) {
			context.setCursor(StandardCursors.POINTING_HAND);
		}
	
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			textures[MathHelper.clamp(column, 0, textures.length)],
			tabX,
			tabY,
			TAB_WIDTH,
			TAB_HEIGHT
		);
	
		int iconX = tabX + TAB_ICON_X_OFFSET - 8;
		int iconY = tabY + TAB_ICON_Y_OFFSET - 8 + (isTopRow ? 1 : -1);
		context.drawItem(tab.getIcon(), iconX, iconY);
	}
	
	public boolean isInventoryTabSelected() {
		return selectedTab.getType() == ItemGroup.Type.INVENTORY;
	}
	
	/**
		* Обрабатывает нажатие клавиши хотбара в творческом режиме:
		* восстанавливает или сохраняет набор предметов хотбара.
		*
		* @param client клиент Minecraft
		* @param index индекс слота хотбара (0–8)
		* @param restore {@code true} — загрузить сохранённый хотбар
		* @param save {@code true} — сохранить текущий хотбар
		*/
	public static void onHotbarKeyPress(MinecraftClient client, int index, boolean restore, boolean save) {
		ClientPlayerEntity player = client.player;
		DynamicRegistryManager registries = player.getEntityWorld().getRegistryManager();
		HotbarStorage hotbarStorage = client.getCreativeHotbarStorage();
		HotbarStorageEntry entry = hotbarStorage.getSavedHotbar(index);
	
		if (restore) {
			List<ItemStack> hotbarItems = entry.deserialize(registries);
			for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
				ItemStack itemStack = hotbarItems.get(slot);
				player.getInventory().setStack(slot, itemStack);
				client.interactionManager.clickCreativeStack(itemStack, 36 + slot);
			}
	
			player.playerScreenHandler.sendContentUpdates();
		}
		else if (save) {
			entry.serialize(player.getInventory(), registries);
			Text hotbarKey = client.options.hotbarKeys[index].getBoundKeyLocalizedText();
			Text loadKey = client.options.loadToolbarActivatorKey.getBoundKeyLocalizedText();
			Text savedMessage = Text.translatable("inventory.hotbarSaved", loadKey, hotbarKey);
			client.inGameHud.setOverlayMessage(savedMessage, false);
			client.getNarratorManager().narrateSystemImmediately(savedMessage);
			hotbarStorage.save();
		}
	}
	
	/**
		* Обработчик экрана творческого инвентаря, управляющий прокруткой
		* и отображением предметов из списка {@link #itemList}.
		*/
	@Environment(EnvType.CLIENT)
	public static class CreativeScreenHandler extends ScreenHandler {
	
		public final DefaultedList<ItemStack> itemList = DefaultedList.of();
		private final ScreenHandler parent;
	
		public CreativeScreenHandler(PlayerEntity player) {
			super(null, 0);
			parent = player.playerScreenHandler;
			PlayerInventory playerInventory = player.getInventory();
	
			for (int row = 0; row < ROWS_COUNT; row++) {
				for (int col = 0; col < COLUMNS_COUNT; col++) {
					addSlot(new CreativeInventoryScreen.LockableSlot(
						CreativeInventoryScreen.INVENTORY,
						row * COLUMNS_COUNT + col,
						INVENTORY_SLOT_GRID_X + col * SLOT_SIZE,
						SCROLLBAR_AREA_Y_OFFSET + row * SLOT_SIZE
					));
				}
			}
	
			addPlayerHotbarSlots(playerInventory, INVENTORY_SLOT_GRID_X, INVENTORY_HOTBAR_Y);
			scrollItems(0.0F);
		}
	
		@Override
		public boolean canUse(PlayerEntity player) {
			return true;
		}
	
		protected int getOverflowRows() {
			return MathHelper.ceilDiv(itemList.size(), COLUMNS_COUNT) - ROWS_COUNT;
		}
	
		protected int getRow(float scroll) {
			return Math.max((int) (scroll * getOverflowRows() + 0.5), 0);
		}
	
		protected float getScrollPosition(int row) {
			return MathHelper.clamp((float) row / getOverflowRows(), 0.0F, 1.0F);
		}
	
		protected float getScrollPosition(float current, double amount) {
			return MathHelper.clamp(current - (float) (amount / getOverflowRows()), 0.0F, 1.0F);
		}
	
		/**
			* Прокручивает список предметов к указанной позиции,
			* заполняя слоты инвентаря соответствующими предметами.
			*
			* @param position нормализованная позиция прокрутки [0.0, 1.0]
			*/
		public void scrollItems(float position) {
			int startRow = getRow(position);
	
			for (int row = 0; row < ROWS_COUNT; row++) {
				for (int col = 0; col < COLUMNS_COUNT; col++) {
					int itemIndex = col + (row + startRow) * COLUMNS_COUNT;
					ItemStack stack = itemIndex >= 0 && itemIndex < itemList.size()
						? itemList.get(itemIndex)
						: ItemStack.EMPTY;
					CreativeInventoryScreen.INVENTORY.setStack(col + row * COLUMNS_COUNT, stack);
				}
			}
		}
	
		public boolean shouldShowScrollbar() {
			return itemList.size() > ITEMS_PER_PAGE;
		}
	
		@Override
		public ItemStack quickMove(PlayerEntity player, int slot) {
			if (slot >= slots.size() - COLUMNS_COUNT && slot < slots.size()) {
				Slot targetSlot = slots.get(slot);
				if (targetSlot != null && targetSlot.hasStack()) {
					targetSlot.setStack(ItemStack.EMPTY);
				}
			}
	
			return ItemStack.EMPTY;
		}
	
		@Override
		public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
			return slot.inventory != CreativeInventoryScreen.INVENTORY;
		}
	
		@Override
		public boolean canInsertIntoSlot(Slot slot) {
			return slot.inventory != CreativeInventoryScreen.INVENTORY;
		}
	
		@Override
		public ItemStack getCursorStack() {
			return parent.getCursorStack();
		}
	
		@Override
		public void setCursorStack(ItemStack stack) {
			parent.setCursorStack(stack);
		}
	}
	
	/**
		* Слот творческого инвентаря, делегирующий все операции оригинальному слоту.
		*/
	@Environment(EnvType.CLIENT)
	static class CreativeSlot extends Slot {
	
		final Slot slot;
	
		public CreativeSlot(Slot slot, int invSlot, int x, int y) {
			super(slot.inventory, invSlot, x, y);
			this.slot = slot;
		}
	
		@Override
		public void onTakeItem(PlayerEntity player, ItemStack stack) {
			slot.onTakeItem(player, stack);
		}
	
		@Override
		public boolean canInsert(ItemStack stack) {
			return slot.canInsert(stack);
		}
	
		@Override
		public ItemStack getStack() {
			return slot.getStack();
		}
	
		@Override
		public boolean hasStack() {
			return slot.hasStack();
		}
	
		@Override
		public void setStack(ItemStack stack, ItemStack previousStack) {
			slot.setStack(stack, previousStack);
		}
	
		@Override
		public void setStackNoCallbacks(ItemStack stack) {
			slot.setStackNoCallbacks(stack);
		}
	
		@Override
		public void markDirty() {
			slot.markDirty();
		}
	
		@Override
		public int getMaxItemCount() {
			return slot.getMaxItemCount();
		}
	
		@Override
		public int getMaxItemCount(ItemStack stack) {
			return slot.getMaxItemCount(stack);
		}
	
		@Override
		public @Nullable Identifier getBackgroundSprite() {
			return slot.getBackgroundSprite();
		}
	
		@Override
		public ItemStack takeStack(int amount) {
			return slot.takeStack(amount);
		}
	
		@Override
		public boolean isEnabled() {
			return slot.isEnabled();
		}
	
		@Override
		public boolean canTakeItems(PlayerEntity playerEntity) {
			return slot.canTakeItems(playerEntity);
		}
	}
	
	/**
		* Слот с блокировкой: запрещает взятие предметов с компонентом {@code CREATIVE_SLOT_LOCK}
		* или отключённых текущим набором функций.
		*/
	@Environment(EnvType.CLIENT)
	static class LockableSlot extends Slot {
	
		public LockableSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}
	
		@Override
		public boolean canTakeItems(PlayerEntity playerEntity) {
			ItemStack itemStack = getStack();
			if (itemStack.isEmpty()) {
				return true;
			}
	
			return super.canTakeItems(playerEntity)
				&& itemStack.isItemEnabled(playerEntity.getEntityWorld().getEnabledFeatures())
				&& !itemStack.contains(DataComponentTypes.CREATIVE_SLOT_LOCK);
		}
	}
	}
