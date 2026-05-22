package net.minecraft.client.gui.screen.multiplayer;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.QuickPlay;
import net.minecraft.client.QuickPlayLogger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.*;
import net.minecraft.client.resource.server.ServerResourcePackManager;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.state.LoginStates;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Экран подключения к серверу. Запускает фоновый поток для установки соединения
 * и отображает статус подключения с возможностью отмены.
 */
@Environment(EnvType.CLIENT)
public class ConnectScreen extends Screen {

	private static final AtomicInteger CONNECTOR_THREADS_COUNT = new AtomicInteger(0);
	static final Logger LOGGER = LogUtils.getLogger();
	private static final long NARRATOR_INTERVAL = 2000L;
	private static final int CANCEL_BUTTON_Y_OFFSET = 120 + 12;
	private static final int CANCEL_BUTTON_WIDTH = 200;
	private static final int CANCEL_BUTTON_HEIGHT = 20;
	private static final int STATUS_Y_OFFSET = 50;
	private static final int TEXT_COLOR_WHITE = -1;

	public static final Text ABORTED_TEXT = Text.translatable("connect.aborted");
	public static final Text UNKNOWN_HOST_TEXT = Text.translatable("disconnect.genericReason", Text.translatable("disconnect.unknownHost"));

	volatile @Nullable ClientConnection connection;
	@Nullable ChannelFuture future;
	volatile boolean connectingCancelled;
	final Screen parent;
	private Text status = Text.translatable("connect.connecting");
	private long lastNarrationTime = -1L;
	final Text failureErrorMessage;

	private ConnectScreen(Screen parent, Text failureErrorMessage) {
		super(NarratorManager.EMPTY);
		this.parent = parent;
		this.failureErrorMessage = failureErrorMessage;
	}

	/**
	 * Инициирует подключение к серверу, создавая экран подключения и запуская фоновый поток.
	 * Если уже идёт подключение — логирует ошибку и прерывает выполнение.
	 *
	 * @param screen экран, на который вернуться при отмене или ошибке
	 * @param client клиент Minecraft
	 * @param address адрес сервера
	 * @param info информация о сервере
	 * @param quickPlay {@code true} если подключение через QuickPlay
	 * @param cookieStorage хранилище куки при трансфере, или {@code null}
	 */
	public static void connect(
		Screen screen,
		MinecraftClient client,
		ServerAddress address,
		ServerInfo info,
		boolean quickPlay,
		@Nullable CookieStorage cookieStorage
	) {
		if (client.currentScreen instanceof ConnectScreen) {
			LOGGER.error("Attempt to connect while already connecting");
			return;
		}

		Text failureTitle;
		if (cookieStorage != null) {
			failureTitle = ScreenTexts.CONNECT_FAILED_TRANSFER;
		}
		else if (quickPlay) {
			failureTitle = QuickPlay.ERROR_TITLE;
		}
		else {
			failureTitle = ScreenTexts.CONNECT_FAILED;
		}

		ConnectScreen connectScreen = new ConnectScreen(screen, failureTitle);
		if (cookieStorage != null) {
			connectScreen.setStatus(Text.translatable("connect.transferring"));
		}

		client.disconnectWithProgress(false);
		client.loadBlockList();
		client.ensureAbuseReportContext(ReporterEnvironment.ofThirdPartyServer(info.address));
		client.getQuickPlayLogger().setWorld(QuickPlayLogger.WorldType.MULTIPLAYER, info.address, info.name);
		client.setScreen(connectScreen);
		connectScreen.connect(client, address, info, cookieStorage);
	}

	private void connect(
			MinecraftClient client,
			ServerAddress address,
			ServerInfo info,
			@Nullable CookieStorage cookieStorage
	) {
		LOGGER.info("Connecting to {}, {}", address.getAddress(), address.getPort());
		Thread thread = new Thread("Server Connector #" + CONNECTOR_THREADS_COUNT.incrementAndGet()) {
			@Override
			public void run() {
				InetSocketAddress inetSocketAddress = null;

				try {
					if (ConnectScreen.this.connectingCancelled) {
						return;
					}

					Optional<InetSocketAddress>
							optional =
							AllowedAddressResolver.DEFAULT.resolve(address).map(Address::getInetSocketAddress);
					if (ConnectScreen.this.connectingCancelled) {
						return;
					}

					if (optional.isEmpty()) {
						client.execute(
								() -> client.setScreen(
										new DisconnectedScreen(
												ConnectScreen.this.parent,
												ConnectScreen.this.failureErrorMessage,
												ConnectScreen.UNKNOWN_HOST_TEXT
										)
								)
						);
						return;
					}

					inetSocketAddress = optional.get();
					ClientConnection clientConnection;
					synchronized (ConnectScreen.this) {
						if (ConnectScreen.this.connectingCancelled) {
							return;
						}

						clientConnection = new ClientConnection(NetworkSide.CLIENTBOUND);
						clientConnection.resetPacketSizeLog(client.getDebugHud().getPacketSizeLog());
						ConnectScreen.this.future = ClientConnection.connect(
								inetSocketAddress,
								NetworkingBackend.remote(client.options.shouldUseNativeTransport()),
								clientConnection
						);
					}

					ConnectScreen.this.future.syncUninterruptibly();
					synchronized (ConnectScreen.this) {
						if (ConnectScreen.this.connectingCancelled) {
							clientConnection.disconnect(ConnectScreen.ABORTED_TEXT);
							return;
						}

						ConnectScreen.this.connection = clientConnection;
						client
								.getServerResourcePackProvider()
								.init(clientConnection, toAcceptanceStatus(info.getResourcePackPolicy()));
					}

					ConnectScreen.this.connection
							.connect(
									inetSocketAddress.getHostName(),
									inetSocketAddress.getPort(),
									LoginStates.C2S,
									LoginStates.S2C,
									new ClientLoginNetworkHandler(
											ConnectScreen.this.connection,
											client,
											info,
											ConnectScreen.this.parent,
											false,
											null,
											ConnectScreen.this::setStatus,
											new ClientChunkLoadProgress(),
											cookieStorage
									),
									cookieStorage != null
							);
					ConnectScreen.this.connection.send(new LoginHelloC2SPacket(
							client.getSession().getUsername(),
							client.getSession().getUuidOrNull()
					));
				}
				catch (Exception exception) {
					if (ConnectScreen.this.connectingCancelled) {
						return;
					}
	
					Exception rootCause = exception.getCause() instanceof Exception cause ? cause : exception;
					ConnectScreen.LOGGER.error("Couldn't connect to server", exception);
					String errorMessage = inetSocketAddress == null
						? rootCause.getMessage()
						: rootCause.getMessage()
							.replaceAll(inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort(), "")
							.replaceAll(inetSocketAddress.toString(), "");
					client.execute(
						() -> client.setScreen(
							new DisconnectedScreen(
								ConnectScreen.this.parent,
								ConnectScreen.this.failureErrorMessage,
								Text.translatable("disconnect.genericReason", errorMessage)
							)
						)
					);
				}
			}
	
			private static ServerResourcePackManager.AcceptanceStatus toAcceptanceStatus(ServerInfo.ResourcePackPolicy policy) {
				return switch (policy) {
					case ENABLED -> ServerResourcePackManager.AcceptanceStatus.ALLOWED;
					case DISABLED -> ServerResourcePackManager.AcceptanceStatus.DECLINED;
					case PROMPT -> ServerResourcePackManager.AcceptanceStatus.PENDING;
				};
			}
		};
		thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
		thread.start();
	}
	
	private void setStatus(Text status) {
		this.status = status;
	}
	
	@Override
	public void tick() {
		if (connection == null) {
			return;
		}
	
		if (connection.isOpen()) {
			connection.tick();
		}
		else {
			connection.handleDisconnection();
		}
	}
	
	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
	
	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(
			ScreenTexts.CANCEL, button -> {
				synchronized (this) {
					connectingCancelled = true;
					if (future != null) {
						future.cancel(true);
						future = null;
					}
	
					if (connection != null) {
						connection.disconnect(ABORTED_TEXT);
					}
				}
	
				client.setScreen(parent);
			}
		).dimensions(width / 2 - 100, height / 4 + CANCEL_BUTTON_Y_OFFSET, CANCEL_BUTTON_WIDTH, CANCEL_BUTTON_HEIGHT).build());
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		long currentTime = Util.getMeasuringTimeMs();
		if (currentTime - lastNarrationTime > NARRATOR_INTERVAL) {
			lastNarrationTime = currentTime;
			client.getNarratorManager().narrateSystemImmediately(Text.translatable("narrator.joining"));
		}
	
		context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height / 2 - STATUS_Y_OFFSET, TEXT_COLOR_WHITE);
	}
	}
