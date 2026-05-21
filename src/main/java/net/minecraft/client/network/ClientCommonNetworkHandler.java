package net.minecraft.client.network;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.dialog.DialogNetworkAccess;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.client.gui.screen.dialog.DialogScreens;
import net.minecraft.client.gui.screen.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ServerPacketListener;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.UnknownCustomPayload;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.s2c.common.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerLinks;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.ReportType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Базовый обработчик общих пакетов клиента (keep-alive, ресурс-паки, куки, диалоги, трансфер).
 * Является родительским классом для {@link ClientConfigurationNetworkHandler}
 * и {@link ClientPlayNetworkHandler}.
 */
@Environment(EnvType.CLIENT)
public abstract class ClientCommonNetworkHandler implements ClientCommonPacketListener {

	private static final Text LOST_CONNECTION_TEXT = Text.translatable("disconnect.lost");
	private static final Logger LOGGER = LogUtils.getLogger();

	protected final MinecraftClient client;
	protected final ClientConnection connection;
	protected final @Nullable ServerInfo serverInfo;
	protected @Nullable String brand;
	protected final WorldSession worldSession;
	protected final @Nullable Screen postDisconnectScreen;
	protected boolean transferring;
	protected final Map<Identifier, byte[]> serverCookies;
	protected Map<String, String> customReportDetails;
	protected final Map<UUID, PlayerListEntry> seenPlayers;
	protected boolean seenInsecureChatWarning;

	private final List<ClientCommonNetworkHandler.QueuedPacket> queuedPackets = new ArrayList<>();
	private ServerLinks serverLinks;

	/**
	 * @param client          клиент Minecraft
	 * @param connection      сетевое соединение
	 * @param connectionState снимок состояния соединения
	 */
	protected ClientCommonNetworkHandler(
			MinecraftClient client,
			ClientConnection connection,
			ClientConnectionState connectionState
	) {
		this.client = client;
		this.connection = connection;
		serverInfo = connectionState.serverInfo();
		brand = connectionState.serverBrand();
		worldSession = connectionState.worldSession();
		postDisconnectScreen = connectionState.postDisconnectScreen();
		serverCookies = connectionState.serverCookies();
		customReportDetails = connectionState.customReportDetails();
		serverLinks = connectionState.serverLinks();
		seenPlayers = new HashMap<>(connectionState.seenPlayers());
		seenInsecureChatWarning = connectionState.seenInsecureChatWarning();
	}

	/**
	 * Возвращает ссылки сервера (баг-репорт, поддержка и т.д.).
	 *
	 * @return ссылки сервера
	 */
	public ServerLinks getServerLinks() {
		return serverLinks;
	}

	@Override
	public void onPacketException(Packet packet, Exception exception) {
		LOGGER.error("Failed to handle packet {}, disconnecting", packet, exception);
		Optional<Path> reportPath = savePacketErrorReport(packet, exception);
		Optional<URI> bugLink = serverLinks.getEntryFor(ServerLinks.Known.BUG_REPORT).map(ServerLinks.Entry::link);
		connection.disconnect(new DisconnectionInfo(Text.translatable("disconnect.packetError"), reportPath, bugLink));
	}

	@Override
	public DisconnectionInfo createDisconnectionInfo(Text reason, Throwable exception) {
		Optional<Path> reportPath = savePacketErrorReport(null, exception);
		Optional<URI> bugLink = serverLinks.getEntryFor(ServerLinks.Known.BUG_REPORT).map(ServerLinks.Entry::link);
		return new DisconnectionInfo(reason, reportPath, bugLink);
	}

	@Override
	public boolean accepts(Packet<?> packet) {
		return ClientCommonPacketListener.super.accepts(packet)
				|| (transferring && (packet instanceof StoreCookieS2CPacket
				|| packet instanceof ServerTransferS2CPacket
		)
		);
	}

	@Override
	public void onKeepAlive(KeepAliveS2CPacket packet) {
		send(
				new KeepAliveC2SPacket(packet.getId()),
				() -> RenderSystem.isFrozenAtPollEvents() == false,
				Duration.ofMinutes(1L)
		);
	}

	@Override
	public void onPing(CommonPingS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		sendPacket(new CommonPongC2SPacket(packet.getParameter()));
	}

	@Override
	public void onCustomPayload(CustomPayloadS2CPacket packet) {
		CustomPayload payload = packet.payload();

		if (payload instanceof UnknownCustomPayload) {
			return;
		}

		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		if (payload instanceof BrandCustomPayload brandPayload) {
			brand = brandPayload.brand();
			worldSession.setBrand(brandPayload.brand());
		}
		else {
			onCustomPayload(payload);
		}
	}

	/**
	 * Обрабатывает нестандартные кастомные пакеты (переопределяется в подклассах).
	 *
	 * @param payload содержимое пакета
	 */
	protected abstract void onCustomPayload(CustomPayload payload);

	@Override
	public void onResourcePackSend(ResourcePackSendS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		UUID packId = packet.id();
		URL packUrl = getParsedResourcePackUrl(packet.url());

		if (packUrl == null) {
			connection.send(new ResourcePackStatusC2SPacket(packId, ResourcePackStatusC2SPacket.Status.INVALID_URL));
			return;
		}

		String hash = packet.hash();
		boolean required = packet.required();
		ServerInfo.ResourcePackPolicy policy = serverInfo != null
		                                       ? serverInfo.getResourcePackPolicy()
		                                       : ServerInfo.ResourcePackPolicy.PROMPT;

		if (policy != ServerInfo.ResourcePackPolicy.PROMPT && (required == false
				|| policy != ServerInfo.ResourcePackPolicy.DISABLED
		)) {
			client.getServerResourcePackProvider().addResourcePack(packId, packUrl, hash);
		}
		else {
			client.setScreen(createConfirmServerResourcePackScreen(
					packId,
					packUrl,
					hash,
					required,
					packet.prompt().orElse(null)
			));
		}
	}

	@Override
	public void onResourcePackRemove(ResourcePackRemoveS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		packet.id().ifPresentOrElse(
				id -> client.getServerResourcePackProvider().remove(id),
				() -> client.getServerResourcePackProvider().removeAll()
		);
	}

	@Override
	public void onCookieRequest(CookieRequestS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		connection.send(new CookieResponseC2SPacket(packet.key(), serverCookies.get(packet.key())));
	}

	@Override
	public void onStoreCookie(StoreCookieS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		serverCookies.put(packet.key(), packet.payload());
	}

	@Override
	public void onCustomReportDetails(CustomReportDetailsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		customReportDetails = packet.details();
	}

	@Override
	public void onServerLinks(ServerLinksS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		List<ServerLinks.StringifiedEntry> entries = packet.links();
		ImmutableList.Builder<ServerLinks.Entry> builder = ImmutableList.builderWithExpectedSize(entries.size());

		for (ServerLinks.StringifiedEntry entry : entries) {
			try {
				URI uri = Util.validateUri(entry.link());
				builder.add(new ServerLinks.Entry(entry.type(), uri));
			}
			catch (Exception e) {
				LOGGER.warn("Received invalid link for type {}:{}", entry.type(), entry.link(), e);
			}
		}

		serverLinks = new ServerLinks(builder.build());
	}

	@Override
	public void onShowDialog(ShowDialogS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		showDialog(packet.dialog(), client.currentScreen);
	}

	/**
	 * Создаёт объект доступа к сети для диалогов (переопределяется в подклассах).
	 *
	 * @return объект доступа к сети
	 */
	protected abstract DialogNetworkAccess createDialogNetworkAccess();

	/**
	 * Показывает диалог поверх текущего экрана.
	 *
	 * @param dialog         диалог для отображения
	 * @param previousScreen предыдущий экран
	 */
	public void showDialog(RegistryEntry<Dialog> dialog, @Nullable Screen previousScreen) {
		showDialog(dialog, createDialogNetworkAccess(), previousScreen);
	}

	/**
	 * Показывает диалог с указанным объектом доступа к сети.
	 *
	 * @param dialog         диалог
	 * @param networkAccess  объект доступа к сети
	 * @param previousScreen предыдущий экран
	 */
	protected void showDialog(
			RegistryEntry<Dialog> dialog,
			DialogNetworkAccess networkAccess,
			@Nullable Screen previousScreen
	) {
		if (previousScreen instanceof DialogScreen.WarningScreen warningScreen) {
			Screen inner = warningScreen.getDialogScreen();
			Screen parent = inner instanceof DialogScreen<?> dialogScreen ? dialogScreen.getParentScreen() : inner;
			DialogScreen<?> created = DialogScreens.create(dialog.value(), parent, networkAccess);

			if (created != null) {
				warningScreen.setDialogScreen(created);
			}
			else {
				LOGGER.warn("Failed to show dialog for data {}", dialog);
			}

			return;
		}

		Screen parent;
		if (previousScreen instanceof DialogScreen<?> dialogScreen) {
			parent = dialogScreen.getParentScreen();
		}
		else if (previousScreen instanceof WaitingForResponseScreen waitingScreen) {
			parent = waitingScreen.getParentScreen();
		}
		else {
			parent = previousScreen;
		}

		Screen created = DialogScreens.create(dialog.value(), parent, networkAccess);
		if (created != null) {
			client.setScreen(created);
		}
		else {
			LOGGER.warn("Failed to show dialog for data {}", dialog);
		}
	}

	@Override
	public void onClearDialog(ClearDialogS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		clearDialog();
	}

	/**
	 * Закрывает текущий диалог и возвращается к предыдущему экрану.
	 */
	public void clearDialog() {
		if (client.currentScreen instanceof DialogScreen.WarningScreen warningScreen) {
			if (warningScreen.getDialogScreen() instanceof DialogScreen<?> dialogScreen) {
				warningScreen.setDialogScreen(dialogScreen.getParentScreen());
			}
		}
		else if (client.currentScreen instanceof DialogScreen<?> dialogScreen) {
			client.setScreen(dialogScreen.getParentScreen());
		}
	}

	@Override
	public void onServerTransfer(ServerTransferS2CPacket packet) {
		transferring = true;
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		if (serverInfo == null) {
			throw new IllegalStateException("Cannot transfer to server from singleplayer");
		}

		connection.disconnect(Text.translatable("disconnect.transfer"));
		connection.tryDisableAutoRead();
		connection.handleDisconnection();

		ServerAddress target = new ServerAddress(packet.host(), packet.port());
		ConnectScreen.connect(
				Objects.requireNonNullElseGet(postDisconnectScreen, TitleScreen::new),
				client,
				target,
				serverInfo,
				false,
				new CookieStorage(serverCookies, seenPlayers, seenInsecureChatWarning)
		);
	}

	@Override
	public void onDisconnect(DisconnectS2CPacket packet) {
		connection.disconnect(packet.reason());
	}

	/**
	 * Отправляет накопленные пакеты, условие отправки которых стало истинным.
	 * Удаляет просроченные пакеты.
	 */
	protected void sendQueuedPackets() {
		Iterator<ClientCommonNetworkHandler.QueuedPacket> iterator = queuedPackets.iterator();

		while (iterator.hasNext()) {
			ClientCommonNetworkHandler.QueuedPacket queued = iterator.next();

			if (queued.sendCondition().getAsBoolean()) {
				sendPacket(queued.packet);
				iterator.remove();
			}
			else if (queued.expirationTime() <= Util.getMeasuringTimeMs()) {
				iterator.remove();
			}
		}
	}

	/**
	 * Отправляет пакет через соединение.
	 *
	 * @param packet пакет для отправки
	 */
	public void sendPacket(Packet<?> packet) {
		connection.send(packet);
	}

	@Override
	public void onDisconnected(DisconnectionInfo info) {
		worldSession.onUnload();
		client.disconnectWithScreen(createDisconnectedScreen(info), transferring);
		LOGGER.warn("Client disconnected with reason: {}", info.reason().getString());
	}

	@Override
	public void addCustomCrashReportInfo(CrashReport report, CrashReportSection section) {
		section.add("Is Local", () -> String.valueOf(connection.isLocal()));
		section.add("Server type", () -> serverInfo != null ? serverInfo.getServerType().toString() : "<none>");
		section.add("Server brand", () -> brand);

		if (customReportDetails.isEmpty() == false) {
			CrashReportSection detailsSection = report.addElement("Custom Server Details");
			customReportDetails.forEach(detailsSection::add);
		}
	}

	/**
	 * Возвращает имя бренда сервера (мода или ванилла).
	 *
	 * @return бренд или {@code null}
	 */
	public @Nullable String getBrand() {
		return brand;
	}

	/**
	 * Создаёт экран отключения в зависимости от типа сервера.
	 *
	 * @param info информация об отключении
	 * @return экран отключения
	 */
	protected Screen createDisconnectedScreen(DisconnectionInfo info) {
		Screen fallback = Objects.requireNonNullElseGet(
				postDisconnectScreen,
				() -> serverInfo != null ? new MultiplayerScreen(new TitleScreen()) : new TitleScreen()
		);
		return serverInfo != null && serverInfo.isRealm()
		       ? new DisconnectedScreen(fallback, LOST_CONNECTION_TEXT, info, ScreenTexts.BACK)
		       : new DisconnectedScreen(fallback, LOST_CONNECTION_TEXT, info);
	}

	private void send(Packet<? extends ServerPacketListener> packet, BooleanSupplier sendCondition, Duration expiry) {
		if (sendCondition.getAsBoolean()) {
			sendPacket(packet);
		}
		else {
			queuedPackets.add(new ClientCommonNetworkHandler.QueuedPacket(
					packet,
					sendCondition,
					Util.getMeasuringTimeMs() + expiry.toMillis()
			));
		}
	}

	private Optional<Path> savePacketErrorReport(@Nullable Packet packet, Throwable exception) {
		CrashReport report = CrashReport.create(exception, "Packet handling error");
		NetworkThreadUtils.fillCrashReport(report, this, packet);
		Path debugDir = client.runDirectory.toPath().resolve("debug");
		Path reportFile = debugDir.resolve("disconnect-" + Util.getFormattedCurrentTime() + "-client.txt");
		Optional<ServerLinks.Entry> bugEntry = serverLinks.getEntryFor(ServerLinks.Known.BUG_REPORT);
		List<String> extraLines = bugEntry
				.<List<String>>map(e -> List.of("Server bug reporting link: " + e.link()))
				.orElse(List.of());
		return report.writeToFile(reportFile, ReportType.MINECRAFT_NETWORK_PROTOCOL_ERROR_REPORT, extraLines)
		       ? Optional.of(reportFile)
		       : Optional.empty();
	}

	private Screen createConfirmServerResourcePackScreen(
			UUID id,
			URL url,
			String hash,
			boolean required,
			@Nullable Text prompt
	) {
		Screen current = client.currentScreen;
		return current instanceof ClientCommonNetworkHandler.ConfirmServerResourcePackScreen existing
		       ? existing.add(client, id, url, hash, required, prompt)
		       : new ClientCommonNetworkHandler.ConfirmServerResourcePackScreen(
				       client,
				       current,
				       List.of(new ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack(id, url, hash)),
				       required,
				       prompt
		       );
	}

	private static @Nullable URL getParsedResourcePackUrl(String url) {
		try {
			URL parsed = new URL(url);
			String protocol = parsed.getProtocol();
			return "http".equals(protocol) || "https".equals(protocol) ? parsed : null;
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	static Text getPrompt(Text requirementPrompt, @Nullable Text customPrompt) {
		return customPrompt == null
		       ? requirementPrompt
		       : Text.translatable("multiplayer.texturePrompt.serverPrompt", requirementPrompt, customPrompt);
	}

	/**
	 * Базовый класс для доступа к сети из диалогов.
	 */
	@Environment(EnvType.CLIENT)
	protected abstract class CommonDialogNetworkAccess implements DialogNetworkAccess {

		@Override
		public void disconnect(Text reason) {
			ClientCommonNetworkHandler.this.connection.disconnect(reason);
			ClientCommonNetworkHandler.this.connection.handleDisconnection();
		}

		@Override
		public void showDialog(RegistryEntry<Dialog> dialog, @Nullable Screen afterActionScreen) {
			ClientCommonNetworkHandler.this.showDialog(dialog, this, afterActionScreen);
		}

		@Override
		public void sendCustomClickActionPacket(Identifier id, Optional<NbtElement> payload) {
			ClientCommonNetworkHandler.this.sendPacket(new CustomClickActionC2SPacket(id, payload));
		}

		@Override
		public ServerLinks getServerLinks() {
			return ClientCommonNetworkHandler.this.getServerLinks();
		}
	}

	/**
	 * Экран подтверждения загрузки ресурс-пака сервера.
	 */
	@Environment(EnvType.CLIENT)
	class ConfirmServerResourcePackScreen extends ConfirmScreen {

		private final List<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack> packs;
		private final @Nullable Screen parent;

		ConfirmServerResourcePackScreen(
				final @Nullable MinecraftClient client,
				final Screen parent,
				final List<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack> pack,
				final boolean required,
				final @Nullable Text prompt
		) {
			super(
					confirmed -> {
						client.setScreen(parent);
						ServerResourcePackLoader loader = client.getServerResourcePackProvider();

						if (confirmed) {
							if (ClientCommonNetworkHandler.this.serverInfo != null) {
								ClientCommonNetworkHandler.this.serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.ENABLED);
							}

							loader.acceptAll();
						}
						else {
							loader.declineAll();

							if (required) {
								ClientCommonNetworkHandler.this.connection.disconnect(
										Text.translatable("multiplayer.requiredTexturePrompt.disconnect")
								);
							}
							else if (ClientCommonNetworkHandler.this.serverInfo != null) {
								ClientCommonNetworkHandler.this.serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
							}
						}

						for (ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack p : pack) {
							loader.addResourcePack(p.id, p.url, p.hash);
						}

						if (ClientCommonNetworkHandler.this.serverInfo != null) {
							ServerList.updateServerListEntry(ClientCommonNetworkHandler.this.serverInfo);
						}
					},
					required
					? Text.translatable("multiplayer.requiredTexturePrompt.line1")
					: Text.translatable("multiplayer.texturePrompt.line1"),
					ClientCommonNetworkHandler.getPrompt(
							required
							? Text
							  .translatable("multiplayer.requiredTexturePrompt.line2")
							  .formatted(Formatting.YELLOW, Formatting.BOLD)
							: Text.translatable("multiplayer.texturePrompt.line2"),
							prompt
					),
					required ? ScreenTexts.PROCEED : ScreenTexts.YES,
					required ? ScreenTexts.DISCONNECT : ScreenTexts.NO
			);
			this.packs = pack;
			this.parent = parent;
		}

		public ClientCommonNetworkHandler.ConfirmServerResourcePackScreen add(
				MinecraftClient client,
				UUID id,
				URL url,
				String hash,
				boolean required,
				@Nullable Text prompt
		) {
			List<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack> updated =
					ImmutableList
							.<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack>builderWithExpectedSize(
									packs.size() + 1)
							.addAll(packs)
							.add(new ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack(id, url, hash))
							.build();
			return ClientCommonNetworkHandler.this.new ConfirmServerResourcePackScreen(
					client,
					parent,
					updated,
					required,
					prompt
			);
		}

		/**
		 * Данные одного ресурс-пака для загрузки.
		 */
		@Environment(EnvType.CLIENT)
		record Pack(UUID id, URL url, String hash) {
		}
	}

	/**
	 * Пакет, ожидающий выполнения условия перед отправкой.
	 */
	@Environment(EnvType.CLIENT)
	record QueuedPacket(
			Packet<? extends ServerPacketListener> packet,
			BooleanSupplier sendCondition,
			long expirationTime
	) {
	}
}
