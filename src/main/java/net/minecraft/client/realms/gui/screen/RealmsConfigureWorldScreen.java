package net.minecraft.client.realms.gui.screen;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tab.LoadingTab;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsError;
import net.minecraft.client.realms.ServiceQuality;
import net.minecraft.client.realms.dto.*;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.screen.tab.*;
import net.minecraft.client.realms.task.CloseServerTask;
import net.minecraft.client.realms.task.OpenServerTask;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsConfigureWorldScreen}.
 */
public class RealmsConfigureWorldScreen extends RealmsScreen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text PLAY_BUTTON_TEXT = Text.translatable("mco.selectServer.play");
	private final RealmsMainScreen parent;
	private @Nullable RealmsServer server;
	private @Nullable RealmsRegionDataList regionDataList;
	private final Map<RealmsRegion, ServiceQuality> regions = new LinkedHashMap<>();
	private final long serverId;
	private boolean stateChanged;
	private final TabManager tabManager = new TabManager(
			loadedWidget -> {
				ClickableWidget var10000 = this.addDrawableChild(loadedWidget);
			}, unloadedWidget -> this.remove(unloadedWidget), this::onTabLoaded, this::onTabUnloaded
	);
	private @Nullable ButtonWidget playButton;
	private @Nullable TabNavigationWidget tabNavigation;
	public final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

	public RealmsConfigureWorldScreen(
			RealmsMainScreen parent,
			long serverId,
			@Nullable RealmsServer server,
			@Nullable RealmsRegionDataList regionDataList
	) {
		super(Text.empty());
		this.parent = parent;
		this.serverId = serverId;
		this.server = server;
		this.regionDataList = regionDataList;
	}

	public RealmsConfigureWorldScreen(RealmsMainScreen parent, long serverId) {
		this(parent, serverId, null, null);
	}

	@Override
	public void init() {
		if (this.server == null) {
			this.fetchServerData(this.serverId);
		}

		if (this.regionDataList == null) {
			this.fetchRegionDataList();
		}

		Text text = Text.translatable("mco.configure.world.loading");
		this.tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width)
		                                        .tabs(
				                                        new LoadingTab(
						                                        this.getTextRenderer(),
						                                        RealmsWorldsTab.TITLE_TEXT,
						                                        text
				                                        ),
				                                        new LoadingTab(
						                                        this.getTextRenderer(),
						                                        RealmsPlayerTab.TITLE,
						                                        text
				                                        ),
				                                        new LoadingTab(
						                                        this.getTextRenderer(),
						                                        RealmsSubscriptionInfoTab.SUBSCRIPTION_TITLE,
						                                        text
				                                        ),
				                                        new LoadingTab(
						                                        this.getTextRenderer(),
						                                        RealmsSettingsTab.TITLE_TEXT,
						                                        text
				                                        )
		                                        )
		                                        .build();
		this.tabNavigation.setTabActive(3, false);
		this.addDrawableChild(this.tabNavigation);
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		this.playButton = directionalLayoutWidget.add(ButtonWidget.builder(
				PLAY_BUTTON_TEXT, button -> {
					this.close();
					RealmsMainScreen.play(this.server, this);
				}
		).width(150).build());
		this.playButton.active = false;
		directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).build());
		this.layout.forEachChild(child -> {
			child.setNavigationOrder(1);
			this.addDrawableChild(child);
		});
		this.tabNavigation.selectTab(0, false);
		this.refreshWidgetPositions();
		if (this.server != null && this.regionDataList != null) {
			this.refresh();
		}
	}

	private void onTabLoaded(Tab tab) {
		if (this.server != null && tab instanceof RealmsUpdatableTab realmsUpdatableTab) {
			realmsUpdatableTab.onLoaded(this.server);
		}
	}

	private void onTabUnloaded(Tab tab) {
		if (this.server != null && tab instanceof RealmsUpdatableTab realmsUpdatableTab) {
			realmsUpdatableTab.onUnloaded(this.server);
		}
	}

	public int getContentHeight() {
		return this.layout.getContentHeight();
	}

	public int getHeaderHeight() {
		return this.layout.getHeaderHeight();
	}

	public Screen getParent() {
		return this.parent;
	}

	/**
	 * Создаёт error screen.
	 *
	 * @param error error
	 *
	 * @return Screen — результат операции
	 */
	public Screen createErrorScreen(RealmsServiceException error) {
		return new RealmsGenericErrorScreen(error, this.parent);
	}

	@Override
	public void refreshWidgetPositions() {
		if (this.tabNavigation != null) {
			this.tabNavigation.setWidth(this.width);
			this.tabNavigation.init();
			int i = this.tabNavigation.getNavigationFocus().getBottom();
			ScreenRect screenRect = new ScreenRect(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
			this.tabManager.setTabArea(screenRect);
			this.layout.setHeaderHeight(i);
			this.layout.refreshPositions();
		}
	}

	private void updatePlayButton() {
		if (this.server != null && this.playButton != null) {
			this.playButton.active = this.server.shouldAllowPlay();
			if (!this.playButton.active && this.server.state == RealmsServer.State.CLOSED) {
				this.playButton.setTooltip(Tooltip.of(RealmsServer.REALM_CLOSED_TEXT));
			}
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				Screen.FOOTER_SEPARATOR_TEXTURE,
				0,
				this.height - this.layout.getFooterHeight() - 2,
				0.0F,
				0.0F,
				this.width,
				2,
				32,
				2
		);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return this.tabNavigation.keyPressed(input) ? true : super.keyPressed(input);
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
		if (this.server != null && this.tabManager.getCurrentTab() instanceof RealmsUpdatableTab realmsUpdatableTab) {
			realmsUpdatableTab.onUnloaded(this.server);
		}

		this.client.setScreen(this.parent);
		if (this.stateChanged) {
			this.parent.removeSelection();
		}
	}

	/**
	 * Fetch region data list.
	 */
	public void fetchRegionDataList() {
		RealmsUtil.<RealmsRegionDataList>runAsync(
				          (RealmsUtil.RealmsSupplier<RealmsRegionDataList>) RealmsClient::getRegionDataList,
				          RealmsUtil.openingScreenAndLogging(this::createErrorScreen, "Couldn't get realms region data")
		          )
		          .thenAcceptAsync(
				          regionDataList -> {
					          this.regionDataList = regionDataList;
					          this.refresh();
				          }, this.client
		          );
	}

	/**
	 * Fetch server data.
	 *
	 * @param worldId world id
	 */
	public void fetchServerData(long worldId) {
		RealmsUtil.<RealmsServer>runAsync(
				          (RealmsUtil.RealmsSupplier<RealmsServer>) client -> client.getOwnWorld(worldId),
				          RealmsUtil.openingScreenAndLogging(this::createErrorScreen, "Couldn't get own world")
		          )
		          .thenAcceptAsync(
				          server -> {
					          this.server = server;
					          this.refresh();
				          }, this.client
		          );
	}

	private void refresh() {
		if (this.server != null && this.regionDataList != null) {
			this.regions.clear();

			for (RegionData regionData : this.regionDataList.regionData()) {
				if (regionData.region() != RealmsRegion.INVALID_REGION) {
					this.regions.put(regionData.region(), regionData.serviceQuality());
				}
			}

			int i = -1;
			if (this.tabNavigation != null) {
				i = this.tabNavigation.getTabs().indexOf(this.tabManager.getCurrentTab());
			}

			if (this.tabNavigation != null) {
				this.remove(this.tabNavigation);
			}

			this.tabNavigation = this.addDrawableChild(
					TabNavigationWidget.builder(this.tabManager, this.width)
					                   .tabs(
							                   new RealmsWorldsTab(
									                   this,
									                   Objects.requireNonNull(this.client),
									                   this.server
							                   ),
							                   new RealmsPlayerTab(this, this.client, this.server),
							                   new RealmsSubscriptionInfoTab(this, this.client, this.server),
							                   new RealmsSettingsTab(this, this.client, this.server, this.regions)
					                   )
					                   .build()
			);
			this.setFocused(this.tabNavigation);
			if (i != -1) {
				this.tabNavigation.selectTab(i, false);
			}

			this.tabNavigation.setTabActive(3, !this.server.expired);
			if (this.server.expired) {
				this.tabNavigation.setTabTooltip(
						3,
						Tooltip.of(Text.translatable("mco.configure.world.settings.expired"))
				);
			}
			else {
				this.tabNavigation.setTabTooltip(3, null);
			}

			this.updatePlayButton();
			this.refreshWidgetPositions();
		}
	}

	/**
	 * Сохраняет slot settings.
	 *
	 * @param slot slot
	 */
	public void saveSlotSettings(RealmsSlot slot) {
		RealmsSlot realmsSlot = this.server.slots.get(this.server.activeSlot);
		slot.options.templateId = realmsSlot.options.templateId;
		slot.options.templateImage = realmsSlot.options.templateImage;
		RealmsClient realmsClient = RealmsClient.create();

		try {
			if (this.server.activeSlot != slot.slotId) {
				throw new RealmsServiceException(RealmsError.SimpleHttpError.configurationError());
			}

			realmsClient.updateSlot(this.server.id, slot.slotId, slot.options, slot.settings);
			this.server.slots.put(this.server.activeSlot, slot);
			if (slot.options.gameMode != realmsSlot.options.gameMode || slot.isHardcore() != realmsSlot.isHardcore()) {
				RealmsMainScreen.resetServerList();
			}

			this.stateChanged();
		}
		catch (RealmsServiceException var5) {
			LOGGER.error("Couldn't save slot settings", var5);
			this.client.setScreen(new RealmsGenericErrorScreen(var5, this));
			return;
		}

		this.client.setScreen(this);
	}

	public void saveSettings(
			String name,
			String description,
			RegionSelectionMethod regionSelectionMethod,
			@Nullable RealmsRegion region
	) {
		String string = StringHelper.isBlank(description) ? "" : description;
		String string2 = StringHelper.isBlank(name) ? "" : name;
		RealmsClient realmsClient = RealmsClient.create();

		try {
			RealmsSlot realmsSlot = this.server.slots.get(this.server.activeSlot);
			RealmsRegion realmsRegion = regionSelectionMethod == RegionSelectionMethod.MANUAL ? region : null;
			RealmsRegionSelectionPreference
					realmsRegionSelectionPreference =
					new RealmsRegionSelectionPreference(regionSelectionMethod, realmsRegion);
			realmsClient.configure(
					this.server.id,
					string2,
					string,
					realmsRegionSelectionPreference,
					realmsSlot.slotId,
					realmsSlot.options,
					realmsSlot.settings
			);
			this.server.regionSelectionPreference = realmsRegionSelectionPreference;
			this.server.name = name;
			this.server.description = string;
			this.stateChanged();
		}
		catch (RealmsServiceException var11) {
			LOGGER.error("Couldn't save settings", var11);
			this.client.setScreen(new RealmsGenericErrorScreen(var11, this));
			return;
		}

		this.client.setScreen(this);
	}

	/**
	 * Открывает the world.
	 *
	 * @param join join
	 */
	public void openTheWorld(boolean join) {
		RealmsConfigureWorldScreen realmsConfigureWorldScreen = this.withServer(this.server);
		this.client
				.setScreen(new RealmsLongRunningMcoTaskScreen(
						this.getNewScreen(),
						new OpenServerTask(this.server, realmsConfigureWorldScreen, join, this.client)
				));
	}

	/**
	 * Закрывает the world.
	 */
	public void closeTheWorld() {
		RealmsConfigureWorldScreen realmsConfigureWorldScreen = this.withServer(this.server);
		this.client.setScreen(new RealmsLongRunningMcoTaskScreen(
				this.getNewScreen(),
				new CloseServerTask(this.server, realmsConfigureWorldScreen)
		));
	}

	/**
	 * State changed.
	 */
	public void stateChanged() {
		this.stateChanged = true;
		if (this.tabNavigation != null) {
			for (Tab tab : this.tabNavigation.getTabs()) {
				if (tab instanceof RealmsUpdatableTab realmsUpdatableTab) {
					realmsUpdatableTab.update(this.server);
				}
			}
		}
	}

	/**
	 * Invite.
	 *
	 * @param worldId world id
	 * @param profileName profile name
	 *
	 * @return boolean — результат операции
	 */
	public boolean invite(long worldId, String profileName) {
		RealmsClient realmsClient = RealmsClient.create();

		try {
			List<PlayerInfo> list = realmsClient.invite(worldId, profileName);
			if (this.server != null) {
				this.server.players = list;
			}
			else {
				this.server = realmsClient.getOwnWorld(worldId);
			}

			this.stateChanged();
			return true;
		}
		catch (RealmsServiceException var6) {
			LOGGER.error("Couldn't invite user", var6);
			return false;
		}
	}

	public RealmsConfigureWorldScreen getNewScreen() {
		RealmsConfigureWorldScreen
				realmsConfigureWorldScreen =
				new RealmsConfigureWorldScreen(this.parent, this.serverId);
		realmsConfigureWorldScreen.stateChanged = this.stateChanged;
		return realmsConfigureWorldScreen;
	}

	/**
	 * With server.
	 *
	 * @param server server
	 *
	 * @return RealmsConfigureWorldScreen — результат операции
	 */
	public RealmsConfigureWorldScreen withServer(RealmsServer server) {
		RealmsConfigureWorldScreen
				realmsConfigureWorldScreen =
				new RealmsConfigureWorldScreen(this.parent, this.serverId, server, this.regionDataList);
		realmsConfigureWorldScreen.stateChanged = this.stateChanged;
		return realmsConfigureWorldScreen;
	}
}
