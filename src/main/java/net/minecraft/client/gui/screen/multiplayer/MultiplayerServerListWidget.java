package net.minecraft.client.gui.screen.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldIcon;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.LoadingWidget;
import net.minecraft.client.gui.widget.SquareWidgetEntry;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.LanServerInfo;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Виджет списка серверов мультиплеера — отображает сохранённые серверы, LAN-серверы
 * и строку сканирования локальной сети. Управляет пингом серверов в фоновом пуле потоков.
 */
@Environment(EnvType.CLIENT)
public class MultiplayerServerListWidget extends AlwaysSelectedEntryListWidget<MultiplayerServerListWidget.Entry> {

	static final Identifier INCOMPATIBLE_TEXTURE = Identifier.ofVanilla("server_list/incompatible");
	static final Identifier UNREACHABLE_TEXTURE = Identifier.ofVanilla("server_list/unreachable");
	static final Identifier PING_1_TEXTURE = Identifier.ofVanilla("server_list/ping_1");
	static final Identifier PING_2_TEXTURE = Identifier.ofVanilla("server_list/ping_2");
	static final Identifier PING_3_TEXTURE = Identifier.ofVanilla("server_list/ping_3");
	static final Identifier PING_4_TEXTURE = Identifier.ofVanilla("server_list/ping_4");
	static final Identifier PING_5_TEXTURE = Identifier.ofVanilla("server_list/ping_5");
	static final Identifier PINGING_1_TEXTURE = Identifier.ofVanilla("server_list/pinging_1");
	static final Identifier PINGING_2_TEXTURE = Identifier.ofVanilla("server_list/pinging_2");
	static final Identifier PINGING_3_TEXTURE = Identifier.ofVanilla("server_list/pinging_3");
	static final Identifier PINGING_4_TEXTURE = Identifier.ofVanilla("server_list/pinging_4");
	static final Identifier PINGING_5_TEXTURE = Identifier.ofVanilla("server_list/pinging_5");
	static final Identifier JOIN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("server_list/join_highlighted");
	static final Identifier JOIN_TEXTURE = Identifier.ofVanilla("server_list/join");
	static final Identifier MOVE_UP_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("server_list/move_up_highlighted");
	static final Identifier MOVE_UP_TEXTURE = Identifier.ofVanilla("server_list/move_up");
	static final Identifier MOVE_DOWN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("server_list/move_down_highlighted");
	static final Identifier MOVE_DOWN_TEXTURE = Identifier.ofVanilla("server_list/move_down");
	static final Logger LOGGER = LogUtils.getLogger();
	static final ThreadPoolExecutor SERVER_PINGER_THREAD_POOL = new ScheduledThreadPoolExecutor(
		5,
		new ThreadFactoryBuilder()
			.setNameFormat("Server Pinger #%d")
			.setDaemon(true)
			.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER))
			.build()
	);
	static final Text LAN_SCANNING_TEXT = Text.translatable("lanServer.scanning");
	static final Text CANNOT_RESOLVE_TEXT = Text.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
	static final Text CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect").withColor(-65536);
	static final Text INCOMPATIBLE_TEXT = Text.translatable("multiplayer.status.incompatible");
	static final Text NO_CONNECTION_TEXT = Text.translatable("multiplayer.status.no_connection");
	static final Text PINGING_TEXT = Text.translatable("multiplayer.status.pinging");
	static final Text ONLINE_TEXT = Text.translatable("multiplayer.status.online");

	private final MultiplayerScreen screen;
	private final List<ServerEntry> servers = Lists.newArrayList();
	private final Entry scanningEntry = new ScanningEntry();
	private final List<LanServerEntry> lanServers = Lists.newArrayList();

	public MultiplayerServerListWidget(
		MultiplayerScreen screen,
		MinecraftClient client,
		int width,
		int height,
		int top,
		int bottom
	) {
		super(client, width, height, top, bottom);
		this.screen = screen;
	}

	/**
	 * Перестраивает список записей: сохранённые серверы + строка сканирования + LAN-серверы.
	 * Восстанавливает ранее выбранный элемент по типу.
	 */
	private void updateEntries() {
		Entry previousSelection = getSelectedOrNull();
		List<Entry> allEntries = new ArrayList<>(servers);
		allEntries.add(scanningEntry);
		allEntries.addAll(lanServers);
		replaceEntries(allEntries);
		if (previousSelection == null) {
			return;
		}

		for (Entry entry : allEntries) {
			if (entry.isOfSameType(previousSelection)) {
				setSelected(entry);
				break;
			}
		}
	}

	public void setSelected(@Nullable Entry entry) {
		super.setSelected(entry);
		screen.updateButtonActivationStates();
	}

	/**
	 * Заменяет список сохранённых серверов и перестраивает виджет.
	 */
	public void setServers(ServerList serverList) {
		servers.clear();
		for (int index = 0; index < serverList.size(); index++) {
			servers.add(new ServerEntry(screen, serverList.get(index)));
		}

		updateEntries();
	}

	/**
	 * Обновляет список LAN-серверов. Для каждого нового сервера, видимого в текущей области
	 * прокрутки, произносит нарративное сообщение об обнаружении.
	 */
	public void setLanServers(List<LanServerInfo> newLanServers) {
		int newCount = newLanServers.size() - lanServers.size();
		lanServers.clear();
		for (LanServerInfo lanServerInfo : newLanServers) {
			lanServers.add(new LanServerEntry(screen, lanServerInfo));
		}

		updateEntries();

		for (int entryIndex = lanServers.size() - newCount; entryIndex < lanServers.size(); entryIndex++) {
			LanServerEntry lanEntry = lanServers.get(entryIndex);
			int listIndex = entryIndex - lanServers.size() + children().size();
			int rowTop = getRowTop(listIndex);
			int rowBottom = getRowBottom(listIndex);
			if (rowBottom >= getY() && rowTop <= getBottom()) {
				client.getNarratorManager().narrateSystemMessage(
					Text.translatable("multiplayer.lan.server_found", lanEntry.getMotdNarration())
				);
			}
		}
	}

	@Override
	public int getRowWidth() {
		return 305;
	}

	public void onRemoved() {
	}

	@Environment(EnvType.CLIENT)
	public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> implements AutoCloseable {

		@Override
		public void close() {
		}

		abstract boolean isOfSameType(Entry entry);

		public abstract void connect();
	}

	@Environment(EnvType.CLIENT)
	public static class LanServerEntry extends Entry {

		private static final int LAN_ICON_SIZE = 32;
		private static final int TEXT_OFFSET_X = 3;
		private static final int TITLE_OFFSET_Y = 1;
		private static final int MOTD_OFFSET_Y = 12;
		private static final int ADDRESS_OFFSET_Y = 23;
		private static final int TEXT_COLOR_GRAY = -8355712;
		private static final Text TITLE_TEXT = Text.translatable("lanServer.title");
		private static final Text HIDDEN_ADDRESS_TEXT = Text.translatable("selectServer.hiddenAddress");

		private final MultiplayerScreen screen;
		protected final MinecraftClient client;
		protected final LanServerInfo server;

		protected LanServerEntry(MultiplayerScreen screen, LanServerInfo server) {
			this.screen = screen;
			this.server = server;
			client = MinecraftClient.getInstance();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int textX = getContentX() + LAN_ICON_SIZE + TEXT_OFFSET_X;
			context.drawTextWithShadow(client.textRenderer, TITLE_TEXT, textX, getContentY() + TITLE_OFFSET_Y, -1);
			context.drawTextWithShadow(client.textRenderer, server.getMotd(), textX, getContentY() + MOTD_OFFSET_Y, TEXT_COLOR_GRAY);
			net.minecraft.text.Text addressText = client.options.hideServerAddress ? HIDDEN_ADDRESS_TEXT : net.minecraft.text.Text.literal(server.getAddressPort());
			context.drawTextWithShadow(client.textRenderer, addressText, textX, getContentY() + ADDRESS_OFFSET_Y, TEXT_COLOR_GRAY);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			if (doubled) {
				connect();
			}

			return super.mouseClicked(click, doubled);
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnterOrSpace()) {
				connect();
				return true;
			}

			return super.keyPressed(input);
		}

		@Override
		public void connect() {
			screen.connect(new ServerInfo(server.getMotd(), server.getAddressPort(), ServerInfo.ServerType.LAN));
		}

		@Override
		public Text getNarration() {
			return Text.translatable("narrator.select", getMotdNarration());
		}

		public Text getMotdNarration() {
			return Text.empty().append(TITLE_TEXT).append(ScreenTexts.SPACE).append(server.getMotd());
		}

		@Override
		boolean isOfSameType(Entry entry) {
			return entry instanceof LanServerEntry lanEntry && lanEntry.server == server;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class ScanningEntry extends Entry {

		private final MinecraftClient client = MinecraftClient.getInstance();
		private final LoadingWidget loadingWidget = new LoadingWidget(client.textRenderer, LAN_SCANNING_TEXT);

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			loadingWidget.setPosition(
				getContentMiddleX() - client.textRenderer.getWidth(LAN_SCANNING_TEXT) / 2,
				getContentY()
			);
			loadingWidget.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public Text getNarration() {
			return LAN_SCANNING_TEXT;
		}

		@Override
		boolean isOfSameType(Entry entry) {
			return entry instanceof ScanningEntry;
		}

		@Override
		public void connect() {
		}
	}

	@Environment(EnvType.CLIENT)
	public class ServerEntry extends Entry implements SquareWidgetEntry {

		private static final int SERVER_ICON_SIZE = 32;
		private static final int PADDING = 5;
		private static final int ICON_PADDING = 10;
		private static final int TEXT_PADDING = 8;
		private static final int ICON_HEIGHT = 8;
		private static final int OVERLAY_COLOR = -1601138544;
		private static final int TEXT_COLOR_WHITE = -1;
		private static final int TEXT_COLOR_GRAY = -8355712;
		private static final long PING_EXCELLENT = 150L;
		private static final long PING_GOOD = 300L;
		private static final long PING_MEDIUM = 600L;
		private static final long PING_BAD = 1000L;
		private static final int PING_ANIM_PERIOD = 8;
		private static final int PING_ANIM_HALF = 4;

		private final MultiplayerScreen screen;
		private final MinecraftClient client;
		private final ServerInfo server;
		private final WorldIcon icon;
		private byte @Nullable [] favicon;
		private @Nullable List<Text> playerListSummary;
		private @Nullable Identifier statusIconTexture;
		private @Nullable Text statusTooltipText;

		protected ServerEntry(final MultiplayerScreen screen, final ServerInfo server) {
			this.screen = screen;
			this.server = server;
			client = MinecraftClient.getInstance();
			icon = WorldIcon.forServer(client.getTextureManager(), server.address);
			update();
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			if (server.getStatus() == ServerInfo.Status.INITIAL) {
				server.setStatus(ServerInfo.Status.PINGING);
				server.label = ScreenTexts.EMPTY;
				server.playerCountLabel = ScreenTexts.EMPTY;
				SERVER_PINGER_THREAD_POOL.submit(() -> {
					try {
						screen.getServerListPinger().add(
							server,
							() -> client.execute(this::saveFile),
							() -> {
								server.setStatus(
									server.protocolVersion == SharedConstants.getGameVersion().protocolVersion()
										? ServerInfo.Status.SUCCESSFUL
										: ServerInfo.Status.INCOMPATIBLE
								);
								client.execute(this::update);
							},
							NetworkingBackend.remote(client.options.shouldUseNativeTransport())
						);
					}
					catch (UnknownHostException e) {
						server.setStatus(ServerInfo.Status.UNREACHABLE);
						server.label = CANNOT_RESOLVE_TEXT;
						client.execute(this::update);
					}
					catch (Exception e) {
						server.setStatus(ServerInfo.Status.UNREACHABLE);
						server.label = CANNOT_CONNECT_TEXT;
						client.execute(this::update);
					}
				});
			}

			context.drawTextWithShadow(
				client.textRenderer,
				server.name,
				getContentX() + SERVER_ICON_SIZE + TEXT_PADDING - PADDING,
				getContentY() + 1,
				TEXT_COLOR_WHITE
			);
			List<OrderedText> labelLines = client.textRenderer.wrapLines(
				server.label,
				getContentWidth() - SERVER_ICON_SIZE - 2
			);
			for (int lineIndex = 0; lineIndex < Math.min(labelLines.size(), 2); lineIndex++) {
				context.drawTextWithShadow(
					client.textRenderer,
					labelLines.get(lineIndex),
					getContentX() + SERVER_ICON_SIZE + TEXT_PADDING - PADDING,
					getContentY() + 12 + 9 * lineIndex,
					TEXT_COLOR_GRAY
				);
			}

			draw(context, getContentX(), getContentY(), icon.getTextureId());
			int entryIndex = MultiplayerServerListWidget.this.children().indexOf(this);
			if (server.getStatus() == ServerInfo.Status.PINGING) {
				int pingFrame = (int) (Util.getMeasuringTimeMs() / 100L + entryIndex * 2 & 7L);
				if (pingFrame > PING_ANIM_HALF) {
					pingFrame = PING_ANIM_PERIOD - pingFrame;
				}

				statusIconTexture = switch (pingFrame) {
					case 1 -> PINGING_2_TEXTURE;
					case 2 -> PINGING_3_TEXTURE;
					case 3 -> PINGING_4_TEXTURE;
					case 4 -> PINGING_5_TEXTURE;
					default -> PINGING_1_TEXTURE;
				};
			}

			int iconX = getContentRightEnd() - ICON_PADDING - PADDING;
			if (statusIconTexture != null) {
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, statusIconTexture, iconX, getContentY(), ICON_PADDING, ICON_HEIGHT);
			}

			byte[] currentFavicon = server.getFavicon();
			if (!Arrays.equals(currentFavicon, favicon)) {
				if (uploadFavicon(currentFavicon)) {
					favicon = currentFavicon;
				}
				else {
					server.setFavicon(null);
					saveFile();
				}
			}

			Text playerCountText = server.getStatus() == ServerInfo.Status.INCOMPATIBLE
				? server.version.copy().formatted(Formatting.RED)
				: server.playerCountLabel;
			int playerCountWidth = client.textRenderer.getWidth(playerCountText);
			int playerCountX = iconX - playerCountWidth - PADDING;
			context.drawTextWithShadow(client.textRenderer, playerCountText, playerCountX, getContentY() + 1, TEXT_COLOR_GRAY);
			if (statusTooltipText != null
				&& mouseX >= iconX && mouseX <= iconX + ICON_PADDING
				&& mouseY >= getContentY() && mouseY <= getContentY() + ICON_HEIGHT) {
				context.drawTooltip(statusTooltipText, mouseX, mouseY);
			}
			else if (playerListSummary != null
				&& mouseX >= playerCountX && mouseX <= playerCountX + playerCountWidth
				&& mouseY >= getContentY() && mouseY <= getContentY() - 1 + 9) {
				context.drawTooltip(Lists.transform(playerListSummary, Text::asOrderedText), mouseX, mouseY);
			}

			if (client.options.getTouchscreen().getValue() || hovered) {
				context.fill(
					getContentX(), getContentY(),
					getContentX() + SERVER_ICON_SIZE, getContentY() + SERVER_ICON_SIZE,
					OVERLAY_COLOR
				);
				int relX = mouseX - getContentX();
				int relY = mouseY - getContentY();
				if (isRight(relX, relY, SERVER_ICON_SIZE)) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, JOIN_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), SERVER_ICON_SIZE, SERVER_ICON_SIZE);
					MultiplayerServerListWidget.this.setCursor(context);
				}
				else {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, JOIN_TEXTURE, getContentX(), getContentY(), SERVER_ICON_SIZE, SERVER_ICON_SIZE);
				}

				if (entryIndex > 0) {
					if (isBottomLeft(relX, relY, SERVER_ICON_SIZE)) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, MOVE_UP_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), SERVER_ICON_SIZE, SERVER_ICON_SIZE);
						MultiplayerServerListWidget.this.setCursor(context);
					}
					else {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, MOVE_UP_TEXTURE, getContentX(), getContentY(), SERVER_ICON_SIZE, SERVER_ICON_SIZE);
					}
				}

				if (entryIndex < screen.getServerList().size() - 1) {
					if (isTopLeft(relX, relY, SERVER_ICON_SIZE)) {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_HIGHLIGHTED_TEXTURE, getContentX(), getContentY(), SERVER_ICON_SIZE, SERVER_ICON_SIZE);
						MultiplayerServerListWidget.this.setCursor(context);
					}
					else {
						context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_TEXTURE, getContentX(), getContentY(), SERVER_ICON_SIZE, SERVER_ICON_SIZE);
					}
				}
			}
		}

		/**
		 * Обновляет иконку статуса и тултип на основе текущего состояния пинга сервера.
		 * Выбирает текстуру иконки по диапазону задержки (ping < 150ms → 5 полосок, и т.д.).
		 */
		private void update() {
			playerListSummary = null;
			switch (server.getStatus()) {
				case INITIAL, PINGING -> {
					statusIconTexture = PING_1_TEXTURE;
					statusTooltipText = PINGING_TEXT;
				}
				case INCOMPATIBLE -> {
					statusIconTexture = INCOMPATIBLE_TEXTURE;
					statusTooltipText = INCOMPATIBLE_TEXT;
					playerListSummary = server.playerListSummary;
				}
				case UNREACHABLE -> {
					statusIconTexture = UNREACHABLE_TEXTURE;
					statusTooltipText = NO_CONNECTION_TEXT;
				}
				case SUCCESSFUL -> {
					if (server.ping < PING_EXCELLENT) {
						statusIconTexture = PING_5_TEXTURE;
					}
					else if (server.ping < PING_GOOD) {
						statusIconTexture = PING_4_TEXTURE;
					}
					else if (server.ping < PING_MEDIUM) {
						statusIconTexture = PING_3_TEXTURE;
					}
					else if (server.ping < PING_BAD) {
						statusIconTexture = PING_2_TEXTURE;
					}
					else {
						statusIconTexture = PING_1_TEXTURE;
					}

					statusTooltipText = Text.translatable("multiplayer.status.ping", server.ping);
					playerListSummary = server.playerListSummary;
				}
			}
		}

		public void saveFile() {
			screen.getServerList().saveFile();
		}

		protected void draw(DrawContext context, int x, int y, Identifier textureId) {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0F, 0.0F, SERVER_ICON_SIZE, SERVER_ICON_SIZE, SERVER_ICON_SIZE, SERVER_ICON_SIZE);
		}

		/**
		 * Загружает фавикон сервера в текстурный менеджер. При ошибке декодирования
		 * логирует предупреждение и возвращает {@code false}, чтобы сбросить фавикон.
		 */
		private boolean uploadFavicon(byte @Nullable [] bytes) {
			if (bytes == null) {
				icon.destroy();
				return true;
			}

			try {
				icon.load(NativeImage.read(bytes));
				return true;
			}
			catch (Throwable error) {
				LOGGER.error("Invalid icon for server {} ({})", server.name, server.address, error);
				return false;
			}
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnterOrSpace()) {
				connect();
				return true;
			}

			if (!input.hasShift()) {
				return super.keyPressed(input);
			}

			MultiplayerServerListWidget widget = screen.serverListWidget;
			int index = widget.children().indexOf(this);
			if (index == -1) {
				return true;
			}

			if (input.isDown() && index < screen.getServerList().size() - 1
				|| input.isUp() && index > 0) {
				swapEntries(index, input.isDown() ? index + 1 : index - 1);
				return true;
			}

			return super.keyPressed(input);
		}

		@Override
		public void connect() {
			screen.connect(server);
		}

		private void swapEntries(int fromIndex, int toIndex) {
			screen.getServerList().swapEntries(fromIndex, toIndex);
			screen.serverListWidget.swapEntriesOnPositions(fromIndex, toIndex);
		}

		@Override
		public boolean mouseClicked(Click click, boolean doubled) {
			int relX = (int) click.x() - getContentX();
			int relY = (int) click.y() - getContentY();
			if (isRight(relX, relY, SERVER_ICON_SIZE)) {
				connect();
				return true;
			}

			int entryIndex = screen.serverListWidget.children().indexOf(this);
			if (entryIndex > 0 && isBottomLeft(relX, relY, SERVER_ICON_SIZE)) {
				swapEntries(entryIndex, entryIndex - 1);
				return true;
			}

			if (entryIndex < screen.getServerList().size() - 1 && isTopLeft(relX, relY, SERVER_ICON_SIZE)) {
				swapEntries(entryIndex, entryIndex + 1);
				return true;
			}

			if (doubled) {
				connect();
			}

			return super.mouseClicked(click, doubled);
		}

		public ServerInfo getServer() {
			return server;
		}

		@Override
		public Text getNarration() {
			MutableText narration = Text.empty();
			narration.append(Text.translatable("narrator.select", server.name));
			narration.append(ScreenTexts.SENTENCE_SEPARATOR);
			switch (server.getStatus()) {
				case PINGING -> narration.append(PINGING_TEXT);
				case INCOMPATIBLE -> {
					narration.append(INCOMPATIBLE_TEXT);
					narration.append(ScreenTexts.SENTENCE_SEPARATOR);
					narration.append(Text.translatable("multiplayer.status.version.narration", server.version));
					narration.append(ScreenTexts.SENTENCE_SEPARATOR);
					narration.append(Text.translatable("multiplayer.status.motd.narration", server.label));
				}
				case UNREACHABLE -> narration.append(NO_CONNECTION_TEXT);
				default -> {
					narration.append(ONLINE_TEXT);
					narration.append(ScreenTexts.SENTENCE_SEPARATOR);
					narration.append(Text.translatable("multiplayer.status.ping.narration", server.ping));
					narration.append(ScreenTexts.SENTENCE_SEPARATOR);
					narration.append(Text.translatable("multiplayer.status.motd.narration", server.label));
					if (server.players != null) {
						narration.append(ScreenTexts.SENTENCE_SEPARATOR);
						narration.append(Text.translatable(
							"multiplayer.status.player_count.narration",
							server.players.online(),
							server.players.max()
						));
						narration.append(ScreenTexts.SENTENCE_SEPARATOR);
						narration.append(Texts.join(server.playerListSummary, Text.literal(", ")));
					}
				}
			}

			return narration;
		}

		@Override
		public void close() {
			icon.close();
		}

		@Override
		boolean isOfSameType(Entry entry) {
			return entry instanceof ServerEntry serverEntry && serverEntry.server == server;
		}
	}
}
