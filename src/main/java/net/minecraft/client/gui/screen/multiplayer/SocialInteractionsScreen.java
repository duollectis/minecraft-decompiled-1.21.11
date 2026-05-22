package net.minecraft.client.gui.screen.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Urls;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Экран социальных взаимодействий — управление списком игроков, скрытие,
 * блокировка и отправка жалоб. Поддерживает три вкладки: все, скрытые, заблокированные.
 */
@Environment(EnvType.CLIENT)
public class SocialInteractionsScreen extends Screen {

	private static final Text TITLE = Text.translatable("gui.socialInteractions.title");
	private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("social_interactions/background");
	private static final Identifier SEARCH_ICON_TEXTURE = Identifier.ofVanilla("icon/search");
	private static final Text ALL_TAB_TITLE = Text.translatable("gui.socialInteractions.tab_all");
	private static final Text HIDDEN_TAB_TITLE = Text.translatable("gui.socialInteractions.tab_hidden");
	private static final Text BLOCKED_TAB_TITLE = Text.translatable("gui.socialInteractions.tab_blocked");
	private static final Text SELECTED_ALL_TAB_TITLE = ALL_TAB_TITLE.copyContentOnly().formatted(Formatting.UNDERLINE);
	private static final Text SELECTED_HIDDEN_TAB_TITLE = HIDDEN_TAB_TITLE.copyContentOnly().formatted(Formatting.UNDERLINE);
	private static final Text SELECTED_BLOCKED_TAB_TITLE = BLOCKED_TAB_TITLE.copyContentOnly().formatted(Formatting.UNDERLINE);
	private static final Text SEARCH_TEXT = Text.translatable("gui.socialInteractions.search_hint").fillStyle(TextFieldWidget.SEARCH_STYLE);
	static final Text EMPTY_SEARCH_TEXT = Text.translatable("gui.socialInteractions.search_empty").formatted(Formatting.GRAY);
	private static final Text EMPTY_HIDDEN_TEXT = Text.translatable("gui.socialInteractions.empty_hidden").formatted(Formatting.GRAY);
	private static final Text EMPTY_BLOCKED_TEXT = Text.translatable("gui.socialInteractions.empty_blocked").formatted(Formatting.GRAY);
	private static final Text BLOCKING_TEXT = Text.translatable("gui.socialInteractions.blocking_hint");

	private static final int PADDING = 8;
	private static final int SEARCH_BOX_WIDTH = 236;
	private static final int HEADER_HEIGHT = 16;
	private static final int TABS_HEIGHT = 64;
	private static final int SEARCH_MAX_LENGTH = 16;
	public static final int PLAYER_LIST_Y = 72;
	public static final int TABS_Y = 88;
	private static final int SCREEN_WIDTH = 238;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW_HEIGHT = 36;
	private static final int TAB_BUTTONS_Y = 45;
	private static final int SEARCH_BOX_Y = 74;
	private static final int SEARCH_ICON_OFFSET_X = 10;
	private static final int SEARCH_ICON_Y = 76;
	private static final int SEARCH_ICON_SIZE = 12;
	private static final int SEARCH_BOX_OFFSET_X = 28;

	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final @Nullable Screen parent;
	@Nullable SocialInteractionsPlayerListWidget playerList;
	TextFieldWidget searchBox;
	private String currentSearch = "";
	private Tab currentTab = Tab.ALL;
	private ButtonWidget allTabButton;
	private ButtonWidget hiddenTabButton;
	private ButtonWidget blockedTabButton;
	private ButtonWidget blockingButton;
	private @Nullable Text serverLabel;
	private int playerCount;

	public SocialInteractionsScreen() {
		this(null);
	}

	public SocialInteractionsScreen(@Nullable Screen parent) {
		super(TITLE);
		this.parent = parent;
		updateServerLabel(MinecraftClient.getInstance());
	}

	private int getScreenHeight() {
		return Math.max(52, height - 128 - HEADER_HEIGHT);
	}

	private int getPlayerListBottom() {
		return 80 + getScreenHeight() - PADDING;
	}

	private int getSearchBoxX() {
		return (width - SCREEN_WIDTH) / 2;
	}

	@Override
	public Text getNarratedTitle() {
		return serverLabel != null
			? ScreenTexts.joinSentences(super.getNarratedTitle(), serverLabel)
			: super.getNarratedTitle();
	}

	@Override
	protected void init() {
		layout.addHeader(TITLE, textRenderer);
		playerList = new SocialInteractionsPlayerListWidget(
			this,
			client,
			width,
			getPlayerListBottom() - TABS_Y,
			TABS_Y,
			ROW_HEIGHT
		);
		int tabWidth = playerList.getRowWidth() / 3;
		int rowLeft = playerList.getRowLeft();
		int rowRight = playerList.getRowRight();
		allTabButton = addDrawableChild(
			ButtonWidget.builder(ALL_TAB_TITLE, button -> setCurrentTab(Tab.ALL))
				.dimensions(rowLeft, TAB_BUTTONS_Y, tabWidth, BUTTON_HEIGHT)
				.build()
		);
		hiddenTabButton = addDrawableChild(
			ButtonWidget.builder(HIDDEN_TAB_TITLE, button -> setCurrentTab(Tab.HIDDEN))
				.dimensions((rowLeft + rowRight - tabWidth) / 2 + 1, TAB_BUTTONS_Y, tabWidth, BUTTON_HEIGHT)
				.build()
		);
		blockedTabButton = addDrawableChild(
			ButtonWidget.builder(BLOCKED_TAB_TITLE, button -> setCurrentTab(Tab.BLOCKED))
				.dimensions(rowRight - tabWidth + 1, TAB_BUTTONS_Y, tabWidth, BUTTON_HEIGHT)
				.build()
		);
		String savedSearch = searchBox != null ? searchBox.getText() : "";
		searchBox = addDrawableChild(
			new TextFieldWidget(textRenderer, getSearchBoxX() + SEARCH_BOX_OFFSET_X, SEARCH_BOX_Y, 200, 15, SEARCH_TEXT) {
				@Override
				protected MutableText getNarrationMessage() {
					return !SocialInteractionsScreen.this.searchBox.getText().isEmpty()
						&& SocialInteractionsScreen.this.playerList.isEmpty()
						? super.getNarrationMessage().append(", ").append(SocialInteractionsScreen.EMPTY_SEARCH_TEXT)
						: super.getNarrationMessage();
				}
			}
		);
		searchBox.setMaxLength(SEARCH_MAX_LENGTH);
		searchBox.setVisible(true);
		searchBox.setEditableColor(-1);
		searchBox.setText(savedSearch);
		searchBox.setPlaceholder(SEARCH_TEXT);
		searchBox.setChangedListener(this::onSearchChange);
		blockingButton = addDrawableChild(
			ButtonWidget.builder(BLOCKING_TEXT, ConfirmLinkScreen.opening(this, Urls.JAVA_BLOCKING))
				.dimensions(width / 2 - 100, TABS_HEIGHT + getScreenHeight(), 200, BUTTON_HEIGHT)
				.build()
		);
		addSelectableChild(playerList);
		setCurrentTab(currentTab);
		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	public void onDisplayed() {
		if (playerList != null) {
			playerList.updateHasDraftReport();
		}
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		playerList.position(width, getPlayerListBottom() - TABS_Y, TABS_Y);
		searchBox.setPosition(getSearchBoxX() + SEARCH_BOX_OFFSET_X, SEARCH_BOX_Y);
		int rowLeft = playerList.getRowLeft();
		int rowRight = playerList.getRowRight();
		int tabWidth = playerList.getRowWidth() / 3;
		allTabButton.setPosition(rowLeft, TAB_BUTTONS_Y);
		hiddenTabButton.setPosition((rowLeft + rowRight - tabWidth) / 2 + 1, TAB_BUTTONS_Y);
		blockedTabButton.setPosition(rowRight - tabWidth + 1, TAB_BUTTONS_Y);
		blockingButton.setPosition(width / 2 - 100, TABS_HEIGHT + getScreenHeight());
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(searchBox);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	private void setCurrentTab(Tab tab) {
		currentTab = tab;
		allTabButton.setMessage(ALL_TAB_TITLE);
		hiddenTabButton.setMessage(HIDDEN_TAB_TITLE);
		blockedTabButton.setMessage(BLOCKED_TAB_TITLE);
		boolean isEmpty = false;
		switch (tab) {
			case ALL -> {
				allTabButton.setMessage(SELECTED_ALL_TAB_TITLE);
				Collection<UUID> onlinePlayers = client.player.networkHandler.getPlayerUuids();
				playerList.update(onlinePlayers, playerList.getScrollY(), true);
			}
			case HIDDEN -> {
				hiddenTabButton.setMessage(SELECTED_HIDDEN_TAB_TITLE);
				Set<UUID> hiddenPlayers = client.getSocialInteractionsManager().getHiddenPlayers();
				isEmpty = hiddenPlayers.isEmpty();
				playerList.update(hiddenPlayers, playerList.getScrollY(), false);
			}
			case BLOCKED -> {
				blockedTabButton.setMessage(SELECTED_BLOCKED_TAB_TITLE);
				SocialInteractionsManager socialManager = client.getSocialInteractionsManager();
				Set<UUID> blockedPlayers = client.player.networkHandler
					.getPlayerUuids()
					.stream()
					.filter(socialManager::isPlayerBlocked)
					.collect(Collectors.toSet());
				isEmpty = blockedPlayers.isEmpty();
				playerList.update(blockedPlayers, playerList.getScrollY(), false);
			}
		}

		NarratorManager narratorManager = client.getNarratorManager();
		if (!searchBox.getText().isEmpty() && playerList.isEmpty() && !searchBox.isFocused()) {
			narratorManager.narrateSystemImmediately(EMPTY_SEARCH_TEXT);
		}
		else if (isEmpty) {
			if (tab == Tab.HIDDEN) {
				narratorManager.narrateSystemImmediately(EMPTY_HIDDEN_TEXT);
			}
			else if (tab == Tab.BLOCKED) {
				narratorManager.narrateSystemImmediately(EMPTY_BLOCKED_TEXT);
			}
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.renderBackground(context, mouseX, mouseY, deltaTicks);
		int backgroundX = getSearchBoxX() + 3;
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			BACKGROUND_TEXTURE,
			backgroundX,
			TABS_HEIGHT,
			SEARCH_BOX_WIDTH,
			getScreenHeight() + HEADER_HEIGHT
		);
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SEARCH_ICON_TEXTURE, backgroundX + SEARCH_ICON_OFFSET_X, SEARCH_ICON_Y, SEARCH_ICON_SIZE, SEARCH_ICON_SIZE);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		updateServerLabel(client);
		if (serverLabel != null) {
			context.drawTextWithShadow(client.textRenderer, serverLabel, getSearchBoxX() + PADDING, 35, -1);
		}

		if (!playerList.isEmpty()) {
			playerList.render(context, mouseX, mouseY, deltaTicks);
		}
		else if (!searchBox.getText().isEmpty()) {
			context.drawCenteredTextWithShadow(
				client.textRenderer,
				EMPTY_SEARCH_TEXT,
				width / 2,
				(PLAYER_LIST_Y + getPlayerListBottom()) / 2,
				-1
			);
		}
		else if (currentTab == Tab.HIDDEN) {
			context.drawCenteredTextWithShadow(
				client.textRenderer,
				EMPTY_HIDDEN_TEXT,
				width / 2,
				(PLAYER_LIST_Y + getPlayerListBottom()) / 2,
				-1
			);
		}
		else if (currentTab == Tab.BLOCKED) {
			context.drawCenteredTextWithShadow(
				client.textRenderer,
				EMPTY_BLOCKED_TEXT,
				width / 2,
				(PLAYER_LIST_Y + getPlayerListBottom()) / 2,
				-1
			);
		}

		blockingButton.visible = currentTab == Tab.BLOCKED;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!searchBox.isFocused() && client.options.socialInteractionsKey.matchesKey(input)) {
			close();
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void onSearchChange(String newSearch) {
		String normalized = newSearch.toLowerCase(Locale.ROOT);
		if (normalized.equals(currentSearch)) {
			return;
		}

		playerList.setCurrentSearch(normalized);
		currentSearch = normalized;
		setCurrentTab(currentTab);
	}

	private void updateServerLabel(MinecraftClient mc) {
		int onlineCount = mc.getNetworkHandler().getPlayerList().size();
		if (playerCount == onlineCount) {
			return;
		}

		String serverName = "";
		ServerInfo serverInfo = mc.getCurrentServerEntry();
		if (mc.isInSingleplayer()) {
			serverName = mc.getServer().getServerMotd();
		}
		else if (serverInfo != null) {
			serverName = serverInfo.name;
		}

		serverLabel = onlineCount > 1
			? Text.translatable("gui.socialInteractions.server_label.multiple", serverName, onlineCount)
			: Text.translatable("gui.socialInteractions.server_label.single", serverName, onlineCount);

		playerCount = onlineCount;
	}

	public void setPlayerOnline(PlayerListEntry player) {
		playerList.setPlayerOnline(player, currentTab);
	}

	public void setPlayerOffline(UUID uuid) {
		playerList.setPlayerOffline(uuid);
	}

	/**
	 * Вкладки экрана социальных взаимодействий.
	 */
	@Environment(EnvType.CLIENT)
	public enum Tab {
		ALL,
		HIDDEN,
		BLOCKED
	}
}
