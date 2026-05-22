package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.ProfilesTooltipComponent;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.realms.Ping;
import net.minecraft.client.realms.RealmsAvailability;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsPeriodicCheckers;
import net.minecraft.client.realms.dto.*;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.task.RealmsPrepareConnectionTask;
import net.minecraft.client.realms.util.PeriodicRunnerFactory;
import net.minecraft.client.realms.util.RealmsPersistence;
import net.minecraft.client.realms.util.RealmsServerFilterer;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Urls;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Главный экран Realms — отображает список серверов, уведомления и кнопки управления.
 */
@Environment(EnvType.CLIENT)
public class RealmsMainScreen extends RealmsScreen {

	static final Identifier INFO_ICON_TEXTURE = Identifier.ofVanilla("icon/info");
	static final Identifier NEW_REALM_ICON_TEXTURE = Identifier.ofVanilla("icon/new_realm");
	static final Identifier EXPIRED_STATUS_TEXTURE = Identifier.ofVanilla("realm_status/expired");
	static final Identifier EXPIRES_SOON_STATUS_TEXTURE = Identifier.ofVanilla("realm_status/expires_soon");
	static final Identifier OPEN_STATUS_TEXTURE = Identifier.ofVanilla("realm_status/open");
	static final Identifier CLOSED_STATUS_TEXTURE = Identifier.ofVanilla("realm_status/closed");
	private static final Identifier INVITE_ICON_TEXTURE = Identifier.ofVanilla("icon/invite");
	private static final Identifier NEWS_ICON_TEXTURE = Identifier.ofVanilla("icon/news");
	public static final Identifier HARDCORE_ICON_TEXTURE = Identifier.ofVanilla("hud/heart/hardcore_full");
	static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier NO_REALMS_TEXTURE = Identifier.ofVanilla("textures/gui/realms/no_realms.png");
	private static final Text MENU_TEXT = Text.translatable("menu.online");
	private static final Text LOADING_TEXT = Text.translatable("mco.selectServer.loading");
	static final Text UNINITIALIZED_TEXT = Text.translatable("mco.selectServer.uninitialized");
	static final Text EXPIRED_LIST_TEXT = Text.translatable("mco.selectServer.expiredList");
	private static final Text EXPIRED_RENEW_TEXT = Text.translatable("mco.selectServer.expiredRenew");
	static final Text EXPIRED_TRIAL_TEXT = Text.translatable("mco.selectServer.expiredTrial");
	private static final Text PLAY_TEXT = Text.translatable("mco.selectServer.play");
	private static final Text LEAVE_TEXT = Text.translatable("mco.selectServer.leave");
	private static final Text CONFIGURE_TEXT = Text.translatable("mco.selectServer.configure");
	static final Text EXPIRED_TEXT = Text.translatable("mco.selectServer.expired");
	static final Text EXPIRES_SOON_TEXT = Text.translatable("mco.selectServer.expires.soon");
	static final Text EXPIRES_IN_A_DAY_TEXT = Text.translatable("mco.selectServer.expires.day");
	static final Text OPEN_TEXT = Text.translatable("mco.selectServer.open");
	static final Text CLOSED_TEXT = Text.translatable("mco.selectServer.closed");
	static final Text UNINITIALIZED_BUTTON_NARRATION = Text.translatable("gui.narrate.button", UNINITIALIZED_TEXT);
	private static final Text NO_REALMS_TEXT = Text.translatable("mco.selectServer.noRealms");
	private static final Text NO_PENDING_TOOLTIP = Text.translatable("mco.invites.nopending");
	private static final Text PENDING_TOOLTIP = Text.translatable("mco.invites.pending");
	private static final Text
			INCOMPATIBLE_POPUP_TITLE =
			Text.translatable("mco.compatibility.incompatible.popup.title");
	private static final Text
			INCOMPATIBLE_RELEASE_TYPE_MESSAGE =
			Text.translatable("mco.compatibility.incompatible.releaseType.popup.message");
	private static final int BUTTON_WIDTH = 100;
	private static final int PERIODIC_CHECK_INTERVAL_SECONDS = 3;
	private static final int BOTTOM_BUTTON_COUNT = 4;
	private static final int SERVER_LIST_WIDTH = 308;
	private static final int EDGE_PADDING = 5;
	private static final int LIST_ENTRY_HEIGHT = 44;
	private static final int ICON_PADDING = 11;
	private static final int NEW_REALM_ICON_WIDTH = 40;
	private static final int NEW_REALM_ICON_HEIGHT = 20;
	private static final int HEADER_SIDE_WIDTH = 90;
	private static final boolean GAME_ON_SNAPSHOT = !SharedConstants.getGameVersion().stable();
	private static boolean showingSnapshotRealms = GAME_ON_SNAPSHOT;
	private final CompletableFuture<RealmsAvailability.Info> availabilityInfo = RealmsAvailability.check();
	private PeriodicRunnerFactory.@Nullable RunnersManager periodicRunnersManager;
	private final Set<UUID> seenNotifications = new HashSet<>();
	private static boolean regionsPinged;
	private final RateLimiter rateLimiter;
	private final Screen parent;
	private ButtonWidget playButton;
	private ButtonWidget backButton;
	private ButtonWidget renewButton;
	private ButtonWidget configureButton;
	private ButtonWidget leaveButton;
	RealmsMainScreen.RealmSelectionList realmSelectionList;
	RealmsServerFilterer serverFilterer;
	List<RealmsServer> availableSnapshotServers = List.of();
	RealmsServerPlayerList onlinePlayers = new RealmsServerPlayerList(Map.of());
	private volatile boolean trialAvailable;
	private volatile @Nullable String newsLink;
	final List<RealmsNotification> notifications = new ArrayList<>();
	private ButtonWidget purchaseButton;
	private RealmsMainScreen.NotificationButtonWidget inviteButton;
	private RealmsMainScreen.NotificationButtonWidget newsButton;
	private RealmsMainScreen.LoadStatus loadStatus;
	private @Nullable ThreePartsLayoutWidget layout;

	public RealmsMainScreen(Screen parent) {
		super(MENU_TEXT);
		this.parent = parent;
		this.rateLimiter = RateLimiter.create(0.016666668F);
	}

	@Override
	public void init() {
		this.serverFilterer = new RealmsServerFilterer(this.client);
		this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
		Text text = Text.translatable("mco.invites.title");
		this.inviteButton = new RealmsMainScreen.NotificationButtonWidget(
				text,
				INVITE_ICON_TEXTURE,
				button -> this.client.setScreen(new RealmsPendingInvitesScreen(this, text)),
				null
		);
		Text text2 = Text.translatable("mco.news");
		this.newsButton = new RealmsMainScreen.NotificationButtonWidget(
				text2, NEWS_ICON_TEXTURE, button -> {
			String string = this.newsLink;
			if (string != null) {
				ConfirmLinkScreen.open(this, string);
				if (this.newsButton.getNotificationCount() != 0) {
					RealmsPersistence.RealmsPersistenceData realmsPersistenceData = RealmsPersistence.readFile();
					realmsPersistenceData.hasUnreadNews = false;
					RealmsPersistence.writeFile(realmsPersistenceData);
					this.newsButton.setNotificationCount(0);
				}
			}
		}, text2
		);
		this.playButton =
				ButtonWidget.builder(PLAY_TEXT, button -> play(this.getSelectedServer(), this)).width(BUTTON_WIDTH).build();
		this.configureButton =
				ButtonWidget
						.builder(CONFIGURE_TEXT, button -> this.configureClicked(this.getSelectedServer()))
						.width(BUTTON_WIDTH)
						.build();
		this.renewButton =
				ButtonWidget
						.builder(EXPIRED_RENEW_TEXT, button -> this.onRenew(this.getSelectedServer()))
						.width(BUTTON_WIDTH)
						.build();
		this.leaveButton =
				ButtonWidget
						.builder(LEAVE_TEXT, button -> this.leaveClicked(this.getSelectedServer()))
						.width(BUTTON_WIDTH)
						.build();
		this.purchaseButton =
				ButtonWidget
						.builder(Text.translatable("mco.selectServer.purchase"), button -> this.showBuyRealmsScreen())
						.size(BUTTON_WIDTH, 20)
						.build();
		this.backButton = ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).width(BUTTON_WIDTH).build();
		if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
			this.addDrawableChild(
					CyclingButtonWidget
							.onOffBuilder(Text.literal("Snapshot"), Text.literal("Release"), showingSnapshotRealms)
							.build(
									5, 5, BUTTON_WIDTH, NEW_REALM_ICON_HEIGHT, Text.literal("Realm"), (button, snapshot) -> {
										showingSnapshotRealms = snapshot;
										this.availableSnapshotServers = List.of();
										this.resetPeriodicCheckers();
									}
							)
			);
		}

		this.onLoadStatusChange(RealmsMainScreen.LoadStatus.LOADING);
		this.refreshButtons();
		this.availabilityInfo.thenAcceptAsync(
				availabilityInfo -> {
					Screen screen = availabilityInfo.createScreen(this.parent);
					if (screen == null) {
						this.periodicRunnersManager =
								this.createPeriodicRunnersManager(this.client.getRealmsPeriodicCheckers());
					}
					else {
						this.client.setScreen(screen);
					}
				}, this.executor
		);
	}

	/**
	 * Проверяет, должен ли экран показывать снапшот-серверы Realms.
	 * Возвращает {@code true} только если текущая версия игры нестабильна и пользователь включил режим снапшота.
	 */
	public static boolean isSnapshotRealmsEligible() {
		return GAME_ON_SNAPSHOT && showingSnapshotRealms;
	}

	@Override
	protected void refreshWidgetPositions() {
		if (this.layout != null) {
			this.realmSelectionList.position(this.width, this.layout);
			this.layout.refreshPositions();
		}
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}

	/**
	 * Определяет текущий статус загрузки на основе наличия серверов и уведомлений,
	 * переключая экран между состояниями {@code NO_REALMS} и {@code LIST}.
	 */
	private void updateLoadStatus() {
		if (this.serverFilterer.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
			this.onLoadStatusChange(RealmsMainScreen.LoadStatus.NO_REALMS);
		}
		else {
			this.onLoadStatusChange(RealmsMainScreen.LoadStatus.LIST);
		}
	}

	/**
	 * Перестраивает весь layout экрана при смене статуса загрузки.
	 * Удаляет старые виджеты, создаёт новый layout и регистрирует дочерние виджеты.
	 */
	private void onLoadStatusChange(RealmsMainScreen.LoadStatus loadStatus) {
		if (this.loadStatus != loadStatus) {
			if (this.layout != null) {
				this.layout.forEachChild(child -> this.remove(child));
			}

			this.layout = this.makeLayoutFor(loadStatus);
			this.loadStatus = loadStatus;
			this.layout.forEachChild(child -> {
				ClickableWidget var10000 = this.addDrawableChild(child);
			});
			this.refreshWidgetPositions();
		}
	}

	/**
	 * Создаёт трёхчастный layout (шапка / тело / подвал) для заданного статуса загрузки.
	 * Тело варьируется: спиннер загрузки, заглушка «нет серверов» или список серверов.
	 */
	private ThreePartsLayoutWidget makeLayoutFor(RealmsMainScreen.LoadStatus loadStatus) {
		ThreePartsLayoutWidget threePartsLayoutWidget = new ThreePartsLayoutWidget(this);
		threePartsLayoutWidget.setHeaderHeight(LIST_ENTRY_HEIGHT);
		threePartsLayoutWidget.addHeader(this.makeHeader());
		LayoutWidget layoutWidget = this.makeInnerLayout(loadStatus);
		layoutWidget.refreshPositions();
		threePartsLayoutWidget.setFooterHeight(layoutWidget.getHeight() + 22);
		threePartsLayoutWidget.addFooter(layoutWidget);
		switch (loadStatus) {
			case LOADING:
				threePartsLayoutWidget.addBody(new LoadingWidget(this.textRenderer, LOADING_TEXT));
				break;
			case NO_REALMS:
				threePartsLayoutWidget.addBody(this.makeNoRealmsLayout());
				break;
			case LIST:
				threePartsLayoutWidget.addBody(this.realmSelectionList);
		}

		return threePartsLayoutWidget;
	}

	/**
	 * Строит горизонтальный layout шапки: логотип Realms по центру,
	 * кнопки приглашений и новостей выровнены по правому краю.
	 */
	private LayoutWidget makeHeader() {
		DirectionalLayoutWidget iconsLayout = DirectionalLayoutWidget.horizontal().spacing(4);
		iconsLayout.getMainPositioner().alignVerticalCenter();
		iconsLayout.add(this.inviteButton);
		iconsLayout.add(this.newsButton);
		DirectionalLayoutWidget headerLayout = DirectionalLayoutWidget.horizontal();
		headerLayout.getMainPositioner().alignVerticalCenter();
		headerLayout.add(EmptyWidget.ofWidth(HEADER_SIDE_WIDTH));
		headerLayout.add(createRealmsLogoIconWidget(), Positioner::alignHorizontalCenter);
		headerLayout
				.add(new SimplePositioningWidget(HEADER_SIDE_WIDTH, LIST_ENTRY_HEIGHT))
				.add(iconsLayout, Positioner::alignRight);
		return headerLayout;
	}

	/**
	 * Строит сетку кнопок подвала. В режиме {@code LIST} добавляются кнопки
	 * «Играть», «Настроить», «Продлить», «Покинуть»; в остальных — только «Купить» и «Назад».
	 */
	private LayoutWidget makeInnerLayout(RealmsMainScreen.LoadStatus loadStatus) {
		GridWidget gridWidget = new GridWidget().setSpacing(4);
		GridWidget.Adder adder = gridWidget.createAdder(3);
		if (loadStatus == RealmsMainScreen.LoadStatus.LIST) {
			adder.add(this.playButton);
			adder.add(this.configureButton);
			adder.add(this.renewButton);
			adder.add(this.leaveButton);
		}

		adder.add(this.purchaseButton);
		adder.add(this.backButton);
		return gridWidget;
	}

	/**
	 * Создаёт вертикальный layout-заглушку для случая, когда у игрока нет серверов Realms.
	 */
	private DirectionalLayoutWidget makeNoRealmsLayout() {
		DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.vertical().spacing(8);
		directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
		directionalLayoutWidget.add(IconWidget.create(130, 64, NO_REALMS_TEXTURE, 130, 64));
		directionalLayoutWidget.add(
				NarratedMultilineTextWidget.builder(NO_REALMS_TEXT, this.textRenderer)
				                           .width(SERVER_LIST_WIDTH)
				                           .alwaysShowBorders(false)
				                           .backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.ON_FOCUS)
				                           .build()
		);
		return directionalLayoutWidget;
	}

	/**
	 * Обновляет состояние активности кнопок управления сервером в зависимости
	 * от выбранного сервера и его текущего состояния (истёк, закрыт, чужой и т.д.).
	 */
	void refreshButtons() {
		RealmsServer realmsServer = this.getSelectedServer();
		boolean serverSelected = realmsServer != null;
		this.purchaseButton.active = this.loadStatus != RealmsMainScreen.LoadStatus.LOADING;
		this.playButton.active = serverSelected && realmsServer.shouldAllowPlay();
		if (!this.playButton.active && serverSelected && realmsServer.state == RealmsServer.State.CLOSED) {
			this.playButton.setTooltip(Tooltip.of(RealmsServer.REALM_CLOSED_TEXT));
		}

		this.renewButton.active = serverSelected && this.shouldRenewButtonBeActive(realmsServer);
		this.leaveButton.active = serverSelected && this.shouldLeaveButtonBeActive(realmsServer);
		this.configureButton.active = serverSelected && this.shouldConfigureButtonBeActive(realmsServer);
	}

	/** Кнопка «Продлить» активна только для собственных истёкших серверов. */
	private boolean shouldRenewButtonBeActive(RealmsServer server) {
		return server.expired && isSelfOwnedServer(server);
	}

	/** Кнопка «Настроить» активна только для собственных инициализированных серверов. */
	private boolean shouldConfigureButtonBeActive(RealmsServer server) {
		return isSelfOwnedServer(server) && server.state != RealmsServer.State.UNINITIALIZED;
	}

	/** Кнопка «Покинуть» активна только для чужих серверов (не владелец). */
	private boolean shouldLeaveButtonBeActive(RealmsServer server) {
		return !isSelfOwnedServer(server);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.periodicRunnersManager != null) {
			this.periodicRunnersManager.runAll();
		}
	}

	public static void resetPendingInvitesCount() {
		MinecraftClient.getInstance().getRealmsPeriodicCheckers().pendingInvitesCount.reset();
	}

	public static void resetServerList() {
		MinecraftClient.getInstance().getRealmsPeriodicCheckers().serverList.reset();
	}

	/**
	 * Сбрасывает все периодические проверки Realms, чтобы они немедленно
	 * выполнились заново при следующем тике (используется при переключении режима снапшота).
	 */
	private void resetPeriodicCheckers() {
		for (PeriodicRunnerFactory.PeriodicRunner<?> periodicRunner : this.client
				.getRealmsPeriodicCheckers()
				.getCheckers()) {
			periodicRunner.reset();
		}
	}

	/**
	 * Регистрирует все периодические задачи Realms: обновление списка серверов,
	 * уведомлений, приглашений, новостей и онлайн-игроков. Пинг регионов запускается
	 * однократно при первом обнаружении собственного активного сервера.
	 */
	private PeriodicRunnerFactory.RunnersManager createPeriodicRunnersManager(RealmsPeriodicCheckers periodicCheckers) {
		PeriodicRunnerFactory.RunnersManager runnersManager = periodicCheckers.runnerFactory.create();
		runnersManager.add(
				periodicCheckers.serverList, availableServers -> {
					this.serverFilterer.filterAndSort(availableServers.serverList());
					this.availableSnapshotServers = availableServers.availableSnapshotServers();
					this.refresh();
					boolean hasOwnedServer = false;

					for (RealmsServer realmsServer : this.serverFilterer) {
						if (this.isOwnedNotExpired(realmsServer)) {
							hasOwnedServer = true;
						}
					}

					if (!regionsPinged && hasOwnedServer) {
						regionsPinged = true;
						this.pingRegions();
					}
				}
		);
		request(
				RealmsClient::listNotifications, notifications -> {
					this.notifications.clear();
					this.notifications.addAll(notifications);

					for (RealmsNotification realmsNotification : notifications) {
						if (realmsNotification instanceof RealmsNotification.InfoPopup infoPopup) {
							PopupScreen popupScreen = infoPopup.createScreen(this, this::dismissNotification);
							if (popupScreen != null) {
								this.client.setScreen(popupScreen);
								this.markAsSeen(List.of(realmsNotification));
								break;
							}
						}
					}

					if (!this.notifications.isEmpty() && this.loadStatus != RealmsMainScreen.LoadStatus.LOADING) {
						this.refresh();
					}
				}
		);
		runnersManager.add(
				periodicCheckers.pendingInvitesCount, pendingInvitesCount -> {
					this.inviteButton.setNotificationCount(pendingInvitesCount);
					this.inviteButton.setTooltip(
							pendingInvitesCount == 0 ? Tooltip.of(NO_PENDING_TOOLTIP) : Tooltip.of(PENDING_TOOLTIP));
					if (pendingInvitesCount > 0 && this.rateLimiter.tryAcquire(1)) {
						this.client
								.getNarratorManager()
								.narrateSystemImmediately(Text.translatable(
										"mco.configure.world.invite.narration",
										pendingInvitesCount
								));
					}
				}
		);
		runnersManager.add(periodicCheckers.trialAvailability, trialAvailable -> this.trialAvailable = trialAvailable);
		runnersManager.add(periodicCheckers.onlinePlayers, onlinePlayers -> this.onlinePlayers = onlinePlayers);
		runnersManager.add(
				periodicCheckers.news, news -> {
					periodicCheckers.newsUpdater.updateNews(news);
					this.newsLink = periodicCheckers.newsUpdater.getNewsLink();
					this.newsButton.setNotificationCount(
							periodicCheckers.newsUpdater.hasUnreadNews() ? Integer.MAX_VALUE : 0);
				}
		);
		return runnersManager;
	}

	/**
	 * Отправляет на сервер Realms запрос о прочтении уведомлений, которые ещё не были
	 * помечены как просмотренные ни локально, ни на сервере.
	 */
	void markAsSeen(Collection<RealmsNotification> notifications) {
		List<UUID> unseenIds = new ArrayList<>(notifications.size());

		for (RealmsNotification realmsNotification : notifications) {
			if (!realmsNotification.isSeen() && !this.seenNotifications.contains(realmsNotification.getUuid())) {
				unseenIds.add(realmsNotification.getUuid());
			}
		}

		if (!unseenIds.isEmpty()) {
			request(
					client -> {
						client.markNotificationsAsSeen(unseenIds);
						return null;
					}, result -> this.seenNotifications.addAll(unseenIds)
			);
		}
	}

	/**
	 * Выполняет асинхронный запрос к Realms API в фоновом потоке и передаёт результат
	 * в {@code resultConsumer} на главном потоке. Ошибки логируются без пробрасывания.
	 */
	private static <T> void request(RealmsMainScreen.Request<T> request, Consumer<T> resultConsumer) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		CompletableFuture.<T>supplyAsync(() -> {
			try {
				return request.request(RealmsClient.createRealmsClient(minecraftClient));
			}
			catch (RealmsServiceException error) {
				throw new RuntimeException(error);
			}
		}).thenAcceptAsync(resultConsumer, minecraftClient).exceptionally(throwable -> {
			LOGGER.error("Failed to execute call to Realms Service", throwable);
			return null;
		});
	}

	/**
	 * Перестраивает список серверов, обновляет статус загрузки и состояние кнопок.
	 */
	private void refresh() {
		this.realmSelectionList.refresh(this);
		this.updateLoadStatus();
		this.refreshButtons();
	}

	/**
	 * Запускает фоновый поток, который пингует все регионы Realms и отправляет
	 * результаты на сервер для оптимального выбора региона.
	 */
	private void pingRegions() {
		new Thread(() -> {
			List<RegionPingResult> list = Ping.pingAllRegions();
			RealmsClient realmsClient = RealmsClient.create();
			PingResult pingResult = new PingResult(list, this.getOwnedNonExpiredWorldIds());

			try {
				realmsClient.sendPingResults(pingResult);
			}
			catch (Throwable error) {
				LOGGER.warn("Could not send ping result to Realms: ", error);
			}
		}).start();
	}

	/**
	 * Возвращает список ID собственных активных (не истёкших) серверов Realms.
	 * Используется при отправке результатов пинга регионов.
	 */
	private List<Long> getOwnedNonExpiredWorldIds() {
		List<Long> worldIds = Lists.newArrayList();

		for (RealmsServer realmsServer : this.serverFilterer) {
			if (this.isOwnedNotExpired(realmsServer)) {
				worldIds.add(realmsServer.id);
			}
		}

		return worldIds;
	}

	/**
	 * Открывает экран подтверждения перехода по ссылке продления подписки Realms.
	 * URL формируется с учётом UUID игрока и типа подписки (пробная/обычная).
	 */
	private void onRenew(@Nullable RealmsServer realmsServer) {
		if (realmsServer != null) {
			String renewUrl = Urls.getExtendJavaRealmsUrl(
					realmsServer.remoteSubscriptionId,
					this.client.getSession().getUuidOrNull(),
					realmsServer.expiredTrial
			);
			this.client.setScreen(new ConfirmLinkScreen(
					confirmed -> {
						if (confirmed) {
							Util.getOperatingSystem().open(renewUrl);
						}
						else {
							this.client.setScreen(this);
						}
					}, renewUrl, true
			));
		}
	}

	/**
	 * Открывает экран настройки сервера, если текущий игрок является его владельцем.
	 */
	private void configureClicked(@Nullable RealmsServer serverData) {
		if (serverData != null && this.client.uuidEquals(serverData.ownerUUID)) {
			this.client.setScreen(new RealmsConfigureWorldScreen(this, serverData.id));
		}
	}

	/**
	 * Показывает диалог подтверждения выхода с чужого сервера Realms.
	 */
	private void leaveClicked(@Nullable RealmsServer selectedServer) {
		if (selectedServer != null && !this.client.uuidEquals(selectedServer.ownerUUID)) {
			Text text = Text.translatable("mco.configure.world.leave.question.line1");
			this.client.setScreen(RealmsPopups.createInfoPopup(this, text, popup -> this.leaveServer(selectedServer)));
		}
	}

	/**
	 * Возвращает выбранный сервер из списка, или {@code null}, если выбрана не запись сервера.
	 */
	private @Nullable RealmsServer getSelectedServer() {
		return this.realmSelectionList.getSelectedOrNull() instanceof RealmsMainScreen.RealmSelectionListEntry realmSelectionListEntry
		       ? realmSelectionListEntry.getRealmsServer()
		       : null;
	}

	/**
	 * Выполняет выход с сервера в фоновом потоке: отзывает собственное приглашение
	 * и сбрасывает список серверов. При ошибке показывает экран с описанием проблемы.
	 */
	private void leaveServer(RealmsServer server) {
		(new Thread("Realms-leave-server") {
			@Override
			public void run() {
				try {
					RealmsClient realmsClient = RealmsClient.create();
					realmsClient.uninviteMyselfFrom(server.id);
					RealmsMainScreen.this.client.execute(RealmsMainScreen::resetServerList);
				}
				catch (RealmsServiceException error) {
					RealmsMainScreen.LOGGER.error("Couldn't configure world", error);
					RealmsMainScreen.this.client.execute(() -> RealmsMainScreen.this.client.setScreen(new RealmsGenericErrorScreen(
							error,
							RealmsMainScreen.this
					)));
				}
			}
		}
		).start();
		this.client.setScreen(this);
	}

	/**
	 * Асинхронно отправляет запрос на скрытие уведомления и удаляет его из локального списка.
	 */
	void dismissNotification(UUID notification) {
		request(
				client -> {
					client.dismissNotifications(List.of(notification));
					return null;
				}, void_ -> {
					this.notifications.removeIf(notificationId -> notificationId.isDismissable() && notification.equals(
							notificationId.getUuid()));
					this.refresh();
				}
		);
	}

	public void removeSelection() {
		this.realmSelectionList.setSelected(null);
		resetServerList();
	}

	@Override
	public Text getNarratedTitle() {
		return (Text) (switch (this.loadStatus) {
			case LOADING -> ScreenTexts.joinSentences(super.getNarratedTitle(), LOADING_TEXT);
			case NO_REALMS -> ScreenTexts.joinSentences(super.getNarratedTitle(), NO_REALMS_TEXT);
			case LIST -> super.getNarratedTitle();
		}
		);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		if (isSnapshotRealmsEligible()) {
			context.drawTextWithShadow(
					this.textRenderer,
					"Minecraft " + SharedConstants.getGameVersion().name(),
					2,
					this.height - 10,
					-1
			);
		}

		if (this.trialAvailable && this.purchaseButton.active) {
			BuyRealmsScreen.drawTrialAvailableTexture(context, this.purchaseButton);
		}

		switch (RealmsClient.ENVIRONMENT) {
			case STAGE:
				this.drawEnvironmentText(context, "STAGE!", -256);
				break;
			case LOCAL:
				this.drawEnvironmentText(context, "LOCAL!", -8388737);
		}
	}

	private void showBuyRealmsScreen() {
		this.client.setScreen(new BuyRealmsScreen(this, this.trialAvailable));
	}

	/**
	 * Запускает подключение к серверу Realms без принудительной подготовки.
	 */
	public static void play(@Nullable RealmsServer serverData, Screen parent) {
		play(serverData, parent, false);
	}

	/**
	 * Запускает подключение к серверу Realms с учётом совместимости версий.
	 * В зависимости от {@link RealmsServer#compatibility} показывает соответствующий
	 * экран предупреждения или сразу запускает задачу подключения.
	 *
	 * @param needsPreparation если {@code true}, пропускает проверку совместимости снапшота
	 */
	public static void play(@Nullable RealmsServer server, Screen parent, boolean needsPreparation) {
		if (server != null) {
			if (!isSnapshotRealmsEligible() || needsPreparation || server.isMinigame()) {
				MinecraftClient
						.getInstance()
						.setScreen(new RealmsLongRunningMcoTaskScreen(
								parent,
								new RealmsPrepareConnectionTask(parent, server)
						));
				return;
			}

			switch (server.compatibility) {
				case COMPATIBLE:
					MinecraftClient
							.getInstance()
							.setScreen(new RealmsLongRunningMcoTaskScreen(
									parent,
									new RealmsPrepareConnectionTask(parent, server)
							));
					break;
				case UNVERIFIABLE:
					showCompatibilityScreen(
							server,
							parent,
							Text.translatable("mco.compatibility.unverifiable.title").withColor(-171),
							Text.translatable("mco.compatibility.unverifiable.message"),
							ScreenTexts.CONTINUE
					);
					break;
				case NEEDS_DOWNGRADE:
					showCompatibilityScreen(
							server,
							parent,
							Text.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
							Text.translatable(
									"mco.compatibility.downgrade.description",
									Text.literal(server.activeVersion).withColor(-171),
									Text.literal(SharedConstants.getGameVersion().name()).withColor(-171)
							),
							Text.translatable("mco.compatibility.downgrade")
					);
					break;
				case NEEDS_UPGRADE:
					showNeedsUpgradeScreen(server, parent);
					break;
				case INCOMPATIBLE:
					MinecraftClient.getInstance()
					               .setScreen(
							               new PopupScreen.Builder(parent, INCOMPATIBLE_POPUP_TITLE)
									               .message(
											               Text.translatable(
													               "mco.compatibility.incompatible.series.popup.message",
													               Text.literal(server.activeVersion).withColor(-171),
													               Text
															               .literal(SharedConstants
																	               .getGameVersion()
																	               .name())
															               .withColor(-171)
											               )
									               )
									               .button(ScreenTexts.BACK, PopupScreen::close)
									               .build()
					               );
					break;
				case RELEASE_TYPE_INCOMPATIBLE:
					MinecraftClient.getInstance()
					               .setScreen(
							               new PopupScreen.Builder(parent, INCOMPATIBLE_POPUP_TITLE)
									               .message(INCOMPATIBLE_RELEASE_TYPE_MESSAGE)
									               .button(ScreenTexts.BACK, PopupScreen::close)
									               .build()
					               );
			}
		}
	}

	/**
	 * Показывает всплывающий экран предупреждения о несовместимости версий с кнопками
	 * подтверждения (запускает подключение) и отмены.
	 */
	private static void showCompatibilityScreen(
			RealmsServer server,
			Screen parent,
			Text title,
			Text description,
			Text confirmText
	) {
		MinecraftClient.getInstance().setScreen(new PopupScreen.Builder(parent, title).message(description).button(
				confirmText, popup -> {
					MinecraftClient
							.getInstance()
							.setScreen(new RealmsLongRunningMcoTaskScreen(
									parent,
									new RealmsPrepareConnectionTask(parent, server)
							));
					resetServerList();
				}
		).button(ScreenTexts.CANCEL, PopupScreen::close).build());
	}

	/**
	 * Показывает экран предупреждения о необходимости обновить сервер Realms.
	 * Текст описания различается для владельца и участника сервера.
	 */
	private static void showNeedsUpgradeScreen(RealmsServer serverData, Screen parent) {
		Text title = Text.translatable("mco.compatibility.upgrade.title").withColor(-171);
		Text upgradeText = Text.translatable("mco.compatibility.upgrade");
		Text serverVersion = Text.literal(serverData.activeVersion).withColor(-171);
		Text clientVersion = Text.literal(SharedConstants.getGameVersion().name()).withColor(-171);
		Text description = isSelfOwnedServer(serverData)
				? Text.translatable("mco.compatibility.upgrade.description", serverVersion, clientVersion)
				: Text.translatable("mco.compatibility.upgrade.friend.description", serverVersion, clientVersion);
		showCompatibilityScreen(serverData, parent, title, description, upgradeText);
	}

	/**
	 * Возвращает текст версии сервера: серый для совместимой, красный для несовместимой.
	 */
	public static Text getVersionText(String version, boolean compatible) {
		return getVersionText(version, compatible ? -8355712 : -2142128);
	}

	/**
	 * Возвращает текст версии сервера с заданным цветом, или {@link ScreenTexts#EMPTY} если версия пустая.
	 */
	public static Text getVersionText(String version, int color) {
		return (Text) (StringUtils.isBlank(version) ? ScreenTexts.EMPTY : Text.literal(version).withColor(color));
	}

	/**
	 * Возвращает локализованное название режима игры. Для хардкора возвращает
	 * «Хардкор» красным цветом вне зависимости от {@code id}.
	 */
	public static Text getGameModeText(int id, boolean hardcore) {
		return (Text) (hardcore ? Text.translatable("gameMode.hardcore").withColor(-65536)
		                        : GameMode.byIndex(id).getTranslatableName()
		);
	}

	/** Проверяет, является ли текущий игрок владельцем данного сервера Realms. */
	static boolean isSelfOwnedServer(RealmsServer server) {
		return MinecraftClient.getInstance().uuidEquals(server.ownerUUID);
	}

	/** Проверяет, что сервер принадлежит текущему игроку и его подписка не истекла. */
	private boolean isOwnedNotExpired(RealmsServer serverData) {
		return isSelfOwnedServer(serverData) && !serverData.expired;
	}

	/**
	 * Рисует диагональную надпись среды (STAGE/LOCAL) поверх экрана для визуальной
	 * идентификации нерелизного окружения Realms.
	 */
	private void drawEnvironmentText(DrawContext context, String text, int color) {
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(this.width / 2 - 25, 20.0F);
		context.getMatrices().rotate((float) (-Math.PI / 9));
		context.getMatrices().scale(1.5F, 1.5F);
		context.drawTextWithShadow(this.textRenderer, text, 0, 0, color);
		context.getMatrices().popMatrix();
	}

	@Environment(EnvType.CLIENT)
	static class CrossButton extends TexturedButtonWidget {

		private static final ButtonTextures TEXTURES = new ButtonTextures(
				Identifier.ofVanilla("widget/cross_button"), Identifier.ofVanilla("widget/cross_button_highlighted")
		);

		protected CrossButton(ButtonWidget.PressAction onPress, net.minecraft.text.Text tooltip) {
			super(0, 0, 14, 14, TEXTURES, onPress);
			this.setTooltip(Tooltip.of(tooltip));
		}
	}

	@Environment(EnvType.CLIENT)
	abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<RealmsMainScreen.Entry> {

		protected static final int STATUS_ICON_MARGIN_RIGHT = 10;
		private static final int PLAYER_HEAD_SIZE = 28;
		protected static final int STATUS_ICON_OFFSET_X = 7;
		protected static final int STATUS_ICON_OFFSET_Y = 2;

		/**
		 * Рисует иконку статуса сервера (истёк, закрыт, скоро истечёт, открыт) с тултипом.
		 * Тултип показывается только при наведении мыши на иконку.
		 */
		protected void renderStatusIcon(
				RealmsServer server,
				DrawContext context,
				int x,
				int y,
				int mouseX,
				int mouseY
		) {
			int iconX = x - STATUS_ICON_MARGIN_RIGHT - 7;
			int iconY = y + 2;
			if (server.expired) {
				this.drawTextureWithTooltip(
						context,
						iconX,
						iconY,
						mouseX,
						mouseY,
						RealmsMainScreen.EXPIRED_STATUS_TEXTURE,
						() -> RealmsMainScreen.EXPIRED_TEXT
				);
			}
			else if (server.state == RealmsServer.State.CLOSED) {
				this.drawTextureWithTooltip(
						context,
						iconX,
						iconY,
						mouseX,
						mouseY,
						RealmsMainScreen.CLOSED_STATUS_TEXTURE,
						() -> RealmsMainScreen.CLOSED_TEXT
				);
			}
			else if (RealmsMainScreen.isSelfOwnedServer(server) && server.daysLeft < 7) {
				this.drawTextureWithTooltip(
						context,
						iconX,
						iconY,
						mouseX,
						mouseY,
						RealmsMainScreen.EXPIRES_SOON_STATUS_TEXTURE,
						() -> {
							if (server.daysLeft <= 0) {
								return RealmsMainScreen.EXPIRES_SOON_TEXT;
							}
							else {
								return (Text) (server.daysLeft == 1
								               ? RealmsMainScreen.EXPIRES_IN_A_DAY_TEXT
								               : Text.translatable("mco.selectServer.expires.days", server.daysLeft)
								);
							}
						}
				);
			}
			else if (server.state == RealmsServer.State.OPEN) {
				this.drawTextureWithTooltip(
						context,
						iconX,
						iconY,
						mouseX,
						mouseY,
						RealmsMainScreen.OPEN_STATUS_TEXTURE,
						() -> RealmsMainScreen.OPEN_TEXT
				);
			}
		}

		/**
		 * Рисует текстуру и показывает тултип, если курсор находится над ней.
		 * Тултип вычисляется лениво через {@code Supplier} для экономии ресурсов.
		 */
		private void drawTextureWithTooltip(
				DrawContext context,
				int x,
				int y,
				int mouseX,
				int mouseY,
				Identifier texture,
				Supplier<Text> tooltip
		) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, STATUS_ICON_MARGIN_RIGHT, PLAYER_HEAD_SIZE);
			if (RealmsMainScreen.this.realmSelectionList.isMouseOver(mouseX, mouseY) && mouseX >= x && mouseX <= x + STATUS_ICON_MARGIN_RIGHT
					&& mouseY >= y && mouseY <= y + PLAYER_HEAD_SIZE) {
				context.drawTooltip(tooltip.get(), mouseX, mouseY);
			}
		}

		/**
		 * Рисует название сервера и его версию. Версия выравнивается по правому краю;
		 * для минигр версия не отображается.
		 */
		protected void drawServerNameAndVersion(
				DrawContext context,
				int y,
				int x,
				int width,
				int color,
				RealmsServer server
		) {
			int nameX = this.getNameX(x);
			int nameY = this.getNameY(y);
			Text versionText = RealmsMainScreen.getVersionText(server.activeVersion, server.isCompatible());
			int versionRight = this.getVersionRight(x, width, versionText);
			this.drawTrimmedText(context, server.getName(), nameX, nameY, versionRight, color);
			if (versionText != ScreenTexts.EMPTY && !server.isMinigame()) {
				context.drawTextWithShadow(RealmsMainScreen.this.textRenderer, versionText, versionRight, nameY, -8355712);
			}
		}

		/**
		 * Рисует строку описания сервера. Для минигр показывает название минигры;
		 * для обычных серверов — режим игры и обрезанное описание.
		 */
		protected void drawDescription(DrawContext context, int y, int x, int width, RealmsServer server) {
			int nameX = this.getNameX(x);
			int nameY = this.getNameY(y);
			int descriptionY = this.getDescriptionY(nameY);
			String minigameName = server.getMinigameName();
			boolean isMinigame = server.isMinigame();
			if (isMinigame && minigameName != null) {
				Text minigameText = Text.literal(minigameName).formatted(Formatting.GRAY);
				context.drawTextWithShadow(
						RealmsMainScreen.this.textRenderer,
						Text.translatable("mco.selectServer.minigameName", minigameText).withColor(-171),
						nameX,
						descriptionY,
						-1
				);
			}
			else {
				int gameModeRight = this.drawGameMode(server, context, x, width, nameY);
				this.drawTrimmedText(context, server.getDescription(), nameX, descriptionY, gameModeRight, -8355712);
			}
		}

		/**
		 * Рисует третью строку записи: имя владельца для чужих серверов,
		 * или текст об истечении подписки для собственных истёкших серверов.
		 */
		protected void drawOwnerOrExpiredText(DrawContext context, int y, int x, RealmsServer server) {
			int nameX = this.getNameX(x);
			int nameY = this.getNameY(y);
			int statusY = this.getStatusY(nameY);
			if (!RealmsMainScreen.isSelfOwnedServer(server)) {
				context.drawTextWithShadow(
						RealmsMainScreen.this.textRenderer,
						server.owner,
						nameX,
						statusY,
						-8355712
				);
			}
			else if (server.expired) {
				Text expiredText = server.expiredTrial
						? RealmsMainScreen.EXPIRED_TRIAL_TEXT
						: RealmsMainScreen.EXPIRED_LIST_TEXT;
				context.drawTextWithShadow(RealmsMainScreen.this.textRenderer, expiredText, nameX, statusY, -2142128);
			}
		}

		/**
		 * Рисует строку, обрезая её с добавлением «...» если она не помещается
		 * в диапазон [{@code left}, {@code right}].
		 */
		protected void drawTrimmedText(
				DrawContext context,
				@Nullable String string,
				int left,
				int y,
				int right,
				int color
		) {
			if (string != null) {
				int maxWidth = right - left;
				if (RealmsMainScreen.this.textRenderer.getWidth(string) > maxWidth) {
					String trimmed = RealmsMainScreen.this.textRenderer.trimToWidth(
							string,
							maxWidth - RealmsMainScreen.this.textRenderer.getWidth("... ")
					);
					context.drawTextWithShadow(RealmsMainScreen.this.textRenderer, trimmed + "...", left, y, color);
				}
				else {
					context.drawTextWithShadow(RealmsMainScreen.this.textRenderer, string, left, y, color);
				}
			}
		}

		protected int getVersionRight(int x, int width, Text version) {
			return x + width - RealmsMainScreen.this.textRenderer.getWidth(version) - NEW_REALM_ICON_HEIGHT;
		}

		protected int getGameModeRight(int x, int width, Text gameMode) {
			return x + width - RealmsMainScreen.this.textRenderer.getWidth(gameMode) - NEW_REALM_ICON_HEIGHT;
		}

		/**
		 * Рисует название режима игры и иконку хардкора (если применимо) по правому краю.
		 *
		 * @return X-координата левого края нарисованного блока (для выравнивания описания)
		 */
		protected int drawGameMode(RealmsServer server, DrawContext context, int x, int entryWidth, int y) {
			boolean isHardcore = server.hardcore;
			int gameModeId = server.gameMode;
			int gameModeRight = x;
			if (GameMode.isValid(gameModeId)) {
				Text gameModeText = RealmsMainScreen.getGameModeText(gameModeId, isHardcore);
				gameModeRight = this.getGameModeRight(x, entryWidth, gameModeText);
				context.drawTextWithShadow(
						RealmsMainScreen.this.textRenderer,
						gameModeText,
						gameModeRight,
						this.getDescriptionY(y),
						-8355712
				);
			}

			if (isHardcore) {
				gameModeRight -= STATUS_ICON_MARGIN_RIGHT;
				context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						RealmsMainScreen.HARDCORE_ICON_TEXTURE,
						gameModeRight,
						this.getDescriptionY(y),
						8,
						8
				);
			}

			return gameModeRight;
		}

		protected int getNameY(int y) {
			return y + 1;
		}

		protected int getTextHeight() {
			return 2 + 9;
		}

		protected int getNameX(int x) {
			return x + 36 + 2;
		}

		protected int getDescriptionY(int y) {
			return y + this.getTextHeight();
		}

		protected int getStatusY(int y) {
			return y + this.getTextHeight() * 2;
		}
	}

	@Environment(EnvType.CLIENT)
	static enum LoadStatus {
		LOADING,
		NO_REALMS,
		LIST;
	}

	@Environment(EnvType.CLIENT)
	static class NotificationButtonWidget extends TextIconButtonWidget.IconOnly {

		private static final Identifier[] TEXTURES = new Identifier[]{
				Identifier.ofVanilla("notification/1"),
				Identifier.ofVanilla("notification/2"),
				Identifier.ofVanilla("notification/3"),
				Identifier.ofVanilla("notification/4"),
				Identifier.ofVanilla("notification/5"),
				Identifier.ofVanilla("notification/more")
		};
		private static final int UNLIMITED_NOTIFICATIONS = Integer.MAX_VALUE;
		private static final int SIZE = 20;
		private static final int TEXTURE_SIZE = 14;
		private int notificationCount;

		public NotificationButtonWidget(
				net.minecraft.text.Text message,
				Identifier texture,
				ButtonWidget.PressAction onPress,
				net.minecraft.text.Text tooltip
		) {
			super(SIZE, SIZE, message, TEXTURE_SIZE, TEXTURE_SIZE, new ButtonTextures(texture), onPress, tooltip, null);
		}

		int getNotificationCount() {
			return this.notificationCount;
		}

		public void setNotificationCount(int notificationCount) {
			this.notificationCount = notificationCount;
		}

		@Override
		public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			super.drawIcon(context, mouseX, mouseY, deltaTicks);
			if (this.active && this.notificationCount != 0) {
				this.render(context);
			}
		}

		private void render(DrawContext context) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					TEXTURES[Math.min(this.notificationCount, 6) - 1],
					this.getX() + this.getWidth() - 5,
					this.getY() - 3,
					8,
					8
			);
		}
	}

	@Environment(EnvType.CLIENT)
	class ParentRealmSelectionListEntry extends RealmsMainScreen.Entry {

		private final RealmsServer server;
		private final TooltipState tooltip = new TooltipState();

		public ParentRealmSelectionListEntry(final RealmsServer server) {
			this.server = server;
			if (!server.expired) {
				this.tooltip.setTooltip(Tooltip.of(Text.translatable("mco.snapshot.parent.tooltip")));
			}
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			this.renderStatusIcon(this.server, context, this.getContentRightEnd(), this.getContentY(), mouseX, mouseY);
			RealmsUtil.drawPlayerHead(context, this.getContentX(), this.getContentY(), 32, this.server.ownerUUID);
			this.drawServerNameAndVersion(
					context,
					this.getContentY(),
					this.getContentX(),
					this.getContentWidth(),
					-8355712,
					this.server
			);
			this.drawDescription(context, this.getContentY(), this.getContentX(), this.getContentWidth(), this.server);
			this.drawOwnerOrExpiredText(context, this.getContentY(), this.getContentX(), this.server);
			this.tooltip
					.render(
							context,
							mouseX,
							mouseY,
							hovered,
							this.isFocused(),
							new ScreenRect(
									this.getContentX(),
									this.getContentY(),
									this.getContentWidth(),
									this.getContentHeight()
							)
					);
		}

		@Override
		public Text getNarration() {
			return Text.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
		}
	}

	@Environment(EnvType.CLIENT)
	class RealmSelectionList extends AlwaysSelectedEntryListWidget<RealmsMainScreen.Entry> {

		public RealmSelectionList() {
			super(MinecraftClient.getInstance(), RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
		}

		public void setSelected(RealmsMainScreen.@Nullable Entry entry) {
			super.setSelected(entry);
			RealmsMainScreen.this.refreshButtons();
		}

		@Override
		public int getRowWidth() {
			return 300;
		}

		/**
		 * Перестраивает список: сначала добавляет уведомления-ссылки (VisitUrl),
		 * затем записи серверов. Восстанавливает ранее выбранный элемент.
		 */
		void refresh(RealmsMainScreen mainScreen) {
			RealmsMainScreen.Entry entry = this.getSelectedOrNull();
			this.clearEntries();

			for (RealmsNotification realmsNotification : RealmsMainScreen.this.notifications) {
				if (realmsNotification instanceof RealmsNotification.VisitUrl visitUrl) {
					this.addVisitEntries(visitUrl, mainScreen, entry);
					RealmsMainScreen.this.markAsSeen(List.of(realmsNotification));
					break;
				}
			}

			this.addServerEntries(entry);
		}

		/**
		 * Добавляет запись-уведомление с кнопкой перехода по URL. Высота записи
		 * вычисляется динамически по количеству строк текста сообщения.
		 */
		private void addVisitEntries(
				RealmsNotification.VisitUrl url,
				RealmsMainScreen mainScreen,
				RealmsMainScreen.@Nullable Entry selectedEntry
		) {
			Text text = url.getDefaultMessage();
			int textHeight = RealmsMainScreen.this.textRenderer.getWrappedLinesHeight(
					text,
					RealmsMainScreen.VisitUrlNotification.getTextWidth(this.getRowWidth())
			);
			RealmsMainScreen.VisitUrlNotification visitUrlNotification =
					RealmsMainScreen.this.new VisitUrlNotification(mainScreen, textHeight, text, url);
			this.addEntry(visitUrlNotification, 38 + textHeight);
			if (selectedEntry instanceof RealmsMainScreen.VisitUrlNotification visitUrlNotification2
					&& visitUrlNotification2.getMessage().equals(text)) {
				this.setSelected((RealmsMainScreen.Entry) visitUrlNotification);
			}
		}

		/**
		 * Добавляет записи серверов: снапшот-серверы идут первыми, затем обычные.
		 * В режиме снапшота нединамические серверы отображаются как ParentRealmSelectionListEntry.
		 */
		private void addServerEntries(RealmsMainScreen.@Nullable Entry selectedEntry) {
			for (RealmsServer realmsServer : RealmsMainScreen.this.availableSnapshotServers) {
				this.addEntry(RealmsMainScreen.this.new SnapshotEntry(realmsServer));
			}

			for (RealmsServer realmsServer : RealmsMainScreen.this.serverFilterer) {
				RealmsMainScreen.Entry entry;
				if (RealmsMainScreen.isSnapshotRealmsEligible() && !realmsServer.isPrerelease()) {
					if (realmsServer.state == RealmsServer.State.UNINITIALIZED) {
						continue;
					}

					entry = RealmsMainScreen.this.new ParentRealmSelectionListEntry(realmsServer);
				}
				else {
					entry = RealmsMainScreen.this.new RealmSelectionListEntry(realmsServer);
				}

				this.addEntry(entry);
				if (selectedEntry instanceof RealmsMainScreen.RealmSelectionListEntry realmSelectionListEntry
						&& realmSelectionListEntry.server.id == realmsServer.id) {
					this.setSelected(entry);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	class RealmSelectionListEntry extends RealmsMainScreen.Entry {

		private static final Text ONLINE_PLAYERS_TEXT = Text.translatable("mco.onlinePlayers");
		private static final int FONT_HEIGHT = 9;
		private static final int PLAYER_ICON_SPACING = 3;
		private static final int TEXT_OFFSET_FROM_ICON = 36;
		final RealmsServer server;
		private final TooltipState tooltip = new TooltipState();

		public RealmSelectionListEntry(final RealmsServer server) {
			this.server = server;
			boolean bl = RealmsMainScreen.isSelfOwnedServer(server);
			if (RealmsMainScreen.isSnapshotRealmsEligible() && bl && server.isPrerelease()) {
				this.tooltip.setTooltip(Tooltip.of(Text.translatable("mco.snapshot.paired", server.parentWorldName)));
			}
			else if (!bl && server.needsDowngrade()) {
				this.tooltip.setTooltip(Tooltip.of(Text.translatable(
						"mco.snapshot.friendsRealm.downgrade",
						server.activeVersion
				)));
			}
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			if (this.server.state == RealmsServer.State.UNINITIALIZED) {
				context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						RealmsMainScreen.NEW_REALM_ICON_TEXTURE,
						this.getContentX() - 5,
						this.getContentMiddleY() - STATUS_ICON_MARGIN_RIGHT,
						NEW_REALM_ICON_WIDTH,
						NEW_REALM_ICON_HEIGHT
				);
				int textY = this.getContentMiddleY() - 9 / 2;
				context.drawTextWithShadow(
						RealmsMainScreen.this.textRenderer,
						RealmsMainScreen.UNINITIALIZED_TEXT,
						this.getContentX() + NEW_REALM_ICON_WIDTH - 2,
						textY,
						-8388737
				);
			}
			else {
				RealmsUtil.drawPlayerHead(context, this.getContentX(), this.getContentY(), 32, this.server.ownerUUID);
				this.drawServerNameAndVersion(
						context,
						this.getContentY(),
						this.getContentX(),
						this.getContentWidth(),
						-1,
						this.server
				);
				this.drawDescription(
						context,
						this.getContentY(),
						this.getContentX(),
						this.getContentWidth(),
						this.server
				);
				this.drawOwnerOrExpiredText(context, this.getContentY(), this.getContentX(), this.server);
				this.renderStatusIcon(
						this.server,
						context,
						this.getContentRightEnd(),
						this.getContentY(),
						mouseX,
						mouseY
				);
				boolean bl = this.drawPlayers(
						context,
						this.getContentY(),
						this.getContentX(),
						this.getContentWidth(),
						this.getContentHeight(),
						mouseX,
						mouseY,
						deltaTicks
				);
				if (!bl) {
					this.tooltip
							.render(
									context,
									mouseX,
									mouseY,
									hovered,
									this.isFocused(),
									new ScreenRect(
											this.getContentX(),
											this.getContentY(),
											this.getContentWidth(),
											this.getContentHeight()
									)
							);
				}
			}
		}

		/**
		 * Рисует иконки голов онлайн-игроков в нижнем правом углу записи.
		 * При наведении мыши показывает тултип с именами игроков.
		 *
		 * @return {@code true} если тултип был показан (чтобы подавить стандартный тултип записи)
		 */
		private boolean drawPlayers(
				DrawContext context,
				int top,
				int left,
				int width,
				int height,
				int mouseX,
				int mouseY,
				float tickProgress
		) {
			List<ProfileComponent> players = RealmsMainScreen.this.onlinePlayers.get(this.server.id);
			int playerCount = players.size();
			if (playerCount > 0) {
				int rightEdge = left + width - 21;
				int bottomY = top + height - 9 - 2;
				int totalWidth = 9 * playerCount + 3 * (playerCount - 1);
				int leftEdge = rightEdge - totalWidth;
				List<PlayerSkinCache.Entry> hoveredPlayers;
				if (mouseX >= leftEdge && mouseX <= rightEdge && mouseY >= bottomY && mouseY <= bottomY + 9) {
					hoveredPlayers = new ArrayList<>(playerCount);
				}
				else {
					hoveredPlayers = null;
				}

				PlayerSkinCache playerSkinCache = RealmsMainScreen.this.client.getPlayerSkinCache();

				for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {
					ProfileComponent profileComponent = players.get(playerIndex);
					PlayerSkinCache.Entry entry = playerSkinCache.get(profileComponent);
					int playerX = leftEdge + 12 * playerIndex;
					PlayerSkinDrawer.draw(context, entry.getTextures(), playerX, bottomY, 9);
					if (hoveredPlayers != null) {
						hoveredPlayers.add(entry);
					}
				}

				if (hoveredPlayers != null) {
					context.drawTooltip(
							RealmsMainScreen.this.textRenderer,
							List.of(ONLINE_PLAYERS_TEXT),
							Optional.of(new ProfilesTooltipComponent.ProfilesData(hoveredPlayers)),
							mouseX,
							mouseY
					);
					return true;
				}
			}

			return false;
		}

		private void play() {
			RealmsMainScreen.this.client
					.getSoundManager()
					.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			RealmsMainScreen.play(this.server, RealmsMainScreen.this);
		}

		private void createRealm() {
			RealmsMainScreen.this.client
					.getSoundManager()
					.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			RealmsCreateRealmScreen
					realmsCreateRealmScreen =
					new RealmsCreateRealmScreen(RealmsMainScreen.this, this.server, this.server.isPrerelease());
			RealmsMainScreen.this.client.setScreen(realmsCreateRealmScreen);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (this.server.state == RealmsServer.State.UNINITIALIZED) {
				this.createRealm();
			}
			else if (this.server.shouldAllowPlay() && doubled && this.isFocused()) {
				this.play();
			}

			return true;
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnterOrSpace()) {
				if (this.server.state == RealmsServer.State.UNINITIALIZED) {
					this.createRealm();
					return true;
				}

				if (this.server.shouldAllowPlay()) {
					this.play();
					return true;
				}
			}

			return super.keyPressed(input);
		}

		@Override
		public Text getNarration() {
			return (Text) (this.server.state == RealmsServer.State.UNINITIALIZED
			               ? RealmsMainScreen.UNINITIALIZED_BUTTON_NARRATION
			               : Text.translatable(
					               "narrator.select",
					               Objects.requireNonNullElse(this.server.name, "unknown server")
			               )
			);
		}

		public RealmsServer getRealmsServer() {
			return this.server;
		}
	}

	@Environment(EnvType.CLIENT)
	interface Request<T> {

		T request(RealmsClient client) throws RealmsServiceException;
	}

	@Environment(EnvType.CLIENT)
	class SnapshotEntry extends RealmsMainScreen.Entry {

		private static final Text START_TEXT = Text.translatable("mco.snapshot.start");
		private static final int SNAPSHOT_TEXT_OFFSET_Y = 5;
		private final TooltipState tooltip = new TooltipState();
		private final RealmsServer server;

		public SnapshotEntry(final RealmsServer server) {
			this.server = server;
			this.tooltip.setTooltip(Tooltip.of(Text.translatable("mco.snapshot.tooltip")));
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			context.drawGuiTexture(
					RenderPipelines.GUI_TEXTURED,
					RealmsMainScreen.NEW_REALM_ICON_TEXTURE,
					this.getContentX() - 5,
					this.getContentMiddleY() - STATUS_ICON_MARGIN_RIGHT,
					NEW_REALM_ICON_WIDTH,
					NEW_REALM_ICON_HEIGHT
			);
			int textY = this.getContentMiddleY() - 9 / 2;
			context.drawTextWithShadow(
					RealmsMainScreen.this.textRenderer,
					START_TEXT,
					this.getContentX() + NEW_REALM_ICON_WIDTH - 2,
					textY - 5,
					-8388737
			);
			context.drawTextWithShadow(
					RealmsMainScreen.this.textRenderer,
					Text.translatable(
							"mco.snapshot.description",
							Objects.requireNonNullElse(this.server.name, "unknown server")
					),
					this.getContentX() + NEW_REALM_ICON_WIDTH - 2,
					textY + 5,
					-8355712
			);
			this.tooltip
					.render(
							context,
							mouseX,
							mouseY,
							hovered,
							this.isFocused(),
							new ScreenRect(
									this.getContentX(),
									this.getContentY(),
									this.getContentWidth(),
									this.getContentHeight()
							)
					);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			this.showPopup();
			return true;
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnterOrSpace()) {
				this.showPopup();
				return false;
			}
			else {
				return super.keyPressed(input);
			}
		}

		/**
		 * Показывает всплывающий диалог с предложением создать снапшот-сервер.
		 */
		private void showPopup() {
			RealmsMainScreen.this.client
					.getSoundManager()
					.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			RealmsMainScreen.this.client
					.setScreen(
							new PopupScreen.Builder(
									RealmsMainScreen.this,
									Text.translatable("mco.snapshot.createSnapshotPopup.title")
							)
									.message(Text.translatable("mco.snapshot.createSnapshotPopup.text"))
									.button(
											Text.translatable("mco.selectServer.create"),
											screen -> RealmsMainScreen.this.client.setScreen(new RealmsCreateRealmScreen(
													RealmsMainScreen.this,
													this.server,
													true
											))
									)
									.button(ScreenTexts.CANCEL, PopupScreen::close)
									.build()
					);
		}

		@Override
		public Text getNarration() {
			return Text.translatable(
					"gui.narrate.button",
					ScreenTexts.joinSentences(
							START_TEXT,
							Text.translatable(
									"mco.snapshot.description",
									Objects.requireNonNullElse(this.server.name, "unknown server")
							)
					)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	class VisitUrlNotification extends RealmsMainScreen.Entry {

		private static final int NOTIFICATION_ICON_WIDTH = 40;
		public static final int NOTIFICATION_ICON_MARGIN = 7;
		public static final int NOTIFICATION_BUTTON_WIDTH = 38;
		private final Text message;
		private final List<ClickableWidget> gridChildren = new ArrayList<>();
		private final RealmsMainScreen.@Nullable CrossButton dismissButton;
		private final MultilineTextWidget textWidget;
		private final GridWidget grid;
		private final SimplePositioningWidget textGrid;
		private final ButtonWidget urlButton;
		private int width = -1;

		public VisitUrlNotification(
				final RealmsMainScreen parent,
				final int lines,
				final Text message,
				final RealmsNotification.VisitUrl url
		) {
			this.message = message;
			this.grid = new GridWidget();
			this.grid.add(
					IconWidget.create(NEW_REALM_ICON_HEIGHT, NEW_REALM_ICON_HEIGHT, RealmsMainScreen.INFO_ICON_TEXTURE),
					0,
					0,
					this.grid.copyPositioner().margin(7, 7, 0, 0)
			);
			this.grid.add(EmptyWidget.ofWidth(NEW_REALM_ICON_WIDTH), 0, 0);
			this.textGrid =
					this.grid.add(new SimplePositioningWidget(0, lines), 0, 1, this.grid.copyPositioner().marginTop(7));
			this.textWidget = this.textGrid
					.add(
							new MultilineTextWidget(message, RealmsMainScreen.this.textRenderer).setCentered(true),
							this.textGrid.copyPositioner().alignHorizontalCenter().alignTop()
					);
			this.grid.add(EmptyWidget.ofWidth(NEW_REALM_ICON_WIDTH), 0, 2);
			if (url.isDismissable()) {
				this.dismissButton = this.grid
						.add(
								new RealmsMainScreen.CrossButton(
										button -> RealmsMainScreen.this.dismissNotification(url.getUuid()),
										Text.translatable("mco.notification.dismiss")
								),
								0,
								2,
								this.grid.copyPositioner().alignRight().margin(0, 7, 7, 0)
						);
			}
			else {
				this.dismissButton = null;
			}

			this.urlButton =
					this.grid.add(
							url.createButton(parent),
							1,
							1,
							this.grid.copyPositioner().alignHorizontalCenter().margin(4)
					);
			this.urlButton.setFocusOverride(() -> this.isFocused());
			this.grid.forEachChild(this.gridChildren::add);
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (this.urlButton.keyPressed(input)) {
				return true;
			}

			if (this.dismissButton != null && this.dismissButton.keyPressed(input)) {
				return true;
			}

			return super.keyPressed(input);
		}

		private void setWidth() {
			int currentWidth = this.getWidth();
			if (this.width != currentWidth) {
				this.updateWidth(currentWidth);
				this.width = currentWidth;
			}
		}

		private void updateWidth(int width) {
			int textWidth = getTextWidth(width);
			this.textGrid.setMinWidth(textWidth);
			this.textWidget.setMaxWidth(textWidth);
			this.grid.refreshPositions();
		}

		/**
		 * Вычисляет доступную ширину для текста уведомления с учётом иконки и кнопок по бокам.
		 */
		public static int getTextWidth(int width) {
			return width - 80;
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			this.grid.setPosition(this.getContentX(), this.getContentY());
			this.setWidth();
			this.gridChildren.forEach(child -> child.render(context, mouseX, mouseY, deltaTicks));
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (this.dismissButton != null && this.dismissButton.mouseClicked(click, doubled)) {
				return true;
			}
			else {
				return this.urlButton.mouseClicked(click, doubled) ? true : super.mouseClicked(click, doubled);
			}
		}

		public Text getMessage() {
			return this.message;
		}

		@Override
		public Text getNarration() {
			return this.getMessage();
		}
	}
}
